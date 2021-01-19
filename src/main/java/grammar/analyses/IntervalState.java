package grammar.analyses;

import com.google.common.primitives.Ints;
import grammar.AST;
import org.apache.commons.lang3.ArrayUtils;
import org.nd4j.linalg.api.ndarray.BaseNDArray;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.ops.impl.transforms.strict.Log;
import org.nd4j.linalg.api.shape.Shape;
import org.nd4j.linalg.cpu.nativecpu.NDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.BooleanIndexing;
import org.nd4j.linalg.indexing.conditions.Condition;
import org.nd4j.linalg.indexing.conditions.Conditions;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.*;

import static org.nd4j.linalg.ops.transforms.Transforms.exp;
import static org.nd4j.linalg.ops.transforms.Transforms.log;


public class IntervalState extends AbstractState{
    // deprecated
    @Deprecated
    public Map<String, Integer> paramMap = new HashMap<>();// idx for lower, upper idx is lower + 1
    @Deprecated
    public INDArray intervalProbPairs = null; // each row has prob, param1_l, param1_u, param2...

    // in use
    public List<Long> dimSize = new ArrayList<>(); // length of each dim, shape of probCube
    public INDArray[] probCube = new INDArray[2]; // Stores log prob
    public Map<String, Pair<Integer, INDArray>> paramValues = new HashMap<>(); // dim idx and values of params

    IntervalState() {
        dimSize.add((long) 1);
    }

    public Integer intOut(String intName) {
        Pair<Integer, INDArray> pair =  paramValues.remove(intName);
        Integer intIdx = pair.getKey();
        dimSize.remove((int) intIdx);
        long[] squeezeShape = getSqueezeShape(intIdx, probCube[0]);
        probCube[0] = probCube[0].reshape(squeezeShape);
        probCube[1] = probCube[1].reshape(squeezeShape);
        for (Pair<Integer, INDArray> pp:paramValues.values()) {
            if (pp.getKey() > intIdx) {
                squeezeShape = getSqueezeShape(intIdx, pp.getValue());
                pp.setValue(pp.getValue().reshape(squeezeShape));
                pp.setKey(pp.getKey() - 1);
            }
        }
        return intIdx;
    }

    private static long[] getSqueezeShape(Integer intIdx, INDArray valueCube) {
        long[] realShape = valueCube.shape();
        long[] squeezeShape = new long[realShape.length - 1];
        System.arraycopy(realShape, 0, squeezeShape, 0, intIdx);
        System.arraycopy(realShape, intIdx+1, squeezeShape, intIdx, squeezeShape.length - intIdx);
        return squeezeShape;
    }

    public void addDepParamCube(String paramName, INDArray splits) {
        paramValues.put(paramName, new Pair<>(null,splits));
    }

    public void addParamCube(String paramName, INDArray splits, INDArray probLower, INDArray probUpper) {
        if (splits.shape().length == 0) {
            addDepParamCube(paramName, Nd4j.empty());
            return;
        }
        int currDim = dimSize.size();
        long newSplitLen = splits.shape()[splits.shape().length - 1];
        if (currDim == 1) {
            paramValues.put(paramName, new Pair<>(currDim, splits.reshape(1,splits.length())));
            dimSize.add(newSplitLen);
            probCube[0] = (probLower.reshape(1,newSplitLen));
            probCube[1] = (probUpper.reshape(1, newSplitLen));
        }
        else {
            INDArray oldProbLower = probCube[0];
            INDArray oldProbUpper = probCube[1];
            dimSize.add((long) 1);
            long[] singleDim = new long[dimSize.size()];
            Arrays.fill(singleDim, 1);
            long[] realShapes = splits.shape();
            if (splits.shape().length > 1) {
                // System.arraycopy(realShapes, 0, singleDim, 0, realShapes.length); // changed
                for (int i = 1; i <= realShapes.length; i++)
                    singleDim[singleDim.length - i] = realShapes[realShapes.length - i];
            }
            singleDim[dimSize.size() - 1] = newSplitLen;
            paramValues.put(paramName, new Pair<>(currDim, splits.reshape(singleDim)));
            // dimSize: 2,3,4,1  singleDim: 1,3,1,5
            int[] oldDimSize = Ints.toArray(dimSize);
            dimSize.set(dimSize.size() - 1, newSplitLen);
            long[] outComeDimSize = dimSize.stream().mapToLong(i -> i).toArray();
            if (paramName.contains("robust_local_") || paramName.contains("robust_weight")) {
                probCube[0] = oldProbLower.reshape(oldDimSize);
                probCube[1] = oldProbUpper.reshape(oldDimSize);
                dimSize.set(dimSize.size() - 1, (long) 1);
            } else {
                probCube[0] = oldProbLower.reshape(oldDimSize).broadcast(outComeDimSize).addi(log(probLower).reshape(singleDim).broadcast(outComeDimSize));
                probCube[1] = oldProbUpper.reshape(oldDimSize).broadcast(outComeDimSize).addi(log(probUpper).reshape(singleDim).broadcast(outComeDimSize));
            }
        }
    }

