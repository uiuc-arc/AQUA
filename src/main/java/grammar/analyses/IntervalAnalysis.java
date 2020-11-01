package grammar.analyses;

import grammar.AST;
import grammar.cfg.*;
import grammar.cfg.BasicBlock;
import jdk.internal.org.objectweb.asm.Type;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.*;
import org.apache.commons.math3.distribution.AbstractRealDistribution;
import org.apache.commons.math3.distribution.UniformRealDistribution;
import org.nd4j.linalg.api.buffer.DataBuffer;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.cpu.nativecpu.NDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.ops.transforms.Transforms;
import utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static java.lang.Math.*;
import static org.nd4j.linalg.ops.transforms.Transforms.exp;
import static org.nd4j.linalg.ops.transforms.Transforms.sigmoid;


public class IntervalAnalysis {
    private Map<String, Pair<Double[], ArrayList<Integer>>> paramMap = new HashMap<>();
    private Map<String, Pair<AST.Data, String>> dataList = new HashMap<>();
    private Map<String, Double> scalarParam = new HashMap<>();
    private int piCounts = 1000;
    private double pi = 0.001;


    public void forwardAnalysis(ArrayList<Section> cfgSections) {
        Nd4j.setDataType(DataType.DOUBLE);

        for (Section section : cfgSections) {
            System.out.println(section.sectionType);
            if (section.sectionType == SectionType.DATA) {
                ArrayList<AST.Data> dataSets = section.basicBlocks.get(0).getData();
                for (AST.Data dd: dataSets) {
                    String dataString = Utils.parseData(dd, 'f');
                    dataString = dataString.replaceAll("\\s", "").replaceAll("\\[", "").replaceAll("\\]", "").replaceAll("\\.0,",",").replaceAll("\\.0$","");
                    dataList.put(dd.decl.id.id, new Pair(dd, dataString));
                }

            } else if(section.sectionType == SectionType.FUNCTION) {
                System.out.println(section.sectionName);
                if (section.sectionName.equals("main")) {
                    for (BasicBlock basicBlock : section.basicBlocks) {
                        BlockAnalysisCube(basicBlock);
                        // BooleanIndexing.replaceWhere(params[parami],Double.MAX_VALUE, Conditions.isInfinite());
                        basicBlock.dataflowFacts.writeResults(paramMap.keySet());
                    }
                }

            } else if(section.sectionType == SectionType.QUERIES) {
                for (BasicBlock basicBlock: section.basicBlocks)
                    for (Statement statement : basicBlock.getStatements())
                        System.out.println(statement.statement.toString());

            }

        }
    }

    private void BlockAnalysisCube(BasicBlock basicBlock) {
        IntervalState intervalState = new IntervalState();
        for (Statement statement : basicBlock.getStatements()) {
            if (statement.statement instanceof AST.Decl) {
                System.out.println("Decl: " + statement.statement.toString());
                ArrayList<AST.Annotation> annotations = statement.statement.annotations;
                addParams(statement);
                System.out.println(statement.statement.toString());

            } else if (statement.statement instanceof AST.AssignmentStatement) {
                ArrayList<AST.Annotation> annotations = statement.statement.annotations;
                AST.AssignmentStatement assignment = (AST.AssignmentStatement) statement.statement;
                if (annotations != null && !annotations.isEmpty() &&
                        annotations.get(0).annotationType == AST.AnnotationType.Observe) {
                    System.out.println("Observe (assign): " + statement.statement.toString());
                    String dataYID = assignment.lhs.toString();
                    Pair<AST.Data, String> yDataPair = dataList.get(dataYID.split("\\[")[0]);
                    String yValue = yDataPair.getValue();
                    double[] yArray = Arrays.stream(yValue.split(",")).mapToDouble(Double::parseDouble).toArray();
                    if (!dataYID.contains("[")) {
                        ObsDistrCube(yArray, assignment, intervalState);
                    }
                    else {
                        INDArray lhs = DistrCube(assignment.lhs, intervalState);
                    }
                } else {
                    System.out.println("Assignment: " + statement.statement.toString());
                    String paramID = assignment.lhs.toString().split("\\[")[0];
                    if (paramMap.containsKey(paramID)) {
                        Pair<Double[], ArrayList<Integer>> paramInfo = paramMap.get(paramID);
                        Double[] paramLimits = paramInfo.getKey();
                        ArrayList<Integer> paramDims = paramInfo.getValue();
                        if (assignment.rhs instanceof AST.FunctionCall
                                && isFuncConst(assignment.rhs)) { // completely independent new param
                            System.out.println("Const param");
                            INDArray rhs[] = IndDistrSingle(assignment.rhs, paramLimits); // split, probLower, probUpper
                            for (int ii=0; ii < rhs.length; ii++) {
                                rhs[ii] = rhs[ii].reshape(intervalState.getNewDim(rhs[ii].shape()[0]));
                            }
                            // TODO: fix dim if rhs contains data
                            // TODO: fix usage of single Id without dim
                            if (paramDims.size() == 1) {
                                for (Integer jj = 1; jj <= paramDims.get(0); jj++) {
                                    intervalState.addParamCube(String.format("%s[%s]", paramID, jj), rhs[0], rhs[1], rhs[2]);
                                }
                            } else if (paramDims.size() == 2) {
                                for (Integer jj = 1; jj <= paramDims.get(0); jj++) {
                                    for (Integer kk = 1; kk <= paramDims.get(1); kk++)
                                        intervalState.addParamCube(String.format("%s[%s,%s]", paramID, jj, kk), rhs[0], rhs[1], rhs[2]);
                                }
                            } else if (paramDims.size() == 0)
                                intervalState.addParamCube(paramID, rhs[0], rhs[1], rhs[2]);
                            intervalState.printAbsState();
                        }
                        else { // transformed parameters, completely dependent on other params
                            if (!(assignment.rhs instanceof AST.FunctionCall)) { // TODO: cond: not a distr
                                INDArray rhs = DistrCube(assignment.rhs, intervalState);
                                intervalState.addDepParamCube(assignment.lhs.toString(), rhs); // with dim null
                            }
                            else {
                                // add hierarchical param
                                HierDistrCube(assignment, intervalState);
                            }
                        }
                    }
                }
            } else if (statement.statement instanceof AST.FunctionCallStatement) {
                System.out.println("FunctionCall: " + statement.statement.toString());

            } else if (statement.statement instanceof AST.IfStmt) {
                AST.IfStmt ifStmt = (AST.IfStmt) statement.statement;
                // BlockAnalysis(ifStmt.BBtrueBlock);
                // BlockAnalysis(ifStmt.BBelseBlock);
            } else if (statement.statement instanceof AST.ForLoop) {
                AST.ForLoop forLoop = (AST.ForLoop) statement.statement;
                System.out.println("ForLoop: "+ statement.statement);
                // BlockAnalysis(forLoop.BBloopBody);
            }
            basicBlock.dataflowFacts = intervalState;
        }
    }

