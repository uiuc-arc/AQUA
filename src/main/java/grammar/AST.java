package grammar;

import java.util.ArrayList;

public class AST {
    public static class ASTNode{

    }

    public static class Program extends ASTNode{
        public ArrayList<Data> data;
        public ArrayList<Query> queries;
        public ArrayList<Statement> statements;

        public Program(){
            this.data = new ArrayList<>();
            this.queries = new ArrayList<>();
            this.statements = new ArrayList<>();
        }

        public Program(ArrayList<Data> data, ArrayList<Statement> statements,  ArrayList<Query> queries){
            this.data = data;
            this.statements = statements;
            this.queries = queries;
        }

        public void addQuery(Query query){
            this.queries.add(query);
        }

        public void addStatement(Statement statement){
            this.statements.add(statement);
        }

        public void addData(Data data){
            this.data.add(data);
        }
    }


    public enum QueryType
    {   POSTERIOR,
        EXPECTATION }

    public static class Query extends ASTNode{
        public QueryType queryType;
        public String id;

        @Override
        public String toString() {
            return queryType.toString() + "(" +  id + ")";
        }
    }

    public static class Statement extends ASTNode{
        public ArrayList<Annotation> annotations;
    }

    public static class AssignmentStatement extends Statement{
        public Expression lhs;
        public Expression rhs;
        public AssignmentStatement(Expression lhs, Expression rhs){
            this.lhs = lhs;
            this.rhs = rhs;
        }

        @Override
        public String toString() {
            return this.lhs.toString() + " = "+this.rhs.toString();
        }
    }

    public static class ForLoop extends Statement{
        public Id loopVar;
        public Range range;
        public Block block;
        public ForLoop(Id loopVar, Range range, Block block){
            this.loopVar = loopVar;
            this.range = range;
            this.block = block;
        }

        @Override
        public String toString() {
            return loopVar.toString() + " in " + range.toString();
        }
    }

    public static class Range extends ASTNode{
        public Expression start;
        public Expression end;
        public Range(Expression start, Expression end){
            this.start = start;
            this.end = end;
        }

        @Override
        public String toString() {
            return "(" + start.toString() + " : " + end.toString() + ")";
        }
    }

    public static class IfStmt extends Statement{
        public Expression condition;
        public Block trueBlock;
        public Block elseBlock;
        public IfStmt(){

        }

        public IfStmt(Expression condition, Block trueBlock, Block elseBlock){
            this.condition = condition;
            this.trueBlock = trueBlock;
            this.elseBlock = elseBlock;
        }

        @Override
        public String toString() {
            return "if(" + condition.toString() +")";
        }
    }

    public static class TernaryIf extends  Expression{
        public Expression condition;
        public Expression trueExpression;
        public Expression falseExpression;

        public TernaryIf(Expression condition, Expression trueExpression, Expression falseExpression){
            this.condition = condition;
            this.trueExpression = trueExpression;
            this.falseExpression = falseExpression;
        }
    }

    public static class ExponOp extends Expression{
        public Expression base;
        public Expression power;
        public ExponOp(Expression base, Expression power){
            this.base = base;
            this.power = power;
        }

        @Override
        public String toString() {
            return base.toString() + "^" + power.toString();
        }
    }

    public static class AddOp extends Expression{
        public Expression op1;
        public Expression op2;
        public AddOp(Expression base, Expression power){
            this.op1 = base;
            this.op2 = power;
        }

        @Override
        public String toString() {
            return op1.toString() + "+" + op2.toString();
        }
    }

    public static class MinusOp extends Expression{
        public Expression op1;
        public Expression op2;
        public MinusOp(Expression op1, Expression op2){
            this.op1 = op1;
            this.op2 = op2;
        }

        @Override
        public String toString() {
            return op1.toString() + "-" + op2.toString();
        }
    }

    public static class MulOp extends Expression{
        public Expression op1;
        public Expression op2;
        public MulOp(Expression op1, Expression op2){
            this.op1 = op1;
            this.op2 = op2;
        }

