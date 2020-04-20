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

import static java.lang.Math.round;

public class PsiMatheTranslator implements ITranslator{

    private String defaultIndent = "\t";
    private OutputStream out;
    private Set<BasicBlock> visited;
    private StringBuilder stringBuilder;
    private StringBuilder output;
    private String pathDirString;
    private Boolean nomean=true;
    private Integer dataReduceRatio=10;
    private String transformparamOut;
    private String bodyString;
    private HashMap<String, AST.Decl> paramDeclStatement = new HashMap<>();
    private final List<JsonObject> models= Utils.getDistributions(null);

    public void setOut(OutputStream o){
        out = o;
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

            dataString = dataString.replaceAll("\\s", "").replaceAll("\\[", "").replaceAll("\\]", "").replaceAll(".0,",",").replaceAll(".0$","");
            if (dimsString.length() == 0) {
                if(dataString.contains(".")) {
                    stringWriter.write(String.format("%s := %s;\n", data.decl.id, dataString));
                } else {
                    stringWriter.write(String.format("%s := %s;\n", data.decl.id, String.valueOf((round(Integer.valueOf(dataString)/dataReduceRatio)))));
                }
            } else if (dimsString.split(",").length == 1) {
                // stringWriter.write(String.format("%s := [%s];\n", data.decl.id, dataString));
                stringWriter.write(String.format("%1$s := readCSV(\"%1$s_data_csv\");\n", data.decl.id));
                String[] dataStringSplit = dataString.split(",");
                Integer dataLength = dataStringSplit.length;
                dataString = String.join(",",Arrays.copyOfRange(dataStringSplit,0,round(dataLength/10)));
                try {

                    FileOutputStream out = new FileOutputStream(String.format("%1$s/%2$s_data_csv",pathDirString,data.decl.id));
                    out.write(dataString.getBytes());
                    out.write("\n".getBytes());
                    out.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                System.out.println(dataString);


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
                AST.Decl lhsDecl = paramDeclStatement.get(assignmentStatement.lhs.toString());
                if (lhsDecl != null && lhsDecl.annotations.size() > 0) {
                    String dist = tempRhs.split("\\(")[0];
                    String params = tempRhs.replace(dist,"").substring(1,tempRhs.length() - dist.length() - 1);
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

                    newRhs = String.format("sampleFrom(\"(x;%2$s) => PDF[%1$sDistribution[%2$s],x]\", %3$s)",
                            dist.substring(0,1).toUpperCase() + dist.substring(1),
                            innerParams.substring(1),
                            params
                            );
                }
                else{
                    newRhs = tempRhs;
                }
                String assignStr;
                if (lhsDecl != null && (lhsDecl.dims != null || lhsDecl.dtype.dims != null)) {
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
                    assignStr = assignmentStatement.lhs + " = " + newRhs + ";\n";
                }
                // for (AST.Annotation ann : statement.statement.annotations){
                //     if(ann.annotationType == AST.AnnotationType.Observe){
                //         assignStr = "observe(" + assignStr + ")";
                //     }
                // }
                output += assignStr;
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
                    if (declaration.dtype.dims != null) {
                        output += String.format(" %1$s := array(%2$s+1);\n",declaration.id,declaration.dtype.dims);
                    } else if (declaration.dims != null){
                        output += String.format(" %1$s := array(%2$s+1);\n",declaration.id,declaration.dims);
                    } else {
                        if (bodyString.contains(declaration.id + "=normal") ||bodyString.contains(declaration.id + "=gamma") || bodyString.contains(declaration.id + "=inv_gamma") ) {
                            output += declaration.id + " := 0;\n";
                        } else {
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
                            if (curBlock.getOutgoingEdges().containsKey("back")) {
                                block_text = block_text + "}\n";
                            }
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
        int size = queries.size();
        dump("return ", indent);
        if(size == 1){
            dump(queries.get(0).id + ";");
        } else {
            dump("(");
            for (int i = 0; i < size; ++i){
                dump(queries.get(i).id);
                if( i != size - 1){
                    dump(", ");
                }
            }
            dump(");");
        }
    }


    public void dump(String str){
        try {
           out.write(str.getBytes());
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
