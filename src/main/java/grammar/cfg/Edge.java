package grammar.cfg;

import org.jgrapht.graph.DefaultEdge;

public class Edge extends DefaultEdge {

    @Override
    public BasicBlock getTarget() {
        return target;
    }

    private BasicBlock target;

    public String getLabel() {
        return label;
    }

    private String label;

    public Edge(BasicBlock target, String label){
        this.target = target;
        this.label = label;
    }

    @Override
    public String toString() {
        if(this.label == null){
            return "";
        }
        else{
            return this.label;
        }
    }
}
