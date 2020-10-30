package grammar.analyses;

import com.google.common.primitives.Ints;
import grammar.AST;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.shape.Shape;
import org.nd4j.linalg.cpu.nativecpu.NDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.util.*;


public class IntervalState extends AbstractState{
    public Map<String, Integer> paramMap = new HashMap<>();// idx for lower, upper idx is lower + 1
    public INDArray intervalProbPairs = null; // each row has prob, param1_l, param1_u, param2...
    public List<Integer> dimSize = new ArrayList<>(); // length of each dim, shape of probCube
    public List<INDArray> probCube = new ArrayList<>();
    public Map<String, Pair<Integer, INDArray>> paramValues = new HashMap<>(); // dim idx and values of params

    public void addParamCube(String paramName, INDArray splits, INDArray probLower, INDArray probUpper) {
        int currDim = dimSize.size();
        paramValues.put(paramName, new Pair<>(currDim, splits));
        int newSplitLen = (int) splits.shape()[0];
        if (currDim == 0) {
            dimSize.add(newSplitLen);
            probCube.add(probLower);
            probCube.add(probUpper);
        }
        else {
            INDArray oldProbLower = probCube.get(0);
            INDArray oldProbUpper = probCube.get(1);
            dimSize.add(1);
            int[] singleDim = new int[dimSize.size()];
            Arrays.fill(singleDim, 1);
            singleDim[dimSize.size() - 1] = newSplitLen;
            // dimSize: 2,3,4,1  singleDim: 1,1,1,5
            int[] oldDimSize = Ints.toArray(dimSize);
            dimSize.set(dimSize.size() - 1, newSplitLen);
            INDArray outComeDimSize = Nd4j.create(dimSize);
            probCube.add(oldProbLower.reshape(oldDimSize).broadcast(outComeDimSize).mul(probLower.reshape(singleDim)).broadcast(outComeDimSize));
            probCube.add(oldProbUpper.reshape(oldDimSize).broadcast(outComeDimSize).mul(probUpper.reshape(singleDim)).broadcast(outComeDimSize));
        }
    }

    public INDArray getParamCube(String paramName) {
        Pair<Integer, INDArray> paramValuePair = paramValues.get(paramName);
        int[] singleDim = new int[dimSize.size()];
        Arrays.fill(singleDim, 1);
        int paramDim = paramValuePair.getKey();
        singleDim[paramDim] = dimSize.get(paramDim);
        return paramValuePair.getValue().reshape(singleDim);
    }

    public int[] getDim4Data(int dataLength) {
        int[] singleDim = new int[dimSize.size() + 1];
        Arrays.fill(singleDim, 1);
        singleDim[dimSize.size()] = dataLength;
        return singleDim;
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
        System.out.println(paramMap);
        // System.out.println(intervalProbPairs.toString());
        // System.out.println(String.format("%d,%d",intervalProbPairs.shape()[0], intervalProbPairs.shape()[1]));
    }
}