    private void HierDistrCube(AST.AssignmentStatement assignment, IntervalState intervalState) {
        AST.FunctionCall distrExpr = (AST.FunctionCall) assignment.rhs;
        INDArray[] params = new NDArray[distrExpr.parameters.size()];
        int parami = 0;
        for (AST.Expression pp: distrExpr.parameters) {
            if (pp instanceof AST.Integer) {
                params[parami] = Nd4j.createFromArray(((AST.Integer) pp).value);

            }
            else if (pp instanceof AST.Double) {
                params[parami] = Nd4j.createFromArray(((AST.Double) pp).value);
            }
            else {
                params[parami] = DistrCube(pp, intervalState);
            }
            parami++;
        }
        if (params.length == 1) {
            assert(!(params[0].shape().length == 1 && params[0].shape()[0] == 1)); // o.w const distr
            long[] retshape = new long[params[0].shape().length + 1];
            System.arraycopy(params[0].shape(), 0, retshape, 0, params[0].shape().length);
            retshape[params[0].shape().length] = piCounts;
            // TODO:
        }
        else if (params.length == 2) {
            INDArray[] singleprob = expand1D(params[0], params[1], "normal");
            intervalState.addParamCube(assignment.lhs.toString(), singleprob[0], singleprob[1], singleprob[2]);
            // TODO: support other dists
        }
    }

    private INDArray[] expand1D(INDArray input1, INDArray input2, String distr) {
        long[] shape1 = input1.shape();
        long[] shape2 = input2.shape();
        int maxDimCount = max(shape1.length,shape2.length);
        long[] shape1b = new long[maxDimCount + 1];
        long[] shape2b = new long[maxDimCount + 1];
        Arrays.fill(shape1b, 1);
        Arrays.fill(shape2b, 1);
        System.arraycopy(shape1, 0, shape1b, 0, shape1b.length);
        System.arraycopy(shape2, 0, shape2b, 0, shape2b.length);
        long[] broadcastDims = new long[maxDimCount + 1];
        for (int i=0; i < maxDimCount + 1; i++) {
            broadcastDims[i] = max(shape1[i], shape2[i]);
        }
        broadcastDims[maxDimCount] = piCounts;
        INDArray input1b = input1.broadcast(shape1b);
        INDArray input2b = input2.broadcast(shape2b);
        INDArray single = Nd4j.createUninitialized(broadcastDims);
        INDArray prob1 = Nd4j.createUninitialized(broadcastDims);
        INDArray prob2 = Nd4j.createUninitialized(broadcastDims);
        for (int i=0; i < input1b.length(); i++) {
            double mean = input1b.getDouble(i);
            double sd = input2b.getDouble(i);
            // if (distrName.equals("normal")) {
            double[] singlej = new double[piCounts];
            double[] prob1j = new double[piCounts];
            double[] prob2j = new double[piCounts];
            NormalDistribution normal = new NormalDistributionImpl(mean, sd);
            getDiscretePriorsSingle(singlej, prob1j, prob2j, normal);
            // }
            prob1.tensorAlongDimension(i, maxDimCount).assign(Nd4j.createFromArray(prob1j));
            prob2.tensorAlongDimension(i, maxDimCount).assign(Nd4j.createFromArray(prob2j));
            single.tensorAlongDimension(i, maxDimCount).assign(Nd4j.createFromArray(singlej));
        }
        return new INDArray[]{single, prob1, prob2};
    }

