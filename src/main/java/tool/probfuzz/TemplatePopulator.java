package tool.probfuzz;

import grammar.AST;
import grammar.Template3BaseListener;
import grammar.Template3Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.TokenStreamRewriter;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.nd4j.linalg.api.ndarray.INDArray;
import utils.CommonUtils;

import javax.json.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.stream.Collectors;

public class TemplatePopulator extends Template3BaseListener {

    private final Template3Parser parser;
    private final PLogger logger;

    private HashMap<String, ParserRuleContext> prior_dict;
    private TokenStreamRewriter rewriter;

    public TemplatePopulator(String templateFile, PLogger logger){
        this(CommonUtils.readTemplateFile(templateFile), logger);
    }

    public TemplatePopulator(Template3Parser parser, PLogger logger) {
       this.parser = parser;
       this.logger = logger;
       this.rewriter = new TokenStreamRewriter(this.parser.getTokenStream());
       prior_dict = new HashMap<>();
    }

    public void transformCode(){
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(this, this.parser.template());
    }

    public String getTransformedCode(){
        return this.rewriter.getText();
    }

    @Override
    public void enterStatement(Template3Parser.StatementContext ctx) {
        if(CommonUtils.containsAnnotation(ctx.value.annotations, AST.AnnotationType.Prior)){
            this.prior_dict.put(ctx.decl().ID.getText(), ctx.decl());
        }
    }

    @Override
    public void enterAssign(Template3Parser.AssignContext ctx) {
        if(ctx.e2.value instanceof AST.FunctionCall){
            if(((AST.FunctionCall) ctx.e2.value).isDistX){
                // find a distribution
                // TODO: generalize the support
                ArrayList<JsonObject> models = (ArrayList<JsonObject>) CommonUtils.getDistributions(null, "C", "f", true);
                JsonObject model = models.get(new Random().nextInt(models.size()));
                // decide parameters
                String args = "";
                for(JsonObject j:model.getJsonArray("args").getValuesAs(JsonObject.class)){
                    args += Utils.generateData(j.getString("type"), new int[]{1}).getDouble(0) + ",";
                }
                this.rewriter.replace(ctx.e2.getStart(), ctx.e2.getStop(), String.format("%s(%s)", model.getString("name"), args.substring(0, args.length()-1)));
            }
            else if(((AST.FunctionCall) ctx.e2.value).isDistHole){
                ArrayList<JsonObject> models = (ArrayList<JsonObject>) CommonUtils.getDistributions(null, "C", "f", true);
                int numParams = ((AST.FunctionCall) ctx.e2.value).parameters.size();
                models = new ArrayList<>(models.stream().filter(x -> x.getJsonArray("args").size() == numParams).collect(Collectors.toList()));
                JsonObject model = models.get(new Random().nextInt(models.size()));
                this.rewriter.replace(ctx.e2.getStart(), ctx.e2.getStop(), String.format("%s(%s)", model.getString("name"), ((AST.FunctionCall) ctx.e2.value).parameters.toString().replaceAll("\\[", "").replaceAll("\\]", "")));
            }
        }
    }

    @Override
    public void enterData(Template3Parser.DataContext ctx) {
        if(ctx.dtype() != null){
            if(ctx.dtype().primitive().value == AST.Primitive.FLOAT){
                INDArray data;
                if(ctx.dtype().dims() != null) {
                    int[] size = new int[ctx.dtype().value.dims.dims.size()];
                    for (int k = 0; k < size.length; k++) {
                        size[k] = Integer.parseInt(ctx.dtype().value.dims.dims.get(k).toString());
                    }
                    data = Utils.generateData("float", size);
                }
                else{
                    data = Utils.generateData("float", new int[]{1});
                }

                if (data != null) {
                    this.rewriter.replace(ctx.dtype().getStart(),
                            ctx.dtype().getStop(),
                            data.toString().replaceAll(" ", ""));
                }
            }
            else if(ctx.dtype().primitive().value == AST.Primitive.INTEGER){
                INDArray data;
                if(ctx.dtype().dims() != null) {
                    int[] size = new int[ctx.dtype().value.dims.dims.size()];
                    for (int k = 0; k < size.length; k++) {
                        size[k] = Integer.parseInt(ctx.dtype().value.dims.dims.get(k).toString());
                    }
                    data = Utils.generateData("int", size);
                }
                else{
                    data = Utils.generateData("int", new int[]{1});
                }

                if (data != null) {
                    this.rewriter.replace(ctx.dtype().getStart(),
                            ctx.dtype().getStop(),
                            data.toString().replaceAll(" ", ""));
                }
            }
        }
    }

    @Override
    public void enterExpr(Template3Parser.ExprContext ctx) {
        if(ctx.CONSTHOLE() != null){
            INDArray data = Utils.generateData("float", new int[]{1});
            if (data != null) {
                this.rewriter.replace(ctx.getStart(), ctx.getStop(), Float.toString(data.getFloat(0)));
            }
        }
    }
}
