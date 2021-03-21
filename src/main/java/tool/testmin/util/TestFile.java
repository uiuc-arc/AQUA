package tool.testmin.util;

import javax.json.JsonObject;

public class TestFile {

    private String testfile;
    private String testdir;
    private boolean skip;
    private String datafile;
    private String pps;
    private String ppsfilename;
    private String filename;
    private String algotype;
    private int iterations;
    private String backupfile;
    private String ppsfile;
    private String ppsbackupfile;
    private String databackupfile;

    public TestFile(String testfile, String testdir, boolean skip, String datafile, String pps) {
        this.testfile = testfile;
        this.testdir = testdir;
        this.skip = skip;
        this.datafile = datafile;
        this.pps = pps;
    }

    public TestFile(JsonObject j) {
        if (j.containsKey("file")) {
            this.testfile = j.getString("file");
            this.backupfile = this.testfile.substring(0, this.testfile.lastIndexOf(".")) + ".bak";
        }

        if (j.containsKey("dir")) {
            this.testdir = j.getString("dir");
            this.filename = this.testdir.split("/")[this.testdir.split("/").length - 1];
        }

        if (j.containsKey("data")) {
            this.datafile = j.getString("data");
            this.databackupfile = this.datafile + ".bak";
        }


        if (j.containsKey("skip")) {
            this.skip = j.getBoolean("skip");
        }
        if (j.containsKey("pps")) {
            this.pps = j.getString("pps");
        } else {
            this.pps = "stan";
        }

        if (this.pps.contains("stan")) {
            this.ppsfile = this.testfile.substring(0, this.testfile.lastIndexOf(".")) + ".stan";
            this.ppsfilename = this.ppsfile.split("/")[this.ppsfile.split("/").length - 1];
            this.ppsbackupfile = this.ppsfile + ".original";
        } else if (this.pps.contains("pyro")) {
            this.ppsfile = this.testfile.substring(0, this.testfile.lastIndexOf(".")) + ".py";
            this.ppsfilename = this.ppsfile.split("/")[this.ppsfile.split("/").length - 1];
            this.ppsbackupfile = this.ppsfile + ".original";
        }

        if (j.containsKey("type")) {
            this.algotype = j.getString("type");
        }

        if (j.containsKey("default")) {
            this.iterations = j.getInt("default");
        } else if (j.containsKey("iter")) {
            this.iterations = j.getInt("iter");
        } else {
            this.iterations = 1000;
        }
    }

    public String getPpsfilename() {
        return ppsfilename;
    }

    public String getFilename() {
        return filename;
    }

    public String getBackupfile() {
        return backupfile;
    }

    public String getPpsfile() {
        return ppsfile;
    }

    public String getPpsbackupfile() {
        return ppsbackupfile;
    }

    public String getDatabackupfile() {
        return databackupfile;
    }

    public String getAlgotype() {
        return algotype;
    }

    public int getIterations() {
        return iterations;
    }


    public String getTestfile() {
        return testfile;
    }

    public String getTestdir() {
        return testdir;
    }

    public boolean isSkip() {
        return skip;
    }

    public String getDatafile() {
        return datafile;
    }

    public String getPps() {
        return pps;
    }

}
