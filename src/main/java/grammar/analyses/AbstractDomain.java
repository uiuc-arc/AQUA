package grammar.cfg;

import org.jgrapht.Graph;

import java.util.Collection;
import java.util.Iterator;
import java.util.*;

public abstract class AbstractDomain {

    //this will be overriden
    public  abstract AbstractState Join(AbstractState A1, AbstractState A2);

    //this will be overriden
    public  abstract AbstractState Meet(AbstractState A1, AbstractState A2);


    //this will be overriden
    //Also: Separation of concerns. Assume that the CFG has been pre-annotated with things like Gen/Kill if needed
    public abstract AbstractState TransferFunction(Statement stmt);


    public AbstractState nFoldMeet(Collection<AbstractState> C){
        assert !C.isEmpty();

        AbstractState prev;
        Iterator<AbstractState> iterator = C.iterator();
        AbstractState curr = iterator.next();

        while (iterator.hasNext()){
            prev = curr;
            curr = iterator.next();
            curr = this.Meet(curr, prev);
        }

        return curr;
    }

    public AbstractState nFoldJoin(Collection<AbstractState> C){
        assert !C.isEmpty();

        AbstractState prev;
        Iterator<AbstractState> iterator = C.iterator();
        AbstractState curr = iterator.next();

        while (iterator.hasNext()){
            prev = curr;
            curr = iterator.next();
            curr = this.Join(curr,prev);
        }

        return curr;
    }

    //gets the bottom element of this abstract domain
    public abstract AbstractState getBottom(Collection<String> input);

    //gets the top element of this abstract domain
    public abstract AbstractState getTop(Collection<String> input);

    //gets the element to initialize each dataflow fact to
    public abstract AbstractState getInitialVal(Collection<String> input);

    //compares two abstract states from this abstract domain to see if they are equal
    public abstract boolean areEqual(AbstractState A1, AbstractState A2);

}
