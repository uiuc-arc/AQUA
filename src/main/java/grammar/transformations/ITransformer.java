package grammar.transformations;

import grammar.cfg.Section;

import java.util.ArrayList;

public interface ITransformer<T> {

    public ArrayList<T> getAvailTransforms(ArrayList<Section> sections) throws Exception;
}
