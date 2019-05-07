package translators;

import grammar.AST;
import grammar.cfg.*;

import java.util.ArrayList;

public class StanTranslator implements ITranslator{

    public String getDataSection() {
        return dataSection;
    }

    String dataSection = "";
    String paramSection = "";

    public String getModelSection() {
        return modelSection;
    }

    String modelSection = "";

    @Override
    public void translate(ArrayList<Section> sections) throws Exception {
        for(Section section:sections){
            if(section.sectionType == SectionType.DATA){
                for(AST.Data data:section.basicBlocks.get(0).getData()){
                    dataSection += processData(data) +"\n";
                }
            }
            else if(section.sectionType == SectionType.FUNCTION){
                if(section.sectionName.equals("main")){
                    for(BasicBlock basicBlock: section.basicBlocks){
                        SymbolTable symbolTable = basicBlock.getSymbolTable();
                        for(Statement statement:basicBlock.getStatements()){
                            if(statement.statement instanceof AST.AssignmentStatement){
                                AST.AssignmentStatement assignmentStatement = (AST.AssignmentStatement) statement.statement;
                                modelSection += StanVisitor.evaluate(assignmentStatement.lhs) + "=" + StanVisitor.evaluate(assignmentStatement.rhs) +";\n";
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
