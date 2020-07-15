package grammar.transformations.util;

import grammar.cfg.CFGBuilder;
import grammar.transformations.*;
import org.antlr.v4.runtime.TokenStreamRewriter;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apache.commons.io.FileUtils;
import translators.Stan2IRTranslator;
import translators.StanTranslator;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;


public class TransWriter {
    String tempFileName;
    String suffix = ".tmpT";
    String code;
    String reuseCode;
    String predCode;
    OrgPredRewriter orgPredRewriter = null;


    String functionPrefix = "functions {\n" +
            "    real pl_RU(real f_lp_corr, real f_lp_good, real f_lp_org_good) {\n" +
            "        return exp(f_lp_corr + f_lp_good - f_lp_org_good);\n" +
            "    }\n" +
            "    real pl_RL(real f_lp_good, real f_lp_org_good) {\n" +
            "        return exp(f_lp_good - f_lp_org_good);\n" +
            "    }\n" +
            "    real normal_lpdf_C(real f_s, real f_lambda) {\n" +
            "        return log(((2*pi())^(0.5 - 0.5*f_lambda)*f_s^(1 - f_lambda))/sqrt(f_lambda));\n" +
            "    }\n" +
            "    real bernoulli_lpmf_C(real f_s, real f_lambda) {\n" +
            "        return log((1 - f_s)^f_lambda + f_s^f_lambda);\n" +
            "    }\n" +
            "    real bernoulli_logit_lpmf_C(real f_s, real f_lambda) {\n" +
            "        real s;\n" +
            "        s = inv_logit(f_s);\n" +
            "        return log((1 - s)^f_lambda + s^f_lambda);\n" +
            "    }\n" +
            "    real binomial_lpmf_C(real f_s, real f_n, real f_lambda) {\n" +
            "        return log(((1 - f_s)^f_n))*f_lambda;\n" +
            "    }\n" +
            "    real binomial_logit_lpmf_C(real f_s, real f_n, real f_lambda) {\n" +
            "        real s;\n" +
            "        s = inv_logit(f_s);\n" +
            "        return log(((1 - s)^f_n))*f_lambda;\n" +
            "    }\n" +
            "    real poisson_lpmf_C(real f_s, real f_lambda) {\n" +
            "        if (f_s > 100) {\n" +
            "            return log(((2*pi())^(0.5 - 0.5*f_lambda)*sqrt(f_s)^(1 - f_lambda))/sqrt(f_lambda));\n" +
            "        }\n" +
            "        else {\n" +
            "            real k[200];\n" +
            "            for (i in 0:199) {k[i+1] = poisson_lpmf(i| f_s) * f_lambda;}\n" +
            "            return log_sum_exp(k);\n" +
            "        }\n" +
            "    }\n" +
            "}\n";

    ParseTreeWalker walker = new ParseTreeWalker();

    public TransWriter(String stanfile, String standata) {
        System.out.println("Stanfile" + stanfile);
        Stan2IRTranslator stan2IRTranslator = new Stan2IRTranslator(stanfile, standata);
        tempFileName = stanfile.replace(".stan", "");
        code = stan2IRTranslator.getCode();
        System.out.println("========Stan Code to Template=======");
        System.out.println(code);

    }


    public TransWriter(String templateFile) throws IOException {
        tempFileName = templateFile.replace(".template","");
        code = new String(Files.readAllBytes(Paths.get(templateFile)));
    }

    public void setReuseCode() {
        reuseCode = code;
    }

    public void resetCode() {
        code = reuseCode;
    }

    public void transformOrgPredCode() throws IOException {
        File file = File.createTempFile(tempFileName, suffix);
        FileUtils.writeStringToFile(file, code);
        CFGBuilder cfgBuilder = new CFGBuilder(file.getAbsolutePath(), null, false);
        TokenStreamRewriter antlrRewriter = new TokenStreamRewriter(cfgBuilder.parser.getTokenStream());
        OrgPredCode orgPredCode = new OrgPredCode(cfgBuilder, antlrRewriter);
        cfgBuilder.parser.reset();
        walker.walk(orgPredCode, cfgBuilder.parser.template());
        predCode =  antlrRewriter.getText();
    }

    public String getCode() {
        return code;
    }

    public String getPredCode() {
        return predCode;
    }

    public String getStanPredCode () throws Exception {
        File file = File.createTempFile(tempFileName, suffix);
        FileUtils.writeStringToFile(file, predCode);
        StanTranslator stanTranslator = new StanTranslator();
        stanTranslator.translate((new CFGBuilder(file.getAbsolutePath(), null, false)).getSections());
        return functionPrefix + stanTranslator.getCode();
    }

