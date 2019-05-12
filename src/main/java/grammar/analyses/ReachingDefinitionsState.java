package grammar.analyses;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ReachingDefinitionsState extends AbstractState {

    //maps each variable to a set of program locations (the Integer is the node id of a program point where the variable
    //is defined such that this definition reaches the edge annotated with this abstract state class
    public Map<String, Set<Integer>> VarLocations;

    public ReachingDefinitionsState(){
        this.VarLocations = new HashMap<>();
    }

    //two constructors (one parametrized by a specific VarLocations map)
    //this is the copy constructor
    public ReachingDefinitionsState(Map<String,Set<Integer>> VarLocations){
        //DOES A DEEP COPY!!
        this.VarLocations = new HashMap<>();
        for (String key : VarLocations.keySet()){
            this.VarLocations.put(key,VarLocations.get(key));
        }
    }

    Map<String,Set<Integer>> getVarLocations(){
        return this.VarLocations;
    }

    //need to do a deep copy (not a shallow copy)
    void setVarLocations(Map<String,Set<Integer>> VL){
        this.VarLocations = new HashMap<>();
        for (String key : VL.keySet()){
            this.VarLocations.put(key,VL.get(key));
        }
    }

    @Override
    public void printAbsState(){
        if (!(this.VarLocations == null)) {
            for (String id : this.VarLocations.keySet()) {
                System.out.print("Key:  ");
                System.out.println(id);
                System.out.println(this.VarLocations.get(id));
            }
        }
    }
}