    private void ObsDistrCube(double[] yArray, AST.AssignmentStatement assignment, IntervalState intervalState) {
        int yLength = yArray.length;
        AST.FunctionCall distrExpr = (AST.FunctionCall) assignment.rhs;
        INDArray[] params = new NDArray[distrExpr.parameters.size()];
        int parami = 0;
        for (AST.Expression pp: distrExpr.parameters) {
            if (pp instanceof AST.Integer) {
                params[parami] = Nd4j.createFromArray(((AST.Integer) pp).value);

            }
            else if (pp instanceof AST.Double) {
                params[parami] = Nd4j.createFromArray(((AST.Double) pp).value);
            }
            else {
                params[parami] = DistrCube(pp, intervalState);
            }
            parami++;
        }
        if (distrExpr.id.id.equals("normal")) {
            long[] shape1 = params[0].shape();
            long[] shape2 = params[1].shape();
            INDArray yNDArray = Nd4j.createFromArray(yArray);
            long[] maxShape = getMaxShape(shape1, shape2);
            maxShape = getMaxShape(maxShape, yNDArray.shape());
            params[0] = params[0].reshape(getReshape(shape1, maxShape)).broadcast(maxShape);
            params[1] = params[1].reshape(getReshape(shape2, maxShape)).broadcast(maxShape);
            yNDArray = yNDArray.reshape(getReshape(yNDArray.shape(), maxShape)).broadcast(maxShape);
            INDArray likeCube = Nd4j.createUninitialized(maxShape);
            for (long ii=0; ii<likeCube.length(); ii++) {
                double sd = params[1].getDouble(ii);
                sd = sd != 0? sd: 0.00000000001;
                NormalDistributionImpl normal = new NormalDistributionImpl(params[0].getDouble(ii), sd);
                likeCube.putScalar(ii, log(normal.density(yNDArray.getDouble(ii))));
            }
            INDArray sumExp = exp(likeCube.sum(0));
            intervalState.addProb(sumExp, sumExp);
        }
    }

    private INDArray DistrCube(AST.Expression pp, IntervalState intervalState) {
        if (pp instanceof AST.Id) {
            return DistrCube((AST.Id) pp, intervalState);
        }
        else if (pp instanceof AST.AddOp) {
            return DistrCube((AST.AddOp) pp, intervalState);

        }
        else if (pp instanceof AST.MulOp) {
            return DistrCube((AST.MulOp) pp, intervalState);

        }
        else if (pp instanceof AST.Braces) {
            return DistrCube((AST.Braces) pp, intervalState);

        }
        else if (pp instanceof AST.ArrayAccess) {
            return DistrCube((AST.ArrayAccess) pp, intervalState);
        }
        else if (pp instanceof AST.Integer) {
            return DistrCube((AST.Integer) pp, intervalState);
        }
        else if (pp instanceof AST.Double) {
            return DistrCube((AST.Double) pp, intervalState);
        }
        return null;
    }


    private INDArray DistrCube(AST.Integer pp, IntervalState intervalState) {
        return Nd4j.createFromArray(pp.value);
    }

    private INDArray DistrCube(AST.Double pp, IntervalState intervalState) {
        return Nd4j.createFromArray(pp.value);
    }

    private INDArray DistrCube(AST.Id pp, IntervalState intervalState) {
        System.out.println("Distr ID=================");

        if (dataList.containsKey(pp.id)) {
            Pair<AST.Data, String> xDataPair = dataList.get(pp.id);
            String xValue = xDataPair.getValue();
            double[] xArray = Arrays.stream(xValue.split(",")).mapToDouble(Double::parseDouble).toArray();
            if (! intervalState.paramValues.containsKey("Datai")) {
                long[] dataDim = new long[intervalState.dimSize.size()];
                Arrays.fill(dataDim, 1);
                dataDim[0] = xArray.length;
                INDArray retData = Nd4j.createFromArray(xArray).reshape(dataDim);
                intervalState.addDataDim(xArray.length);
                return retData;
            } else { // already data in datai
                Pair<Integer, INDArray> arrayPair = intervalState.paramValues.get("Datai");
                assert(arrayPair.getValue().length() == xArray.length);
                int dataDimIdx = arrayPair.getKey();
                long[] dataDim = new long[intervalState.dimSize.size()];
                Arrays.fill(dataDim, 1);
                dataDim[dataDimIdx] = xArray.length;
                return Nd4j.createFromArray(xArray).reshape(dataDim);
            }
        }
        else if (intervalState.paramValues.containsKey(pp.id)){
            return intervalState.getParamCube(pp.id);
        }
        else {// uninitialized param on RHS,
            String paramName = pp.toString();
            addUninitParam(intervalState, paramName);
            return intervalState.getParamCube(paramName);
        }
    }


    private INDArray DistrCube(AST.ArrayAccess pp, IntervalState intervalState) {
        System.out.println("Distr ArrayAccess==================");
        // if (dataList.containsKey(pp.id)) {

        // }
        // else {
        if (intervalState.paramValues.containsKey(pp.toString()))
            return intervalState.getParamCube(pp.toString());
        else if (dataList.containsKey(pp.id.id)) { // is Data
            ArrayList<Integer> dims = new ArrayList<>();
            for (AST.Expression dd : pp.dims.dims)
                getConstN(dims, dd);
            Pair<AST.Data, String> xDataPair = dataList.get(pp.id);
            String xValue = xDataPair.getValue();
            double[] xArray = Arrays.stream(xValue.split(",")).mapToDouble(Double::parseDouble).toArray();
            double dataElement = xArray[dims.get(0)]; // TODO: support 2D array access
            return Nd4j.createFromArray(dataElement); //.reshape(intervalState.getNewDim(1));
        }
        else { // id is param. uninitialized param on RHS,
            // TODO: support 2D array access
            if (pp.dims.toString().matches("\\d+")) {
                String paramName = pp.toString();
                addUninitParam(intervalState, paramName);
                return intervalState.getParamCube(paramName);
            }
            else { // other expr in dims
                INDArray dimConst = DistrCube(pp.dims.dims.get(0), intervalState);
                int dim0 = dimConst.getInt(0);
                String paramName = pp.id.id + "[" + dim0 + "]";
                if (intervalState.paramValues.containsKey(paramName))
                    return intervalState.getParamCube(paramName);
                else {
                    addUninitParam(intervalState, paramName);
                    return intervalState.getParamCube(paramName);
                }
            }
        }
    }

