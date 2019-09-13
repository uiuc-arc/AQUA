package translators;

import grammar.AST;
import grammar.StanBaseListener;
import grammar.StanListener;
import grammar.StanParser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.TerminalNode;
import utils.Utils;

import java.util.Arrays;

public class Stan2IRTranslator extends StanBaseListener {
    private Object curBlock;
    private String dataCode = "";
    private String modelCode = "";

    @Override
    public void enterDecl(StanParser.DeclContext ctx) {
        checkBlockEndAnnotation(ctx.getParent());
        if(curBlock == StanParser.DatablkContext.class){
            if(ctx.limits() != null){
                dataCode+= "@limits "+ ctx.limits().getText() + "\n";
            }
            if(ctx.type().PRIMITIVE() != null){
                dataCode += ctx.type().PRIMITIVE().getText().replace("real", "float")+" ";
                if(ctx.dims().size() > 1)
                    dataCode += ctx.dims(0).getText() + " ";
                dataCode += ctx.ID().getText();
                if(ctx.dims().size() > 1)
                    dataCode += ctx.dims(1).getText() ;
                else if(ctx.dims().size() > 0)
                    dataCode += ctx.dims(0).getText();
                dataCode+="\n";
            }
            else {
                dataCode += Utils.complexTypeMap(ctx.type().COMPLEX().getText())+" ";
                if(ctx.dims().size() > 1)
                    dataCode += ctx.dims(0).getText() + " ";
                dataCode += ctx.ID().getText();
                if(ctx.dims().size() > 1)
                    dataCode += ctx.dims(1).getText() ;
                else if(ctx.dims().size() > 0)
                    dataCode += ctx.dims(0).getText();
                dataCode+="\n";
            }
        }
        else if(curBlock == StanParser.ParamblkContext.class){
            if(ctx.limits() != null){
                dataCode+= "@limits "+ ctx.limits().getText() + "\n";
            }
            dataCode+="@prior\n";
            if(ctx.type().PRIMITIVE() != null){
                dataCode += ctx.type().PRIMITIVE().getText().replace("real", "float")+" ";
                if(ctx.dims().size() > 1)
                    dataCode += ctx.dims(0).getText() + " ";
                dataCode += ctx.ID().getText();
                if(ctx.dims().size() > 1)
                    dataCode += ctx.dims(1).getText() ;
                else if(ctx.dims().size() > 0)
                    dataCode += ctx.dims(0).getText();
                dataCode+="\n";
            }
            else {
                dataCode += Utils.complexTypeMap(ctx.type().COMPLEX().getText())+" ";
                if(ctx.dims().size() > 1)
                    dataCode += ctx.dims(0).getText() + " ";
                dataCode += ctx.ID().getText();
                if(ctx.dims().size() > 1)
                    dataCode += ctx.dims(1).getText() ;
                else if(ctx.dims().size() > 0)
                    dataCode += ctx.dims(0).getText();
                dataCode+="\n";
            }
        }
        else{
            if(ctx.type().PRIMITIVE() != null){
                modelCode += ctx.type().PRIMITIVE().getText().replace("real", "float")+" ";
                if(ctx.dims().size() > 1)
                    modelCode += ctx.dims(0).getText() + " ";
                modelCode += ctx.ID().getText();
                if(ctx.dims().size() > 1)
                    modelCode += ctx.dims(1).getText() ;
                else if(ctx.dims().size() > 0)
                    modelCode += ctx.dims(0).getText();
                modelCode+="\n";
            }
            else {
                modelCode += Utils.complexTypeMap(ctx.type().COMPLEX().getText())+" ";
                if(ctx.dims().size() > 1)
                    modelCode += ctx.dims(0).getText() + " ";
                modelCode += ctx.ID().getText();
                if(ctx.dims().size() > 1)
                    modelCode += ctx.dims(1).getText() ;
                else if(ctx.dims().size() > 0)
                    modelCode += ctx.dims(0).getText();
                modelCode+="\n";
            }
        }
    }

    @Override
    public void enterFor_loop_stmt(StanParser.For_loop_stmtContext ctx) {
        checkBlockEndAnnotation(ctx.getParent());
        this.modelCode+= String.format("for(%s in %s){\n", ctx.ID().getText(), ctx.range_exp().getText());
    }

