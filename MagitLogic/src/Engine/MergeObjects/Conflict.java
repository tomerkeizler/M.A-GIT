package Engine.MergeObjects;

import java.io.IOException;
import java.nio.file.Path;


public class Conflict extends WorkingCopyUpdate {

    public enum Action {UNCHANGED, CREATED, UPDATED, DELETED}

    //-------------------------------------------------- //
    //------------------- Attributes ------------------- //
    //-------------------------------------------------- //

    private boolean solved;
    private Action oursAction;
    private Action theirsAction;

    private String ancestorContent;
    private String oursContent;
    private String theirsContent;

    //------------------------------------------------- //
    //------------------ Constructor ------------------ //
    //------------------------------------------------- //

    public Conflict(Path filePath, Action oursAction, Action theirsAction) {
        super(filePath);
        this.solved = false;
        this.oursAction = oursAction;
        this.theirsAction = theirsAction;
        this.ancestorContent = null;
        this.oursContent = null;
        this.theirsContent = null;
    }

    //------------------------------------------------- //
    //-------------- Getters and Setters -------------- //
    //------------------------------------------------- //

    public boolean isSolved() {
        return solved;
    }

    public String getOursAction() {
        return oursAction.toString();
    }

    public String getTheirsAction() {
        return theirsAction.toString();
    }

    public String getAncestorContent() {
        return ancestorContent;
    }

    public String getOursContent() {
        return oursContent;
    }

    public String getTheirsContent() {
        return theirsContent;
    }

    // -------------------------------------------------------------------

    public void setSolved(boolean solved) {
        this.solved = solved;
    }

    public void setAncestorContent(String ancestorContent) {
        this.ancestorContent = ancestorContent;
    }

    public void setOursContent(String oursContent) {
        this.oursContent = oursContent;
    }

    public void setTheirsContent(String theirsContent) {
        this.theirsContent = theirsContent;
    }

    //----------------------------------------------- //
    //------------------- Methods ------------------- //
    //----------------------------------------------- //

    public boolean isAutoSolved() {
        boolean res = false;
        if (oursContent.equals(theirsContent) && oursAction == theirsAction) {
            if (theirsAction == Action.CREATED || theirsAction == Action.UPDATED) {
                res = true;
                saveFile = true;
                finalContent = theirsContent;
            }
        } else if (oursAction == Action.DELETED && theirsAction == Action.DELETED) {
            res = true;
            saveFile = false;
        }
        return res;
    }


    public void solveManually(boolean saveFile, String finalContent)
    {
        setSolved(true);
        setSaveFile(saveFile);
        setFinalContent(finalContent);
        try {
            solve();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append(String.format("file path: %s\n", getFilePath().toString()));
        /*if (oursAction != Action.UNCHANGED && oursAction == theirsAction)
            str.append(String.format("Both branches %s the file", oursAction.toString()));
        else {
            str.append(String.format("The file was %s in Head branch\n", oursAction.toString()));
            str.append(String.format("The file was %s in the additional branch", theirsAction.toString()));
        }*/
        return str.toString();
    }

}