    private void addUninitParam(IntervalState intervalState, String paramName) {
        Pair<Double[], ArrayList<Integer>> LimitsDim = paramMap.get(paramName);
        Double[] limits = LimitsDim.getKey();
        double[] probLower = new double[piCounts];
        double[] probUpper = new double[piCounts];
        double[] single = new double[piCounts];
        if (limits[0] != null && limits[1] != null) {
            UniformRealDistribution unif = new UniformRealDistribution(limits[0], limits[1]);
            getDiscretePriorsSingleUnif(single, probLower, probUpper, unif);
        }
        else if (limits[0] != null) {
            if (limits[0] == 0) {
                GammaDistribution gamma = new GammaDistributionImpl(1, 1);
                getDiscretePriorsSingle(single, probLower, probUpper, gamma);
            }
            else{
                UniformRealDistribution unif = new UniformRealDistribution(limits[0], limits[0] + 5);
                getDiscretePriorsSingleUnif(single, probLower, probUpper, unif);
            }
        }
        else if (limits[2] != null) {
            NormalDistribution normal = new NormalDistributionImpl(limits[2], 1);
            getDiscretePriorsSingle(single, probLower, probUpper, normal);
        }
        else { // all are null
            System.out.println("Prior: Normal");
            NormalDistribution normal = new NormalDistributionImpl(0, 1);
            getDiscretePriorsSingle(single, probLower, probUpper, normal);
        }
        intervalState.addParamCube(paramName, Nd4j.createFromArray(single).reshape(intervalState.getNewDim(piCounts)),
                Nd4j.createFromArray(probLower).reshape(intervalState.getNewDim(piCounts)),
                Nd4j.createFromArray(probUpper).reshape(intervalState.getNewDim(piCounts)));
    }




    private INDArray DistrCube(AST.Braces pp, IntervalState intervalState) {
        System.out.println("Distr Brace==================");
        return DistrCube(pp.expression, intervalState);
    }

    private INDArray DistrCube(AST.AddOp pp, IntervalState intervalState) {
        System.out.println("Distr Add==================");
        INDArray op1Array = DistrCube(pp.op1, intervalState);
        INDArray op2Array = DistrCube(pp.op2, intervalState);
        long[] op1shape = op1Array.shape();
        long[] op2shape = op2Array.shape();
        long[] outShape = getMaxShape(op1shape, op2shape);
        if (outShape.length > op1shape.length)
            op1Array = op1Array.reshape(getReshape(op1shape, outShape));
        else
            op2Array = op2Array.reshape(getReshape(op2shape, outShape));
        return op1Array.broadcast(outShape).add(op2Array.broadcast(outShape));
    }

    private INDArray DistrCube(AST.MulOp pp, IntervalState intervalState) {
        System.out.println("Distr Mul==================");
        INDArray op1Array = DistrCube(pp.op1, intervalState);
        INDArray op2Array = DistrCube(pp.op2, intervalState);
        long[] op1shape = op1Array.shape();
        long[] op2shape = op2Array.shape();
        long[] outShape = getMaxShape(op1shape, op2shape);
        if (outShape.length > op1shape.length)
            op1Array = op1Array.reshape(getReshape(op1shape, outShape));
        else
            op2Array = op2Array.reshape(getReshape(op2shape, outShape));

        return op1Array.broadcast(outShape).mul(op2Array.broadcast(outShape));
    }

    private long[] getReshape(long[] op1shape, long[] outShape) {
        long[] reShape = new long[outShape.length];
        Arrays.fill(reShape, 1);
        System.arraycopy(op1shape, 0, reShape, 0, op1shape.length);
        return reShape;
    }

    private long[] getMaxShape(long[] op1shape, long[] op2shape) {
        long[] outShape;
        long minLength;
        if (op1shape.length >= op2shape.length) {
            outShape = new long[op1shape.length];
            System.arraycopy(op1shape, 0, outShape, 0, op1shape.length);
            minLength = op2shape.length;
        } else {
            outShape = new long[op2shape.length];
            System.arraycopy(op2shape, 0, outShape, 0, op2shape.length);
            minLength = op1shape.length;
        }
        for (int i=0; i < minLength; i++) {
            outShape[i] = max(op1shape[i], op2shape[i]);
        }
        return outShape;
    }


