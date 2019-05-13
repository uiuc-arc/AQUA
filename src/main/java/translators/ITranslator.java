package translators;

import grammar.cfg.Section;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;

public interface ITranslator {
    public String translatedCode = null;

    public void translate(ArrayList<Section> sections) throws Exception;


    public Pair run();
}
