package grammar.transformations;

import grammar.cfg.Section;
import grammar.cfg.Statement;

import java.util.ArrayList;
import java.util.Queue;

public abstract class BaseTransformer {

    public abstract void transform() throws Exception;
    public abstract void undo() throws Exception;
    public abstract void availTransformers(ArrayList<Section> sections, Queue<BaseTransformer> availTrans) throws Exception;
    public abstract  boolean isTransformed();

    public abstract boolean statementFilter(Statement statement);
}