        @Override
        public String toString() {
            String op1String = op1.toString();
            String op2String = op2.toString();
            if(!(op1 instanceof Id || op1 instanceof Transpose)){
                op1String = "(" + op1String + ")";
            }
            if(!(op2 instanceof Id || op2 instanceof Transpose)){
                op2String = "(" + op2String + ")";
            }
            return op1String + "*" + op2String;
        }
    }

    public static class DivOp extends Expression{
        public Expression op1;
        public Expression op2;
        public DivOp(Expression op1, Expression op2){
            this.op1 = op1;
            this.op2 = op2;
        }

        @Override
        public String toString() {
            String op1String = op1.toString();
            String op2String = op2.toString();
            if(!(op1 instanceof Id || op1 instanceof Transpose)){
                op1String = "(" + op1String + ")";
            }
            if(!(op2 instanceof Id || op2 instanceof Transpose)){
                op2String = "(" + op2String + ")";
            }
            return op1.toString() + "/" + op2.toString();
        }
    }

    public static class LtOp extends Expression{
        public Expression op1;
        public Expression op2;
        public LtOp(Expression op1, Expression op2){
            this.op1 = op1;
            this.op2 = op2;
        }

        @Override
        public String toString() {
            return op1.toString() + "<" + op2.toString();
        }
    }

    public static class LeqOp extends Expression{
        public Expression op1;
        public Expression op2;
        public LeqOp(Expression op1, Expression op2){
            this.op1 = op1;
            this.op2 = op2;
        }

        @Override
        public String toString() {
            return op1.toString() + "<=" + op2.toString();
        }
    }

    public static class GtOp extends Expression{
        public Expression op1;
        public Expression op2;
        public GtOp(Expression op1, Expression op2){
            this.op1 = op1;
            this.op2 = op2;
        }

        @Override
        public String toString() {
            return op1.toString() + ">" + op2.toString();
        }
    }

    public static class GeqOp extends Expression{
        public Expression op1;
        public Expression op2;
        public GeqOp(Expression op1, Expression op2){
            this.op1 = op1;
            this.op2 = op2;
        }

        @Override
        public String toString() {
            return op1.toString() + ">=" + op2.toString();
        }
    }

    public static class NeOp extends Expression{
        public Expression op1;
        public Expression op2;
        public NeOp(Expression op1, Expression op2){
            this.op1 = op1;
            this.op2 = op2;
        }

        @Override
        public String toString() {
            return op1.toString() + "!=" + op2.toString();
        }
    }

    public static class EqOp extends Expression{
        public Expression op1;
        public Expression op2;
        public EqOp(Expression op1, Expression op2){
            this.op1 = op1;
            this.op2 = op2;
        }

        @Override
        public String toString() {
            return op1.toString() + " == " + op2.toString();
        }
    }

    public static class AndOp extends Expression{
        public Expression op1;
        public Expression op2;
        public AndOp(Expression op1, Expression op2){
            this.op1 = op1;
            this.op2 = op2;
        }

        @Override
        public String toString() {
            return op1.toString() + "&&" + op2.toString();
        }
    }

    public static class OrOp extends Expression{
        public Expression op1;
        public Expression op2;
        public OrOp(Expression op1, Expression op2){
            this.op1 = op1;
            this.op2 = op2;
        }
        @Override
        public String toString() {
            return op1.toString() + "||" + op2.toString();
        }
    }


    public static class Block extends ASTNode{
        public ArrayList<Statement> statements;
        public Block(){
            this.statements = new ArrayList<>();
        }
    }

    public static class Decl extends Statement{
        public Dtype dtype;
        public Dims dims;
        public Id id;

        @Override
        public String toString() {
            String res = dtype.toString() + " " + id.toString();
            if(dims != null && dims.dims.size() > 0){
                res += " " + dims.toString();
            }

            return res;
        }
    }

    public static class FunctionCallStatement extends Statement{
        private final FunctionCall functionCall;
        public FunctionCallStatement(FunctionCall functionCall){
            this.functionCall = functionCall;
        }

        @Override
        public String toString() {
            return functionCall.toString();
        }
    }

    public static class FunctionCall extends Expression{
        public boolean isDist;
        public boolean isDistX;
        public ArrayList<Expression> parameters;
        public Id id;
        public FunctionCall(){
            this.isDist = false;
            this.isDistX = false;
            parameters = new ArrayList<>();
        }

