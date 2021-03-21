package tool.testmin.templatetransformers;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import grammar.Template2Parser;
import tool.testmin.TemplateBaseTransformer;
import tool.testmin.util.MyLogger;
import tool.testmin.util.TestFile;
import tool.testmin.util.TMUtil;
import tool.testmin.util.template.Data;
import tool.testmin.util.template.TemplateDataReader;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DataTransformer extends TemplateBaseTransformer {
    ParseTreeProperty<String> values;
    private TemplateDataReader dataReader;
    private MyLogger logger;
    private List<String> marked;

    public DataTransformer(String testfile, MyLogger logger) {
        super(TMUtil.getTemplateParser(testfile), logger);
        marked = new ArrayList<>();
        values = new ParseTreeProperty<String>();
        print("Reducing data : " + testfile);
        dataReader = new TemplateDataReader(testfile, logger);

        this.logger = logger;
    }

    public static boolean Transform(String testfile, boolean detmode, MyLogger logger, String testdir, TestFile testFile) {
        boolean changed = false;
        if (detmode) {
            try {
                String filecontent = new String(Files.readAllBytes(Paths.get(testfile)));
                DataTransformer dataTransformer = new DataTransformer(testfile, logger);
                int dataitems = dataTransformer.dataReader.array1d.size() + dataTransformer.dataReader.array2d.size();
                boolean first = true;
                for (int l = 0; l < dataitems; ) {
                    dataTransformer = new DataTransformer(testfile, logger);
                    boolean reduced = dataTransformer.reduce_det(l + 1, first);

                    ParseTreeWalker walker = new ParseTreeWalker();
                    walker.walk(dataTransformer, dataTransformer.parser.template());

                    String newContent = dataTransformer.rewriter.getText();
                    if (reduced &&
                            TMUtil.runTransformedCode(filecontent,
                                    newContent,
                                    TMUtil.TemplateTransformationMap.inverse().get(DataTransformer.class),
                                    logger,
                                    testFile)) {
                        changed = true;
                        filecontent = newContent;
                        logger.print("Changed", true);
                        first = true;
                    } else {
                        if (first) {
                            first = false;
                        } else {
                            first = true;
                            l++;
                        }
                    }
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return changed;
    }

    @Override
    public void exitData(Template2Parser.DataContext ctx) {
        String id = ctx.ID().getText();
        if (!this.marked.contains(id)) {
            print("Skipping :" + id);
            return;
        }

        if (ctx.getText().contains("[[[")) {
            print("Skipping 3d " + ctx.ID().getText());
            return;
        }


        Data data = this.dataReader.map.get(id);
        if (data.type == Data.Type.Int) {
            this.rewriter.replace(ctx.expr().getStart(), ctx.expr().getStop(), Integer.toString(data.dataIntVal));
        } else if (data.type == Data.Type.Double) {
            this.rewriter.replace(ctx.expr().getStart(), ctx.expr().getStop(), Double.toString(data.dataDoubleVal));
            this.rewriter.replace(ctx.expr().getStart(), ctx.expr().getStop(), Double.toString(data.dataDoubleVal));
        } else if (data.type == Data.Type.Array) {
            Token start, end;
            if (ctx.vector() != null) {
                start = ctx.vector().expr(0).getStart();
                end = ctx.vector().expr(ctx.vector().expr().size() - 1).getStop();
            } else {
                start = ctx.array().expr(0).getStart();
                end = ctx.array().expr(ctx.array().expr().size() - 1).getStop();
            }
            if (data.dataInt1d != null) {
                this.rewriter.replace(start, end, values.get(data.dataInt1d));
            } else {
                this.rewriter.replace(start, end, values.get(data.dataDouble1d));
            }
        } else if (data.type == Data.Type.Array2d) {
            Token start, end;
            if (ctx.vector() != null) {
                if (ctx.vector().vector() != null && ctx.vector().vector().size() > 0) {
                    start = ctx.vector().vector(0).getStart();
                    end = ctx.vector().vector(ctx.vector().vector().size() - 1).getStop();
                } else {
                    start = ctx.vector().array(0).getStart();
                    end = ctx.vector().vector(ctx.vector().array().size() - 1).getStop();
                }
            } else {
                if (ctx.array().vector() != null && ctx.array().vector().size() > 0) {
                    start = ctx.array().vector(0).getStart();
                    end = ctx.array().vector(ctx.array().vector().size() - 1).getStop();
                } else {
                    start = ctx.array().array(0).getStart();
                    end = ctx.array().array(ctx.array().array().size() - 1).getStop();
                }
            }
            if (data.dataInt2d != null) {
                this.rewriter.replace(start, end, values.get(data.dataInt2d));
            } else {
                this.rewriter.replace(start, end, values.get(data.dataDouble2d));

            }
        } else {
            print("Unsupported data type: " + id);
        }
    }

    private void reduceData(ParserRuleContext node, int index1, int index2, boolean unidimensional, boolean twodim, Data.DataType type) {
        if (unidimensional) {
            if (node instanceof Template2Parser.VectorContext) {
                List<Template2Parser.ExprContext> sublist = ((Template2Parser.VectorContext) node).expr().subList(index1, index2);
                String txt = "";
                for (Template2Parser.ExprContext e : sublist) {
                    if (type == Data.DataType.DoubleType && !e.getText().contains(".")) {
                        txt += e.getText() + ".0,";
                    } else {
                        txt += e.getText() + ",";
                    }
                }
                txt = txt.substring(0, txt.length() - 1);
                values.put(node, txt);
            } else if (node instanceof Template2Parser.ArrayContext) {
                List<Template2Parser.ExprContext> sublist = ((Template2Parser.ArrayContext) node).expr().subList(index1, index2);
                String txt = "";
                for (Template2Parser.ExprContext e : sublist) {
                    if (type == Data.DataType.DoubleType && !e.getText().contains(".")) {
                        txt += e.getText() + ".0,";
                    } else {
                        txt += e.getText() + ",";
                    }
                }
                txt = txt.substring(0, txt.length() - 1);
                values.put(node, txt);
            }
        } else {
            String txt = "";
            if (node instanceof Template2Parser.VectorContext) {
                if (((Template2Parser.VectorContext) node).vector() != null && ((Template2Parser.VectorContext) node).vector().size() > 0) {
                    List<Template2Parser.VectorContext> sublist = ((Template2Parser.VectorContext) node).vector().subList(index1, index2);
                    for (Template2Parser.VectorContext v : sublist) {
                        if (twodim) {
                            reduceData(v, index1, index2, true, false, type);
                            txt += "<" + this.values.get(v) + ">,";
                        } else {
                            txt += "<";
                            for (Template2Parser.ExprContext e : v.expr()) {
                                txt += e.getText() + ",";
                            }
                            txt = txt.substring(0, txt.length() - 1) + ">,";
                        }
                    }
                } else if (((Template2Parser.VectorContext) node).array() != null && ((Template2Parser.VectorContext) node).array().size() > 0) {
                    List<Template2Parser.ArrayContext> sublist = ((Template2Parser.VectorContext) node).array().subList(index1, index2);
                    for (Template2Parser.ArrayContext a : sublist) {
                        if (twodim) {
                            reduceData(a, index1, index2, true, false, type);
                            txt += "[" + this.values.get(a) + "],";
                        } else {
                            txt += "[";
                            for (Template2Parser.ExprContext e : a.expr()) {
                                txt += e.getText() + ",";
                            }
                            txt = txt.substring(0, txt.length() - 1) + "],";
                        }
                    }
                }
            } else if (node instanceof Template2Parser.ArrayContext) {
                if (((Template2Parser.ArrayContext) node).vector() != null && ((Template2Parser.ArrayContext) node).vector().size() > 0) {
                    List<Template2Parser.VectorContext> sublist = ((Template2Parser.ArrayContext) node).vector().subList(index1, index2);
                    for (Template2Parser.VectorContext v : sublist) {
                        if (twodim) {
                            reduceData(v, index1, index2, true, false, type);
                            txt += "<" + this.values.get(v) + ">,";
                        } else {
                            txt += "<";
                            for (Template2Parser.ExprContext e : v.expr()) {
                                txt += e.getText() + ",";
                            }
                            txt = txt.substring(0, txt.length() - 1) + ">,";
                        }
                    }
                } else if (((Template2Parser.ArrayContext) node).array() != null && ((Template2Parser.ArrayContext) node).array().size() > 0) {
                    List<Template2Parser.ArrayContext> sublist = ((Template2Parser.ArrayContext) node).array().subList(index1, index2);
                    for (Template2Parser.ArrayContext a : sublist) {
                        if (twodim) {
                            reduceData(a, index1, index2, true, false, type);
                            txt += "[" + this.values.get(a) + "],";
                        } else {
                            txt += "[";
                            for (Template2Parser.ExprContext e : a.expr()) {
                                txt += e.getText() + ",";
                            }
                            txt = txt.substring(0, txt.length() - 1) + "],";
                        }
                    }
                }
            }
            txt = txt.substring(0, txt.length() - 1);
            values.put(node, txt);
        }
    }

    private void reduceArray(TemplateDataReader dataReader, String indexKey, Data index, int index1, int index2) {
        for (String arr : dataReader.indexMap.get(indexKey)) {

            Data data = dataReader.map.get(arr);

            if (data.type == Data.Type.Array) {
                if (data.dataInt1d != null) {
                    this.reduceData(data.dataInt1d, index1, index2, true, false, Data.DataType.IntType);
                } else {
                    this.reduceData(data.dataDouble1d, index1, index2, true, false, Data.DataType.DoubleType);
                }
            } else if (data.type == Data.Type.Array2d) {
                if (data.indices.get(0) == data.indices.get(1)) {

                    if (data.dataInt2d != null) {
                        this.reduceData(data.dataInt2d, index1, index2, false, true, Data.DataType.IntType);
                    } else {
                        this.reduceData(data.dataDouble2d, index1, index2, false, true, Data.DataType.DoubleType);
                    }
                } else {
                    if (data.dataInt2d != null) {
                        this.reduceData(data.dataInt2d, index1, index2, false, false, Data.DataType.IntType);

                    } else {
                        this.reduceData(data.dataDouble2d, index1, index2, false, false, Data.DataType.DoubleType);
                    }
                }
            }

            print("Reducing array : " + data.ID);
            this.marked.add(data.ID);
        }

        Integer finalIndex = index2 - index1;
        index.dataIntVal = finalIndex;
        this.marked.add(index.ID);
        print("Index value : " + index.dataIntVal);
    }

    public boolean reduce_det(int counter, boolean first) {
        return reduce_det(counter, first, 2);
    }

    public boolean reduce_det(int counter, boolean first, int split) {

        String indexKey;
        if (dataReader.array1d.size() >= counter && counter > 0) {
            Data randData = dataReader.array1d.get(counter - 1);
            indexKey = randData.indices.get(0);
            print(randData.ID);
        } else if (dataReader.array2d.size() >= (counter - dataReader.array1d.size()) && counter > dataReader.array1d.size()) {
            Data randData = dataReader.array2d.get(counter - dataReader.array1d.size() - 1);
            indexKey = randData.indices.get(0);
            print(randData.ID);
        } else {
            print("No suitable data found. Cannot reduce");
            return false;
        }


        Data index = dataReader.map.get(indexKey);
        print(indexKey);

        if (index == null) {
            print("Index id is null for key: " + indexKey + " ...quiting...");
            return false;
        }

        print("Choosing index " + index.ID);
        Integer indexInt = Integer.parseInt(index.dataInt.getText());
        if (indexInt <= 1) {
            print("Index too small ... quitting");
            return false;
        }


        if (split == 0) {
            int i = 1;
            int newsplit = 2;
            while (i <= 5 && new Random().nextBoolean() && newsplit * 2 < indexInt) {
                newsplit = newsplit * 2;
            }
            split = newsplit;
        }

        int index1 = 0;
        int index2;

        if (first) {
            index2 = (indexInt + 1) / split;
            print("Choosing first half of data : " + split);
        } else {
            print("Choosing second half of data: " + split);
            index1 = indexInt - indexInt / split;
            index2 = indexInt;
        }
        print(Integer.toString(index1));
        print(Integer.toString(index2));

        reduceArray(dataReader, indexKey, index, index1, index2);

        return true;
    }


}