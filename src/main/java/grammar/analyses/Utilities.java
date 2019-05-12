package grammar.analyses;
import grammar.AST;
import org.jgrapht.Graph;

import java.util.ArrayList;
import grammar.cfg.*;
public class Utilities {




    public static void printStatements(ArrayList<Statement> statements){
        for (Statement statement : statements){
            System.out.println(statement.statement);
        }
    }

    //for each statement, print it's predecessors and successors
    public static void printStamentAttributes(ArrayList<Statement> statements){
        for (Statement statement : statements){
            System.out.print("Statement is: ");
            System.out.println(statement.statement);

            ArrayList<Statement> preds = statement.getPredStatements();
            ArrayList<AST.Statement> predStmts = new ArrayList<>();
            for (Statement s : preds){
                predStmts.add(s.statement);
            }
            System.out.print("Pred Statements are:  ");
            System.out.println(predStmts);


            ArrayList<Statement> succ = statement.getSuccStatements();
            ArrayList<AST.Statement> succStmts = new ArrayList<>();
            for (Statement n : succ){
                succStmts.add(n.statement);
            }

            System.out.print("Succ Statements are:  ");
            System.out.println(succStmts);

            System.out.println("");
        }
    }

//    public static void printCFGInfo(Graph<BasicBlock,Edge> origCFG){
//        System.out.println("\nPrinting all Basic Block ids");
//        for (BasicBlock bb : origCFG.vertexSet()){
//            if (!bb.getStatements().isEmpty()){
//                System.out.print("statement: ");
//                //System.out.println(bb.statements);
//                Statement stmtWrapper = bb.getStatements().get(0);
//                AST.Statement stmt = stmtWrapper.statement;
//                System.out.println(stmt);
//                if (stmt instanceof AST.AssignmentStatement){
//                    AST.AssignmentStatement AS = (AST.AssignmentStatement) stmt;
//                    if (AS.lhs instanceof AST.ArrayAccess){
//                        AST.ArrayAccess ac = (AST.ArrayAccess) AS.lhs;
//                        System.out.println(ac.id);
//                    }
//                }
//            }
//            if (!bb.getData().isEmpty()){
//                System.out.print("data: ");
//                System.out.println(bb.getData().get(0).id);
//            }
//
//            if (!bb.getQueries().isEmpty()){
//                System.out.print("queries: ");
//                System.out.println(bb.getQueries());
//            }
//            System.out.println("");
//        }
//
//    }

    public static void printDataflowInfo(Graph<BasicBlock,Edge> origCFG,String data){
        for (BasicBlock bb : origCFG.vertexSet()){
            for (Statement st : bb.getStatements()){
                System.out.println(st.statement);
                st.printDataFlowFacts(data);
                System.out.println(" ");
            }
        }
    }


//    public static ArrayList<BasicBlock> getPredBlocks(BasicBlock bb){
//        ArrayList<BasicBlock> predBlocks = new ArrayList<>();
//        for (String key : bb.getIncomingEdges().keySet()){
//            predBlocks.add(bb.getIncomingEdges().get(key));
//        }
//        return predBlocks;
//    }

}
