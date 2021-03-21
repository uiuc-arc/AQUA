package tool.testmin.util;

import java.util.ArrayList;

public class Dimension {
    public String type;
    public ArrayList<String> dims;

    public Dimension() {
    }

    public Dimension(String type, ArrayList<String> dims) {
        this.type = type;
        this.dims = dims;
    }

    @Override
    public String toString() {
        return "Type : " + this.type + ", Dimension : " + (dims != null ? dims.toString() : "null");
    }

    public boolean isPrimitive() {
        return (this.type.equals("int") || this.type.equals("real")) && (this.dims == null || this.dims.size() == 0);
    }
}
