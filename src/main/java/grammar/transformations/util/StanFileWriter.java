package grammar.transformations.util;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.sql.SQLOutput;

public class StanFileWriter {

    public void writeStanCode(String stanCode, String newFilePath) {
        System.out.println("====================Writing" + newFilePath);
        try (PrintWriter out = new PrintWriter(newFilePath)) {
            out.println(stanCode);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }


    public void createStanDir(String newDir) {
        System.out.println("====================Create Dir" + newDir);
        new File(newDir).mkdir();
    }

    public void transCodeToDir(TransWriter transWriter, String transName, File oldFilePath) throws Exception {
        String strFilePath = oldFilePath.getAbsolutePath();
        String progName = oldFilePath.getName();
        createStanDir(strFilePath + "_" + transName);
        writeStanCode(transWriter.getStanCode(), strFilePath + "_" + transName + "/" + progName + "_" + transName + ".stan");
        writeStanCode(transWriter.getCode(), strFilePath + "_" + transName + "/" + progName + "_" + transName + ".template");
    }

    public void predCodeToDir(TransWriter transWriter, File oldFilePath) throws Exception {
        String transName = "NPL";
        String strFilePath = oldFilePath.getAbsolutePath().replace("/trans/","/pred/");
        String progName = oldFilePath.getName();
        createStanDir(strFilePath + "_" + transName);
        writeStanCode(transWriter.getStanPredCode(), strFilePath + "_" + transName + "/" + progName + "_" + transName + ".stan");
        writeStanCode(transWriter.getPredCode(), strFilePath + "_" + transName + "/" + progName + "_" + transName + ".template");
    }

    public void tryAllTrans(File filePath) throws Exception {

        String strFilePath = filePath.getAbsolutePath();
        String progName = filePath.getName();

        TransWriter transWriter = new TransWriter(strFilePath+ "/" + progName + ".stan",
                strFilePath + "/" + progName + ".data.R");
        transWriter.transformObserveToLoop();
        transWriter.transformSampleToTarget();
        transWriter.setReuseCode();
        transWriter.transformOrgPredCode();
        Boolean transformed;

        String transName;
        // ConstToParam
        System.out.println("========ConstToParam========");
        transName = "robust_const";
        transWriter.resetCode();
        transformed = transWriter.transformConstToParam();
        if (transformed) {
            transCodeToDir(transWriter, transName, filePath);
            transWriter.addPredCode(transWriter.getCode(), transName);
        }


        // Reweighter
        System.out.println("========Reweighting========");
        transName="robust_reweight";
        transWriter.resetCode();
        transWriter.transformReweighter();
        writeStanCode(transWriter.code, strFilePath + "_" + transName + "/" + progName + "_" + transName + ".stan");
        transWriter.addPredCode(transWriter.getCode(), transName);

        // Localizer
        Boolean existNext = true;
        int paramCount = 0;
        while (existNext){
            System.out.println("========Localizing Param " + paramCount + "========");
            transWriter.resetCode();
            existNext = transWriter.transformLocalizer(paramCount);
            transName = "robust_local" + (1 + paramCount);
            transCodeToDir(transWriter, transName, filePath);
            transWriter.addPredCode(transWriter.getCode(), transName);
            paramCount++;
        }

        // Reparam, Normal2T
        System.out.println("========Reparam:Normal2T========");
        transWriter.resetCode();
        transformed = transWriter.transformReparamLocalizer();
        if (transformed) {
            transName = "robust_reparam";
            transCodeToDir(transWriter, transName, filePath);
            transWriter.addPredCode(transWriter.getCode(), transName);
        }

        // Logit
        System.out.println("========Logit========");
        transWriter.resetCode();
        transformed = transWriter.transformLogit();
        if (transformed) {
            transName = "robust_logit";
            transCodeToDir(transWriter, transName, filePath);
            transWriter.addPredCode(transWriter.getCode(), transName);
        }

        // MixNormal
        System.out.println("========MixNormal========");
        transWriter.resetCode();
        transformed = transWriter.transformMixNormal();
        if (transformed) {
            transName = "robust_mix";
            transCodeToDir(transWriter, transName, filePath);
            transWriter.addPredCode(transWriter.getCode(), transName);
        }

        predCodeToDir(transWriter, filePath);
        System.out.println(transWriter.getStanPredCode());
    }
}
