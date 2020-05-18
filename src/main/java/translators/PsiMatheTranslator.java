package translators;

import grammar.AST;
import grammar.cfg.*;
import org.apache.commons.lang3.tuple.Pair;
import utils.Utils;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Math.ceil;
import static java.lang.Math.min;
import static java.lang.Math.round;

public class PsiMatheTranslator implements ITranslator{

    private String defaultIndent = "\t";
    private OutputStream out;
    private OutputStream Matheout;
    private Set<BasicBlock> visited;
    private StringBuilder stringBuilder;
    private StringBuilder output;
    private String pathDirString;
    private Boolean nomean=true;
    public Integer dataReduceRatio=8;
    private String transformparamOut = "";
    private String bodyString;
    private HashMap<String, AST.Decl> paramDeclStatement = new HashMap<>();
    private Set<String> paramPriorNotAdded = new HashSet<>();
    private final List<JsonObject> models= Utils.getDistributions(null);
    private HashMap<String, Integer> constMap = new HashMap<>();
    private boolean ratioChanged=false;


    public void setOut(OutputStream o){
        out = o;
    }
    public void setMatheOut(OutputStream o){
        Matheout = o;
            dumpMathe( "Get[polyPath <> \"base0417.m\"];   \n"); //"#!/usr/bin/env wolframscript\n\n" +
    }

    public void setPath(String s) {
        pathDirString = s;
    }