    public double getResultsMean(String param) {
        INDArray lower = probCube[0];
        INDArray upper = probCube[1];
        Integer nDim = lower.shape().length;
        int[] numbers = new int[nDim - 1];
        for(int i = 1; i < nDim; i++){
            numbers[i - 1] = i;
        }
        Pair<Integer, INDArray> paramIdxValues = paramValues.get(param);
        Integer paramDimIdx = paramIdxValues.getKey();
        numbers[paramDimIdx - 1] = 0;
        INDArray paramvalue = paramIdxValues.getValue();
        INDArray currsumLower = lower.sum(numbers);
        INDArray currsumUpper = upper.sum(numbers);
        INDArray flatSingles = Nd4j.toFlattened(paramvalue);
        INDArray lowerMaxIdx = Nd4j.toFlattened(currsumLower).argMax();
        INDArray upperMaxIdx = Nd4j.toFlattened(currsumUpper).argMax();
        double lowerMax = flatSingles.getDouble(lowerMaxIdx.getLong());
        double upperMax = flatSingles.getDouble(upperMaxIdx.getLong());
        return (lowerMax + upperMax) /2;
    }

    public void writeResults(Set<String> strings, String path) {
        if (probCube[0] == null || paramValues.size() == 1)
            return;
        INDArray logLower = probCube[0];
        INDArray logUpper = probCube[1];
        BooleanIndexing.replaceWhere(logLower, Math.pow(10,16), Conditions.greaterThan(Math.pow(10,16)));
        BooleanIndexing.replaceWhere(logUpper, Math.pow(10,16), Conditions.greaterThan(Math.pow(10,16)));
        BooleanIndexing.replaceWhere(logLower, -Math.pow(10,16), Conditions.lessThan(-Math.pow(10,16)));
        BooleanIndexing.replaceWhere(logUpper, -Math.pow(10,16), Conditions.lessThan(-Math.pow(10,16)));
        INDArray lower = exp(logLower.subi((logLower.maxNumber()))); //
        INDArray upper = exp(logUpper.subi((logUpper.maxNumber()))); //
        BooleanIndexing.replaceWhere(lower, 0, Conditions.isNan());
        BooleanIndexing.replaceWhere(upper, 0, Conditions.isNan());
        BooleanIndexing.replaceWhere(lower, Math.pow(10,16), Conditions.isInfinite());
        BooleanIndexing.replaceWhere(upper, Math.pow(10,16), Conditions.isInfinite());
        BooleanIndexing.replaceWhere(lower, Math.pow(10,16), Conditions.greaterThan(Math.pow(10,16)));
        BooleanIndexing.replaceWhere(upper, Math.pow(10,16), Conditions.greaterThan(Math.pow(10,16)));
        Integer nDim = lower.shape().length;
        int[] numbers = new int[nDim - 1];
        for(int i = 1; i < nDim; i++){
            numbers[i - 1] = i;
        }
        Double fullLower = null;
        Double fullUpper = null;
        String[] pathSplits = path.split("/");
        FileWriter mathOut = null;
        BufferedWriter bw;
        PrintWriter pw = null;
        try {
            mathOut = new FileWriter(path + "/" + pathSplits[pathSplits.length - 1] + ".m", true);
            bw = new BufferedWriter(mathOut);
            pw = new PrintWriter(bw);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Numerical Integration for Equi-Prob Intervals
        // for (Pair<Integer, INDArray> pv: paramValues.values()) {
        //     Integer dim = pv.getKey();
        //     if (dim == null || dim <= 0) {
        //         continue;
        //     }
        //     INDArray vv = pv.getValue();
        //     INDArray vvdiff = vv.dup();
        //     vvdiff.getScalar(0).assign(0);
        //     for (int kk=1;kk<vv.length();kk++) {
        //         vvdiff.getScalar(kk).subi(vv.getScalar(kk-1));
        //     }
        //     BooleanIndexing.replaceWhere(vvdiff, 0.01, Conditions.greaterThan(1.5));
        //     lower.muli(vvdiff.reshape(IntervalAnalysis.getReshape(vvdiff.shape(),
        //             lower.shape())).broadcast(lower.shape()));
        //     upper.muli(vvdiff.reshape(IntervalAnalysis.getReshape(vvdiff.shape(),
        //             lower.shape())).broadcast(lower.shape()));
        // }
        int j = 0;
        for (String ss: strings) {
            j++;
            // System.out.println(ss);
            if (ss.contains("robust_")) {
                continue;
            }
            int[] numbersCopy = numbers.clone();
            Pair<Integer, INDArray> paramIdxValues = paramValues.get(ss);
            if (paramIdxValues == null)
                continue;
            if (paramIdxValues.getKey() == null)
                continue;
            Integer paramDimIdx = paramIdxValues.getKey();
            numbersCopy[paramDimIdx - 1] = 0;
            INDArray paramvalue = paramIdxValues.getValue();
            if (paramvalue.shape().length > 1) {
                paramvalue = paramvalue.tensorAlongDimension(0,paramDimIdx);
            }
            INDArray currsumLower = lower.sum(numbersCopy);
            INDArray currsumUpper = upper.sum(numbersCopy);
            if (fullLower == null) {
                fullLower = currsumLower.sumNumber().doubleValue();
                fullUpper = currsumUpper.sumNumber().doubleValue();
            }
            INDArray outMatrix = Nd4j.vstack(Nd4j.toFlattened(paramvalue),
                    Nd4j.toFlattened(currsumLower).div(fullLower),
                    Nd4j.toFlattened(currsumUpper).div(fullUpper));
            String outputFile = path + "/analysis_" + ss + ".txt";
            File file = new File(outputFile);
            if (!file.exists()) {
                Nd4j.writeTxt(outMatrix, outputFile);
                pw.println(String.format("txt%s=Import[\"%s\"];", j, "./analysis_" + ss + ".txt"));
                pw.println(String.format("data%s = getToData[txt%s][[4]];", j, j));
                pw.println(String.format("Graphics[{Orange, Table[Rectangle @@ nn, {nn, pairNewRect[data%s]}]}, \n" +
                        " Axes -> True, ImageSize -> Small, PlotRange -> {Automatic, All}, \n" +
                        " AspectRatio -> 1, PlotLabel -> \"%s\"] ", j, ss));
                pw.println(String.format("Graphics[{Orange, \n" +
                        "  Table[Rectangle @@ nn, {nn, pairNewRectCDF[data%s]}]}, Axes -> True, \n" +
                        " ImageSize -> Small, PlotRange -> {Automatic, All}, AspectRatio -> 1, \n" +
                        " PlotLabel -> \"%s\"]", j, ss));
                pw.flush();
            }
            else {
                INDArray lastOut = null;
                try {
                    lastOut = Nd4j.readTxt(outputFile);
                    lastOut.slice(1).mul(outMatrix.slice(1));
                    lastOut.slice(2).mul(outMatrix.slice(2));
                    Nd4j.writeTxt(lastOut,outputFile);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
            // LinkedList<Integer> restDims = numbers.remove(paramDimIdx);
            // System.out.println(intervalState.probCube.get(0));
            // Nd4j.writeTxt(outputTable, "./analysis_" + ss + ".txt");
        }
        try {
            mathOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void deleteAnalysisOutputs(String path) {
        File dir = new File(path);
        File fileList[] = dir.listFiles();
        for (File file: fileList) {
            String fileName = file.getName();
            if (fileName.endsWith(".txt") && fileName.startsWith("analysis"))
                file.delete();

        }
        String helperFunc = "SetDirectory[NotebookDirectory[]];\n" +
                "getToData[txt_] := \n" +
                " ToExpression[\n" +
                "  StringReplace[\n" +
                "   txt, {\"[\" -> \"{\", \"]\" -> \"}\", \"{\" -> \"<|\", \"}\" -> \"|>\", \n" +
                "    \":\" -> \"->\", \"E\" -> \"*10^\"}]]\n" +
                "pairNewRect[data_] := (Table[\n" +
                "   {{tt[[1]], tt[[2]]}, {tt[[3]], tt[[4]]}},\n" +
                "   {tt, Delete[\n" +
                "     Transpose[{data[[1]], data[[2]], RotateLeft[data[[1]]], \n" +
                "       RotateLeft[data[[3]]]}], -1]}])\n" +
                "pairNewRectCDF[data_] := (Table[\n" +
                "   {{tt[[1]], tt[[2]]}, {tt[[3]], tt[[4]]}},\n" +
                "   {tt, Delete[\n" +
                "     Transpose[{data[[1]], Accumulate[data[[2]]], \n" +
                "       RotateLeft[data[[1]]], \n" +
                "       RotateLeft[Accumulate[data[[3]]]]}], -1]}])\n";
        String[] pathSplits = path.split("/");
        try {
            FileWriter mathOut = new FileWriter(path + "/" + pathSplits[pathSplits.length - 1] + ".m");
            mathOut.write(helperFunc);
            mathOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    public void addProb(INDArray likeProbLower, INDArray likeProbUpper) {
        // System.out.println("Add Prob");
        INDArray lower = probCube[0];
        INDArray upper = probCube[1];
        BooleanIndexing.replaceWhere(likeProbLower, -Math.pow(1,16), Conditions.isNan());
        BooleanIndexing.replaceWhere(likeProbUpper, -Math.pow(1,16), Conditions.isNan());
        BooleanIndexing.replaceWhere(likeProbLower, -Math.pow(1,16), Conditions.isInfinite());
        BooleanIndexing.replaceWhere(likeProbUpper, -Math.pow(1,16), Conditions.isInfinite());
        BooleanIndexing.replaceWhere(lower, -Math.pow(1,16), Conditions.isInfinite());
        BooleanIndexing.replaceWhere(upper, -Math.pow(1,16), Conditions.isInfinite());
        // System.out.println(Nd4j.createFromArray(lower.shape()));
        // System.out.println(Nd4j.createFromArray(likeProbLower.shape()));
        INDArray newLower = lower.add(likeProbLower.broadcast(lower.shape())); // mul
        INDArray newUpper = upper.add(likeProbUpper.broadcast(lower.shape())); // mul
        // System.out.println("old lower:" + lower);
        // System.out.println("likelihood:" + likeProbLower);
        // System.out.println("lower adds likelihood:" + newLower);
        probCube[0] = newLower;
        probCube[1] = newUpper;
    }

    public INDArray getParamCube(String paramName) {
        Pair<Integer, INDArray> paramValuePair = paramValues.get(paramName);
        if (paramValuePair.getValue().length() != 0) {
            long[] singleDim = new long[dimSize.size()];
            Arrays.fill(singleDim, 1);
            long[] realShapes = paramValuePair.getValue().shape();
            System.arraycopy(realShapes, 0, singleDim, 0, realShapes.length);
            return paramValuePair.getValue().reshape(singleDim);
        }
        else {
            return paramValuePair.getValue();
        }

    }

    public long[] getNewDim(long dataLength) {
        long[] singleDim = new long[dimSize.size() + 1];
        Arrays.fill(singleDim, 1);
        singleDim[dimSize.size()] = dataLength;
        return singleDim;
    }

    public void addDataDim(long dataLength) {
        // dimSize.set(0,dataLength);
        paramValues.put("Datai", new Pair<>(0, Nd4j.arange(dataLength)));
    }

    @Deprecated
    public void addIndParam(String paramName, INDArray prob, INDArray lower, INDArray upper) {
        if (intervalProbPairs != null) {
            Integer paramIdx = (int) intervalProbPairs.shape()[1]; // idx for lower
            paramMap.put(paramName, paramIdx);
            int pairLength = (int) intervalProbPairs.shape()[0];
            int newProbLength = (int) prob.shape()[0];
            INDArray repProb = Nd4j.toFlattened(Nd4j.repeat(prob, pairLength));
            INDArray repLower = Nd4j.toFlattened(Nd4j.repeat(lower, pairLength));
            INDArray repUpper = Nd4j.toFlattened(Nd4j.repeat(upper, pairLength));
            intervalProbPairs = intervalProbPairs.repeat(0,newProbLength);
            intervalProbPairs.getColumn(0).assign(intervalProbPairs.getColumn(0).mul(repProb));
            intervalProbPairs = Nd4j.hstack(intervalProbPairs,
                    repLower.reshape(pairLength * newProbLength, 1),
                    repUpper.reshape(pairLength * newProbLength, 1));

        } else {
            paramMap.put(paramName, 1);
            int newProbLength = (int) prob.shape()[0];
            intervalProbPairs = Nd4j.hstack(prob.reshape(newProbLength, 1),
                    lower.reshape(newProbLength, 1),
                    upper.reshape(newProbLength, 1));
        }
    }

    @Deprecated
    public INDArray[] getParam(String paramName) {
        int paramIdx = paramMap.get(paramName);
        return new INDArray[]{intervalProbPairs.getColumn(paramIdx), intervalProbPairs.getColumn(paramIdx + 1)};
    }

    @Deprecated
    public int getParamIdx(String paramName) {
        return (paramMap.get(paramName) - 1)/2;
    }

    @Override
    public void printAbsState() {
        // for (Pair intervalProbPair: IntervalProbPairs) {
        //     intervalProbPair.getKey()
        // }
        if (paramValues.size() > 0) {
            System.out.println(paramValues.keySet());
        }
        else
            System.out.println("no param values");
        // System.out.println(intervalProbPairs.toString());
        // System.out.println(String.format("%d,%d",intervalProbPairs.shape()[0], intervalProbPairs.shape()[1]));
    }

}
