package Engine.MergeObjects;

import org.apache.commons.io.FileUtils;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;


public class WorkingCopyUpdate {

    //-------------------------------------------------- //
    //------------------- Attributes ------------------- //
    //-------------------------------------------------- //

    protected Path filePath;
    protected boolean saveFile;
    protected String finalContent;

    //------------------------------------------------- //
    //------------------ Constructor ------------------ //
    //------------------------------------------------- //

    public WorkingCopyUpdate(Path filePath) {
        this.filePath = filePath;
        this.saveFile = true; // default
        this.finalContent = null;
    }

    public WorkingCopyUpdate(Path filePath, boolean saveFile, String finalContent) {
        this.filePath = filePath;
        this.saveFile = saveFile;
        this.finalContent = finalContent;
    }

    //------------------------------------------------- //
    //-------------- Getters and Setters -------------- //
    //------------------------------------------------- //

    public Path getFilePath() {
        return filePath;
    }

    public boolean isSaveFile() {
        return saveFile;
    }

    public void setSaveFile(boolean saveFile) {
        this.saveFile = saveFile;
    }

    public String getFinalContent() {
        return finalContent;
    }

    public void setFinalContent(String finalContent) {
        this.finalContent = finalContent;
    }

    //----------------------------------------------- //
    //------------------- Methods ------------------- //
    //----------------------------------------------- //

    public void solve() throws IOException {
        if (!saveFile)
            filePath.toFile().delete();
        else
            FileUtils.writeStringToFile(filePath.toFile(), finalContent, Charset.defaultCharset());
    }

}

