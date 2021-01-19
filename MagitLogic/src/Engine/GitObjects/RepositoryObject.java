package Engine.GitObjects;

import java.nio.file.Path;


public abstract class RepositoryObject extends GitObject implements Comparable {

    //-------------------------------------------------- //
    //------------------- Attributes ------------------- //
    //-------------------------------------------------- //

    protected Path path;

    //------------------------------------------------- //
    //------------------ Constructor ------------------ //
    //------------------------------------------------- //

    public RepositoryObject(Path path) {
        this.path = path;
    }

    public RepositoryObject(String lastModifier, String lastModifyDate, Path path) {
        super(lastModifier, lastModifyDate);
        this.path = path;
    }

    public RepositoryObject(String sha1Hash, String lastModifier, String lastModifyDate, Path path) {
        super(sha1Hash, lastModifier, lastModifyDate);
        this.path = path;
    }

    //------------------------------------------------- //
    //-------------- Getters and Setters -------------- //
    //------------------------------------------------- //

    public String getName() {
        return path.getFileName().toString();
    }

    public Path getPath() {
        return path;
    }

    //----------------------------------------------- //
    //------------------- Methods ------------------- //
    //----------------------------------------------- //

    public String getObjectDetails() {
        return String.format("%s,%s,%s,%s,%s", getName(), sha1, this.getClass().getSimpleName().toLowerCase(), lastModifier, lastModifyDate);
    }

    @Override
    public String toString() {
        // return getObjectDetails();
        return getName();
    }

    @Override
    public boolean equals(Object obj) {
        return path.equals(((RepositoryObject) obj).path);
     /*   boolean res;
        if (sha1 == null || ((RepositoryObject) obj).sha1 == null)
            res = path.equals(((RepositoryObject) obj).path);
        else
            res = sha1.equals(((RepositoryObject) obj).sha1) && path.equals(((RepositoryObject) obj).path);
        return res;*/
    }

    @Override
    public int compareTo(Object obj) {
        //return path.compareTo(((RepositoryObject) obj).path);
        return this.getObjectDetails().compareTo(((RepositoryObject) obj).getObjectDetails());
    }

}