    public String getStanGenCode (String progpath) throws Exception {
        File file = File.createTempFile(tempFileName, suffix);
        FileUtils.writeStringToFile(file, code);
        CFGBuilder cfgBuilder = new CFGBuilder(file.getAbsolutePath(), null, false);
        OrgGenCode orgGenCode = new OrgGenCode(cfgBuilder);
        cfgBuilder.parser.reset();
        walker.walk(orgGenCode, cfgBuilder.parser.template());
        FileUtils.writeStringToFile(file, orgGenCode.genCode);
        StanTranslator stanTranslator = new StanTranslator();
        stanTranslator.translate((new CFGBuilder(file.getAbsolutePath(), null, false)).getSections());
        PrintWriter out = new PrintWriter(progpath + ".npl");
        out.println(orgGenCode.nplCode);
        out.close();
        return stanTranslator.getCode();
    }

    public void addPredCode(String transCode, String transName) throws Exception {
        if (orgPredRewriter == null) {
            File file = File.createTempFile(tempFileName, suffix);
            FileUtils.writeStringToFile(file, predCode);
            CFGBuilder cfgBuilder = new CFGBuilder(file.getAbsolutePath(), null, false);
            TokenStreamRewriter antlrRewriter = new TokenStreamRewriter(cfgBuilder.parser.getTokenStream());
            orgPredRewriter = new OrgPredRewriter(cfgBuilder, antlrRewriter);
            cfgBuilder.parser.reset();
            walker.walk(orgPredRewriter, cfgBuilder.parser.template());
        }
        File file = File.createTempFile(tempFileName, suffix);
        FileUtils.writeStringToFile(file, transCode);
        CFGBuilder cfgBuilder = new CFGBuilder(file.getAbsolutePath(), null, false);
        TokenStreamRewriter antlrRewriter = new TokenStreamRewriter(cfgBuilder.parser.getTokenStream());
        TargetToPL targetToPL = new TargetToPL(orgPredRewriter, transName);
        cfgBuilder.parser.reset();
        walker.walk(targetToPL, cfgBuilder.parser.template());
        predCode = orgPredRewriter.antlrRewriter.getText();

    }

    public StanTranslator getStanTranslator() throws Exception {
        File file = File.createTempFile(tempFileName, suffix);
        FileUtils.writeStringToFile(file, code);
        StanTranslator stanTranslator = new StanTranslator();
        stanTranslator.translate((new CFGBuilder(file.getAbsolutePath(), null, false)).getSections());
        return stanTranslator;
    }

    public String getStanCode() throws Exception {
        File file = File.createTempFile(tempFileName, suffix);
        FileUtils.writeStringToFile(file, code);
        StanTranslator stanTranslator = new StanTranslator();
        stanTranslator.translate((new CFGBuilder(file.getAbsolutePath(), null, false)).getSections());
        return stanTranslator.getCode();
    }

    public void transformObserveToLoop() throws IOException {
        File file = File.createTempFile(tempFileName, suffix);
        FileUtils.writeStringToFile(file, code);
        CFGBuilder cfgBuilder = new CFGBuilder(file.getAbsolutePath(), null, false);
        TokenStreamRewriter antlrRewriter = new TokenStreamRewriter(cfgBuilder.parser.getTokenStream());
        ObserveToLoop observeToLoop = new ObserveToLoop(cfgBuilder, antlrRewriter);
        cfgBuilder.parser.reset();
        walker.walk(observeToLoop, cfgBuilder.parser.template());
        code = antlrRewriter.getText();
    }

    public void transformSampleToTarget() throws IOException {
        File file = File.createTempFile(tempFileName, suffix);
        FileUtils.writeStringToFile(file, code);
        CFGBuilder cfgBuilder = new CFGBuilder(file.getAbsolutePath(), null, false);
        TokenStreamRewriter antlrRewriter = new TokenStreamRewriter(cfgBuilder.parser.getTokenStream());
        SampleToTarget sampleToTarget = new SampleToTarget(cfgBuilder, antlrRewriter);
        cfgBuilder.parser.reset();
        walker.walk(sampleToTarget, cfgBuilder.parser.template());
        code = antlrRewriter.getText();
    }

