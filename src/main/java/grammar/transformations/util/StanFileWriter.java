package grammar.transformations.util;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.List;

public class StanFileWriter {
    List<String> orderedOrgList;

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
        String stancode = transWriter.getStanCode();
        if(!orderedOrgList.isEmpty()){
            for (String ll:orderedOrgList)
                stancode = stancode.replace(ll.replaceAll("\\s*[a-zA-Z0-9_]*ordered|\\s*[a-zA-Z0-9_]*simplex","vector"), ll);

        }
        writeStanCode(stancode, strFilePath + "_" + transName + "/" + progName + "_" + transName + ".stan");
        writeStanCode(transWriter.getCode(), strFilePath + "_" + transName + "/" + progName + "_" + transName + ".template");
    }

    public void genStanCodeToDir(String genStanCode, File oldFilePath) throws Exception {
        String strFilePath = oldFilePath.getAbsolutePath();
        String progName = oldFilePath.getName();
        writeStanCode(genStanCode, strFilePath + "/" + progName + ".genquant");
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
        List<String> contents = Files.readAllLines(Paths.get(strFilePath+ "/" + progName + ".stan"), StandardCharsets.US_ASCII);
        orderedOrgList = new ArrayList<>();
        FileWriter newCopyOrg = new FileWriter(strFilePath + "/" + progName + "_copy.stan");
        for (String ll:contents) {
            if (ll.contains("ordered") || ll.contains("simplex")) {
                orderedOrgList.add(ll);
            }
            newCopyOrg.write(ll.replaceAll("\\b[a-zA-Z0-9_]*ordered|\\s*[a-zA-Z0-9_]*simplex","vector") + "\n");
        }
        newCopyOrg.close();

        TransWriter transWriter;
        if (orderedOrgList.isEmpty())
            transWriter = new TransWriter(strFilePath+ "/" + progName + ".stan",
                strFilePath + "/" + progName + ".data.R");
        else {
            transWriter = new TransWriter(strFilePath + "/" + progName + "_copy.stan",
                    strFilePath + "/" + progName + ".data.R");
        }

        try {
            transWriter.transformObserveToLoop();
        } catch (Exception e) {
            e.printStackTrace();
        }


        try {
            genStanCodeToDir(transWriter.getStanGenCode(strFilePath + "/" + progName), filePath);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // if (true)
        //     return;
        try {
            transWriter.transformSampleToTarget();
        } catch (Exception e) {
            e.printStackTrace();
        }

        transWriter.setReuseCode();
        writeStanCode(transWriter.getCode(), strFilePath + "/" + progName + ".template");
        // try {
        //     transWriter.transformOrgPredCode();
        // } catch (Exception e){
        //     e.printStackTrace();

        // }
        Boolean transformed=false;

        String transName;
        // ConstToParam
        // try {
        //     System.out.println("========ConstToParam========");
        //     transName = "robust_const";
        //     transWriter.resetCode();
        //     transformed = transWriter.transformConstToParam();
        //     if (transformed) {
        //         transCodeToDir(transWriter, transName, filePath);
        //         transWriter.addPredCode(transWriter.getCode(), transName);
        //     }
        // } catch (Exception e) {
        //     e.printStackTrace();
        // }


        // Reweighter
        try {
            System.out.println("========Reweighting========");
            transName = "robust_reweight";
            transWriter.resetCode();
            transWriter.transformReweighter();
            transCodeToDir(transWriter, transName, filePath);
            // transWriter.addPredCode(transWriter.getCode(), transName);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Localizer
        Boolean existNext = true;
        int paramCount = 0;
        try {
            while (existNext){
                if (paramCount > 2)
                    break;
                System.out.println("========Localizing Param " + paramCount + "========");
                transWriter.resetCode();
                existNext = transWriter.transformLocalizer(paramCount);
                System.out.println("==============================Exist Next: " + existNext );
                transName = "robust_local" + (1 + paramCount);
                transCodeToDir(transWriter, transName, filePath);
                try {
                    // transWriter.addPredCode(transWriter.getCode(), transName);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                paramCount++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Reparam, Normal2T
        try {
            System.out.println("========Reparam:Normal2T========");
            transWriter.resetCode();
            transformed = transWriter.transformReparamLocalizer();
            if (transformed) {
                transName = "robust_reparam";
                transCodeToDir(transWriter, transName, filePath);
                // transWriter.addPredCode(transWriter.getCode(), transName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Logit
        // try {
        //     System.out.println("========Logit========");
        //     transWriter.resetCode();
        //     transformed = transWriter.transformLogit();
        //     if (transformed) {
        //         transName = "robust_logit";
        //         transCodeToDir(transWriter, transName, filePath);
        //         transWriter.addPredCode(transWriter.getCode(), transName);
        //     }
        // } catch (Exception e) {
        //     e.printStackTrace();
        // }

        try {
            // MixNormal
            System.out.println("========MixNormal========");
            transWriter.resetCode();
            transformed = transWriter.transformMixNormal();
            if (transformed) {
                transName = "robust_mix";
                transCodeToDir(transWriter, transName, filePath);
                // transWriter.addPredCode(transWriter.getCode(), transName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            // NewNormal2T
            System.out.println("========NewNormal2T========");
            transWriter.resetCode();
            transformed = transWriter.transformNewNormal2T();
            if (transformed) {
                transName = "robust_student";
                transCodeToDir(transWriter, transName, filePath);
                // transWriter.addPredCode(transWriter.getCode(), transName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // try {
        //     predCodeToDir(transWriter, filePath);
        // } catch (Exception e) {
        //     e.printStackTrace();
        // }
        File stancopy = new File(strFilePath + "/" + progName + "_copy.stan");
        boolean success = stancopy.delete();
    }
}
