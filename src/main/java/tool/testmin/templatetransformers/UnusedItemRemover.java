package tool.testmin.templatetransformers;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import grammar.Template2Parser;
import tool.testmin.TemplateBaseTransformer;
import tool.testmin.util.MyLogger;
import tool.testmin.util.TestFile;
import tool.testmin.util.TMUtil;
import tool.testmin.util.template.TemplateScraper;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

public class UnusedItemRemover extends TemplateBaseTransformer {
    ArrayList<String> ids;
    boolean modifiedTree;
    boolean inDataBlock;
    String datafile;
    boolean dataChanged;

    public UnusedItemRemover(String testfile, String datafile, ArrayList<String> ids, MyLogger logger) {
        super(TMUtil.getTemplateParser(testfile), logger);
        this.ids = ids;
        this.datafile = datafile;
    }

    private static ArrayList<String> getHitList(String filecontent, MyLogger logger) {

        ArrayList<ParserRuleContext> idaccess = TemplateScraper.scrape(TMUtil.getTemplateParserFromString(filecontent), "ref", null);
        ArrayList<ParserRuleContext> arrayAccess = TemplateScraper.scrape(TMUtil.getTemplateParserFromString(filecontent), "array_access", null);
        ArrayList<ParserRuleContext> assigns = TemplateScraper.scrape(TMUtil.getTemplateParserFromString(filecontent), "assign", "");


        ArrayList<ParserRuleContext> accesses = new ArrayList<>();
        accesses.addAll(idaccess);
        accesses.addAll(arrayAccess);
        accesses.addAll(assigns);


        ArrayList<ParserRuleContext> declarations = TemplateScraper.scrape(TMUtil.getTemplateParserFromString(filecontent), "decl", "");
        ArrayList<ParserRuleContext> datadeclaration = TemplateScraper.scrape(TMUtil.getTemplateParserFromString(filecontent), "data", "");
        ArrayList<ParserRuleContext> prior = TemplateScraper.scrape(TMUtil.getTemplateParserFromString(filecontent), "prior", "");


        ArrayList<ParserRuleContext> defs = new ArrayList<>();
        defs.addAll(declarations);
        defs.addAll(datadeclaration);
        defs.addAll(prior);


        ArrayList<String> hitList = new ArrayList<>();

        for (ParserRuleContext rule : defs) {
            String curId = null;
            if (rule instanceof Template2Parser.DeclContext) {
                curId = ((Template2Parser.DeclContext) rule).ID().getText();
            } else if (rule instanceof Template2Parser.DataContext) {
                curId = ((Template2Parser.DataContext) rule).ID().getText();
            } else if (rule instanceof Template2Parser.PriorContext) {
                curId = TMUtil.getParamName(rule);
            } else if (rule instanceof Template2Parser.AssignContext) {
                if (((Template2Parser.AssignContext) rule).ID() != null)
                    curId = ((Template2Parser.AssignContext) rule).ID().getText();
                else
                    curId = ((Template2Parser.Array_accessContext) ((Template2Parser.AssignContext) rule).expr(0)).ID().getText();
            }

            boolean used = false;

            for (ParserRuleContext access : accesses) {
                if (access instanceof Template2Parser.RefContext) {
                    Template2Parser.RefContext id_accessContext = (Template2Parser.RefContext) access;
                    if (id_accessContext.getParent() instanceof Template2Parser.PriorContext) {
                        Template2Parser.PriorContext pctx = (Template2Parser.PriorContext) id_accessContext.getParent();
                        if (pctx.distexpr().getText().contains("1234.0")) {
                            logger.print("Ignoring ref for prior: " + pctx.getText(), true);
                            continue;
                        }
                    }

                    if (id_accessContext.ID().getText().equals(curId)) {
                        used = true;
                        break;
                    }
                } else if (access instanceof Template2Parser.Array_accessContext) {
                    Template2Parser.Array_accessContext array_accessContext = (Template2Parser.Array_accessContext) access;
                    if (array_accessContext.getParent() instanceof Template2Parser.PriorContext) {
                        Template2Parser.PriorContext pctx = (Template2Parser.PriorContext) array_accessContext.getParent();
                        if (pctx.distexpr().getText().contains("1234.0")) {
                            logger.print("Ignoring arr for prior: " + pctx.getText(), true);
                            continue;
                        }
                    }
                    if (array_accessContext.ID().getText().equals(curId)) {
                        used = true;
                        break;
                    }
                } else if (access instanceof Template2Parser.AssignContext) {
                    Template2Parser.AssignContext assignContext = (Template2Parser.AssignContext) access;
                    if (assignContext.ID() != null) {
                        if (assignContext.ID().getText().equals(curId)) {
                            used = true;
                            break;
                        }
                    } else if (assignContext.expr(0) instanceof Template2Parser.Array_accessContext) {
                        if (((Template2Parser.Array_accessContext) assignContext.expr(0)).ID().getText().equals(curId)) {
                            used = true;
                            break;
                        }
                    } else {
                        System.out.println("Unknown assign type :" + assignContext.getText());
                    }
                }
            }

            if (!used)
                hitList.add(curId);
        }

        return hitList;
    }

