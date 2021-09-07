package translators;

import grammar.DataParser;
import grammar.StanBaseListener;
import grammar.StanParser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.renjin.sexp.*;
import translators.visitors.Stan2IRVisitor;
import utils.DataReader;
import utils.Utils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Stan2IRTranslator extends StanBaseListener {
    private Object curBlock;
    private String dataCode = "";
    private String modelCode = "";
    private String testfile;
    private String datafile;
    private Map<String, SEXP> datamap;
    private DataReader dataReader;

    public Stan2IRTranslator(String testfile, String data){
        this.testfile = testfile;
        this.datafile = data;
        this.datamap = new HashMap<>();
        readData2();
        StanParser parser = Utils.readStanFile(testfile);
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(this, parser.program());
    }

    private void readData2(){
        // System.out.println(this.datafile);
        DataParser parser = null;
        try {
            parser = Utils.readDataFile(this.datafile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        ParseTreeWalker walker = new ParseTreeWalker();
        dataReader = new DataReader();
        walker.walk(dataReader, parser.datafile());
    }

    /*
    private void readData(){
        try {
            String data = new String(Files.readAllBytes(Paths.get(this.datafile)));
            RenjinScriptEngineFactory factory = new RenjinScriptEngineFactory();

            ScriptEngine engine = factory.getScriptEngine();
            engine.eval(data);
            Session session = ((RenjinScriptEngine) engine).getSession();
            Environment global = session.getGlobalEnvironment();
            //System.out.println(global.getNames());
            for(String name:global.getNames()){
                //System.out.println(name);
                SEXP variable = global.getVariable(name);
                this.datamap.put(name, variable);
                if(variable.getClass() == DoubleArrayVector.class){
                    DoubleArrayVector dav = (DoubleArrayVector) variable;
                    if(variable.toString().contains("c(")){
                        // array
                        String d = "";
                        for(double x:dav){
                            d+= x+",";
                        }
                        //this.datamap.put(name, d.substring(0, d.length()-1));
                    }
                    else{
                        //this.datamap.put(name, variable.toString());
                    }
                }

            }
            //System.out.println(this.datamap);

        } catch (IOException | ScriptException e) {
            e.printStackTrace();
        }
    }
    */

    private String getFormattedData(StanParser.DeclContext ctx){
        if(ctx.type().PRIMITIVE() != null){
            if(ctx.dims() != null && ctx.dims().size() > 0) {
                if(ctx.type().getText().equals("int")){
                    System.out.println(ctx.ID().getText());
                    String d = "";
                    return "[" + d.substring(0, d.length()-1) + "]";
                }
                else{
                    DoubleArrayVector v = (DoubleArrayVector) this.datamap.get(ctx.ID().getText());
                    String d = "";
                    for(double x:v){
                        d+= x+",";
                    }
                    return "[" + d.substring(0, d.length()-1) + "]";
                }
            }
            else {
                if(ctx.type().getText().equals("int")){
                    return this.datamap.get(ctx.ID().getText()).asInt() + "";
                }
                else{
                    return this.datamap.get(ctx.ID().getText()).toString();
                }
            }
        }

        return null;
    }

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

                dataCode += " : " + this.dataReader.getData(ctx.ID().getText());
                dataCode+="\n";
            }
            else {
                dataCode += Utils.complexTypeMap(ctx.type().COMPLEX().getText())+" ";
//                String typeName = Utils.complexTypeMap(ctx.type().COMPLEX().getText());
//                dataCode += ctx.getText().replaceFirst(ctx.type().getText(), typeName);
//                if(ctx.dims().size() > 1)
                dataCode += ctx.dims(0).getText() + " ";
                dataCode += ctx.ID().getText();
                if(ctx.dims().size() > 1)
                    dataCode += ctx.dims(1).getText() ;
//                else if(ctx.dims().size() > 0)
                  //  dataCode += ctx.dims(0).getText();
                dataCode += " : " + this.dataReader.getData(ctx.ID().getText());
                dataCode+="\n";
            }
        }
        else if(curBlock == StanParser.ParamblkContext.class){
            if(ctx.limits() != null){
                modelCode+= "@limits "+ ctx.limits().getText() + "\n";
            }
            modelCode+="@prior\n";
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
                modelCode += ctx.dims(0).getText() + " ";
                modelCode += ctx.ID().getText();
                if(ctx.dims().size() > 1)
                    modelCode += ctx.dims(1).getText() ;
//                if(ctx.dims().size() > 1)
//                    modelCode += ctx.dims(0).getText() + " ";
//                modelCode += ctx.ID().getText();
//                if(ctx.dims().size() > 1)
//                    modelCode += ctx.dims(1).getText() ;
//                else if(ctx.dims().size() > 0)
//                    modelCode += ctx.dims(0).getText();
                modelCode+="\n";
            }
        }
        else{
            if(ctx.type().PRIMITIVE() != null){
                //TODO: fix dimension order
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
                modelCode += ctx.dims(0).getText() + " ";
                modelCode += ctx.ID().getText();
                if(ctx.dims().size() > 1)
                    modelCode += ctx.dims(1).getText() ;
//                if(ctx.dims().size() > 1)
//                    modelCode += ctx.dims(0).getText() + " ";
//                modelCode += ctx.ID().getText();
//                if(ctx.dims().size() > 1)
//                    modelCode += ctx.dims(1).getText() ;
//                else if(ctx.dims().size() > 0)
//                    modelCode += ctx.dims(0).getText();
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
        if (this.dataReader.getData(ctx.expression().getText().split("\\[")[0]) != null) {
            this.modelCode += "@observe\n";
        }
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
            params += new Stan2IRVisitor().visit(expr) + ",";
        }

        this.modelCode += String.format("%s(%s)", ctx.ID().getText(), params.substring(0, params.length()-1));
    }

	// @Override
    // public void enterCondition(StanParser.ConditionContext ctx) {
    //     System.out.println(ctx.getText());
    //     this.modelCode += ctx.getText().replace("|", ",");

    // }


    @Override
    public void enterAssign_stmt(StanParser.Assign_stmtContext ctx) {
        checkBlockEndAnnotation(ctx.getParent());

        this.modelCode += (String.format("%s = %s\n", new Stan2IRVisitor().visit(ctx.expression(0)), new Stan2IRVisitor().visit(ctx.expression(1)))).replace("|",",");
    }

    @Override
    public void enterFunction_call_stmt(StanParser.Function_call_stmtContext ctx) {
        checkBlockEndAnnotation(ctx.getParent());
        // Fix in lpdf |
        this.modelCode += ctx.function_call().getText().replace("|",",") + "\n";
    }

    @Override
    public void enterTarget_stmt(StanParser.Target_stmtContext ctx) {
        checkBlockEndAnnotation(ctx.getParent());
        this.modelCode += "target = target + " + new Stan2IRVisitor().visit(ctx.expression()) + "\n";
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
