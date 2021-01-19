package Engine;

import Engine.GitObjects.Blob;
import Engine.GitObjects.Commit;
import Engine.GitObjects.Folder;
import Engine.GitObjects.GitObject;
import Engine.GitObjects.RepositoryObject;
import org.apache.commons.collections4.CollectionUtils;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;


public class Delta {

    //-------------------------------------------------- //
    //------------------- Attributes ------------------- //
    //-------------------------------------------------- //

    private Set<Blob> unchangedFiles;
    private Set<Blob> updatedFiles;
    private Set<Blob> createdFiles;
    private Set<Blob> deletedFiles;

    //------------------------------------------------- //
    //------------------ Constructor ------------------ //
    //------------------------------------------------- //

    public Delta() {
        this.unchangedFiles = new HashSet<>();
        this.updatedFiles = new HashSet<>();
        this.createdFiles = new HashSet<>();
        this.deletedFiles = new HashSet<>();
    }

    //------------------------------------------------- //
    //-------------- Getters and Setters -------------- //
    //------------------------------------------------- //

    public Set<Blob> getUnchangedFiles() {
        return unchangedFiles;
    }

    public Set<Blob> getUpdatedFiles() {
        return updatedFiles;
    }

    public Set<Blob> getCreatedFiles() {
        return createdFiles;
    }

    public Set<Blob> getDeletedFiles() {
        return deletedFiles;
    }

    // -------------------------------------------------------------------

    public Set<String> convertBlobsToPathStrings(Set<Blob> blobs) {
        return new HashSet<>(CollectionUtils.collect(blobs, b -> b.getPath().toString()));
    }

    public Set<String> getUnchangedFilesPaths() {
        return convertBlobsToPathStrings(unchangedFiles);
    }

    public Set<String> getUpdatedFilesPaths() {
        return convertBlobsToPathStrings(updatedFiles);
    }

    public Set<String> getCreatedFilesPaths() {
        return convertBlobsToPathStrings(createdFiles);
    }

    public Set<String> getDeletedFilesPaths() {
        return convertBlobsToPathStrings(deletedFiles);
    }

    //----------------------------------------------- //
    //------------------- Methods ------------------- //
    //----------------------------------------------- //

    public void evaluateChanges(Set<Blob> presentBlobs, Set<Blob> pastBlobs) {
        // WC is empty, current commit is not empty - all files of current commit were deleted
        if (presentBlobs.isEmpty() && !pastBlobs.isEmpty())
            deletedFiles = new HashSet<>(pastBlobs);

            // WC is not empty, current commit is empty - all files of WC were added
        else if (!presentBlobs.isEmpty() && pastBlobs.isEmpty())
            createdFiles = new HashSet<>(presentBlobs);

            // WC is not empty, current commit is not empty
        else {
            for (Blob presentBlob : presentBlobs) {
                for (Blob pastBlob : pastBlobs) {
                    boolean isPathIdentical = presentBlob.getPath().toString().equals(pastBlob.getPath().toString());
                    boolean isContentIdentical = presentBlob.getSha1().equals(pastBlob.getSha1());

                    if (isPathIdentical) {
                        if (isContentIdentical) {
                            unchangedFiles.add(presentBlob);
                        } else
                            updatedFiles.add(presentBlob);
                    } else {
                        createdFiles.add(presentBlob);
                        deletedFiles.add(pastBlob);
                    }
                }
            }
            balanceDelta();
        }
    }


    public void clear() {
        unchangedFiles.clear();
        updatedFiles.clear();
        createdFiles.clear();
        deletedFiles.clear();
    }


    public void balanceDelta() {
        // balancing list of created files
        removeItemsFromSet(createdFiles, unchangedFiles);
        removeItemsFromSet(createdFiles, updatedFiles);

        // balancing list of deleted files
        removeItemsFromSet(deletedFiles, unchangedFiles);
        removeItemsFromSet(deletedFiles, updatedFiles);
    }


    public void removeItemsFromSet(Set<Blob> set, Set<Blob> itemsToRemove) {
        CollectionUtils.filter(set, checkItem -> itemsToRemove.stream().noneMatch(item -> item.getPath().equals(checkItem.getPath())));
    }


    public boolean isDeltaNotEmpty() {
        return !createdFiles.isEmpty() || !updatedFiles.isEmpty() || !deletedFiles.isEmpty();
    }

}
