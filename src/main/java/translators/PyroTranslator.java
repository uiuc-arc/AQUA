package translators;

import grammar.AST;
import grammar.cfg.*;
import org.apache.commons.lang3.tuple.Pair;
import org.nd4j.linalg.api.ndarray.INDArray;
import translators.visitors.PyroVisitor;
import utils.CommonUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class PyroTranslator implements ITranslator {

    private String dataSection = "";
    private String dataArgs = "";
    private String dataStr = "";
    private String modelCode = "";

    private String readTemplate(){
        try {
            String pyroTemplateFile = "src/main/resources/pyroTemplate";
            byte[] bytes = Files.readAllBytes(Paths.get(pyroTemplateFile));
            return new String(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getDataString(ArrayList<AST.Data> datasets){
        String d = "data = dict()\n";
        for(AST.Data data: datasets){
            d += "data['" +  data.decl.id  + "'] =";
            dataArgs += data.decl.id + ",";
            dataStr += "data['" + data.decl.id +"'],";

            PyroVisitor.dataItems.add(data.decl.id.toString());

            if(data.expression != null){
                d+=data.expression.toString();
            }
            else if(data.array != null){
                INDArray arr = CommonUtils.parseArray(data.array, data.decl.dtype.primitive == AST.Primitive.INTEGER);
                d+="torch.Tensor(np.array(" + arr.toString().replaceAll("\\s", "") + "))";
            }
            else if(data.vector != null){
                INDArray arr = CommonUtils.parseVector(data.vector, data.decl.dtype.primitive == AST.Primitive.INTEGER);
                d+="torch.Tensor(np.array(" + arr.toString().replaceAll("\\s", "") + "))";
            }

            d+="\n";
        }

        return d;
    }

    @Override
    public void translate(ArrayList<Section> sections) throws Exception {
        Set<BasicBlock> visited = new HashSet<>();
        for(Section section:sections) {
            if (section.sectionType == SectionType.DATA) {
                dataSection += getDataString(section.basicBlocks.get(0).getData());
            }
            else if (section.sectionType == SectionType.FUNCTION) {
                if (section.sectionName.equals("main")) {
                    this.visitBlocks(section.basicBlocks);
//                    for (BasicBlock basicBlock : section.basicBlocks) {
//                        BasicBlock curBlock = basicBlock;
//                        while (!visited.contains(curBlock)) {
//                            visited.add(curBlock);
//                            String block_text = translate_block(curBlock);
//                            // TODO: if else and for loop block handling
//                            if(curBlock.getIncomingEdges().containsKey("true")){
//                                block_text =  "    " + block_text.replaceAll("\n", "\n    ");
//                            }
//                            else if(curBlock.getIncomingEdges().containsKey("false")){
//                                block_text =  "    " + block_text.replaceAll("\n", "\n    ");
//                                if(curBlock.getIncomingEdges().get("false").getLastStatement().statement instanceof AST.IfStmt){
//                                    block_text = "\nelse:\n" + block_text;
//                                }
//                            }
////
////                            if(curBlock.getOutgoingEdges().containsKey("back")){
////                                block_text = block_text +"}\n";
////                            }
//
//                            if (curBlock.getParent().sectionName.equalsIgnoreCase("main")) {
//                                modelCode += block_text;
//                            } else {
//                                //throw new Exception("Not handled");
//                            }
//
//
//                            if (curBlock.getEdges().size() > 0) {
//                                BasicBlock prevBlock = curBlock;
//                                if (curBlock.getEdges().size() == 1) {
//                                    curBlock = curBlock.getEdges().get(0).getTarget();
//                                } else {
//                                    String label = curBlock.getEdges().get(0).getLabel();
//                                    if (label != null && label.equalsIgnoreCase("false") && !visited.contains(curBlock.getEdges().get(1).getTarget())) {
//                                        curBlock = curBlock.getEdges().get(1).getTarget();
//                                    } else
//                                        curBlock = curBlock.getEdges().get(0).getTarget();
//                                }
//                                //if next is meet block
////                                if (curBlock.getIncomingEdges().containsKey("meet") && !isIfNode(prevBlock)) {
////                                    modelCode += "}\n";
////
////                                }
//
//                            }
//                        }
//                    }
                } else {
                    throw new Exception("Unknown Function! ");
                }
            }
        }

    }

    public void visitBlocks(ArrayList<BasicBlock> basicBlocks){
        Queue<BasicBlock> queue = new LinkedList<>();
        ArrayList<BasicBlock> visited = new ArrayList<>();
        queue.add(basicBlocks.get(0));
        while(!queue.isEmpty()){
            BasicBlock cur = queue.poll();
            if(visited.contains(cur))
                continue;
            visited.add(cur);

            String block_text = translate_block(cur);

            if(cur.getIncomingEdges().containsKey("true")){
                block_text =  "    " + block_text.replaceAll("\n", "\n    ") + "\n";
            }
            else if(cur.getIncomingEdges().containsKey("false") && !cur.getIncomingEdges().containsKey("meet")){
                block_text =  "    " + block_text.replaceAll("\n", "\n    ");
                if(cur.getIncomingEdges().get("false").getLastStatement().statement instanceof AST.IfStmt){
                    block_text = "\nelse:\n" + block_text + "\n";
                }
            }

            modelCode += block_text;
            if(cur.getOutgoingEdges().size() > 0){
                // visit true edge first
                if(cur.getOutgoingEdges().containsKey("true")){
                    queue.add(cur.getOutgoingEdges().get("true"));
                }
                if(cur.getOutgoingEdges().containsKey("false")){
                    queue.add(cur.getOutgoingEdges().get("false"));
                }
                for(BasicBlock b:cur.getOutgoingEdges().values()){
                    if(!queue.contains(b)){
                        queue.add(b);
                    }
                }
            }
        }

    }

    private String translate_block(BasicBlock basicBlock){
        String output = "";
        if(basicBlock.getStatements().size() == 0)
            return output;

        for(Statement statement:basicBlock.getStatements()){
            if(statement.statement instanceof AST.AssignmentStatement){
                AST.AssignmentStatement assignmentStatement = (AST.AssignmentStatement) statement.statement;
                if(CommonUtils.containsAnnotation(statement.statement.annotations, AST.AnnotationType.Observe)){
                    output += "with pyro.iarange(\"data\"):\n    ";
                    String rhs = new PyroVisitor(basicBlock, true, assignmentStatement.lhs).evaluate(assignmentStatement.rhs);
                    output += rhs.replaceAll("\n", "\n    ");
//                    output += new PyroVisitor(basicBlock, false).evaluate(assignmentStatement.lhs) + "=" +
//                            new PyroVisitor(basicBlock, true, assignmentStatement.lhs).evaluate(assignmentStatement.rhs) + "\n";

                }
                else {
                    output += new PyroVisitor(basicBlock, false).evaluate(assignmentStatement.lhs) + "=" +
                            new PyroVisitor(basicBlock, false, assignmentStatement.lhs).evaluate(assignmentStatement.rhs) + "\n";
                }
               // if(assignmentStatement.rhs instanceof AST.FunctionCall && ((AST.FunctionCall) assignmentStatement.rhs).isDistribution){
                 //   output = output.substring(0, output.length()-2) + ",name = \"" + assignmentStatement.lhs.toString() + "\")\n";
//                    if(Utils.isPrior(statement, assignmentStatement.lhs)) {
//                        this.paramStr += assignmentStatement.lhs.toString() + "=" + assignmentStatement.lhs.toString() + ",";
//                        this.initStr += String.format("tf.random_normal([%s]),", SymbolInfo.getDimsString(basicBlock.getSymbolTable(), assignmentStatement.lhs) );
//                    }
//                    else {
//                        this.paramStr += assignmentStatement.lhs.toString() + "=" + "data['" + assignmentStatement.lhs.toString() + "'],";
//                    }
                //}
            }
            else if(statement.statement instanceof AST.Decl){
                AST.Decl declaration = (AST.Decl) statement.statement;
                if(CommonUtils.isPrior(statement, declaration.id)){
                    //this.paramArgs += declaration.id +",";
                    //this.paramStr += declaration.id + "=" + declaration.id+",";
                }
            }
            else if(statement.statement instanceof AST.IfStmt){
                AST.IfStmt ifStmt = (AST.IfStmt) statement.statement;
                //output += "if (" + ifStmt.condition.toString() + "):\n"; //OLD
                output += "if (" + new PyroVisitor(basicBlock, false).evaluate(ifStmt.condition) + "):\n";

            }
            else if(statement.statement instanceof AST.ForLoop){
                AST.ForLoop forLoop = (AST.ForLoop) statement.statement;
                output += String.format("for %s in range(%s,%s):\n", forLoop.loopVar.id,
                        forLoop.range.start.toString(),
                        forLoop.range.end.toString());
            }
        }

//        if(basicBlock.getIncomingEdges().containsKey("true") || basicBlock.getIncomingEdges().containsKey("false")){
//            return "{\n" + output + "}\n";
//        }


        return output ;
    }

    public String getCode() {
        String template = readTemplate();
        if(template != null){
            template = template.replace("$(data)", this.dataSection);
            template = template.replace("$(model)", "def model(data):\n    " + this.modelCode.replaceAll("\n","\n    "));
            template = template.replace("$(lr)", "0.01");
            template = template.replace("$(iter)", "1000");
            return template;
        }
        return "";
    }

    @Override
    public Pair run() {
        return null;
    }
}
