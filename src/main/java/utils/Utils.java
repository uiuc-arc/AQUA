package utils;
import grammar.*;
import grammar.cfg.Statement;
import grammar.cfg.SymbolInfo;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.*;
import java.util.ArrayList;

import java.util.List;
import java.util.Properties;



public class Utils {

    private static String CONFIGURATIONFILE ="src/main/resources/config.properties";
    private static String DISTRIBUTIONSFILE;
    public static String STANRUNNER;
    public static String PSIRUNNER;
    public static String EDWARD2RUNNER;

    static {
    Properties properties = new Properties();
    try{
        FileInputStream fileInputStream = new FileInputStream(CONFIGURATIONFILE);
        properties.load(fileInputStream);

        STANRUNNER = properties.getProperty("stan.script");
        PSIRUNNER = properties.getProperty("psi.script");
        EDWARD2RUNNER = properties.getProperty("edward2.script");
        DISTRIBUTIONSFILE = properties.getProperty("distributionsFile");
    } catch (IOException e) {
        e.printStackTrace();
    }
    }

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

        System.out.println(data.decl.id.toString());
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
            fis = new FileInputStream(DISTRIBUTIONSFILE);
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

    public static DataParser readDataFile(String path){
        CharStream stream = null;
        try {
            stream = CharStreams.fromFileName(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
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
}
