package tool.testmin.util;


import grammar.StanParser;
import grammar.Template2Parser;
import tool.testmin.TestMinimizer;
import tool.testmin.util.template.TemplateDimensionChecker;

import javax.json.JsonArray;
import javax.json.JsonObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static tool.testmin.util.TMUtil.includesSupport;

public class DistributionChooser {

    private List<JsonObject> models;
    private MyLogger logger;
    private String pps;
    private String chosenModelSupport;

    public DistributionChooser(MyLogger logger) {
        this.models = TMUtil.getDistributions(TestMinimizer.pps);
        this.logger = logger;

    }

    public String getChosenModelSupport() {
        return chosenModelSupport;
    }

    public String getSupportingDistrib(String currentDistribution, StanParser.Distribution_expContext ctx) {
        JsonObject currentModel = null;
        for (JsonObject model : this.models) {
            try {
                if (model.getString("name").equals(currentDistribution)) {
                    currentModel = model;
                    break;
                }
            } catch (Exception e) {
                System.out.println("Key not found");
                continue;
            }
        }

        if (currentModel == null) {
            return null;
        }

        String support = currentModel.getString("support");


        ArrayList<JsonObject> candidate_distribs = new ArrayList<>();
        for (JsonObject model : this.models) {
            try {
                if (model.containsKey("complex") &&
                        !model.getBoolean("complex") &&
                        model.getString("type").equals(currentModel.getString("type"))) {

                    candidate_distribs.add(model);
                }
            } catch (Exception e) {
                continue;
            }
        }

        JsonObject chosenModel = candidate_distribs.get(new Random().nextInt(candidate_distribs.size()));
        return modelToString(chosenModel, ctx.expression());
    }

    public String getSupportingDistrib2(String currentDistribution, StanParser.Distribution_expContext ctx) {
        JsonObject currentModel = null;
        for (JsonObject model : this.models) {
            try {
                if (model.getString("name").equals(currentDistribution)) {
                    currentModel = model;
                    break;
                }
            } catch (Exception e) {
                System.out.println("Key not found");
                continue;
            }
        }

        if (currentModel == null) {
            return null;
        }

        String support = currentModel.getString("support");


        ArrayList<JsonObject> candidate_distribs = new ArrayList<>();
        for (JsonObject model : this.models) {
            try {
                if (model.containsKey("name") && includesSupport(model.getString("support"), support)) {
                    candidate_distribs.add(model);
                }
            } catch (Exception e) {
                continue;
            }
        }

        JsonObject chosenModel = candidate_distribs.get(new Random().nextInt(candidate_distribs.size()));

        return modelToString(chosenModel, ctx.expression());
    }


    public String getSupportingDistrib2(String program, String currentDistribution, Template2Parser.DistexprContext ctx) {
        JsonObject currentModel = null;
        for (JsonObject model : this.models) {
            try {
                if (model.getString("name").equals(currentDistribution)) {
                    currentModel = model;
                    break;
                }
            } catch (Exception e) {
                System.out.println("Key not found");
                continue;
            }
        }

        if (currentModel == null) {
            return null;
        }

        String support = currentModel.getString("support");


        ArrayList<JsonObject> candidate_distribs = new ArrayList<>();
        for (JsonObject model : this.models) {
            try {
                if (model.containsKey("name") && includesSupport(model.getString("support"), support)) {
                    candidate_distribs.add(model);
                }
            } catch (Exception e) {
                continue;
            }
        }


        JsonObject chosenModel = chooseDistrib(candidate_distribs);
        this.chosenModelSupport = chosenModel.getString("support");
        System.out.println("Chosen model: " + chosenModel.getString("name"));
        return modelToString(ctx.params().param(), chosenModel, ctx, program);
    }

