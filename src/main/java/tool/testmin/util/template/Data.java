package tool.testmin.util.template;

import org.antlr.v4.runtime.ParserRuleContext;

import java.util.ArrayList;
import java.util.Arrays;

public class Data {
    public String ID;
    public Type type;
    public Integer dataIntVal;
    public Double dataDoubleVal;
    public ParserRuleContext dataInt;
    public ParserRuleContext dataDouble;
    public ParserRuleContext dataInt1d;
    public ParserRuleContext dataDouble1d;

    public ParserRuleContext dataInt2d;
    public ParserRuleContext dataDouble2d;


    public ArrayList<String> indices;

    public double[] parseData() {
        if (this.dataDouble1d != null) {
            return Arrays.stream(this.dataDouble1d.getText().replaceAll("[<>\\[\\]]", "").split(",")).map(String::trim).mapToDouble(Double::parseDouble).toArray();
        } else
            return new double[0];
    }

    public enum Type {
        Array,
        Array2d,
        Index,
        Int,
        Double
    }

    public enum DataType {
        IntType,
        DoubleType
    }

}
