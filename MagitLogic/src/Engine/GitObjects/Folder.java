package Engine.GitObjects;

import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;


public class Folder extends RepositoryObject {

    //-------------------------------------------------- //
    //------------------- Attributes ------------------- //
    //-------------------------------------------------- //

    protected Set<RepositoryObject> folderItems = new TreeSet<>();

    //------------------------------------------------- //
    //------------------ Constructor ------------------ //
    //------------------------------------------------- //

    public Folder(Path fullPath) {
        super(fullPath);
    }

    public Folder(String lastModifier, String lastModifyDate, Path fullPath) {
        super(lastModifier, lastModifyDate, fullPath);
    }

    public Folder(String sha1Hash, String lastModifier, String lastModifyDate, Path fullPath) {
        super(sha1Hash, lastModifier, lastModifyDate, fullPath);
    }

    //------------------------------------------------- //
    //-------------- Getters and Setters -------------- //
    //------------------------------------------------- //

    public Set<RepositoryObject> getFolderItems() {
        return folderItems;
    }

    public void setFolderItems(Set<RepositoryObject> folderItems) {
        this.folderItems = folderItems;
    }

    //----------------------------------------------- //
    //------------------- Methods ------------------- //
    //----------------------------------------------- //

    public void addItem(RepositoryObject obj) {
        folderItems.add(obj);
    }

    public String getObjectContent() {
        StringBuilder sb = new StringBuilder();
        for (RepositoryObject obj : folderItems) {
            sb.append(obj.getObjectDetails());
            if (!obj.equals(((TreeSet) folderItems).last()))
                sb.append("\n");
        }
        return sb.toString();
    }


}
