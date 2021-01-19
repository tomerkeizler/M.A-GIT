package xmlHandlers;

import Exceptions.InvalidXMLfileException;
import Generated.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;


public class xmlValidator {

    //-------------------------------------------------- //
    //------------------- Attributes ------------------- //
    //-------------------------------------------------- //

    private static List<String> validationErrors = new ArrayList<>();
    private static boolean isValidXML = true;
    protected static MagitRepository magit = null;

    //----------------------------------------------------- //
    //---------------- Getters and Setters ---------------- //
    //----------------------------------------------------- //

    public static String getValidationErrors() {
        return String.join("\n", validationErrors);
    }

    public static boolean isValidXML() {
        return isValidXML;
    }

    public static MagitRepository getMagitXML() {
        return magit;
    }

    //---------------------------------------------- //
    //---------------- Main methods ---------------- //
    //---------------------------------------------- //

    public static void validateFileXML(String xmlFilePath) throws IOException, InvalidXMLfileException {
        validateFileExists(xmlFilePath);
        validateFileType(xmlFilePath);

        validationErrors.clear();
        isValidXML = true;
        magit = JAXB.loadXML(xmlFilePath);
        validateDetailsXML();
    }

    private static void validateDetailsXML() {
        if (magit != null) {
            validateBlobsSingularity();
            validateFoldersSingularity();
            validateCommitsSingularity();

            validateObjectsReferencedByFolders("blob");
            validateObjectsReferencedByFolders("folder");

            validateFoldersReferencedByCommits();
            validateCommitsReferencedByBranches();
            validateHeadBranchName();

            validateRemoteRepositoryReference();
            validateBranchesReferencedByTrackingBranches();
        }
    }

    //--------------------------------------------------------- //
    //-------------- Specific validation methods -------------- //
    //--------------------------------------------------------- //

    private static void validateFileExists(String xmlFilePath) throws InvalidXMLfileException {
        if (!new File(xmlFilePath).exists())
            throw new InvalidXMLfileException("That file path does not exist in the file system", null);
    }

    private static void validateFileType(String xmlFilePath) throws InvalidXMLfileException {
        if (!FilenameUtils.getExtension(xmlFilePath).equals("xml"))
            throw new InvalidXMLfileException("This is not an XML file", null);
    }

    // -------------------------------------------------------------------

    private static Set<String> findDuplicates(List<String> ListID) {
        Set<String> uniques = new HashSet<>();
        return ListID.stream().filter(e -> !uniques.add(e)).collect(Collectors.toSet());
    }

    private static void validateBlobsSingularity() {
        List<MagitBlob> magitBlobs = magit.getMagitBlobs().getMagitBlob();
        List<String> blobsID = (List<String>) CollectionUtils.collect(magitBlobs, blob -> blob.getId());
        Set<String> duplicatedBlobs = findDuplicates(blobsID);

        if (!duplicatedBlobs.isEmpty()) {
            isValidXML = false;
            validationErrors.add("The following blob IDs are duplicated:\n" + duplicatedBlobs.toString() + "\n");
        }
    }

    private static void validateFoldersSingularity() {
        List<MagitSingleFolder> magitFolders = magit.getMagitFolders().getMagitSingleFolder();
        List<String> foldersID = (List<String>) CollectionUtils.collect(magitFolders, folder -> folder.getId());
        Set<String> duplicatedFolders = findDuplicates(foldersID);

        if (!duplicatedFolders.isEmpty()) {
            isValidXML = false;
            validationErrors.add("The following folder IDs are duplicated:\n" + duplicatedFolders.toString() + "\n");
        }
    }

    private static void validateCommitsSingularity() {
        List<MagitSingleCommit> magitCommits = magit.getMagitCommits().getMagitSingleCommit();
        List<String> commitsID = (List<String>) CollectionUtils.collect(magitCommits, commit -> commit.getId());
        Set<String> duplicatedCommits = findDuplicates(commitsID);

        if (!duplicatedCommits.isEmpty()) {
            isValidXML = false;
            validationErrors.add("The following commit IDs are duplicated:\n" + duplicatedCommits.toString() + "\n");
        }
    }

    // -------------------------------------------------------------------

    private static void validateObjectsReferencedBySingleFolder(MagitSingleFolder folder, String typeToValidate) {
        List<Item> folderItems = folder.getItems().getItem();

        List<Item> objectsPointedByFolder = folderItems.stream().filter(item -> item.getType().equals(typeToValidate)).collect(Collectors.toList());
        List<String> objectsIDsPointedByFolder = (List<String>) CollectionUtils.collect(objectsPointedByFolder, obj -> obj.getId());

        List<String> allObjectsID;
        if (typeToValidate.equals("blob"))
            allObjectsID = (List<String>) CollectionUtils.collect(magit.getMagitBlobs().getMagitBlob(), b -> b.getId());
        else {
            allObjectsID = (List<String>) CollectionUtils.collect(magit.getMagitFolders().getMagitSingleFolder(), f -> f.getId());
            // check if there are circular references from a folder to itself
            if (objectsIDsPointedByFolder.contains(folder.getId())) {
                isValidXML = false;
                validationErrors.add(String.format("The folder \"%s\" points to itself\n", folder.getName()));
            }
        }

        objectsIDsPointedByFolder.removeAll(allObjectsID);
        if (!objectsIDsPointedByFolder.isEmpty()) {
            isValidXML = false;
            validationErrors.add(String.format("The folder \"%s\" points to non-existing %ss IDs:\n%s\n", folder.getName(), typeToValidate, objectsIDsPointedByFolder));
        }
    }

