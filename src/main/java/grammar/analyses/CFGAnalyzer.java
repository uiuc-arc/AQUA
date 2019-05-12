package grammar.cfg;

import grammar.AST;
//import javafx.util.Pair;
import org.jgrapht.Graph;
import java.lang.Exception;
import java.util.*;

public class CFGAnalyzer {


    public static void labelGenAndKill(Graph<BasicBlock,Edge> origCFG){
        Map <String, Set<Integer>> definitionLocations = new HashMap<>();
        AST.Statement astStatement;
        int statementLocation;
        Set<Integer> locations;

        ArrayList<Pair<Statement,String>> assignmentStatements = new ArrayList<>();
        ArrayList<Pair<Statement,String>> arrayAssignStatements = new ArrayList<>();
        ArrayList<Statement> nonAssignmentStatements = new ArrayList<>();

        //iterate through each basic block
        for (BasicBlock basicBlock : origCFG.vertexSet()){
            //iterate through each statement in the basic block
            for (Statement statement : basicBlock.statements){
                statementLocation = statement.id;
                astStatement = statement.statement;

                //if the statement is an Assignment
                if (isAssignment(astStatement)){
                    AST.Expression lhs = ((AST.AssignmentStatement) astStatement).lhs; //get the LHS of the assignment

                    //if it is a regular assignment such as "a := 5"
                    if (lhs instanceof AST.Id){
                        String id = ((AST.Id) lhs).id;   //fetch the id of the variable being assigned to
                        Pair<Statement,String> stmtIdPair = new Pair<>(statement,id);
                        assignmentStatements.add(stmtIdPair);
                        //if the variable is not already in the Locations Map...
                        if (!definitionLocations.containsKey(id)){
                            locations = new HashSet<Integer>(); //create a new set to map the loc to if there is none
                            locations.add(statementLocation);
                            definitionLocations.put(id,locations);
                        }
                        //when the variable IS already in the Locations Map
                        else {
                            locations = definitionLocations.get(id);  //otherwise update/add to the existing set
                            locations.add(statementLocation);
                            definitionLocations.put(id,locations);
                        }
                    }
                    //if it is an array element assignment such as "a[0] := 4"
                    else if (lhs instanceof AST.ArrayAccess){
                        String id = ((AST.ArrayAccess) lhs).id.id;
                        Pair<Statement,String> stmtIdPair = new Pair<>(statement,id);
                        arrayAssignStatements.add(stmtIdPair);

                        //if the variable is not already in the Locations Map...
                        if (!definitionLocations.containsKey(id)){
                            locations = new HashSet<Integer>(); //create a new set to map the loc to if there is none
                            locations.add(statementLocation);
                            definitionLocations.put(id,locations);
                        }
                        //when the variable IS already in the Locations Map
                        else {
                            locations = definitionLocations.get(id);  //otherwise update/add to the existing set
                            locations.add(statementLocation);
                            definitionLocations.put(id,locations);
                        }
                    }

                    //Shouldn't ever reach this point
                    else{
                        throw new RuntimeException("lhs id was neither a variable nor array access");
                    }
                }

                //non assignment statements
                else{
                    nonAssignmentStatements.add(statement);
                }
            }
        }


        //for Regular (Non-Array) assignments
        for (Pair<Statement,String> regularAssignStatementPair :  assignmentStatements){

            Statement regularAssignStatement = regularAssignStatementPair.getKey();
            String id = regularAssignStatementPair.getValue();

            //create GEN set and Abstract State
            Map<String,Set<Integer>> genMap = new HashMap<>();
            Set<Integer> genLocs = new HashSet<>();
            genLocs.add(regularAssignStatement.id); //statement id is NOT the same as the lhs variable's id
            genMap.put(id,genLocs);

            ReachingDefinitionsState genAbsState = new ReachingDefinitionsState();
            genAbsState.setVarLocations(genMap);
            regularAssignStatement.dataflowFacts.put("GEN",genAbsState);

            //create KILL set and Abstract State
            Set<Integer> killLocs = new HashSet<>();
            Map<String,Set<Integer>> killMap = new HashMap<>();
            killLocs.addAll(definitionLocations.get(id));
            killLocs.removeAll(genLocs);
            killMap.put(id,killLocs);

            ReachingDefinitionsState killAbsState = new ReachingDefinitionsState();
            killAbsState.setVarLocations(killMap);
            regularAssignStatement.dataflowFacts.put("KILL",killAbsState);

        }

        //for Array-indexed assignments
        for (Pair<Statement,String> arrayAccAssignStatementPair : arrayAssignStatements){
            Statement arrayAccAssignStmt = arrayAccAssignStatementPair.getKey();
            String id = arrayAccAssignStatementPair.getValue();

            //create GEN set and Abstract State
            Map<String,Set<Integer>> genMap = new HashMap<>();
            Set<Integer> genLocs = new HashSet<>();
            genLocs.add(arrayAccAssignStmt.id);
            genMap.put(id,genLocs);

            ReachingDefinitionsState genAbsState = new ReachingDefinitionsState();
            genAbsState.setVarLocations(genMap);
            arrayAccAssignStmt.dataflowFacts.put("GEN",genAbsState);

            //make the kill set empty
            ReachingDefinitionsState emptyState = new ReachingDefinitionsState();
            arrayAccAssignStmt.dataflowFacts.put("KILL",emptyState);
        }

        for (Statement nonAssignStatement : nonAssignmentStatements){
            labelNonAssignGenAndKill(nonAssignStatement);
        }
    }

//    public static void labelArrayAccessAssignGenAndKill(Statement arrAccessAssignStmt){
//        assert (arrAccessAssignStmt.statement instanceof AST.AssignmentStatement);
//
//    }

