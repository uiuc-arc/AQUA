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
import java.util.*;

public class PsiTranslator implements ITranslator{

    private String defaultIndent = "\t";
    private OutputStream out;

    public void setOut(OutputStream o){
        out = o;
    }

    @Override
    public void translate(ArrayList<Section> sections) throws Exception {
        for (Section section : sections){
            if(section.sectionType == SectionType.DATA){
                parseData(section.basicBlocks.get(0).getData(), "");
            } else if(section.sectionType == SectionType.FUNCTION){
                if(section.sectionName == "main"){
                    dump("def main() {\n", "");
                    for (BasicBlock bb : section.basicBlocks) {

                        parse(bb.getStatements());
                    }
                } else {
                    throw new Exception("Unsupport Function: " + section.sectionName);
                }
            } else if (section.sectionType == SectionType.QUERIES){
                parseQueries(section.basicBlocks.get(0).getQueries(), "");
                dump("\n}\n");
                return;
            } else {
                throw new Exception("Unsupport section: " + section.sectionName + " " + section.sectionType);

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
            if (d.array != null){
                dump(d.decl.id.toString() + " := ", indent);
                dump("[");
                int length = d.array.expressions.size();
                for (int i = 0; i < length; ++i){
                    dump(d.array.expressions.get(i).toString());
                    if( i != length - 1){
                        dump(", ");
                    }
                }
                dump("];\n", indent);
            } else {
                dump(d.decl.id.toString() + " := ", indent);
                dump("array(" + d.vector+ ");\n");
            }
        }

    }

    public void parse(ArrayList<Statement> stmts){
        for(Statement stmt : stmts){
            System.out.println("curre statement");
            System.out.println(stmt);
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
        dump("}");
    }
    public void parse(Statement s){
        parse(s.statement);
    }

    public void parse(AST.Expression exp ){
        dump("if ");
        dump(exp.toString() + "\n");
    }
    public boolean observe(AST.Statement s){
        boolean res = true;

        for (AST.Annotation ann : s.annotations){
            if(ann.annotationType == AST.AnnotationType.Observe){
                dump("observe(");
                dump(s.toString());
                dump(");");
            } else {
                res = false;
            }

        }
        return res;
    }
    public void parse(AST.Statement s){
        System.out.println("parsing state");
        System.out.println(s);
        if(s.annotations.size()!=0){
            if (observe(s)){
                return;
            }
        }
        if (s instanceof AST.IfStmt){
            AST.IfStmt ifstmt = (AST.IfStmt) s;
            parse(ifstmt.condition);
            //parse(ifstmt.trueBlock);
            //dump("else\n");
            System.out.println(ifstmt.elseBlock.statements.size());
            for(AST.Statement stmt : ifstmt.elseBlock.statements){
                System.out.println(stmt);
            }
            //parse(ifstmt.elseBlock);
        } else if (s instanceof AST.AssignmentStatement) {
            AST.AssignmentStatement assign = (AST.AssignmentStatement) s;
            dump(assign.toString() + ";");
        } else if (s instanceof AST.ForLoop) {
            AST.ForLoop fl = (AST.ForLoop) s;
            dump("for ");
            dump(fl.loopVar.toString());
            dump(" in " + fl.range);
            parse(fl.block);
        } else if (s instanceof AST.Decl){
            AST.Decl decl = (AST.Decl) s;
            if(decl.dtype.primitive == AST.Primitive.FLOAT){
                dump(decl.id.toString() + " := 0.0;");
            } else {
                dump(decl.id.toString() + " := 0;");
            }

        } else {
            System.out.println("not covering: " + s);
        }


    }


}