    private JsonObject chooseDistrib(ArrayList<JsonObject> candidates) {
        System.out.println("Choosing model...");
        if (TMUtil.DistributionChoice.equals("TIMING")) {
            String[] timingfile = new String[0];
            try {
                timingfile = new String(Files.readAllBytes(Paths.get(TMUtil.TimingFile))).split("\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
            ArrayList<Double> timings = new ArrayList<>(candidates.size());
            double sum = 0.0;
            for (JsonObject model : candidates) {
                String name = model.getString("name");
                Double val = null;
                for (String t : timingfile) {
                    if (t.split(",")[0].equals(name)) {
                        try {
                            if (TestMinimizer.pps.contains("pyro"))
                                val = Double.parseDouble(t.split(",")[1]);
                            else
                                val = Double.parseDouble(t.split(",")[2]);
                        } catch (Exception e) {
                            val = null;
                        }
                        break;
                    }
                }

                if (val == null) {
                    timings.add((105.0 - 104.0));
                } else {
                    timings.add((105.0 - val));
                }
                sum += timings.get(timings.size() - 1);
            }
            for (int i = 0; i < timings.size(); i++) {
                timings.set(i, timings.get(i) / sum);
            }

            for (int i = 1; i < timings.size(); i++) {
                timings.set(i, timings.get(i - 1) + timings.get(i));
            }
            double r = new Random().nextDouble();
            for (int i = 0; i < timings.size(); i++) {
                double prev = i > 0 ? timings.get(i - 1) : 0;
                if (prev <= r && r <= timings.get(i)) {
                    return candidates.get(i);
                }
            }

            System.out.println("No match!");
            return candidates.get(new Random().nextInt(candidates.size()));
        } else if (TMUtil.DistributionChoice.equals("EXPONFAMILY")) {
            ArrayList<JsonObject> filtered = new ArrayList<>();
            for (JsonObject model : candidates) {
                if (Arrays.asList(new String[]{"normal", "beta", "gamma"}).contains(model.getString("name"))) {
                    filtered.add(model);
                }
            }
            if (filtered.isEmpty()) {
                System.out.println("No expon family distribution found! Choosing random");
                return candidates.get(new Random().nextInt(candidates.size()));
            } else {
                return filtered.get(new Random().nextInt(filtered.size()));
            }
        } else {
            return candidates.get(new Random().nextInt(candidates.size()));
        }
    }

    public String getSupportingDistrib(String currentDistribution, Template2Parser.DistexprContext ctx) {
        JsonObject currentModel = null;
        for (JsonObject model : this.models) {
            try {
                if (model.getString("name").equals(currentDistribution)) {
                    currentModel = model;
                    break;
                }
            } catch (Exception e) {
                System.out.println("Key not found");
                continue;
            }
        }

        if (currentModel == null) {
            return null;
        }

        String support = currentModel.getString("support");


        ArrayList<JsonObject> candidate_distribs = new ArrayList<>();
        for (JsonObject model : this.models) {
            try {
                if (model.containsKey("complex") &&
                        !model.getBoolean("complex") &&
                        model.getString("type").equals(currentModel.getString("type"))) {

                    candidate_distribs.add(model);
                }
            } catch (Exception e) {
                continue;
            }
        }

        JsonObject chosenModel = candidate_distribs.get(new Random().nextInt(candidate_distribs.size()));
        return modelToString(ctx.params().param(), chosenModel, ctx, null);
    }

    private String modelToString(JsonObject model, List<StanParser.ExpressionContext> expressions) {

        String distCall = model.getString("name");

        if (model.getString("name").equals("uniform")) {

            if (expressions.size() > 1 && expressions.get(1) instanceof StanParser.IntegerContext || expressions.get(1) instanceof StanParser.DoubleContext) {
                double value = Double.parseDouble(expressions.get(1).getText());
                if (value <= 0) {
                    distCall += "-2, 0)";
                }
            } else if (expressions.size() > 0 && expressions.get(0) instanceof StanParser.IntegerContext || expressions.get(0) instanceof StanParser.DoubleContext) {
                double value = Double.parseDouble(expressions.get(0).getText());
                if (value >= 0) {
                    distCall += "0, 1)";
                } else {
                    distCall += "-1, 1)";
                }

            } else {
                distCall += "0, 1)";
            }

            return distCall;
        }

        JsonArray args = model.getJsonArray("args");
        for (JsonObject arg : args.getValuesAs(JsonObject.class)) {


            String type = arg.getString("type");
            if (type.equals("f+") || type.equals("(0,1)")) {
                distCall += "1.0,";
            } else if (type.equals("f") || type.equals("0f+") || type.equals("0i+") || type.equals("i")) {
                distCall += "0,";
            } else {
                distCall += "1,";
            }
        }

        distCall = distCall.substring(0, distCall.length() - 1) + ")";
        return distCall;
    }

    private String modelToString(List<Template2Parser.ParamContext> expressions, JsonObject model, Template2Parser.DistexprContext ctx, String program) {
        String distCall = model.getString("name");

        if (ctx.vectorDIMS() != null) {
            distCall += ctx.vectorDIMS().getText();
        }
        distCall += "(";

        if (model.getString("name").equals("uniform")) {
            String param1 = TemplateDimensionChecker.getSubstitute(program, expressions.get(0), "0");
            String param2 = TemplateDimensionChecker.getSubstitute(program, expressions.get(0), "1");

            distCall += param1 + "," + param2 + ")";
            if (ctx.dims() != null) {
                distCall += "[" + ctx.dims().getText() + "]";
            }

            System.out.println("Choosing uniform dist : " + distCall);
            return distCall;
        } else {
            JsonArray args = model.getJsonArray("args");
            int p = 0;
            for (JsonObject arg : args.getValuesAs(JsonObject.class)) {
                String type = arg.getString("type");
                String val;
                if (type.equals("f+") || type.equals("(0,1)")) {
                    val = "1.0";
                } else if (type.equals("f") || type.equals("0f+") || type.equals("0i+") || type.equals("i")) {
                    val = "0";
                } else {
                    val = "1";
                }
                if (p < expressions.size()) {
                    String repl = TemplateDimensionChecker.getSubstitute(program, expressions.get(p), val);
                    distCall += repl + ",";
                } else {
                    distCall += val + ",";
                }
                p++;
            }

            distCall = distCall.substring(0, distCall.length() - 1) + ")";
            if (ctx.dims() != null) {
                distCall += "[" + ctx.dims().getText() + "]";
            }
            return distCall;
        }
    }
}