    @Override
    public void exitFor_loop_stmt(StanParser.For_loop_stmtContext ctx) {
        this.modelCode += "}\n";
    }

    @Override
    public void enterSample(StanParser.SampleContext ctx) {
        checkBlockEndAnnotation(ctx.getParent());
        this.modelCode += String.format("%s = ", ctx.expression().getText());
    }

    private void checkBlockEndAnnotation(ParserRuleContext ctx) {
        if(Utils.checkIfLastStatement(ctx)){
            if(this.curBlock == StanParser.Transformed_data_blkContext.class){
                this.modelCode += "@blk end transformeddata\n";
            }
            else if(this.curBlock ==  StanParser.Transformed_param_blkContext.class){
                this.modelCode += "@blk end transformedparam\n";
            }
            else if(this.curBlock == StanParser.Generated_quantities_blkContext.class){
                this.modelCode += "@blk end generatedquantities\n";
            }
        }
    }

    @Override
    public void enterIf_stmt(StanParser.If_stmtContext ctx) {
        checkBlockEndAnnotation(ctx.getParent());
        this.modelCode += String.format("if(%s){\n", ctx.expression().getText());
    }

    @Override
    public void exitIf_stmt(StanParser.If_stmtContext ctx) {
        this.modelCode += "}\n";
    }

    @Override
    public void enterBlock(StanParser.BlockContext ctx) {
        if(ctx.getParent() instanceof StanParser.If_stmtContext){
            if(((StanParser.If_stmtContext) ctx.getParent()).block().size() > 1 && ctx == ((StanParser.If_stmtContext) ctx.getParent()).block(1)){
                this.modelCode += "}else {\n";
            }
        }
    }

    @Override
    public void enterPrint_stmt(StanParser.Print_stmtContext ctx) {
        checkBlockEndAnnotation(ctx.getParent());
        this.modelCode += ctx.getText()+ "\n";
    }

    @Override
    public void exitSample(StanParser.SampleContext ctx) {
        this.modelCode += "\n";
    }

    @Override
    public void enterDistribution_exp(StanParser.Distribution_expContext ctx) {
        String params = "";
        for(StanParser.ExpressionContext expr: ctx.expression()){
            params += expr.getText() + ",";
        }

        this.modelCode += String.format("%s(%s)", ctx.ID().getText(), params.substring(0, params.length()-1));
    }



    @Override
    public void enterAssign_stmt(StanParser.Assign_stmtContext ctx) {
        checkBlockEndAnnotation(ctx.getParent());
        this.modelCode += String.format("%s = %s\n", ctx.expression(0).getText(), ctx.expression(1).getText());
    }

    @Override
    public void enterFunction_call_stmt(StanParser.Function_call_stmtContext ctx) {
        checkBlockEndAnnotation(ctx.getParent());
        this.modelCode += ctx.function_call().getText() + "\n";
    }

    @Override
    public void enterTarget_stmt(StanParser.Target_stmtContext ctx) {
        checkBlockEndAnnotation(ctx.getParent());
        this.modelCode += "target = target + " + ctx.expression().getText() + "\n";
    }

    @Override
    public void enterDatablk(StanParser.DatablkContext ctx) {
        this.curBlock = ctx.getClass();
    }

    @Override
    public void enterModelblk(StanParser.ModelblkContext ctx) {
        this.curBlock = ctx.getClass();
    }

    @Override
    public void enterTransformed_data_blk(StanParser.Transformed_data_blkContext ctx) {
        this.curBlock = ctx.getClass();
        this.modelCode += "@blk start transformeddata\n";
    }

    @Override
    public void enterTransformed_param_blk(StanParser.Transformed_param_blkContext ctx) {
        this.curBlock = ctx.getClass();
        this.modelCode += "@blk start transformedparam\n";
    }

    @Override
    public void enterFunctions_blk(StanParser.Functions_blkContext ctx) {
        this.curBlock = ctx.getClass();
    }

    @Override
    public void enterGenerated_quantities_blk(StanParser.Generated_quantities_blkContext ctx) {
        this.curBlock = ctx.getClass();
        this.modelCode += "@blk start generatedquantities\n";
    }

    @Override
    public void enterParamblk(StanParser.ParamblkContext ctx) {
        this.curBlock = ctx.getClass();
    }

    public String getCode(){
        return this.dataCode + this.modelCode;
    }
}