    public static boolean Transform(String testfile, String datafile, MyLogger logger, String testdir, TestFile testFile) {
        boolean changed = false;

        try {
            String filecontent = new String(Files.readAllBytes(Paths.get(testfile)));
            String datacontent = null;
            if (datafile != null && !datafile.isEmpty()) {
                datacontent = new String(Files.readAllBytes(Paths.get(datafile)));
            }

            ArrayList<String> hitList = getHitList(filecontent, logger);
            logger.print("HitList:  " + hitList.size(), true);

            for (int l = hitList.size() - 1; l >= 0; ) {
                ArrayList<String> curHitList = new ArrayList<>();
                curHitList.add(hitList.get(l));
                UnusedItemRemover unusedItemRemover = new UnusedItemRemover(testfile, datafile, curHitList, logger);
                ParseTreeWalker walker = new ParseTreeWalker();
                walker.walk(unusedItemRemover, unusedItemRemover.parser.template());
                String newContent = unusedItemRemover.rewriter.getText();
                if (unusedItemRemover.modifiedTree &&
                        TMUtil.runTransformedCode(filecontent,
                                newContent,
                                TMUtil.TemplateTransformationMap.inverse().get(UnusedItemRemover.class),
                                logger,
                                testFile,
                                unusedItemRemover.dataChanged)) {
                    filecontent = newContent;
                    if (datafile != null && !datafile.isEmpty()) {
                        datacontent = new String(Files.readAllBytes(Paths.get(datafile)));
                    }


                    hitList = getHitList(filecontent, logger);
                    l = hitList.size() - 1;
                    changed = true;
                    logger.print("Changed", true);
                } else {

                    if (datafile != null && !datafile.isEmpty()) {
                        FileWriter fw = new FileWriter(datafile);
                        fw.write(datacontent);
                        fw.close();
                    }

                    l--;
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return changed;
    }

    @Override
    public void exitDecl(Template2Parser.DeclContext ctx) {
        if (ids.contains(ctx.ID().getText())) {
            print("Removing declaration for ID : " + ctx.ID().getText());

            this.rewriter.delete(ctx.getStart(), ctx.getStop());
            modifiedTree = true;


        }
    }

    @Override
    public void exitData(Template2Parser.DataContext ctx) {
        if (ids.contains(ctx.ID().getText())) {
            print("Removing data declaration for ID : " + ctx.ID().getText());
            dataChanged = true;
            this.rewriter.delete(ctx.getStart(), ctx.getStop());
            modifiedTree = true;
        }
    }

    @Override
    public void exitPrior(Template2Parser.PriorContext ctx) {
        if (ids.contains(TMUtil.getParamName(ctx))) {
            print("Removing prior declaration for ID : " + TMUtil.getParamName(ctx));

            this.rewriter.delete(ctx.getStart(), ctx.getStop());
            modifiedTree = true;
        }
    }

    @Override
    public void exitAssign(Template2Parser.AssignContext ctx) {


    }
}
