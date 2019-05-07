package grammar.cfg;

import java.util.HashMap;
import java.util.Map;

public class SymbolTable {
    private Map<String, SymbolInfo> table;
    private BasicBlock parent;
    public SymbolTable(BasicBlock basicBlock){
        this.parent = basicBlock;
        this.table = new HashMap<>();
    }

    public void addEntry(String name, SymbolInfo symbolInfo) throws Exception {
        if(this.table.containsKey(name)){
            throw new Exception("Entry already exists!");
        }
        else{
            this.table.put(name, symbolInfo);
        }
    }

}
