package utils;
import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.layout.mxIGraphLayout;
import com.mxgraph.util.mxCellRenderer;
import grammar.*;
import grammar.cfg.BasicBlock;
import grammar.cfg.Edge;
import grammar.cfg.Statement;
import grammar.cfg.SymbolInfo;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jgrapht.Graph;
import org.jgrapht.ext.JGraphXAdapter;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import javax.imageio.ImageIO;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;

import java.util.List;



public class Utils {

    private static String DISTRIBUTIONSFILE="src/main/resources/models.json";

    public static INDArray parseVector(AST.Vector vector, boolean isInteger){
        return getIndArray(vector.arrays, vector.vectors, vector.expressions, isInteger);
    }

    private static INDArray getIndArray(ArrayList<AST.Array> arrays, ArrayList<AST.Vector> vectors, ArrayList<AST.Expression> expressions, boolean isInteger) {
        INDArray arrnd = null;
        if(arrays != null && arrays.size() > 0){
            //arrnd = null;
            int i = 0;
            for(AST.Array arr1: arrays){
                INDArray newarr = parseArray(arr1, isInteger);
                if(arrnd == null){
                    arrnd = Nd4j.zeros(isInteger ? DataType.INT : DataType.DOUBLE, arrays.size(), newarr.columns());
                }

                arrnd.putRow(i++, newarr);
            }
        }
        else if(vectors != null && vectors.size() > 0){
//            arrnd = Nd4j.zeros(isInteger ? DataType.INT : DataType.DOUBLE, vectors.size());
            int i =0;
            for(AST.Vector arr1: vectors){
                INDArray newarr = parseVector(arr1, isInteger);
                if(arrnd == null){
                    arrnd = Nd4j.zeros(isInteger ? DataType.INT : DataType.DOUBLE, vectors.size(), newarr.columns());
                }
                arrnd.putRow(i++, newarr);
            }
        }
        else{
            arrnd = Nd4j.zeros(isInteger ? DataType.INT : DataType.DOUBLE, expressions.size());
            int i=0;
            for(AST.Expression val: expressions){
                if(!isInteger) {
                    arrnd.putScalar(i++, Double.parseDouble(val.toString()));
                }
                else {
                    arrnd.putScalar(i++, Integer.parseInt(val.toString()));
                }
            }
        }


        return arrnd;
    }

    public static String parseData(AST.Data data, char order){

        // System.out.println(data.decl.id.toString());
        if(data.expression != null){
            return data.expression.toString();
        }
        else if(data.array != null){
            INDArray ndarray = Utils.parseArray(data.array, data.decl.dtype.primitive == AST.Primitive.INTEGER);

            if(ndarray.shape().length > 1){
                return  Utils.printArray(Nd4j.toFlattened(order, ndarray));
            }
            else{
                return  Utils.printArray(Nd4j.toFlattened(ndarray));
            }
        }
        else{
            INDArray ndarray = Utils.parseVector(data.vector, data.decl.dtype.primitive == AST.Primitive.INTEGER);
            if(ndarray.shape().length > 1){
                return  Utils.printArray(Nd4j.toFlattened(order, ndarray));
            }
            else{
                return Utils.printArray(Nd4j.toFlattened(ndarray));
            }
        }
    }

    public static INDArray parseArray(AST.Array array, boolean isInteger){
        return getIndArray(array.arrays, array.vectors, array.expressions, isInteger);
    }

    public static Pair<String, String> runCode(String filename, String RunScript) {
        return runCode(filename, "", RunScript);
    }