    private void BlockAnalysis(BasicBlock basicBlock) {
        IntervalState intervalState = new IntervalState();
        for (Statement statement : basicBlock.getStatements()) {
            if (statement.statement instanceof AST.Decl) {
                System.out.println("Decl: " + statement.statement.toString());
                ArrayList<AST.Annotation> annotations = statement.statement.annotations;
                addParams(statement);
                System.out.println(statement.statement.toString());
                // if (annotations != null && !annotations.isEmpty() &&

                //         (annotations.get(0).annotationType == AST.AnnotationType.Prior ||
                //                 annotations.get(0).annotationType == AST.AnnotationType.Limits)
                //         ) {
                //     addParams(statement);
                //     System.out.println(statement.statement.toString());
                // }
                // else {
                //
                // }
            } else if (statement.statement instanceof AST.AssignmentStatement) {
                ArrayList<AST.Annotation> annotations = statement.statement.annotations;
                AST.AssignmentStatement assignment = (AST.AssignmentStatement) statement.statement;
                if (annotations != null && !annotations.isEmpty() &&
                        annotations.get(0).annotationType == AST.AnnotationType.Observe) {
                    System.out.println("Observe (assign): " + statement.statement.toString());
                    String dataYID = assignment.lhs.toString();
                    if (!dataYID.contains("[")) {
                        Pair<AST.Data, String> yDataPair = dataList.get(dataYID);
                        String yValue = yDataPair.getValue();
                        double[] yArray = Arrays.stream(yValue.split(",")).mapToDouble(Double::parseDouble).toArray();
                        ObsDistr(yArray, assignment, intervalState);
                    }
                } else {
                    System.out.println("Assignment: " + statement.statement.toString());
                    String paramID = assignment.lhs.toString().split("\\[")[0];
                    if (!paramMap.containsKey(paramID)) {
                        addParams(statement);
                    }
                    Pair<Double[], ArrayList<Integer>> paramInfo = paramMap.get(paramID);
                    Double[] paramLimits = paramInfo.getKey();
                    ArrayList<Integer> paramDims = paramInfo.getValue();
                    if (isFuncConst(assignment.rhs)) {
                        System.out.println("Const param");
                        INDArray[] distrArray = IndDistr(assignment.rhs, paramLimits);
                        if (paramDims.size() == 1) {
                            for (Integer jj = 1; jj <= paramDims.get(0); jj++) {
                                intervalState.addIndParam(String.format("%s[%s]", paramID, jj),
                                        distrArray[0], distrArray[1], distrArray[2]);
                            }
                        } else if (paramDims.size() == 2) {
                            for (Integer jj = 1; jj <= paramDims.get(0); jj++) {
                                for (Integer kk = 1; kk <= paramDims.get(1); kk++)
                                    intervalState.addIndParam(String.format("%s[%s,%s]", paramID, jj, kk),
                                            distrArray[0], distrArray[1], distrArray[2]);
                            }
                        } else if (paramDims.size() == 0)
                            intervalState.addIndParam(paramID,
                                    distrArray[0], distrArray[1], distrArray[2]);
                        intervalState.printAbsState();
                    }
                    // TODO: dependent 
                }
            } else if (statement.statement instanceof AST.FunctionCallStatement) {
                System.out.println("FunctionCall: " + statement.statement.toString());

            } else if (statement.statement instanceof AST.IfStmt) {
                AST.IfStmt ifStmt = (AST.IfStmt) statement.statement;
                // BlockAnalysis(ifStmt.BBtrueBlock);
                // BlockAnalysis(ifStmt.BBelseBlock);
            } else if (statement.statement instanceof AST.ForLoop) {
                AST.ForLoop forLoop = (AST.ForLoop) statement.statement;
                System.out.println("ForLoop: "+ statement.statement);
                // BlockAnalysis(forLoop.BBloopBody);
            }
        }
        basicBlock.dataflowFacts = intervalState;
    }

    private void ObsDistr(double[] yArray, AST.AssignmentStatement assignment, IntervalState intervalState) {
        int yLength = yArray.length;
        long traceLength = intervalState.intervalProbPairs.shape()[0];
        AST.FunctionCall distrExpr = (AST.FunctionCall) assignment.rhs;
        INDArray[] params = new NDArray[distrExpr.parameters.size()];
        int parami = 0;
        for (AST.Expression pp: distrExpr.parameters) {
            if (pp instanceof AST.Integer) {
                params[parami] = Nd4j.createFromArray(((AST.Integer) pp).value);

            }
            else if (pp instanceof AST.Double) {
                params[parami] = Nd4j.createFromArray(((AST.Double) pp).value);
            }
            else {
                params[parami] = Distr(pp, intervalState);
                System.out.println(String.format("param %s: %s,%s,%s",pp.toString(), params[parami].shape()[0],params[parami].shape()[1],params[parami].shape()[2]));
            }
            parami++;
        }
        if (distrExpr.id.id.equals("normal")) {
            double[][] obsProb = new double[4][(int) traceLength];
            for (int yi=0; yi < yLength; yi++) {
                for (int ti=0; ti < traceLength; ti++) {

                    INDArray mu = params[0];
                    INDArray sigma = params[1];
                    NormalDistributionImpl normal0 = new NormalDistributionImpl(mu.getDouble(ti,yi,0), sigma.getDouble(ti,0,0) + 0.0001);
                    NormalDistributionImpl normal1 = new NormalDistributionImpl(mu.getDouble(ti,yi,0), sigma.getDouble(ti,0,1) + 0.0001);
                    NormalDistributionImpl normal2 = new NormalDistributionImpl(mu.getDouble(ti,yi,1), sigma.getDouble(ti,0,0) + 0.0001);
                    NormalDistributionImpl normal3 = new NormalDistributionImpl(mu.getDouble(ti,yi,1), sigma.getDouble(ti,0,1) + 0.0001);
                    obsProb[0][ti] += log(normal0.density(yArray[yi]));
                    obsProb[1][ti] += log(normal1.density(yArray[yi]));
                    obsProb[2][ti] += log(normal2.density(yArray[yi]));
                    obsProb[3][ti] += log(normal3.density(yArray[yi]));
                }
            }
            INDArray newProb = exp(Nd4j.create(obsProb)).transpose();
            INDArray NormNewProb = newProb.diviRowVector(newProb.sum(0));
            newProb = null;
            params = null;
            obsProb = null;
            System.gc();
            System.out.println(NormNewProb);
            System.out.println(intervalState.intervalProbPairs.shape()[1]);
            System.out.println(NormNewProb.shape()[1]);
            outputResults(intervalState, NormNewProb);
        }
    }

