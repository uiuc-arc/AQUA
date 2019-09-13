package utils;

import grammar.DataBaseListener;
import grammar.DataParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class DataReader extends DataBaseListener {

    private String curId;
    private Map<String, Object> dataMap = new HashMap<>();
    private String curString = "";
    private ArrayList<String> curArr;
    private ArrayList<Integer> curDim;

    @Override
    public void enterAssign(DataParser.AssignContext ctx) {
        this.curId = ctx.ID().getText();
    }

    @Override
    public void enterArray(DataParser.ArrayContext ctx) {
        if(ctx.getParent() instanceof DataParser.DimContext){
            curDim = new ArrayList<>();
            for(DataParser.PrimitiveContext pr: ctx.primitive()){
                curDim.add(Integer.parseInt(pr.getText()));
            }
        }
        else {
            curArr = new ArrayList<>();
            for (DataParser.PrimitiveContext pr : ctx.primitive()) {
                curArr.add(pr.getText());
            }
        }
    }

    @Override
    public void exitStructure(DataParser.StructureContext ctx) {
        if(curDim != null && curDim.size() == 2){
            String arr[][]  = new String[curDim.get(0)][curDim.get(1)];
            for(int i=0; i<curDim.get(0);i++){
                for(int j=0; j<curDim.get(1); j++){
                    // getting in column major and storing in row major
                    arr[i][j] = curArr.get(j*curDim.get(1)+i);
                }
            }
            this.dataMap.put(this.curId, arr);
            return;
        }

        assert false;
    }

    @Override
    public void exitAssign(DataParser.AssignContext ctx) {
        if(ctx.dt().array() != null)
            this.dataMap.put(this.curId, curArr);
        else if(ctx.dt().primitive() != null)
            this.dataMap.put(this.curId, ctx.dt().primitive().getText());
    }

    public void printData(){
        for(String id:this.dataMap.keySet()){
            if(this.dataMap.get(id) instanceof ArrayList)
                System.out.println(id + " : " + ((ArrayList<String>) this.dataMap.get(id)).toString());
            else if(this.dataMap.get(id) instanceof String[][])
                System.out.println(id + " : " + strArrtoString((String[][]) this.dataMap.get(id)) );
            else if(this.dataMap.get(id) instanceof String){
                System.out.println(id + " : " + this.dataMap.get(id));
            }
        }
    }

    public String getData(String id){
        if(this.dataMap.get(id) instanceof ArrayList)
            return ((ArrayList<String>) this.dataMap.get(id)).toString();
        else if(this.dataMap.get(id) instanceof String[][])
            return  strArrtoString((String[][]) this.dataMap.get(id));
        else if(this.dataMap.get(id) instanceof String){
            return (String) this.dataMap.get(id);
        }
        return null;
    }

    private String strArrtoString(String[][] arr){
        StringBuilder res = new StringBuilder("[");
        for(int i = 0; i<arr.length; i++){
            res.append("[");
            for(int j=0; j<arr[0].length; j++){
                res.append(arr[i][j]).append(",");
            }
            res.deleteCharAt(res.length()-1);
            res.append("],");
        }
        return res.substring(0, res.length()-1) +"]";
    }


}
