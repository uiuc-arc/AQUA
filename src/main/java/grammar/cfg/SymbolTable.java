package grammar.cfg;

import grammar.AST;

import java.util.*;

public class SymbolTable {
    private Map<String, SymbolInfo> table;
    private BasicBlock parentBlock;
    private Set<SymbolTable> parents;

    public SymbolTable(BasicBlock basicBlock){
        this.parentBlock = basicBlock;
        this.table = new HashMap<>();
        this.parents = new HashSet<>();
    }

    public Set<SymbolTable> getParent(){
        return this.parents;
    }

    public void setParent(SymbolTable parent){
        this.parents.add(parent);
    }

    public BasicBlock getParentBlock(){
        return this.parentBlock;
    }

    public Set<Map.Entry<String, SymbolInfo>> getTable(){
        return this.table.entrySet();
    }

    public void addEntry(String name, SymbolInfo symbolInfo) throws Exception {
        if(this.table.containsKey(name)){
            throw new Exception("Entry already exists!");
        }
        else{
            this.table.put(name, symbolInfo);
        }
    }

    public void addEntry(AST.Decl decl, boolean isData) throws Exception{
        String name = decl.id.id;
        if(this.table.containsKey(name)){
            throw new Exception("Entry already exists!");
        }
        else {
            SymbolInfo symbolInfo = new SymbolInfo();
            symbolInfo.setSymbolName(name);
            symbolInfo.setPrior(checkPrior(decl));
            symbolInfo.setType(decl.dtype);
            symbolInfo.setLimitsString(getLimits(decl));
            symbolInfo.setData(isData);
            symbolInfo.setDims(decl.dims);
            // TODO: other annotations
            this.table.put(name, symbolInfo);
        }
    }

    public SymbolInfo fetch(String name){
        if(name == null)
            return null;

        if(this.table.containsKey(name))
            return this.table.get(name);
        else {
            for(SymbolTable parent:this.parents){
                SymbolInfo res = parent.fetch(name);
                if(res != null)
                    return res;
            }
//            SymbolTable curTable = this.parent;
//            if(curTable != null)
//                return curTable.fetch(name);
        }
        return null;
    }

    public void fork(BasicBlock basicBlock){
        SymbolTable symbolTable = basicBlock.getSymbolTable();
        symbolTable.parents.add(this);
    }

    private boolean checkPrior(AST.Statement statement){
        ArrayList<AST.Annotation> annotations = statement.annotations;
        if(annotations != null && annotations.size() > 0){
            for(AST.Annotation annotation:annotations){
                if(annotation.annotationType == AST.AnnotationType.Prior){
                    return true;
                }
            }
        }
        return false;
    }

    private String getLimits(AST.Statement statement){
        ArrayList<AST.Annotation> annotations = statement.annotations;
        if(annotations != null && annotations.size() > 0){
            for(AST.Annotation annotation:annotations){
                if(annotation.annotationType == AST.AnnotationType.Limits){
                    return annotation.annotationValue.toString();
                }
            }
        }

        return null;
    }
}