    private static void validateObjectsReferencedByFolders(String typeToValidate) {
        List<MagitSingleFolder> magitFolders = magit.getMagitFolders().getMagitSingleFolder();

        for (MagitSingleFolder folder : magitFolders)
            validateObjectsReferencedBySingleFolder(folder, typeToValidate);
    }

    // -------------------------------------------------------------------

    private static void validateFoldersReferencedByCommits() {
        List<MagitSingleCommit> magitCommits = magit.getMagitCommits().getMagitSingleCommit();

        List<MagitSingleFolder> magitFolders = magit.getMagitFolders().getMagitSingleFolder();
        List<String> foldersID = (List<String>) CollectionUtils.collect(magitFolders, folder -> folder.getId());

        for (MagitSingleCommit com : magitCommits) {
            if (!foldersID.contains(com.getRootFolder().getId())) {
                isValidXML = false;
                validationErrors.add(String.format("The commit \"%s\" points to a non-existing folder ID: %s\n", com.getMessage(), com.getRootFolder().getId()));
            } else {
                MagitSingleFolder root = null;
                for (MagitSingleFolder folder : magitFolders)
                    if (folder.getId().equals(com.getRootFolder().getId()))
                        root = folder;
                if (root != null)
                    if (!root.isIsRoot()) {
                        isValidXML = false;
                        validationErrors.add(String.format("The commit \"%s\" points to a folder which is not a root folder\n", com.getMessage()));
                    }
            }
        }
    }

    // -------------------------------------------------------------------

    private static void validateCommitsReferencedByBranches() {
        List<MagitSingleBranch> magitBranches = magit.getMagitBranches().getMagitSingleBranch();

        List<MagitSingleCommit> magitCommits = magit.getMagitCommits().getMagitSingleCommit();
        List<String> commitsID = (List<String>) CollectionUtils.collect(magitCommits, com -> com.getId());

        for (MagitSingleBranch branch : magitBranches) {
            if (!commitsID.contains(branch.getPointedCommit().getId())) {
                isValidXML = false;
                validationErrors.add(String.format("The branch \"%s\" points to a non-existing commit ID: %s\n", branch.getName(), branch.getPointedCommit().getId()));
            }
        }
    }

    // -------------------------------------------------------------------

    private static void validateHeadBranchName() {
        // only check the head branch if the repository is not empty (there is at least one commit in it)
        if (!magit.getMagitCommits().getMagitSingleCommit().isEmpty()) {
            List<MagitSingleBranch> magitBranches = magit.getMagitBranches().getMagitSingleBranch();
            List<String> branchesNames = (List<String>) CollectionUtils.collect(magitBranches, com -> com.getName());

            String headBranchName = magit.getMagitBranches().getHead();
            if (!branchesNames.contains(headBranchName)) {
                isValidXML = false;
                validationErrors.add(String.format("The head branch \"%s\" does not exist\n", headBranchName));
            }
        }
    }

    // -------------------------------------------------------------------

    private static void validateRemoteRepositoryReference() {
        if (magit.getMagitRemoteReference() != null) {
            String remoteRepositoryLocation = magit.getMagitRemoteReference().getLocation();
            if (remoteRepositoryLocation != null && !remoteRepositoryLocation.isEmpty())
                if (!Files.isDirectory(Paths.get(remoteRepositoryLocation)) || !Files.isDirectory(Paths.get(remoteRepositoryLocation, ".magit"))) {
                    isValidXML = false;
                    validationErrors.add("The remote repository location refers to a directory which is not a M.A.GIT repository");
                }
        }
    }


    private static void validateBranchesReferencedByTrackingBranches() {
        List<MagitSingleBranch> magitBranches = magit.getMagitBranches().getMagitSingleBranch();
        Map<String, Boolean> branchNameToIsRemoteMap = magitBranches.stream().collect(Collectors.toMap(MagitSingleBranch::getName, MagitSingleBranch::isIsRemote));

        for (MagitSingleBranch branch : magitBranches) {
            if (branch.isTracking() && !branchNameToIsRemoteMap.get(branch.getTrackingAfter())) {
                isValidXML = false;
                validationErrors.add(String.format("The RTB (Remote Tracking Branch) \"%s\" is tracking after a non-remote branch \"%s\"\n", branch.getName(), branch.getTrackingAfter()));
            }
        }
    }

}
