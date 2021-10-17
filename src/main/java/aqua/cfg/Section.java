package aqua.cfg;

import java.util.ArrayList;
import grammar.cfg.SectionType;

public class Section {
    public SectionType sectionType;
    public ArrayList<aqua.cfg.BasicBlock> basicBlocks;
    public String sectionName;

    public Section(){
        this.basicBlocks = new ArrayList<>();
        this.sectionType = SectionType.PROGRAM;
    }

    public Section(SectionType sectionType){
        this();
        this.sectionType = sectionType;
    }

    public Section(SectionType sectionType, String sectionName){
        this(sectionType);
        this.sectionName = sectionName;
    }
}
