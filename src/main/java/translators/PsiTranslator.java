package translators;

import grammar.AST;
import grammar.Template3Parser;
import grammar.cfg.*;
import org.apache.commons.lang3.tuple.Pair;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import utils.Utils;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Stack;

public class PsiTranslator implements ITranslator{

    private String defaultIndent = "\t";

    @Override
    public void translate(ArrayList<Section> sections) throws Exception {

    }

    @Override
    public void run() {

    }

    public void run(String codeFileName, String dataFileName){

    }

    public static void main(String args[]){
        String filename = "src/test/resources/test3.template";
        String outputFile= filename+".png";
        CFGBuilder cfg = new CFGBuilder(filename, outputFile);
        //Template3Parser parser = trans.getParser(filename);
        //AST.Program program = parser.template().value;

        PsiTranslator trans = new PsiTranslator();
        try {
            trans.translate(cfg.getSections());
        } catch (Exception e){

        }
    }


    public void parse(AST.Program program){
        dump("def main() {\n", "");
        parseData(program.data, defaultIndent);
        parse(program.statements);
        parseQueries(program.queries, defaultIndent);

        dump("\n}\n", "");

    }
    public void parseQueries(List<AST.Query> queries, String indent){
        int size = queries.size();
        dump("return ", indent);
        if(size == 1){
            dump(queries.get(0) + ";");
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
                dump(d.id.toString() + " := ", indent);
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
                dump(d.id.toString().substring(1) + " := ", indent);
                dump("array(" + d.vector+ ");\n");
            }
        }

    }

    public void parse(List<AST.Statement> stmts){
        for(AST.Statement stmt : stmts){
            parse(stmt);
            dump("\n", "");
        }
    }
    public void parse(List<AST.Statement> stmts, String indent){
        for(AST.Statement stmt : stmts){
            parse(stmt, indent);
            dump("\n", "");
        }
    }
    public void dump(String str){
        System.out.print(str);

    }
    public void dump(String str, String indent){
        System.out.print(indent + str);
    }
    public void dumpLeftParenthesis(String indent){
        dump("{\n", indent);
    }
    public void dumpRightParenthesis(String indent){
        dump("}\n", indent);
    }
    public void parse(AST.Block bb, String indent){
        dumpLeftParenthesis(indent);
        parse(bb.statements, indent + defaultIndent);
        dumpRightParenthesis(indent);
    }
    public void parse(AST.Statement s){
        parse(s, "\t");
    }

    public void parse(AST.Expression exp, String indent){
        dump("if ", indent);
        dump(exp.toString() + "\n", indent);
    }
    public void parse(AST.Statement s, String indent){
        if (s instanceof AST.IfStmt){
            AST.IfStmt ifstmt = (AST.IfStmt) s;
            parse(ifstmt.condition, indent);
            parse(ifstmt.trueBlock, indent);
            dump("else\n", indent);
            parse(ifstmt.elseBlock, indent);
        } else if (s instanceof AST.AssignmentStatement) {
            AST.AssignmentStatement assign = (AST.AssignmentStatement) s;
            dump(assign.toString() + ";", indent);
        } else if (s instanceof AST.ForLoop) {
            AST.ForLoop fl = (AST.ForLoop) s;
            dump("for ", indent);
            dump(fl.loopVar.toString());
            dump(" in " + fl.range);
            parse(fl.block, indent);
        } else if (s instanceof AST.Decl){
            AST.Decl decl = (AST.Decl) s;
            if(decl.dtype.primitive == AST.Primitive.FLOAT){
                dump(decl.id.toString() + " = 0.0;", indent);
            } else {
                dump(decl.id.toString() + " = 0;", indent);
            }

        } else {
            System.out.println("not covering: " + s);
        }


    }


}
