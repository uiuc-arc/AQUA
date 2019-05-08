package translators;

import grammar.AST;
import grammar.cfg.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class StanTranslator implements ITranslator{

    public String getDataSection() {
        return dataSection;
    }

    public String getModelSection() {
        return modelSection;
    }

    String dataSection = "";
    String paramSection = "";
    String transformedData = "";
    String transformedParam = "";
    String modelSection = "";
    String generatedQuantities = "";

    @Override
    public void translate(ArrayList<Section> sections) throws Exception {
        for(Section section:sections){
            if(section.sectionType == SectionType.DATA){
                for(AST.Data data:section.basicBlocks.get(0).getData()){
                    dataSection += getDeclarationString(section.basicBlocks.get(0), data.decl);
                    //dataSection += processData(data) +"\n";
                }
            }
            else if(section.sectionType == SectionType.FUNCTION){
                if(section.sectionName.equals("main")){
                    Set<BasicBlock> visited = new HashSet<>();
                    for(BasicBlock basicBlock: section.basicBlocks){
                        BasicBlock curBlock = basicBlock;
                        while(!visited.contains(curBlock)){
                            visited.add(curBlock);
                            String block_text = translate_block(curBlock);
                            if(curBlock.getParent().sectionName.equalsIgnoreCase("main")){
                                modelSection += block_text;
                            }
                            else if(curBlock.getParent().sectionName.equalsIgnoreCase("transformedparam")){
                                transformedParam += block_text;
                            }

                            if(curBlock.getEdges().size() > 0){
                                // check true edge first
                                if(curBlock.getEdges().size() == 1){
                                    curBlock = curBlock.getEdges().get(0).getTarget();
                                }
                                else{
                                    String label = curBlock.getEdges().get(0).getLabel();
                                    if(label != null && label.equalsIgnoreCase("true")){
                                        curBlock = curBlock.getEdges().get(0).getTarget();
                                    }
                                    else{
                                        curBlock = curBlock.getEdges().get(1).getTarget();
                                    }
                                }
                            }
                        }
                    }
                }
                else{
                    throw new Exception("Unknown Function!");
                }
            }
        }
    }

    public String print(){
        String stanCode = "";

        if(dataSection != null && dataSection.length() > 0){
            stanCode += "data{\n" + dataSection + "}\n";
        }

        if(paramSection != null && paramSection.length() > 0){
            stanCode += "parameters{\n" + paramSection + "}\n";
        }

        if(transformedData != null && transformedData.length() > 0){
            stanCode += "transformed data{\n" + transformedData + "}\n";
        }

        if(transformedParam != null && transformedParam.length() > 0){
            stanCode += "transformed param{\n" + transformedParam + "}\n";
        }

        if(modelSection != null && modelSection.length() > 0){
            stanCode += "model{\n" + modelSection + "}\n";
        }

        if(generatedQuantities != null && generatedQuantities.length() > 0){
            stanCode += "generated quantities{\n" + generatedQuantities + "}\n";
        }

        return stanCode;
    }

    private String translate_block(BasicBlock bBlock){
        SymbolTable symbolTable = bBlock.getSymbolTable();
        String output = "";
        if(bBlock.getStatements().size() == 0)
            return output;

        for(Statement statement:bBlock.getStatements()){
            if(statement.statement instanceof AST.AssignmentStatement){
                AST.AssignmentStatement assignmentStatement = (AST.AssignmentStatement) statement.statement;
                if(isPrior(statement, assignmentStatement.lhs) || isData(statement, assignmentStatement.lhs)){
                    output += StanVisitor.evaluate(assignmentStatement.lhs) + "~" + StanVisitor.evaluate(assignmentStatement.rhs) +";\n";
                }
                else {
                    output += StanVisitor.evaluate(assignmentStatement.lhs) + "=" + StanVisitor.evaluate(assignmentStatement.rhs) + ";\n";
                }
            }
            else if(statement.statement instanceof AST.ForLoop){
                AST.ForLoop loop = (AST.ForLoop) statement.statement;
                output += "for(" + loop.toString() + ")\n";
            }
            else if(statement.statement instanceof AST.Decl){
                AST.Decl declaration = (AST.Decl) statement.statement;
                String declarationString = getDeclarationString(statement, declaration);

                if(statement.parent.getParent().sectionName.equalsIgnoreCase("main") && isPrior(statement, declaration.id)){
                    this.paramSection+= declarationString;
                }
                else{
                    output += declarationString;
                }
            }
        }

        if(bBlock.getIncomingEdges().containsKey("true") || bBlock.getIncomingEdges().containsKey("false")){
            return "{\n" + output + "}\n";
        }


        return output ;
    }

    private String getDeclarationString(Statement statement, AST.Decl declaration) {
        return getDeclarationString(statement.parent, declaration);
    }

    private String getDeclarationString(BasicBlock basicBlock, AST.Decl declaration) {
        String declarationStringFormat = "%s%s%s %s%s;\n";

        String declarationString = String.format(
                declarationStringFormat,
                declaration.dtype.primitive.toString(),
                getLimitsString(basicBlock, declaration.id),
                declaration.dtype.dims != null ? StanVisitor.evaluate(declaration.dtype.dims) : "",
                declaration.id.toString(),
                declaration.dims != null ? StanVisitor.evaluate(declaration.dims) : "");

        declarationString = declarationString.replace("FLOAT", "real").
                replace("INTEGER", "int").
                replace("VECTOR", "vector");
        return declarationString;
    }

    private boolean isPrior(Statement statement, AST.Expression expression){
        // get id
        String id = null;
        if(expression instanceof AST.Id){
            id = expression.toString();
        }
        else if(expression instanceof AST.ArrayAccess){
            id = ((AST.ArrayAccess) expression) .id.toString();
        }

        if(id != null){
            SymbolInfo info = statement.parent.getSymbolTable().fetch(id);
            if(info != null)
                return info.isPriorVariable();
        }

        return false;
    }

    private boolean isData(Statement statement, AST.Expression expression){
        // get id
        String id = null;
        if(expression instanceof AST.Id){
            id = expression.toString();
        }
        else if(expression instanceof AST.ArrayAccess){
            id = ((AST.ArrayAccess) expression).id.toString();
        }

        if(id != null){
            SymbolInfo info = statement.parent.getSymbolTable().fetch(id);
            if(info != null)
                return info.isData();
            else{
                System.out.println("Variable not found " +id);
            }
        }

        return false;
    }

    private String getLimitsString(BasicBlock basicBlock, AST.Expression expression){
        String id = null;
        if(expression instanceof AST.Id){
            id = expression.toString();
        }
        else if(expression instanceof AST.ArrayAccess){
            id = ((AST.ArrayAccess) expression) .id.toString();
        }
        if(id != null){
            SymbolInfo info = basicBlock.getSymbolTable().fetch(id);
            if(info != null)
                return info.getLimitsString() != null? info.getLimitsString() : "";
            else{
                System.out.println("Symbol not found " + id);
            }
        }

        return "";
    }



    private String processData(AST.Data data) throws IllegalAccessException {
        String typeString = "";
        String dimsString = "";
        String outerDimsString = "";

        if(data.decl.dtype.primitive == AST.Primitive.INTEGER){
            typeString += "int";
        }
        else if(data.decl.dtype.primitive == AST.Primitive.FLOAT){
            typeString += "real";
        }
        else if(data.decl.dtype.primitive == AST.Primitive.VECTOR){
            if(data.annotations.get(0).annotationType == AST.AnnotationType.Type){
                typeString += data.annotations.get(0).annotationValue.toString();
            }
            else{
                typeString += "vector";
            }
        }
        else if(data.decl.dtype.primitive == AST.Primitive.MATRIX){
            if(data.annotations.get(0).annotationType == AST.AnnotationType.Type){
                typeString += data.annotations.get(0).annotationValue.toString();
            }
            else{
                typeString += "matrix";
            }
        }
        else{
            throw new IllegalAccessException("Unknown data type : " + data.decl.dtype.primitive.toString());
        }

        if(data.decl.dtype.dims != null
                && data.decl.dtype.dims.dims != null
                && data.decl.dtype.dims.dims.size() > 0){
            dimsString = "[" +data.decl.dtype.dims.toString() +"]";
        }

        if(data.decl.dims != null){
            outerDimsString = "[" + data.decl.dims.toString() + "]";
        }

        return typeString + " " + dimsString + data.decl.id.toString() + " " + outerDimsString + ";";
    }

    @Override
    public void run() {

    }
}
