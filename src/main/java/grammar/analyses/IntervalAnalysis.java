package grammar.analyses;

import com.google.common.primitives.Doubles;
import grammar.AST;
import grammar.cfg.*;
import grammar.cfg.BasicBlock;
import jdk.internal.org.objectweb.asm.Type;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.*;
import org.apache.commons.math3.distribution.AbstractRealDistribution;
import org.apache.commons.math3.distribution.UniformRealDistribution;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.ops.impl.summarystats.StandardDeviation;
import org.nd4j.linalg.cpu.nativecpu.NDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.BooleanIndexing;
import org.nd4j.linalg.indexing.conditions.Conditions;
import utils.Utils;

import java.util.*;

import static java.lang.Math.*;
import static org.nd4j.linalg.ops.transforms.Transforms.exp;


public class IntervalAnalysis {
    private Map<String, Pair<Double[], ArrayList<Integer>>> paramMap = new HashMap<>();
    private Map<String, Integer> paramDivs = new HashMap<>();
    private Map<String, Pair<AST.Data, double[]>> dataList = new HashMap<>();
    private Set<String> obsDataList = new HashSet<>();
    private Map<String, Integer> scalarParam = new HashMap<>();
    private Queue<BasicBlock> worklistAll = new LinkedList<>();
    private int maxCounts = 100;
    private int minCounts = 10;
    private int PACounts = 10;
    private Boolean toAttack;

    @Deprecated
    private double maxProb = 1.0/(maxCounts - 1);
    @Deprecated
    private double minProb = 1.0/(minCounts - 1);


    public void forwardAnalysis(ArrayList<Section> cfgSections) {
        Nd4j.setDataType(DataType.DOUBLE);
        InitWorklist(cfgSections);
        System.out.println(paramMap.keySet());
        IntervalState endFacts;
        Queue<BasicBlock> worklist = new LinkedList<>();
        // Pre-Analysis: Run a Worklist Algorithm
        toAttack = false;
        worklist.add(worklistAll.peek());
        endFacts = WorklistIter(worklist);
        // Then focus on the max value from Pre-Analysis
        if (endFacts != null) {
            for (String kk: endFacts.paramValues.keySet()) {
                if (kk.equals("Datai"))
                    continue;
                Pair<Double[], ArrayList<Integer>> limitsDims = paramMap.get(kk);
                Double[] limits = limitsDims.getKey();
                limits[2] = endFacts.getResultsMean(kk);
                paramDivs.put(kk, minCounts);
            }
        }
        // Analysis again by individual/groups of params
        String prevKk = null;
        toAttack = true;
        for (String kk: paramMap.keySet()) {
            paramDivs.put(kk, maxCounts);
            if (prevKk != null) {
                paramDivs.put(prevKk, minCounts);
            }
            for (BasicBlock bb: worklistAll) {
                bb.dataflowFacts = null;
            }
            scalarParam.clear();
            System.gc();
            prevKk = kk;
            worklist.add(worklistAll.peek());
            endFacts = WorklistIter(worklist);
            System.out.println(Nd4j.createFromArray(endFacts.probCube.get(0).shape()));
            endFacts.writeResults(new HashSet<>(Collections.singletonList(kk)));
        }
    }

    private IntervalState WorklistIter(Queue<BasicBlock> worklist) {
        IntervalState endFacts = null;
        BasicBlock currBlock;
        while (!worklist.isEmpty()) {
            currBlock = worklist.remove();
            System.out.println("//////////// Analyze block: " + currBlock.getId());
            Boolean changed = BlockAnalysisCube(currBlock);
            endFacts = currBlock.dataflowFacts;
            Map<String, BasicBlock> succs = currBlock.getOutgoingEdges();
            for (String cond : succs.keySet()) {
                BasicBlock succ = succs.get(cond);
                succ.dataflowFacts = endFacts;
                if (changed) {
                    worklist.add(succ);
                }
            }
        }
        return endFacts;
    }