    public void transformReweighter() throws IOException {
        File file = File.createTempFile(tempFileName, suffix);
        FileUtils.writeStringToFile(file, code);
        CFGBuilder cfgBuilder = new CFGBuilder(file.getAbsolutePath(), null, false);
        TokenStreamRewriter antlrRewriter = new TokenStreamRewriter(cfgBuilder.parser.getTokenStream());
        Reweighter reweighter = new Reweighter(cfgBuilder, antlrRewriter);
        cfgBuilder.parser.reset();
        walker.walk(reweighter, cfgBuilder.parser.template());
        code = antlrRewriter.getText();
    }

    public Boolean transformLogit() throws IOException {
        File file = File.createTempFile(tempFileName, suffix);
        FileUtils.writeStringToFile(file, code);
        CFGBuilder cfgBuilder = new CFGBuilder(file.getAbsolutePath(), null, false);
        TokenStreamRewriter antlrRewriter = new TokenStreamRewriter(cfgBuilder.parser.getTokenStream());
        Logit logit = new Logit(cfgBuilder, antlrRewriter);
        cfgBuilder.parser.reset();
        walker.walk(logit, cfgBuilder.parser.template());
        code = antlrRewriter.getText();
        return logit.transformed;
    }


    // Return whether next transformation exist
    public Boolean transformLocalizer(int paramToTransform) throws IOException {
        File file = File.createTempFile(tempFileName, suffix);
        FileUtils.writeStringToFile(file, code);
        CFGBuilder cfgBuilder = new CFGBuilder(file.getAbsolutePath(), null, false);
        TokenStreamRewriter antlrRewriter = new TokenStreamRewriter(cfgBuilder.parser.getTokenStream());
        Localizer localizer = new Localizer(cfgBuilder, antlrRewriter, paramToTransform);
        cfgBuilder.parser.reset();
        walker.walk(localizer, cfgBuilder.parser.template());
        code = antlrRewriter.getText();
        return localizer.existNext;
    }

    //Return whether there is a normal distr
    public Boolean transformReparamLocalizer() throws IOException {
        File file = File.createTempFile(tempFileName, suffix);
        FileUtils.writeStringToFile(file, code);
        CFGBuilder cfgBuilder = new CFGBuilder(file.getAbsolutePath(), null, false);
        TokenStreamRewriter antlrRewriter = new TokenStreamRewriter(cfgBuilder.parser.getTokenStream());
        ReparamLocalizer reparamLocalizer = new ReparamLocalizer(cfgBuilder, antlrRewriter);
        cfgBuilder.parser.reset();
        walker.walk(reparamLocalizer, cfgBuilder.parser.template());
        code = antlrRewriter.getText();
        return reparamLocalizer.transformed;
    }

    public Boolean transformConstToParam() throws IOException {
        File file = File.createTempFile(tempFileName, suffix);
        FileUtils.writeStringToFile(file, code);
        CFGBuilder cfgBuilder = new CFGBuilder(file.getAbsolutePath(), null, false);
        TokenStreamRewriter antlrRewriter = new TokenStreamRewriter(cfgBuilder.parser.getTokenStream());
        ConstToParam constToParam = new ConstToParam(cfgBuilder, antlrRewriter);
        cfgBuilder.parser.reset();
        walker.walk(constToParam, cfgBuilder.parser.template());
        code = antlrRewriter.getText();
        return constToParam.transformed;
    }

    public Boolean transformMixNormal() throws IOException {
        File file = File.createTempFile(tempFileName, suffix);
        FileUtils.writeStringToFile(file, code);
        CFGBuilder cfgBuilder = new CFGBuilder(file.getAbsolutePath(), null, false);
        TokenStreamRewriter antlrRewriter = new TokenStreamRewriter(cfgBuilder.parser.getTokenStream());
        MixNormal mixNormal = new MixNormal(cfgBuilder, antlrRewriter);
        cfgBuilder.parser.reset();
        walker.walk(mixNormal, cfgBuilder.parser.template());
        code = antlrRewriter.getText();
        return mixNormal.transformed;
    }

    public Boolean transformNewNormal2T() throws IOException {
        File file = File.createTempFile(tempFileName, suffix);
        FileUtils.writeStringToFile(file, code);
        CFGBuilder cfgBuilder = new CFGBuilder(file.getAbsolutePath(), null, false);
        TokenStreamRewriter antlrRewriter = new TokenStreamRewriter(cfgBuilder.parser.getTokenStream());
        NewNormal2T newNormal2T = new NewNormal2T(cfgBuilder, antlrRewriter);
        cfgBuilder.parser.reset();
        walker.walk(newNormal2T, cfgBuilder.parser.template());
        code = antlrRewriter.getText();
        return newNormal2T.transformed;
    }
}
