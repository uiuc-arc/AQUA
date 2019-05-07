package translators;

import grammar.cfg.Section;

import java.util.ArrayList;

public interface ITranslator {
    public String translatedCode = null;

    public void translate(ArrayList<Section> sections) throws Exception;


    public void run();
}