    private void InitWorklist(ArrayList<Section> cfgSections) {
        for (Section section : cfgSections) {
            System.out.println(section.sectionType);
            if (section.sectionType == SectionType.DATA) {
                ArrayList<AST.Data> dataSets = section.basicBlocks.get(0).getData();
                for (AST.Data dd: dataSets) {
                    String dataString = Utils.parseData(dd, 'f');
                    dataString = dataString.replaceAll("\\s", "").replaceAll("\\[", "").replaceAll("\\]", "").replaceAll("\\.0,",",").replaceAll("\\.0$","");
                    double[] dataArray = Arrays.stream(dataString.split(",")).mapToDouble(Double::parseDouble).toArray();
                    dataList.put(dd.decl.id.id, new Pair(dd, dataArray));
                }

            } else if(section.sectionType == SectionType.FUNCTION) {
                System.out.println(section.sectionName);
                if (section.sectionName.equals("main")) {
                    for (BasicBlock basicBlock: section.basicBlocks) {
                        worklistAll.add(basicBlock);
                        for (Statement statement : basicBlock.getStatements()) {
                            if (statement.statement instanceof AST.Decl)
                                addParams(statement);
                            ArrayList<AST.Annotation> annotations = statement.statement.annotations;
                            if (annotations != null && !annotations.isEmpty() &&
                                    annotations.get(0).annotationType == AST.AnnotationType.Observe) {
                                AST.AssignmentStatement assignment = (AST.AssignmentStatement) statement.statement;
                                String dataYName = assignment.lhs.toString().split("\\[")[0];
                                obsDataList.add(dataYName);
                                System.out.println(obsDataList);
                                Pair<AST.Data, double[]> orgDataPair = dataList.get(dataYName);
                                double[] newDataValue = orgDataPair.getValue().clone();
                                getAttack(newDataValue);
                                dataList.put(dataYName + "_corrupted", new Pair<>(orgDataPair.getKey(), newDataValue));
                            }
                        }
                    }
                }

            } else if(section.sectionType == SectionType.QUERIES) {
                for (BasicBlock basicBlock: section.basicBlocks)
                    for (Statement statement : basicBlock.getStatements())
                        System.out.println(statement.statement.toString());
            }
        }
    }

    private void getAttack(double[] newDataValue) {
        double sd = 0;
        double sum = 0;
        for (int i=0; i<newDataValue.length;i++) {
            sum += newDataValue[i];
        }
        sum = sum / (double) newDataValue.length;
        for (int i=0; i<newDataValue.length;i++) {
            sd = sd + Math.pow(newDataValue[i] - sum, 2);
        }
        for (int i=0; i<newDataValue.length; i+= 100) {
            newDataValue[i] = newDataValue[i]; // + 2*Math.sqrt(sd);
        }
    }

    private Boolean BlockAnalysisCube(BasicBlock basicBlock) {
        Boolean changed = false;
        IntervalState intervalState;
        if (basicBlock.dataflowFacts == null)
            intervalState = new IntervalState();
        else
            intervalState = basicBlock.dataflowFacts;
        for (Statement statement : basicBlock.getStatements()) {
            if (statement.statement instanceof AST.Decl) {
                System.out.println("Decl: " + statement.statement.toString());
                addParams(statement);
                System.out.println(statement.statement.toString());
                changed = true;

            } else if (statement.statement instanceof AST.AssignmentStatement) {
                changed = analyzeAssignment(intervalState, statement);
            } else if (statement.statement instanceof AST.FunctionCallStatement) {
                System.out.println("FunctionCall: " + statement.statement.toString());

            } else if (statement.statement instanceof AST.IfStmt) {
                AST.IfStmt ifStmt = (AST.IfStmt) statement.statement;
                // BlockAnalysis(ifStmt.BBtrueBlock);
                // BlockAnalysis(ifStmt.BBelseBlock);
            } else if (statement.statement instanceof AST.ForLoop) {
                AST.ForLoop forLoop = (AST.ForLoop) statement.statement;
                System.out.println("ForLoop: "+ statement.statement);
                changed = incLoop(forLoop);
            }
            basicBlock.dataflowFacts = intervalState;
        }
        return changed;
    }

