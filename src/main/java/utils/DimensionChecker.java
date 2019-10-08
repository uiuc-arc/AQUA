package utils;

import grammar.AST;
import grammar.Template3BaseListener;
import grammar.Template3BaseVisitor;
import grammar.Template3Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Collectors;

public class DimensionChecker extends Template3BaseListener {
   HashMap<String, Dimension>  dimensionHashMap = new HashMap<>();
   @Override
   public void enterData(Template3Parser.DataContext ctx) {
      Dimension dimension = new Dimension();
      AST.Primitive primitive = ctx.value.datatype.primitive;
      if(primitive == AST.Primitive.FLOAT){
         dimension.type = Types.FLOAT;
      }
      else if(primitive == AST.Primitive.INTEGER){
         dimension.type = Types.INT;
      }
      else if(primitive ==AST.Primitive.MATRIX){
         dimension.type = Types.MATRIX;
      }
      else if(primitive == AST.Primitive.VECTOR){
         dimension.type = Types.VECTOR;
      }
      else{
         assert false;
      }
      if(ctx.value.datatype.dims != null && ctx.value.datatype.dims.dims.size() > 0){
         dimension.dims = new ArrayList(ctx.value.datatype.dims.dims.stream().map(x -> x.toString()).collect(Collectors.toList()));
      }

      this.dimensionHashMap.put(ctx.value.decl.id.toString(), dimension);

   }

   public static Dimension getDimension(Template3Parser.ExprContext exprContext){
      DimensionChecker dimensionChecker = new DimensionChecker();
      ParseTreeWalker walker = new ParseTreeWalker();
      ParserRuleContext rootContext = exprContext;
      while(!(rootContext instanceof Template3Parser.TemplateContext)){
         rootContext = rootContext.getParent();
      }
      // populates the dimension map
      walker.walk(dimensionChecker, rootContext);

   }
}
