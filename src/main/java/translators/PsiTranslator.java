package translators;

import com.github.os72.protobuf351.ByteString;
import grammar.AST;
import grammar.Template3Parser;
import grammar.cfg.*;
import org.apache.commons.lang3.tuple.Pair;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import translators.visitors.StanVisitor;
import utils.Utils;

import javax.swing.plaf.nimbus.State;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.*;

import static grammar.transformations.CFGUtil.isData;

public class PsiTranslator implements ITranslator{

    private String defaultIndent = "\t";
    private OutputStream out;
    private Set<BasicBlock> visited;

    public void setOut(OutputStream o){
        out = o;
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




    private String translate_block(BasicBlock bBlock, Set<BasicBlock> visited) {
        String output = "";

        return output;
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
                String res = translate_block(section.basicBlocks.get(0), visited);
                System.out.println(res);
                dump(res);

            } else if (section.sectionType == SectionType.QUERIES){
                parseQueries(section.basicBlocks.get(0).getQueries(), "");
                dump("\n}\n");
                return;
            } else {
                System.out.println("Unsupport section (ignored): " + section.sectionName + " " + section.sectionType);
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
