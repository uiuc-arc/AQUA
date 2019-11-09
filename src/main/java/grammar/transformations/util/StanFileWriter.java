package grammar.transformations.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;

public class StanFileWriter {

    public void writeStanCode(String stanCode, String newFilePath) {
        try (PrintWriter out = new PrintWriter(newFilePath)) {
            out.println(stanCode);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }

    public void tryAllTrans(String filePath) throws Exception {

        TransWriter transWriter = new TransWriter(filePath,
                filePath.replace(".stan", ".data.R"));
        transWriter.transformObserveToLoop();
        transWriter.transformSampleToTarget();
        transWriter.setReuseCode();
        transWriter.transformOrgPredCode();
        Boolean transformed;

        // ConstToParam
        System.out.println("========ConstToParam========");
        transWriter.resetCode();
        transformed = transWriter.transformConstToParam();
        if (transformed)
            transWriter.addPredCode(transWriter.getCode(), "robust_const");


        // Reweighter
        System.out.println("========Reweighting========");
        transWriter.resetCode();
        transWriter.transformReweighter();
        transWriter.addPredCode(transWriter.getCode(), "robust_reweight");

        // Localizer
        Boolean existNext = true;
        int paramCount = 0;
        while (existNext){
            System.out.println("========Localizing Param " + paramCount + "========");
            transWriter.resetCode();
            existNext = transWriter.transformLocalizer(paramCount);
            transWriter.addPredCode(transWriter.getCode(), "robust_local" + (1 + paramCount));
            paramCount++;
        }

        // Reparam, Normal2T
        System.out.println("========Reparam:Normal2T========");
        transWriter.resetCode();
        transformed = transWriter.transformReparamLocalizer();
        if (transformed)
            transWriter.addPredCode(transWriter.getCode(), "robust_reparam");

        // Logit
        System.out.println("========Logit========");
        transWriter.resetCode();
        transformed = transWriter.transformLogit();
        if (transformed)
            transWriter.addPredCode(transWriter.getCode(), "robust_logit");

        // MixNormal
        System.out.println("========MixNormal========");
        transWriter.resetCode();
        transformed = transWriter.transformMixNormal();
        if (transformed)
            transWriter.addPredCode(transWriter.getCode(), "robust_mix");

        System.out.println(transWriter.getPredCode());
        System.out.println(transWriter.getStanPredCode());
    }
}
