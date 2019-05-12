package translators;

import com.github.os72.protobuf351.ByteString;
import grammar.AST;
import grammar.Template3Parser;
import grammar.cfg.*;
import org.apache.commons.lang3.tuple.Pair;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import utils.Utils;

import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.*;

public class PsiTranslator implements ITranslator{

    private String defaultIndent = "\t";
    private OutputStream out;
    private Set<BasicBlock> visited;

    public void setOut(OutputStream o){
        out = o;
    }

    public void parseBlock(BasicBlock block){
        boolean dumpRight = false;
        ArrayList<Statement>  stmts = block.getStatements();
        if (!visited.contains(block) && stmts.size()>=1 && (block.getIncomingEdges().containsKey("true") || block.getIncomingEdges().containsKey("false"))) {
            dump("{\n");
            dumpRight = true;
        }
        if(!visited.contains(block)) {
            visited.add(block);
            parse(stmts);
            for (Edge e : block.getEdges()) {
                parseBlock(e.getTarget());
            }
        }
        if(dumpRight){
            dump("}\n");
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

            dataString = dataString.replaceAll("\\s", "").replaceAll("\\[", "").replaceAll("\\]", "");
            if (dimsString.length() == 0) {
                stringWriter.write(String.format("%s := array(%s);\n", data.decl.id, dataString));
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

    @Override
    public void translate(ArrayList<Section> sections) throws Exception {
        visited = new HashSet<>();
        for (Section section : sections){
            if(section.sectionType == SectionType.DATA){
                dump(dumpR(section.basicBlocks.get(0).getData()));
            } else if(section.sectionType == SectionType.FUNCTION){
                if(section.sectionName == "main") {
                    dump("def main() {\n", "");
                }
                    Set<BasicBlock> visitedBlock = new HashSet<>();
                    List<BasicBlock> blocks = section.basicBlocks;
                    BasicBlock currBlock = section.basicBlocks.get(0);
                    parseBlock(currBlock);

            } else if (section.sectionType == SectionType.QUERIES){
                parseQueries(section.basicBlocks.get(0).getQueries(), "");
                dump("\n}\n");
                return;
            } else {

                System.out.println("Unsupport section (ignored): " + section.sectionName + " " + section.sectionType);
                Set<BasicBlock> visitedBlock = new HashSet<>();
                List<BasicBlock> blocks = section.basicBlocks;
                BasicBlock currBlock = section.basicBlocks.get(0);
                parseBlock(currBlock);
            }

        }

    }

    @Override
    public void run() {

    }

    public void run(String codeFileName){
        Pair results = Utils.runPsi(codeFileName);
        System.out.println(results.getLeft());
        System.out.println(results.getRight());
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

    public void parseData(List<AST.Data> data, String indent){
        for(AST.Data d : data){
            switch(d.decl.dtype.primitive){
                case INTEGER:
                case FLOAT: {


                    break;
                }
                case VECTOR: {
                    System.out.println(d.decl.id);
                    dump(d.decl.id.toString() + " := ", indent);
                    dump(d.annotations.get(0).annotationValue.toString());
                    dump("\n");

                    break;
                }
                case MATRIX:
                    break;
                default:
                    System.out.println("Not support data");
            }

        }

    }

    public void parse(ArrayList<Statement> stmts){
        for(Statement stmt : stmts){
                parse(stmt);
                dump("\n");
        }
    }
    public void parse(List<AST.Statement> stmts){
        for(AST.Statement stmt : stmts){
            parse(stmt);
            dump("\n");
        }
    }
    public void dump(String str){
        try {
            this.out.write(str.getBytes());
        } catch (Exception e){
            e.printStackTrace();
        }

    }
    public void dump(String str, String indent){
        dump(str + indent);
    }
    public void parse(AST.Block bb){
        dump("{");
        parse(bb.statements);
        dump("}\n");
    }
    public void parse(Statement s){
        parse(s.statement);
    }

    public void parse(AST.Expression exp ){
        dump(exp.toString());
    }
    public boolean observe(AST.Statement s){
        boolean res = true;

        for (AST.Annotation ann : s.annotations){
            if(ann.annotationType == AST.AnnotationType.Observe){
                dump("observe(");
                dump(s.toString());
                dump(");\n");
            } else {
                res = false;
            }

        }
        return res;
    }
    public void parse(AST.Statement s){
        if(s.annotations.size()!=0){
            if (observe(s)){
                return;
            }
        }
        if (s instanceof AST.IfStmt){
            AST.IfStmt ifstmt = (AST.IfStmt) s;
            dump("if (");
            parse(ifstmt.condition);
            dump(")\n");
        } else if (s instanceof AST.AssignmentStatement) {
            AST.AssignmentStatement assign = (AST.AssignmentStatement) s;
            dump(assign.toString() + ";\n");
        } else if (s instanceof AST.ForLoop) {
            AST.ForLoop fl = (AST.ForLoop) s;
            dump(String.format("for %s in [%s .. %s) ", fl.loopVar.toString(), fl.range.start, fl.range.end));
        } else if (s instanceof AST.Decl){
            AST.Decl decl = (AST.Decl) s;
            if(decl.dtype.primitive == AST.Primitive.FLOAT){
                dump(decl.id.toString() + " := 0.0;\n");
            } else {
                dump(decl.id.toString() + " := 0;\n");
            }

        } else {
            System.out.println("not covering: " + s);
        }


    }


}
