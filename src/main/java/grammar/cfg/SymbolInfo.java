package grammar.cfg;

import grammar.AST;

import java.util.Set;

public class SymbolInfo {
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

    private String symbolName;
    private Set<Statement> users;
    private AST.Dtype type;
    private boolean isPrior;
    private boolean isData;
    private boolean isLocal;
}
