package utils;

import java.util.ArrayList;

public class Dimension {
    public Types getType() {
        return type;
    }

    public ArrayList<String> getDims() {
        return dims;
    }

    private Types type;
    private ArrayList<String> dims;


    public Dimension(Types type, ArrayList<String> dims){
        this.type = type;
        this.dims = dims;
    }

    @Override
    public String toString() {
        return "Type : " + this.type + ", Dimension : " + (dims != null ? dims.toString() : "null");
    }

    public boolean isPrimitive(){
        return   (this.type == Types.INT || this.type == Types.FLOAT && (this.dims == null || this.dims.size() ==0));
    }
}
