package translators;

import grammar.AST;
import grammar.cfg.*;
import org.apache.commons.lang3.tuple.Pair;
import org.nd4j.linalg.api.ndarray.INDArray;
import translators.visitors.Edward2Visitor;
import utils.Utils;

import java.io.FileNotFoundException;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class Edward2Translator implements ITranslator {

    private String imports =
            "import tensorflow_probability as tfp\n" +
            "from tensorflow_probability import edward2 as ed\n" +
            "import tensorflow as tf\n"+
            "import numpy as np\n";

    public String getModelCode() {
        return modelCode;
    }

    private String modelCode = "";
    private String dataArgs = "";
    private String dataStr = "";
    private String paramArgs = "";
    private String paramStr = "";
    private String initStr = "";

    private String logJointCode =
            "log_joint = ed.make_log_joint_fn(%1$s)\n" +
            "def target_log_prob_fn(%2$s):\n" +
            "    return log_joint(data,\n" +
            "                     %3$s,\n" +
            "                     obs=%4$s)\n";

    private String hmcCode =
            "hmc_kernel = tfp.mcmc.HamiltonianMonteCarlo(\n" +
            "    target_log_prob_fn=target_log_prob_fn,\n" +
            "    step_size=0.1,\n" +
            "    num_leapfrog_steps=10)";

    private String dataSection = "";

    private static final String edward2TemplateFile = "src/main/resources/edward2Template";


    private String readTemplate(){
        try {
            byte[] bytes = Files.readAllBytes(Paths.get(edward2TemplateFile));
            String str = new String(bytes);
            return str;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public void translate(ArrayList<Section> sections) throws Exception {
        for(Section section:sections){
            if(section.sectionType == SectionType.DATA){
                dataSection += getDataString(section.basicBlocks.get(0).getData());

            }
            else if (section.sectionType == SectionType.FUNCTION){
                if(section.sectionName.equals("main")){
                    Set<BasicBlock> visited = new HashSet<>();
                    for(BasicBlock basicBlock:section.basicBlocks){
                        BasicBlock curBlock = basicBlock;
                        while(!visited.contains(curBlock)){
                            visited.add(curBlock);
                            String block_text = translate_block(curBlock);
                            if(curBlock.getParent().sectionName.equalsIgnoreCase("main")){
                                modelCode+=block_text;
                            }
                            else if(curBlock.getParent().sectionName.equalsIgnoreCase("transformedparam")){
                                modelCode += block_text;
                            }

                            if(curBlock.getEdges().size() > 0){
                                if(curBlock.getEdges().size() == 1){
                                    curBlock = curBlock.getEdges().get(0).getTarget();
                                }
                                else{
                                    String label = curBlock.getEdges().get(0).getLabel();
                                    if(label != null && label.equalsIgnoreCase("false")){
                                        curBlock = curBlock.getEdges().get(1).getTarget();
                                    }
                                    else
                                        curBlock = curBlock.getEdges().get(0).getTarget();
                                }
                            }


                        }
                    }
                }
                else{
                    throw new Exception("Unknown Function! ");
                }
            }
        }
    }

    private String translate_block(BasicBlock basicBlock){
        SymbolTable symbolTable = basicBlock.getSymbolTable();
        String output = "";
        if(basicBlock.getStatements().size() == 0)
            return output;

        for(Statement statement:basicBlock.getStatements()){
            if(statement.statement instanceof AST.AssignmentStatement){
                AST.AssignmentStatement assignmentStatement = (AST.AssignmentStatement) statement.statement;
                output += new Edward2Visitor(false).evaluate(assignmentStatement.lhs) + "=" +
                        new Edward2Visitor(true).evaluate(assignmentStatement.rhs) + "\n";

                if(assignmentStatement.rhs instanceof AST.FunctionCall && ((AST.FunctionCall) assignmentStatement.rhs).isDistribution){
                    output = output.substring(0, output.length()-2) + ",name = \"" + assignmentStatement.lhs.toString() + "\")\n";
                    if(Utils.isPrior(statement, assignmentStatement.lhs)) {
                        this.paramStr += assignmentStatement.lhs.toString() + "=" + assignmentStatement.lhs.toString() + ",";
                        this.initStr += "tf.random_normal([]),";
                    }
                    else {
                        this.paramStr += assignmentStatement.lhs.toString() + "=" + "data['" + assignmentStatement.lhs.toString() + "'],";
                    }
                }
            }
            else if(statement.statement instanceof AST.ForLoop){
//                AST.ForLoop loop = (AST.ForLoop) statement.statement;
//                output += "for(" + loop.toString() + ")\n";
            }
            else if(statement.statement instanceof AST.Decl){
                    AST.Decl declaration = (AST.Decl) statement.statement;
                    if(Utils.isPrior(statement, declaration.id)){
                        this.paramArgs += declaration.id +",";
                        //this.paramStr += declaration.id + "=" + declaration.id+",";
                    }
            }
        }

        if(basicBlock.getIncomingEdges().containsKey("true") || basicBlock.getIncomingEdges().containsKey("false")){
            return "{\n" + output + "}\n";
        }


        return output ;
    }

    private String getDataString(ArrayList<AST.Data> datasets){
        String d = "";
        for(AST.Data data: datasets){
            d += "data['" +  data.decl.id  + "'] =";
            dataArgs += data.decl.id + ",";
            dataStr += "data['" + data.decl.id +"'],";

            Edward2Visitor.dataItems.add(data.decl.id.toString());

            if(data.expression != null){
                d+=data.expression.toString();
            }
            else if(data.array != null){
                INDArray arr = Utils.parseArray(data.array, data.decl.dtype.primitive == AST.Primitive.INTEGER);
                d+="np.array(" + arr.toString().replaceAll("\\s", "") + ")";
            }
            else if(data.vector != null){
                INDArray arr = Utils.parseVector(data.vector, data.decl.dtype.primitive == AST.Primitive.INTEGER);
                d+="np.array(" + arr.toString().replaceAll("\\s", "") + ")";
            }

            d+="\n";
        }

        return d;
    }

    public String getDataSection() {
        return dataSection;
    }

    public String getCode(){
        String template = readTemplate();
        template = template.replace("$(data)", this.dataSection);
        //template = template.replace("$(data_str)", this.dataStr.substring(0, this.dataStr.length() - 1));
        template = template.replace("$(data_str)", "data");
        template = template.replace("$(model)", "def model(data):\n    " + this.modelCode.replaceAll("\n","\n    "));
        template = template.replace("$(params_list)", this.paramArgs.substring(0, this.paramArgs.length() - 1));
        template = template.replace("$(params)", this.paramStr.substring(0, this.paramStr.length() -1));
        template = template.replace("$(init)",  this.initStr.substring(0, this.initStr.length() -1));
        return template;
    }

    @Override
    public void run() {
        run("/tmp/edward2code.py");
    }

    public void run(String codeFileName){
        System.out.println("Running Edward...");
        try {
            FileWriter fileWriter = new FileWriter(codeFileName);
            fileWriter.write(this.getCode());
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Pair results = Utils.runCode(codeFileName, Utils.EDWARD2RUNNER);
        System.out.println(results.getLeft());
        System.out.println(results.getRight());
    }
}