    private void outputResults(IntervalState intervalState, INDArray normNewProb) {
        int numParams = intervalState.paramMap.keySet().size();
        int[] shapeArray = new int[numParams];
        for (int i = 0; i < numParams; i++)
            shapeArray[i] = piCounts;
        int parami = 0;
        for (String param: intervalState.paramMap.keySet()) {
            INDArray outputTable = new NDArray(piCounts, 2 + normNewProb.shape()[1]);
            INDArray[] paramLU = intervalState.getParam(param);
            int paramInd = intervalState.getParamIdx(param);
            int[] paramIndArray = new int[numParams - 1];
            for (int i = 0; i < paramInd; i++)
                paramIndArray[i] = i;
            for (int i = paramInd; i < numParams - 1; i++)
                paramIndArray[i] = i + 1;
            INDArray lowerCube = paramLU[0].reshape(shapeArray);
            INDArray upperCube = paramLU[1].reshape(shapeArray);
            System.out.println(Arrays.toString(paramIndArray));
            outputTable.putColumn(0, lowerCube.vectorAlongDimension(0,paramInd));
            outputTable.putColumn(1, upperCube.vectorAlongDimension(0,paramInd));

            for (int probi = 0; probi < normNewProb.shape()[1]; probi++) {
                INDArray probCol = normNewProb.getColumn(probi);
                INDArray probCube = probCol.reshape(shapeArray);
                outputTable.putColumn(2 + probi, probCube.sum(paramIndArray));
            }
            // System.out.println(outputTable);
            // Nd4j.writeTxt(outputTable, "./analysis_"+ param + ".txt");
        }
    }

    private INDArray Distr(AST.Expression pp, IntervalState intervalState) {
        if (pp instanceof AST.Id) {
            return Distr((AST.Id) pp, intervalState);
        }
        else if (pp instanceof AST.AddOp) {
            return Distr((AST.AddOp) pp, intervalState);

        }
        else if (pp instanceof AST.MulOp) {
            return Distr((AST.MulOp) pp, intervalState);

        }
        else if (pp instanceof AST.Braces) {
            return Distr((AST.Braces) pp, intervalState);

        }
        else if (pp instanceof AST.ArrayAccess) {
            return Distr((AST.ArrayAccess) pp, intervalState);
        }
        return null;
    }

    private INDArray Distr(AST.Id pp, IntervalState intervalState) {
        System.out.println("Distr ID=================");

        if (dataList.containsKey(pp.id)) {
            Pair<AST.Data, String> xDataPair = dataList.get(pp.id);
            String xValue = xDataPair.getValue();
            double[] xArray = Arrays.stream(xValue.split(",")).mapToDouble(Double::parseDouble).toArray();
            System.gc();
            return Nd4j.create(xArray).reshape(1,-1);

        }
        else if (scalarParam.containsKey(pp.id)) {
            return Nd4j.create(new double[]{scalarParam.get(pp.id)});
        }
        else {
            int ididx = intervalState.paramMap.get(pp.id);
            return intervalState.intervalProbPairs.getColumns(ididx, ididx + 1).reshape(-1,1,2);
        }
    }

    private INDArray Distr(AST.ArrayAccess pp, IntervalState intervalState) {
        System.out.println("Distr ArrayAccess==================");
        // if (dataList.containsKey(pp.id)) {

        // }
        // else {
        int ididx = intervalState.paramMap.get(pp.toString());
        return intervalState.intervalProbPairs.getColumns(ididx, ididx + 1).reshape(-1,1,2);
        // }
    }

    private INDArray Distr(AST.Braces pp, IntervalState intervalState) {
        System.out.println("Distr Brace==================");
        return Distr(pp.expression, intervalState);
    }

    private INDArray Distr(AST.AddOp pp, IntervalState intervalState) {
        System.out.println("Distr Add==================");
        INDArray op1Array = Distr(pp.op1, intervalState);
        INDArray op2Array = Distr(pp.op2, intervalState);
        System.gc();
        INDArray retArray = null;
        if (op1Array.shape().length > 1 && op1Array.shape()[1] == 1) {
            retArray = Nd4j.stack(2, op2Array.slice(0, 2).addColumnVector(op1Array.slice(0, 2)), op2Array.slice(1, 2).addColumnVector(op1Array.slice(1, 2)));
        }
        else if (op2Array.shape().length > 1 && op2Array.shape()[1] == 1)
            retArray = Nd4j.stack(2, op1Array.slice(0, 2).addColumnVector(op2Array.slice(0, 2)), op1Array.slice(1, 2).addColumnVector(op2Array.slice(1, 2)));
        else
            retArray = Nd4j.stack(2, op1Array.slice(0, 2).add(op2Array.slice(0, 2)), op1Array.slice(1, 2).add(op2Array.slice(1, 2)));
        op1Array = null;
        op2Array = null;
        System.gc();
        return retArray;
    }

