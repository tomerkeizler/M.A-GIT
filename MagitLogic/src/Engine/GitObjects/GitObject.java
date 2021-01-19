package Engine.GitObjects;

import org.apache.commons.codec.digest.DigestUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public abstract class GitObject {

    //-------------------------------------------------- //
    //------------------- Attributes ------------------- //
    //-------------------------------------------------- //

    protected String sha1 = null;
    protected String lastModifier = null;
    protected String lastModifyDate = null;

    //------------------------------------------------- //
    //------------------ Constructor ------------------ //
    //------------------------------------------------- //

    public GitObject() {
        // empty constructor
    }

    public GitObject(String lastModifier, String lastModifyDate) {
        this.sha1 = "";
        this.lastModifier = lastModifier;
        this.lastModifyDate = lastModifyDate;
    }

    public GitObject(String sha1Hash, String lastModifier, String lastModifyDate) {
        this.sha1 = sha1Hash;
        this.lastModifier = lastModifier;
        this.lastModifyDate = lastModifyDate;
    }

    //------------------------------------------------- //
    //-------------- Getters and Setters -------------- //
    //------------------------------------------------- //

    public String getSha1() {
        return sha1;
    }

    public void setSha1(String sha1) {
        this.sha1 = sha1;
    }


    public String getLastModifyDate() {
        return lastModifyDate;
    }

    public void setLastModifyDate(String lastModifyDate) {
        this.lastModifyDate = lastModifyDate;
    }

    public String getLastModifier() {
        return lastModifier;
    }

    public void setLastModifier(String lastModifier) {
        this.lastModifier = lastModifier;
    }

//----------------------------------------------- //
    //------------------- Methods ------------------- //
    //----------------------------------------------- //

    public abstract String getObjectDetails();

    public abstract String getObjectContent();

    public void setSha1() {
        this.sha1 = DigestUtils.sha1Hex(getObjectContent());
    }

    @Override
    public String toString() {
        return getObjectDetails();
    }

}