    public void parseBlock(BasicBlock block){

        ArrayList<Statement>  stmts = block.getStatements();

        if(!visited.contains(block)) {
            visited.add(block);
            StringBuilder sb = new StringBuilder();
            Statement st = null;

            for(Statement stmt : stmts){
                String res = parse(stmt);
                sb.append(res);
            }
            dump(sb.toString());

            for (Edge e : block.getEdges()) {
                parseBlock(e.getTarget());
            }
        }

    }
    private String dumpR(ArrayList<AST.Data> dataSets) {
        StringWriter stringWriter = null;
        stringWriter = new StringWriter();

        for (AST.Data data : dataSets) {
            String dataString = Utils.parseData(data, 'f');
            String dimsString = "";
            if (data.decl.dtype.dims != null && data.decl.dtype.dims.dims.size() > 0) {
                dimsString += data.decl.dtype.dims.toString();
            }
            if (data.decl.dims != null && data.decl.dims.dims.size() > 0) {
                if (dimsString.length() > 0) {
                    dimsString += ",";
                }
                dimsString += data.decl.dims.toString();
            }

            dataString = dataString.replaceAll("\\s", "").replaceAll("\\[", "").replaceAll("\\]", "").replaceAll("\\.0,",",").replaceAll("\\.0$","");
            if (dimsString.length() == 0) {
                if(dataString.contains(".")) {
                    stringWriter.write(String.format("%s := %s;\n", data.decl.id, dataString));
                } else {
                    if (data.decl.id.id.length() == 1 && !ratioChanged) {
                        ratioChanged = true;
                        if (Integer.valueOf(dataString) < 100 && dataReduceRatio == 8)
                            dataReduceRatio = 1; //2
                        else if (Integer.valueOf(dataString) < 300 && Integer.valueOf(dataString) >= 100 && dataReduceRatio == 8)
                            dataReduceRatio = 4; //4
                        else if (Integer.valueOf(dataString) < 1000 && Integer.valueOf(dataString) >= 300 && dataReduceRatio == 8)
                            dataReduceRatio = 16;
                        else if (Integer.valueOf(dataString) >= 1000 && dataReduceRatio == 8)
                            dataReduceRatio = 32;
                        if (pathDirString.contains("gp-fit"))
                            dataReduceRatio = dataReduceRatio*4;
                        else if (pathDirString.contains("twomode") && dataReduceRatio>1)
                            //dataReduceRatio = dataReduceRatio*5;
                            dataReduceRatio/=2;
                    }
                    // temp fix for flight
                    if ( ! (Integer.valueOf(dataString)/dataReduceRatio < 1))
                        // temp fix for electric
                        if (data.decl.id.id.equals("n_pair"))
                            dataString = String.valueOf((int) (round(Float.valueOf(dataString) * 2 /dataReduceRatio)));
                        else if (data.decl.id.id.equals("n_scenarios"))
                            dataString = String.valueOf(min((int) (round(Float.valueOf(dataString) * 5/dataReduceRatio)),8));
                        else if (data.decl.id.id.equals("J"))
                            dataString = String.valueOf((int) (round(Float.valueOf(dataString) /dataReduceRatio)) + 1);
                        else
                            dataString = String.valueOf((int) (round(Float.valueOf(dataString)/dataReduceRatio)));
                    stringWriter.write(String.format("%s := %s;\n", data.decl.id, dataString));
                    System.out.println(data.decl.id.id + "========================");
                    System.out.println(dataString + "========================");
                    constMap.put(data.decl.id.id,(Integer.valueOf(dataString)));
                }
                // write to mathe
                if (!data.decl.id.id.equals("N"))
                    dumpMathe(data.decl.id.id.replace("_","MMMM") + "= " + dataString + "\n");
            } else if (dimsString.split(",").length == 1) {
                // more data to file
                stringWriter.write(String.format("%1$s := readCSV(\"%1$s_data_csv\");\n", data.decl.id));
                String[] dataStringSplit = dataString.split(",");
                Integer dataLength = dataStringSplit.length;
                if (dataLength/dataReduceRatio >= 1)
                    dataString = String.join(",",Arrays.copyOfRange(dataStringSplit,0,(int) round((Float.valueOf(dataLength)/dataReduceRatio))));
                String addOneDataString = "1," + dataString;
                try {

                    FileOutputStream out = new FileOutputStream(String.format("%1$s/%2$s_data_csv",pathDirString,data.decl.id));
                    out.write(addOneDataString.getBytes());
                    out.write("\n".getBytes());
                    out.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                // write to mathe
                dumpMathe(data.decl.id.id.replace("_","MMMM") + "= {" + dataString + "}\n");

            } else if (dimsString.split(",").length == 2) {
                String[] splited = dimsString.split(",");
                String[] dataSplited = dataString.split(",");
                List<String> dataS = new ArrayList<>();
                int outterDim= Integer.valueOf(splited[0]);
                for(int i = 0; i < outterDim; ++i){
                    StringBuilder res = new StringBuilder();
                    int innerDim = Integer.valueOf(splited[1]);
                    res.append("[");
                    for(int j = 0; j < innerDim; ++j){
                        res.append(dataSplited[i+j*outterDim]);
                        if(j != innerDim-1){
                            res.append(",");
                        }
                    }
                    res.append("]");
                    dataS.add(res.toString());
                }
                stringWriter.write(String.format("%s := [%s];\n", data.decl.id,  String.join(",", dataS)));
            } else {

            }

        }
        try {
            stringWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return stringWriter.toString();
    }




    private String translate_block(BasicBlock bBlock) {
        String output = "";
        if (bBlock.getStatements().size() == 0)
            return output;

        for (Statement statement : bBlock.getStatements()) {
            if (statement.statement instanceof AST.AssignmentStatement) {
                AST.AssignmentStatement assignmentStatement = (AST.AssignmentStatement) statement.statement;
                String tempRhs = assignmentStatement.rhs.toString();
                String newRhs;
                AST.Decl lhsDecl = paramDeclStatement.get(assignmentStatement.lhs.toString().split("\\[")[0]);
                if (lhsDecl != null && lhsDecl.annotations.size() > 0 &&
                                            (lhsDecl.annotations.get(0).annotationType.toString().equals("Prior") ||
                                                    lhsDecl.annotations.get(0).annotationType.toString().equals("Limits")
                                            )) {
                    paramPriorNotAdded.remove(lhsDecl.id.toString());
                    String dist = tempRhs.split("\\(")[0];
                    System.out.println(dist);
                    String params = tempRhs.replace(dist,"").substring(1,tempRhs.length() - dist.length() - 1);
                    String innerParams = "";
                    for (JsonObject model : this.models) {
                        if (dist.equals(model.getString("stan"))) {
                            JsonArray modelParams = model.getJsonArray("args");
                            for (JsonValue iipp : modelParams) {
                                String paramName = iipp.asJsonObject().getString("name");
                                innerParams += "," + paramName;
                            }
                        }
                    }
                    String innerParams2 = innerParams.substring(1);
                    if (dist.equals("inv_gamma"))
                        dist = "InverseGamma";
                    if(dist.equals("normal") && !params.split(",")[1].matches("\\d*\\.?\\d*"))
                        innerParams2 = innerParams2.replace("sigma","Sqrt[sigma]");
                    // if(dist.equals("normal_cholesky")) {
                    //     dist = "normal";
                    // }

                    newRhs = String.format("sampleFrom(\"(x;%2$s) => PDF[%1$sDistribution[%4$s],x]\", %3$s)",
                            dist.substring(0,1).toUpperCase() + dist.substring(1),
                            innerParams.substring(1),
                            params,
                            innerParams2
                            );
                    if(dist.contains("normal") && tempRhs.contains("(0,2)")) {
                        newRhs = "sampleFrom(\"(x;theta) => PDF[HalfNormalDistribution[theta],x]\", 0.6266570687)";

                    }
                }
                else{
                    //TODO: temp fix
                    newRhs = tempRhs
                            .replace("sigma_a1*eta1","sigma_a1*eta1[ppjj]")
                            .replace("sigma_a2*eta2","sigma_a2*eta2[ppjj]") //hiv_chr
                            .replace("sigma_a*eta","sigma_a*eta[ppjj]") // anova_chr
                                ;
                    System.out.println("===================");
                    System.out.println(assignmentStatement.lhs);
                    System.out.println(newRhs);
                }
                String assignStr;
                if (lhsDecl != null && (lhsDecl.dims != null || lhsDecl.dtype.dims != null) && !assignmentStatement.lhs.toString().contains("[")) {
                    String loopDim;
                    if (lhsDecl.dims != null) {
                        loopDim = lhsDecl.dims.toString();
                    }
                    else {
                        loopDim = lhsDecl.dtype.dims.toString();
                    }
                    assignStr = String.format("for ppjj in [1..%1$s+1) {\n",loopDim);
                    assignStr += String.format("%1$s[ppjj] = %2$s;\n",assignmentStatement.lhs,newRhs);
                    assignStr += "}\n";

                } else {
                    // Deal with target +=
                    if (assignmentStatement.lhs.toString().equals("target")) {
                        String dist = tempRhs.split("_lpdf\\(")[0].split("target\\+")[1];
                        String[] paramsList = tempRhs.split("_lpdf\\(")[1].split(",");
                        String firstParam = paramsList[0];
                        String params = tempRhs.split("_lpdf\\(")[1].replaceAll("\\)$","").replace(firstParam+",","");
                        String innerParams = "";
                        for (JsonObject model : this.models) {
                            if (dist.equals(model.getString("name"))) {
                                JsonArray modelParams = model.getJsonArray("args");
                                for (JsonValue iipp : modelParams) {
                                    String paramName = iipp.asJsonObject().getString("name");
                                    innerParams += "," + paramName;
                                }
                            }
                        }
                        // if (dist.equals("normal_cholesky")) {
                        //
                        // }
                        if (innerParams.length()>1) { // not log mix or sum_log_exp
                            String innerParams2 = innerParams.substring(1);
                            if (dist.equals("normal"))
                                innerParams2 = innerParams2.replace("sigma", "Sqrt[sigma]");
                            String observeI = firstParam.split("(\\[|])", 3)[1];
                            newRhs = String.format("cobserve(sampleFrom(\"(x;%2$s,i) => PDF[%1$sDistribution[%6$s],x+Delta[i]]\", %3$s,%5$s),%4$s)",
                                    dist.substring(0, 1).toUpperCase() + dist.substring(1),
                                    innerParams.substring(1),
                                    params,
                                    firstParam,
                                    observeI,//.replace("observe_i","observe_i-1")
                                    innerParams2
                            );
                            assignStr = newRhs;
                            assignStr += ";\n";
                            // write getMSEfromMAP in mathe file
                            String meanString = paramsList[1].replaceAll("\\[([0-9]+)]", "$1").replace("[" + observeI + "]",
                                    "[msei]").replace("_", "MMMM");//.replace("[","[[").replace("]","]]");
                            String newll = meanString;
                            List<String> allMatches = new ArrayList<String>();
                            Matcher m = Pattern.compile("\\w+\\[").matcher(meanString);
                            while (m.find()) {
                                allMatches.add(m.group());
                            }
                            System.out.println(allMatches);
                            for (String mm : allMatches) {
                                String paramKey = mm.replace("MMMM", "_").replace("[", "");
                                if (paramDeclStatement.containsKey(paramKey)) {
                                    AST.Decl paramDecl = paramDeclStatement.get(paramKey);
                                    if (paramDecl.annotations.size() > 0 &&
                                            (paramDecl.annotations.get(0).annotationType.toString().equals("Prior") ||
                                                    paramDecl.annotations.get(0).annotationType.toString().equals("Limits")
                                            )
                                            ) // is a param
                                        newll = newll.replaceAll("\\b" + mm.replace("[", "") + "\\b\\[", "addrParam[" + mm.replace("[", "") + ",");
                                }
                            }
                            // not always i
                            newll = newll.replaceAll("\\[(\\w+)]", "[[$1]]");
                            meanString = newll;
                            String stdString;
                            if (paramsList.length >= 3)
                                stdString = paramsList[2].replaceAll("\\[([0-9]+)]", "$1").replace("[" + observeI + "]",
                                        "[msei]").replace("_", "MMMM").replace("[", "[[").replace("]", "]]");
                            else
                                stdString = "sigma";
                            dumpMathe(String.format("getMSEfromMAP[mapVal_] := \n" +
                                    " Module[{}, \n" +
                                    "  1/Length[%1$s]*\n" +
                                    "   Sum[((%1$s[[msei]] - %2$s) /. mapVal[[2]])^2, {msei, 1, Length[%1$s]}]\n" +
                                    "  ]\n", firstParam.split("\\[")[0].replace("_","MMMM"), meanString));
                            dumpMathe("maxDataIdxMap[deltaFullMap_] := \n" +
                                    " Module[{maxDataIdx}, \n" +
                                    "  maxDataIdx = \n" +
                                    "   StringDelete[\n" +
                                    "    SymbolName[Keys[TakeLargest[Association[deltaFullMap], 1]][[1]]], \n" +
                                    "    \"Delta\"];\n" +
                                    "  <|dweight1 -> Symbol[\"dweight\" <> maxDataIdx], \n" +
                                    "   Delta1Rep -> Symbol[\"Delta\" <> maxDataIdx],\n" +
                                    "   betaRep -> ReleaseHold[(HoldForm[\n" +
                                    "       " + meanString + "] /. (msei -> ToExpression[maxDataIdx]))],\n" +
                                    "   sigmaRep -> " + stdString.replace(")", "") + "|>\n" +
                                    "  ]\n");
                            dumpMathe("maxDataIdxMap[deltaFullMap_,maxDataIdx_] := \n" +
                                    " Module[{}, \n" +
                                    "  <|dweight1 -> Symbol[\"dweight\" <> maxDataIdx], \n" +
                                    "   Delta1Rep -> Symbol[\"Delta\" <> maxDataIdx],\n" +
                                    "   betaRep -> ReleaseHold[(HoldForm[\n" +
                                    "       " + meanString + "] /. (msei -> ToExpression[maxDataIdx]))],\n" +
                                    "   sigmaRep -> " + stdString.replace(")", "") + "|>\n" +
                                    "  ]");

                            // write observe alternative for transformations
                            // original
                            try {
                                FileOutputStream out = new FileOutputStream(String.format("%1$s/OrgObserve", pathDirString));
                                out.write(newRhs.getBytes());
                                out.close();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            // reparam
                            if (dist.equals("normal")) {
                                try {
                                    String newRhsTrans;
                                    newRhsTrans = String.format(
                                            "cobserve(sampleFrom(\"(x;mu,sigma,i) => PDF[GammaDistribution[1/2,1/2],weight[i]]*PDF[NormalDistribution[mu,Sqrt[sigma]*weight[i]^(-1/2)],x+Delta[i]]\", %1$s,%3$s),%2$s)",
                                            params,
                                            firstParam,
                                            observeI);
                                    FileOutputStream out = new FileOutputStream(String.format("%1$s/ReparamTransObserve", pathDirString));
                                    out.write(newRhsTrans.getBytes());
                                    out.close();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                try {
                                    String newRhsTrans;
                                    newRhsTrans = String.format(
                                            "cobserve(sampleFrom(\"(x;mu,sigma,i) => PDF[StudentTDistribution[mu,Sqrt[sigma],weight],x+Delta[i]]\", %1$s,%3$s),%2$s)",
                                            params,
                                            firstParam,
                                            observeI);
                                    FileOutputStream out = new FileOutputStream(String.format("%1$s/StudentTransObserve", pathDirString));
                                    out.write(newRhsTrans.getBytes());
                                    out.close();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                // Mixture
                                if (!assignmentStatement.rhs.toString().contains("log_mix"))
                                    try {
                                        String newRhsTrans;
                                        newRhsTrans = String.format(
                                                "cobserve(sampleFrom(\"(x;mu,sigma,i) => (1-weight0)*PDF[NormalDistribution[mu,Sqrt[sigma]],x+Delta[i]]+weight0*PDF[NormalDistribution[mu,Sqrt[weight1]],x+Delta[i]]\", %1$s,%3$s),%2$s)",
                                                params,
                                                firstParam,
                                                observeI);
                                        FileOutputStream out = new FileOutputStream(String.format("%1$s/MixtureTransObserve", pathDirString));
                                        out.write(newRhsTrans.getBytes());
                                        out.close();
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                            }
                            // Reweight
                            try {
                                String newRhsTrans;
                                newRhsTrans = String.format("cobserve(sampleFrom(\"(x;%2$s,i) => PDF[%1$sDistribution[%6$s],x+Delta[i]]^(weight[i])\", %3$s,%5$s),%4$s)",
                                        dist.substring(0, 1).toUpperCase() + dist.substring(1),
                                        innerParams.substring(1),
                                        params,
                                        firstParam,
                                        observeI,
                                        innerParams2
                                );
                                FileOutputStream out = new FileOutputStream(String.format("%1$s/ReweightTransObserve", pathDirString));
                                out.write(newRhsTrans.getBytes());
                                out.close();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            // Local1
                            try {
                                String newRhsTrans;
                                newRhsTrans = String.format("cobserve(sampleFrom(\"(x;%2$s,i) => PDF[NormalDistribution[0,0.25],weight[i]]*PDF[%1$sDistribution[%5$s],x+Delta[i]]\", %3$s,%6$s),%4$s)",
                                        dist.substring(0, 1).toUpperCase() + dist.substring(1),
                                        innerParams.substring(1),
                                        params,
                                        firstParam,
                                        innerParams2.replaceFirst(",", "+(weight[i]),"),
                                        observeI
                                );
                                FileOutputStream out = new FileOutputStream(String.format("%1$s/Local1TransObserve", pathDirString));
                                out.write(newRhsTrans.getBytes());
                                out.close();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            // Local2
                            if (innerParams.substring(1).contains(",")) {
                                try {
                                    String newRhsTrans;
                                    String[] innerParamsList = innerParams2.split(",");
                                    innerParamsList[1] = innerParamsList[1] + "+(weight[i])";
                                    newRhsTrans = String.format("cobserve(sampleFrom(\"(x;%2$s,i) => PDF[NormalDistribution[0,0.25],weight[i]]*PDF[%1$sDistribution[%5$s],x+Delta[i]]\", %3$s,%6$s),%4$s)",
                                            dist.substring(0, 1).toUpperCase() + dist.substring(1),
                                            innerParams.substring(1),
                                            params,
                                            firstParam,
                                            String.join(",", innerParamsList),
                                            observeI
                                    );
                                    FileOutputStream out = new FileOutputStream(String.format("%1$s/Local2TransObserve", pathDirString));
                                    out.write(newRhsTrans.getBytes());
                                    out.close();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        } else { // mixture
                            // TODO: temp fix for gauss_mix
                            if (tempRhs.equals("target+log_mix(theta,normal_lpdf(y[n],mu[1],sigma[1]),normal_lpdf(y[n],mu[2],sigma[2]))")) {
                                System.out.println("gauss_mix");
                                System.out.println(firstParam);
                                String observeI = firstParam.split("(\\[|])", 3)[1];
                                newRhs = "cobserve(sampleFrom(\"(x;thetamix,mu,sigma,mumix,sigmamix,i) => thetamix*PDF[NormalDistribution[mu,Sqrt[sigma]],x + Delta[i]] + (1-thetamix)*PDF[NormalDistribution[mumix,Sqrt[sigmamix]],x + Delta[i]]\", theta,mu[1],sigma[1],mu[2],sigma[2],n), y[n])";
                                assignStr = newRhs;
                                assignStr += ";\n";
                                // write getMSEfromMAP in mathe file
                                String mseVar = "((mu1-2.75)^2 + (mu2+2.75)^2 + (sigma1-1)^2 +(sigma2-1)^2 + (theta-0.4)^2)";
//                                if (! paramDeclStatement.keySet().contains("theta"))
//                                    mseVar = mseVar.replace("(theta-0.4)^2)","0");
                                dumpMathe("getMSEfromMAP[mapVal_] := \n" +
                                        " Module[{}, \n" +  mseVar +
                                        " /. mapVal[[2]]" +
                                        "  ]\n");
                                dumpMathe("maxDataIdxMap[deltaFullMap_] := \n" +
                                        " Module[{maxDataIdx}, \n" +
                                        "  maxDataIdx = \n" +
                                        "   StringDelete[\n" +
                                        "    SymbolName[Keys[TakeLargest[Association[deltaFullMap], 1]][[1]]], \n" +
                                        "    \"Delta\"];\n" +
                                        "  <|dweight1 -> Symbol[\"dweight\" <> maxDataIdx], \n" +
                                        "   Delta1Rep -> Symbol[\"Delta\" <> maxDataIdx],\n" +
                                        "   betaRep -> ReleaseHold[(HoldForm[\n" +
                                        "       " + "-0.55" + "] /. (msei -> ToExpression[maxDataIdx]))],\n" +
                                        "   sigmaRep -> " + "1" + "|>\n" +
                                        "  ]\n");
                                dumpMathe("maxDataIdxMap[deltaFullMap_,maxDataIdx_] := \n" +
                                        " Module[{}, \n" +
                                        "  <|dweight1 -> Symbol[\"dweight\" <> maxDataIdx], \n" +
                                        "   Delta1Rep -> Symbol[\"Delta\" <> maxDataIdx],\n" +
                                        "   betaRep -> ReleaseHold[(HoldForm[\n" +
                                        "       " + "-0.55" + "] /. (msei -> ToExpression[maxDataIdx]))],\n" +
                                        "   sigmaRep -> " + "1" + "|>\n" +
                                        "  ]");

                                // write observe alternative for transformations
                                // original
                                try {
                                    FileOutputStream out = new FileOutputStream(String.format("%1$s/OrgObserve", pathDirString));
                                    out.write(newRhs.getBytes());
                                    out.close();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                // reparam
                                try {
                                    String newRhsTrans;
                                    newRhsTrans = "cobserve(sampleFrom(\"(x;thetamix,mu,sigma,mumix,sigmamix,i) =>  PDF[GammaDistribution[1/2,1/2],weight[i]]*(thetamix*PDF[NormalDistribution[mu,Sqrt[sigma]*weight[i]^(-1/2)],x + Delta[i]] + (1-thetamix)*PDF[NormalDistribution[mumix,Sqrt[sigmamix]*weight[i]^(-1/2)],x + Delta[i]])\", theta,mu[1],sigma[1],mu[2],sigma[2],n), y[n])";
                                    FileOutputStream out = new FileOutputStream(String.format("%1$s/ReparamTransObserve", pathDirString));
                                    out.write(newRhsTrans.getBytes());
                                    out.close();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                // Student-T
                                try {
                                    String newRhsTrans;
                                    newRhsTrans = "cobserve(sampleFrom(\"(x;thetamix,mu,sigma,mumix,sigmamix,i) =>  thetamix*PDF[StudentTDistribution[mu,Sqrt[sigma],weight],x + Delta[i]] + (1-thetamix)*PDF[StudentTDistribution[mumix,Sqrt[sigmamix],weight],x + Delta[i]]\", theta,mu[1],sigma[1],mu[2],sigma[2],n), y[n])";
                                    FileOutputStream out = new FileOutputStream(String.format("%1$s/StudentTransObserve", pathDirString));
                                    out.write(newRhsTrans.getBytes());
                                    out.close();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                // Mixture
                                if (!assignmentStatement.rhs.toString().contains("log_mix"))
                                    System.out.println("no mixture implemented");
                                // Reweight
                                try {
                                    String newRhsTrans;
                                    newRhsTrans = "cobserve(sampleFrom(\"(x;thetamix,mu,sigma,mumix,sigmamix,i) =>  (thetamix*PDF[NormalDistribution[mu,Sqrt[sigma]],x + Delta[i]]^weight[i] + (1-thetamix)*PDF[NormalDistribution[mumix,Sqrt[sigmamix]],x + Delta[i]]^weight[i])\", theta,mu[1],sigma[1],mu[2],sigma[2],n), y[n])";
                                    FileOutputStream out = new FileOutputStream(String.format("%1$s/ReweightTransObserve", pathDirString));
                                    out.write(newRhsTrans.getBytes());
                                    out.close();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                // Local1
                                try {
                                    String newRhsTrans;
                                    newRhsTrans = "cobserve(sampleFrom(\"(x;thetamix,mu,sigma,mumix,sigmamix,i) =>  PDF[NormalDistribution[0,0.25],weight[i]]*(thetamix*PDF[NormalDistribution[mu+weight[i],Sqrt[sigma]],x + Delta[i]] + (1-thetamix)*PDF[NormalDistribution[mumix+weight[i],Sqrt[sigmamix]],x + Delta[i]])\", theta,mu[1],sigma[1],mu[2],sigma[2],n), y[n])";
                                    FileOutputStream out = new FileOutputStream(String.format("%1$s/Local1TransObserve", pathDirString));
                                    out.write(newRhsTrans.getBytes());
                                    out.close();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                // Local2
                                try {
                                    String newRhsTrans;
                                    newRhsTrans = "cobserve(sampleFrom(\"(x;thetamix,mu,sigma,mumix,sigmamix,i) =>  PDF[NormalDistribution[0,0.25],weight[i]]*(thetamix*PDF[NormalDistribution[mu,Sqrt[sigma]+weight[i]],x + Delta[i]] + (1-thetamix)*PDF[NormalDistribution[mumix,Sqrt[sigmamix]+weight[i]],x + Delta[i]])\", theta,mu[1],sigma[1],mu[2],sigma[2],n), y[n])";
                                    FileOutputStream out = new FileOutputStream(String.format("%1$s/Local2TransObserve", pathDirString));
                                    out.write(newRhsTrans.getBytes());
                                    out.close();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            } else {
                                assignStr = tempRhs;
                            }

                        }
                    } // end dealing with target
                    else {
                        assignStr = assignmentStatement.lhs + " = " + newRhs + ";\n";
                    }
                }
                // for (AST.Annotation ann : statement.statement.annotations){
                //     if(ann.annotationType == AST.AnnotationType.Observe){
                //         assignStr = "observe(" + assignStr + ")";
                //     }
                // }
                output += assignStr;
                System.out.println(paramPriorNotAdded);
                if (paramPriorNotAdded.isEmpty()) {
                    output += transformparamOut;
                    // transformed param calc in mathe
                    String[] allLines = transformparamOut.replace("_","MMMM").split("\n");
                    for (String ll:allLines) {
                        if (ll.contains(":=")) {
                            if (ll.contains("array")) {
                                String[] lls = ll.split("(array\\(|\\+1,|\\))");
                                if (constMap.containsKey(lls[1]))
                                    lls[1] = constMap.get(lls[1]).toString();
                                dumpMathe(lls[0].replace(":=","=") + String.format("Table[%1$s, {i,1,%2$s}]", lls[2], lls[1]) + "\n");

                            } else {
                                dumpMathe(ll + "\n");
                            }

                        }
                        else if (ll.contains("for")) {
                            String[] lls = ll.split("(for| in |\\[|\\.\\.|\\+1\\))");
                            String newLoopBound = lls[4];
                            if (constMap.containsKey(lls[4].trim()))
                                newLoopBound = String.valueOf(constMap.get(lls[4].trim()));
                            dumpMathe(String.format("For[%1$s=%2$s,%1$s<=%3$s,%1$s++,\n",lls[1],lls[3],newLoopBound));
                        }
                        else {
                            System.out.println(ll);
                            if (ll.contains("[")){
                                String newll = ll;
                                List<String> allMatches = new ArrayList<String>();
                                Matcher m = Pattern.compile("\\w+\\[").matcher(ll);
                                while (m.find()) {
                                    allMatches.add(m.group());
                                }
                                System.out.println(allMatches);
                                for (String mm:allMatches) {
                                    String paramKey = mm.replace("MMMM","_").replace("[","");
                                    if (paramDeclStatement.containsKey(paramKey) ){
                                        AST.Decl paramDecl = paramDeclStatement.get(paramKey);
                                        if (paramDecl.annotations.size() >0 &&
                                            (paramDecl.annotations.get(0).annotationType.toString().equals("Prior") ||
                                                    paramDecl.annotations.get(0).annotationType.toString().equals("Limits")
                                            )
                                            ) // is a param
                                            newll = newll.replaceAll("\\b"+mm.replace("[","") +"\\b\\[","addrParam[" + mm.replace("[","") + ",");
                                    }

                                }
                                // not always i
                                newll = newll.replaceAll("\\[(\\w+)]","[[$1]]");
                                newll = newll.replaceAll("\\[(\\w+\\[\\[\\w+]])]","[[$1]]");
                                dumpMathe(newll.replace("{","").replace("}","]"));
                            } else if (! ll.matches("\\s+"))
                                dumpMathe(ll.replace("{","").replace("}","]\n"));
                        }

                    }
                    dumpMathe("\n");
                    transformparamOut = "";
                }
            } else if (statement.statement instanceof AST.ForLoop) {
                AST.ForLoop loop = (AST.ForLoop) statement.statement;
                output += "for " + loop.loopVar + " in [" + loop.range.start + ".." + loop.range.end + "+1) \n";
            } else if (statement.statement instanceof AST.Decl) {
                AST.Decl declaration = (AST.Decl) statement.statement;
                paramDeclStatement.put(declaration.id.toString(),declaration);
                if (declaration.annotations.size() > 0 &&
                        (declaration.annotations.get(0).annotationType.toString().equals("Prior") ||
                                declaration.annotations.get(0).annotationType.toString().equals("Limits")
                        )){
                    paramPriorNotAdded.add(declaration.id.toString());
                    if (declaration.dtype.dims != null || declaration.dims != null) {
                        String loopDim;
                        if (declaration.dtype.dims != null)
                            loopDim = declaration.dtype.dims.toString();
                        else
                            loopDim = declaration.dims.toString();
                        output += String.format(" %1$s := array(%2$s+1);\n", declaration.id, loopDim);
                        // give a flat prior
                        if (! (bodyString.contains(declaration.id + "=normal")
                                || bodyString.contains(declaration.id + "[1]=normal")
                                || bodyString.contains(declaration.id + "[k]=normal")
                                ||bodyString.contains(declaration.id + "=gamma")
                                ||bodyString.contains(declaration.id + "=cauchy")
                                ||bodyString.contains(declaration.id + "=beta")
                                || bodyString.contains(declaration.id + "=inv_gamma")
                                || bodyString.contains(declaration.id + "=uniform"))) {
                            paramPriorNotAdded.remove(declaration.id.toString());
                            output += String.format("for ppjj in [1..%1$s+1) {\n",loopDim);
                            if (declaration.annotations.size() > 1) {
                                for(AST.Annotation currAnno : declaration.annotations){
                                    if(currAnno.annotationType.toString().equals("Limits")){
                                        String lower = currAnno.annotationValue.toString().split("(<lower=|,|>)")[1];
                                        if (lower.matches("[0-9]+"))
                                            output += declaration.id + "[ppjj] = sampleFrom(\"(c) => [c>" + lower + "]\");\n";
                                    }
                                }
                            } else {
                                output += declaration.id + "[ppjj] = sampleFrom(\"(c) => [c=c]\");\n";
                            }
                            output += "}\n";

                        }
                    } else {
                        if (bodyString.contains(declaration.id + "=normal")
                                || bodyString.contains(declaration.id + "[1]=normal")
                                || bodyString.contains(declaration.id + "[k]=normal")
                                ||bodyString.contains(declaration.id + "=gamma")
                                ||bodyString.contains(declaration.id + "=cauchy")
                                ||bodyString.contains(declaration.id + "=beta")
                                || bodyString.contains(declaration.id + "=inv_gamma")
                                || bodyString.contains(declaration.id + "=uniform") ) {
                            output += declaration.id + " := 0;\n";
                        } else {
                            paramPriorNotAdded.remove(declaration.id.toString());
                            if (declaration.annotations.size() > 1) {
                                for(AST.Annotation currAnno : declaration.annotations){
                                    if(currAnno.annotationType.toString().equals("Limits")){
                                        String lower = currAnno.annotationValue.toString().split("(<lower=|,|>)")[1];
                                        if (lower.matches("[0-9]+"))
                                            output += declaration.id + " := sampleFrom(\"(c) => [c>" + lower + "]\");\n";
                                    }
                                }
                            } else {
                                output += declaration.id + " := sampleFrom(\"(c) => [c=c]\");\n";
                            }
                        }

                    }
                }
                else {
                    if (declaration.dtype.dims != null) {
                        output += String.format(" %1$s := array(%2$s+1,1);\n",declaration.id,declaration.dtype.dims);
                    } else if (declaration.dims != null){
                        output += String.format(" %1$s := array(%2$s+1,1);\n",declaration.id,declaration.dims);
                    } else {
                        output += declaration.id + " := 0;\n";

                    }
                }
            }
            else if(statement.statement instanceof AST.IfStmt){
                AST.IfStmt ifStmt = (AST.IfStmt) statement.statement;
                output += "if(" + ifStmt.condition.toString() + ")\n";
            }
        }
        return output;
    }
    @Override
    public void translate(ArrayList<Section> sections) throws Exception {
        stringBuilder = new StringBuilder();
        visited = new HashSet<>();
        for (Section section : sections){
            if(section.sectionType == SectionType.DATA){
                if(nomean) {
                    dump("def main() {\n", "");
                    nomean = false;
                }
                dump(dumpR(section.basicBlocks.get(0).getData()));
            } else if(section.sectionType == SectionType.FUNCTION){

                if(section.sectionName == "main") {
                    if(nomean) {
                        dump("def main() {\n", "");
                        nomean = false;
                    }
                }
                if (section.sectionName.equals("main")) {
                    bodyString = section.basicBlocks.toString().replaceAll("\\s+","");
                    for (BasicBlock basicBlock : section.basicBlocks) {

                        BasicBlock curBlock = basicBlock;
                        while (!visited.contains(curBlock)) {
                            visited.add(curBlock);
                            String block_text = translate_block(curBlock);
                            if (curBlock.getIncomingEdges().containsKey("true")) {
                                block_text = "{\n" + block_text;
                            }
                            // if (curBlock.getIncomingEdges().containsKey("false")) {
                            //     System.out.println("dddddddddddddfffffffffffffff");
                            //     System.out.println(block_text);
                            //     if (block_text.contains("cobserve"))
                            //         block_text = "else{\n" + block_text + "}\n";
                            // }
                            if (curBlock.getOutgoingEdges().containsKey("back")) {
                                block_text = block_text + "}\n";
                            }
                            if (curBlock.getParent().sectionName.equals("transformedparam")) {
                                transformparamOut += block_text;
                            }
                            else
                                stringBuilder.append(block_text);
                            if (curBlock.getEdges().size() > 0) {
                                BasicBlock prevBlock = curBlock;
                                if (curBlock.getEdges().size() == 1) {
                                    curBlock = curBlock.getEdges().get(0).getTarget();
                                } else {
                                    String label = curBlock.getEdges().get(0).getLabel();
                                    if (label != null && label.equalsIgnoreCase("true") && !visited.contains(curBlock.getEdges().get(0).getTarget())) {
                                        curBlock = curBlock.getEdges().get(0).getTarget();
                                    } else {
                                        curBlock = curBlock.getEdges().get(1).getTarget();
                                    }
                                }
                                if (!visited.contains(curBlock) && curBlock.getIncomingEdges().containsKey("meet") && !isIfNode(prevBlock)) {
                                    if (curBlock.getParent().sectionName.equals("transformedparam"))
                                        transformparamOut += ("}\n");
                                    else
                                        stringBuilder.append("}\n");
                                }
                            }
                        }
                    }
                }
                dump(stringBuilder.toString());

            } else if (section.sectionType == SectionType.QUERIES){
                parseQueries(section.basicBlocks.get(0).getQueries(), "");

                dump("\n}\n");
                return;
            } else {
                System.out.println("Unsupport section (ignored): " + section.sectionName + " " + section.sectionType);
                BasicBlock currBlock = section.basicBlocks.get(0);
                if (section.sectionName.equals("transformedparam")) {
                    transformparamOut = translate_block(currBlock);
                }
                else {
                    dump(translate_block(currBlock));
                }
            }
        }

    }
    private boolean isIfNode(BasicBlock basicBlock){
        return basicBlock.getStatements().size() == 1 && basicBlock.getStatements().get(0).statement instanceof AST.IfStmt;
    }
    @Override
    public Pair run() {
        return null;
    }

    public Pair run(String codeFileName){
        Pair results = Utils.runPsi(codeFileName);
        return results;
    }




    public void parseQueries(List<AST.Query> queries, String indent){
        StringBuilder retStr = new StringBuilder();
        for (AST.Decl param_i:paramDeclStatement.values()){
            if (param_i.annotations.size() > 0 &&
                    (param_i.annotations.get(0).annotationType.toString().equals("Prior") ||
                            param_i.annotations.get(0).annotationType.toString().equals("Limits")
                    )) {
                retStr.append(",");
                if (param_i.dims == null && param_i.dtype.dims == null) {
                    retStr.append(param_i.id.toString());
                } else {
                    Integer paramDims;
                    if (param_i.dims != null) {
                        if (param_i.dims.toString().matches("[1-9]+"))
                            paramDims = Integer.valueOf(param_i.dims.toString());
                        else {
                            paramDims = constMap.get(param_i.dims.toString());
                        }
                    } else {
                        System.out.println("/////////////////");
                        System.out.println(param_i.dtype.dims.toString());
                        if (param_i.dtype.dims.toString().matches("[1-9]+"))
                            paramDims = Integer.valueOf(param_i.dtype.dims.toString());
                        else
                            paramDims = constMap.get(param_i.dtype.dims.toString());
                        System.out.println(constMap);
                    }
                    System.out.println(paramDims);
                    System.out.println(param_i.toString());
                    for (Integer ii = 1; ii <= paramDims; ii++) {
                        if (ii != 1)
                            retStr.append(",");
                        retStr.append(param_i.id.id + "[" + String.valueOf(ii) + "]");
                    }

                }
            }
        }
        dump("return (" + retStr.substring(1) + ");");
    }


    public void dump(String str){
        try {
           out.write(str.getBytes());
        } catch (Exception e){
            e.printStackTrace();
        }

    }
    public void dumpMathe(String str){
        try {
            Matheout.write(str.getBytes());
        } catch (Exception e){
            e.printStackTrace();
        }

    }
    public void dump(String str, String indent){
        dump(str + indent);
    }

    public String parse(Statement s){
        return parse(s.statement);
    }

    public boolean observe(AST.Statement s, StringBuilder sb){
        boolean res = true;

        for (AST.Annotation ann : s.annotations){
            if(ann.annotationType == AST.AnnotationType.Observe){
                sb.append(String.format("observe(%s)\n", s.toString()));
            } else {
                res = false;
            }
        }
        return res;
    }
    public String parse(AST.Statement s){
        StringBuilder sb = new StringBuilder();
        if(s.annotations.size()!=0){
            if (observe(s, sb)){
                return sb.toString();
            }
        }
        if (s instanceof AST.IfStmt){
            AST.IfStmt ifstmt = (AST.IfStmt) s;
            sb.append(String.format("if (%s)", ifstmt.condition));
        } else if (s instanceof AST.AssignmentStatement) {
            AST.AssignmentStatement assign = (AST.AssignmentStatement) s;
            sb.append(assign.toString() + ";\n");
        } else if (s instanceof AST.ForLoop) {
            AST.ForLoop fl = (AST.ForLoop) s;
            sb.append(String.format("for %s in [%s .. %s) ", fl.loopVar.toString(), fl.range.start, fl.range.end));
        } else if (s instanceof AST.Decl){
            AST.Decl decl = (AST.Decl) s;
            if(decl.dtype.primitive == AST.Primitive.FLOAT){
                sb.append(decl.id.toString() + " := 1.0;\n");
            } else {
                sb.append(decl.id.toString() + " := 0;\n");
            }

        } else {
            System.out.println("not covering: " + s);
        }
        return sb.toString();


    }


}
