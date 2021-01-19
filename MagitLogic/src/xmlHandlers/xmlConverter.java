package xmlHandlers;

import Engine.GitEngine;
import Generated.*;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;


public class xmlConverter {

    //-------------------------------------------------- //
    //------------------- Attributes ------------------- //
    //-------------------------------------------------- //

    private static MagitRepository magit = null;

    private static Map<String, MagitBlob> magitBlobsMap = new HashMap<>();
    private static Map<String, MagitSingleFolder> magitFoldersMap = new HashMap<>();
    private static Map<String, MagitSingleCommit> magitCommitsMap = new HashMap<>();

    // maps of <ID,SHA-1>
    private static Map<String, String> doneBlobs = new HashMap<>();
    private static Map<String, String> doneFolders = new HashMap<>();
    private static Map<String, String> doneCommits = new LinkedHashMap<>();

    //----------------------------------------- //
    //---------------- Methods ---------------- //
    //----------------------------------------- //

    public static void convertRepositoryFromXML(MagitRepository magitXML) throws IOException {
        magit = magitXML;

        // create maps for enabling access to abjects by their ID
        mapAllObjects();

        // convert XML generated objects into text/zip files in the file system
        createRepositoryFileSystemFromXML();

        // create a list that stores the entries of the commits map
        List<Map.Entry<String, String>> entryList = new ArrayList<>(doneCommits.entrySet());
        Map.Entry<String, String> lastEntry = entryList.get(entryList.size() - 1);

        // get a list of the SHA-1 codes of all commits in the repository
        StringBuilder allCommitsSha1 = new StringBuilder();
        for (String commitID : doneCommits.keySet()) {
            allCommitsSha1.append(doneCommits.get(commitID));
            if (!commitID.equals(lastEntry.getKey()))
                allCommitsSha1.append("\n");
        }

        // write the SHA-1 codes of all commits into commits.txt
        Path commitsFilePath = Paths.get(magitXML.getLocation(), ".magit", "Objects", "commits.txt");
        Files.write(commitsFilePath, allCommitsSha1.toString().getBytes());
    }


    public static void mapAllObjects() {
        // create a map for enabling access to a MagitBlob by its ID
        List<MagitBlob> blobsXML = magit.getMagitBlobs().getMagitBlob();
        for (MagitBlob blob : blobsXML)
            magitBlobsMap.put(blob.getId(), blob);

        // create a map for enabling access to a MagitSingleFolder by its ID
        List<MagitSingleFolder> foldersXML = magit.getMagitFolders().getMagitSingleFolder();
        for (MagitSingleFolder folder : foldersXML)
            magitFoldersMap.put(folder.getId(), folder);

        // create a map for enabling access to a MagitSingleCommit by its ID
        List<MagitSingleCommit> commitsXML = magit.getMagitCommits().getMagitSingleCommit();
        for (MagitSingleCommit com : commitsXML)
            magitCommitsMap.put(com.getId(), com);
    }


    private static void createRepositoryFileSystemFromXML() throws IOException {
        // iterate all branches
        for (MagitSingleBranch branch : magit.getMagitBranches().getMagitSingleBranch()) {
            // create zip files in "Objects" directory, by iterating the entire commits chain of the current branch
            String commitSha1 = scanXmlCommitRec(magitCommitsMap.get(branch.getPointedCommit().getId()));
            // create a text file in "Branches" directory
            Path branchPath = Paths.get(magit.getLocation(), ".magit", "Branches", branch.getName() + ".txt");
            FileUtils.writeStringToFile(branchPath.toFile(), commitSha1, Charset.defaultCharset());
        }
    }


