package translators;

import grammar.AST;
import grammar.cfg.*;
import org.apache.commons.lang3.tuple.Pair;
import translators.visitors.StanVisitor;
import utils.Utils;

import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class StanTranslator implements ITranslator {

    private String dataSection = "";
    private String paramSection = "";
    private String transformedData = "";
    private String transformedParam = "";
    private String modelSection = "";
    private String generatedQuantities = "";
    private String dataR = "";

    @Override
    public void translate(ArrayList<Section> sections) throws Exception {

        Set<BasicBlock> visited = new HashSet<>();
        for (Section section : sections) {
            if (section.sectionType == SectionType.DATA) {
                for (AST.Data data : section.basicBlocks.get(0).getData()) {
                    dataSection += getDeclarationString(section.basicBlocks.get(0), data.decl);
                }

                dataR = dumpR(section.basicBlocks.get(0).getData());
            } else if (section.sectionType == SectionType.FUNCTION) {
                if (section.sectionName.equals("main")) {
                    for (BasicBlock basicBlock : section.basicBlocks) {
                        BasicBlock curBlock = basicBlock;
                        while (!visited.contains(curBlock)) {
                            visited.add(curBlock);
                            String block_text = translate_block(curBlock);

                            if(curBlock.getIncomingEdges().containsKey("true")){
                                block_text = "{\n" +block_text;
                            }

                            if(curBlock.getOutgoingEdges().containsKey("back")){
                                block_text = block_text +"}\n";
                            }


                            if (curBlock.getParent().sectionName.equalsIgnoreCase("main")) {
                                modelSection += block_text;
                            } else if (curBlock.getParent().sectionName.equalsIgnoreCase("transformedparam")) {
                                transformedParam += block_text;
                            } else if (curBlock.getParent().sectionName.equalsIgnoreCase("transformeddata")) {
                                transformedData += block_text;
                            } else if (curBlock.getParent().sectionName.equalsIgnoreCase("generatedquantities")) {
                                generatedQuantities += block_text;
                            }

                            if (curBlock.getEdges().size() > 0) {
                                BasicBlock prevBlock = curBlock;
                                // check true edge first

                                if (curBlock.getEdges().size() == 1) {
                                    curBlock = curBlock.getEdges().get(0).getTarget();
                                } else {
                                    String label = curBlock.getEdges().get(0).getLabel();
                                    if (label != null && label.equalsIgnoreCase("true") && !visited.contains(curBlock.getEdges().get(0).getTarget())) {
                                        curBlock = curBlock.getEdges().get(0).getTarget();
                                    } else {
                                        curBlock = curBlock.getEdges().get(1).getTarget();
                                    }
                                }
                                //if next is meet block
                                if(!visited.contains(curBlock) && curBlock.getIncomingEdges().containsKey("meet") && !isIfNode(prevBlock)){
                                    if (curBlock.getParent().sectionName.equalsIgnoreCase("main")) {
                                        modelSection += "}\n";
                                    } else if (curBlock.getParent().sectionName.equalsIgnoreCase("transformedparam")) {
                                        transformedParam += "}\n";
                                    } else if (curBlock.getParent().sectionName.equalsIgnoreCase("transformeddata")) {
                                        transformedData += "}\n";
                                    } else if (curBlock.getParent().sectionName.equalsIgnoreCase("generatedquantities")) {
                                        generatedQuantities += "}\n";
                                    }

                                }
                            }
                        }
                    }
                } else {
                    throw new Exception("Unknown Function!");
                }
            } else if (section.sectionType == SectionType.NAMEDSECTION) {
                for (BasicBlock basicBlock : section.basicBlocks) {
                    BasicBlock curBlock = basicBlock;
                    while (!visited.contains(curBlock)) {
                        visited.add(curBlock);
                        String block_text = translate_block(curBlock);
                        if(curBlock.getIncomingEdges().containsKey("true")){
                            block_text = "{\n" +block_text;
                        }
                        if(curBlock.getOutgoingEdges().containsKey("back")){
                            block_text = block_text +"}\n";
                        }

                        if (curBlock.getParent().sectionName.equalsIgnoreCase("main")) {
                            modelSection += block_text;
                        }
                        else if (curBlock.getParent().sectionName.equalsIgnoreCase("transformedparam")) {
                            transformedParam += block_text;
                        } else if (curBlock.getParent().sectionName.equalsIgnoreCase("transformeddata")) {
                            transformedData += block_text;
                        } else if (curBlock.getParent().sectionName.equalsIgnoreCase("generatedquantities")) {
                            generatedQuantities += block_text;
                        }

                        if (curBlock.getEdges().size() > 0) {
                            // check true edge first
                            BasicBlock prevBlock = curBlock;
                            if (curBlock.getEdges().size() == 1) {
                                curBlock = curBlock.getEdges().get(0).getTarget();
                            } else {
                                String label = curBlock.getEdges().get(0).getLabel();
                                if (label != null && label.equalsIgnoreCase("true")  && !visited.contains(curBlock.getEdges().get(0).getTarget())) {
                                    curBlock = curBlock.getEdges().get(0).getTarget();
                                } else {
                                    curBlock = curBlock.getEdges().get(1).getTarget();
                                }
                            }

                            if(!visited.contains(curBlock) && curBlock.getIncomingEdges().containsKey("meet") && !isIfNode(prevBlock)){
                                if (curBlock.getParent().sectionName.equalsIgnoreCase("main")) {
                                    modelSection += "}\n";
                                } else if (curBlock.getParent().sectionName.equalsIgnoreCase("transformedparam")) {
                                    transformedParam += "}\n";
                                } else if (curBlock.getParent().sectionName.equalsIgnoreCase("transformeddata")) {
                                    transformedData += "}\n";
                                } else if (curBlock.getParent().sectionName.equalsIgnoreCase("generatedquantities")) {
                                    generatedQuantities += "}\n";
                                }

                            }
                        }
                    }
                }
            }
        }
    }

    private boolean isIfNode(BasicBlock basicBlock){
        return basicBlock.getStatements().size() == 1 && basicBlock.getStatements().get(0).statement instanceof AST.IfStmt;
    }

    public String getData() {
        return this.dataR;
    }

    public String getCode() {
        String stanCode = "";

        if (dataSection != null && dataSection.length() > 0) {
            stanCode += "data{\n" + dataSection + "}\n";
        }

        if (transformedData != null && transformedData.length() > 0) {
            stanCode += "transformed data{\n" + transformedData + "}\n";
        }

        if (paramSection != null && paramSection.length() > 0) {
            stanCode += "parameters{\n" + paramSection + "}\n";
        }

        if (transformedParam != null && transformedParam.length() > 0) {
            stanCode += "transformed parameters{\n" + transformedParam + "}\n";
        }

        if (modelSection != null && modelSection.length() > 0) {
            stanCode += "model{\n" + modelSection + "}\n";
        }

        if (generatedQuantities != null && generatedQuantities.length() > 0) {
            stanCode += "generated quantities{\n" + generatedQuantities + "}\n";
        }

        return stanCode;
    }

    private String translate_block(BasicBlock bBlock) {
        StringBuilder output = new StringBuilder();
        if (bBlock.getStatements().size() == 0)
            return output.toString();

        for (Statement statement : bBlock.getStatements()) {
            if (statement.statement instanceof AST.AssignmentStatement) {
                AST.AssignmentStatement assignmentStatement = (AST.AssignmentStatement) statement.statement;
                if (Utils.isPrior(statement, assignmentStatement.lhs) || isData(statement, assignmentStatement.lhs)) {
                    output.append(new StanVisitor().evaluate(assignmentStatement.lhs)).append("~").append(new StanVisitor().evaluate(assignmentStatement.rhs)).append(";\n");
                } else {
                    output.append(new StanVisitor().evaluate(assignmentStatement.lhs)).append("=").append(new StanVisitor().evaluate(assignmentStatement.rhs)).append(";\n");
                }
            } else if (statement.statement instanceof AST.ForLoop) {
                AST.ForLoop loop = (AST.ForLoop) statement.statement;
                output.append("for(").append(loop.toString()).append(")\n");
            } else if (statement.statement instanceof AST.Decl) {
                AST.Decl declaration = (AST.Decl) statement.statement;
                String declarationString = getDeclarationString(statement, declaration);

                if (statement.parent.getParent().sectionName.equalsIgnoreCase("main") && Utils.isPrior(statement, declaration.id)) {
                    this.paramSection += declarationString;
                } else {
                    output.append(declarationString);
                }
            }
            else if(statement.statement instanceof AST.IfStmt){
                AST.IfStmt ifStmt = (AST.IfStmt) statement.statement;
                output.append("if(").append(ifStmt.condition.toString()).append(")\n");
            } else if (statement.statement instanceof AST.FunctionCallStatement) {
                AST.FunctionCallStatement functionCallStatement = (AST.FunctionCallStatement) statement.statement;
                output.append(new StanVisitor().evaluate(functionCallStatement.functionCall));
                // if (functionCallStatement.functionCall.id.id.endsWith("lpdf") || functionCallStatement.functionCall.id.id.endsWith("lpmf")) {
                //     StringBuilder restparams = new StringBuilder();
                //     for (AST.Expression pp : functionCallStatement.functionCall.parameters) {
                //         if (functionCallStatement.functionCall.parameters.indexOf(pp) != 0) {
                //             if (functionCallStatement.functionCall.parameters.indexOf(pp)
                //                     == functionCallStatement.functionCall.parameters.size() - 1)
                //                 restparams.append(pp.toString());
                //             else
                //                 restparams.append(pp.toString()).append(",");
                //         }
                //     }
                //     output.append(String.format("%s(%s|%s)\n",
                //             functionCallStatement.functionCall.id.id,
                //             functionCallStatement.functionCall.parameters.get(0),
                //             restparams));
                // } else {
                //     output.append(functionCallStatement.toString());
                // }

            }
        }

//        if (bBlock.getIncomingEdges().containsKey("true") || bBlock.getIncomingEdges().containsKey("false")) {
//            return "{\n" + output + "}\n";
//        }

        return output.toString();
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
                declaration.dtype.dims != null ? new StanVisitor().evaluate(declaration.dtype.dims) : "",
                declaration.id.toString(),
                declaration.dims != null ? new StanVisitor().evaluate(declaration.dims) : "");

        declarationString = declarationString.replace("FLOAT", "real").
                replace("INTEGER", "int").
                replace("VECTOR", "vector").replace("MATRIX", "matrix");
        return declarationString;
    }


    private boolean isData(Statement statement, AST.Expression expression) {
        // get id
        String id = null;
        if (expression instanceof AST.Id) {
            id = expression.toString();
        } else if (expression instanceof AST.ArrayAccess) {
            id = ((AST.ArrayAccess) expression).id.toString();
        }

        if (id != null) {
            SymbolInfo info = statement.parent.getSymbolTable().fetch(id);
            if (info != null)
                return info.isData();
            else {
                System.out.println("Variable not found " + id);
            }
        }

        return false;
    }

    private String getLimitsString(BasicBlock basicBlock, AST.Expression expression) {
        String id = null;
        if (expression instanceof AST.Id) {
            id = expression.toString();
        } else if (expression instanceof AST.ArrayAccess) {
            id = ((AST.ArrayAccess) expression).id.toString();
        }
        if (id != null) {
            SymbolInfo info = basicBlock.getSymbolTable().fetch(id);
            if (info != null)
                return info.getLimitsString() != null ? info.getLimitsString() : "";
            else {
                System.out.println("Symbol not found " + id);
            }
        }

        return "";
    }


    private String dumpR(ArrayList<AST.Data> dataSets) {
        StringWriter stringWriter = null;
        stringWriter = new StringWriter();

        for (AST.Data data : dataSets) {
            String dataString = Utils.parseData(data, 'f');
            String dimsString = "";
            if (data.decl.dtype.dims != null && data.decl.dtype.dims.dims.size() > 0) {
                dimsString += data.decl.dtype.dims.toString();
            }
            if (data.decl.dims != null && data.decl.dims.dims.size() > 0) {
                if (dimsString.length() > 0) {
                    dimsString += ",";
                }
                dimsString += data.decl.dims.toString();
            }

            dataString = dataString.replaceAll("\\s", "").replaceAll("\\[", "").replaceAll("\\]", "");
            if (dimsString.length() == 0) {
                stringWriter.write(String.format("%s <- %s\n", data.decl.id, dataString));
            } else if (dimsString.split(",").length == 1) {
                stringWriter.write(String.format("%s <- c(%s)\n", data.decl.id, dataString));
            } else {
                stringWriter.write(String.format("%s <- structure(c(%s), .Dim=c(%s))\n", data.decl.id, dataString, dimsString));
            }
        }

        try {
            stringWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return stringWriter.toString();
    }

    private String processData(AST.Data data) throws IllegalAccessException {
        String typeString = "";
        String dimsString = "";
        String outerDimsString = "";

        if (data.decl.dtype.primitive == AST.Primitive.INTEGER) {
            typeString += "int";
        } else if (data.decl.dtype.primitive == AST.Primitive.FLOAT) {
            typeString += "real";
        } else if (data.decl.dtype.primitive == AST.Primitive.VECTOR) {
            if (data.annotations.get(0).annotationType == AST.AnnotationType.Type) {
                typeString += data.annotations.get(0).annotationValue.toString();
            } else {
                typeString += "vector";
            }
        } else if (data.decl.dtype.primitive == AST.Primitive.MATRIX) {
            if (data.annotations.get(0).annotationType == AST.AnnotationType.Type) {
                typeString += data.annotations.get(0).annotationValue.toString();
            } else {
                typeString += "matrix";
            }
        } else {
            throw new IllegalAccessException("Unknown data type : " + data.decl.dtype.primitive.toString());
        }

        if (data.decl.dtype.dims != null
                && data.decl.dtype.dims.dims != null
                && data.decl.dtype.dims.dims.size() > 0) {
            dimsString = "[" + data.decl.dtype.dims.toString() + "]";
        }

        if (data.decl.dims != null) {
            outerDimsString = "[" + data.decl.dims.toString() + "]";
        }

        return typeString + " " + dimsString + data.decl.id.toString() + " " + outerDimsString + ";";
    }

    @Override
    public Pair run() {
        return run("/tmp/stancode.stan", "/tmp/stancode.R");
    }

    public Pair run(String codeFileName, String dataFileName) {
        System.out.println("Running Stan...");
        try {
            FileWriter fileWriter = new FileWriter(codeFileName);
            fileWriter.write(this.getCode());
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            FileWriter fileWriter = new FileWriter(dataFileName);
            fileWriter.write(this.getData());
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Pair results = Utils.runCode(codeFileName, dataFileName, Utils.STANRUNNER);
        System.out.println(results.getLeft());
        System.out.println(results.getRight());
        return results;
    }
}