    public static void labelNonAssignGenAndKill(Statement nonAssignStmt){
        assert (!(nonAssignStmt.statement instanceof AST.AssignmentStatement));
        ReachingDefinitionsState emptyState = new ReachingDefinitionsState();
        nonAssignStmt.dataflowFacts.put("GEN",emptyState);
        nonAssignStmt.dataflowFacts.put("KILL",emptyState);
    }

    public static void initCFGLabels(Graph<BasicBlock,Edge> origCFG, AbstractDomain AD){
        Set<String> allUsedVars = getAllVars(origCFG);
        for (BasicBlock bb : origCFG.vertexSet()){
            for (Statement stmt : bb.statements){
                AbstractState init = AD.getInitialVal(allUsedVars);
                stmt.dataflowFacts.put("OUT",init);
            }
        }
    }


    public static boolean isAssignment(Statement s){
        AST.Statement statement = s.statement;
        return (statement instanceof AST.AssignmentStatement);
    }

    public static boolean isAssignment(AST.Statement statement){
        return (statement instanceof AST.AssignmentStatement);
    }

    public static Set<String> getAllVars(Graph<BasicBlock,Edge> origCFG){
        Set<String> allUsedVars = new HashSet<>();
        for (BasicBlock basicBlock : origCFG.vertexSet()){
            for (Statement statement : basicBlock.getStatements()){
                AST.Statement stmt = statement.statement;
                if (stmt instanceof AST.AssignmentStatement){
                    AST.AssignmentStatement assignStmt = (AST.AssignmentStatement) stmt;
                    AST.Expression lhs = assignStmt.lhs;

                    if (lhs instanceof AST.Id){
                        String id = ((AST.Id) lhs).id;
                        allUsedVars.add(id);
                    }
                    else if (lhs instanceof AST.ArrayAccess){
                        AST.Id arrayAccessId = ((AST.ArrayAccess) lhs).id;
                        String id = arrayAccessId.id;
                        allUsedVars.add(id);
                    }
                }
            }
        }
        return allUsedVars;
    }

}
