package grammar.analyses;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.cpu.nativecpu.NDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class IntervalState extends AbstractState{
    public Map<String, Integer> paramMap = new HashMap<>();// idx for lower, upper idx is lower + 1
    public INDArray intervalProbPairs = null; // each row has prob, param1_l, param1_u, param2...

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
