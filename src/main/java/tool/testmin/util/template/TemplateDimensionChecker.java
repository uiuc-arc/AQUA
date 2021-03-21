package tool.testmin.util.template;


import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import grammar.Template2Parser;
import tool.testmin.TemplateBaseTransformer;
import tool.testmin.TestMinimizer;
import tool.testmin.util.Dimension;
import tool.testmin.util.MyLogger;
import tool.testmin.util.TMUtil;

import javax.json.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TemplateDimensionChecker extends TemplateBaseTransformer {

    Map<String, Dimension> dimensionMap;

    public TemplateDimensionChecker(Template2Parser parser) {
        super(parser, null);
        dimensionMap = new HashMap<>();
    }

    public static Map<String, Dimension> getDimensionMap(String program) {
        TemplateDimensionChecker dimensionChecker = new TemplateDimensionChecker(TMUtil.getTemplateParserFromString(program));
        new ParseTreeWalker().walk(dimensionChecker, dimensionChecker.parser.template());
        return dimensionChecker.dimensionMap;
    }

    public static String getSubstitute(String program, ParserRuleContext context) {

        return getSubstitute(program, context, null);
    }

    public static String getSubstitute(String program, ParserRuleContext context, String value) {

        TemplateDimensionEvaluator dimensionEvaluator = new TemplateDimensionEvaluator(TemplateDimensionChecker.getDimensionMap(program));

        Dimension dim = dimensionEvaluator.visit(context);


        if (dim == null) {
            return null;
        }

        System.out.println("Context:: " + context.getText() + "; Dimension:: " + dim.toString());

        if (value == null) {
            if (dim.type.equals("int")) {
                value = "1";
            } else {
                value = "1.0";
            }
        }

        if (dim.dims.size() == 0 || (!dim.type.equals("vector") && dim.dims.size() == 1 && dim.dims.get(0).trim().equals("1"))) {

            return value;
        } else if (dim.type.equals("real")) {
            String res = String.format("rep_array(%s,", value);
            for (String d : dim.dims) {
                res += d + ",";
            }
            return res.substring(0, res.length() - 1) + ")";

        } else if (dim.type.equals("int")) {
            String res = String.format("rep_array(%s,", value);
            for (String d : dim.dims) {
                res += d + ",";
            }
            return res.substring(0, res.length() - 1) + ")";
        } else if (dim.type.equals("vector")) {
            String res = String.format("rep_vector(%s,", value);
            for (String d : dim.dims) {
                res += d + ",";
            }
            return res.substring(0, res.length() - 1) + ")";
        } else if (dim.type.equals("row_vector")) {
            String res = String.format("rep_row_vector(%s,", value);
            for (String d : dim.dims) {
                res += d + ",";
            }
            return res.substring(0, res.length() - 1) + ")";
        } else if (dim.type.equals("matrix")) {
            String res = String.format("rep_matrix(%s,", value);
            for (String d : dim.dims) {
                res += d + ",";
            }
            return res.substring(0, res.length() - 1) + ")";
        } else if (dim.type.equals("simplex")) {

            String res = String.format("rep_vector(1/(%s), %s)", dim.dims.get(0), dim.dims.get(0));
            return res;
        } else {
            MyLogger logger = MyLogger.LoggerPool.get(Thread.currentThread().getName());
            if (logger != null) {
                logger.print("Unknown type" + dim.type, true);
            } else {
                System.out.println("Unknown type" + dim.type);
            }
        }

        return null;
    }

    public static String getSubstitute_old(String program, ParserRuleContext context, String value) {
        TemplateDimensionEvaluator dimensionEvaluator = new TemplateDimensionEvaluator(TemplateDimensionChecker.getDimensionMap(program));
        Dimension dim = dimensionEvaluator.visit(context);

        if (dim == null)
            return null;

        if (value == null) {
            if (dim.type.contains("i")) {
                value = "1";
            } else {
                value = "1.0";
            }
        }

        if (dim.dims.size() == 0) {
            return value;
        } else {
            String res = String.format("rep_val(%s,", value);
            for (String d : dim.dims) {
                res += d + ",";
            }
            return res.substring(0, res.length() - 1) + ")";
        }

    }

    @Override
    public void enterDecl(Template2Parser.DeclContext ctx) {
        if (ctx.dtype().primitive() != null) {
            ArrayList<String> dims = new ArrayList<>();
            if (ctx.dims() != null && ctx.dims().size() > 0) {
                if (ctx.dims().size() > 1) {
                    for (Template2Parser.ExprContext d : ctx.dims(1).expr()) {
                        dims.add(d.getText());
                    }
                }

                for (Template2Parser.ExprContext d : ctx.dims(0).expr()) {
                    dims.add(d.getText());
                }
            }
            Dimension dimension = new Dimension(ctx.dtype().primitive().getText().replaceAll("float", "real"), dims);

            dimensionMap.put(ctx.ID().getText(), dimension);
        } else if (ctx.dtype().COMPLEX() != null) {
            ArrayList<String> dims = new ArrayList<>();
            String type = "";
            if (ctx.dims() != null && ctx.dims().size() > 0) {
                if (ctx.dims().size() > 1) {
                    for (Template2Parser.ExprContext d : ctx.dims(1).expr()) {
                        dims.add(d.getText());
                    }
                    if (ctx.dtype().getText().equals("vector")) {
                        type = "matrix";
                    }
                }

                for (Template2Parser.ExprContext d : ctx.dims(0).expr()) {
                    dims.add(d.getText());
                }

                String ctxtype = ctx.dtype().COMPLEX().getText();
                if (ctxtype.equalsIgnoreCase("cov_matrix") || ctxtype.equalsIgnoreCase("corr_matrix") || ctxtype.equalsIgnoreCase("cholesky_factor_cov") || ctxtype.equalsIgnoreCase("cholesky_factor_corr")) {
                    type = "matrix";
                    dims.add(dims.get(0));
                }
            }

            Dimension dimension = new Dimension(ctx.dtype().COMPLEX().getText(), dims);
            if (type != null && !type.isEmpty()) {
                dimension.type = type;
            }
            dimensionMap.put(ctx.ID().getText(), dimension);
        } else {
            print("Unknown");
        }
    }

    @Override
    public void enterPrior(Template2Parser.PriorContext ctx) {

        Template2Parser.DistexprContext distexpr = ctx.distexpr();
        ArrayList<String> dims = new ArrayList<String>();

        if (distexpr.dims() != null) {
            for (Template2Parser.ExprContext dim : distexpr.dims().expr()) {
                dims.add(dim.getText());
            }
        }
        if (distexpr.vectorDIMS() != null) {
            for (Template2Parser.ExprContext dim : distexpr.vectorDIMS().dims().expr()) {
                dims.add(dim.getText());
            }
            if (distexpr.ID().getText().equals("lkj_corr_cholesky") || distexpr.ID().getText().equals("lkj_corr")) {

                dims.add(dims.get(0));
            }
        }


        String currentDistribution = distexpr.ID().getText();
        String type = null;
        List<JsonObject> dists = TMUtil.getDistributions(TestMinimizer.pps);
        for (JsonObject model : dists) {
            try {
                if (model.getString("name").equals(currentDistribution)) {
                    if (TMUtil.isInteger(model.getString("support"))) {
                        type = "int";
                    } else {
                        type = "real";
                    }
                    break;
                }
            } catch (Exception e) {
                System.out.println("Key not found");
                continue;
            }
        }

        if (distexpr.vectorDIMS() != null) {
            if (dims.size() == 2) {
                type = "matrix";
            } else {
                type = "vector";
            }
        }

        Dimension dimension = new Dimension(type, dims);


        dimensionMap.put(TMUtil.getParamName(ctx), dimension);
    }

    @Override
    public void enterData(Template2Parser.DataContext ctx) {

        if (ctx.distexpr() != null) {
            TemplateDimensionEvaluator dimensionEvaluator = new TemplateDimensionEvaluator(this.dimensionMap);
            Dimension dim = dimensionEvaluator.visit(ctx.distexpr());
            dimensionMap.put(ctx.ID().getText(), dim);
        } else if (ctx.dtype() != null) {


            print("exception: Got type!");

        } else if (ctx.expr() != null) {
            TemplateDimensionEvaluator dimensionEvaluator = new TemplateDimensionEvaluator(this.dimensionMap);
            Dimension dim = dimensionEvaluator.visit(ctx.expr());
            dimensionMap.put(ctx.ID().getText(), dim);
        } else if (ctx.array() != null) {
            TemplateDimensionEvaluator dimensionEvaluator = new TemplateDimensionEvaluator(this.dimensionMap);
            Dimension dim = dimensionEvaluator.visit(ctx.array());
            dimensionMap.put(ctx.ID().getText(), dim);
        } else if (ctx.vector() != null) {
            TemplateDimensionEvaluator dimensionEvaluator = new TemplateDimensionEvaluator(this.dimensionMap);
            Dimension dim = dimensionEvaluator.visit(ctx.vector());
            dimensionMap.put(ctx.ID().getText(), dim);
        }

        if (ctx.dims() != null) {
            Dimension dim = dimensionMap.get(ctx.ID().getText());
            ArrayList<String> dims = new ArrayList<>();
            for (Template2Parser.ExprContext e : ctx.dims().expr()) {
                dims.add(e.getText());
            }
            dim.dims = dims;
            print("Getting dims from template: " + ctx.ID().getText());
        }

    }
}