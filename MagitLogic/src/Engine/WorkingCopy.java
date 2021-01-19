package Engine;

import Engine.GitObjects.Blob;
import Engine.GitObjects.Folder;
import Engine.GitObjects.RepositoryObject;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;


public class WorkingCopy {

    //-------------------------------------------------- //
    //------------------- Attributes ------------------- //
    //-------------------------------------------------- //

    protected Path repositoryPath;
    protected Folder rootFolder;
    protected Set<Path> emptyDirectories;
    protected Delta delta;

    //------------------------------------------------- //
    //------------------ Constructor ------------------ //
    //------------------------------------------------- //

    public WorkingCopy(String repPath) {
        this.repositoryPath = Paths.get(repPath);
        this.emptyDirectories = new HashSet<>();
        this.delta = new Delta();
    }

    //------------------------------------------------- //
    //-------------- Getters and Setters -------------- //
    //------------------------------------------------- //

    public Path getRepositoryPath() {
        return repositoryPath;
    }

    public Folder getRootFolder() {
        return rootFolder;
    }

    public void setRootFolder(Folder rootFolder) {
        this.rootFolder = rootFolder;
    }

    public Set<Path> getEmptyDirectories() {
        return emptyDirectories;
    }

    public Delta getDelta() {
        return delta;
    }

    //----------------------------------------------- //
    //------------------- Methods ------------------- //
    //----------------------------------------------- //

    public void evaluateChanges(Set<Blob> workingCopyBlobs, Set<Blob> commitBlobs) {
        delta.evaluateChanges(workingCopyBlobs, commitBlobs);
    }


    public void deleteWorkingCopy() throws IOException {
        // clean all files in the repository library, except ".magit" folder
        for (File file : repositoryPath.toFile().listFiles())
            if (!file.getName().equals(".magit")) {
                if (file.isDirectory())
                    FileUtils.deleteDirectory(file);
                else
                    file.delete();
            }

        this.rootFolder = null;
        emptyDirectories.clear();
        delta.clear();
    }

    // -------------------------------------------------------------------

    public void createWorkingCopyFileSystem(Folder rootFolderToGenerate) throws IOException {
        deleteWorkingCopy();
        if (rootFolderToGenerate != null)
            createWorkingCopyFileSystemRec(rootFolderToGenerate);
        this.rootFolder = rootFolderToGenerate;
    }


    public void createWorkingCopyFileSystemRec(RepositoryObject obj) throws IOException {
        if (obj instanceof Blob) {
            File newBlob = new File(obj.getPath().toString());
            FileUtils.writeStringToFile(newBlob, ((Blob) obj).getFileContent(), Charset.defaultCharset());

        } else {
            new File(obj.getPath().toString()).mkdir();
            for (RepositoryObject item : ((Folder) obj).getFolderItems()) {
                createWorkingCopyFileSystemRec(item);
            }
        }
    }

    // -------------------------------------------------------------------

    public void createWorkingCopyMemory() throws IOException {
        emptyDirectories.clear();
        delta.clear();
        rootFolder = new Folder(repositoryPath);
        createWorkingCopyMemoryRec(rootFolder);
    }


    public void createWorkingCopyMemoryRec(RepositoryObject obj) throws NullPointerException, IOException {
        if (obj instanceof Blob) {
            // obj is a blob - then read its content and calculate its SHA-1 code
            String blobContent = new String(Files.readAllBytes(obj.getPath()));
            ((Blob) obj).setFileContent(blobContent.replaceAll("\r\n","\n"));
            obj.setSha1();

        } else {
            // obj is a folder = then iterate over its internal items
            File[] folderItems = new File(obj.getPath().toString()).listFiles();
            if (folderItems != null)
                for (File fileSystemItem : folderItems) {
                    Path itemPath = Paths.get(fileSystemItem.getAbsolutePath());
                    if (!itemPath.getFileName().toString().equals(".magit")) {
                        /* verify the type of this internal item (in obj directory)
                        1) empty directory
                        2) non-empty directory
                        3) blob*/
                        if (Files.isDirectory(itemPath) && fileSystemItem.list().length == 0) {
                            // empty directory
                            emptyDirectories.add(Paths.get(fileSystemItem.toString()));
                            Files.deleteIfExists(itemPath);
                        } else {
                            RepositoryObject item;
                            if (Files.isDirectory(itemPath))
                                // non-empty directory
                                item = new Folder(itemPath);
                            else
                                // blob
                                item = new Blob(itemPath);

                            // add the internal item to the item list of obj
                            ((Folder) obj).addItem(item);

                            // load the internal item into the repository, recursively
                            createWorkingCopyMemoryRec(item);
                        }
                    }
                }
        }
    }

}
