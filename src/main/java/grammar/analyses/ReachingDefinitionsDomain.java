package grammar.cfg;
import grammar.AST;
import org.jgrapht.Graph;

import java.util.HashMap;
import java.util.Map;
import java.util.*;
import java.lang.Exception;

public class ReachingDefinitionsDomain extends AbstractDomain {


    // Join for this dataflow abstract domain is Union
    @Override
    public AbstractState Join(AbstractState A1, AbstractState A2){
        ReachingDefinitionsState RD1 = (ReachingDefinitionsState) A1;   //Downcasting
        ReachingDefinitionsState RD2 = (ReachingDefinitionsState) A2;
        ReachingDefinitionsState Joined = new ReachingDefinitionsState();

        //Set Joined to have the same map as RD1
        Joined.setVarLocations(RD1.getVarLocations());

        //Now add RD2's map to Joined
        for (String key : RD2.VarLocations.keySet()) {

            Set<Integer> value = RD2.VarLocations.get(key);

            //If RD2 has a variable mapping that is already in Joined's variable mapping add all the nodes from
            //RD2's set that the variable maps to
            if (Joined.VarLocations.containsKey(key)){
                if (!value.isEmpty()) {
                    Set<Integer> old = Joined.VarLocations.get(key);
                    Set<Integer> RD2set = RD2.VarLocations.get(key);
                    Set<Integer> actual = new HashSet<>();  //need to make a NEW set because shallow copy screws things up
                    actual.addAll(old);
                    actual.addAll(RD2set);
                    Joined.VarLocations.put(key, actual);
                }
            }
            //if RD2 has a variable that is NOT in the Hashmap of RD1, then add that mapping to the Joined map
            else {
                Joined.VarLocations.put(key,value);
            }
        }
        return Joined;
    }

    @Override
    public AbstractState Meet(AbstractState A1, AbstractState A2){
        return null;
    }


    //computes set subtraction of A1 - A2
    public AbstractState Subtract(AbstractState A1, AbstractState A2){
        ReachingDefinitionsState RD1 = (ReachingDefinitionsState) A1;   //Downcasting
        ReachingDefinitionsState RD2 = (ReachingDefinitionsState) A2;
        ReachingDefinitionsState Subtracted = new ReachingDefinitionsState(RD1.getVarLocations());

        //do a deep copy
        for (String key : RD2.VarLocations.keySet()){
            Set<Integer> RD2Locs = RD2.VarLocations.get(key);
            if (Subtracted.VarLocations.containsKey(key)){
                Set<Integer> RD1Locs = Subtracted.VarLocations.get(key);
                Set<Integer> newRD1Locs = new HashSet<>();
                newRD1Locs.addAll(RD1Locs);
                newRD1Locs.removeAll(RD2Locs);
                Subtracted.VarLocations.put(key,newRD1Locs);
            }
        }
        return Subtracted;
    }

    @Override
    public AbstractState TransferFunction(Statement stmt){
        //if the basic block corresponds to a statement, check that it is an assignment statement
        AST.Statement statement = stmt.statement;

        boolean flag = false;   //trying to debug weird case with for loop
        if (statement instanceof AST.ForLoop) {
            flag = true;
        }
        ArrayList<Statement> preds = stmt.getPredStatements();

        ArrayList<AbstractState> predAbsStates = new ArrayList<>();

        if (!preds.isEmpty()) {
            //compute the Union of the OUTs of all predecessors node
            for (Statement predStmt : preds) {
                predAbsStates.add(predStmt.dataflowFacts.get("OUT"));
            }

            AbstractState joined = this.nFoldJoin(predAbsStates);

//            if (flag){
//                System.out.println("join over all Preds for loop");
//                joined.printAbsState();
//            }

            AbstractState inMinusKill = this.Subtract(joined, stmt.dataflowFacts.get("KILL"));   //(U_preds - Kill)
            AbstractState newOut = this.Join(stmt.dataflowFacts.get("GEN"), inMinusKill);        //GEn U (U_preds-Kill)

            return newOut;
        }

        //else if the statement has NO predecessors, then just return it's GEN set
        return stmt.dataflowFacts.get("GEN");
    }

    @Override
    public AbstractState getTop(Collection<String> input){
        return null;
    }

    @Override
    public AbstractState getBottom(Collection<String> input){
        Map<String,Set<Integer>> varLocations = new HashMap<>();
        Set<Integer> emptySet = new HashSet<>();    //creates an empty set
        for (String var : input){
            varLocations.put(var,emptySet);         //maps each var to an empty set
        }
        ReachingDefinitionsState Bottom = new ReachingDefinitionsState(varLocations);
        return Bottom;
    }


    @Override
    public AbstractState getInitialVal(Collection<String> input){
        return getBottom(input);
    }

    @Override
    public boolean areEqual(AbstractState A1, AbstractState A2){
        ReachingDefinitionsState RD1 = (ReachingDefinitionsState) A1;   //Downcasting
        ReachingDefinitionsState RD2 = (ReachingDefinitionsState) A2;
        if (!(RD1.VarLocations.keySet().equals(RD2.VarLocations.keySet()))){
            return false;
        }
        for (String key : RD1.VarLocations.keySet()){
            Set<Integer> rd1Set = RD1.VarLocations.get(key);
            Set<Integer> rd2Set = RD2.VarLocations.get(key);

            if (!(rd1Set.equals(rd2Set))){
                return false;
            }
        }
        //if we got to this point, they must be the same
        return true;
    }
}