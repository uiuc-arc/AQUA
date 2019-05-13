package translators.visitors;

import grammar.AST;
import grammar.cfg.BasicBlock;
import grammar.cfg.SymbolInfo;
import org.apache.commons.lang3.StringUtils;
import translators.visitors.BaseVisitor;
import utils.Utils;

import javax.json.JsonObject;
import java.util.ArrayList;
import java.util.List;

public class Edward2Visitor extends BaseVisitor {

    boolean markData;
    BasicBlock basicBlock;
    AST.Expression lhs ;

    public Edward2Visitor(BasicBlock basicBlock, boolean markData){
        this.markData = markData;
        this.basicBlock = basicBlock;
    }

    public Edward2Visitor(BasicBlock basicBlock, boolean markData, AST.Expression lhs){
        this(basicBlock, markData);
        this.lhs = lhs;
    }

    public static ArrayList<String> dataItems = new ArrayList<>();

    @Override
    public String evaluate(AST.FunctionCall functionCall) {
        String id = evaluate(functionCall.id);
        JsonObject currentModel = null;
        String distname = null;
        List<JsonObject> models = Utils.getDistributions(null);
        for (JsonObject model : models) {
            try {
                if (model.getString("name").equals(id)) {
                    currentModel = model;
                    break;
                }
            } catch (Exception e) {
                System.out.println("Key not found");
                continue;
            }
        }

        if (currentModel != null) {
            if(currentModel.containsKey("edward2")) {
                distname = currentModel.getString("edward2");
            }
            else{
                System.out.println("Distribution " + id + " not found");
            }
        }

        if(distname != null){
            String res = distname+"(";
            String dimsString = SymbolInfo.getDimsString(this.basicBlock.getSymbolTable(), lhs);
            String liftString = "tf.ones([%s])";
            if(dimsString.length() > 0){
                    liftString = String.format(liftString, dimsString);
            }
            else{
                liftString = "";
            }

            for (AST.Expression e : functionCall.parameters) {
                // raise constant value to proper dimension
                if(e instanceof AST.Number && liftString.length() > 0){
                    res += evaluate(e) +  "*" + liftString + ",";
                }
                else {
                    res += evaluate(e) + ",";
                }
            }
            if (res.endsWith(","))
                res = res.substring(0, res.length() - 1);

            return res + ")";
        }

        return super.evaluate(functionCall);
    }

    @Override
    public String evaluate(AST.MulOp mulOp) {

        // get operands
        // TODO: check for expressions other than Id
        if(mulOp.op1 instanceof AST.Id && mulOp.op2 instanceof AST.Id){
            String dimsString1 =  SymbolInfo.getDimsString(this.basicBlock.getSymbolTable(), mulOp.op1);
            String dimsString2 = SymbolInfo.getDimsString(this.basicBlock.getSymbolTable(), mulOp.op2);

            if(StringUtils.countMatches(dimsString1, ",") > StringUtils.countMatches(dimsString2, ",")){
                return String.format("tf.squeeze(tf.matmul(%s, tf.expand_dims(%s, axis=-1)))", evaluate(mulOp.op1), evaluate(mulOp.op2));
            }
            else if(StringUtils.countMatches(dimsString1, ",") < StringUtils.countMatches(dimsString2, ",")){
                return String.format("tf.squeeze(tf.matmul(tf.expand_dims(%s, axis=0), %s))", evaluate(mulOp.op1), evaluate(mulOp.op2));
            }
        }

        return super.evaluate(mulOp);
    }

    @Override
    public String evaluate(AST.Id id){
        if(this.markData && dataItems.contains(id.toString())){
            return "data['" + id.toString() + "']";
        }
        else
            return id.toString();
    }

    private boolean hasDims(SymbolInfo symbolInfo){
        if(symbolInfo.getDims() != null && symbolInfo.getDims().dims.size() > 0){
            return true;
        }
        else if(symbolInfo.getVariableType().dims != null && symbolInfo.getVariableType().dims.dims.size() > 0){
            return true;
        }

        return false;
    }
}