    private INDArray Distr(AST.MulOp pp, IntervalState intervalState) {
        System.out.println("Distr Mul==================");
        INDArray op1Array = Distr(pp.op1, intervalState);
        INDArray op2Array = Distr(pp.op2, intervalState);
        String op1Id = pp.op1.toString().split("\\[")[0].replace("(","");
        String op2Id = pp.op2.toString().split("\\[")[0].replace("(","");
        INDArray retArray;
        if ((dataList.containsKey(op1Id) && paramMap.containsKey(op2Id))){
            retArray = Nd4j.stack(2, op2Array.slice(0,2).mmul(op1Array), op2Array.slice(1,2).mmul(op1Array));
        }
        else if (dataList.containsKey(op2Id) && paramMap.containsKey(op1Id)) {
            retArray = Nd4j.stack(2, op1Array.slice(0, 2).mmul(op2Array), op1Array.slice(1, 2).mmul(op2Array));
            // System.out.println(op1Id + " " + op2Id);
            // System.out.println(String.format("%s,%s, %s", retArray.shape()[0], retArray.shape()[1], retArray.shape()[2]));
            // System.out.println(String.format("%s,%s", op1Array.shape()[0], op1Array.shape()[1]));
            // System.out.println(String.format("%s,%s", op2Array.shape()[0], op2Array.shape()[1]));
            // System.out.println(String.format("%s,%s", retArray.slice(1, 2).shape()[0], retArray.slice(1,2).shape()[1]));
            // System.out.println(Nd4j.hstack(Nd4j.arange(4).reshape(4,1).mmul(Nd4j.arange(3).reshape(1,3)),
            // Nd4j.arange(4,8).reshape(4,1).mmul(Nd4j.arange(3).reshape(1,3))));
        }
        else {
            retArray = Nd4j.stack(2, op1Array.slice(0, 2).mul(op2Array.slice(0, 2)), op1Array.getColumn(1, true).mul(op2Array.slice(1, 2)));
        }
        op1Array = null;
        op2Array = null;
        System.gc();
        return retArray;
    }

    private boolean isFuncConst(AST.Expression rhs) {
        if (rhs instanceof AST.FunctionCall) {
            AST.FunctionCall distrExpr = (AST.FunctionCall) rhs;
            for (AST.Expression pp: distrExpr.parameters){
                if(!(pp instanceof AST.Integer || pp instanceof AST.Double)){
                    return false;
                }
            }
            return true;
        }
        else
            return false;
    }

    // private INDArray[] Distr(AST.Expression expr) {
    //     Double[] paramLimits = {null,null};
    //     return Distr(expr, paramLimits);

    // }

    // private INDArray sameTraceExprs(ArrayList<AST.Expression> parameters) {
    //     return retArray;
    // }


    private INDArray[] IndDistr(AST.Expression expr, Double[] paramLimits) {
        // if (rhs instanceof AST.F)
        double[] lower = new double[piCounts];
        double[] upper = new double[piCounts];
        if (expr instanceof AST.FunctionCall) {
            AST.FunctionCall distrExpr = (AST.FunctionCall) expr;
            ArrayList<Double> funcParams = new ArrayList<>();
            for (AST.Expression pp: distrExpr.parameters){
                if (pp instanceof AST.Integer)
                    funcParams.add((double) ((AST.Integer) pp).value);
                else
                    funcParams.add(((AST.Double) pp).value);
            }
            String distrName = distrExpr.id.id;
            if (distrName.equals("normal")) {
                NormalDistribution normal = new NormalDistributionImpl(funcParams.get(0), funcParams.get(1));
                getDiscretePriors(lower, upper, normal);
            }
            else if (distrName.equals("gamma")) {
                GammaDistribution gamma = new GammaDistributionImpl(funcParams.get(0), funcParams.get(1));
                getDiscretePriors(lower, upper, gamma);
            }
        }
        // retArray[0]: prob, [1] lower, [2] upper
        return new INDArray[]{Nd4j.zeros(DataType.DOUBLE,piCounts).addi(pi), Nd4j.create(lower), Nd4j.create(upper)};


    }

    private void getDiscretePriors(double[] lower, double[] upper, ContinuousDistribution normal) {
        int ii = 0;
        //for(double pp=pi; pp <= 1-2*pi; pp += pi) {
        for(double pp=0; pp <= 1-1*pi; pp += pi) {
            try {
                lower[ii] = normal.inverseCumulativeProbability(pp);
                upper[ii] = normal.inverseCumulativeProbability(pp+pi);
            } catch (MathException e) {
                e.printStackTrace();
            }
            ii++;
        }
        if (lower[0] == Double.NEGATIVE_INFINITY)
            lower[0] = Double.MIN_VALUE;
        if (upper[0] == Double.NEGATIVE_INFINITY)
            upper[0] = Double.MIN_VALUE;
        if (lower[1] == Double.POSITIVE_INFINITY)
            lower[1] = Double.MAX_VALUE;
        if (upper[1] == Double.POSITIVE_INFINITY)
            upper[1] = Double.MAX_VALUE;
    }

    private void getDiscretePriorsSingle(double[] single, double[] prob1, double[] prob2, ContinuousDistribution normal) {
        HasDensity<Double> castHasDensity = (HasDensity<Double>) normal;
        //for(double pp=pi; pp <= 1-2*pi; pp += pi) {
        try {
            single[0] = normal.inverseCumulativeProbability(0);
            single[single.length - 1] = normal.inverseCumulativeProbability(1);
        } catch (MathException e) {
            e.printStackTrace();
        }
        if (single[0] == Double.NEGATIVE_INFINITY)
            single[0] = Double.MIN_VALUE;
        if (single[single.length - 1] == Double.POSITIVE_INFINITY)
            single[single.length - 1] = Double.MAX_VALUE;
        prob1[0] = 0;
        prob1[prob1.length - 1] = 0;
        prob2[0] = 0;
        prob2[prob2.length - 1] = 0;
        prob2[prob2.length - 2] = 0;
        int ii = 1;
        for(double pp=pi; pp <= 1 - pi; pp += pi, ii++) {
            try {
                single[ii] = normal.inverseCumulativeProbability(pp);
                prob1[ii] = (castHasDensity.density(single[ii]));
                prob2[ii - 1] = prob1[ii];
            } catch (MathException e) {
                e.printStackTrace();
            }
        }
    }


