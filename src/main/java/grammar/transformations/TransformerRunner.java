package grammar.transformations;

import grammar.transformations.util.StanFileWriter;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.util.ArrayList;

public class TransformerRunner {
    public static void main(String[] args) throws IOException {
        // BufferedReader in = new BufferedReader(new FileReader("autotemp.config"));
        // String ll;
        String inputOrgDir = null;
        String inputTransDir = null;
        // while ((ll = in.readLine()) != null) {
        //     if (ll.contains("input_org_dir"))
        //         inputOrgDir = ll.split("=")[1].split(" ")[0];
        //     else if (ll.contains("input_trans_dir"))
        //         inputTransDir = ll.split("=")[1].split(" ")[0];
        // }
        // in.close();
        inputOrgDir = "/home/zixin/Documents/are/aura_package/autotemp/timeseries_org";
        inputTransDir = "/home/zixin/Documents/are/aura_package/autotemp/custom_trans";

        System.out.println(inputOrgDir);
        System.out.println(inputTransDir);

        File folder = new File(inputOrgDir + "/");
        File[] listOfFiles = folder.listFiles();
        StanFileWriter stanFileWriter = new StanFileWriter();
        String targetOrgDir = inputTransDir + "/";
        //TODO: check sshfs mount; before run ./patch_vector.sh; after finish run ./patch_simplex.sh

        int i = 0;
        ArrayList<String> restFiles=new ArrayList<>();
        for (File orgProgDir : listOfFiles) {
            if (orgProgDir.isDirectory()) {
                // if (!orgProgDir.getName().contains("radon.pooling"))
                //         continue;
                // if (orgProgDir.getName().contains("gp-fit-ARD") || orgProgDir.getName().contains("mix"))
                //    continue;
                try {
                    File newDir = new File(targetOrgDir + orgProgDir.getName());
                    newDir.mkdir();
                    System.out.println(orgProgDir.getAbsolutePath());
                    for (File orgProgDirFile : orgProgDir.listFiles())
                        if (orgProgDirFile.getName().equals(orgProgDir.getName() + ".stan")) {
                            FileUtils.copyFileToDirectory(orgProgDirFile.getAbsoluteFile(), newDir);
                        } else if (orgProgDirFile.getName().equals(orgProgDir.getName() + ".data.R")) {
                            FileUtils.copyFileToDirectory(orgProgDirFile, newDir);
                        }
                    // else if (orgProgDirFile.getName().equals("gen_data.R")) {
                    //         FileUtils.copyFileToDirectory(orgProgDirFile, newDir);
                    //     }
                    stanFileWriter.tryAllTrans(newDir);
                } catch (Exception e) {
                    restFiles.add(orgProgDir.getName());
                    e.printStackTrace();
                }
            }
            i++;
            if (i>1)
                break;
        }
        System.out.println("=====================");
        System.out.println("Successfully Transformed " + i + " Files!");
        System.out.println("Files Failed: " + restFiles);
        System.out.println("=====================");

    }
}
