package translators.visitors;

import grammar.AST;

public abstract class BaseVisitor {
    public String evaluate(AST.Id id){
        return id.toString();
    }

    public  String evaluate(AST.ArrayAccess arrayAccess){
        return arrayAccess.toString();
    }

    public  String evaluate(AST.AddOp addOp){
        return evaluate(addOp.op1) + "+" + evaluate(addOp.op2);
    }

    public  String evaluate(AST.MinusOp minusOp){
        return evaluate(minusOp.op1) + "-" + evaluate(minusOp.op2);
    }

    public  String evaluate(AST.DivOp divOp){
        return evaluate(divOp.op1) + "/" + evaluate(divOp.op2);
    }

    public  String evaluate(AST.ExponOp exponOp){
        return evaluate(exponOp.base) + "^" + evaluate(exponOp.power);
    }

    public  String evaluate(AST.UnaryExpression unaryExpression){
        return "-" + evaluate(unaryExpression.expression);
    }

    public String evaluate(AST.MulOp mulOp){
        return evaluate(mulOp.op1)+ "*" + evaluate(mulOp.op2);
    }

    public  String evaluate(AST.Number number){
        return number.toString();
    }

    public  String evaluate(AST.FunctionCall functionCall){
        String res = evaluate(functionCall.id) + "(";
        for (AST.Expression e : functionCall.parameters) {
            res += evaluate(e) + ",";
        }
        if (res.endsWith(","))
            res = res.substring(0, res.length() - 1);
        return res + ")";
    }

    public  String evaluate(AST.Expression expression){
        if(expression instanceof AST.Id){
            return evaluate((AST.Id) expression);
        }
        else if(expression instanceof AST.ArrayAccess){
            return evaluate((AST.ArrayAccess) expression);
        }
        else if(expression instanceof AST.FunctionCall){
            return evaluate((AST.FunctionCall) expression);
        }
        else if(expression instanceof AST.AddOp){
            return evaluate((AST.AddOp)expression);
        }
        else if(expression instanceof AST.MinusOp){
            return evaluate((AST.MinusOp)expression);
        }
        else if(expression instanceof AST.DivOp){
            return evaluate((AST.DivOp)expression);
        }
        else if(expression instanceof AST.MulOp){
            return evaluate((AST.MulOp)expression);
        }
        else if(expression instanceof AST.ExponOp){
            return evaluate((AST.ExponOp)expression);
        }
        else if(expression instanceof AST.UnaryExpression){
            return evaluate((AST.UnaryExpression) expression);
        }
        else if(expression instanceof AST.Number){
            return evaluate((AST.Number)expression);
        }
        else{
            return expression.toString();
        }
    }

    public  String evaluate(AST.Dims dims){
        return "[" + dims.toString() + "]";
    }
}