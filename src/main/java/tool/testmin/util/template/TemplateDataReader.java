package tool.testmin.util.template;

import org.antlr.v4.runtime.tree.ParseTreeWalker;
import grammar.Template2Parser;
import tool.testmin.TemplateBaseTransformer;
import tool.testmin.util.MyLogger;
import tool.testmin.util.TMUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class TemplateDataReader extends TemplateBaseTransformer {

    public ArrayList<String> sequence;
    public Map<String, Data> map = new HashMap<>();
    public Map<String, ArrayList<String>> indexMap = new HashMap<>();
    public ArrayList<Data> array1d = new ArrayList<>();
    public ArrayList<Data> array2d = new ArrayList<>();
    public ArrayList<Data> ints = new ArrayList<>();
    public ArrayList<Data> doubles = new ArrayList<>();
    private String testfile;


    public TemplateDataReader(String testfile, MyLogger logger) {
        super(TMUtil.getTemplateParser(testfile), logger);
        this.testfile = testfile;
        this.sequence = new ArrayList<>();
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(this, this.parser.template());
    }

    public int getDouble1dCount() {
        int c = 0;
        for (Data d : this.array1d) {
            if (d.dataDouble1d != null) {
                c++;
            }
        }
        return c;
    }

    public Data getNDouble1d(int n) {
        int c = 0;

        for (Data d : this.array1d) {
            if (d.dataDouble1d != null)
                c++;
            if (c == n)
                return d;
        }

        return null;
    }

    public int getInt1dCount() {
        int c = 0;
        for (Data d : this.array1d) {
            if (d.dataInt1d != null) {
                c++;
            }
        }
        return c;
    }


    @Override
    public void enterData(Template2Parser.DataContext ctx) {
        Data newData = new Data();
        newData.ID = ctx.ID().getText();
        newData.indices = new ArrayList<>();
        if (ctx.getText().contains("[[[")) {
            print("Not reading 3d " + ctx.ID().getText());
            return;
        }

        if (ctx.array() != null) {

            if (ctx.array().expr() == null || ctx.array().expr().size() == 0) {
                if (ctx.array().getText().contains(".")) {
                    newData.dataDouble2d = ctx.array();
                    newData.type = Data.Type.Array2d;
                } else {
                    newData.dataInt2d = ctx.array();
                    newData.type = Data.Type.Array2d;
                }
                String prevKey1 = ctx.dims().expr(0).getText();
                String prevKey2 = ctx.dims().expr(1).getText();
                newData.indices.add(prevKey1);
                newData.indices.add(prevKey2);
                if (!indexMap.containsKey(prevKey1)) {
                    indexMap.put(prevKey1, new ArrayList<String>());
                }

                indexMap.get(prevKey1).add(newData.ID);

                if (!indexMap.containsKey(prevKey2)) {
                    indexMap.put(prevKey2, new ArrayList<String>());
                }

                array2d.add(newData);

            } else {
                if (ctx.array().getText().contains(".")) {
                    newData.dataDouble1d = ctx.array();
                    newData.type = Data.Type.Array;
                } else {
                    newData.dataInt1d = ctx.array();
                    newData.type = Data.Type.Array;
                }

                String prevKey2 = ctx.dims().expr(0).getText();
                newData.indices.add(prevKey2);
                if (!indexMap.containsKey(prevKey2)) {
                    indexMap.put(prevKey2, new ArrayList<String>());
                }
                indexMap.get(prevKey2).add(newData.ID);
                this.array1d.add(newData);
            }

        } else if (ctx.vector() != null) {
            if (ctx.vector().expr() == null || ctx.vector().expr().size() == 0) {
                if (ctx.vector().getText().contains(".")) {
                    newData.dataDouble2d = ctx.vector();
                    newData.type = Data.Type.Array2d;
                } else {
                    newData.dataInt2d = ctx.vector();
                    newData.type = Data.Type.Array2d;
                }
                String prevKey1 = ctx.dims().expr(0).getText();
                String prevKey2 = ctx.dims().expr(1).getText();
                if (!indexMap.containsKey(prevKey1)) {
                    indexMap.put(prevKey1, new ArrayList<String>());
                }

                indexMap.get(prevKey1).add(newData.ID);

                if (!indexMap.containsKey(prevKey2)) {
                    indexMap.put(prevKey2, new ArrayList<String>());
                }
                newData.indices.add(prevKey1);
                newData.indices.add(prevKey2);

                array2d.add(newData);
            } else {
                if (ctx.vector().getText().contains(".")) {
                    newData.dataDouble1d = ctx.vector();
                    newData.type = Data.Type.Array;
                } else {
                    newData.dataInt1d = ctx.vector();
                    newData.type = Data.Type.Array;
                }

                String prevKey2 = ctx.dims().expr(0).getText();
                newData.indices.add(prevKey2);
                if (!indexMap.containsKey(prevKey2)) {
                    indexMap.put(prevKey2, new ArrayList<String>());
                }
                indexMap.get(prevKey2).add(newData.ID);
                this.array1d.add(newData);
            }

        } else if (ctx.expr() != null) {

            if (ctx.expr() instanceof Template2Parser.ValContext) {
                if (((Template2Parser.ValContext) ctx.expr()).number().INT() != null) {
                    newData.dataInt = ctx.expr();
                    newData.type = Data.Type.Int;
                    if (!map.containsKey(newData.ID)) {
                        ints.add(newData);
                    }


                } else {
                    newData.dataDouble = ctx.expr();
                    newData.type = Data.Type.Double;
                    if (!map.containsKey(newData.ID)) {
                        doubles.add(newData);
                    }
                }
            }
        }

        if (!map.containsKey(newData.ID)) {
            map.put(newData.ID, newData);
        }
        sequence.add(newData.ID);
    }
}
