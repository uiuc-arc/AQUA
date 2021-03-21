package tool.testmin.util.template;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;
import grammar.Template2BaseVisitor;
import grammar.Template2Parser;
import tool.testmin.TestMinimizer;
import tool.testmin.util.Dimension;
import tool.testmin.util.MyLogger;
import tool.testmin.util.TMUtil;

import javax.json.JsonObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class TemplateDimensionEvaluator extends Template2BaseVisitor<Dimension> {
    private static ArrayList<String> realfunctions = new ArrayList<>(Arrays.asList("log_sum_exp", "normal_cdf_log", "normal_lpdf", "mean", "sd", "gamma_p", "normal_rng"));
    private static ArrayList<String> intfunctions = new ArrayList<>(Arrays.asList("binomial_rng", "bernoulli_logit_rng"));
    private final Map<String, Dimension> dimensionMap;
    Map<String, Dimension> dimensions;


    public TemplateDimensionEvaluator(Map<String, Dimension> dimensionMap) {
        this.dimensionMap = dimensionMap;
    }

    private Dimension copyDim(Dimension dimension) {
        if (dimension == null) {
            return dimension;
        } else {
            ArrayList<String> dims = new ArrayList<>();
            for (String d : dimension.dims) {
                dims.add(d);
            }
            return new Dimension(dimension.type, dims);
        }
    }

    @Override
    public Dimension visitTranspose(Template2Parser.TransposeContext ctx) {


        return visit(ctx.expr());
    }

    @Override
    public Dimension visitDistexpr(Template2Parser.DistexprContext ctx) {
        Dimension diminner = visit(ctx.params().param(0));

        ArrayList<String> dims = new ArrayList<String>();
        if (ctx.dims() != null) {
            for (Template2Parser.ExprContext dim : ctx.dims().expr()) {
                dims.add(dim.getText());
            }
        }

        if (ctx.vectorDIMS() != null) {
            for (Template2Parser.ExprContext dim : ctx.vectorDIMS().dims().expr()) {
                dims.add(dim.getText());
            }
        }

        dims.addAll(diminner.dims);


        String currentDistribution = ctx.ID().getText();
        String type = null;
        List<JsonObject> dists = TMUtil.getDistributions(TestMinimizer.pps);
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

        Dimension dim = new Dimension(type, dims);
        return dim;
    }

    @Override
    public Dimension visitAssign(Template2Parser.AssignContext ctx) {
        if (ctx.distexpr() != null)
            return this.visit(ctx.distexpr());
        else {
            if (ctx.expr().size() > 1)
                return this.visit(ctx.expr(1));
            else
                return this.visit(ctx.expr(0));
        }
    }

    @Override
    public Dimension visitArray(Template2Parser.ArrayContext ctx) {
        if (ctx.expr() != null && ctx.expr().size() > 0) {
            Dimension dim = visit(ctx.expr(0));
            ArrayList<String> dims = new ArrayList<>();
            dims.add(Integer.toString(ctx.expr().size()));
            dims.addAll(dim.dims);
            dim.dims = dims;
            return dim;
        } else if (ctx.vector() != null && ctx.vector().size() > 0) {
            Dimension dim = visit(ctx.vector(0));
            ArrayList<String> dims = new ArrayList<>();
            dims.add(Integer.toString(ctx.vector().size()));
            dims.addAll(dim.dims);
            dim.dims = dims;
            if (dim.dims.size() == 2) {
                dim.type = "matrix";
            } else {
                dim.type = "vector";
            }
            return dim;

        } else if (ctx.array() != null && ctx.array().size() > 0) {
            Dimension dim = visit(ctx.array(0));
            ArrayList<String> dims = new ArrayList<>();
            dims.add(Integer.toString(ctx.array().size()));
            dims.addAll(dim.dims);
            dim.dims = dims;
            return dim;
        } else {
            print("Exception array type: " + ctx.getText());
            return null;
        }
    }

    @Override
    public Dimension visitDivop(Template2Parser.DivopContext ctx) {
        Dimension dim = visit(ctx.expr(0));
        return dim;
    }

    @Override
    public Dimension visitRef(Template2Parser.RefContext ctx) {
        Dimension dim = copyDim(dimensionMap.get(ctx.ID().getText()));
        if (dim == null)
            return dim;
        if (ctx.parent instanceof Template2Parser.TransposeContext) {
            if (dim.dims.size() == 2) {


                dim.dims.add(dim.dims.get(0));
                dim.dims.remove(0);
            } else if (dim.dims.size() == 1) {
                if (dim.type == "vector") {
                    dim.type = "row_vector";
                } else if (dim.type == "row_vector") {
                    dim.type = "vector";
                }
            }
        }
        return dim;
    }

    @Override
    public Dimension visitVal(Template2Parser.ValContext ctx) {
        Dimension dim = new Dimension();
        if (ctx.number().DOUBLE() != null) {
            dim.type = "real";
        } else {
            dim.type = "int";
        }
        dim.dims = new ArrayList<String>();
        return dim;
    }

    @Override
    public Dimension visitTerminal(TerminalNode node) {
        Token id = node.getSymbol();

        if (id.getType() == Template2Parser.ID) {
            Dimension dim = dimensionMap.get(id.getText());
            return copyDim(dim);
        }

        return null;
    }

    @Override
    public Dimension visitVector(Template2Parser.VectorContext ctx) {
        if (ctx.expr() != null && ctx.expr().size() > 0) {
            Dimension dim = visit(ctx.expr(0));
            ArrayList<String> dims = new ArrayList<>();
            dims.add(Integer.toString(ctx.expr().size()));
            dims.addAll(dim.dims);
            dim.dims = dims;
            dim.type = "vector";
            return dim;
        } else if (ctx.vector() != null && ctx.vector().size() > 0) {
            Dimension dim = visit(ctx.vector(0));
            ArrayList<String> dims = new ArrayList<>();
            dims.add(Integer.toString(ctx.vector().size()));
            dims.addAll(dim.dims);
            dim.dims = dims;
            if (dim.dims.size() == 2) {
                dim.type = "matrix";
            } else {
                dim.type = "vector";
            }
            return dim;

        } else if (ctx.array() != null && ctx.array().size() > 0) {
            Dimension dim = visit(ctx.array(0));
            ArrayList<String> dims = new ArrayList<>();
            dims.add(Integer.toString(ctx.array().size()));
            dims.addAll(dim.dims);
            dim.dims = dims;
            return dim;

        } else {
            print("Exception array type: " + ctx.getText());
            return null;
        }
    }

    @Override
    public Dimension visitBrackets(Template2Parser.BracketsContext ctx) {
        return visit(ctx.expr());
    }

    @Override
    public Dimension visitArray_access(Template2Parser.Array_accessContext ctx) {
        Dimension dim = visit(ctx.ID());
        if (dim == null) {
            return null;
        }
        if (ctx.dims().expr().size() == 1) {
            if (dim.type.equals("vector")) {
                dim.type = "real";
                dim.dims = new ArrayList<>();
            } else if (dim.type.equals("matrix")) {

                dim.dims.remove(0);
                if (ctx.parent instanceof Template2Parser.TransposeContext) {
                    dim.type = "row_vector";
                } else {
                    dim.type = "vector";
                }
            } else {

                dim.dims.remove(0);

            }

        } else if (ctx.dims().expr().size() == 2) {
            if (dim.type.equals("matrix")) {
                dim.type = "real";
            }

            dim.dims.remove(0);
            dim.dims.remove(0);
        }

        return dim;
    }

    @Override
    public Dimension visitAddop(Template2Parser.AddopContext ctx) {
        Dimension dim1 = visit(ctx.expr(0));
        Dimension dim2 = visit(ctx.expr(1));
        if (isPrimitive(dim1) && isPrimitive(dim2)) {
            if (dim1.type.equals("real"))
                return dim1;
            else
                return dim2;
        } else if (isPrimitive(dim1)) {
            return dim2;
        }

        return dim1;

    }

    @Override
    public Dimension visitMinusop(Template2Parser.MinusopContext ctx) {
        Dimension dim1 = visit(ctx.expr(0));
        Dimension dim2 = visit(ctx.expr(1));
        if (isPrimitive(dim1) && isPrimitive(dim2)) {
            if (dim1.type.equals("real"))
                return dim1;
            else
                return dim2;
        } else if (isPrimitive(dim1))
            return dim2;

        return dim1;
    }

    @Override
    public Dimension visitSubset(Template2Parser.SubsetContext ctx) {
        Dimension var = visit(ctx.expr(0));
        var.dims = new ArrayList<>();
        var.dims.add("(" + ctx.expr(2).getText() + " - " + ctx.expr(1).getText() + " + 1)");
        return var;
    }

    @Override
    public Dimension visitVecdivop(Template2Parser.VecdivopContext ctx) {
        return this.visit(ctx.expr(0));
    }

    @Override
    public Dimension visitVecmulop(Template2Parser.VecmulopContext ctx) {
        return this.visit(ctx.expr(0));
    }

    @Override
    public Dimension visitExponop(Template2Parser.ExponopContext ctx) {
        return visit(ctx.expr(0));
    }

    @Override
    public Dimension visitFunction_call(Template2Parser.Function_callContext ctx) {

        String functionName = ctx.FUNCTION().getText();
        print("Evaluation function dimension of : " + functionName);
        if (functionName.equals("diag_pre_multiply")) {
            Dimension dim1 = visit(ctx.params().param(0));
            Dimension dim2 = visit(ctx.params().param(1));
            print(dim1.toString());
            print(dim2.toString());
            Dimension dimr = new Dimension();
            dimr.type = "matrix";
            dimr.dims = new ArrayList<>();
            if (dim1 != null && dim2 != null) {
                if (dim1.dims.size() > 0)
                    dimr.dims.add(dim1.dims.get(0));
                if (dim2.dims.size() > 1)
                    dimr.dims.add(dim2.dims.get(1));
            }
            return dimr;

        }
        if (functionName.equals("diag_matrix")) {
            Dimension innerdim = visit(ctx.params().param(0));

            if (innerdim == null)
                return null;

            ArrayList<String> dims = new ArrayList<>();
            dims.add(innerdim.dims.get(0));
            dims.add(innerdim.dims.get(0));
            Dimension newdim = new Dimension("matrix", dims);
            return newdim;
        } else if (functionName.equals("rep_vector")) {
            Dimension dim = new Dimension(
                    "vector",
                    new ArrayList<>(Arrays.asList(ctx.params().param(1).getText())));
            return dim;
        } else if (functionName.equals("rep_matrix")) {
            Dimension dim = new Dimension(
                    "matrix",
                    new ArrayList<>(Arrays.asList(ctx.params().param(1).getText(), ctx.params().param(2).getText())));
            return dim;
        } else if (functionName.equals("rep_row_vector")) {
            Dimension dim = new Dimension(
                    "row_vector",
                    new ArrayList<>(Arrays.asList(ctx.params().param(1).getText())));
            return dim;
        } else if (functionName.equals("rep_array")) {
            String type;
            if (ctx.params().param(0).getText().contains(".")) {

                type = "real";
            } else {
                type = "int";
            }
            ArrayList<String> dims = new ArrayList<>();
            for (int i = 1; i < ctx.params().param().size(); i++) {
                dims.add(ctx.params().param(i).getText());
            }
            Dimension dim = new Dimension(type, dims);

            return dim;
        } else if (functionName.equals("max") || functionName.equals("sum")) {
            Dimension dim = visit(ctx.params().param(0));
            if (dim != null) {
                if (dim.type.equals("int")) {
                    dim.dims = new ArrayList<>();
                } else {
                    dim.type = "real";
                    dim.dims = new ArrayList<>();
                }
            }

            return dim;
        } else if (functionName.equals("categorical_rng")) {
            Dimension dim = new Dimension("int", new ArrayList<>());
            return dim;
        } else if (functionName.equals("negative_infinity")) {
            Dimension dim = new Dimension("real", new ArrayList<>());
            return dim;
        } else if (functionName.equals("to_vector")) {
            Dimension dim1 = visit(ctx.params().param(0));
            String newdim = "";
            for (String d : dim1.dims) {
                newdim = newdim + "*" + "(" + d + ")";
            }
            dim1.dims = new ArrayList<>();
            if (newdim.startsWith("*"))
                dim1.dims.add(newdim.substring(1));
            else
                dim1.dims.add(newdim);
            dim1.type = "vector";
            return dim1;
        } else if (realfunctions.contains(functionName)) {
            Dimension dimension = new Dimension("real", new ArrayList<>());
            return dimension;
        } else if (intfunctions.contains(functionName)) {
            Dimension dimension = new Dimension("int", new ArrayList<>());
            return dimension;
        } else if (functionName.endsWith("_lpmf") || functionName.endsWith("_lpdf")) {
            Dimension dimension = new Dimension("real", new ArrayList<>());
            return dimension;
        } else {
            if (ctx.params() != null && ctx.params().param().size() > 0) {

                return visit(ctx.params().param(0));
            }
        }

        return null;
    }

    @Override
    public Dimension visitFunction(Template2Parser.FunctionContext ctx) {
        return visit(ctx.function_call());
    }

    @Override
    public Dimension visitMulop(Template2Parser.MulopContext ctx) {
        Dimension dim1 = visit(ctx.expr(0));
        Dimension dim2 = visit(ctx.expr(1));

        if (dim1 == null) {
            print("No dim found: " + ctx.expr(0).getText());
            return null;
        }

        if (dim2 == null) {
            print("No dim found: " + ctx.expr(1).getText());
            return null;
        }
        print(ctx.getText());
        print(dim1.dims.toString());
        print(dim2.dims.toString());

        print("type1: " + dim1.type);
        print("type2: " + dim2.type);

        if (isPrimitive(dim1) && isPrimitive(dim2)) {
            if (dim1.type.equals("real"))
                return dim1;
            else
                return dim2;
        } else if (isPrimitive(dim1)) {

            return dim2;
        } else if (isPrimitive(dim2)) {

            return dim1;
        } else if (dim1.type.equals("row_vector") && dim2.type.equals("vector")) {
            Dimension dim = new Dimension("real", new ArrayList<>());
            return dim;
        } else if (dim1.type.equals("vector") && dim2.type.equals("row_vector")) {
            Dimension dim = new Dimension("real", new ArrayList<>());
            return dim;
        } else if (dim1.type.equals("matrix") && dim2.type.equals("vector")) {
            Dimension dim = new Dimension("vector", new ArrayList<>(Arrays.asList(dim1.dims.get(0))));
            return dim;
        } else if (dim1.type.equals("vector") && dim2.type.equals("vector")) {

            return dim1;
        } else {
            String newdim1 = dim1.dims.get(0);
            String newdim2;
            newdim2 = dim2.dims.get(1);
            Dimension newdim = new Dimension(dim1.type,
                    new ArrayList<>(Arrays.asList(newdim1, newdim2)));

            return newdim;
        }
    }

    private void print(String message) {
        String threadName = Thread.currentThread().getName();
        MyLogger logger = MyLogger.LoggerPool.get(threadName);
        if (logger != null) {
            logger.print(message, true);
        } else {
            System.out.println(message);
        }
    }

    private boolean isPrimitive(Dimension dim) {

        if (dim != null && (dim.type.equals("int") || dim.type.equals("real"))) {
            return dim.dims.size() == 0 || (dim.dims.size() == 1 && dim.dims.get(0).equals("1"));
        }

        return false;
    }
}
