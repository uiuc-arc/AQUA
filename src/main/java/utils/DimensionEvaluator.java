package utils;

import grammar.AST;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class DimensionEvaluator {
    private static ArrayList<String> realfunctions = new ArrayList<>(Arrays.asList("log_sum_exp", "normal_cdf_log", "normal_lpdf", "mean", "sd", "gamma_p", "normal_rng"));
    private  static ArrayList<String> intfunctions = new ArrayList<>(Arrays.asList("binomial_rng", "bernoulli_logit_rng"));

    private HashMap<String, Dimension> dimensionHashMap;
    public DimensionEvaluator(HashMap<String, Dimension> dimensionMap){
        this.dimensionHashMap = dimensionMap;
    }

    public Dimension visit(AST.Expression expr) {
        if(expr instanceof AST.AddOp)
            return visit((AST.AddOp)expr);
        else if(expr instanceof AST.MinusOp)
            return visit((AST.MinusOp)expr);
        else if(expr instanceof AST.VecDivOp)
            return visit((AST.VecDivOp) expr);
        else if(expr instanceof AST.VecMulOp)
            return visit((AST.VecMulOp) expr);
        else if(expr instanceof AST.ArrayAccess)
            return visit((AST.ArrayAccess) expr);
        else if(expr instanceof AST.Transpose)
            return visit((AST.Transpose) expr);
        else if(expr instanceof AST.ExponOp)
            return visit((AST.ExponOp) expr);
        else if(expr instanceof AST.DivOp)
            return visit((AST.DivOp) expr);
        else if(expr instanceof AST.MulOp)
            return visit((AST.MulOp) expr);
        else if(expr instanceof AST.FunctionCall)
            return visit((AST.FunctionCall) expr);
        else if(expr instanceof AST.UnaryExpression)
            return visit((AST.UnaryExpression) expr);
        else if(expr instanceof AST.Braces)
            return visit(((AST.Braces) expr).expression);
        else if(expr instanceof AST.Id)
            return visit((AST.Id) expr);
        else if(expr instanceof AST.StringValue)
            return visit((AST.StringValue) expr);
        else if(expr instanceof AST.Number)
            return visit((AST.Number) expr);
        else
            assert false;
        return null;
    }

    private Dimension visit(AST.Number expr) {
        return new Dimension(expr instanceof AST.Integer ? Types.INT : Types.FLOAT, null);
    }

    private Dimension visit(AST.StringValue expr) {
        return null;
    }

    private Dimension visit(AST.Id expr) {
        return this.dimensionHashMap.get(expr.toString());
    }

    private Dimension visit(AST.UnaryExpression expr) {
        return visit(expr.expression);
    }

    private Dimension visit(AST.Braces expr) {
        return visit(expr.expression);
    }
    private Dimension visit(AST.FunctionCall expr) {
        String functionName = expr.id.toString();
        if(realfunctions.contains(functionName)){
            Dimension dimension = new Dimension(Types.FLOAT, new ArrayList<>());
            return dimension;
        }
        else if(intfunctions.contains(functionName)){
            Dimension dimension = new Dimension(Types.INT, new ArrayList<>());
            return dimension;
        }
        else if(functionName.endsWith("_lpmf") || functionName.endsWith("_lpdf")){
            Dimension dimension = new Dimension(Types.FLOAT, new ArrayList<>());
            return dimension;
        }
        else if(functionName.equals("diag_pre_multiply")){
            Dimension dim1 = visit(expr.parameters.get(0));
            Dimension dim2 = visit(expr.parameters.get(1));


            Types type = Types.MATRIX;
            ArrayList<String> dims = new ArrayList<>();
            if(dim1 != null && dim2 != null) {
                if (dim1.getDims().size() > 0)
                    dims.add(dim1.getDims().get(0));
                if (dim2.getDims().size() > 1)
                    dims.add(dim2.getDims().get(1));
            }
            return new Dimension(type, dims);
        }
        else if(functionName.equals("diag_matrix")){
            Dimension innerdim = visit(expr.parameters.get(0));

            if(innerdim == null)
                return null;

            ArrayList<String> dims = new ArrayList<>();
            dims.add(innerdim.getDims().get(0));
            dims.add(innerdim.getDims().get(0));
            Dimension newdim = new Dimension(Types.MATRIX, dims);
            return newdim;
        }
        else if(functionName.equals("rep_vector")){
            Dimension dim = new Dimension(
                    Types.VECTOR,
                    new ArrayList<>(Arrays.asList(new String[]{expr.parameters.get(1).toString()})));
            return dim;
        }
        else if(functionName.equals("rep_matrix")){
            Dimension dim = new Dimension(
                    Types.MATRIX,
                    new ArrayList<>(Arrays.asList(new String[]{expr.parameters.get(1).toString(),
                            expr.parameters.get(2).toString()})));
            return dim;
        }
        else if(functionName.equals("rep_row_vector")){
            Dimension dim = new Dimension(
                    Types.ROW_VECTOR,
                    new ArrayList<>(Arrays.asList(new String[]{expr.parameters.get(1).toString()})));
            return dim;
        }
        else if(functionName.equals("rep_array")){
            Types type;
            if(expr.parameters.get(0).toString().contains(".")){
                //real
                type = Types.FLOAT;
            }
            else{
                type = Types.INT;
            }
            ArrayList<String> dims = new ArrayList<>();
            for(int i=1; i < expr.parameters.size(); i++)
            {
                dims.add(expr.parameters.get(i).toString());
            }
            Dimension dim = new Dimension(type, dims);

            return dim;
        }
        else if(functionName.equals("max") || functionName.equals("sum")){
            Dimension dim = visit( expr.parameters.get(0));
            Types type = null;
            ArrayList<String> dims = null;
            if(dim!=null){
                if(dim.getType() == Types.INT){
                    type = Types.INT;
                    dims = new ArrayList<>();
                }
                else{
                    type =Types.FLOAT;
                    dims = new ArrayList<>();
                }
            }
            return new Dimension(type, dims);
        }
        else if(functionName.equals("categorical_rng")){
            Dimension dim = new Dimension(Types.INT, new ArrayList<>());
            return dim;
        }
        else if(functionName.equals("negative_infinity")){
            Dimension dim = new Dimension(Types.FLOAT, new ArrayList<>());
            return dim;
        }
        else if(functionName.equals("to_vector")){
            Dimension dim1 = visit(expr.parameters.get(0));
            String newdim ="";
            for(String d:dim1.getDims()){
                newdim = newdim +"*"+ "(" + d +")";
            }
            ArrayList<String> dims = new ArrayList<>();
            if(newdim.startsWith("*"))
                dims.add(newdim.substring(1));
            else
                dims.add(newdim);
            Types type = Types.VECTOR;
            return new Dimension(type, dims);
        }
        else {
            if(expr.parameters != null && expr.parameters.size() > 0){
                // return the dimension of one of the arguments of the function
                return visit(expr.parameters.get(0));
            }
        }

        return null;
    }

    private Dimension visit(AST.MulOp expr) {
        Dimension dim1 = visit(expr.op1);
        Dimension dim2 = visit(expr.op2);

        if(dim1 == null){
            return null;
        }

        if(dim2 == null){
            return null;
        }

        if(dim1.isPrimitive() && dim2.isPrimitive()){
            if(dim1.getType() == Types.FLOAT)
                return dim1;
            else
                return dim2;
        }
        else if(dim1.isPrimitive()){
            // scalar * vector
            return dim2;
        }
        else if(dim2.isPrimitive()){
            return dim1;
        }
        else if(dim1.getType() == Types.ROW_VECTOR && dim2.getType() == Types.VECTOR){
            Dimension dim = new Dimension(Types.FLOAT, new ArrayList<>());
            return dim;
        }
        else if(dim1.getType() == Types.VECTOR && dim2.getType() == Types.ROW_VECTOR){
            Dimension dim = new Dimension(Types.FLOAT, new ArrayList<>());
            return dim;
        }
        else if(dim1.getType() == Types.MATRIX && dim2.getType() == Types.VECTOR){
            Dimension dim = new Dimension(Types.VECTOR, new ArrayList<>(Arrays.asList(new String[]{dim1.getDims().get(0)})));
            return dim;
        }
        else if(dim1.getType() == Types.VECTOR && dim2.getType() == Types.VECTOR){
            // .* multiplication
            return  dim1;
        }
        else {
            String newdim1 = dim1.getDims().get(0);
            String newdim2 ;
            //TODO: check row_vector later
//            if(dim2.type == "row_vector") {
//                newdim2 = dim2.dims.get(0);
//            }g
//            else{
            newdim2 = dim2.getDims().get(1);
            //}

            Dimension newdim = new Dimension(dim1.getType(),
                    new ArrayList<>(Arrays.asList(new String[]{newdim1, newdim2})));
//           if(newdim1.equals("1") && newdim2.equals("1")){
//               newdim = new Dimension("real", new ArrayList<>());
//           }
//           else if(newdim1.equals("1")){
//               newdim = new Dimension("real", new ArrayList<>(Arrays.asList(new String[]{newdim2})));
//           }
//           else if(newdim2.equals("1")){
//               newdim = new Dimension("real", new ArrayList<>(Arrays.asList(new String[]{newdim2})));
//           }
            return newdim;
        }
    }

    private Dimension visit(AST.DivOp expr) {
        Dimension dim = visit(expr.op1);
        return dim;
    }

    private Dimension visit(AST.ExponOp expr) {
        return visit(expr.base);
    }


    private Dimension visit(AST.Transpose expr) {
        Dimension dim =visit(expr.expression);
        Types type = null;
        if(dim.getType() == Types.VECTOR){
            type = Types.ROW_VECTOR;
        }
        else if(dim.getType() == Types.ROW_VECTOR){
            type = Types.VECTOR;
        }
        else if(dim.getType() == Types.MATRIX){
            type = Types.MATRIX;
        }
        else{
            System.out.println(dim.getType());
            assert false;
        }

        Dimension dimension = new Dimension(type, new ArrayList<>(dim.getDims()));
        return dimension;
    }

    private Dimension visit(AST.ArrayAccess expr) {
        Dimension a = visit(expr.id);
        Types type = null;
        ArrayList<String> dims = null;
        if(a == null)
            return null;
        assert a.getDims() != null;

        if(a.getType() == Types.VECTOR){
            type = Types.FLOAT;
        }
        else if(a.getType() == Types.MATRIX){
            int dimsUsed = expr.dims.dims.size();

            if(a.getDims().size()  - dimsUsed == 1)
                type = Types.VECTOR;
            else if (a.getDims().size()  - dimsUsed == 0)
                type = Types.FLOAT;
            else
                type = Types.MATRIX;
            dims = new ArrayList<>(a.getDims());
            for(int i=0; i < dimsUsed; i++) {
                dims.remove(0);
            }
            //TODO: check if row_vector is needed
        }
        else{
            type = a.getType();
            dims = new ArrayList<>(a.getDims());
            dims.remove(0);
        }

        Dimension dimension = new Dimension(type, dims);
        return dimension;
    }

    private Dimension visit(AST.MinusOp expr) {
        Dimension a = visit(expr.op1);
        Dimension b = visit(expr.op2);
        if(a.isPrimitive()){
            return b;
        }
        else{
            return a;
        }
    }

    private Dimension visit(AST.VecDivOp expr){
        Dimension a = visit(expr.op1);
        return a;
    }

    private Dimension visit(AST.VecMulOp expr){
        Dimension a = visit(expr.op1);
        return a;
    }

    public Dimension visit(AST.AddOp expr){
        Dimension a = visit(expr.op1);
        Dimension b = visit(expr.op2);
        if(a.isPrimitive()){
            return b;
        }
        else{
            return a;
        }
    }

}
