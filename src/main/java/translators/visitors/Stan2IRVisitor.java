package translators.visitors;

import grammar.StanBaseVisitor;
import grammar.StanParser;

import java.util.stream.Collectors;

public class Stan2IRVisitor extends StanBaseVisitor<String> {
    @Override
    public String visitCondition(StanParser.ConditionContext ctx) {
        String res = ctx.getChild(0).getText() + ",";
        for(StanParser.ExpressionContext expr: ctx.expression()){
            res+= visit(expr) + ",";
        }
        return res.substring(0, res.length()-1);
    }

    @Override
    public String visitAddop(StanParser.AddopContext ctx) {
        return visit(ctx.expression(0)) + "+" + visit(ctx.expression(1));
    }

    @Override
    public String visitMinusop(StanParser.MinusopContext ctx) {
        return visit(ctx.expression(0)) + "-" + visit(ctx.expression(1));
    }

    @Override
    public String visitMulop(StanParser.MulopContext ctx) {
        return visit(ctx.expression(0)) + "*" + visit(ctx.expression(1));
    }

    @Override
    public String visitDivop(StanParser.DivopContext ctx) {
        return visit(ctx.expression(0)) + "/" + visit(ctx.expression(1));
    }

    @Override
    public String visitExponop(StanParser.ExponopContext ctx) {
        return visit(ctx.expression(0)) + "^" + visit(ctx.expression(1));
    }

    @Override
    public String visitEq(StanParser.EqContext ctx) {
        return visit(ctx.expression(0)) + "==" + visit(ctx.expression(1));
    }

    @Override
    public String visitTernary_if(StanParser.Ternary_ifContext ctx) {
        return visit(ctx.expression(0)) + "?" + visit(ctx.expression(1)) + ":" + visit(ctx.expression(2));
    }

    @Override
    public String visitInteger(StanParser.IntegerContext ctx) {
        return ctx.getText();
    }

    @Override
    public String visitDouble(StanParser.DoubleContext ctx) {
        return ctx.getText();
    }

    @Override
    public String visitString(StanParser.StringContext ctx) {
        return ctx.getText();
    }

    @Override
    public String visitInbuilt(StanParser.InbuiltContext ctx) {
        return ctx.getText();
    }

    @Override
    public String visitDim(StanParser.DimContext ctx) {
        if(ctx.expression() != null)
            return visit(ctx.expression());
        else if(ctx.ID() != null)
            return ctx.ID().getText();
        else if(ctx.INT() != null)
            return ctx.INT().getText();
        else
            return "";
    }

    @Override
    public String visitFunction(StanParser.FunctionContext ctx) {
        return visit(ctx.function_call());
    }

    @Override
    public String visitFunction_call(StanParser.Function_callContext ctx) {
        return String.format("%s(%s)", ctx.inbuilt().getText(), String.join(",", ctx.expression().stream().map(x -> visit(x)).collect(Collectors.toList())));
    }

    @Override
    public String visitUnary(StanParser.UnaryContext ctx) {
        return "-" + visit(ctx.expression());
    }

    @Override
    public String visitId_access(StanParser.Id_accessContext ctx) {
        return ctx.getText();
    }

    @Override
    public String visitBrackets(StanParser.BracketsContext ctx) {
        return "(" + visit(ctx.expression()) + ")";
    }

    @Override
    public String visitArray_access(StanParser.Array_accessContext ctx) {
        return ctx.ID().getText()+"[" + visit(ctx.dims()) + "]";
    }

    @Override
    public String visitDims(StanParser.DimsContext ctx) {
        return String.join(",", ctx.dim().stream().map(x -> visit(x)).collect(Collectors.toList()));
    }
}
