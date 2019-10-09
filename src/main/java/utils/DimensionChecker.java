package utils;

import grammar.AST;
import grammar.Template3BaseListener;
import grammar.Template3BaseVisitor;
import grammar.Template3Parser;
import grammar.cfg.Section;
import grammar.cfg.Statement;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import translators.listeners.CFGWalker;
import translators.listeners.StatementListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Collectors;

public class DimensionChecker implements StatementListener {
   HashMap<String, Dimension>  dimensionHashMap = new HashMap<>();

//   @Override
//   public void enterDecl(Template3Parser.DeclContext ctx) {
//      Types type = null;
//      AST.Primitive primitive = ctx.value.dtype.primitive;
//      if(primitive == AST.Primitive.FLOAT){
//         type = Types.FLOAT;
//      }
//      else if(primitive == AST.Primitive.INTEGER){
//         type = Types.INT;
//      }
//      else if(primitive ==AST.Primitive.MATRIX){
//         type = Types.MATRIX;
//      }
//      else if(primitive == AST.Primitive.VECTOR){
//         type = Types.VECTOR;
//      }
//      else{
//         assert false;
//      }
//      ArrayList<String> dims = null;
//      if(ctx.value.dtype.dims != null && ctx.value.dtype.dims.dims.size() > 0){
//         dims = new ArrayList(ctx.value.dtype.dims.dims.stream().map(x -> x.toString()).collect(Collectors.toList()));
//      }
//      Dimension dimension = new Dimension(type, dims);
//
//      this.dimensionHashMap.put(ctx.value.id.toString(), dimension);
//   }
//
//   @Override
//   public void enterData(Template3Parser.DataContext ctx) {
//      Types type = null;
//      AST.Primitive primitive = ctx.value.decl.dtype.primitive;
//      if(primitive == AST.Primitive.FLOAT){
//         type = Types.FLOAT;
//      }
//      else if(primitive == AST.Primitive.INTEGER){
//         type = Types.INT;
//      }
//      else if(primitive ==AST.Primitive.MATRIX){
//         type = Types.MATRIX;
//      }
//      else if(primitive == AST.Primitive.VECTOR){
//         type = Types.VECTOR;
//      }
//      else{
//         assert false;
//      }
//      ArrayList<String> dims = null;
//      if(ctx.value.datatype.dims != null && ctx.value.datatype.dims.dims.size() > 0){
//         dims = new ArrayList(ctx.value.datatype.dims.dims.stream().map(x -> x.toString()).collect(Collectors.toList()));
//      }
//      Dimension dimension = new Dimension(type, dims);
//
//      this.dimensionHashMap.put(ctx.value.decl.id.toString(), dimension);
//
//   }

   public static Dimension getDimension(AST.Expression expression, ArrayList<Section> sections){
      DimensionChecker dimensionChecker = new DimensionChecker();


      CFGWalker walker = new CFGWalker(sections, dimensionChecker);
      // populates the dimension map
      walker.walk();
      DimensionEvaluator dimensionEvaluator = new DimensionEvaluator(dimensionChecker.dimensionHashMap);
      Dimension dim = dimensionEvaluator.visit(expression);
      return dim;
   }

   @Override
   public void enterAssignmentStatement(Statement statement) {

   }

   @Override
   public void enterForLoopStatement(Statement statement) {

   }

   @Override
   public void enterIfStmt(Statement statement) {

   }

   @Override
   public void enterDeclStatement(Statement statement) {
      Types type = null;
      AST.Decl declstatement = (AST.Decl) statement.statement;
      AST.Primitive primitive = declstatement.dtype.primitive;
      if(primitive == AST.Primitive.FLOAT){
         type = Types.FLOAT;
      }
      else if(primitive == AST.Primitive.INTEGER){
         type = Types.INT;
      }
      else if(primitive ==AST.Primitive.MATRIX){
         type = Types.MATRIX;
      }
      else if(primitive == AST.Primitive.VECTOR){
         type = Types.VECTOR;
      }
      else{
         assert false;
      }
      ArrayList<String> dims = new ArrayList<>();
      if(declstatement.dims != null && declstatement.dims.dims.size() > 0){
         dims.addAll(new ArrayList(declstatement.dims.dims.stream().map(x -> x.toString()).collect(Collectors.toList())));
      }

      if(declstatement.dtype.dims != null && declstatement.dtype.dims.dims.size() > 0){
         dims.addAll(new ArrayList(declstatement.dtype.dims.dims.stream().map(x -> x.toString()).collect(Collectors.toList())));
      }

      Dimension dimension = new Dimension(type, dims);

      this.dimensionHashMap.put(declstatement.id.toString(), dimension);
   }

   @Override
   public void enterFunctionCallStatement(Statement statement) {

   }

   @Override
   public void enterData(AST.Data data) {
      Types type = null;
      AST.Decl declstatement = data.decl;
      AST.Primitive primitive = declstatement.dtype.primitive;
      if(primitive == AST.Primitive.FLOAT){
         type = Types.FLOAT;
      }
      else if(primitive == AST.Primitive.INTEGER){
         type = Types.INT;
      }
      else if(primitive ==AST.Primitive.MATRIX){
         type = Types.MATRIX;
      }
      else if(primitive == AST.Primitive.VECTOR){
         type = Types.VECTOR;
      }
      else{
         assert false;
      }
      ArrayList<String> dims = new ArrayList<>();
      if(declstatement.dims != null && declstatement.dims.dims.size() > 0){
         dims.addAll(new ArrayList(declstatement.dims.dims.stream().map(x -> x.toString()).collect(Collectors.toList())));
      }

      if(declstatement.dtype.dims != null && declstatement.dtype.dims.dims.size() > 0){
         dims.addAll(new ArrayList(declstatement.dtype.dims.dims.stream().map(x -> x.toString()).collect(Collectors.toList())));
      }
      Dimension dimension = new Dimension(type, dims);

      this.dimensionHashMap.put(declstatement.id.toString(), dimension);
   }
}
