package tool.testmin.util.template;

import org.antlr.v4.runtime.tree.ParseTreeWalker;
import grammar.Template2Parser;
import tool.testmin.TemplateBaseTransformer;
import tool.testmin.TestMinimizer;
import tool.testmin.util.MyLogger;
import tool.testmin.util.TMUtil;

import javax.json.JsonObject;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class ParamReplacer extends TemplateBaseTransformer {

    private static boolean InBlock = false;
    private static boolean InSample = false;
    public String value;
    private boolean InParamBlk;
    private String paramName;
    private String dims;
    private String[] rulesNames;
    private int counter;
    private boolean modifiedTree;
    private String program;
    private MyLogger logger;

    public ParamReplacer(Template2Parser parser, MyLogger logger, String paramName, int counter) {
        super(parser, logger);
        this.logger = logger;
        this.paramName = paramName;
        this.counter = counter;
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(this, parser.template());
    }

    @Override
    public void enterPrior(Template2Parser.PriorContext ctx) {
        counter--;

        if (counter == 0) {
            print("Param replacer : " + TMUtil.getParamName(ctx));

            String currentDistribution = ctx.distexpr().ID().getText();
            List<JsonObject> dists = TMUtil.getDistributions(TestMinimizer.pps);
            String type = null;
            for (JsonObject model : dists) {
                try {
                    if (model.getString("name").equals(currentDistribution)) {
                        type = model.getString("support");
                        break;
                    }
                } catch (Exception e) {
                    System.out.println("Key not found");
                    continue;
                }
            }

            String rep = "";

            if (Arrays.asList(new String[]{"f", "f+", "0f+", "f[]", "[f]"}).contains(type)) {
                rep = Double.toString(new Random().nextDouble());
            } else if (Arrays.asList(new String[]{"i", "i+", "0i+", "i[]"}).contains(type)) {
                rep = Integer.toString(new Random().nextInt(5));
            } else if (Arrays.asList(new String[]{"(0,1)", "p"}).contains(type)) {
                rep = Double.toString(new Random().nextDouble());
            } else if (Arrays.asList(new String[]{"b"}).contains(type)) {
                rep = Integer.toString(new Random().nextInt(2));
            } else if (Arrays.asList(new String[]{"[0,N]"}).contains(type)) {
                rep = Integer.toString(new Random().nextInt(2));
            } else if (Arrays.asList(new String[]{"[alpha, beta]"}).contains(type)) {

                rep = Double.toString(new Random().nextDouble());
            } else {
                print("Unsupported parameter type " + type);
                this.value = null;
                return;
            }

            this.value = rep;
            print("Replacement for " + this.paramName + " : " + this.value);

            if (ctx.distexpr().vectorDIMS() != null) {
                if (ctx.distexpr().dims() != null) {
                    this.dims = ctx.distexpr().dims().getText() + "," + ctx.distexpr().vectorDIMS().dims().getText();
                } else {
                    this.dims = ctx.distexpr().vectorDIMS().dims().getText();
                }
            } else {
                if (ctx.distexpr().dims() != null) {
                    this.dims = ctx.distexpr().dims().getText();
                } else {
                    this.dims = "1";
                }
            }

        }
    }

}
