package grammar.analyses;

import com.google.common.primitives.Ints;
import grammar.AST;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.ops.impl.transforms.strict.Log;
import org.nd4j.linalg.api.shape.Shape;
import org.nd4j.linalg.cpu.nativecpu.NDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.BooleanIndexing;
import org.nd4j.linalg.indexing.conditions.Condition;
import org.nd4j.linalg.indexing.conditions.Conditions;
import org.nd4j.linalg.ops.transforms.Transforms;
import sun.awt.image.ImageWatched;

import java.util.*;

import static org.nd4j.linalg.ops.transforms.Transforms.exp;
import static org.nd4j.linalg.ops.transforms.Transforms.log;


public class IntervalState extends AbstractState{
    // deprecated
    public Map<String, Integer> paramMap = new HashMap<>();// idx for lower, upper idx is lower + 1
    public INDArray intervalProbPairs = null; // each row has prob, param1_l, param1_u, param2...

    // in use
    public List<Long> dimSize = new ArrayList<>(); // length of each dim, shape of probCube
    public List<INDArray> probCube = new ArrayList<>(); // Stores log prob
    public Map<String, Pair<Integer, INDArray>> paramValues = new HashMap<>(); // dim idx and values of params

    IntervalState() {
        dimSize.add((long) 1);
    }

    public void addDepParamCube(String paramName, INDArray splits) {
        paramValues.put(paramName, new Pair<>(null,splits));
    }

    public void addParamCube(String paramName, INDArray splits, INDArray probLower, INDArray probUpper) {
        int currDim = dimSize.size();
        paramValues.put(paramName, new Pair<>(currDim, splits));
        long newSplitLen = splits.shape()[splits.shape().length - 1];
        if (currDim == 1) {
            dimSize.add(newSplitLen);
            probCube.add(probLower.reshape(1,newSplitLen));
            probCube.add(probUpper.reshape(1, newSplitLen));
        }
        else {
            INDArray oldProbLower = probCube.get(0);
            INDArray oldProbUpper = probCube.get(1);
            dimSize.add((long) 1);
            long[] singleDim = new long[dimSize.size()];
            Arrays.fill(singleDim, 1);
            long[] realShapes = splits.shape();
            System.arraycopy(realShapes, 0, singleDim, 0, realShapes.length);
            singleDim[dimSize.size() - 1] = newSplitLen;
            // dimSize: 2,3,4,1  singleDim: 1,3,1,5
            int[] oldDimSize = Ints.toArray(dimSize);
            dimSize.set(dimSize.size() - 1, newSplitLen);
            long[] outComeDimSize = dimSize.stream().mapToLong(i -> i).toArray();
            probCube.set(0,oldProbLower.reshape(oldDimSize).broadcast(outComeDimSize).mul(probLower.reshape(singleDim).broadcast(outComeDimSize)));
            probCube.set(1,oldProbUpper.reshape(oldDimSize).broadcast(outComeDimSize).mul(probUpper.reshape(singleDim).broadcast(outComeDimSize)));
        }
    }

    public void writeResults(Set<String> strings) {
        INDArray lower = probCube.get(0);
        INDArray upper = probCube.get(1);
        Integer nDim = lower.shape().length;
        int[] numbers = new int[nDim - 1];
        for(int i = 1; i < nDim; i++){
            numbers[i - 1] = i;
        }
        Double fullLower = null;
        Double fullUpper = null;
        System.out.println(lower);
        for (String ss: strings) {
            int[] numbersCopy = numbers.clone();
            Pair<Integer, INDArray> paramIdxValues = paramValues.get(ss);
            Integer paramDimIdx = paramIdxValues.getKey();
            numbersCopy[paramDimIdx - 1] = 0;
            INDArray paramvalue = paramIdxValues.getValue();
            INDArray currsumLower = lower.sum(numbersCopy);
            INDArray currsumUpper = upper.sum(numbersCopy);
            if (fullLower == null) {
                fullLower = currsumLower.sumNumber().doubleValue();
                fullUpper = currsumUpper.sumNumber().doubleValue();
                System.out.println(fullLower);
            }
            System.out.println(ss);
            System.out.println(Nd4j.vstack(Nd4j.toFlattened(paramvalue), Nd4j.toFlattened(currsumLower).div(fullLower), Nd4j.toFlattened(currsumUpper).div(fullUpper)));
            // LinkedList<Integer> restDims = numbers.remove(paramDimIdx);
            // System.out.println(intervalState.probCube.get(0));
            // Nd4j.writeTxt(outputTable, "./analysis_" + ss + ".txt");
        }
    }

    public void addProb(INDArray likeProbLower, INDArray likeProbUpper) {
        // INDArray lower = probCube.get(0);
        // INDArray upper = probCube.get(1);
        BooleanIndexing.replaceWhere(likeProbLower, 0, Conditions.isNan());
        BooleanIndexing.replaceWhere(likeProbUpper, 0, Conditions.isNan());
        INDArray newLower = probCube.get(0).mul(likeProbLower);
        INDArray newUpper = probCube.get(1).mul(likeProbUpper);
        probCube.set(0, newLower);
        probCube.set(1, newUpper);
    }

    public INDArray getParamCube(String paramName) {
        Pair<Integer, INDArray> paramValuePair = paramValues.get(paramName);
        long[] singleDim = new long[dimSize.size()];
        Arrays.fill(singleDim, 1);
        long[] realShapes = paramValuePair.getValue().shape();
        System.arraycopy(realShapes, 0, singleDim, 0, realShapes.length);
        return paramValuePair.getValue().reshape(singleDim);
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

    // deprecated
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

    public INDArray[] getParam(String paramName) {
        int paramIdx = paramMap.get(paramName);
        return new INDArray[]{intervalProbPairs.getColumn(paramIdx), intervalProbPairs.getColumn(paramIdx + 1)};
    }

    public int getParamIdx(String paramName) {
        return (paramMap.get(paramName) - 1)/2;
    }

    @Override
    public void printAbsState() {
        // for (Pair intervalProbPair: IntervalProbPairs) {
        //     intervalProbPair.getKey()
        // }
        System.out.println(paramValues);
        // System.out.println(intervalProbPairs.toString());
        // System.out.println(String.format("%d,%d",intervalProbPairs.shape()[0], intervalProbPairs.shape()[1]));
    }
}