        @Override
        public String toString() {
            String res = id.toString() + "(";
            for(Expression e:parameters){
                res += e.toString() + ",";
            }
            if( res.endsWith(","))
                res = res.substring(0, res.length() -1);
            return res + ")";
        }
    }

    public enum Primitive {INTEGER, FLOAT, MATRIX, VECTOR}

    public static class Expression extends ASTNode{

    }

    public static class ArrayAccess extends Expression{
        public Id id;
        public Dims dims;
        public ArrayAccess(Id id, Dims dims){
            this.id = id;
            this.dims = dims;
        }

        @Override
        public String toString() {
            return id.toString() +"[" + dims.toString() + "]";
        }
    }

    public static class UnaryExpression extends Expression{
        public Expression expression;
        public UnaryExpression(Expression expression){
            this.expression = expression;
        }
    }

    public static class Transpose extends Expression{
        public Expression expression;
        public Transpose(Expression expression){
            this.expression = expression;
        }
    }

    public static class Dims extends ASTNode{
        public ArrayList<Expression> dims = new ArrayList<>();

        @Override
        public String toString() {
            String r = "";
            for(Expression e:dims){
                r+=e.toString() + ",";
            }

            return  r.length() > 0 ? r.substring(0, r.length() -1) : r;
        }
    }

    public static class Dtype extends ASTNode{
        public Primitive primitive;
        public Dims dims;

        @Override
        public String toString() {
            if(dims != null && dims.dims.size() > 0)
                return primitive.toString() + "[" + dims.toString() + "]";
            else
                return primitive.toString();
        }
    }


    public static class Number extends Expression{

    }

    public static class Integer extends Number{
        public int value;
        public Integer(String integerValue){
            this.value = java.lang.Integer.parseInt(integerValue);
        }

        @Override
        public String toString() {
            return java.lang.Integer.toString(value);
        }
    }

    public static class Double extends Number{
        public double value;
        public Double(String doubleValue){
            this.value = java.lang.Double.parseDouble(doubleValue);
        }

        @Override
        public String toString() {
            return java.lang.Double.toString(value);
        }
    }

    public static class Id extends Expression{
        public String id;
        public Id(String id){
            this.id = id;
        }

        @Override
        public String toString() {
            return id;
        }
    }

    public static class StringValue extends Expression{
        public String string;
        public StringValue(String string){
            this.string = string;
        }
    }

    public static class Limits extends ASTNode{
        public Expression lower;
        public Expression upper;

        @Override
        public String toString() {
            if(lower != null && upper != null){
                return String.format("<lower=%s, upper=%s>", lower.toString(), upper.toString());
            }
            else if(lower != null){
                return String.format("<lower=%s>", lower.toString());
            }
            else if(upper != null){
                return String.format("<upper=%s>", upper.toString());
            }
            else{
                return null;
            }
        }
    }

    public enum Marker{Start, End};

    public static class MarkerWrapper extends ASTNode{
        public Marker marker;
        public Id id;
        public MarkerWrapper(Marker marker, Id id){
            this.marker = marker;
            this.id = id;
        }
    }

    public static class Data extends ASTNode{
        public Id id;
        public Decl decl;
        public Dtype datatype;
        public Expression expression;
        public Array array;
        public Vector vector;
        public ArrayList<Annotation> annotations;

        @Override
        public String toString() {
            return id.toString() + (array != null ? ": array" : ": vector");
        }
    }

    public static class ConstHole extends Expression{

    }

    public static class Array extends Data{
        public ArrayList<Expression> expressions;
        public ArrayList<Array> arrays;
        public ArrayList<Vector> vectors;
    }

    public static class Vector extends Data{
        public ArrayList<Expression> expressions;
        public ArrayList<Array> arrays;
        public ArrayList<Vector> vectors;
    }

    public enum AnnotationType {Limits, Blk, Type, Observe, Prior, Dimension }
    public static class Annotation extends ASTNode{
        public AnnotationType annotationType;
        public ASTNode annotationValue;

    }


}
