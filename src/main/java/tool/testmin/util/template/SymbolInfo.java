package tool.testmin.util.template;

import org.antlr.v4.runtime.ParserRuleContext;
import grammar.Template2Parser;

import java.util.ArrayList;

public class SymbolInfo {
    public ParserRuleContext contextinfo;

    public String id;
    public ArrayList<String> uses;
    public String datatype;
    public boolean isParameter;
    public boolean isTransformedParameter;
    public boolean isData;
    public boolean isTransformedData;
    public boolean usedInModelBlock;
    public boolean usedInTransformedData;
    public boolean isVector;
    public boolean allConstantDims = true;
    public Template2Parser.VectorDIMSContext vectorDIMSContext;
    public Template2Parser.DimsContext dimsContext;
    public ArrayList<Template2Parser.DimsContext> usedDims;
    public ArrayList<Template2Parser.StatementContext> useStatements;
    public ArrayList<Template2Parser.StatementContext> defStatements;

    public SymbolInfo(ParserRuleContext parserRuleContext) {
        if (parserRuleContext instanceof Template2Parser.PriorContext) {
            this.id = getId(((Template2Parser.PriorContext) parserRuleContext).expr());
            this.isParameter = true;
            if (((Template2Parser.PriorContext) parserRuleContext).distexpr().vectorDIMS() != null) {
                this.vectorDIMSContext = ((Template2Parser.PriorContext) parserRuleContext).distexpr().vectorDIMS();
                isVector = true;
            }

            if (((Template2Parser.PriorContext) parserRuleContext).distexpr().dims() != null) {
                this.dimsContext = ((Template2Parser.PriorContext) parserRuleContext).distexpr().dims();
            }

        } else if (parserRuleContext instanceof Template2Parser.DeclContext) {
            this.id = ((Template2Parser.DeclContext) parserRuleContext).ID().getText();
            this.isTransformedParameter = true;

        } else {
            try {
                throw new Exception("Unsupported context");
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(-1);
            }
        }

        usedDims = new ArrayList<>();
    }

    public SymbolInfo(ParserRuleContext parserRuleContext, String context) {
        if (context.equals("data") && parserRuleContext instanceof Template2Parser.DataContext) {
            this.id = ((Template2Parser.DataContext) parserRuleContext).ID().getText();
            this.isData = true;
            if (((Template2Parser.DataContext) parserRuleContext).dtype() != null)
                this.datatype = ((Template2Parser.DataContext) parserRuleContext).dtype().getText();
            else if (parserRuleContext.getText().replaceAll("[^0-9.\\s+]", "").contains("."))
                this.datatype = "float";
            else
                this.datatype = "int";

        } else if (context.equals("transformeddata") && parserRuleContext instanceof Template2Parser.DeclContext) {
            this.id = ((Template2Parser.DeclContext) parserRuleContext).ID().getText();
            this.isTransformedData = true;
            this.datatype = ((Template2Parser.DeclContext) parserRuleContext).dtype().getText();

        } else {
            try {
                throw new Exception("Unsupported context");
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(-1);
            }
        }
        useStatements = new ArrayList<>();
        defStatements = new ArrayList<>();
    }

    private String getId(Template2Parser.ExprContext expressionContext) {
        if (expressionContext instanceof Template2Parser.RefContext)
            return ((Template2Parser.RefContext) expressionContext).ID().getText();
        else
            return ((Template2Parser.Array_accessContext) expressionContext).ID().getText();
    }


}
