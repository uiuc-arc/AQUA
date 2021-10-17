package aqua.analyses;

import aqua.AST;
import aqua.cfg.SectionType;
import grammar.analyses.Pair;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math.distribution.*;
import org.apache.commons.math3.distribution.*;
import org.apache.commons.math3.distribution.BetaDistribution;
import org.apache.commons.math3.distribution.CauchyDistribution;
import org.apache.commons.math3.distribution.GammaDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.TDistribution;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.cpu.nativecpu.NDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.BooleanIndexing;
import org.nd4j.linalg.indexing.conditions.Conditions;
import org.nd4j.linalg.ops.transforms.Transforms;


import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;
import java.util.stream.IntStream;

import static java.lang.Math.*;
import static org.nd4j.linalg.ops.transforms.Transforms.exp;
import static org.nd4j.linalg.ops.transforms.Transforms.log;


public class IntervalAnalysis {
    private Map<String, Pair<Double[], ArrayList<Integer>>> paramMap = new HashMap<>();
    // private Map<String, ArrayList<String>> transParamMap = new HashMap<>();
    private Map<String, Integer> paramDivs = new HashMap<>();
    private Map<String, Pair<aqua.AST.Data, double[]>> dataList = new HashMap<>();
    private Set<String> obsDataList = new HashSet<>();
    private Map<String, Integer> scalarParam = new HashMap<>();
    private Queue<aqua.cfg.BasicBlock> worklistAll = new LinkedList<>();
    public int maxCounts;
    private int minCounts = 0;
    private int PACounts = 1;
    private Boolean toAttack=false;
    private String path;
    private Boolean addPrior = true;
    private String stansummary;
    private Boolean no_tau = false;
    public Boolean no_prior = false;
    private Set<String> innerIntParams = new HashSet<>();
    private Stack<String> integrateStack = new Stack<>();
    private HashSet<String> majorParam = new HashSet<>();
    private String dataYNameGlobal;
    private HashSet<String> discreteDist = new HashSet<>(Arrays.asList("bernoulli","flip","categorical","binomial","poisson","atom","uniformClose"));

    @Deprecated
    private double maxProb = 1.0 / (maxCounts - 1);
    @Deprecated
    private double minProb = 1.0 / (minCounts - 1);

    public void setPath(String filepath) {
        if (filepath.substring(filepath.length() - 1).equals("/"))
            path = filepath.substring(0, filepath.length() - 2);
        else
            path = filepath;
    }

    public void setSummaryFile(String stansummarypath) {
        stansummary = stansummarypath;
    }


    public void forwardAnalysis(ArrayList<aqua.cfg.Section> cfgSections) {
        Nd4j.setDataType(DataType.DOUBLE);
        GridState endFacts;
        // GridState.deleteAnalysisOutputs(path);
        ArrayList<aqua.cfg.BasicBlock> worklist = new ArrayList<>();
        InitWorklist(cfgSections, worklist);
        // getMeanFromMCMC();
        addPrior = true;
        no_tau = true;
        // toAttack = true;
        for (String kk : paramMap.keySet()) {
            if (kk.contains("robust")) {
                paramDivs.put(kk, maxCounts);
                if (!(kk.contains("robust_local") || kk.contains("robust_weight")))
                    majorParam.add(kk);
            }
            else {
                paramDivs.put(kk, maxCounts);
                if (kk.contains("timeseries_mu")) {
                    if (kk.contains("[1]"))
                        majorParam.add(kk);
                }
                else
                    majorParam.add(kk);
            }
        }
        // worklist.add(worklistAll.peek());
        endFacts = WorklistIter(worklist);
        if (endFacts.probCube == null) {
            System.out.println("Prob Cube Empty!");
        }// else {
            //System.out.println("End Prob Cube Shape:" + Nd4j.createFromArray(endFacts.probCube[0].shape()));
            // System.out.println(majorParam);
            // System.out.println(path);
        // }
        // endFacts.writeResults(majorParam, path);
        endFacts.writeResults(majorParam, path);
        // endFacts.writeMathe(majorParam, path);
        // repeat

    }

    public void repeatAna() {
        //TODO
        if (maxCounts == 61) {
            int paramSize = paramMap.size();
            if (paramSize == 1)
                maxCounts = 40401;
            if (paramSize == 2)
                maxCounts = 201;
            else if (paramSize == 3 && paramMap.containsKey("Y") && paramMap.containsKey("Z"))
                maxCounts = 40401;
        }
        System.out.println("Current Splits: " + String.valueOf(maxCounts - 1));
        for (String kk : paramMap.keySet()) {
            paramDivs.put(kk, maxCounts);
        }
        ArrayList<aqua.cfg.BasicBlock> worklist = new ArrayList<>();
        for (aqua.cfg.BasicBlock bb: worklistAll) {
            bb.dataflowFacts = null;
        }
        scalarParam.clear();
        // toAttack = false;
        // Pair<AST.Data, double[]> dataYGood = dataList.get(dataYNameGlobal + "_good");
        // if (!toAttack) {
        //     dataList.put(dataYNameGlobal, dataYGood);
        //     dataList.put(dataYNameGlobal + "_corrupted", dataYGood);
        // }
        worklist.add(worklistAll.peek());
        GridState endFacts = WorklistIter(worklist);
        endFacts.writeResults(majorParam, path);
    }

    public boolean getNewRange() {
        boolean single = false;
        for (String paramName: majorParam) {
            String txt1File = path + "/analysis_" + paramName + ".txt";
            if (paramName.equals("Y") || paramName.equals("Z"))
                continue;
            INDArray lastOut = Nd4j.readTxt(txt1File);
            INDArray lastVal = lastOut.slice(0);
            INDArray lastProb = lastOut.slice(1);
            Double lowProb = (double) lastProb.maxNumber() * 0.01;
            Integer firstGt = BooleanIndexing.firstIndex(lastProb, Conditions.greaterThanOrEqual(lowProb)).getInt();
            Integer lastGt = BooleanIndexing.lastIndex(lastProb, Conditions.greaterThanOrEqual(lowProb)).getInt();
            if (abs(firstGt - lastGt) < 2)
                single = true;
            Double[] meanSd = paramMap.get(paramName).getKey();
            meanSd[0] = lastVal.getDouble(max(firstGt-1, 0));
            meanSd[1] = lastVal.getDouble(min(lastGt+1, lastVal.length() - 1));
        }
        return single;
    }

    /*
    @Deprecated
    public void forwardAnalysisByGroups(ArrayList<Section> cfgSections) {
        Nd4j.setDataType(DataType.DOUBLE);
        InitWorklist(cfgSections);
        ArrayList<Set<String>> paramGroups = GroupParams(cfgSections);
        addRobustToGroups(paramGroups);
        System.out.println(paramGroups);
        System.out.println(innerIntParams);
        getMeanFromMCMC();
        // if (paramGroups.size() > 1)
        // PACounts = 1;
        GridState endFacts;
        GridState.deleteAnalysisOutputs(path);
        ArrayList<BasicBlock> worklist = new ArrayList<>();
        // Pre-Analysis: Run a Worklist Algorithm
        // toAttack = false;
        // worklist.add(worklistAll.peek());
        // endFacts = WorklistIter(worklist);
        // Again with min splits to find max interval
        // toAttack = false;
        // minCounts = 0;
        // maxCounts = 21;
        for (String kk : paramMap.keySet())
            paramDivs.put(kk, minCounts);
        // if (endFacts != null) {
        //     for (String kk: endFacts.paramValues.keySet()) {
        //         if (kk.equals("Datai") || transParamMap.containsKey(kk.split("\\[")[0]) || (!paramMap.containsKey(kk)))
        //             continue;
        //         paramDivs.put(kk, minCounts);
        //     }
        // }
        Set<String> prevKk = null;
        // addPrior = true;
        // for (Set<String> paramSet: paramGroups) {
        //     if (prevKk != null) {
        //         for (String param: prevKk) {
        //             paramDivs.put(param, minCounts);
        //         }
        //     }
        //     for (String param: paramSet) {
        //         paramDivs.put(param, maxCounts);
        //         if (param.contains("robust_"))
        //             paramDivs.put(param, 11);
        //     }
        //     for (BasicBlock bb: worklistAll) {
        //         bb.dataflowFacts = null;
        //     }
        //     scalarParam.clear();
        //     System.gc();
        //     prevKk = paramSet;
        //     worklist.add(worklistAll.peek());
        //     endFacts = WorklistIter(worklist);
        //     if (endFacts.probCube.size() > 0) {
        //         // System.out.println("End Prob Cube Shape:" + Nd4j.createFromArray(endFacts.probCube.get(0).shape()));
        //         for (String kk: endFacts.paramValues.keySet()) {
        //             if (kk.equals("Datai") || transParamMap.containsKey(kk.split("\\[")[0]) || (!paramMap.containsKey(kk)))
        //                 continue;
        //             Pair<Integer, INDArray> kkResults = endFacts.paramValues.get(kk);
        //             if (kkResults.getKey()==null || kkResults.getValue().length() == 0)
        //                 continue;
        //             Pair<Double[], ArrayList<Integer>> limitsDims = paramMap.get(kk);
        //             Double[] limits = limitsDims.getKey();
        //             double newMean =endFacts.getResultsMean(kk);
        //             if ((limits[2] != null  && abs(newMean) < abs(limits[2]))
        //                 ||  (limits[1] == null && abs(newMean) < 25)) {
        //                 limits[2] = newMean;
        //             }
        //             paramDivs.put(kk, minCounts);
        //         }
        //     }
        //     else
        //         System.out.println("Prob Cube Empty!");
        //     addPrior = false;
        // }
        toAttack = true;
        minCounts = 0;
        maxCounts =41;
        prevKk = null;
        addPrior = true;
        for (Set<String> paramSet : paramGroups) {
            if (prevKk != null) {
                for (String param : prevKk) {
                    paramDivs.put(param, minCounts);
                }
            }
            for (String param : paramSet) {
                paramDivs.put(param, maxCounts);
                if (param.contains("robust_"))
                    paramDivs.put(param, 11);
            }
            for (BasicBlock bb : worklistAll) {
                bb.dataflowFacts = null;
            }
            scalarParam.clear();
            System.gc();
            prevKk = paramSet;
            worklist.add(worklistAll.peek());
            endFacts = WorklistIter(worklist);
            if (endFacts.probCube[0] != null) {
                // System.out.println("End Prob Cube Shape:" + Nd4j.createFromArray(endFacts.probCube.get(0).shape()));
            } else
                System.out.println("Prob Cube Empty!");
            endFacts.writeResults(new HashSet<>(paramSet), path);
            addPrior = false;
        }
        // Analysis by individual params
        // String prevKk = null;
        // for (String kk: paramMap.keySet()) {
        //     paramDivs.put(kk, maxCounts);
        //     if (prevKk != null) {
        //         paramDivs.put(prevKk, minCounts);
        //     }
        //     for (BasicBlock bb: worklistAll) {
        //         bb.dataflowFacts = null;
        //     }
        //     scalarParam.clear();
        //     System.gc();
        //     prevKk = kk;
        //     worklist.add(worklistAll.peek());
        //     endFacts = WorklistIter(worklist);
        //     if (endFacts.probCube.size() > 0)
        //         System.out.println(Nd4j.createFromArray(endFacts.probCube.get(0).shape()));
        //     else
        //         System.out.println("Prob Cube Empty!");
        //     endFacts.writeResults(new HashSet<>(Collections.singletonList(kk)));
        // }
    }
    */

