package translators;

import grammar.AST;
import grammar.cfg.*;
import org.apache.commons.lang3.tuple.Pair;
import utils.CommonUtils;

import java.io.IOException;
import java.io.StringWriter;
import java.util.*;

import static tool.probfuzz.Utils.runPsi;

public class PsiTranslator implements ITranslator{

    private String defaultIndent = "\t";
    //private OutputStream out;
    private Set<BasicBlock> visited;
    private StringBuilder stringBuilder;
    private String code;

//    public void setOut(OutputStream o){
//        out = o;
//    }

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
            String dataString = CommonUtils.parseData(data, 'f');
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

            dataString = dataString.replaceAll("\\s", "").replaceAll("\\[", "").replaceAll("\\]", "");
            if (dimsString.length() == 0) {
                stringWriter.write(String.format("%s := %s;\n", data.decl.id, dataString));
            } else if (dimsString.split(",").length == 1) {
                stringWriter.write(String.format("%s := [%s];\n", data.decl.id, dataString));

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
                String assignStr = assignmentStatement.lhs + " = " + assignmentStatement.rhs;
                for (AST.Annotation ann : statement.statement.annotations){
                    if(ann.annotationType == AST.AnnotationType.Observe){
                        assignStr = "observe(" + assignStr + ")";
                    }
                }
                output += assignStr + ";\n";
            } else if (statement.statement instanceof AST.ForLoop) {
                AST.ForLoop loop = (AST.ForLoop) statement.statement;
                output += "for " + loop.loopVar + " in [" + loop.range.start + ".." + loop.range.end + ") \n";
            } else if (statement.statement instanceof AST.Decl) {
                AST.Decl declaration = (AST.Decl) statement.statement;
                output += declaration.id + " := 0;\n";
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
                dump(dumpR(section.basicBlocks.get(0).getData()));
            } else if(section.sectionType == SectionType.FUNCTION){

                if(section.sectionName == "main") {
                    dump("def main() {\n", "");
                }
                if (section.sectionName.equals("main")) {
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
                dump(translate_block(currBlock));
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
        Pair results = runPsi(codeFileName);
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
        this.code += str;
    }

    public void dump(String str, String indent){
        dump(str + indent);
    }

    public String parse(Statement s){
        return parse(s.statement);
    }

    public String getCode(){
        return this.code;
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
                sb.append(decl.id.toString() + " := 0.0;\n");
            } else {
                sb.append(decl.id.toString() + " := 0;\n");
            }

        } else {
            System.out.println("not covering: " + s);
        }
        return sb.toString();


    }


}
