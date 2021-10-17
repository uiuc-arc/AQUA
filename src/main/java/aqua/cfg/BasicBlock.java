package aqua.cfg;
import aqua.analyses.GridState;
import com.jgraph.layout.JGraphCompoundLayout;
import grammar.cfg.Section;

public class BasicBlock extends grammar.cfg.BasicBlock {
    public GridState dataflowFacts = null;


    public BasicBlock(int blockId, Section section) {
        super(blockId, section);
    }
}
