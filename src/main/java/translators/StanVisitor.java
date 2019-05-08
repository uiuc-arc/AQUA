package translators;

import grammar.AST;

import java.lang.reflect.InvocationTargetException;

public class StanVisitor {
    public static String evaluate(AST.Id id){
        return id.toString();
    }

    public static String evaluate(AST.ArrayAccess arrayAccess){
        return arrayAccess.toString();
    }

    public static String evaluate(AST.AddOp addOp){
        return evaluate(addOp.op1) + "+" + evaluate(addOp.op2);
    }

    public static String evaluate(AST.MinusOp minusOp){
        return evaluate(minusOp.op1) + "+" + evaluate(minusOp.op2);
    }

    public static String evaluate(AST.DivOp divOp){
        return evaluate(divOp.op1) + "+" + evaluate(divOp.op2);
    }

    public static String evaluate(AST.ExponOp exponOp){
        return evaluate(exponOp.base) + "+" + evaluate(exponOp.power);
    }

    public static String evaluate(AST.Expression expression){
        if(expression instanceof AST.Id){
            return evaluate((AST.Id) expression);
        }
        else if(expression instanceof AST.ArrayAccess){
            return evaluate((AST.ArrayAccess) expression);
        }
        else if(expression instanceof AST.AddOp){
            return evaluate((AST.AddOp)expression);
        }

        else{
            return expression.toString();
        }
    }

    public static String evaluate(AST.Dims dims){
        return "[" + dims.toString() + "]";
    }
}
