package grammar.cfg;

import java.util.HashMap;
import java.util.Map;

public class SymbolTable {
    private Map<String, SymbolInfo> table;
    private BasicBlock parentBlock;
    private SymbolTable parent;

    public SymbolTable(BasicBlock basicBlock){
        this.parentBlock = basicBlock;
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

    public SymbolInfo fetch(String name){
        if(this.table.containsKey(name))
            return this.table.get(name);
        else
            return null;
    }

    public SymbolTable fork(BasicBlock basicBlock){
        SymbolTable symbolTable = new SymbolTable(basicBlock);
        symbolTable.parent = this;
        return symbolTable;
    }
}