    private Boolean analyzeAssignment(IntervalState intervalState, Statement statement) {
        Boolean changed;
        ArrayList<AST.Annotation> annotations = statement.statement.annotations;
        AST.AssignmentStatement assignment = (AST.AssignmentStatement) statement.statement;
        if (annotations != null && !annotations.isEmpty() &&
                annotations.get(0).annotationType == AST.AnnotationType.Observe) {
            System.out.println("Observe (assign): " + statement.statement.toString());
            double[][] yArray = getYArray(assignment.lhs, intervalState);
            ObsDistrCube(yArray, assignment, intervalState);
            changed = true;
        } else {
            System.out.println("Assignment: " + statement.statement.toString());
            String paramID = assignment.lhs.toString().split("\\[")[0];
            if (paramMap.containsKey(paramID) || paramMap.containsKey(paramID + "[1]") || paramMap.containsKey(paramID + "[1,1]")) {
                String newParamID = paramID;
                Pair<Double[], ArrayList<Integer>> paramInfo = paramMap.get(paramID);
                if (paramInfo == null) {
                    paramInfo = paramMap.get(paramID + "[1]");
                    newParamID = paramID + "[1]";
                }
                if (paramInfo == null) {
                    paramInfo = paramMap.get(paramID + "[1,1]");
                    newParamID = paramID + "[1,1]";
                }
                Double[] paramLimits = paramInfo.getKey();
                ArrayList<Integer> paramDims = paramInfo.getValue();
                if (assignment.rhs instanceof AST.FunctionCall
                        && isFuncConst(assignment.rhs)) { // completely independent new param
                    System.out.println("Const param");
                    // TODO: fix dim if rhs contains data
                    // TODO: fix usage of single Id without dim, e.g beta~... but beta has dim 2
                    if (paramDivs.containsKey(newParamID)) {
                        initIndParamAllDims(intervalState, assignment, paramID, paramLimits, paramDims);
                    }
                    else {
                        initIndParamAllDimsPA(intervalState, assignment, paramID, paramLimits, paramDims);
                    }
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
                changed = true;
            } else {
                // TODO:
                changed = true;
                throw new AssertionError();
            }
        }
        return changed;
    }

    private double[][] getYArray(AST.Expression lhs, IntervalState intervalState) {
        String dataYID = lhs.toString();
        double[] ret1;
        double[] ret2;
        if (!toAttack) {
            if (!dataYID.contains("[")) {
                Pair<AST.Data, double[]> yDataPair = dataList.get(dataYID.split("\\[")[0]);
                ret1 = yDataPair.getValue();
            } else {
                ret1 = DistrCube(lhs, intervalState).toDoubleVector();
            }
            return new double[][]{ret1};
        } else {
            if (!dataYID.contains("[")) {
                Pair<AST.Data, double[]> yDataPair = dataList.get(dataYID.split("\\[")[0]);
                ret1 = yDataPair.getValue();
                Pair<AST.Data, double[]> yDataPairCorrupted = dataList.get(dataYID.split("\\[")[0] + "_corrupted");
                ret2 = yDataPairCorrupted.getValue();
            } else {
                INDArray twoRet = DistrCube(lhs, intervalState);
                System.out.println(twoRet);
                assert(twoRet.length() == 2);
                ret1 = new double[]{twoRet.getDouble(0)};
                ret2 = new double[]{twoRet.getDouble(1)};
            }
            return new double[][]{ret2, ret1};
        }
    }


    private Boolean incLoop(AST.ForLoop forLoop) {
        Boolean changed;
        String loopVar = forLoop.loopVar.id;
        if (scalarParam.containsKey(loopVar)) {
            int currLoopValue = scalarParam.get(loopVar);
            ArrayList<Integer> endValue = new ArrayList<>();
            getConstN(endValue, forLoop.range.end);
            System.out.println("/////////////////" + String.valueOf(currLoopValue));
            if (currLoopValue < endValue.get(0)) {
                scalarParam.put(loopVar, currLoopValue + 1);
                changed = true;
            }
            else {
                changed = false;
            }

        } else {
            scalarParam.put(loopVar, Integer.valueOf(forLoop.range.start.toString()));
            changed = true;
        }
        // BlockAnalysis(forLoop.BBloopBody);
        return changed;
    }

    private void initIndParamAllDims(IntervalState intervalState, AST.AssignmentStatement assignment, String paramID, Double[] paramLimits, ArrayList<Integer> paramDims) {
        if (paramDims.size() == 1) {
            INDArray rhsMin[] = IndDistrSingle(assignment.rhs, paramLimits, minCounts); // split, probLower, probUpper
            for (Integer jj = 1; jj <= paramDims.get(0); jj++) {
                String currParamName = String.format("%s[%s]", paramID, jj);
                if (paramDivs.get(currParamName) == minCounts)
                    intervalState.addParamCube(currParamName, rhsMin[0], rhsMin[1], rhsMin[2]);
                else { // maxCounts
                    INDArray rhsMax[] = IndDistrSingle(assignment.rhs, paramLimits, maxCounts); // split, probLower, probUpper
                    intervalState.addParamCube(currParamName, rhsMax[0], rhsMax[1], rhsMax[2]);
                }
            }
        } else if (paramDims.size() == 2) {
            INDArray rhsMin[] = IndDistrSingle(assignment.rhs, paramLimits, minCounts); // split, probLower, probUpper
            for (Integer jj = 1; jj <= paramDims.get(0); jj++) {
                for (Integer kk = 1; kk <= paramDims.get(1); kk++) {
                    String currParamName = String.format("%s[%s,%s]", paramID, jj, kk);
                    if (paramDivs.get(currParamName) == minCounts)
                        intervalState.addParamCube(currParamName, rhsMin[0], rhsMin[1], rhsMin[2]);
                    else {
                        INDArray rhsMax[] = IndDistrSingle(assignment.rhs, paramLimits, maxCounts); // split, probLower, probUpper
                        intervalState.addParamCube(currParamName, rhsMax[0], rhsMax[1], rhsMax[2]);
                    }
                }
            }
        } else if (paramDims.size() == 0) {
            if (paramDivs.get(paramID) == minCounts) {
                INDArray rhsMin[] = IndDistrSingle(assignment.rhs, paramLimits, minCounts); // split, probLower, probUpper
                intervalState.addParamCube(paramID, rhsMin[0], rhsMin[1], rhsMin[2]);
            } else { // maxCounts
                INDArray rhsMax[] = IndDistrSingle(assignment.rhs, paramLimits, maxCounts); // split, probLower, probUpper
                intervalState.addParamCube(paramID, rhsMax[0], rhsMax[1], rhsMax[2]);

            }
        }
    }

    private void initIndParamAllDimsPA(IntervalState intervalState, AST.AssignmentStatement assignment, String paramID, Double[] paramLimits, ArrayList<Integer> paramDims) {
        if (paramDims.size() == 1) {
            INDArray rhsMin[] = IndDistrSingle(assignment.rhs, paramLimits, PACounts); // split, probLower, probUpper
            for (Integer jj = 1; jj <= paramDims.get(0); jj++) {
                String currParamName = String.format("%s[%s]", paramID, jj);
                intervalState.addParamCube(currParamName, rhsMin[0], rhsMin[1], rhsMin[2]);
            }
        } else if (paramDims.size() == 2) {
            INDArray rhsMin[] = IndDistrSingle(assignment.rhs, paramLimits, PACounts); // split, probLower, probUpper
            for (Integer jj = 1; jj <= paramDims.get(0); jj++) {
                for (Integer kk = 1; kk <= paramDims.get(1); kk++) {
                    String currParamName = String.format("%s[%s,%s]", paramID, jj, kk);
                    intervalState.addParamCube(currParamName, rhsMin[0], rhsMin[1], rhsMin[2]);
                }
            }
        } else if (paramDims.size() == 0) {
            INDArray rhsMin[] = IndDistrSingle(assignment.rhs, paramLimits, PACounts); // split, probLower, probUpper
            intervalState.addParamCube(paramID, rhsMin[0], rhsMin[1], rhsMin[2]);
        }
    }

    private void HierDistrCube(AST.AssignmentStatement assignment, IntervalState intervalState) {
        AST.FunctionCall distrExpr = (AST.FunctionCall) assignment.rhs;
        INDArray[] params = getParams(intervalState, distrExpr);
        if (params == null) return;
        if (params.length == 1) {
            // assert(!(params[0].shape().length == 1 && params[0].shape()[0] == 1)); // o.w const distr
            // long[] retshape = new long[params[0].shape().length + 1];
            // System.arraycopy(params[0].shape(), 0, retshape, 0, params[0].shape().length);
            // retshape[params[0].shape().length] = piCounts;
            // TODO:
        }
        else if (params.length == 2) {
            INDArray[] singleprob = expand1D(params[0], params[1], "normal", assignment.lhs.toString());
            if (singleprob[0].shape().length > 0)
                intervalState.addParamCube(assignment.lhs.toString(), singleprob[0], singleprob[1], singleprob[2]);
            else
                intervalState.addDepParamCube(assignment.lhs.toString(), Nd4j.empty());
            // TODO: support other dists
        }
    }

    private INDArray[] expand1D(INDArray input1, INDArray input2, String distr, String paramName) {
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
        int piCounts;
        if (paramDivs.containsKey(paramName))
            piCounts = paramDivs.get(paramName);
        else
            piCounts = PACounts;
        if (piCounts >= 1) {
            double pi;
            if (piCounts > 1)
                pi = 1.0 / (piCounts - 1);
            else
                pi = -1; // indicating only one point
            broadcastDims[maxDimCount] = piCounts;
            INDArray input1b = input1.broadcast(shape1b);
            INDArray input2b = input2.broadcast(shape2b);
            INDArray single = Nd4j.createUninitialized(broadcastDims);
            INDArray prob1 = Nd4j.createUninitialized(broadcastDims);
            INDArray prob2 = Nd4j.createUninitialized(broadcastDims);
            for (int i = 0; i < input1b.length(); i++) {
                double mean = input1b.getDouble(i);
                double sd = input2b.getDouble(i);
                // if (distrName.equals("normal")) {
                double[] singlej = new double[piCounts];
                double[] prob1j = new double[piCounts];
                double[] prob2j = new double[piCounts];
                NormalDistribution normal = new NormalDistributionImpl(mean, sd);
                getDiscretePriorsSingle(singlej, prob1j, prob2j, normal, pi);
                // }
                prob1.tensorAlongDimension(i, maxDimCount).assign(Nd4j.createFromArray(prob1j));
                prob2.tensorAlongDimension(i, maxDimCount).assign(Nd4j.createFromArray(prob2j));
                single.tensorAlongDimension(i, maxDimCount).assign(Nd4j.createFromArray(singlej));
            }
            return new INDArray[]{single, prob1, prob2};
        }
        else {
            return new INDArray[]{Nd4j.empty(), Nd4j.empty(), Nd4j.empty()};
        }
    }

    private void ObsDistrCube(double[][] yArray, AST.AssignmentStatement assignment, IntervalState intervalState) {
        INDArray sumExpLower = null;
        INDArray sumExpUpper = null;
        AST.FunctionCall distrExpr = (AST.FunctionCall) assignment.rhs;
        INDArray[] params = getParams(intervalState, distrExpr);
        if (params == null) return;
        if (distrExpr.id.id.equals("normal")) {
            long[] shape1 = params[0].shape();
            long[] shape2 = params[1].shape();
            INDArray yNDArray = Nd4j.createFromArray(yArray[0]);
            long[] maxShape = getMaxShape(shape1, shape2);
            maxShape = getMaxShape(maxShape, yNDArray.shape());
            params[0] = params[0].reshape(getReshape(shape1, maxShape)).broadcast(maxShape);
            params[1] = params[1].reshape(getReshape(shape2, maxShape)).broadcast(maxShape);
            yNDArray = yNDArray.reshape(getReshape(yNDArray.shape(), maxShape)).broadcast(maxShape);
            INDArray likeCube = Nd4j.createUninitialized(maxShape);
            for (long ii=0; ii<likeCube.length(); ii++) {
                // NormalDistributionImpl normal = new NormalDistributionImpl(params[0].getDouble(ii), sd);
                // likeCube.putScalar(ii, log(normal.density(yNDArray.getDouble(ii))));
                likeCube.putScalar(ii, normal_LPDF(yNDArray.getDouble(ii),
                        params[0].getDouble(ii),
                        params[1].getDouble(ii)));
            }
            // double minVal = Nd4j.min(likeCube).getDouble() - 100;
            // BooleanIndexing.replaceWhere(likeCube, minVal, Conditions.isInfinite());
            INDArray logSum = likeCube.sum(0);
            sumExpUpper = logSum; // Exp
            if (!toAttack)
                sumExpLower = sumExpUpper;
            else {
                yNDArray = Nd4j.createFromArray(yArray[1]);
                yNDArray = yNDArray.reshape(getReshape(yNDArray.shape(), maxShape)).broadcast(maxShape);
                for (long ii=0; ii<likeCube.length(); ii++) {
                    likeCube.putScalar(ii, normal_LPDF(yNDArray.getDouble(ii),
                            params[0].getDouble(ii),
                            params[1].getDouble(ii)));
                }
                logSum = likeCube.sum(0);
                sumExpLower = logSum; // Exp
            }
            intervalState.addProb(sumExpLower, sumExpUpper);
        }
    }

    private INDArray[] getParams(IntervalState intervalState, AST.FunctionCall distrExpr) {
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
                if (params[parami].length() == 0)
                    return null;
            }
            parami++;
        }
        return params;
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
            Pair<AST.Data, double[]> xDataPair = dataList.get(pp.id);
            double[] xArray = xDataPair.getValue();
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
            Pair<AST.Data, double[]> xDataPair = dataList.get(pp.id.id);
            double[] xArray = xDataPair.getValue();
            double dataElement = xArray[dims.get(0) - 1]; // TODO: support 2D array access
            System.out.println("******************");
            System.out.println(toAttack);
            System.out.println(obsDataList);
            if ((!toAttack) || (!obsDataList.contains(pp.id.id)))
                return Nd4j.createFromArray(dataElement); //.reshape(intervalState.getNewDim(1));
            else {
                xDataPair = dataList.get(pp.id.id + "_corrupted");
                xArray = xDataPair.getValue();
                double dataElement2 = xArray[dims.get(0) - 1]; // TODO: support 2D array access
                return Nd4j.createFromArray(dataElement, dataElement2); //.reshape(intervalState.getNewDim(1));
            }
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
        int piCounts;
        if (paramDivs.containsKey(paramName))
            piCounts = paramDivs.get(paramName);
        else
            piCounts = PACounts;
        double[] probLower = new double[piCounts];
        double[] probUpper = new double[piCounts];
        double[] single = new double[piCounts];
        if (piCounts >= 1) {
            double pi;
            if (piCounts > 1)
                pi = 1.0 / (piCounts - 1);
            else
                pi = -1;
            if (limits[0] != null && limits[1] != null) {
                UniformRealDistribution unif = new UniformRealDistribution(limits[0], limits[1]);
                getDiscretePriorsSingleUnif(single, probLower, probUpper, unif, pi);
            } else if (limits[0] != null) {
                if (limits[0] == 0) {
                    GammaDistribution gamma = new GammaDistributionImpl(1, 1);
                    getDiscretePriorsSingleUn(single, probLower, probUpper, gamma, pi);
                } else {
                    UniformRealDistribution unif = new UniformRealDistribution(limits[0], limits[0] + 5);
                    getDiscretePriorsSingleUnif(single, probLower, probUpper, unif, pi);
                }
            } else if (limits[2] != null) {
                NormalDistribution normal = new NormalDistributionImpl(limits[2], 1);
                getDiscretePriorsSingleUn(single, probLower, probUpper, normal, pi);
            } else { // all are null
                System.out.println("Prior: Normal");
                NormalDistribution normal = new NormalDistributionImpl(0, 5);
                getDiscretePriorsSingleUn(single, probLower, probUpper, normal, pi);
            }
            intervalState.addParamCube(paramName, Nd4j.createFromArray(single).reshape(intervalState.getNewDim(piCounts)),
                    Nd4j.createFromArray(probLower).reshape(intervalState.getNewDim(piCounts)),
                    Nd4j.createFromArray(probUpper).reshape(intervalState.getNewDim(piCounts)));
        }
        else {
            intervalState.addDepParamCube(paramName, Nd4j.empty());

        }
    }




    private INDArray DistrCube(AST.Braces pp, IntervalState intervalState) {
        System.out.println("Distr Brace==================");
        return DistrCube(pp.expression, intervalState);
    }

    private INDArray DistrCube(AST.AddOp pp, IntervalState intervalState) {
        System.out.println("Distr Add==================");
        INDArray op1Array = DistrCube(pp.op1, intervalState);
        INDArray op2Array = DistrCube(pp.op2, intervalState);
        if (op1Array.length() != 0 && op2Array.length() != 0) {
            long[] op1shape = op1Array.shape();
            long[] op2shape = op2Array.shape();
            long[] outShape = getMaxShape(op1shape, op2shape);
            if (outShape.length > op1shape.length)
                op1Array = op1Array.reshape(getReshape(op1shape, outShape));
            else
                op2Array = op2Array.reshape(getReshape(op2shape, outShape));
            return op1Array.broadcast(outShape).add(op2Array.broadcast(outShape));
        } else {
            return Nd4j.empty();
        }
    }

    private INDArray DistrCube(AST.MulOp pp, IntervalState intervalState) {
        System.out.println("Distr Mul==================");
        INDArray op1Array = DistrCube(pp.op1, intervalState);
        INDArray op2Array = DistrCube(pp.op2, intervalState);
        if (op1Array.length() != 0 && op2Array.length() != 0) {
            long[] op1shape = op1Array.shape();
            long[] op2shape = op2Array.shape();
            long[] outShape = getMaxShape(op1shape, op2shape);
            if (outShape.length > op1shape.length)
                op1Array = op1Array.reshape(getReshape(op1shape, outShape));
            else
                op2Array = op2Array.reshape(getReshape(op2shape, outShape));

            return op1Array.broadcast(outShape).mul(op2Array.broadcast(outShape));
        }
        else {
            return Nd4j.empty();
        }
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


    private void getDiscretePriorsSingle(double[] single, double[] prob1, double[] prob2,
                                         ContinuousDistribution normal, double pi) {
        HasDensity<Double> castHasDensity = (HasDensity<Double>) normal;
        if (pi >= 0) {
            try {
                single[0] = normal.inverseCumulativeProbability(0.0000000001);
                single[single.length - 1] = normal.inverseCumulativeProbability(0.9999999999);
            } catch (MathException e) {
                e.printStackTrace();
            }
            if (single[0] == Double.NEGATIVE_INFINITY)
                single[0] = -pow(10, 16);
            if (single[single.length - 1] == Double.POSITIVE_INFINITY)
                single[single.length - 1] = pow(10, 16);
            prob1[0] = 0;
            prob1[prob1.length - 1] = 0;
            prob2[0] = 0;
            prob2[prob2.length - 1] = 0;
            prob2[prob2.length - 2] = 0;
            double pp = pi;
            for (int ii = 1; ii <= single.length - 2; pp += pi, ii++) {
                try {
                    single[ii] = normal.inverseCumulativeProbability(pp);
                    prob1[ii] = (castHasDensity.density(single[ii]));
                    prob2[ii - 1] = prob1[ii];
                } catch (MathException e) {
                    e.printStackTrace();
                }
            }
        }
        else {
            try {
                single[0] = normal.inverseCumulativeProbability(0.5);
                prob1[0] = castHasDensity.density(single[0]);
                prob2[0] = castHasDensity.density(single[0]);
            } catch (MathException e) {
                e.printStackTrace();
            }

        }
    }

    private void getDiscretePriorsSingleUn(double[] single, double[] prob1, double[] prob2, ContinuousDistribution normal, double pi) {
        HasDensity<Double> castHasDensity = (HasDensity<Double>) normal;
        if (pi >= 0) {
            try {
                single[0] = normal.inverseCumulativeProbability(0.00000000001);
                single[single.length - 1] = normal.inverseCumulativeProbability(0.9999999999);
            } catch (MathException e) {
                e.printStackTrace();
            }
            if (single[0] == Double.NEGATIVE_INFINITY)
                single[0] = -pow(10, 16);
            if (single[single.length - 1] == Double.POSITIVE_INFINITY)
                single[single.length - 1] = pow(10, 16);
            int ii = 1;
            for (double pp = pi; pp <= 1 - pi; pp += pi, ii++) {
                try {
                    single[ii] = normal.inverseCumulativeProbability(pp);
                } catch (MathException e) {
                    e.printStackTrace();
                }
            }
            Arrays.fill(prob1, 1);
            Arrays.fill(prob2, 1);
        }
        else {
            try {
                single[0] = normal.inverseCumulativeProbability(0.5);
            } catch (MathException e) {
                e.printStackTrace();
            }
            prob1[0] = 1;
            prob2[0] = 1;
        }
    }

    private void getDiscretePriorsSingleUnif(double[] single, double[] prob1, double[] prob2, AbstractRealDistribution normal, double pi) {
        AbstractRealDistribution castHasDensity = (AbstractRealDistribution) normal;
        if (pi >= 0) {
            single[0] = normal.inverseCumulativeProbability(0.0000000001);
            single[single.length - 1] = normal.inverseCumulativeProbability(0.9999999999);
            if (single[0] == Double.NEGATIVE_INFINITY)
                single[0] = -pow(10, 10);
            if (single[single.length - 1] == Double.POSITIVE_INFINITY)
                single[single.length - 1] = pow(10, 10);
            Arrays.fill(prob1, 1);
            Arrays.fill(prob2, 1);
            int ii = 1;
            for (double pp = pi; pp <= 1 - pi; pp += pi, ii++) {
                single[ii] = normal.inverseCumulativeProbability(pp);
                // prob1[ii] = (castHasDensity.density(single[ii]));
                // prob2[ii - 1] = prob1[ii];
            }
        }
        else {
            single[0] = normal.inverseCumulativeProbability(0.5);
            prob1[0] = 1;
            prob2[0] = 1;
        }
    }


    private INDArray[] IndDistrSingle(AST.Expression expr, Double[] paramLimits, int piCounts) {
        // if (rhs instanceof AST.F)
        if (piCounts >= 1) {
            double pi;
            if (piCounts > 1)
                pi = 1.0 / (piCounts - 1);
            else
                pi = -1;
            double[] single = new double[piCounts];
            double[] prob1 = new double[piCounts];
            double[] prob2 = new double[piCounts];
            if (expr instanceof AST.FunctionCall) {
                AST.FunctionCall distrExpr = (AST.FunctionCall) expr;
                ArrayList<Double> funcParams = new ArrayList<>();
                for (AST.Expression pp : distrExpr.parameters) {
                    if (pp instanceof AST.Integer)
                        funcParams.add((double) ((AST.Integer) pp).value);
                    else
                        funcParams.add(((AST.Double) pp).value);
                }
                String distrName = distrExpr.id.id;
                if (distrName.equals("normal")) {
                    NormalDistribution normal = new NormalDistributionImpl(funcParams.get(0), funcParams.get(1));
                    getDiscretePriorsSingle(single, prob1, prob2, normal, pi);
                } else if (distrName.equals("gamma")) {
                    GammaDistribution gamma = new GammaDistributionImpl(funcParams.get(0), funcParams.get(1));
                    getDiscretePriorsSingle(single, prob1, prob2, gamma, pi);
                }
            }
            return new INDArray[]{Nd4j.createFromArray(single), Nd4j.createFromArray(prob1), Nd4j.createFromArray(prob2)};
        }
        else {
            return new INDArray[]{Nd4j.empty(), Nd4j.empty(), Nd4j.empty()};
        }
    }

    // Used in Pre-Analysis to find all params definition
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
        else if (dataList.containsKey(dd.toString())){
            Pair<AST.Data, double[]> dataPair = dataList.get(dd.toString());
            AST.Data data = dataPair.getKey();
            assert (data.decl.dtype.primitive == AST.Primitive.INTEGER);
            dimArray.add(Integer.valueOf(data.expression.toString()));
        }
        else {
            dimArray.add(scalarParam.get(dd.toString()));

        }
    }

    private double normal_LPDF(double y, double mu, double sigma) {
        return -log(sigma) - 0.5*((y - mu)*(y - mu)/(sigma*sigma));
    }

    //=================================================================================================
    //=============  An Inefficient Implementation by Joint Probability Tables ========================
    //=================================================================================================

    // private INDArray[] Distr(AST.Expression expr) {
    //     Double[] paramLimits = {null,null};
    //     return Distr(expr, paramLimits);

    // }

    // private INDArray sameTraceExprs(ArrayList<AST.Expression> parameters) {
    //     return retArray;
    // }

    @Deprecated
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
                        Pair<AST.Data, double[]> yDataPair = dataList.get(dataYID);
                        double[] yArray = yDataPair.getValue();
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

    @Deprecated
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

    @Deprecated
    private void outputResults(IntervalState intervalState, INDArray normNewProb) {
        int numParams = intervalState.paramMap.keySet().size();
        int[] shapeArray = new int[numParams];
        for (int i = 0; i < numParams; i++)
            shapeArray[i] = maxCounts;
        int parami = 0;
        for (String param: intervalState.paramMap.keySet()) {
            INDArray outputTable = new NDArray(maxCounts, 2 + normNewProb.shape()[1]);
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

    @Deprecated
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


    @Deprecated
    private INDArray Distr(AST.Id pp, IntervalState intervalState) {
        System.out.println("Distr ID=================");

        if (dataList.containsKey(pp.id)) {
            Pair<AST.Data, double[]> xDataPair = dataList.get(pp.id);
            double[] xArray = xDataPair.getValue();
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

    @Deprecated
    private INDArray Distr(AST.ArrayAccess pp, IntervalState intervalState) {
        System.out.println("Distr ArrayAccess==================");
        // if (dataList.containsKey(pp.id)) {

        // }
        // else {
        int ididx = intervalState.paramMap.get(pp.toString());
        return intervalState.intervalProbPairs.getColumns(ididx, ididx + 1).reshape(-1,1,2);
        // }
    }

    @Deprecated
    private INDArray Distr(AST.Braces pp, IntervalState intervalState) {
        System.out.println("Distr Brace==================");
        return Distr(pp.expression, intervalState);
    }

    @Deprecated
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

    @Deprecated
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

    @Deprecated
    private INDArray[] IndDistr(AST.Expression expr, Double[] paramLimits) {
        // if (rhs instanceof AST.F)
        double[] lower = new double[maxCounts];
        double[] upper = new double[maxCounts];
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
        return new INDArray[]{Nd4j.zeros(DataType.DOUBLE,maxCounts).addi(maxProb), Nd4j.create(lower), Nd4j.create(upper)};


    }

    @Deprecated
    private void getDiscretePriors(double[] lower, double[] upper, ContinuousDistribution normal) {
        int ii = 0;
        //for(double pp=pi; pp <= 1-2*pi; pp += pi) {
        for(double pp=0; pp <= 1-1*maxProb; pp += maxProb) {
            try {
                lower[ii] = normal.inverseCumulativeProbability(pp);
                upper[ii] = normal.inverseCumulativeProbability(pp+maxProb);
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
}
