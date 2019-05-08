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

    public String getLimitsString() {
        return limitsString;
    }

    public void setLimitsString(String limitsString) {
        this.limitsString = limitsString;
    }

    private String limitsString;
}