    private static String scanXmlCommitRec(MagitSingleCommit com) throws IOException {
        if (doneCommits.containsKey(com.getId()))
            return doneCommits.get(com.getId());

        // get SHA-1 of root folder
        Item commitRootItem = new Item();
        commitRootItem.setType("folder");
        commitRootItem.setId(com.getRootFolder().getId());
        String commitRootFolderSha1 = scanXmlRootFolderRec(commitRootItem);

        // get SHA-1 of previous commits
        List<PrecedingCommits.PrecedingCommit> preComs = com.getPrecedingCommits().getPrecedingCommit();
        List<String> previousCommitsSha1 = new ArrayList<>(2);
        previousCommitsSha1.add("");
        previousCommitsSha1.add("");

        for (int i = 0; i < preComs.size(); i++) {
            MagitSingleCommit previousCom = magitCommitsMap.get(preComs.get(i).getId());
            previousCommitsSha1.set(i, scanXmlCommitRec(previousCom));
        }

        // create a zip file for the current commit
        String commitFileContent = String.format("%s\n%s\n%s\n%s\n%s\n%s", commitRootFolderSha1, previousCommitsSha1.get(0), previousCommitsSha1.get(1), com.getMessage(), com.getDateOfCreation(), com.getAuthor());
        String commitSha1 = DigestUtils.sha1Hex(commitFileContent);
        zipRepositoryObject(magit.getLocation(), commitSha1, commitFileContent);

        doneCommits.put(com.getId(), commitSha1);
        return commitSha1;
    }


    private static String scanXmlRootFolderRec(Item magitItem) throws IOException {
        String itemName, itemContent = "", itemSha1, itemLastModifier, itemLastModifyDate;

        if (magitItem.getType().equalsIgnoreCase("blob")) {
            // check if this blob was already done
            if (doneBlobs.containsKey(magitItem.getId()))
                return doneBlobs.get(magitItem.getId());

            // update blob details
            MagitBlob blob = magitBlobsMap.get(magitItem.getId());
            itemName = blob.getName();
            itemLastModifier = blob.getLastUpdater();
            itemLastModifyDate = blob.getLastUpdateDate();
            itemContent = blob.getContent().replaceAll("\r\n","\n");
        }

        /////////////////////////////////////////////////////////////////

        else {
            // check if this folder was already done
            if (doneFolders.containsKey(magitItem.getId()))
                return doneFolders.get(magitItem.getId());

            // update folder details
            MagitSingleFolder folder = magitFoldersMap.get(magitItem.getId());
            itemName = folder.getName();
            itemLastModifier = folder.getLastUpdater();
            itemLastModifyDate = folder.getLastUpdateDate();

            // iterate the internal items in this folder, recursively
            Set<String> descriptionLines = new TreeSet<>();
            List<Item> folderItems = folder.getItems().getItem();

            for (Item internalItem : folderItems)
                descriptionLines.add(scanXmlRootFolderRec(internalItem));

            int i = 1;
            for (String line : descriptionLines) {
                itemContent = itemContent.concat(line);
                if (i != folderItems.size())
                    itemContent = itemContent.concat("\n");
                i++;
            }
        }

        // calculate SHA-1 of "magitItem"
        itemSha1 = DigestUtils.sha1Hex(itemContent);
        // create a zip file for "magitItem"
        zipRepositoryObject(magit.getLocation(), itemSha1, itemContent);

        // return a SHA-1 if this is a root, or return item details otherwise
        if (magitItem.getType().equalsIgnoreCase("folder") && magitFoldersMap.get(magitItem.getId()).isIsRoot()) {
            doneFolders.put(magitItem.getId(), itemSha1);
            return itemSha1;
        } else {
            String itemDetails = String.format("%s,%s,%s,%s,%s", itemName, itemSha1, magitItem.getType().toLowerCase(), itemLastModifier, itemLastModifyDate);
            if (magitItem.getType().equalsIgnoreCase("blob"))
                doneBlobs.put(magitItem.getId(), itemDetails);
            else
                doneFolders.put(magitItem.getId(), itemDetails);
            return itemDetails;
        }
    }


    private static void zipRepositoryObject(String repPath, String zipName, String zipContent) throws IOException {
        Path objPath = Paths.get(repPath, ".magit", "Objects", zipName + ".txt");
        File objFile = new File(objPath.toString());

        if (!objFile.exists()) {
            // create a text file for the current blob
            objFile.createNewFile();
            Files.write(objPath, zipContent.getBytes());
            // create a zip file for the current blob, and then delete the text file
            GitEngine.zipFile(objPath);
            objFile.delete();
        }
    }

}
