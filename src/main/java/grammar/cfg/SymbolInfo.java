package grammar.cfg;

import grammar.AST;

import java.util.HashSet;
import java.util.Set;

public class SymbolInfo {

    public SymbolInfo(){
        this.users = new HashSet<>();
    }

    public String getSymbolName() {
        return symbolName;
    }

    public Set<Statement> getUsers() {
        return users;
    }

    public AST.Dtype getVariableType() {
        return type;
    }

    public boolean isPriorVariable() {
        return isPrior;
    }

    public boolean isData() {
        return isData;
    }

    public boolean isLocal() {
        return isLocal;
    }

    public void setSymbolName(String symbolName) {
        this.symbolName = symbolName;
    }

    public void setType(AST.Dtype type) {
        this.type = type;
    }

    public void setPrior(boolean prior) {
        isPrior = prior;
    }

    public void setData(boolean data) {
        isData = data;
    }

    public void setLocal(boolean local) {
        isLocal = local;
    }

    public void addUser(Statement statement){
        this.users.add(statement);
    }

    private String symbolName;
    private Set<Statement> users;
    private AST.Dtype type;
    private boolean isPrior;
    private boolean isData;
    private boolean isLocal;
    private AST.Dims dims;

    public String getLimitsString() {
        return limitsString;
    }

    public void setLimitsString(String limitsString) {
        this.limitsString = limitsString;
    }

    private String limitsString;

    public AST.Dims getDims() {
        return dims;
    }

    public void setDims(AST.Dims dims) {
        this.dims = dims;
    }

    public static String getDimsString(SymbolTable symbolTable, AST.Expression expression){
        if(symbolTable == null)
            return "";

        String id = null;
        if(expression instanceof AST.Id){
            id = ((AST.Id) expression).id;
        }
        else if(expression instanceof AST.ArrayAccess){
            id = ((AST.ArrayAccess) expression).id.toString();
        }

        SymbolInfo symbolInfo = symbolTable.fetch(id);

        String dimsString = "";

        if(symbolInfo == null)
            return dimsString;

        if(symbolInfo.getVariableType().dims != null && symbolInfo.getVariableType().dims.dims.size() > 0){
            dimsString += symbolInfo.getVariableType().dims.toString();
        }

        if(symbolInfo.getDims() != null && symbolInfo.getDims().dims.size() > 0){
            if(dimsString.length() > 0)
                dimsString += ",";

            dimsString+= symbolInfo.getDims().toString();
        }

        return dimsString;
    }
}