    private void getDiscretePriorsSingleUnif(double[] single, double[] prob1, double[] prob2, AbstractRealDistribution normal) {
        AbstractRealDistribution castHasDensity = (AbstractRealDistribution) normal;
        //for(double pp=pi; pp <= 1-2*pi; pp += pi) {
        single[0] = normal.inverseCumulativeProbability(0);
        single[single.length - 1] = normal.inverseCumulativeProbability(1);
        // if (single[0] == Double.NEGATIVE_INFINITY)
        //     single[0] = Double.MIN_VALUE;
        // if (single[single.length - 1] == Double.POSITIVE_INFINITY)
        //     single[single.length - 1] = Double.MAX_VALUE;
        prob1[0] = 0;
        prob1[prob1.length - 1] = 0;
        prob2[0] = 0;
        prob2[prob2.length - 1] = 0;
        prob2[prob2.length - 2] = 0;
        int ii = 1;
        for(double pp=pi; pp <= 1 - pi; pp += pi, ii++) {
            single[ii] = normal.inverseCumulativeProbability(pp);
            prob1[ii] = (castHasDensity.density(single[ii]));
            prob2[ii - 1] = prob1[ii];
        }
    }


    private INDArray[] IndDistrSingle(AST.Expression expr, Double[] paramLimits) {
        // if (rhs instanceof AST.F)
        double[] single = new double[piCounts];
        double[] prob1 = new double[piCounts];
        double[] prob2 = new double[piCounts];
        if (expr instanceof AST.FunctionCall) {
            AST.FunctionCall distrExpr = (AST.FunctionCall) expr;
            ArrayList<Double> funcParams = new ArrayList<>();
            for (AST.Expression pp: distrExpr.parameters){
                if (pp instanceof AST.Integer)
                    funcParams.add((double) ((AST.Integer) pp).value);
                else
                    funcParams.add(((AST.Double) pp).value);
            }
            String distrName = distrExpr.id.id;
            if (distrName.equals("normal")) {
                NormalDistribution normal = new NormalDistributionImpl(funcParams.get(0), funcParams.get(1));
                getDiscretePriorsSingle(single, prob1, prob2, normal);
            }
            else if (distrName.equals("gamma")) {
                GammaDistribution gamma = new GammaDistributionImpl(funcParams.get(0), funcParams.get(1));
                getDiscretePriorsSingle(single, prob1, prob2, gamma);
            }
        }
        return new INDArray[]{Nd4j.createFromArray(single), Nd4j.createFromArray(prob1), Nd4j.createFromArray(prob2)};
    }

    private void addParams(Statement statement) {
        if (statement.statement instanceof AST.Decl) {
            AST.Decl declStatement = (AST.Decl) statement.statement;
            Double[] limits = {null, null, null};
            for(AST.Annotation aa : declStatement.annotations) {
                if (aa.annotationType == AST.AnnotationType.Limits){
                    if (aa.annotationValue instanceof AST.Limits) {
                        AST.Limits aaLimits = (AST.Limits) aa.annotationValue;
                        if(aaLimits.lower != null)
                            limits[0] = Double.valueOf(aaLimits.lower.toString());
                        if(aaLimits.upper != null)
                            limits[1] = Double.valueOf(aaLimits.upper.toString());
                    }
                }
            }
            ArrayList<Integer> dimArray = new ArrayList<>();
            if (declStatement.dtype.dims != null) {
                for (AST.Expression dd: declStatement.dtype.dims.dims) {
                    getConstN(dimArray, dd);
                }
            }
            if (declStatement.dims != null) {
                for (AST.Expression dd : declStatement.dims.dims) {
                    getConstN(dimArray, dd);
                }
            }
            String arrayId = declStatement.id.id;
            if (dimArray.size() == 1) {
                for (Integer jj = 1; jj <= dimArray.get(0); jj++) {
                    String eleId = String.format("%s[%s]", arrayId, jj);
                    paramMap.put(eleId, new Pair(limits, dimArray));
                }
            } else if (dimArray.size() == 2) {
                for (Integer jj = 1; jj <= dimArray.get(0); jj++) {
                    for (Integer kk = 1; kk <= dimArray.get(1); kk++) {
                        String eleId = String.format("%s[%s,%s]", arrayId, jj, kk);
                        paramMap.put(eleId, new Pair(limits, dimArray));
                    }
                }
            } else if (dimArray.size() == 0) {
                String eleId = arrayId;
                paramMap.put(eleId, new Pair(limits, dimArray));
            }
        }
    }

    private void getConstN(ArrayList<Integer> dimArray, AST.Expression dd) {
        if (dd.toString().matches("\\d+"))
            dimArray.add(Integer.valueOf(dd.toString()));
        else {
            Pair<AST.Data, String> dataPair = dataList.get(dd.toString());
            AST.Data data = dataPair.getKey();
            assert (data.decl.dtype.primitive == AST.Primitive.INTEGER);
            dimArray.add(Integer.valueOf(data.expression.toString()));
        }
    }
}
