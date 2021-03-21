package translators.visitors;

import grammar.AST;
import grammar.cfg.BasicBlock;
import grammar.cfg.SymbolInfo;
import utils.CommonUtils;

import javax.json.JsonObject;
import java.util.ArrayList;
import java.util.List;

public class PyroVisitor extends BaseVisitor {

    boolean markData;
    BasicBlock basicBlock;
    AST.Expression lhs ;
    public static ArrayList<String> dataItems = new ArrayList<>();
    private static int varCount = 0;

    public PyroVisitor(BasicBlock basicBlock, boolean markData){
        this.markData = markData;
        this.basicBlock = basicBlock;
    }

    public PyroVisitor(BasicBlock basicBlock, boolean markData, AST.Expression lhs){
        this(basicBlock, markData);
        this.lhs = lhs;
    }

    @Override
    public String evaluate(AST.FunctionCall functionCall) {
        String id = evaluate(functionCall.id);
        JsonObject currentModel = null;
        String distname = null;
        List<JsonObject> models = CommonUtils.getDistributions(null);
        for (JsonObject model : models) {
            try {
                if (model.getString("name").equals(id)) {
                    currentModel = model;
                    break;
                }
            } catch (Exception e) {
                System.out.println("Key not found");
            }
        }

        if (currentModel != null) {
            if(currentModel.containsKey("pyro")) {
                distname = currentModel.getString("pyro");
            }
            else{
                System.out.println("Distribution " + id + " not found");
            }
        }

        if(distname != null){
            String res = distname+"(";
            String dimsString = SymbolInfo.getDimsString(this.basicBlock.getSymbolTable(), lhs);
            String liftString = "torch.ones([%s])";
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

            res = res + ")";
            if(this.markData){
                return String.format("pyro.sample(\"obs\", %s, obs=%s)\n", res, this.evaluate(this.lhs));
            }
            if(!CommonUtils.isPrior(basicBlock.getLastStatement(), lhs)){
                return String.format("%s.sample()", res);
            }
            varCount++;
            return String.format("pyro.sample(\"%s\", %s)", "var"+varCount, res);
        }

        return super.evaluate(functionCall);
    }

//    @Override
//    public String evaluate(AST.MulOp mulOp) {
//        // get operands
//        // TODO: check for expressions other than Id
//        if(mulOp.op1 instanceof AST.Id && mulOp.op2 instanceof AST.Id){
//            String dimsString1 =  SymbolInfo.getDimsString(this.basicBlock.getSymbolTable(), mulOp.op1);
//            String dimsString2 = SymbolInfo.getDimsString(this.basicBlock.getSymbolTable(), mulOp.op2);
//
//            if(StringUtils.countMatches(dimsString1, ",") > StringUtils.countMatches(dimsString2, ",")){
//                return String.format("tf.squeeze(tf.matmul(%s, tf.expand_dims(%s, axis=-1)))", evaluate(mulOp.op1), evaluate(mulOp.op2));
//            }
//            else if(StringUtils.countMatches(dimsString1, ",") < StringUtils.countMatches(dimsString2, ",")){
//                return String.format("tf.squeeze(tf.matmul(tf.expand_dims(%s, axis=0), %s))", evaluate(mulOp.op1), evaluate(mulOp.op2));
//            }
//        }
//
//        return super.evaluate(mulOp);
//    }

    @Override
    public String evaluate(AST.Id id){
        if(this.markData && dataItems.contains(id.toString())){
            return "data['" + id.toString() + "']";
        }
        else
            return id.toString();
    }

    @Override
    public String evaluate(AST.ArrayAccess arrayAccess){
        if(this.markData && dataItems.contains(arrayAccess.id.id)){
            return "data['" + arrayAccess.id.id + "'][" + arrayAccess.dims.toString() + "]";
        }
        else{
            return arrayAccess.toString();
        }
    }

    @Override
    public  String evaluate(AST.AndOp andOp){
        return evaluate(andOp.op1) + " and " + evaluate(andOp.op2);
    }

    @Override
    public  String evaluate(AST.OrOp orOp){
        return evaluate(orOp.op1) + " or " + evaluate(orOp.op2);
    }
}
