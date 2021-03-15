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

    public String evaluate(AST.VecDivOp vecDivOp){
        return evaluate(vecDivOp.op1) + "./" + evaluate(vecDivOp.op2);
    }

    public String evaluate(AST.VecMulOp vecMulOp){
        return evaluate(vecMulOp.op1) + "./" + evaluate(vecMulOp.op2);
    }

    public  String evaluate(AST.DivOp divOp){
        return evaluate(divOp.op1) + "/" + evaluate(divOp.op2);
    }

    public  String evaluate(AST.AndOp andOp){
        return evaluate(andOp.op1) + "&&" + evaluate(andOp.op2);
    }

    public  String evaluate(AST.OrOp orOp){
        return evaluate(orOp.op1) + "||" + evaluate(orOp.op2);
    }

    public  String evaluate(AST.ExponOp exponOp){
        return evaluate(exponOp.base) + "^" + evaluate(exponOp.power);
    }

    public  String evaluate(AST.UnaryExpression unaryExpression){
        return "-" + evaluate(unaryExpression.expression);
    }

    public  String evaluate(AST.Braces unaryExpression){
        return "(" + evaluate(unaryExpression.expression) + ")";
    }
    public String evaluate(AST.MulOp mulOp){
        return evaluate(mulOp.op1)+ "*" + evaluate(mulOp.op2);
    }

    public  String evaluate(AST.Number number){
        return number.toString();
    }

    public  String evaluate(AST.FunctionCall functionCall){
        StringBuilder res = new StringBuilder(evaluate(functionCall.id) + "(");
        for (AST.Expression e : functionCall.parameters) {
            if (functionCall.parameters.indexOf(e) == 0 && (functionCall.id.id.endsWith("_lpdf") || functionCall.id.id.endsWith("_lpmf")))
                res.append(evaluate(e)).append("|");
            else
                res.append(evaluate(e)).append(",");
        }
        if (res.toString().endsWith(","))
            res = new StringBuilder(res.substring(0, res.length() - 1));
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
        else if(expression instanceof AST.Braces){
            return evaluate((AST.Braces) expression);
        }
        else if(expression instanceof AST.Number){
            return evaluate((AST.Number)expression);
        }
        else if(expression instanceof AST.VecMulOp){
            return evaluate((AST.VecMulOp)expression);
        }
        else if(expression instanceof AST.VecDivOp){
            return evaluate((AST.VecDivOp)expression);
        }
        else if(expression instanceof AST.AndOp){
            return evaluate((AST.AndOp)expression);
        }
        else if(expression instanceof AST.OrOp){
            return evaluate((AST.OrOp)expression);
        }
        else{
            return expression.toString();
        }
    }

    public  String evaluate(AST.Dims dims){
        return "[" + dims.toString() + "]";
    }
}