    public static Pair<String, String> runCode(String filename, String datafilename, String RunScript){
        Process p = null;
        try {
            p = Runtime.getRuntime().exec(RunScript + " " + filename + " " + datafilename);
            p.waitFor();
            StringBuilder output = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            reader.lines().forEach(e -> output.append(e).append("\n"));
            reader = new BufferedReader(new InputStreamReader(p.getErrorStream()));

            StringBuilder error = new StringBuilder();
            reader.lines().forEach(e -> error.append(e).append("\n"));

            return new ImmutablePair<>(output.toString(), error.toString());

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return null;
    }
    /*
    public static Pair<String, String> runPsi(String filename){
        Process p = null;
        try {
            p = Runtime.getRuntime().exec( PSIRUNNER + " " + filename + " " );

            p.waitFor();
            StringBuilder output = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            reader.lines().forEach(e -> output.append(e).append("\n"));
            reader = new BufferedReader(new InputStreamReader(p.getErrorStream()));

            StringBuilder error = new StringBuilder();
            reader.lines().forEach(e -> error.append(e).append("\n"));
            return new ImmutablePair<>(output.toString(), error.toString());

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return null;
    }
    */

    public static String printArray(INDArray array){
        String res = "";
        for(int i =0; i < array.length(); i++){
            res += array.getDouble(i)+",";
        }
        return "[" + res.substring(0, res.length() -1) + "]";
    }

    public static List<JsonObject> getDistributions(String pps) {
        FileInputStream fis;
        List<JsonObject> models = null;
        try {
            String[] configFileNameSplit = DISTRIBUTIONSFILE.split("/");
            String configFileName = configFileNameSplit[configFileNameSplit.length - 1];
            Path temp = Files.createTempFile(configFileName.split("\\.")[0],"." +  configFileName.split("\\.")[1]);
            Files.copy(Utils.class.getClassLoader().getResourceAsStream(configFileName), temp, StandardCopyOption.REPLACE_EXISTING);
            fis = new FileInputStream(temp.toFile());
            // fis = new FileInputStream(configFileName);
            JsonReader jsonReader = Json.createReader(fis);
            JsonObject jsonObject = jsonReader.readObject();
            models = new ArrayList<>();
            List<JsonObject> dists = jsonObject.getJsonArray("models").getValuesAs(JsonObject.class);
            for (JsonObject j : dists) {
                if (pps != null && !pps.isEmpty()) {
                    if (j.containsKey(pps)) {
                        models.add(j);
                    }
                }
                else{
                    models.add(j);
                }
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return models;
    }

    public static boolean isPrior(Statement statement, AST.Expression expression){
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

    public static StanParser readStanFile(String path){
        CharStream stream = null;
        try {
            stream = CharStreams.fromFileName(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
        StanLexer stanLexer = new StanLexer(stream);
        CommonTokenStream tokens = new CommonTokenStream(stanLexer);
        StanParser stanParser = new StanParser(tokens);
        return stanParser;
    }

    public static Template3Parser readTemplateFile(String path){
        CharStream stream = null;
        try {
            stream = CharStreams.fromFileName(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
       return readTemplateFromString(stream);
    }

    public static Template3Parser readTemplateFromString(CharStream stream){
        Template3Lexer template3Lexer = new Template3Lexer(stream);
        CommonTokenStream tokens = new CommonTokenStream(template3Lexer);
        return new Template3Parser(tokens);
    }

    public static DataParser readDataFile(String path) throws IOException {
        CharStream stream = null;
        stream = CharStreams.fromFileName(path);
        DataLexer dataLexer = new DataLexer(stream);
        CommonTokenStream tokens = new CommonTokenStream(dataLexer);
        DataParser dataParser = new DataParser(tokens);
        return dataParser;
    }

    public static String complexTypeMap(String type){
        if(type.equals("simplex"))
            return "vector";
        else if(type.equals("vector")){
            return type;
        }
        else if(type.equals("matrix")){
            return type;
        }
        else if(type.equals("cov_matrix")){
            return "matrix";
        }
        else {
            System.out.println(type);
            assert false;

        }
        return "";
    }

    public static boolean checkIfLastStatement(ParserRuleContext statementContext){
        if(statementContext instanceof StanParser.StatementContext){
            if(statementContext.getParent() instanceof StanParser.BlockContext){
                StanParser.BlockContext blockContext = (StanParser.BlockContext) statementContext.getParent();
                if(blockContext.getParent() instanceof StanParser.Transformed_param_blkContext || blockContext.getParent() instanceof StanParser.Transformed_data_blkContext || blockContext.getParent() instanceof StanParser.Generated_quantities_blkContext) {
                    if (blockContext.statement(blockContext.statement().size() - 1) == statementContext) {
                        // last statement
                        return true;
                    }
                }
            }
        }
        return false;

    }

    public static void showGraph(Graph graph, String outputfile){
        JGraphXAdapter<BasicBlock, Edge> graphXAdapter = new JGraphXAdapter<BasicBlock, Edge>(graph);
        mxIGraphLayout layout = new mxHierarchicalLayout(graphXAdapter);// new mxCircleLayout(graphXAdapter);
        layout.execute(graphXAdapter.getDefaultParent());
        BufferedImage image =
                mxCellRenderer.createBufferedImage(graphXAdapter, null, 2, Color.WHITE, true, null);
        File imgFile = new File(outputfile);
        try {
            ImageIO.write(image, "PNG", imgFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
