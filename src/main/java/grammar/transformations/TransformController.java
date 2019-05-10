package grammar.transformations;

import grammar.cfg.Section;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;

public class TransformController {
    private ArrayList<Section> sections;
    private Stack<BaseTransformer> undoStack = new Stack<>();
    private Stack<BaseTransformer> redoStack = new Stack<>();
    private Normal2T normal2TAnalysis = new Normal2T();
    private Queue<BaseTransformer> queuedTransformers;

    public TransformController(ArrayList<Section> newSections) {
        sections = newSections;
        queuedTransformers = new LinkedList<>();
    }


    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    public void analyze() throws Exception {
        normal2TAnalysis.availTransformers(sections, queuedTransformers);
    }

    public void transform() throws Exception {
        BaseTransformer newTransformer = queuedTransformers.poll();
        while (newTransformer != null) {
            transform_one(newTransformer);
            newTransformer = queuedTransformers.poll();
        }

    }


    public void transform_one(final BaseTransformer transformer) throws Exception {
        if (!transformer.isTransformed()) {
            transformer.transform();
        }
        redoStack.clear(); // disable undo after new transformation
        System.out.println("Add to Undo Stack");
        undoStack.push(transformer);
    }

    public void undo() throws Exception {
        if (!canUndo()) {
            throw new IllegalStateException("Empty undo stack");
        }
        BaseTransformer baseTransformer = undoStack.pop();
        redoStack.push(baseTransformer);
        baseTransformer.undo();
    }

    public void redo() throws Exception {
        if (!canRedo()) {
            throw new IllegalStateException("Empty redo stack");
        }

        BaseTransformer baseTransformer = redoStack.pop();
        undoStack.push(baseTransformer);
        baseTransformer.transform();
    }

    public void undoAll() throws Exception {
        while (canUndo()) {
            undo();
        }
    }

    public void redoAll() throws Exception {
        while (canRedo()) {
            redo();
        }
    }
}