    private void getMeanFromMCMC() {
        if (stansummary == null)
            return;
        Map<String, String[]> records = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(stansummary))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("#"))
                    continue;
                String[] values = line.split(",");
                if (values[0].contains("__"))
                    continue;
                records.put(values[0].replace("\"", ""), new String[]{values[1], values[3]});
            }
        } catch (Exception e) {

            e.printStackTrace();

        }
        for (Map.Entry<String, Pair<Double[], ArrayList<Integer>>> pp : paramMap.entrySet()) {
            Double[] paramLimits = pp.getValue().getKey();
            String strMeanSd[] = records.get(pp.getKey());
            if (strMeanSd != null) {
                paramLimits[2] = Double.valueOf(strMeanSd[0]);
                paramLimits[3] = Double.valueOf(strMeanSd[1]); // max(Double.valueOf(strMeanSd[1]), 0.5);
                // System.out.println(pp.getKey() + " " + strMeanSd[0] + " " + strMeanSd[1]);
            }
        }
    }

    /*
    @Deprecated
    private ArrayList<Set<String>> GroupParams(ArrayList<Section> cfgSections) {
        ArrayList<Set<String>> groups = new ArrayList<>();
        for (Section section : cfgSections) {
            // System.out.println(section.sectionType);
            if (section.sectionType == SectionType.DATA) {
                ArrayList<AST.Data> dataSets = section.basicBlocks.get(0).getData();
            } else if (section.sectionType == SectionType.FUNCTION) {
                // System.out.println(section.sectionName);
                for (BasicBlock basicBlock : section.basicBlocks) {
                    for (Statement statement : basicBlock.getStatements()) {
                        if (statement.statement instanceof AST.AssignmentStatement) {
                            AST.AssignmentStatement assignment = (AST.AssignmentStatement) statement.statement;
                            addGroupsFromAssignment(groups, assignment);
                        }
                        else if (statement.statement instanceof AST.FunctionCallStatement) {
                            // System.out.println("FunctionCall: " + statement.statement.toString());

                        } else if (statement.statement instanceof AST.IfStmt) {
                            AST.IfStmt ifStmt = (AST.IfStmt) statement.statement;
                        } else if (statement.statement instanceof AST.ForLoop) {
                            AST.ForLoop forLoop = (AST.ForLoop) statement.statement;
                        }
                    }
                }
            } else if (section.sectionType == SectionType.NAMEDSECTION) {
                if (section.sectionName.equals("transformedparam")){
                    for (BasicBlock basicBlock : section.basicBlocks) {
                        for (Statement statement : basicBlock.getStatements()) {
                            if (statement.statement instanceof AST.AssignmentStatement) {
                                AST.AssignmentStatement assignment = (AST.AssignmentStatement) statement.statement;
                                // because already added, do nothing
                                // addGroupsFromAssignment(groups, assignment);
                            }
                        }
                    }
                }
            }
        }
        // Set<String> firstGroup = new HashSet<>(groups.get(0));
        // Set<String> firstGroupDup = new HashSet<>(groups.get(0));
        // for (String pp: firstGroup) {
        //     if (!pp.contains("[") && paramMap.get(pp + "[1]") != null && paramMap.get(pp + "[1]").getValue().size() > 0) {
        //         groups.remove(0);
        //         ArrayList<Integer> dims = paramMap.get(pp + "[1]").getValue();
        //         firstGroupDup.remove(pp);
        //         for (int i = 0; i < dims.get(0); i++) {
        //             Set<String> newfirstGroup = new HashSet<>(firstGroupDup);
        //             newfirstGroup.add(String.format("%s[%s]", pp, i + 1));
        //             groups.add(newfirstGroup);
        //         }
        //     }
        // }
        return groups;
    }
    */

    /*
    @Deprecated
    private void addRobustToGroups(ArrayList<Set<String>> groups) {
        if (paramMap.containsKey("robust_local_nu")) {
            ArrayList<Set<String>> newGroups = new ArrayList<>();
            Pair<Double[], ArrayList<Integer>> tauDim = paramMap.get("robust_local_tau[1]");
            for (Set<String> gg: groups) {
                for (int i=0; i< tauDim.getValue().get(0); i++) {
                    Set<String> ggDup = new HashSet<>(gg);
                    ggDup.add(String.format("robust_local_tau[%s]", i+1));
                    newGroups.add(ggDup);
                }
            }
            if (newGroups.get(0).size() <= 3) {
                for (Set<String> ng : newGroups) //))))))))))))))))
                    ng.add("robust_local_nu");
            } else {
                no_tau = true;
            }
            groups.clear();
            groups.addAll(newGroups);
            innerIntParams.add("robust_local_tau");

        }
        else if (paramMap.containsKey("robust_weight[1]")) {
            ArrayList<Set<String>> newGroups = new ArrayList<>();
            Pair<Double[], ArrayList<Integer>> tauDim = paramMap.get("robust_weight[1]");
            for (Set<String> gg: groups) {
                for (int i=0; i< tauDim.getValue().get(0); i++) {
                    Set<String> ggDup = new HashSet<>(gg);
                    ggDup.add(String.format("robust_weight[%s]", i+1));
                    newGroups.add(ggDup);
                }
            }
            groups.clear();
            groups.addAll(newGroups);
            innerIntParams.add("robust_weight");
        }
    }
    */

    // private void addRobustToGroups(ArrayList<Set<String>> groups) {
    //     System.out.println(groups);
    //     System.out.println(robustParams);
    //     for (String param: robustParams) {
    //         if (paramMap.containsKey(param)) {
    //             for (Set<String> gg: groups) {
    //                 gg.add(param);
    //             }
    //         } else {
    //             String paramID = param.split("\\[")[0];
    //             Pair<Double[], ArrayList<Integer>> paramInfo = paramMap.get(paramID + "[1]");
    //             ArrayList<Integer> dims = paramInfo.getValue();
    //             if (dims.get(0) > groups.size()) {
    //                 ArrayList<Set<String>> newGroupsDup = new ArrayList<>(groups);
    //                 groups.clear();
    //                 while (newGroupsDup.size() > 0) {
    //                     Set<String> onlyGroup = newGroupsDup.remove(0);
    //                     for (int i = 1; i <= dims.get(0); i++) {
    //                         Set<String> dupNewGroup = new HashSet<>(onlyGroup);
    //                         dupNewGroup.add(String.format("%s[%s]", paramID, i));
    //                         groups.add(dupNewGroup);
    //                     }
    //                 }
    //             } else {
    //                 for (int i = 1; i <= dims.get(0); i++) {
    //                     groups.get(i - 1).add(String.format("%s[%s]", paramID, i));
    //                 }
    //             }
    //         }
    //     }

    // }

    /*
    private void addGroupsFromAssignment(ArrayList<Set<String>> groups, AST.AssignmentStatement assignment) {
        ArrayList<String> rhsParams = extractParamsFromExpr(assignment.rhs);
        rhsParams.removeAll(Collections.singleton("robust_local_tau"));
        rhsParams.removeAll(Collections.singleton("robust_local_nu"));
        rhsParams.removeAll(Collections.singleton("robust_weight"));
        String lhsParam = assignment.lhs.toString();
        if (lhsParam.equals("target") && assignment.rhs.toString().contains("log_mix")) {
            groups.add(new HashSet<>(rhsParams));
            return;
        } else if (lhsParam.equals("target")) {
            lhsParam = assignment.rhs.toString().split("_")[1].split(",")[0].split("\\(")[1];
        }
        ArrayList<Set<String>> newGroups = new ArrayList<>();
        for (String param: rhsParams) {
            if (newGroups.isEmpty()) {
                if (paramMap.containsKey(param)) {
                    Set<String> newGroup = new HashSet<>();
                    newGroup.add(param);
                    newGroups.add(newGroup);
                } else {
                    String paramID = param.split("\\[")[0];
                    Pair<Double[], ArrayList<Integer>> paramInfo = paramMap.get(paramID + "[1]");
                    ArrayList<Integer> dims = paramInfo.getValue();
                    for (int i=1; i<= dims.get(0); i++) {
                        Set<String> newGroup = new HashSet<>();
                        newGroup.add(String.format("%s[%s]", paramID, i));
                        newGroups.add(newGroup);
                    }
                }
            } else {
                if (paramMap.containsKey(param)) {
                    for (Set<String> currGroup : newGroups) {
                        currGroup.add(param);
                    }
                } else {
                    String paramID = param.split("\\[")[0];
                    Pair<Double[], ArrayList<Integer>> paramInfo = paramMap.get(paramID + "[1]");
                    ArrayList<Integer> dims = paramInfo.getValue();
                    if (newGroups.size() > 1) {
                        for (int i = 1; i <= dims.get(0); i++) {
                            newGroups.get(i - 1).add(String.format("%s[%s]", paramID, i));
                        }
                    }
                    else { // size() == 1
                        Set<String> onlyGroup = newGroups.get(0);
                        newGroups.clear();
                        for (int i = 1; i <= dims.get(0); i++) {
                            Set<String> dupNewGroup = new HashSet<>(onlyGroup);
                            dupNewGroup.add(String.format("%s[%s]", paramID, i));
                            newGroups.add(dupNewGroup);
                        }

                    }
                }
            }
        }
        // System.out.println("==============");
        // System.out.println(assignment.toString());
        // System.out.println(groups);
        // System.out.println(rhsParams);
        // System.out.println(transParamMap.keySet());
        if (paramMap.containsKey(lhsParam)
                && (!transParamMap.containsKey(lhsParam.split("\\[")[0]))
                && (!transParamMap.containsKey(lhsParam))) {
            for (Set<String> currGroup : newGroups) {
                currGroup.add(lhsParam);
                Boolean added = false;
                for (Set<String> oldGroup: groups) {
                    if (isGroupOverlaping(oldGroup, currGroup)) {
                        oldGroup.addAll(currGroup);
                        added = true;
                    }
                }
                if (!added) {
                    groups.add(currGroup);
                }
            }
        } else if (paramMap.containsKey(lhsParam.split("\\[")[0] + "[1]")
                && (!transParamMap.containsKey(lhsParam.split("\\[")[0]))
                && (!transParamMap.containsKey(lhsParam.split("\\[")[0] + "[1]"))){
            String paramID = lhsParam.split("\\[")[0];
            Pair<Double[], ArrayList<Integer>> paramInfo = paramMap.get(paramID + "[1]");
            ArrayList<Integer> dims = paramInfo.getValue();
            if (newGroups.size() == dims.get(0)) {
                for (int i = 1; i <= dims.get(0); i++) {
                    newGroups.get(i - 1).add(String.format("%s[%s]", paramID, i));
                }
                groups.addAll(newGroups);
            } else if (newGroups.size() == 1) {
                for (int i = 1; i <= dims.get(0); i++) {
                    Set<String> dupNewGroup = new HashSet<>(newGroups.get(0));
                    dupNewGroup.add(String.format("%s[%s]", paramID, i));
                    groups.add(dupNewGroup);
                }
            }
        } else { // lhs is data or transparam
            if (!dataList.containsKey(lhsParam.split("\\[")[0])) { // transparam
                if (!transParamMap.containsKey(lhsParam.split("\\[")[0])) {
                    transParamMap.put(lhsParam.split("\\[")[0], rhsParams);
                    // System.out.println(transParamMap);
                }
            } else {
                ArrayList<Set<String>> oldGroups = new ArrayList<>(groups.size());
                for (Set<String> group : groups) {
                    oldGroups.add(new HashSet<>(group));
                }
                for (Set<String> currGroup : newGroups) {
                    Boolean added = false;
                    for (int i=0; i< oldGroups.size(); i++) {
                        if (isGroupOverlaping(oldGroups.get(i), currGroup)) {
                            groups.get(i).addAll(currGroup);
                            added = true;
                        }
                    }
                    if (!added) {
                        groups.add(currGroup);
                    }
                }
            }
        }
    }
    */

    /*
    private boolean isGroupOverlaping(Set<String> oldGroup, Set<String> currGroup) {
        return !Sets.intersection(oldGroup, currGroup).isEmpty();
    }

    private ArrayList<String> extractParamsFromExpr(AST.Expression rhs) {
        ArrayList<String> retParams = new ArrayList<>();
        if (rhs instanceof AST.FunctionCall) {
            AST.FunctionCall rhsFunctionCall = (AST.FunctionCall) rhs;
            for (AST.Expression arg: rhsFunctionCall.parameters)
                retParams.addAll(extractParamsFromExpr(arg));
        }
        else if (rhs instanceof AST.Id) {
            AST.Id rhsId = (AST.Id) rhs;
            if ((paramMap.containsKey(rhsId.id) || paramMap.containsKey(rhsId.id + "[1]"))
                    && (!(transParamMap.containsKey(rhsId.id) || transParamMap.containsKey(rhsId.id + "[1]")))){
                retParams.add(rhsId.id);
            } else if (transParamMap.containsKey(rhsId.id)) {
                retParams.addAll(transParamMap.get(rhsId.id));
            } // else if (transParamMap.containsKey(rhsId.id + "[1]")) {
            // }
        }
        else if (rhs instanceof AST.AddOp) {
            AST.AddOp rhsAddOp = (AST.AddOp) rhs;
            retParams.addAll(extractParamsFromExpr(rhsAddOp.op1));
            retParams.addAll(extractParamsFromExpr(rhsAddOp.op2));
        }
        else if (rhs instanceof AST.MulOp) {
            AST.MulOp rhsMulOp = (AST.MulOp) rhs;
            retParams.addAll(extractParamsFromExpr(rhsMulOp.op1));
            retParams.addAll(extractParamsFromExpr(rhsMulOp.op2));
        }
        else if (rhs instanceof AST.DivOp) {
            AST.DivOp rhsDivOp = (AST.DivOp) rhs;
            retParams.addAll(extractParamsFromExpr(rhsDivOp.op1));
            retParams.addAll(extractParamsFromExpr(rhsDivOp.op2));
        }
        else if (rhs instanceof AST.Braces) {
            retParams.addAll(extractParamsFromExpr(((AST.Braces) rhs).expression));
        }
        else if (rhs instanceof AST.ArrayAccess) {
            AST.ArrayAccess rhsArrayAccess = (AST.ArrayAccess) rhs;
            if (paramMap.containsKey(rhsArrayAccess.toString())
                    && ! transParamMap.containsKey(rhsArrayAccess.id.id)) {
                retParams.add(rhsArrayAccess.toString());
            }
            else if (paramMap.containsKey(rhsArrayAccess.id.id + "[1]")
                    && ! transParamMap.containsKey(rhsArrayAccess.id.id)) {
                retParams.add(rhsArrayAccess.id.id); // then assume idp of different elements in array
            }
            else if (transParamMap.containsKey(rhsArrayAccess.toString())) {
                retParams.addAll(transParamMap.get(rhsArrayAccess.toString()));
            }
            else if (transParamMap.containsKey(rhsArrayAccess.id.id)) {
                retParams.addAll(transParamMap.get(rhsArrayAccess.id.id)); // same as their dependent param
            }
        }
        return retParams;
    }
    */

    private GridState WorklistIter(ArrayList<aqua.cfg.BasicBlock> worklist) {
        GridState endFacts = null;
        aqua.cfg.BasicBlock currBlock;
        Set<Integer> visited = new HashSet<>();
        while (!worklist.isEmpty()) {
            currBlock = worklist.remove(0);
            Map<String, aqua.cfg.BasicBlock> nameIncoming = currBlock.getIncomingEdges();
            if (nameIncoming.size() > 1 && ! nameIncoming.keySet().contains("back")) {
                boolean unvisitPred = false;
                for (aqua.cfg.BasicBlock bb : nameIncoming.values()) {
                    if (!visited.contains(bb.getId()))
                        unvisitPred = true;
                }
                if (unvisitPred) {
                    worklist.add(currBlock);
                    continue;
                }
            }
            visited.add(currBlock.getId());
            // Join and prepare to analyze block
            if (currBlock.getIncomingEdges().keySet().contains("meetT")) {
                Map<String, aqua.cfg.BasicBlock> incoming = currBlock.getIncomingEdges();
                GridState dfT = null, dfF = null;
                for (String cond : incoming.keySet()) {
                    // get true
                    if (cond.equals("meetT"))
                        dfT = incoming.get(cond).dataflowFacts;
                    else if (cond.equals("meetF"))
                        dfF = incoming.get(cond).dataflowFacts;
                    else if (cond.equals("false"))
                        dfF = currBlock.dataflowFacts;
                }
                // join
                dfT.join(dfF.probCube);
                currBlock.dataflowFacts = dfT;
            }
            // System.out.println("//////////// Analyze block: " + currBlock.getId());
            Boolean changed = BlockAnalysisCube(currBlock);
            endFacts = currBlock.dataflowFacts;
            // Get marginal changes
            // INDArray tmp = Transforms.exp(endFacts.probCube);
            // int[] tmpint = new int[tmp.shape().length - 1];
            // for (int tt=2; tt < tmpint.length + 1; tt++)
            //     tmpint[tt-1] = tt;
            // //tmpint[0] = 1;
            // tmpint[tmpint.length -1] = 2;
            // System.out.println(Nd4j.createFromArray(tmpint));
            // System.out.println(tmp.sum(tmpint));
            Map<String, aqua.cfg.BasicBlock> succs = currBlock.getOutgoingEdges();
            if (succs.containsKey("true") && succs.containsKey("false") && (!currBlock.getIncomingEdges().containsKey("back"))) {
                    // if else union from previous results
                aqua.AST.IfStmt ifCond = (aqua.AST.IfStmt) currBlock.getLastStatement().statement;
                INDArray condCube = DistrCube(ifCond.condition, endFacts);
                if (condCube.length() > 1) {
                    aqua.cfg.BasicBlock succFalse = succs.get("false");
                    succFalse.dataflowFacts = endFacts.clone();
                    succFalse.dataflowFacts.meet(condCube, false); // meetF
                    worklist.add(0, succFalse);

                    aqua.cfg.BasicBlock succTrue = succs.get("true");
                    succTrue.dataflowFacts = endFacts;
                    succTrue.dataflowFacts.meet(condCube, true); // meetT
                    worklist.add(0, succTrue);
                } else { // deterministic
                    if (condCube.getDouble(0) == 0) {
                        aqua.cfg.BasicBlock succFalse = succs.get("false");
                        worklist.add(0, succFalse);
                    }
                    else {
                        aqua.cfg.BasicBlock succTrue = succs.get("true");
                        worklist.add(0, succTrue);
                    }
                }

            } else if (succs.containsKey("meetT")) {
                aqua.cfg.BasicBlock meetT = succs.get("meetT");
                worklist.add(meetT);
                // Collection<BasicBlock> tfBlocks = meetT.getIncomingEdges().values();
                // System.out.println("meetT ID " + meetT.getId());
                // System.out.println(meetT.getIncomingEdges().keySet());
                // int max = 0;
                // System.out.println(worklist);
                // for (BasicBlock bb:tfBlocks) {
                //     if (worklist.contains(bb))
                //         max = max(max, worklist.indexOf(bb) + 1);
                //     else if (!visited.contains(bb.getId()))
                //         max = worklist.size();
                // }
                // System.out.println(max);
            } else {
                for (String cond : succs.keySet()) {
                    aqua.cfg.BasicBlock succ = succs.get(cond);
                    succ.dataflowFacts = endFacts;
                    if (changed) {
                        if (cond == null)
                            worklist.add(succ);
                        else if ((cond.equals("back") || cond.equals("true")))
                            worklist.add(0, succ);
                    } else if (cond != null && cond.equals("false")) {
                        worklist.add(succ);
                    }
                }
            }
        }
        return endFacts;
    }


    // private GridState ForwardDFS(BasicBlock entryBlock) {
    //     GridState endFacts;
    //     Boolean changed = BlockAnalysisCube(entryBlock);
    //     endFacts = entryBlock.dataflowFacts;
    //     Map<String, BasicBlock> succs = entryBlock.getOutgoingEdges();
    //     if (changed) {
    //         for (String cond : succs.keySet()) {
    //             BasicBlock succ = succs.get(cond);
    //             if (cond == null) {
    //                 succ.dataflowFacts = endFacts;
    //                 changed = BlockAnalysisCube(succ);
    //                 endFacts = succ.dataflowFacts;
    //             } else if (cond.equals("true")) {
    //                 succ.dataflowFacts = endFacts;
    //                 changed = BlockAnalysisCube(succ);
    //                 endFacts = succ.dataflowFacts;
    //             }
    //         }
    //     }
    //     return endFacts;
    // }

    private void InitWorklist(ArrayList<aqua.cfg.Section> cfgSections, ArrayList<aqua.cfg.BasicBlock> worklist) {
        for (aqua.cfg.Section section : cfgSections) {
            // System.out.println(section.sectionType);
            if (section.sectionType == aqua.cfg.SectionType.DATA) {
                ArrayList<aqua.AST.Data> dataSets = section.basicBlocks.get(0).getData();
                int dataDivConst = 1;
                for (aqua.AST.Data dd: dataSets) {
                    String dataString = Utils.parseData(dd, 'f');
                    dataString = dataString.replaceAll("\\s", "").replaceAll("\\[", "").replaceAll("\\]", "").replaceAll("\\.0,",",").replaceAll("\\.0$","");
                    double[] dataArray = Arrays.stream(dataString.split(",")).mapToDouble(Double::parseDouble).toArray();
                    if (dataArray.length == 1) {
                        dataArray[0] = dataArray[0] / dataDivConst;
                    } else {
                        double[] newDataArray = new double[dataArray.length / dataDivConst];
                        System.arraycopy(dataArray, 0, newDataArray, 0, newDataArray.length);
                        dataArray = newDataArray;
                    }
                    dataList.put(dd.decl.id.id, new Pair(dd, dataArray));
                }

            } else if(section.sectionType == SectionType.FUNCTION) {
                // System.out.println(section.sectionName);
                if (section.sectionName.equals("main")) {
                    worklist.add(section.basicBlocks.get(0));
                    for (aqua.cfg.BasicBlock basicBlock: section.basicBlocks) {
                        worklistAll.add(basicBlock);
                        // If no attack!!!
                        for (aqua.cfg.Statement statement : basicBlock.getStatements()) {
                            if (statement.statement instanceof aqua.AST.Decl) {
                                addParams(statement);
                            }
                            /*
                            ArrayList<AST.Annotation> annotations = statement.statement.annotations;
                            if (annotations != null && !annotations.isEmpty() &&
                                    annotations.get(0).annotationType == AST.AnnotationType.Observe) {
                                if (statement.statement instanceof AST.AssignmentStatement) {
                                    AST.AssignmentStatement assignment = (AST.AssignmentStatement) statement.statement;
                                    String dataYName = assignment.lhs.toString().split("\\[")[0];
                                    attackDataY(dataYName);
                                }
                            }
                            if (statement.statement instanceof AST.AssignmentStatement) {
                                AST.AssignmentStatement assignment = (AST.AssignmentStatement) statement.statement;
                                // if (! (assignment.rhs instanceof AST.FunctionCall)) {
                                //     // ArrayList<String> rhsParams = extractParamsFromExpr(assignment.rhs);
                                //    //  String lhsParam = assignment.lhs.toString();
                                //    // transParamMap.put(lhsParam.split("\\[")[0], rhsParams);
                                // }else {
                                //     ArrayList<String> rhsParams = extractParamsFromExpr(assignment.rhs);
                                //     String lhsParam = assignment.lhs.toString().split("\\[")[0];
                                //     if (rhsParams.size() > 0 && !lhsParam.equals("target")) {
                                //         rhsParams.add(lhsParam);
                                //         transParamMap.put(lhsParam, rhsParams);
                                //     }
                                // }

                                if (assignment.lhs.toString().equals("target")) {
                                    String rhs = assignment.rhs.toString();
                                    if (rhs.contains("_lpdf(")) {
                                        String dataYName = rhs.split("_lpdf\\(")[1].split(",")[0];
                                        if (dataList.containsKey(dataYName))
                                            attackDataY(dataYName.split("\\[")[0]);
                                    }
                                    else if (rhs.contains("_lpmf(")) {
                                        String dataYName = rhs.split("_lpmf\\(")[1].split(",")[0];
                                        if (dataList.containsKey(dataYName))
                                            attackDataY(dataYName.split("\\[")[0]);
                                    }
                                    else
                                        attackDataY("y");
                                }
                            }
                            */
                        }
                    }
                }

            } // else if(section.sectionType == SectionType.QUERIES) {
                // for (BasicBlock basicBlock: section.basicBlocks)
                    // for (Statement statement : basicBlock.getStatements());
                        // System.out.println(statement.statement.toString());
            // } else if (section.sectionName.equals("transformedparam")) {
              //  for (BasicBlock basicBlock : section.basicBlocks) {
                   //  for (Statement statement : basicBlock.getStatements()) {
                        // if (statement.statement instanceof AST.AssignmentStatement) {
                            // AST.AssignmentStatement assignment = (AST.AssignmentStatement) statement.statement;
                            // ArrayList<String> rhsParams = extractParamsFromExpr(assignment.rhs);
                           //  String lhsParam = assignment.lhs.toString();
                            // transParamMap.put(lhsParam.split("\\[")[0], rhsParams);
                       //  }
                   //  }
              //  }
           //  }
        }
    }

    /*
    private void attackDataY(String dataYName){
        obsDataList.add(dataYName);
        Pair<AST.Data, double[]> orgDataPair = dataList.get(dataYName);
        double[] newDataValueL = orgDataPair.getValue(); // .clone();
        // double[] newDataValueU = orgDataPair.getValue().clone();
        // Attack
        // getAttackLU(newDataValueL, newDataValueU);
        dataYNameGlobal = dataYName;
        dataList.put(dataYName, new Pair<>(orgDataPair.getKey(), newDataValueL));
        // dataList.put(dataYName + "_corrupted", new Pair<>(orgDataPair.getKey(), newDataValueU));
        // dataList.put(dataYName + "_good", new Pair<>(orgDataPair.getKey(), orgDataPair.getValue()));
    }
    */

    private void getAttackLU(double[] newDataValueL, double[] newDataValueU) {
        double sd = getStdDev(newDataValueL);

        Double[] orgDataValue = ArrayUtils.toObject(newDataValueL);
        int size = orgDataValue.length;
        int[] sortedIndices = IntStream.range(0, size)
                .boxed().sorted(Comparator.comparing(i -> orgDataValue[i]))
                .mapToInt(ele -> ele).toArray();
        newDataValueU[sortedIndices[0]] -= 6 * sd;
        newDataValueU[sortedIndices[size - 1]] += 6 * sd;
        // for(int i = 0; i < size * 0.01; i++){
        //     newDataValueU[sortedIndices[i]] -= 3 * sd;
        // }
        // for(int i = (int) (size * (0.99)); i < size; i++){
        //     newDataValueU[sortedIndices[i]] += 3 * sd;
        // }
    }

    double getMean(double[] data) {
        double sum = 0.0;
        for(double a : data)
            sum += a;
        return sum/data.length;
    }

    double getVariance(double[] data) {
        double mean = getMean(data);
        double temp = 0;
        for(double a :data)
            temp += (a-mean)*(a-mean);
        return temp/(data.length-1);
    }

    double getStdDev(double[] data) {
        return Math.sqrt(getVariance(data));
    }

    private void getAttack(double[] newDataValue) {
        double sd = getSd(newDataValue);
        for (int i=0; i<newDataValue.length; i+= 12) { // 5 *
            newDataValue[i] = newDataValue[i] + 2 * Math.sqrt(sd/(double) newDataValue.length);
        }
    }

    private double getSd(double[] newDataValue) {
        double sd = 0;
        double sum = 0;
        for (int i=0; i<newDataValue.length;i++) {
            sum += newDataValue[i];
        }
        sum = sum / (double) newDataValue.length;
        for (int i=0; i<newDataValue.length;i++) {
            sd = sd + Math.pow(newDataValue[i] - sum, 2);
        }
        return sqrt(sd);
    }

    private Boolean BlockAnalysisCube(aqua.cfg.BasicBlock basicBlock) {
        Boolean changed = false;
        GridState intervalState;
        if (basicBlock.dataflowFacts == null)
            intervalState = new GridState();
        else
            intervalState = basicBlock.dataflowFacts;
        for (aqua.cfg.Statement statement : basicBlock.getStatements()) {
            if (statement.statement instanceof aqua.AST.Decl) {
                // System.out.println("Decl: " + statement.statement.toString());
                // addParams(statement);
                // System.out.println(statement.statement.toString());
                changed = true;

            } else if (statement.statement instanceof aqua.AST.AssignmentStatement) {
                // changed always true
                changed = analyzeAssignment(intervalState, statement);
            } else if (statement.statement instanceof aqua.AST.FunctionCallStatement) {
                if (statement.statement.toString().startsWith("hardObserve")) {
                    analyzeObserve(intervalState, statement);
                    changed = true;
                }

            } else if (statement.statement instanceof aqua.AST.IfStmt) {
                aqua.AST.IfStmt ifStmt = (aqua.AST.IfStmt) statement.statement;
                // BlockAnalysis(ifStmt.BBtrueBlock);
                // BlockAnalysis(ifStmt.BBelseBlock);
            } else if (statement.statement instanceof aqua.AST.ForLoop) {
                aqua.AST.ForLoop forLoop = (aqua.AST.ForLoop) statement.statement;
                // System.out.println("ForLoop: "+ statement.statement);
                changed = incLoop(forLoop);
            }
            basicBlock.dataflowFacts = intervalState;
        }
        return changed;
    }

    private void analyzeObserve(GridState intervalState, aqua.cfg.Statement statement) {
        aqua.AST.FunctionCall funcStat = ((aqua.AST.FunctionCallStatement) statement.statement).functionCall;
        INDArray hardCond = DistrCube(funcStat.parameters.get(0), intervalState);
        INDArray loghardCond  = Transforms.log(hardCond);
        intervalState.addProb(loghardCond);
    }

    private Boolean analyzeAssignment(GridState intervalState, aqua.cfg.Statement statement) {
        Boolean changed = true;
        ArrayList<aqua.AST.Annotation> annotations = statement.statement.annotations;
        aqua.AST.AssignmentStatement assignment = (aqua.AST.AssignmentStatement) statement.statement;
        if (annotations != null && !annotations.isEmpty() &&
                annotations.get(0).annotationType == aqua.AST.AnnotationType.Observe) {
            // System.out.println("Observe (assign): " + statement.statement.toString());
            INDArray yArray = getYArrayUpper(assignment.lhs, intervalState);
            ObsDistrCube(yArray, (aqua.AST.FunctionCall) assignment.rhs, intervalState);
            changed = true;
        } else {
            // System.out.println("Assignment: " + statement.statement.toString());
            String paramID = assignment.lhs.toString().split("\\[")[0];
            if (no_tau && paramID.contains("robust_local_tau")) // Don't consider nu
                return true;
            if (paramMap.containsKey(paramID) || paramMap.containsKey(paramID + "[1]") || paramMap.containsKey(paramID + "[1,1]")) {
                String newParamID = paramID;
                Pair<Double[], ArrayList<Integer>> paramInfo = paramMap.get(paramID);
                // no multi dim prior
                if (paramInfo == null && no_prior) {
                    return true;
                }
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
                if (assignment.rhs instanceof aqua.AST.FunctionCall
                        && isFuncConst(assignment.rhs)) { // completely independent new param
                    // System.out.println("Const param");
                    // TODO: fix dim if rhs contains data
                    // TODO: fix usage of single Id without dim, e.g beta~... but beta has dim 2
                    if (assignment.lhs.toString().contains("[")) {
                        ArrayList<Integer> dims = new ArrayList<>();
                        getConstN(dims, ((aqua.AST.ArrayAccess) assignment.lhs).dims.dims.get(0));
                        if (paramDivs.containsKey(newParamID)) {
                            initIndParamAllDims(intervalState, assignment, assignment.lhs.toString(), paramLimits, new ArrayList<>());
                        } else {
                            initIndParamAllDimsPA(intervalState, assignment, assignment.lhs.toString(), paramLimits, new ArrayList<>());

                        }
                    }
                    else {
                        if (paramDivs.containsKey(newParamID)) {
                            initIndParamAllDims(intervalState, assignment, paramID, paramLimits, paramDims);
                            // System.out.println(retArray.tensorAlongDimension(0, 0));
                        } else {
                            initIndParamAllDimsPA(intervalState, assignment, paramID, paramLimits, paramDims);
                        }
                    }
                    // intervalState.printAbsState();
                }
                else { // transformed parameters, completely dependent on other params
                    if (!(assignment.rhs instanceof aqua.AST.FunctionCall)) { // TODO: cond: not a distr
                        INDArray rhs = DistrCube(assignment.rhs, intervalState);
                        if (!(assignment.lhs instanceof aqua.AST.ArrayAccess)) {
                            long[] rhsShape = rhs.shape();
                            // Pair<Double[], ArrayList<Integer>> lhsLength = paramMap.get(assignment.lhs.toString());
                            if (rhsShape.length == 0 || rhsShape[0] == 1)
                                intervalState.addDepParamCube(assignment.lhs.toString(), rhs); // with dim null
                            else {
                                long[] rhsClone = rhsShape.clone();
                                long dim0 = rhsClone[0];
                                rhsClone[0] = 1;
                                String lhsString = assignment.lhs.toString();
                                for (int i=0; i<dim0; i++) {
                                    String lhsIString = String.format("%s[%s]", lhsString, i+1);
                                    INDArray slicei = rhs.slice(i);
                                    intervalState.addDepParamCube(lhsIString, slicei.reshape(rhsClone));
                                }
                            }
                        }
                        else { // is arrayaccess like y_hat[i]
                            aqua.AST.ArrayAccess lhsArrayAccess = (aqua.AST.ArrayAccess) assignment.lhs;
                            String transParamId = lhsArrayAccess.id.id;
                            ArrayList<Integer> dimArray = new ArrayList<>();
                            getConstN(dimArray, lhsArrayAccess.dims.dims.get(0));
                            intervalState.addDepParamCube(transParamId + "[" + dimArray.get(0) + "]", rhs);
                        }
                    }
                    else {
                        // add hierarchical param
                        HierInterDistrCube(assignment, intervalState);
                    }
                }
                changed = true;
            } else if (paramID.equals("target")) {
                // System.out.println("===============target");
                analyzeTarget(intervalState, assignment.rhs);
                changed = true;
            }
        }
        return changed;
    }

    private void HierInterDistrCube(aqua.AST.AssignmentStatement assignment, GridState intervalState) {
        aqua.AST.Expression lhs = assignment.lhs;
        aqua.AST.FunctionCall rhs = (aqua.AST.FunctionCall) assignment.rhs;
        if (intervalState.paramValues.containsKey(lhs.toString())){
            INDArray[] params = getParams(intervalState, rhs);
            INDArray lhsParam = intervalState.getParamCube(lhs.toString());
            String idName = rhs.id.toString();
            if(idName.equals("normal")) {
                INDArray logNoSum = getProbLogUpper(lhsParam, params, idName);
                intervalState.addProb(logNoSum);

            } else if (idName.equals("triangle")){
                INDArray logNoSum = getProbLogUpper(lhsParam, params, idName);
                intervalState.addProb(logNoSum);
            }
        } else {
            assert false;
        }
    }

    private void analyzeTarget(GridState intervalState, aqua.AST.Expression rhs) {
        aqua.AST.AddOp plusRhs = (aqua.AST.AddOp) rhs;
        if (plusRhs.op2 instanceof aqua.AST.FunctionCall
                || plusRhs.op2 instanceof aqua.AST.MulOp || plusRhs.op2 instanceof aqua.AST.Braces) {
            INDArray probLUcat = DistrCube(plusRhs.op2, intervalState);
            if(probLUcat == null || probLUcat.length() == 0)
                return;

            probLUcat = intOutRobustUpper(probLUcat, intervalState);
            intervalState.addProb(probLUcat);
            // long[] newShape = probLUcat.shape().clone();
            // newShape[0] = 1;
            // INDArray[] sumExpLowerUpper = new INDArray[]{probLUcat.slice(0).reshape(newShape),probLUcat.slice(1).reshape(newShape)};
            // if (!integrateStack.empty())
            //     intOutRobust(intervalState);
            // intervalState.addProb(sumExpLowerUpper[0], sumExpLowerUpper[1]);
            // System.out.println(Nd4j.createFromArray(sumExpLowerUpper[0].shape()));
        }
    }

    private INDArray getYArrayUpper(aqua.AST.Expression lhs, GridState intervalState) {
        String dataYID = lhs.toString();
        INDArray ret1;
        if (!dataYID.contains("[") && dataList.containsKey(dataYID.split("\\[")[0])) {
            Pair<aqua.AST.Data, double[]> yDataPair = dataList.get(dataYID.split("\\[")[0]);
            ret1 = Nd4j.createFromArray(yDataPair.getValue());
        } else {
            ret1 = DistrCube(lhs, intervalState);
        }
        return ret1;
    }

    /*
    private double[][] getYArray(AST.Expression lhs, GridState intervalState) {
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
                assert(twoRet.length() == 2);
                ret1 = new double[]{twoRet.getDouble(0)};
                ret2 = new double[]{twoRet.getDouble(1)};
            }
            return new double[][]{ret1, ret2}; // 1 for good, 2 for bad
        }
    }
    */


    private Boolean incLoop(aqua.AST.ForLoop forLoop) {
        Boolean changed;
        String loopVar = forLoop.loopVar.id;
        if (scalarParam.containsKey(loopVar)) {
            int currLoopValue = scalarParam.get(loopVar);
            ArrayList<Integer> endValue = new ArrayList<>();
            getConstN(endValue, forLoop.range.end);
            // System.out.println("/////////////////" + String.valueOf(currLoopValue));
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

    private void initIndParamAllDims(GridState intervalState, aqua.AST.AssignmentStatement assignment, String paramID, Double[] paramLimits, ArrayList<Integer> paramDims) {
        if (paramDims.size() == 1) {
            for (Integer jj = 1; jj <= paramDims.get(0); jj++) {
                String currParamName = String.format("%s[%s]", paramID, jj);
                INDArray rhsMin[] = IndDistrSingle(assignment.rhs, paramLimits, minCounts, currParamName, intervalState); // split, probLower, probUpper
                initParamHelper(intervalState, assignment, paramLimits, rhsMin, currParamName);
            }
        } else if (paramDims.size() == 2) {
            for (Integer jj = 1; jj <= paramDims.get(0); jj++) {
                for (Integer kk = 1; kk <= paramDims.get(1); kk++) {
                    String currParamName = String.format("%s[%s,%s]", paramID, jj, kk);
                    INDArray rhsMin[] = IndDistrSingle(assignment.rhs, paramLimits, minCounts, currParamName, intervalState); // split, probLower, probUpper
                    initParamHelper(intervalState, assignment, paramLimits, rhsMin, currParamName);
                }
            }
        } else if (paramDims.size() == 0) {
            if (paramDivs.get(paramID) == minCounts) {
                if (minCounts != 0) {
                    INDArray rhsMin[] = IndDistrSingle(assignment.rhs, paramLimits, minCounts, paramID, intervalState); // split, probLower, probUpper
                    intervalState.addParamCube(paramID, rhsMin[0], rhsMin[1]);
                }
                else {
                    intervalState.addDepParamCube(paramID, Nd4j.empty());
                }
            } else { // maxCounts
                INDArray rhsMax[] = IndDistrSingle(assignment.rhs, paramLimits, paramDivs.get(paramID), paramID, intervalState); // split, probLower, probUpper
                intervalState.addParamCube(paramID, rhsMax[0], rhsMax[1]);

            }
        }
    }

    private void initParamHelper(GridState intervalState, aqua.AST.AssignmentStatement assignment, Double[] paramLimits, INDArray[] rhsMin, String currParamName) {
        if (paramDivs.get(currParamName) == minCounts) {
            if (minCounts != 0)
                intervalState.addParamCube(currParamName, rhsMin[0], rhsMin[1]);
            else
                intervalState.addDepParamCube(currParamName, Nd4j.empty());
        }
        else { // maxCounts
            INDArray rhsMax[] = IndDistrSingle(assignment.rhs, paramLimits, paramDivs.get(currParamName), currParamName, intervalState); // split, probLower, probUpper
            intervalState.addParamCube(currParamName, rhsMax[0], rhsMax[1]);
        }
    }

    private void initIndParamAllDimsPA(GridState intervalState, aqua.AST.AssignmentStatement assignment, String paramID, Double[] paramLimits, ArrayList<Integer> paramDims) {
        if (paramDims.size() == 1) {
            for (Integer jj = 1; jj <= paramDims.get(0); jj++) {
                String currParamName = String.format("%s[%s]", paramID, jj);
                INDArray rhsMin[] = IndDistrSingle(assignment.rhs, paramLimits, PACounts, currParamName, intervalState); // split, probLower, probUpper
                intervalState.addParamCube(currParamName, rhsMin[0], rhsMin[1]);
            }
        } else if (paramDims.size() == 2) {
            for (Integer jj = 1; jj <= paramDims.get(0); jj++) {
                for (Integer kk = 1; kk <= paramDims.get(1); kk++) {
                    String currParamName = String.format("%s[%s,%s]", paramID, jj, kk);
                    INDArray rhsMin[] = IndDistrSingle(assignment.rhs, paramLimits, PACounts, currParamName, intervalState); // split, probLower, probUpper
                    intervalState.addParamCube(currParamName, rhsMin[0], rhsMin[1]);
                }
            }
        } else if (paramDims.size() == 0) {
            INDArray rhsMin[] = IndDistrSingle(assignment.rhs, paramLimits, PACounts, paramID, intervalState); // split, probLower, probUpper
            intervalState.addParamCube(paramID, rhsMin[0], rhsMin[1]);
        }
    }

    private void HierDistrCube(aqua.AST.AssignmentStatement assignment, GridState intervalState) {
        aqua.AST.FunctionCall distrExpr = (aqua.AST.FunctionCall) assignment.rhs;
        INDArray[] params = getParams(intervalState, distrExpr);
        if (params == null) return;
        if (params.length == 1) {
            // assert(!(params[0].shape().length == 1 && params[0].shape()[0] == 1)); // o.w const distr
            // long[] retshape = new long[params[0].shape().length + 1];
            // System.arraycopy(params[0].shape(), 0, retshape, 0, params[0].shape().length);
            // retshape[params[0].shape().length] = piCounts;
            assert false;
            // TODO:
        }
        else if (params.length == 2) {
            String arrayId = assignment.lhs.toString();
            Pair<Double[], ArrayList<Integer>> limitDims = paramMap.get(arrayId);
            if (limitDims == null && paramMap.containsKey(arrayId + "[1]"))
                limitDims = paramMap.get(arrayId + "[1]");
            ArrayList<Integer> dimArray = limitDims.getValue();
            if (dimArray.size() == 1) {
                for (Integer jj = 1; jj <= dimArray.get(0); jj++) {
                    String eleId = String.format("%s[%s]", arrayId, jj);
                    INDArray[] singleprob = expand1D(params[0], params[1], distrExpr.id.id, eleId);
                    if (paramDivs.get(eleId) != 0 && singleprob[0].shape().length > 0) {
                        intervalState.addParamCube(eleId, singleprob[0], singleprob[1]);
                    }
                    else
                        intervalState.addDepParamCube(eleId, Nd4j.empty());
                }
            }  else if (dimArray.size() == 0) {
                String eleId = arrayId;
                INDArray[] singleprob = expand1D(params[0], params[1], distrExpr.id.id, eleId);
                if (paramDivs.get(eleId) != 0 && singleprob[0].shape().length > 0) {
                    intervalState.addParamCube(eleId, singleprob[0], singleprob[1]);
                }
                else
                    intervalState.addDepParamCube(eleId, Nd4j.empty());
            }
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
        System.arraycopy(shape1, 0, shape1b, 0, shape1.length);
        System.arraycopy(shape2, 0, shape2b, 0, shape2.length);
        long[] maxDim12 = new long[maxDimCount + 1];
        long[] broadcastDims = new long[maxDimCount + 1];
        for (int i=0; i < maxDimCount + 1; i++) {
            broadcastDims[i] = max(shape1b[i], shape2b[i]);
            maxDim12[i] = broadcastDims[i];
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
            INDArray input1b = input1.reshape(shape1b).broadcast(maxDim12);
            INDArray input2b = input2.reshape(shape2b).broadcast(maxDim12);
            INDArray single = Nd4j.createUninitialized(broadcastDims);
            INDArray prob1 = Nd4j.createUninitialized(broadcastDims);
            INDArray prob2 = Nd4j.createUninitialized(broadcastDims);
            if (distr.equals("normal")) {
                Double mean = null;
                Double sd = null;
                if (paramMap.containsKey(paramName)) {
                    Double[] limitsMeanSd = paramMap.get(paramName).getKey();
                    mean = limitsMeanSd[2];
                    sd = limitsMeanSd[3];
                }
                if (mean == null) {
                    mean = Nd4j.mean(input1).getDouble();
                    sd = Nd4j.mean(input2).getDouble();
                }
                if (sd == 0)
                    sd = pow(10, -16);
                for (int i = 0; i < Math.max(input1b.length(), input2b.length()); i++) {
                    double givenMean = input1b.getDouble(i);
                    double givenSd = input2b.getDouble(i);
                    if (givenSd == 0)
                        givenSd = pow(10, -16);
                    // if (distrName.equals("normal")) {
                    double[] singlej = new double[piCounts];
                    double[] prob1j = new double[piCounts];
                    double[] prob2j = new double[piCounts];
                    NormalDistribution normal = new NormalDistribution(givenMean, givenSd);
                    NormalDistribution splitDistr = new NormalDistribution(mean, sd);
                    getDiscretePriorsSingleSplit(singlej, prob1j, prob2j, normal, splitDistr, pi);
                    // }
                    prob1.tensorAlongDimension(i, maxDimCount).assign(Nd4j.createFromArray(prob1j));
                    prob2.tensorAlongDimension(i, maxDimCount).assign(Nd4j.createFromArray(prob2j));
                    single.tensorAlongDimension(i, maxDimCount).assign(Nd4j.createFromArray(singlej));
                }
            } else {
                for (int i = 0; i < input1b.length(); i++) {
                    double mean = input1b.getDouble(i);
                    double sd = input2b.getDouble(i);
                    // if (distrName.equals("normal")) {
                    double[] singlej = new double[piCounts];
                    double[] prob1j = new double[piCounts];
                    double[] prob2j = new double[piCounts];
                    if (mean == 0)
                        mean = pow(10, -16);
                    if (sd == 0)
                        sd = pow(10, -16);
                    GammaDistribution normal = new GammaDistribution(mean, sd);
                    getDiscretePriorsSingle(singlej, prob1j, prob2j, normal, pi);
                    // }
                    prob1.tensorAlongDimension(i, maxDimCount).assign(Nd4j.createFromArray(prob1j));
                    prob2.tensorAlongDimension(i, maxDimCount).assign(Nd4j.createFromArray(prob2j));
                    single.tensorAlongDimension(i, maxDimCount).assign(Nd4j.createFromArray(singlej));
                }
            }
            return new INDArray[]{single, prob1, prob2};
        }
        else {
            return new INDArray[]{Nd4j.empty(), Nd4j.empty(), Nd4j.empty()};
        }
    }

    private void ObsDistrCube(INDArray yArray, aqua.AST.FunctionCall rhs, GridState intervalState) {
        aqua.AST.FunctionCall distrExpr = rhs;
        INDArray[] params = getParams(intervalState, distrExpr);
        if (params == null || params[0].shape().length == 0  ) return;
        if (params.length > 1 && params[1].shape().length == 0) return;
        // System.out.println("Obs param0 shape: " + Nd4j.createFromArray(params[0].shape()));
        // if (params.length > 1)
        //     System.out.println("Obs param1 shape: " + Nd4j.createFromArray(params[1].shape()));
        INDArray sumExpUpper =  getProbLogUpper(yArray, params, distrExpr.id.id);
        if (!integrateStack.empty())
            sumExpUpper = intOutRobustUpper(sumExpUpper, intervalState);
            //intOutRobust(sumExpLowerUpper, intervalState);
        intervalState.addProb(sumExpUpper);
        // intervalState.addProb(sumExpLowerUpper[0], sumExpLowerUpper[1]);
    }

    /*
    private void intOutRobust(INDArray[] sumExpLowerUpper, GridState intervalState) {
        while(!integrateStack.empty()) {
            String intName = integrateStack.pop();
            Integer intIdx = intervalState.intOut(intName);
            // System.out.println(intName);
            // System.out.println(Nd4j.createFromArray(sumExpLowerUpper[0].shape()));
            sumExpLowerUpper[0] = log(exp(sumExpLowerUpper[0]).sum(intIdx));
            sumExpLowerUpper[1] = log(exp(sumExpLowerUpper[1]).sum(intIdx));
        }
    }
    */

    private INDArray intOutRobustUpper(INDArray sumExpUpper, GridState intervalState) {
        while(!integrateStack.empty()) {
            String intName = integrateStack.pop();
            Integer intIdx = intervalState.intOut(intName);
            sumExpUpper = log(exp(sumExpUpper).sum(intIdx));
        }
        return sumExpUpper;
    }

    private INDArray getProbLogUpper(INDArray yNDArray, INDArray[] params, String distrId){
        if (yNDArray.shape()[0] == 1 && params[0].shape()[0] ==2) {
            long[] preShape = new long[params[0].shape().length];
            System.arraycopy(params[0].shape(),0, preShape,0, preShape.length);
            preShape[0] = 1;
            params[0] = params[0].slice(0).reshape(preShape);
        }
        long[][] allShape = new long[params.length][];
        long[] maxShape = params[0].shape();
        for (int j=0; j < params.length; j++) {
            allShape[j] = params[j].shape();
            if (j == 1)
                maxShape = getMaxShape(allShape[j-1], allShape[j]);
            else
                maxShape = getMaxShape(maxShape, allShape[j]);
        }
        maxShape = getMaxShape(maxShape, yNDArray.shape());
        for (int j=0; j < params.length; j++) {
            // System.out.println("allShape " + j+ Nd4j.createFromArray(allShape[j]));
            params[j] = params[j].reshape(getReshape(allShape[j], maxShape)).broadcast(maxShape);
        }
        yNDArray = yNDArray.reshape(getReshape(yNDArray.shape(), maxShape)).broadcast(maxShape);
        INDArray likeCube = Nd4j.createUninitialized(maxShape);
        INDArray logSum;
        logSum = getLogSum(params, yNDArray, likeCube, distrId);
        return logSum;
    }

    /*
    @Deprecated
    private void getProbLogLU(double[][] yArray, INDArray[] sumExpLowerUpper, INDArray[] params, String distrId) {
        INDArray yNDArray = Nd4j.createFromArray(yArray[0]); // good data
        INDArray badcopy = null;
        if (yNDArray.shape()[0] == 1 && params[0].shape()[0] ==2) {
            long[] preShape = new long[params[0].shape().length];
            System.arraycopy(params[0].shape(),0, preShape,0, preShape.length);
            preShape[0] = 1;
            badcopy = params[0].dup().slice(1).reshape(preShape);
            params[0] = params[0].slice(0).reshape(preShape);
        }
        long[][] allShape = new long[params.length][];
        long[] maxShape = params[0].shape();
        for (int j=0; j < params.length; j++) {
            allShape[j] = params[j].shape();
            if (j == 1)
                maxShape = getMaxShape(allShape[j-1], allShape[j]);
            else
                maxShape = getMaxShape(maxShape, allShape[j]);
        }
        maxShape = getMaxShape(maxShape, yNDArray.shape());
        for (int j=0; j < params.length; j++) {
            // System.out.println("allShape " + j+ Nd4j.createFromArray(allShape[j]));
            params[j] = params[j].reshape(getReshape(allShape[j], maxShape)).broadcast(maxShape);
        }
        yNDArray = yNDArray.reshape(getReshape(yNDArray.shape(), maxShape)).broadcast(maxShape);
        INDArray likeCube = Nd4j.createUninitialized(maxShape);
        INDArray logSum;
        logSum = getLogSum(params, yNDArray, likeCube, distrId);
        sumExpLowerUpper[1] = logSum; // upper for good
        if (!toAttack)
            sumExpLowerUpper[0] = sumExpLowerUpper[1];
        else {
            yNDArray = Nd4j.createFromArray(yArray[1]); // bad data
            yNDArray = yNDArray.reshape(getReshape(yNDArray.shape(), maxShape)).broadcast(maxShape);
            if (badcopy != null)
                params[0] = badcopy.reshape(getReshape(badcopy.shape(), maxShape)).broadcast(maxShape);
            logSum = getLogSum(params, yNDArray, likeCube, distrId);
            sumExpLowerUpper[0] = logSum; // lower for bad
        }
    }
    */

    private INDArray getLogSum(INDArray[] params, INDArray yNDArray, INDArray likeCube, String distrId) {
        INDArray logSum;
        if (distrId.equals("normal")) {
            likeCube = Transforms.log(params[1].mul(Math.sqrt(2*PI))).neg().subi(
                    Transforms.pow(yNDArray.sub(params[0]),2).divi(Transforms.pow(params[1],2)).muli(0.5));
            // return -Math.log(sigma) - 0.5*((y - mu)*(y - mu)/(sigma*sigma));
            // for (long ii = 0; ii < likeCube.length(); ii++) {
            //     double yiiL = normal_LPDF(yNDArray.getDouble(ii), params[0].getDouble(ii), params[1].getDouble(ii));
            //     likeCube.putScalar(ii, yiiL);
            // }
        } else if (distrId.equals("gauss")) {
                likeCube = Transforms.log(Transforms.sqrt(params[1]).mul(Math.sqrt(2*PI))).neg().subi(
                        Transforms.pow(yNDArray.sub(params[0]),2).divi(params[1]).muli(0.5));

        } else if (distrId.equals("student_t")) {
            // INDArray nu = params[0].dup();
            // INDArray mu = params[1].dup();
            // INDArray sigma = params[2].dup();
            // if (nu.getDouble(10) <0 )
            //     assert false;
            // -0.846574 + (0.5 - 0.5 nu) Log[nu] + 0.5 nu Log[1. + 1. nu]
            // INDArray nud2 = nu.div(2);
            // likeCube = (((Transforms.log(nud2).muli(nu.sub(1).neg())).addi(Transforms.log(nud2.add(0.5)).subi(1))).divi(2.0)).subi(Transforms.log(nu).muli(0.5))
            //         .subi(Transforms.log(sigma));
            // likeCube = likeCube.addi(Transforms.log((Transforms.pow((yNDArray.sub(mu)).divi(sigma),2).divi(nu)).addi(1.0)).muli((nu.addi(1)).muli(-0.5)));


            for (long ii = 0; ii < likeCube.length(); ii++) {
                double yiiL = student_LPDF(yNDArray.getDouble(ii), params[0].getDouble(ii),  // first param is nu
                        params[1].getDouble(ii), params[2].getDouble(ii));
                likeCube.putScalar(ii, yiiL);
            }
            // INDArray gammaDiv = params[0].dup();
            // if
            // likeCube.replaceWhere()


        } else if (distrId.equals("bernoulli_logit")) {
            BooleanIndexing.replaceWhere(yNDArray, 0, Conditions.greaterThan(1));
            BooleanIndexing.replaceWhere(yNDArray, 1, Conditions.lessThan(0));
            INDArray sigmoidParams = Transforms.sigmoid(params[0]);
            likeCube = yNDArray.mul(sigmoidParams);
            // TODO: fix this
            likeCube = likeCube.add(Nd4j.scalar(1.0).broadcast(yNDArray.shape()).sub(yNDArray)
                    .mul(Nd4j.scalar(1.0).broadcast(sigmoidParams.shape()).sub(sigmoidParams)));
            likeCube = Transforms.log(likeCube);
            // for (long ii = 0; ii < likeCube.length(); ii++) {
            //     double yiiL = bernoulli_logit_LPDF(yNDArray.getDouble(ii), params[0].getDouble(ii));
            //     likeCube.putScalar(ii, yiiL);
            // }
        } else if (distrId.equals("uniform")) {
            INDArray ltArray = params[0].lt(yNDArray);
            INDArray gtArray = params[1].gt(yNDArray);
            INDArray good = Transforms.and(ltArray, gtArray);
            good = good.castTo(DataType.DOUBLE);
            likeCube = Transforms.log(good);
        } else if (distrId.equals("triangle")) {
            INDArray l = params[0].sub(params[1]);
            INDArray r = params[0].add(params[2]);
            INDArray m = params[0];
            INDArray ltArray = l.lt(yNDArray);
            INDArray gtArray = m.gt(yNDArray);
            INDArray eqArray = m.eq(yNDArray);
            gtArray = Transforms.or(gtArray, eqArray);
            INDArray lefthalf = Transforms.and(ltArray, gtArray);
            lefthalf = lefthalf.castTo(DataType.DOUBLE);
            INDArray ltArray2 = m.lt(yNDArray);
            INDArray gtArray2 = r.gt(yNDArray);
            INDArray righthalf = Transforms.and(ltArray2, gtArray2);
            righthalf = righthalf.castTo(DataType.DOUBLE);
            INDArray leftProb = ((yNDArray.sub(l)).div(params[1]));
            INDArray rightProb = (((yNDArray.sub(r)).neg()).div(params[2]));
            INDArray triProb = leftProb.mul(lefthalf).add(rightProb.mul(righthalf));
            likeCube = Transforms.log(triProb);
        }
        BooleanIndexing.replaceWhere(likeCube, 0, Conditions.isNan());
        logSum = likeCube;
        if (likeCube.shape()[0] != 1) {
            logSum = likeCube.sum(0);
            long[] prevShape = likeCube.shape().clone();
            prevShape[0] = 1;
            logSum = logSum.reshape(prevShape);
        }
        return logSum;
    }

    private INDArray[] getParams(GridState intervalState, aqua.AST.FunctionCall distrExpr) {
        INDArray[] params = new NDArray[distrExpr.parameters.size()];
        int parami = 0;
        for (aqua.AST.Expression pp: distrExpr.parameters) {
            if (pp instanceof aqua.AST.Integer) {
                params[parami] = Nd4j.createFromArray(((aqua.AST.Integer) pp).value);

            }
            else if (pp instanceof aqua.AST.Double) {
                params[parami] = Nd4j.createFromArray(((aqua.AST.Double) pp).value);
            }
            else {
                params[parami] = DistrCube(pp, intervalState);
                if (params[parami] == null || params[parami].length() == 0)
                    return null;
            }
            parami++;
        }
        return params;
    }

    private INDArray DistrCube(aqua.AST.Expression pp, GridState intervalState) {
        if (pp instanceof aqua.AST.FunctionCall) {
            return DistrCube((aqua.AST.FunctionCall) pp, intervalState);
        }
        if (pp instanceof aqua.AST.Id) {
            return DistrCube((aqua.AST.Id) pp, intervalState);
        }
        else if (pp instanceof aqua.AST.AddOp) {
            return DistrCube((aqua.AST.AddOp) pp, intervalState);

        }
        else if (pp instanceof aqua.AST.MinusOp) {
            return DistrCube((aqua.AST.MinusOp) pp, intervalState);

        }
        else if (pp instanceof aqua.AST.MulOp) {
            return DistrCube((aqua.AST.MulOp) pp, intervalState);

        }
        else if (pp instanceof aqua.AST.DivOp) {
            return DistrCube((aqua.AST.DivOp) pp, intervalState);

        }
        else if (pp instanceof aqua.AST.Braces) {
            return DistrCube((aqua.AST.Braces) pp, intervalState);

        }
        else if (pp instanceof aqua.AST.ArrayAccess) {
            return DistrCube((aqua.AST.ArrayAccess) pp, intervalState);
        }
        else if (pp instanceof aqua.AST.Integer) {
            return DistrCube((aqua.AST.Integer) pp, intervalState);
        }
        else if (pp instanceof aqua.AST.Double) {
            return DistrCube((aqua.AST.Double) pp, intervalState);
        }
        else if (pp instanceof aqua.AST.UnaryExpression) {
            return DistrCube((aqua.AST.UnaryExpression) pp, intervalState);
        }
        else if (pp instanceof aqua.AST.TernaryIf) {
            return DistrCube((aqua.AST.TernaryIf) pp, intervalState);
        }
        else if (pp instanceof aqua.AST.LtOp) {
            return DistrCube((aqua.AST.LtOp) pp, intervalState);
        }
        else if (pp instanceof aqua.AST.GtOp) {
            return DistrCube((aqua.AST.GtOp) pp, intervalState);
        }
        else if (pp instanceof aqua.AST.GeqOp) {
            return DistrCube((aqua.AST.GeqOp) pp, intervalState);
        }
        else if (pp instanceof aqua.AST.LeqOp) {
            return DistrCube((aqua.AST.LeqOp) pp, intervalState);
        }
        else if (pp instanceof aqua.AST.EqOp) {
            return DistrCube((aqua.AST.EqOp) pp, intervalState);
        }
        else if (pp instanceof aqua.AST.AndOp) {
            return DistrCube((aqua.AST.AndOp) pp, intervalState);
        }
        else if (pp instanceof aqua.AST.OrOp) {
            return DistrCube((aqua.AST.OrOp) pp, intervalState);
        }
        else {
            System.out.println("Expression " + pp.toString() + " not supported!");
            assert false;
            return null;
        }
    }


    private INDArray DistrCube(aqua.AST.UnaryExpression pp, GridState intervalState) {
        INDArray four = DistrCube(pp.expression,intervalState);
        return four.neg();
    }


    private INDArray DistrCube(aqua.AST.TernaryIf pp, GridState intervalState) {
        // System.out.println(pp.condition.toString());
        aqua.AST.Expression condExp;
        INDArray condArray = DistrCube(pp.condition, intervalState);
        Double trueValue = Double.valueOf(pp.trueExpression.toString()) + 1234;
        Double falseValue = Double.valueOf(pp.falseExpression.toString()) + 1234;
        BooleanIndexing.replaceWhere(condArray, trueValue, Conditions.equals(1));
        BooleanIndexing.replaceWhere(condArray, falseValue, Conditions.equals(0));
        condArray = condArray.subi(1234);
        return condArray;
        /*
        if (pp.condition instanceof AST.Braces)
            condExp = ((AST.Braces) pp.condition).expression;
        else
            condExp = pp.condition;
        INDArray  ret = null;
        if (condExp instanceof AST.LtOp) { // only works for < 0 (const)
            AST.LtOp cond = (AST.LtOp) condExp;
            INDArray condL = DistrCube(cond.op1, intervalState);
            System.out.println(Nd4j.createFromArray(condL.shape()));
            Double compZero = Double.valueOf(cond.op2.toString());
            Double trueValue = Double.valueOf(pp.trueExpression.toString());
            Double falseValue = Double.valueOf(pp.falseExpression.toString());
            ret = condL.dup();
            BooleanIndexing.replaceWhere(ret, trueValue, Conditions.lessThan(compZero));
            BooleanIndexing.replaceWhere(ret, falseValue, Conditions.greaterThanOrEqual(compZero));
        }
        else if (condExp instanceof AST.GtOp) {
            AST.GtOp cond = (AST.GtOp) condExp;
            INDArray condL = DistrCube(cond.op1, intervalState);
            System.out.println("========================" + cond.op1.toString());
            System.out.println(Nd4j.createFromArray(condL.shape()));
            Double compZero = Double.valueOf(cond.op2.toString());
            Double trueValue = Double.valueOf(pp.trueExpression.toString());
            Double falseValue = Double.valueOf(pp.falseExpression.toString());
            ret = Nd4j.ones(condL.shape()).muli(0.5*(trueValue + falseValue));
            BooleanIndexing.replaceWhere(ret, trueValue, Conditions.greaterThan(compZero));
            BooleanIndexing.replaceWhere(ret, falseValue, Conditions.notEquals(trueValue));
        }
        else if (condExp instanceof AST.EqOp) {
            AST.EqOp cond = (AST.EqOp) condExp;
            INDArray condL = DistrCube(cond.op1, intervalState);
            System.out.println("========================" + cond.op1.toString());
            System.out.println(Nd4j.createFromArray(condL.shape()));
            INDArray condR = DistrCube(cond.op2, intervalState);
            double compZero = condR.getDouble(0);
            Double trueValue = Double.valueOf(pp.trueExpression.toString());
            Double falseValue = Double.valueOf(pp.falseExpression.toString());
            ret = Nd4j.ones(condL.shape()).muli(0.5*(trueValue + falseValue));
            BooleanIndexing.replaceWhere(ret, trueValue, Conditions.equals(compZero));
            BooleanIndexing.replaceWhere(ret, falseValue, Conditions.notEquals(trueValue));
        }

        System.out.println("========================" + condExp.toString());
        assert ret != null;
        return ret;
        */
    }



    private INDArray DistrCube(aqua.AST.FunctionCall pp, GridState intervalState) {
        // System.out.println("Distr Func=================");
        INDArray ret = null;
        if (pp.id.id.equals("log_mix")) {
            INDArray[] params = getParams(intervalState, pp);
            if (params == null)
                return null;
            for (int i=0; i<params.length; i++) {
                if (params[i] == null)
                    return null;
            }
            ret = getLogMixUpper(params);
            // ret = concat1(new INDArray[]{likeLower, likeUpper});
        }
        else if (pp.id.id.equals("normal_lpdf")) {
            INDArray yArray = getYArrayUpper(pp.parameters.get(0), intervalState);
            INDArray mu = DistrCube(pp.parameters.get(1), intervalState);
            INDArray sigma = DistrCube(pp.parameters.get(2), intervalState);
            if (mu ==null || sigma == null || mu.length() == 0 || sigma.length() == 0)
                return Nd4j.empty();
            // INDArray[] probLU = new INDArray[2];
            ret = getProbLogUpper(yArray, new INDArray[]{mu, sigma}, "normal");
            // ret = concat1(probLU);
        }
        else if (pp.id.id.equals("student_t_lpdf")) {
            INDArray yArray = getYArrayUpper(pp.parameters.get(0), intervalState);
            INDArray nu = DistrCube(pp.parameters.get(1), intervalState);
            INDArray mu = DistrCube(pp.parameters.get(2), intervalState);
            INDArray sigma = DistrCube(pp.parameters.get(3), intervalState);
            if (nu == null || mu ==null || sigma == null || nu.length() == 0 || mu.length() == 0 || sigma.length() == 0)
                return Nd4j.empty();
            // INDArray[] probLU = new INDArray[2];
            ret = getProbLogUpper(yArray, new INDArray[]{nu, mu, sigma}, "student_t");
            // ret = concat1(probLU);
        }
        else if (pp.id.id.equals("bernoulli_logit_lpmf")) {
            INDArray yArray = getYArrayUpper(pp.parameters.get(0), intervalState);
            INDArray pu = DistrCube(pp.parameters.get(1), intervalState);
            if (pu == null || pu.length() == 0)
                return Nd4j.empty();
            // INDArray[] probLU = new INDArray[2];
            ret = getProbLogUpper(yArray, new INDArray[]{pu}, "bernoulli_logit");
            // ret = concat1(probLU);
        }
        else if (pp.id.id.equals("sqrt")) {
            INDArray param0 = DistrCube(pp.parameters.get(0), intervalState);
            if (param0 == null || param0.length() == 0)
                return param0;
            ret = Transforms.sqrt(param0);
        }
        else if (pp.id.id.equals("abs")) {
            INDArray param0 = DistrCube(pp.parameters.get(0), intervalState);
            if (param0 == null || param0.length() == 0)
                return param0;
            ret = Transforms.abs(param0);
        }
        else if (pp.id.id.equals("log")) {
            INDArray param0 = DistrCube(pp.parameters.get(0), intervalState);
            if (param0 == null || param0.length() == 0)
                return param0;
            ret = Transforms.log(param0);
        }
        else if (pp.id.id.equals("exp")) {
            INDArray param0 = DistrCube(pp.parameters.get(0), intervalState);
            if (param0 == null || param0.length() == 0)
                return param0;
            ret = Transforms.exp(param0);
        }
        else if (pp.id.id.equals("fmax")) {
            INDArray param0 = DistrCube(pp.parameters.get(0), intervalState);
            INDArray param1 = DistrCube(pp.parameters.get(1), intervalState);
            if (param0 == null || param0.length() == 0)
                return param0;
            long[] op1shape = param0.shape();
            long[] op2shape = param1.shape();
            long[] outShape = getMaxShape(op1shape, op2shape);
            ret = Transforms.max(param0.broadcast(outShape), param1.broadcast(outShape));
        }
        else if (pp.id.id.equals("inv")) {
            INDArray param0 = DistrCube(pp.parameters.get(0), intervalState);
            if (param0 == null || param0.length()== 0)
                return param0;
            ret = Nd4j.scalar(1.0).broadcast(param0.shape()).div(param0);
        }
        else if (pp.id.id.equals("inv_sqrt")) {
            INDArray param0 = DistrCube(pp.parameters.get(0), intervalState);
            if (param0 == null || param0.length()== 0)
                return param0;
            ret = Transforms.sqrt(param0);
            ret = Nd4j.scalar(1.0).broadcast(ret.shape()).div(ret);
        }
        return ret;
    }

    /*
    private INDArray getLogMixLU(INDArray[] params) {
        INDArray theta = params[0];
        INDArray omtheta = theta.sub(1).neg();
        long[] keepShape = params[1].shape().clone();
        keepShape[0] = 1;
        INDArray tLowerlog = params[1].slice(0);
        INDArray tUpperlog = params[1].slice(1);
        // Number smallConst = params[1].maxNumber();
        // if (params[2].maxNumber().doubleValue() >smallConst.doubleValue())
        //     smallConst = params[2].maxNumber();
        INDArray tLower = exp(tLowerlog).reshape(keepShape); // .subi(smallConst)
        INDArray tUpper = exp(tUpperlog).reshape(keepShape); // .subi(smallConst)
        long[] keepShape2 = params[2].shape().clone();
        keepShape2[0] = 1;
        tLowerlog = params[2].slice(0);
        tUpperlog = params[2].slice(1);
        INDArray omtLower = exp(tLowerlog).reshape(keepShape2);
        INDArray omtUpper = exp(tUpperlog).reshape(keepShape2);
        INDArray thetaTLower = getMulNDArray(theta, tLower);
        INDArray thetaTUpper = getMulNDArray(theta, tUpper);
        INDArray thetaOmtLower = getMulNDArray(omtheta, omtLower);
        INDArray thetaOmtUpper = getMulNDArray(omtheta, omtUpper);
        // BooleanIndexing.replaceWhere(likeLower,-746,Conditions.isInfinite());
        // BooleanIndexing.replaceWhere(likeUpper,-746,Conditions.isInfinite());
        return log(getAddNDArray(thetaTUpper, thetaOmtUpper));
    }
    */

    private INDArray getLogMixUpper(INDArray[] params) {
        INDArray theta = params[0];
        INDArray omtheta = (theta.sub(1)).neg();
        INDArray tUpperlog = params[1];
        INDArray tUpper = exp(tUpperlog);
        tUpperlog = params[2];
        INDArray omtUpper = exp(tUpperlog);
        INDArray thetaTUpper = getMulNDArray(theta, tUpper);
        INDArray thetaOmtUpper = getMulNDArray(omtheta, omtUpper);
        // BooleanIndexing.replaceWhere(likeLower,-746,Conditions.isInfinite());
        // BooleanIndexing.replaceWhere(likeUpper,-746,Conditions.isInfinite());
        INDArray addArray = getAddNDArray(thetaTUpper, thetaOmtUpper);
        return log(addArray);
    }

    /*
    private INDArray concat1(INDArray[] probLU) {
        INDArray ret;
        long[] newShape = new long[probLU[0].shape().length]; // no longer add 1 at front
        // System.arraycopy(probLU[0].shape(), 0, newShape, 0, probLU[0].shape().length);
        // ret = (Nd4j.concat(0, probLU[0].reshape(newShape), probLU[1].reshape(newShape)));
        ret = (Nd4j.concat(0, probLU[0], probLU[1]));
        return ret;
    }
    */


    private INDArray DistrCube(aqua.AST.Integer pp, GridState intervalState) {
        return Nd4j.createFromArray((double) pp.value);
    }

    private INDArray DistrCube(aqua.AST.Double pp, GridState intervalState) {
        return Nd4j.createFromArray(pp.value);
    }

    private INDArray DistrCube(aqua.AST.Id pp, GridState intervalState) {
        // System.out.println("Distr ID=================" + pp.id);

        if (dataList.containsKey(pp.id)) {
            Pair<aqua.AST.Data, double[]> xDataPair = dataList.get(pp.id);
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
        else if (intervalState.paramValues.containsKey(pp.id + "[1]")){
            ArrayList<Integer> dimInfo = paramMap.get(pp.id + "[1]").getValue();
            Integer paramLength = dimInfo.get(0);
            INDArray paramIValue = null;
            int i=1;
            for (; i<1+paramLength; i++) {
                Pair<Integer, INDArray> paramIValuePair = intervalState.paramValues.get(pp.id + "[" + i + "]");
                if (paramIValuePair != null && paramIValuePair.getValue().shape().length != 0) {
                    paramIValue = paramIValuePair.getValue();
                    break;
                }
            }
            if (i == 1+paramLength) {
                return null;
            }
            long[] oneDimShape = paramIValue.shape();
            long[] newShape = new long[oneDimShape.length];
            System.arraycopy(oneDimShape,0, newShape, 0, oneDimShape.length);
            assert(newShape[0] == 1);
            newShape[0] = paramLength;
            INDArray retArray = Nd4j.createUninitialized(newShape);
            i=0;
            for (; i<paramLength; i++ ) {
                String paramIName = String.format("%s[%s]", pp.id, i + 1);
                paramIValue = intervalState.paramValues.get(paramIName).getValue();
                if (paramIValue.length() > 0)
                    retArray.slice(i).assign(paramIValue);
                else
                    retArray.slice(i).assign(Double.NaN);
            }
            // System.out.println(pp.id + "[1] " + Nd4j.createFromArray(retArray.slice(0).shape()));
            return retArray;
        }
        else if (scalarParam.containsKey(pp.id)) {
            return Nd4j.scalar(scalarParam.get(pp.id));
        }
        else {// uninitialized param on RHS,
            String paramName = pp.toString();
            addUninitParam(intervalState, paramName);
            return intervalState.getParamCube(paramName);
        }
    }


    private INDArray DistrCube(aqua.AST.ArrayAccess pp, GridState intervalState) {
        // System.out.println("Distr ArrayAccess==================" + pp.toString());
        // if (dataList.containsKey(pp.id)) {

        // }
        // else {
        ArrayList<Integer> dims = new ArrayList<>();
        for (aqua.AST.Expression dd : pp.dims.dims)
            getConstN(dims, dd);
        if (intervalState.paramValues.containsKey(pp.toString()))
            return intervalState.getParamCube(pp.toString());
        else if (intervalState.paramValues.containsKey(pp.id.id + "[" + dims.get(0) + "]")) {
            // System.out.println(pp.id.id + " access dim " + dims + Nd4j.createFromArray(intervalState.getParamCube(pp.id.id + "[" + dims.get(0) + "]").shape()));
            return intervalState.getParamCube(pp.id.id + "[" + dims.get(0) + "]");
        }
        else if (dataList.containsKey(pp.id.id)) { // is Data
            Pair<aqua.AST.Data, double[]> xDataPair = dataList.get(pp.id.id);
            double[] xArray = xDataPair.getValue();
            double dataElement = xArray[dims.get(0) - 1]; // TODO: support 2D array access
            // System.out.println("To Attack: " + toAttack);
            // System.out.println(obsDataList);
            return Nd4j.createFromArray(dataElement); //.reshape(intervalState.getNewDim(1));
            // if ((!toAttack) || (!obsDataList.contains(pp.id.id)))
            //     return Nd4j.createFromArray(dataElement); //.reshape(intervalState.getNewDim(1));
            // else {
            //     xDataPair = dataList.get(pp.id.id + "_corrupted");
            //     xArray = xDataPair.getValue();
            //     double dataElement2 = xArray[dims.get(0) - 1]; // TODO: support 2D array access
            //     return Nd4j.createFromArray(dataElement, dataElement2); //.reshape(intervalState.getNewDim(1));
            // }
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

    private void addUninitParam(GridState intervalState, String paramName) {
        if (!majorParam.contains(paramName))
            integrateStack.add(paramName);
        Pair<Double[], ArrayList<Integer>> LimitsDim = paramMap.get(paramName);
        Double[] limitsMeanSd = LimitsDim.getKey();
        int piCounts;
        if (paramDivs.containsKey(paramName))
            piCounts = paramDivs.get(paramName);
        else
            piCounts = PACounts;
        // double[] probLower = new double[piCounts];
        double[] probUpper = new double[piCounts];
        double[] single = new double[piCounts];
        if (piCounts >= 1) {
            double pi;
            if (piCounts > 1)
                pi = 1.0 / (piCounts - 1);
            else
                pi = -1;
            double[] lulimits = new double[2];
            getUnifSplitLimits(limitsMeanSd, lulimits);
            getDiscretePriorsSingleUnif(single, probUpper, lulimits[0], lulimits[1], pi);
            intervalState.addParamCube(paramName, Nd4j.createFromArray(single).reshape(intervalState.getNewDim(piCounts)),
                    Nd4j.createFromArray(probUpper).reshape(intervalState.getNewDim(piCounts)));
        }
        else {
            intervalState.addDepParamCube(paramName, Nd4j.empty());

        }
    }

    private void getUnifSplitLimits(Double[] limitsMeanSd, double[] lulimits) {
        double lower;
        double upper;
        if (limitsMeanSd[0] != null && limitsMeanSd[1] != null) {
            if (limitsMeanSd[0] == 0) {
                   limitsMeanSd[0] = 0.000000001;
            }
            // if (limitsMeanSd[1] > 20 && limitsMeanSd[0] == 0) {
            //     limitsMeanSd[1] = limitsMeanSd[0] + 3;
            //     limitsMeanSd[0] = 0.000000001;
            // }
            lower = limitsMeanSd[0];
            upper =limitsMeanSd[1];

        } else if (limitsMeanSd[0] != null) {
            if (limitsMeanSd[0] == 0) {
                // Equiv-prob
                // GammaDistribution gamma = new GammaDistribution(limitsMeanSd[2]*limitsMeanSd[2]/(limitsMeanSd[3]*limitsMeanSd[3]), (limitsMeanSd[3]*limitsMeanSd[3])/limitsMeanSd[2]);
                // getDiscretePriorsSingleUn(single, probLower, probUpper, gamma, pi);
                // Equiv-interval
                // UniformRealDistribution unif = new UniformRealDistribution(0.1,  10);
                if (limitsMeanSd[3] != null) {
                    lower = 0.00000000001;
                    upper = 10*limitsMeanSd[3];
                }
                else {
                    lower = 0.00000000001;
                    upper = 50;
                }
            } else {
                lower = limitsMeanSd[0];
                upper = limitsMeanSd[0] + 5;
            }
        } else if (limitsMeanSd[2] != null) {
            // Equiv-prob
            // NormalDistribution normal = new NormalDistribution(limitsMeanSd[2], limitsMeanSd[3]);
            // getDiscretePriorsSingleUn(single, probLower, probUpper, normal, pi);
            // Equiv-interval
            lower = limitsMeanSd[2] - 6 *limitsMeanSd[3];
            upper = limitsMeanSd[2] + 6 *limitsMeanSd[3];
        } else { // all are null
            // System.out.println("Prior: Normal");
            lower = -50;
            upper = 50;
        }
        lulimits[0] = lower;
        lulimits[1] = upper;
    }


    private INDArray DistrCube(aqua.AST.Braces pp, GridState intervalState) {
        // System.out.println("Distr Brace==================");
        return DistrCube(pp.expression, intervalState);
    }

    private INDArray DistrCube(aqua.AST.AddOp pp, GridState intervalState) {
        // System.out.println("Distr Add==================" + pp.toString());
        INDArray op1Array = DistrCube(pp.op1, intervalState);
        INDArray op2Array = DistrCube(pp.op2, intervalState);
        return getAddNDArray(op1Array, op2Array);
    }

    private INDArray DistrCube(aqua.AST.MinusOp pp, GridState intervalState) {
        // System.out.println("Distr Add==================" + pp.toString());
        INDArray op1Array = DistrCube(pp.op1, intervalState);
        INDArray op2Array = DistrCube(pp.op2, intervalState);
        return getMinusNDArray(op1Array, op2Array);
    }

    private INDArray getAddNDArray(INDArray op1Array, INDArray op2Array) {
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

    private INDArray getMinusNDArray(INDArray op1Array, INDArray op2Array) {
        if (op1Array.length() != 0 && op2Array.length() != 0) {
            long[] op1shape = op1Array.shape();
            long[] op2shape = op2Array.shape();
            long[] outShape = getMaxShape(op1shape, op2shape);
            if (outShape.length > op1shape.length)
                op1Array = op1Array.reshape(getReshape(op1shape, outShape));
            else
                op2Array = op2Array.reshape(getReshape(op2shape, outShape));
            return op1Array.broadcast(outShape).sub(op2Array.broadcast(outShape));
        } else {
            return Nd4j.empty();
        }
    }

    private INDArray DistrCube(aqua.AST.MulOp pp, GridState intervalState) {
        INDArray op1Array = DistrCube(pp.op1, intervalState);
        INDArray op2Array = DistrCube(pp.op2, intervalState);
        return getMulNDArray(op1Array, op2Array);
    }


    private INDArray DistrCube(aqua.AST.DivOp pp, GridState intervalState) {
        // System.out.println("Distr Div==================" + pp.toString());
        INDArray op1Array = DistrCube(pp.op1, intervalState);
        INDArray op2Array = DistrCube(pp.op2, intervalState);
        return getDivNDArray(op1Array, op2Array);
    }

    private INDArray getMulNDArray(INDArray op1Array, INDArray op2Array) {
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

    private INDArray getDivNDArray(INDArray op1Array, INDArray op2Array) {
        if (op1Array.length() != 0 && op2Array.length() != 0) {
            long[] op1shape = op1Array.shape();
            long[] op2shape = op2Array.shape();
            long[] outShape = getMaxShape(op1shape, op2shape);
            if (outShape.length > op1shape.length)
                op1Array = op1Array.reshape(getReshape(op1shape, outShape));
            else
                op2Array = op2Array.reshape(getReshape(op2shape, outShape));

            return op1Array.broadcast(outShape).div(op2Array.broadcast(outShape));
        }
        else {
            return Nd4j.empty();
        }
    }


    private INDArray DistrCube(aqua.AST.LtOp pp, GridState intervalState) {
        INDArray op1Array = DistrCube(pp.op1, intervalState);
        INDArray op2Array = DistrCube(pp.op2, intervalState);
        return getLtNDArray(op1Array, op2Array);
    }

    private INDArray DistrCube(aqua.AST.GtOp pp, GridState intervalState) {
        INDArray op1Array = DistrCube(pp.op1, intervalState);
        INDArray op2Array = DistrCube(pp.op2, intervalState);
        return getGtNDArray(op1Array, op2Array);
    }


    private INDArray DistrCube(aqua.AST.EqOp pp, GridState intervalState) {
        INDArray op1Array = DistrCube(pp.op1, intervalState);
        INDArray op2Array = DistrCube(pp.op2, intervalState);
        return getEqNDArray(op1Array, op2Array);
    }

    private INDArray DistrCube(aqua.AST.GeqOp pp, GridState intervalState) {
        INDArray op1Array = DistrCube(pp.op1, intervalState);
        INDArray op2Array = DistrCube(pp.op2, intervalState);
        return getGeqNDArray(op1Array, op2Array);
    }

    private INDArray DistrCube(aqua.AST.LeqOp pp, GridState intervalState) {
        INDArray op1Array = DistrCube(pp.op1, intervalState);
        INDArray op2Array = DistrCube(pp.op2, intervalState);
        return getLeqNDArray(op1Array, op2Array);
    }

    private INDArray DistrCube(aqua.AST.AndOp pp, GridState intervalState) {
        INDArray op1Array = DistrCube(pp.op1, intervalState);
        INDArray op2Array = DistrCube(pp.op2, intervalState);
        return getAndNDArray(op1Array, op2Array);
    }

    private INDArray DistrCube(aqua.AST.OrOp pp, GridState intervalState) {
        INDArray op1Array = DistrCube(pp.op1, intervalState);
        INDArray op2Array = DistrCube(pp.op2, intervalState);
        return getOrNDArray(op1Array, op2Array);
    }

    private INDArray getLtNDArray(INDArray op1Array, INDArray op2Array) {
        if (op1Array.length() != 0 && op2Array.length() != 0) {
            long[] op1shape = op1Array.shape();
            long[] op2shape = op2Array.shape();
            long[] outShape = getMaxShape(op1shape, op2shape);
            if (outShape.length > op1shape.length) {
                if (op1Array.length() == 1)
                    op1Array.addi(0.000000001);
                op1Array = op1Array.reshape(getReshape(op1shape, outShape));
            }
            else {
                if (op2Array.length() == 1)
                    op2Array.addi(0.000000001); // At Zero ?
                op2Array = op2Array.reshape(getReshape(op2shape, outShape));
            }
            INDArray good = op1Array.broadcast(outShape).lt(op2Array.broadcast(outShape));
            good = good.castTo(DataType.DOUBLE);
            return good;
        }
        else {
            return Nd4j.empty();
        }
    }


    private INDArray getGtNDArray(INDArray op1Array, INDArray op2Array) {
        if (op1Array.length() != 0 && op2Array.length() != 0) {
            long[] op1shape = op1Array.shape();
            long[] op2shape = op2Array.shape();
            long[] outShape = getMaxShape(op1shape, op2shape);
            if (outShape.length > op1shape.length) {
                if (op1Array.length() == 1)
                    op1Array.subi(0.000000001);
                op1Array = op1Array.reshape(getReshape(op1shape, outShape));
            }
            else {
                if (op2Array.length() == 1)
                    op2Array.addi(0.000000001);
                op2Array = op2Array.reshape(getReshape(op2shape, outShape));
            }
            INDArray good = op1Array.broadcast(outShape).gt(op2Array.broadcast(outShape));
            good = good.castTo(DataType.DOUBLE);
            return good;
        }
        else {
            return Nd4j.empty();
        }
    }


    private INDArray getEqNDArray(INDArray op1Array, INDArray op2Array) {
        if (op1Array.length() != 0 && op2Array.length() != 0) {
            long[] op1shape = op1Array.shape();
            long[] op2shape = op2Array.shape();
            long[] outShape = getMaxShape(op1shape, op2shape);
            if (outShape.length > op1shape.length)
                op1Array = op1Array.reshape(getReshape(op1shape, outShape));
            else
                op2Array = op2Array.reshape(getReshape(op2shape, outShape));
            INDArray good = Transforms.abs(op1Array.broadcast(outShape).sub(op2Array.broadcast(outShape))).lt(0.00000001);
            good = good.castTo(DataType.DOUBLE);
            return good;
        }
        else {
            return Nd4j.empty();
        }
    }

    private INDArray getAndNDArray(INDArray op1Array, INDArray op2Array) {
        if (op1Array.length() != 0 && op2Array.length() != 0) {
            long[] op1shape = op1Array.shape();
            long[] op2shape = op2Array.shape();
            long[] outShape = getMaxShape(op1shape, op2shape);
            if (outShape.length > op1shape.length)
                op1Array = op1Array.reshape(getReshape(op1shape, outShape));
            else
                op2Array = op2Array.reshape(getReshape(op2shape, outShape));
            INDArray good = op1Array.broadcast(outShape).mul(op2Array.broadcast(outShape));
            BooleanIndexing.replaceWhere(good, 1, Conditions.greaterThan(1));
            // good = good.castTo(DataType.DOUBLE);
            return good;
        }
        else {
            return Nd4j.empty();
        }
    }

    private INDArray getOrNDArray(INDArray op1Array, INDArray op2Array) {
        if (op1Array.length() != 0 && op2Array.length() != 0) {
            long[] op1shape = op1Array.shape();
            long[] op2shape = op2Array.shape();
            long[] outShape = getMaxShape(op1shape, op2shape);
            if (outShape.length > op1shape.length)
                op1Array = op1Array.reshape(getReshape(op1shape, outShape));
            else
                op2Array = op2Array.reshape(getReshape(op2shape, outShape));
            INDArray good = op1Array.broadcast(outShape).add(op2Array.broadcast(outShape));
            BooleanIndexing.replaceWhere(good, 1, Conditions.greaterThan(1));
            // good = good.castTo(DataType.DOUBLE);
            return good;
        }
        else {
            return Nd4j.empty();
        }
    }

    private INDArray getGeqNDArray(INDArray op1Array, INDArray op2Array) {
        if (op1Array.length() != 0 && op2Array.length() != 0) {
            long[] op1shape = op1Array.shape();
            long[] op2shape = op2Array.shape();
            long[] outShape = getMaxShape(op1shape, op2shape);
            if (outShape.length > op1shape.length)
                op1Array = op1Array.reshape(getReshape(op1shape, outShape));
            else
                op2Array = op2Array.reshape(getReshape(op2shape, outShape));
            INDArray goodgt = op1Array.broadcast(outShape).gt(op2Array.broadcast(outShape));
            INDArray goodeq = op1Array.broadcast(outShape).eq(op2Array.broadcast(outShape));
            INDArray good = Transforms.or(goodgt, goodeq);
            good = good.castTo(DataType.DOUBLE);
            return good;
        }
        else {
            return Nd4j.empty();
        }
    }


    private INDArray getLeqNDArray(INDArray op1Array, INDArray op2Array) {
        if (op1Array.length() != 0 && op2Array.length() != 0) {
            long[] op1shape = op1Array.shape();
            long[] op2shape = op2Array.shape();
            long[] outShape = getMaxShape(op1shape, op2shape);
            if (outShape.length > op1shape.length)
                op1Array = op1Array.reshape(getReshape(op1shape, outShape));
            else
                op2Array = op2Array.reshape(getReshape(op2shape, outShape));
            INDArray goodlt = op1Array.broadcast(outShape).lt(op2Array.broadcast(outShape));
            INDArray goodeq = op1Array.broadcast(outShape).eq(op2Array.broadcast(outShape));
            INDArray good = Transforms.or(goodlt, goodeq);
            good = good.castTo(DataType.DOUBLE);
            return good;
        }
        else {
            return Nd4j.empty();
        }
    }

    public static long[] getReshape(long[] op1shape, long[] outShape) {
        long[] reShape = new long[outShape.length];
        Arrays.fill(reShape, 1);
        System.arraycopy(op1shape, 0, reShape, 0, op1shape.length);
        return reShape;
    }

    public static long[] getMaxShape(long[] op1shape, long[] op2shape) {
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



    private boolean isFuncConst(aqua.AST.Expression rhs) {
        if (rhs instanceof aqua.AST.FunctionCall) {
            aqua.AST.FunctionCall distrExpr = (aqua.AST.FunctionCall) rhs;
            for (aqua.AST.Expression pp: distrExpr.parameters){
                if(!(pp instanceof aqua.AST.Integer || pp instanceof aqua.AST.Double)){
                    if (pp instanceof aqua.AST.UnaryExpression) {
                        aqua.AST.Expression negpp = ((aqua.AST.UnaryExpression) pp).expression;
                        if(!(negpp instanceof aqua.AST.Integer || negpp instanceof aqua.AST.Double))
                            return false;
                    }
                    else
                        return false;
                }
            }
            return true;
        }
        else
            return false;
    }


    private void getDiscretePriorsSingle(double[] single, double[] prob1, double[] prob2,
                                         AbstractRealDistribution normal, double pi) {
        if (pi >= 0) {
            single[0] = normal.inverseCumulativeProbability(0.0000000001);
            single[single.length - 1] = normal.inverseCumulativeProbability(0.9999999999);
            if (single[0] == Double.NEGATIVE_INFINITY)
                single[0] = -pow(10, 16);
            if (single[single.length - 1] == Double.POSITIVE_INFINITY)
                single[single.length - 1] = pow(10, 16);
            if (addPrior) {
                prob1[0] = 0;
                prob1[prob1.length - 1] = 0;
                prob2[0] = 0;
                prob2[prob2.length - 1] = 0;
                prob2[prob2.length - 2] = 0;
                double pp = pi;
                    for (int ii = 1; ii <= single.length - 2; pp += pi, ii++) {
                            single[ii] = normal.inverseCumulativeProbability(pp);
                            prob1[ii] = (normal.density(single[ii]));
                            // prob2[ii - 1] = prob1[ii]; // 2 is upper
                            prob2[ii] = prob1[ii]; // 2 is upper
                    }
            } else {
                double pp = pi;
                for (int ii = 1; ii <= single.length - 2; pp += pi, ii++) {
                        single[ii] = normal.inverseCumulativeProbability(pp);
                }
                Arrays.fill(prob1, 1);
                Arrays.fill(prob2, 1);
            }
        }
        else {
                single[0] = normal.inverseCumulativeProbability(0.5);
                    prob1[0] = normal.density(single[0]);
                    prob2[0] = normal.density(single[0]);
        }
    }


    private void getDiscretePriorsSingleSplitUnif(double[] single, double[] prob2, double lower, double upper,
                                                  AbstractRealDistribution normal, double pi) {
        if (pi >= 0) {
            single[0] = lower;
            prob2[0] = normal.density(single[0]);
            double inc = (upper - lower) / (single.length - 1);
            for (int ii = 1; ii < single.length; ii++) {
                single[ii] = single[ii - 1] + inc;
                prob2[ii] = normal.density(single[ii]);
            }
        }else {
            single[0] = (lower + upper)/2;
            prob2[0] = 1;
        }
    }

    private void getDiscretePriorsSingleSplit(double[] single, double[] prob1, double[] prob2,
                                         AbstractRealDistribution normal, AbstractRealDistribution splitDistr, double pi) {
        if (splitDistr instanceof NormalDistribution)
            splitDistr = new UniformRealDistribution(splitDistr.inverseCumulativeProbability(0.05), splitDistr.inverseCumulativeProbability(0.95));
        if (pi >= 0) {
            single[0] = splitDistr.inverseCumulativeProbability(0.05);
            single[single.length - 1] = splitDistr.inverseCumulativeProbability(0.95);
            if (single[0] == Double.NEGATIVE_INFINITY)
                single[0] = -pow(10, 2);
            if (single[single.length - 1] == Double.POSITIVE_INFINITY)
                single[single.length - 1] = pow(10, 2);
            if (addPrior) {
                prob1[0] = 0;
                prob1[prob1.length - 1] = 0;
                prob2[0] = 0;
                prob2[prob2.length - 1] = 0;
                prob2[prob2.length - 2] = 0;
                double pp = pi;
                for (int ii = 1; ii <= single.length - 2; pp += pi, ii++) {
                    single[ii] = splitDistr.inverseCumulativeProbability(pp);
                    prob1[ii] = (normal.density(single[ii]));
                    // prob2[ii - 1] = prob1[ii];
                    prob2[ii] = prob1[ii];
                }
            } else {
                double pp = pi;
                for (int ii = 1; ii <= single.length - 2; pp += pi, ii++) {
                    single[ii] = splitDistr.inverseCumulativeProbability(pp);
                }
                Arrays.fill(prob1, 1);
                Arrays.fill(prob2, 1);
            }
        }
        else {
            single[0] = splitDistr.inverseCumulativeProbability(0.5);
            prob1[0] = normal.density(single[0]);
            prob2[0] = normal.density(single[0]);
        }
    }

    /*
    private void getDiscretePriorsSingleUn(double[] single, double[] prob1, AbstractRealDistribution normal, double pi) {
        if (pi >= 0) {
            single[0] = normal.inverseCumulativeProbability(0.05);
            single[single.length - 1] = normal.inverseCumulativeProbability(0.95);
            if (single[0] == Double.NEGATIVE_INFINITY)
                single[0] = -pow(10, 5);
            if (single[single.length - 1] == Double.POSITIVE_INFINITY)
                single[single.length - 1] = pow(10, 5);
            double pp = pi;
            for (int ii = 1; ii <= single.length - 2; pp += pi, ii++) {
                single[ii] = normal.inverseCumulativeProbability(pp);
            }
            Arrays.fill(prob1, 1);
            // Arrays.fill(prob2, 1);
        }
        else {
            single[0] = normal.inverseCumulativeProbability(0.5);
            prob1[0] = 1;
            // prob2[0] = 1;
        }
    }
    */

    private void getDiscretePriorsSingleUnif(double[] single, double[] prob1, double lower, double upper, double pi) {
        if (pi >= 0) {
            single[0] = lower;
            double inc = (upper - lower) / (single.length - 1);
            for (int ii = 1; ii < single.length; ii++) {
                single[ii] = single[ii - 1] + inc;
            }
            Arrays.fill(prob1, 1);
            // System.out.println("============lower" + String.valueOf(lower) + " " + String.valueOf(upper));
            // System.out.println(Nd4j.createFromArray(single));
        }else {
            single[0] = (lower + upper)/2;
            prob1[0] = 1;
        }
    }


    private INDArray[] IndDistrSingle(aqua.AST.Expression expr, Double[] paramLimits, int piCounts, String paramName, GridState intervalState) {
        // if (rhs instanceof AST.F)
        if (piCounts >= 1) {
            double pi;
            if (piCounts > 1)
                pi = 1.0 / (piCounts - 1);
            else
                pi = -1;
            if (expr instanceof aqua.AST.FunctionCall) {
                aqua.AST.FunctionCall distrExpr = (aqua.AST.FunctionCall) expr;
                ArrayList<Double> funcParams = new ArrayList<>();
                for (aqua.AST.Expression pp : distrExpr.parameters) {
                    if (pp instanceof aqua.AST.Integer)
                        funcParams.add((double) ((aqua.AST.Integer) pp).value);
                    else if (pp instanceof aqua.AST.Double)
                        funcParams.add(((aqua.AST.Double) pp).value);
                    else {
                        funcParams.add(Double.valueOf(pp.toString()));
                    }
                }
                String distrName = distrExpr.id.id;
                if (discreteDist.contains(distrName))
                    return DiscSplitPrior(piCounts, paramName, pi, funcParams, distrName, intervalState);
                else
                    return ContSplitPrior(piCounts, paramName, pi, funcParams, distrName);
            }
            //return new INDArray[]{Nd4j.createFromArray(single), Nd4j.createFromArray(prob2)};
            assert false;
            return null;
        }
        else {
            return new INDArray[]{Nd4j.empty(), Nd4j.empty()};
        }
    }

    private INDArray[] DiscSplitPrior(int piCounts, String paramName, double pi, ArrayList<Double> funcParams, String distrName, GridState intervalState) {
        double[] single = null;
        double[] prob2 = null;
        switch (distrName) {
            case "bernoulli":
                single = new double[] {0.0,1.0};
                prob2 = new double[] {1-funcParams.get(0), funcParams.get(0)};
                break;
            case "flip":
                single = new double[] {0.0,1.0};
                prob2 = new double[] {1-funcParams.get(0), funcParams.get(0)};
                break;
            case "atom": // full range must be defined
                single = new double[] {funcParams.get(0)};
                prob2 = new double[] {1};
                break;
            case "uniformClose":
                single = new double[piCounts];
                prob2 = new double[piCounts];
                double unifUpper = funcParams.get(1);
                double unifLower = funcParams.get(0);
                if (intervalState.paramValues.containsKey(paramName)) {
                    INDArray currValue = intervalState.paramValues.get(paramName).getValue();
                    single = currValue.reshape(currValue.length()).toDoubleVector();
                    int in_count = 0;
                    int from = single.length - 1;
                    int to = 0;
                    for (int ii=0; ii < single.length; ii++) {
                        double ss = single[ii];
                        if (unifLower - 0.000000001 <= ss && ss <= unifUpper + 0.000000001) {
                            in_count++;
                            if (from > ii)
                                from = ii;
                            to = ii;
                        }
                    }
                    Arrays.fill(prob2, 0);
                    if (from <= to)
                        Arrays.fill(prob2, from, to + 1, 1.0/in_count);
                }
                else{
                    double inc = (unifUpper - unifLower) / (piCounts - 1);
                    double ss = unifLower;
                    for (int ii = 0; ii < piCounts; ii++) {
                        single[ii] = ss;
                        ss += inc;
                    }
                    Arrays.fill(prob2, 1.0/piCounts);
                }
                break;
            case "categorical":
                single = new double[funcParams.size()];
                prob2 = new double[funcParams.size()];
                for (int ii =0; ii < single.length; ii ++ ) {
                    single[ii] = ii;
                    prob2[ii] = funcParams.get(ii);
                }
                break;
            case "binomial":
                Double trials = funcParams.get(0);
                Double prob = funcParams.get(1);
                BinomialDistributionImpl binomialDistribution = new BinomialDistributionImpl(trials.intValue(), prob);
                single = new double[(int) (trials + 1)];
                prob2 = new double[(int) (trials + 1)];
                for (int ii =0; ii < single.length; ii ++ ) {
                    single[ii] = ii;
                    prob2[ii] = binomialDistribution.probability(ii);
                }
                break;
            case "poisson":
                Double ll = funcParams.get(0);
                PoissonDistributionImpl poissonDistribution = new PoissonDistributionImpl(ll);
                double sqrt5 = sqrt(ll)*5;
                int posLower = max(0, (int) (ll - sqrt5));
                int posUpper =(int) (ll + sqrt5);
                single = new double[posUpper - posLower + 1];
                prob2 = new double[posUpper - posLower + 1];
                for (int ii = 0; ii < single.length; ii ++ ) {
                    single[ii] = posLower + ii;
                    prob2[ii] = poissonDistribution.probability(posLower + ii);
                }
                break;

        }
        return new INDArray[]{Nd4j.createFromArray(single), Nd4j.createFromArray(prob2)};

    }

    private INDArray[] ContSplitPrior(int piCounts, String paramName, double pi, ArrayList<Double> funcParams, String distrName) {
        double[] single = new double[piCounts];
        // double[] prob1 = new double[piCounts];
        double[] prob2 = new double[piCounts];
        AbstractRealDistribution normal = null;
        // if (paramMap.get(paramName).getKey()[2] == null) {
        //     switch (distrName) {
        //         case "normal":
        //             normal = new NormalDistribution(funcParams.get(0), funcParams.get(1));
        //             break;
        //         case "gamma":
        //             normal = new GammaDistribution(funcParams.get(0), funcParams.get(1));
        //             break;
        //         case "cauchy":
        //             normal = new CauchyDistribution(funcParams.get(0), funcParams.get(1));
        //             break;
        //         case "beta":
        //             normal = new BetaDistribution(funcParams.get(0), funcParams.get(1));
        //             break;
        //         case "uniform":
        //             normal = new UniformRealDistribution(funcParams.get(0), funcParams.get(1));
        //             break;
        //     }
        //     getDiscretePriorsSingle(single, prob1, prob2, normal, pi);
        // } else {
        Double[] meanSd = paramMap.get(paramName).getKey();
        if (distrName.equals("normal")) {
            meanSd[2] = funcParams.get(0);
            meanSd[3] = funcParams.get(1);
        }
        double[] lulimits = new double[2];
        getUnifSplitLimits(meanSd, lulimits);
        // AbstractRealDistribution splitDistr = null;
        switch (distrName) {
            case "normal":
                normal = new NormalDistribution(funcParams.get(0), funcParams.get(1));
                // splitDistr = new NormalDistribution(meanSd[2],meanSd[3]);
                break;
            case "gauss":
                normal = new NormalDistribution(funcParams.get(0), sqrt(funcParams.get(1)));
                // splitDistr = new NormalDistribution(meanSd[2],meanSd[3]);
                break;
            case "gamma":
                normal = new GammaDistribution(funcParams.get(0), funcParams.get(1));
                // splitDistr = new GammaDistribution(meanSd[2]*meanSd[2]/(meanSd[3]*meanSd[3]), (meanSd[3]*meanSd[3])/meanSd[2]);
                break;
            case "cauchy":
                normal = new CauchyDistribution(funcParams.get(0), funcParams.get(1));
                // splitDistr = new NormalDistribution(meanSd[2],meanSd[3]);
                break;
            case "beta":
                normal = new BetaDistribution(funcParams.get(0), funcParams.get(1));
                // Double alpha = (meanSd[2]*meanSd[2] - meanSd[2]*meanSd[2]*meanSd[2] - meanSd[2]*meanSd[3]*meanSd[3])/(meanSd[3]*meanSd[3]);
                // Double beta = (meanSd[2]-1)*(meanSd[2]*meanSd[2]-meanSd[2]+meanSd[3]*meanSd[3])/(meanSd[3]*meanSd[3]);
                // if (alpha <= 0)
                //     alpha = funcParams.get(0);
                // if (beta <=0 )
                //     beta = funcParams.get(1);
                // splitDistr = new BetaDistribution(alpha,beta);
                break;
            case "uniform":
                normal = new UniformRealDistribution(funcParams.get(0), funcParams.get(1));
                // splitDistr = new NormalDistribution(meanSd[2],meanSd[3]);
                break;
            case "student_t":
                normal = new NormalDistribution(funcParams.get(1), funcParams.get(2));
                // splitDistr = new NormalDistribution(meanSd[2],meanSd[3]);
                break;
            case "triangle":
                normal = new TriangularDistribution(funcParams.get(1),funcParams.get(2),funcParams.get(0));
                break;
            //}
        }
        getDiscretePriorsSingleSplitUnif(single, prob2, lulimits[0], lulimits[1], normal, pi);
        return new INDArray[]{Nd4j.createFromArray(single), Nd4j.createFromArray(prob2)};
    }

    // Used in Pre-Analysis to find all params definition
    private void addParams(aqua.cfg.Statement statement) {
        if (statement.statement instanceof aqua.AST.Decl) {
            aqua.AST.Decl declStatement = (aqua.AST.Decl) statement.statement;
            Double[] limits = {null, null, null, null};
            for(aqua.AST.Annotation aa : declStatement.annotations) {
                if (aa.annotationType == aqua.AST.AnnotationType.Limits){
                    if (aa.annotationValue instanceof aqua.AST.Limits) {
                        aqua.AST.Limits aaLimits = (aqua.AST.Limits) aa.annotationValue;
                        if(aaLimits.lower != null)
                            limits[0] = Double.valueOf(aaLimits.lower.toString());
                        if(aaLimits.upper != null)
                            limits[1] = Double.valueOf(aaLimits.upper.toString());
                        if(declStatement.id.id.contains("robust_local_tau")) {
                            limits[1] = 10.0;
                        }
                        if(declStatement.id.id.contains("robust_weight")) {
                            limits[0] = 0.000000000;
                            limits[1] = 1.0;
                        }
                        if(declStatement.id.id.contains("robust_t_nu")) {
                            limits[0] = 0.000000001;
                            limits[1] = 10.0;
                        }
                    }
                }
            }
            ArrayList<Integer> dimArray = new ArrayList<>();
            if (declStatement.dtype.dims != null) {
                for (aqua.AST.Expression dd: declStatement.dtype.dims.dims) {
                    getConstN(dimArray, dd);
                }
            }
            if (declStatement.dims != null) {
                for (aqua.AST.Expression dd : declStatement.dims.dims) {
                    getConstN(dimArray, dd);
                }
            }
            String arrayId = declStatement.id.id;
            if (dimArray.size() == 1) {
                for (Integer jj = 1; jj <= dimArray.get(0); jj++) {
                    String eleId = String.format("%s[%s]", arrayId, jj);
                    paramMap.put(eleId, new Pair(limits.clone(), dimArray));
                }
            } else if (dimArray.size() == 2) {
                for (Integer jj = 1; jj <= dimArray.get(0); jj++) {
                    for (Integer kk = 1; kk <= dimArray.get(1); kk++) {
                        String eleId = String.format("%s[%s,%s]", arrayId, jj, kk);
                        paramMap.put(eleId, new Pair(limits.clone(), dimArray));
                    }
                }
            } else if (dimArray.size() == 0) {
                String eleId = arrayId;
                paramMap.put(eleId, new Pair(limits, dimArray));
            }
        }
    }

    private void getConstN(ArrayList<Integer> dimArray, aqua.AST.Expression dd) {
        if (dd.toString().matches("\\d+"))
            dimArray.add(Integer.valueOf(dd.toString()));
        else if (dataList.containsKey(dd.toString())){
            Pair<aqua.AST.Data, double[]> dataPair = dataList.get(dd.toString());
            aqua.AST.Data data = dataPair.getKey();
            assert (data.decl.dtype.primitive == aqua.AST.Primitive.INTEGER);
            dimArray.add(Integer.valueOf(data.expression.toString()));
        }
        else if (scalarParam.containsKey(dd.toString())){
            dimArray.add(scalarParam.get(dd.toString()));
        }
        else if (dataList.containsKey(dd.toString().split("\\[")[0])){ // data array access
            Pair<aqua.AST.Data, double[]> dataPair = dataList.get(dd.toString().split("\\[")[0]);
            ArrayList<Integer> nested = new ArrayList<>();
            getConstN(nested, ((aqua.AST.ArrayAccess) dd).dims.dims.get(0));
            double[] dataValue = dataPair.getValue();
            dimArray.add((int) dataValue[nested.get(0) - 1]);
        } else if (scalarParam.containsKey(dd.toString().split("-")[0])) {
            String ddStrings[] = dd.toString().split("-");
            String stringT = ddStrings[0];
            String minusC = ddStrings[1];
            Integer currT = scalarParam.get(stringT);
            Integer one = Integer.valueOf(minusC);
            dimArray.add(currT - one);
        }
    }

    private static double normal_LPDF(double y, double mu, double sigma) {
        if (sigma <= pow(10, -16))// || sigma>20)
            return -pow(10, 16);
        return -Math.log(sigma) - 0.5*((y - mu)*(y - mu)/(sigma*sigma));
    }

    private static double student_LPDF(double y, double nu, double mu, double sigma) {
        if (nu <= pow(10, -16) || sigma <= pow(10, -16))// || sigma>20)
            return -pow(10, 16);
        TDistribution studT = new TDistribution(nu);
        return studT.logDensity((y - mu)/sigma) - Math.log(sigma);
    }

    private static double bernoulli_logit_LPDF(double y, double x) {
        double expx = Math.exp(x);
        double mlog1pexpx = -Math.log(1+expx);
        return ((1-y)*(mlog1pexpx) + y*(x - mlog1pexpx));
    }
    //=====================================================================================
    //=============  Code below is an Inefficient Implementation ==========================
    //=============        by 2D Joint-Probability Tables        ==========================
    //=====================================================================================

    // private INDArray[] Distr(AST.Expression expr) {
    //     Double[] paramLimits = {null,null};
    //     return Distr(expr, paramLimits);

    // }

    // private INDArray sameTraceExprs(ArrayList<AST.Expression> parameters) {
    //     return retArray;
    // }

    @Deprecated
    private void BlockAnalysis(aqua.cfg.BasicBlock basicBlock) {
        GridState intervalState = new GridState();
        for (aqua.cfg.Statement statement : basicBlock.getStatements()) {
            if (statement.statement instanceof aqua.AST.Decl) {
                // System.out.println("Decl: " + statement.statement.toString());
                ArrayList<aqua.AST.Annotation> annotations = statement.statement.annotations;
                addParams(statement);
                // System.out.println(statement.statement.toString());
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
            } else if (statement.statement instanceof aqua.AST.AssignmentStatement) {
                ArrayList<aqua.AST.Annotation> annotations = statement.statement.annotations;
                aqua.AST.AssignmentStatement assignment = (aqua.AST.AssignmentStatement) statement.statement;
                if (annotations != null && !annotations.isEmpty() &&
                        annotations.get(0).annotationType == aqua.AST.AnnotationType.Observe) {
                    // System.out.println("Observe (assign): " + statement.statement.toString());
                    String dataYID = assignment.lhs.toString();
                    if (!dataYID.contains("[")) {
                        Pair<aqua.AST.Data, double[]> yDataPair = dataList.get(dataYID);
                        double[] yArray = yDataPair.getValue();
                        ObsDistr(yArray, assignment, intervalState);
                    }
                } else {
                    // System.out.println("Assignment: " + statement.statement.toString());
                    String paramID = assignment.lhs.toString().split("\\[")[0];
                    if (!paramMap.containsKey(paramID)) {
                        addParams(statement);
                    }
                    Pair<Double[], ArrayList<Integer>> paramInfo = paramMap.get(paramID);
                    Double[] paramLimits = paramInfo.getKey();
                    ArrayList<Integer> paramDims = paramInfo.getValue();
                    if (isFuncConst(assignment.rhs)) {
                        // System.out.println("Const param");
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
                        // intervalState.printAbsState();
                    }
                    // TODO: dependent
                }
            } else if (statement.statement instanceof aqua.AST.FunctionCallStatement) {
                // System.out.println("FunctionCall: " + statement.statement.toString());

            } else if (statement.statement instanceof aqua.AST.IfStmt) {
                aqua.AST.IfStmt ifStmt = (aqua.AST.IfStmt) statement.statement;
                // BlockAnalysis(ifStmt.BBtrueBlock);
                // BlockAnalysis(ifStmt.BBelseBlock);
            } else if (statement.statement instanceof aqua.AST.ForLoop) {
                aqua.AST.ForLoop forLoop = (aqua.AST.ForLoop) statement.statement;
                // System.out.println("ForLoop: "+ statement.statement);
                // BlockAnalysis(forLoop.BBloopBody);
            }
        }
        basicBlock.dataflowFacts = intervalState;
    }

    @Deprecated
    private void ObsDistr(double[] yArray, aqua.AST.AssignmentStatement assignment, GridState intervalState) {
        int yLength = yArray.length;
        long traceLength = intervalState.intervalProbPairs.shape()[0];
        aqua.AST.FunctionCall distrExpr = (aqua.AST.FunctionCall) assignment.rhs;
        INDArray[] params = new NDArray[distrExpr.parameters.size()];
        int parami = 0;
        for (aqua.AST.Expression pp: distrExpr.parameters) {
            if (pp instanceof aqua.AST.Integer) {
                params[parami] = Nd4j.createFromArray(((aqua.AST.Integer) pp).value);

            }
            else if (pp instanceof aqua.AST.Double) {
                params[parami] = Nd4j.createFromArray(((aqua.AST.Double) pp).value);
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
                    obsProb[0][ti] += Math.log(normal0.density(yArray[yi]));
                    obsProb[1][ti] += Math.log(normal1.density(yArray[yi]));
                    obsProb[2][ti] += Math.log(normal2.density(yArray[yi]));
                    obsProb[3][ti] += Math.log(normal3.density(yArray[yi]));
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
    private void outputResults(GridState intervalState, INDArray normNewProb) {
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
    private INDArray Distr(aqua.AST.Expression pp, GridState intervalState) {
        if (pp instanceof aqua.AST.Id) {
            return Distr((aqua.AST.Id) pp, intervalState);
        }
        else if (pp instanceof aqua.AST.AddOp) {
            return Distr((aqua.AST.AddOp) pp, intervalState);

        }
        else if (pp instanceof aqua.AST.MulOp) {
            return Distr((aqua.AST.MulOp) pp, intervalState);

        }
        else if (pp instanceof aqua.AST.Braces) {
            return Distr((aqua.AST.Braces) pp, intervalState);

        }
        else if (pp instanceof aqua.AST.ArrayAccess) {
            return Distr((aqua.AST.ArrayAccess) pp, intervalState);
        }
        return null;
    }


    @Deprecated
    private INDArray Distr(aqua.AST.Id pp, GridState intervalState) {
        System.out.println("Distr ID=================");

        if (dataList.containsKey(pp.id)) {
            Pair<aqua.AST.Data, double[]> xDataPair = dataList.get(pp.id);
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
    private INDArray Distr(aqua.AST.ArrayAccess pp, GridState intervalState) {
        // System.out.println("Distr ArrayAccess==================");
        // if (dataList.containsKey(pp.id)) {

        // }
        // else {
        int ididx = intervalState.paramMap.get(pp.toString());
        return intervalState.intervalProbPairs.getColumns(ididx, ididx + 1).reshape(-1,1,2);
        // }
    }

    @Deprecated
    private INDArray Distr(aqua.AST.Braces pp, GridState intervalState) {
        // System.out.println("Distr Brace==================");
        return Distr(pp.expression, intervalState);
    }

    @Deprecated
    private INDArray Distr(aqua.AST.AddOp pp, GridState intervalState) {
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
    private INDArray Distr(aqua.AST.MulOp pp, GridState intervalState) {
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
    private INDArray[] IndDistr(aqua.AST.Expression expr, Double[] paramLimits) {
        // if (rhs instanceof AST.F)
        double[] lower = new double[maxCounts];
        double[] upper = new double[maxCounts];
        if (expr instanceof aqua.AST.FunctionCall) {
            aqua.AST.FunctionCall distrExpr = (aqua.AST.FunctionCall) expr;
            ArrayList<Double> funcParams = new ArrayList<>();
            for (aqua.AST.Expression pp: distrExpr.parameters){
                if (pp instanceof aqua.AST.Integer)
                    funcParams.add((double) ((aqua.AST.Integer) pp).value);
                else
                    funcParams.add(((AST.Double) pp).value);
            }
            String distrName = distrExpr.id.id;
            if (distrName.equals("normal")) {
                NormalDistribution normal = new NormalDistribution(funcParams.get(0), funcParams.get(1));
                getDiscretePriors(lower, upper, normal);
            }
            else if (distrName.equals("gamma")) {
                GammaDistribution gamma = new GammaDistribution(funcParams.get(0), funcParams.get(1));
                getDiscretePriors(lower, upper, gamma);
            }
        }
        // retArray[0]: prob, [1] lower, [2] upper
        return new INDArray[]{Nd4j.zeros(DataType.DOUBLE,maxCounts).addi(maxProb), Nd4j.create(lower), Nd4j.create(upper)};


    }

    @Deprecated
    private void getDiscretePriors(double[] lower, double[] upper, AbstractRealDistribution normal) {
        int ii = 0;
        //for(double pp=pi; pp <= 1-2*pi; pp += pi) {
        for(double pp=0; pp <= 1-1*maxProb; pp += maxProb) {
            lower[ii] = normal.inverseCumulativeProbability(pp);
            upper[ii] = normal.inverseCumulativeProbability(pp+maxProb);
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
