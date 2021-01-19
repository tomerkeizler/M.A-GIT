package Engine.GitObjects;

import java.nio.file.Path;


public class Blob extends RepositoryObject {

    //-------------------------------------------------- //
    //------------------- Attributes ------------------- //
    //-------------------------------------------------- //

    protected String fileContent = "";

    //------------------------------------------------- //
    //------------------ Constructor ------------------ //
    //------------------------------------------------- //

    public Blob(Path fullPath) {
        super(fullPath);
    }

    public Blob(String lastModifier, String lastModifyDate, Path fullPath) {
        super(lastModifier, lastModifyDate, fullPath);
    }

    public Blob(String sha1Hash, String lastModifier, String lastModifyDate, java.nio.file.Path fullPath) {
        super(sha1Hash, lastModifier, lastModifyDate, fullPath);
    }

    //------------------------------------------------- //
    //-------------- Getters and Setters -------------- //
    //------------------------------------------------- //

    public String getFileContent() {
        return fileContent;
    }

    public void setFileContent(String fileContent) {
        this.fileContent = fileContent;
    }

    //----------------------------------------------- //
    //------------------- Methods ------------------- //
    //----------------------------------------------- //

    public String getObjectContent() {
        return fileContent;
    }

/*    @Override
    public boolean equals(Object obj) {
        return path.equals(((Blob) obj).path);
        }*/
}
