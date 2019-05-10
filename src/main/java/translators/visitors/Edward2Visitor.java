package translators.visitors;

import grammar.AST;
import translators.visitors.BaseVisitor;
import utils.Utils;

import javax.json.JsonObject;
import java.util.ArrayList;
import java.util.List;

public class Edward2Visitor extends BaseVisitor {

    boolean markData;

    public Edward2Visitor(boolean markData){
        this.markData = markData;
    }

    public static ArrayList<String> dataItems = new ArrayList<>();

    @Override
    public String evaluate(AST.FunctionCall functionCall) {
        String id = evaluate(functionCall.id);
        JsonObject currentModel = null;
        String distname = null;
        List<JsonObject> models = Utils.getDistributions(null);
        for (JsonObject model : models) {
            try {
                if (model.getString("name").equals(id)) {
                    currentModel = model;
                    break;
                }
            } catch (Exception e) {
                System.out.println("Key not found");
                continue;
            }
        }

        if (currentModel != null) {
            if(currentModel.containsKey("edward2")) {
                distname = currentModel.getString("edward2");
            }
            else{
                System.out.println("Distribution " + id + " not found");
            }
        }

        if(distname != null){
            String res = distname+"(";
            for (AST.Expression e : functionCall.parameters) {
                res += evaluate(e) + ",";
            }
            if (res.endsWith(","))
                res = res.substring(0, res.length() - 1);

            return res + ")";
        }

        return super.evaluate(functionCall);
    }

    @Override
    public String evaluate(AST.Id id){
        if(this.markData && dataItems.contains(id.toString())){
            return "data['" + id.toString() + "']";
        }
        else
            return id.toString();
    }
}
