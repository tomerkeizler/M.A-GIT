package Engine;

import Engine.Branches.RemoteBranch;
import Engine.Branches.RemoteTrackingBranch;
import Engine.GitObjects.Blob;
import Engine.GitObjects.Commit;
import Engine.GitObjects.Folder;
import Engine.GitObjects.GitObject;
import Engine.GitObjects.RepositoryObject;
import Engine.Branches.Branch;
import Engine.MergeObjects.Conflict;
import Engine.MergeObjects.WorkingCopyUpdate;
import Exceptions.*;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import puk.team.course.magit.ancestor.finder.AncestorFinder;
import puk.team.course.magit.ancestor.finder.CommitRepresentative;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


public class Repository {

    private static SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy-HH:mm:ss:SSS");
    private static String currentTime;

    //-------------------------------------------------- //
    //------------------- Attributes ------------------- //
    //-------------------------------------------------- //

    private String repositoryName;
    private Set<Branch> branches;
    private Branch headBranch;
    private Map<String, GitObject> repositoryMap;
    private Set<String> currentCommitObjects;
    private Set<String> mergedObjects;
    private WorkingCopy myWorkingCopy;
    private Repository remoteRepository;

    //------------------------------------------------- //
    //------------------ Constructor ------------------ //
    //------------------------------------------------- //

    public Repository(String repName, String repLocation) {
        this.repositoryName = repName;
        this.branches = new HashSet<>();
        this.headBranch = null;
        this.repositoryMap = new HashMap<>();
        this.currentCommitObjects = new HashSet<>();
        this.mergedObjects = new HashSet<>();
        this.myWorkingCopy = new WorkingCopy(repLocation);

        // there is no RR (Remote Repository)
        this.remoteRepository = null;
    }

    public Repository(String repName, String repLocation, String remoteRepName, String remoteRepLocation) throws IOException {
        this.repositoryName = repName;
        this.branches = new HashSet<>();
        this.headBranch = null;
        this.repositoryMap = new HashMap<>();
        this.currentCommitObjects = new HashSet<>();
        this.mergedObjects = new HashSet<>();
        this.myWorkingCopy = new WorkingCopy(repLocation);

        // load RR (Remote Repository)
        this.remoteRepository = new Repository(remoteRepName, remoteRepLocation);
        this.remoteRepository.loadRepositoryToMemory();
    }

    //------------------------------------------------- //
    //-------------- Getters and Setters -------------- //
    //------------------------------------------------- //

    public String getRepositoryName() {
        return repositoryName;
    }

    public Path getRepositoryPath() {
        return this.myWorkingCopy.getRepositoryPath();
    }

    public Set<Branch> getBranches() {
        return branches;
    }

    public Set<String> getBranchNames() {
        return new HashSet<>(CollectionUtils.collect(branches, Branch::getBranchName));
    }

    public Set<String> getBranchNamesWithoutHead() {
        Set<String> branchesNames = getBranchNames();
        branchesNames.remove(headBranch.getBranchName());
        return branchesNames;
        // return branchesNames.stream().filter(b -> !b.equals(headBranch.getBranchName())).collect(Collectors.toSet());
    }

    public Branch getHeadBranch() {
        return headBranch;
    }

    public Map<String, GitObject> getRepositoryMap() {
        return repositoryMap;
    }

    public WorkingCopy getWorkingCopy() {
        return myWorkingCopy;
    }

    public Repository getRemoteRepository() {
        return remoteRepository;
    }

    public boolean hasRemoteRepository() {
        return remoteRepository != null;
    }

    //----------------------------------------------- //
    //------------------- Methods ------------------- //
    //----------------------------------------------- //

    public void loadRepositoryToMemory() throws IOException {
        loadBranchesToMemory();
        List<String> allCommitsSha1s = getAllCommitsSha1s();
        for (String commitSha1 : allCommitsSha1s) {
            loadCommitToMemory(commitSha1);
        }
        storeSetOfCurrentCommitObjects();
    }


    public void loadBranchesToMemory() throws FileNotFoundException, IOException {
        Path branchesPath = Paths.get(getRepositoryPath().toString(), ".magit", "branches");
        File branchesDirectory = new File(branchesPath.toString());

        if (Files.isDirectory(branchesPath)) {
            File[] branchItems = branchesDirectory.listFiles();
            if (branchItems != null) {
                for (File branchFile : branchItems) {
                    String branchName, commitSha1;

                    // this is a directory that contains RB (Remote Branches)
                    if (branchFile.isDirectory()) {
                        String remoteRepName = branchFile.getName();
                        File[] remoteBranchItems = branchFile.listFiles();
                        if (remoteBranchItems != null)
                            for (File remoteBranchFile : remoteBranchItems) {
                                branchName = omitFileExtension(remoteBranchFile.getName());
                                commitSha1 = new String(Files.readAllBytes(remoteBranchFile.toPath()));
                                branches.add(new RemoteBranch(remoteRepName, branchName, commitSha1));
                            }
                    }

                    // this is a local branch or RTB (Remote Tracking Branch)
                    else if (branchFile.isFile() && !branchFile.getName().equals("head.txt")) {
                        Branch branchToAdd;
                        branchName = omitFileExtension(branchFile.getName());
                        List<String> commitInfo = FileUtils.readLines(branchFile, "UTF-8");
                        commitSha1 = commitInfo.get(0);
                        if (commitInfo.size() == 1)
                            // local branch
                            branchToAdd = new Branch(branchName, commitSha1);
                        else {
                            // RTB (Remote Tracking Branch)
                            String trackedRemoteBranch = commitInfo.get(1);
                            branchToAdd = new RemoteTrackingBranch(trackedRemoteBranch, branchName, commitSha1);
                        }
                        branches.add(branchToAdd);
                    }
                }
            }
        }

        File headFile = new File(Paths.get(getRepositoryPath().toString(), ".magit", "branches", "head.txt").toString());
        String headBranchName = new Scanner(headFile).nextLine();
        this.headBranch = getBranchByName(headBranchName);
    }


    public void loadCommitToMemory(String commitSha1) throws IOException {
        if (!repositoryMap.containsKey(commitSha1)) {
            // unzip the zip file of this commit
            Path pathOfCommitZip = Paths.get(getRepositoryPath().toString(), ".magit", "Objects", commitSha1 + ".zip");
            List<String> commitDetails = readContentFromZip(pathOfCommitZip);

            // organizing the new commit details
            String rootFolderSha1 = commitDetails.get(0);
            String previousCommitSha1 = commitDetails.get(1);
            String secondPreviousCommitSha1 = commitDetails.get(2);
            String commitContent = commitDetails.get(3);
            String lastModifyDate = commitDetails.get(4);
            String lastModifier = commitDetails.get(5);

            if (previousCommitSha1.equals("null"))
                previousCommitSha1 = "";
            if (secondPreviousCommitSha1.equals("null"))
                secondPreviousCommitSha1 = "";

            // adding the new commit object to the repository map
            Commit newCommit = new Commit(lastModifier, lastModifyDate, rootFolderSha1, previousCommitSha1, secondPreviousCommitSha1, commitContent);
            repositoryMap.put(commitSha1, newCommit);

            // create the root folder object (if it does not exist yet)
            if (!repositoryMap.containsKey(rootFolderSha1)) {
                Folder rootFolder = new Folder(rootFolderSha1, lastModifier, lastModifyDate, getRepositoryPath());
                loadRepositoryObjectToMemoryRec(rootFolder);
            }
        }
    }


    public void loadRepositoryObjectToMemoryRec(RepositoryObject obj) throws IOException {
        // --------------------------------------------------------
        // in case that obj does not exist yet in the repository map
        // --------------------------------------------------------
        if (!repositoryMap.containsKey(obj.getSha1())) {
            // unzip the zip file of this blob/folder
            Path pathOfObjectZip = Paths.get(getRepositoryPath().toString(), ".magit", "Objects", obj.getSha1() + ".zip");
            List<String> zipContent = readContentFromZip(pathOfObjectZip);

            if (obj instanceof Blob) // obj is a blob
            {
                String blobContent = String.join("\n", zipContent);
                ((Blob) obj).setFileContent(blobContent.replaceAll("\r\n", "\n"));
             }

            else {
                // obj is a folder - so iterate over its internal items
                for (String itemDescriptionLine : zipContent) {
                    String[] itemDetails = itemDescriptionLine.split(",", 5);

                    // organizing the blob/folder details
                    String itemName = itemDetails[0];
                    String itemSha1 = itemDetails[1];
                    String itemType = itemDetails[2];
                    String itemLastModifier = itemDetails[3];
                    String itemLastModifyDate = itemDetails[4];

                    Path itemPath = Paths.get(obj.getPath().toString(), itemName);
                    RepositoryObject item;
                    if (itemType.equalsIgnoreCase(Blob.class.getSimpleName().toLowerCase()))
                        item = new Blob(itemSha1, itemLastModifier, itemLastModifyDate, itemPath);
                    else
                        item = new Folder(itemSha1, itemLastModifier, itemLastModifyDate, itemPath);

                    // add the internal item to the item list of obj
                    ((Folder) obj).addItem(item);

                    // load the internal item to the repository, recursively
                    loadRepositoryObjectToMemoryRec(item);
                }
            }
            // adding obj to the repository map
            repositoryMap.put(obj.getSha1(), obj);

            // --------------------------------------------------------
            // in case that obj is already exists in the repository map
            // --------------------------------------------------------
        } else {
            if (obj instanceof Blob)
                ((Blob) obj).setFileContent(((Blob) repositoryMap.get(obj.getSha1())).getFileContent());
            else
                ((Folder) obj).setFolderItems(((Folder) repositoryMap.get(obj.getSha1())).getFolderItems());
        }
    }

    // -------------------------------------------------------------------

    public boolean isWorkingCopyClean(String username) throws IOException {
        // update WC and get SHA-1 of the root folder of WC
        updateWorkingCopyMemory(false, username);
        String workingCopyRootSha1 = myWorkingCopy.getRootFolder().getSha1();

        // get SHA-1 of the root folder of current commit
        String currentCommitRootSha1 = DigestUtils.sha1Hex("");
        if (doesHeadBranchHaveCommit())
            currentCommitRootSha1 = getCurrentCommit().getRootFolderSha1();

        // compare: (root of commit) VS (root of WC)
        return currentCommitRootSha1.equals(workingCopyRootSha1);
    }


    public void evaluateWorkingCopyChanges() throws IOException {
        // generate a set of the WC files only
        Set<RepositoryObject> workingCopyBlobs = new HashSet<>();
        getAllItemsRec(myWorkingCopy.getRootFolder(), workingCopyBlobs, false);

        // generate a set of the current commit files only
        Set<RepositoryObject> commitBlobs = new HashSet<>();
        getAllItemsRec(getCurrentCommitRootFolder(), commitBlobs, false);

        // evaluate the changes
        myWorkingCopy.evaluateChanges((Set<Blob>) (Set<?>) workingCopyBlobs, (Set<Blob>) (Set<?>) commitBlobs);
    }

    // -------------------------------------------------------------------

    public void deleteBranch(String branchName) {
        Branch branchToDelete = getBranchByName(branchName);
        if (branchToDelete != null) {

            // remove this branch from the repository branches set
            branches.remove(branchToDelete);

            // delete text file of that branch
            Path branchPath = Paths.get(getRepositoryPath().toString(), ".magit", "Branches", branchToDelete.getBranchName() + ".txt");
            File branchFile = new File(branchPath.toString());
            branchFile.delete();
        }
    }


    public void addBranch(boolean isRemoteTrackingBranch, String branchName, String commitSha1) throws IOException {
        // create a regular branch or RTB (Remote Tracking Branch)
        Branch newBranch;
        String branchTextFileContent = commitSha1;
        if (!isRemoteTrackingBranch) {
            newBranch = new Branch(branchName, commitSha1);
        } else {
            String trackedRemoteBranch = !hasRemoteRepository() ? "" : String.format("%s\\%s", remoteRepository.getRepositoryName(), branchName);
            branchTextFileContent = branchTextFileContent.concat("\n" + trackedRemoteBranch);
            newBranch = new RemoteTrackingBranch(trackedRemoteBranch, branchName, commitSha1);
        }
        // add this branch to the repository branches set
        branches.add(newBranch);

        // create a text file for the new branch
        Path newBranchPath = Paths.get(getRepositoryPath().toString(), ".magit", "Branches", branchName + ".txt");
        FileUtils.writeStringToFile(newBranchPath.toFile(), branchTextFileContent, Charset.defaultCharset());
    }


    public void setHeadBranch(String newHeadBranchName) throws IOException {
        // update headBranch field
        headBranch = getBranchByName(newHeadBranchName);

        // update head.txt file
        Path headPath = Paths.get(getRepositoryPath().toString(), ".magit", "Branches", "head.txt");
        FileWriter fw = new FileWriter(headPath.toString(), false);
        fw.write(newHeadBranchName);
        fw.close();
    }


    public void resetHeadBranch(String chosenCommitSha1) throws IOException {
        // update the head branch to point on the existing chosen commit (check if this is RTB)
        headBranch.setCommitSha1(chosenCommitSha1);
        Path branchPath = Paths.get(getRepositoryPath().toString(), ".magit", "Branches", headBranch.getBranchName() + ".txt");
        String trackedRemoteBranch = "";
        List<String> branchInfo = FileUtils.readLines(branchPath.toFile(), "UTF-8");
        if (branchInfo.size() == 2)
            trackedRemoteBranch = branchInfo.get(1);
        String branchTextFileContent = trackedRemoteBranch.isEmpty() ? chosenCommitSha1 : (chosenCommitSha1 + "\n" + trackedRemoteBranch);
        FileUtils.writeStringToFile(branchPath.toFile(), branchTextFileContent, Charset.defaultCharset());

        // create the WC according to the updated commit of the head branch
        myWorkingCopy.createWorkingCopyFileSystem(getCurrentCommitRootFolder());

        // create a set containing the SHA-1 codes of all objects in the chosen commit
        storeSetOfCurrentCommitObjects();
    }


    public void checkOut(String newHeadBranchName) throws IOException {
        // update the head branch
        setHeadBranch(newHeadBranchName);

        // create the WC according to the commit of the updated head branch
        myWorkingCopy.createWorkingCopyFileSystem(getCurrentCommitRootFolder());

        // create a set containing the SHA-1 codes of all objects in the current commit after the checkout
        storeSetOfCurrentCommitObjects();
    }

    // -------------------------------------------------------------------

    public void updateWorkingCopyMemory(boolean isMerge, String username) throws IOException {
        // create the WC objects
        myWorkingCopy.createWorkingCopyMemory();
        // Fill the WC objects with details according to current commit
        updateCurrentTime();
        updateWorkingCopyMemoryRec(isMerge, myWorkingCopy.rootFolder, username);
    }


    public void updateWorkingCopyMemoryRec(boolean isMerge, RepositoryObject obj, String username) {
        // do only if obj is a folder
        if (obj instanceof Folder) {
            // update internal items
            for (RepositoryObject item : ((Folder) obj).getFolderItems())
                updateWorkingCopyMemoryRec(isMerge, item, username);
            // after updating internal items - procude a SHA-1 for obj folder (also if empty)
            obj.setSha1();
        }

        // do anyway (obj is a blob/folder)
        RepositoryObject objCounterpart = retrieveObjectFromCommit(isMerge, obj.getSha1());
        if (objCounterpart != null)
        // obj exists in the current commit OR in the commits being merged
        {
            obj.setLastModifyDate(objCounterpart.getLastModifyDate());
            obj.setLastModifier(objCounterpart.getLastModifier());

        } else
        // obj does not exist
        {
            if (getCurrentCommit() == null && obj instanceof Folder && obj.getSha1().equals(DigestUtils.sha1Hex(""))) {
                // obj is an empty root folder, and there are no commits at all in the head branch
                // DO NOTHING
            } else {
                obj.setLastModifyDate(currentTime);
                obj.setLastModifier(username);
            }
        }
    }

    // -------------------------------------------------------------------

    public void addCommit(String username, String commitContent, String secondPreviousCommitSha1) throws IOException {
        Folder folderToCommit = myWorkingCopy.rootFolder;
         addCommitRec(folderToCommit);

        String previousCommitSha1 = getCurrentCommitSha1();
        if (previousCommitSha1 == null)
            previousCommitSha1 = "";
        if (previousCommitSha1.equals("null"))
            previousCommitSha1 = "";
        // create a new commit object
        Commit newCommit = new Commit(username, currentTime, folderToCommit.getSha1(), previousCommitSha1, secondPreviousCommitSha1, commitContent);

        // create a text file for the new commit
        Path commitPath = Paths.get(getRepositoryPath().toString(), ".magit", "Objects", newCommit.getSha1() + ".txt");
        File commitFile = new File(commitPath.toString());
        commitFile.createNewFile();
        Files.write(commitPath, newCommit.getObjectContent().getBytes());

        // create a zip file for the new commit, and then delete the text file
        GitEngine.zipFile(commitPath);
        commitFile.delete();

        // update the head branch to point on the new commit (check if this is RTB)
        headBranch.setCommitSha1(newCommit.getSha1());
        Path headBranchPath = Paths.get(getRepositoryPath().toString(), ".magit", "Branches", headBranch.getBranchName() + ".txt");
        String trackedRemoteBranch = "";
        List<String> branchInfo = FileUtils.readLines(headBranchPath.toFile(), "UTF-8");
        if (branchInfo.size() == 2)
            trackedRemoteBranch = branchInfo.get(1);
        String branchTextFileContent = trackedRemoteBranch.isEmpty() ? newCommit.getSha1() : (newCommit.getSha1() + "\n" + trackedRemoteBranch);
        FileUtils.writeStringToFile(headBranchPath.toFile(), branchTextFileContent, Charset.defaultCharset());

        // add the new commit to the repository
        repositoryMap.put(newCommit.getSha1(), newCommit);

        // add the SHA-1 of the new commit to the content of commits.txt file
        Path pathOfCommitsFile = Paths.get(getRepositoryPath().toString(), ".magit", "Objects", "commits.txt");
        String preCommitDetails = "";
        if (!new String(Files.readAllBytes(pathOfCommitsFile)).isEmpty())
            preCommitDetails = "\n";
        Files.write(pathOfCommitsFile, String.format(preCommitDetails + newCommit.getSha1()).getBytes(), StandardOpenOption.APPEND);

        // create a set containing the SHA-1 codes of all objects in this new commit
        storeSetOfCurrentCommitObjects();
    }


    public void addCommitRec(RepositoryObject obj) throws IOException {
        if (!repositoryMap.containsKey(obj.getSha1())) {
            // create a text file for the new blob/folder
            Path myPath = Paths.get(getRepositoryPath().toString(), ".magit", "Objects", obj.getSha1() + ".txt");
            File myFile = new File(myPath.toString());
            myFile.createNewFile();
            Files.write(myPath, obj.getObjectContent().getBytes());

            // create a zip file for the new blob/folder, and then delete the text file
            GitEngine.zipFile(myPath);
            myFile.delete();

            // add the new blob/folder to the repository
            repositoryMap.put(obj.getSha1(), obj);

            // if this is a folder - then commit all of its internal items
            if (obj instanceof Folder)
                for (RepositoryObject item : ((Folder) obj).getFolderItems())
                    addCommitRec(item);
        }
    }

    //----------------------------------------------- //
    //---------------- Merge Methods ---------------- //
    //----------------------------------------------- //

    public Set<Blob> getBlobsByRoot(Folder rootFolder) {
        Set<RepositoryObject> temp = new HashSet<>();
        getAllItemsRec(rootFolder, temp, false);
        return (Set<Blob>) (Set<?>) temp;
    }


    private List<WorkingCopyUpdate> generateTheirsCreatedUpdates(Set<Blob> ours, Set<Blob> theirs) {
        return theirs.stream()
                .filter(theirBlob -> ours.stream().noneMatch(ourBlob -> ourBlob.getPath().equals(theirBlob.getPath())))
                .map(b -> new WorkingCopyUpdate(b.getPath(), true, b.getFileContent()))
                .collect(Collectors.toList());
    }


    private List<WorkingCopyUpdate> generateUpdates(Set<Blob> ours, Set<Blob> theirs, boolean saveFile) {
        return theirs.stream()
                .filter(theirBlob -> ours.stream().anyMatch(ourBlob -> ourBlob.getPath().equals(theirBlob.getPath())))
                .map(b -> new WorkingCopyUpdate(b.getPath(), saveFile, b.getFileContent()))
                .collect(Collectors.toList());
    }


    private List<Conflict> generateConflicts(Set<Blob> ours, Set<Blob> theirs, Conflict.Action oursAction, Conflict.Action theirsAction) {
        return theirs.stream()
                .filter(theirBlob -> ours.stream().anyMatch(ourBlob -> ourBlob.getPath().equals(theirBlob.getPath())))
                .map(b -> new Conflict(b.getPath(), oursAction, theirsAction))
                .collect(Collectors.toList());
    }


    private Map<Path, String> mapPathToBlobContent(Set<Blob> blobs) {
        return blobs.stream().collect(Collectors.toMap(Blob::getPath, Blob::getFileContent));
    }


    private void getContentVersionsForConflict(Set<Blob> ours, Set<Blob> theirs, Set<Blob> ancestor, List<Conflict> allConflicts) {
        Map<Path, String> oursMap = mapPathToBlobContent(ours);
        Map<Path, String> theirsMap = mapPathToBlobContent(theirs);
        Map<Path, String> ancestorMap = mapPathToBlobContent(ancestor);

        for (Conflict con : allConflicts) {
            Path filePath = con.getFilePath();
            con.setOursContent(oursMap.get(filePath));
            con.setTheirsContent(theirsMap.get(filePath));
            con.setAncestorContent(ancestorMap.get(filePath));
        }
    }

    // -------------------------------------------------------------------

    public boolean calculateMerge(String branchToMerge, List<Conflict> conflicts) throws IOException, HeadBranchAlreadyContainsBranchToMergeException {
        // head branch - generate a set of blobs
        String oursCommitSha1 = getCurrentCommitSha1();
        Set<Blob> oursBlobs = getBlobsByRoot(getCurrentCommitRootFolder());

        // additional branch - generate a set of blobs
        String theirsCommitSha1 = getBranchByName(branchToMerge).getCommitSha1();
        Folder theirsRoot = getRootFolderByCommitSha1(theirsCommitSha1);
        Set<Blob> theirsBlobs = getBlobsByRoot(theirsRoot);

        // find ancestor commit
        AncestorFinder ancestorFinder = new AncestorFinder(commitSha1 -> (CommitRepresentative) repositoryMap.get(commitSha1));
        String ancestorCommitSha1 = ancestorFinder.traceAncestor(oursCommitSha1, theirsCommitSha1);

        // ancestor - generate a set of blobs
        Folder ancestorRoot = getRootFolderByCommitSha1(ancestorCommitSha1);
        Set<Blob> ancestorBlobs = getBlobsByRoot(ancestorRoot);

        // -------------------------------------------------------------------

        // check for FF merge case  - the head branch already contains the other branch
        if (theirsCommitSha1.equals(ancestorCommitSha1))
            throw new HeadBranchAlreadyContainsBranchToMergeException(headBranch.getBranchName(), branchToMerge, null);

        // check for FF merge case  - the other branch already contains the head branch
        if (oursCommitSha1.equals(ancestorCommitSha1)) {
            // create the WC according to theirs branch
            Folder rootFolderToWriteToWC = getRootFolderByCommitSha1(theirsCommitSha1);
            myWorkingCopy.createWorkingCopyFileSystem(rootFolderToWriteToWC);
            return false;
        }

        // this is not FF merge, so perform a merge operation
        else {
            // generate Delta for ours branch
            Delta oursDelta = new Delta();
            oursDelta.evaluateChanges(oursBlobs, ancestorBlobs);

            // generate Delta for theirs branch
            Delta theirsDelta = new Delta();
            theirsDelta.evaluateChanges(theirsBlobs, ancestorBlobs);

            // calculate updates that can be done now
            List<WorkingCopyUpdate> upcomingUpdatesWC = new ArrayList<>();
            upcomingUpdatesWC.addAll(generateTheirsCreatedUpdates(oursDelta.getCreatedFiles(), theirsDelta.getCreatedFiles()));
            upcomingUpdatesWC.addAll(generateUpdates(oursDelta.getUnchangedFiles(), theirsDelta.getUpdatedFiles(), true));
            upcomingUpdatesWC.addAll(generateUpdates(oursDelta.getUnchangedFiles(), theirsDelta.getDeletedFiles(), false));
            // perform those updates
            for (WorkingCopyUpdate update : upcomingUpdatesWC)
                update.solve();

            // calculate conflicts which should be solved manually
            conflicts.addAll(generateConflicts(oursDelta.getCreatedFiles(), theirsDelta.getCreatedFiles(), Conflict.Action.CREATED, Conflict.Action.CREATED));
            conflicts.addAll(generateConflicts(oursDelta.getUpdatedFiles(), theirsDelta.getUpdatedFiles(), Conflict.Action.UPDATED, Conflict.Action.UPDATED));
            conflicts.addAll(generateConflicts(oursDelta.getUpdatedFiles(), theirsDelta.getDeletedFiles(), Conflict.Action.UPDATED, Conflict.Action.DELETED));
            conflicts.addAll(generateConflicts(oursDelta.getDeletedFiles(), theirsDelta.getUpdatedFiles(), Conflict.Action.DELETED, Conflict.Action.UPDATED));
            conflicts.addAll(generateConflicts(oursDelta.getDeletedFiles(), theirsDelta.getDeletedFiles(), Conflict.Action.DELETED, Conflict.Action.DELETED));

            // match contents for the 3 sides in the 3-way merge
            getContentVersionsForConflict(oursBlobs, theirsBlobs, ancestorBlobs, conflicts);
            return true;
        }
    }

    public void commitMerge(String username, String commitContent, String branchToMerge, boolean isStandardMerge) throws IOException {
        String theirsCommitSha1 = getBranchByName(branchToMerge).getCommitSha1();
        // for standard merge - update the WC memory according to the actual WC file system
        if (isStandardMerge) {
            storeSetOfMergedObjects(theirsCommitSha1);
            updateWorkingCopyMemory(true, username);
        }
        // for both standard merge and FF merge - perform the commit
        addCommit(username, commitContent, theirsCommitSha1);
    }

    // -------------------------------------------------------------------

    public void storeSetOfCurrentCommitObjects() {
        currentCommitObjects.clear();

        Folder commitRootFolder = getCurrentCommitRootFolder();
        if (commitRootFolder != null)
            storeSetOfObjectsRec(currentCommitObjects, commitRootFolder);
    }

    public void storeSetOfMergedObjects(String secondCommitSha1) {
        mergedObjects.clear();
        mergedObjects = new HashSet<>(currentCommitObjects);

        Folder secondCommitRootFolder = getRootFolderByCommitSha1(secondCommitSha1);
        if (secondCommitRootFolder != null)
            storeSetOfObjectsRec(mergedObjects, secondCommitRootFolder);
    }

    public void storeSetOfObjectsRec(Set<String> objectSet, RepositoryObject obj) {
        objectSet.add(obj.getSha1());
        if (obj instanceof Folder)
            for (RepositoryObject item : ((Folder) obj).getFolderItems())
                storeSetOfObjectsRec(objectSet, item);
    }

    //----------------------------------------------- //
    //---------------- Unzip Methods ---------------- //
    //----------------------------------------------- //

    public List<String> readContentFromZip(Path zipPath) throws IOException {
        File destDir = new File(zipPath.getParent().toString());
        List<String> fileContent = new ArrayList<>();

        byte[] buffer = new byte[1024];
        ZipInputStream zis = new ZipInputStream(new FileInputStream(zipPath.toString()));
        ZipEntry zipEntry = zis.getNextEntry();

        while (zipEntry != null) {
            File newFile = createFileFromZipEntry(destDir, zipEntry);
            FileOutputStream fos = new FileOutputStream(newFile);
            int len;
            while ((len = zis.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
            fos.close();
            zipEntry = zis.getNextEntry();
            fileContent = FileUtils.readLines(newFile, "UTF-8");
            newFile.delete();
        }
        zis.closeEntry();
        zis.close();
        return fileContent;
    }


    public File createFileFromZipEntry(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());

        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }

        return destFile;
    }

    //----------------------------------------------- //
    //--------------- Utility Methods --------------- //
    //----------------------------------------------- //

    public List<String> getAllCommitsSha1s() throws IOException {
        Path pathOfCommitsFile = Paths.get(getRepositoryPath().toString(), ".magit", "Objects", "commits.txt");
        return FileUtils.readLines(pathOfCommitsFile.toFile(), "UTF-8");
    }


    public void getAllItemsRec(RepositoryObject obj, Set<RepositoryObject> outputObjects, boolean includeFoldes) {
        if (obj != null) {
            // if obj is a blob - then add it
            if (obj instanceof Blob)
                outputObjects.add(obj);
            else {
                // if obj is a folder - then add it if includeFolders = true
                if (includeFoldes)
                    outputObjects.add(obj);
                // iterate over the folder items
                for (RepositoryObject item : ((Folder) obj).getFolderItems()) {
                    getAllItemsRec(item, outputObjects, includeFoldes);
                }
            }
        }
    }

    // -------------------------------------------------------------------

    public Branch getBranchByName(String branchName) {
        return branches.stream().filter(branch -> branch.getBranchName().equals(branchName)).findAny().orElse(null);
    }

    public boolean isHeadBranch(String branchName) {
        Branch branch = getBranchByName(branchName);
        if (branch != null && branch.equals(headBranch))
            return true;
        else
            return false;
    }

    public boolean doesHeadBranchHaveCommit() {
        return headBranch != null && headBranch.getCommitSha1() != null;
    }

    // -------------------------------------------------------------------

    public Commit getCommitBySha1(String commitSha1) {
        if (repositoryMap.containsKey(commitSha1))
            return ((Commit) repositoryMap.get(commitSha1));
        else
            return null;
    }

    public Folder getRootFolderByCommitSha1(String commitSha1) {
        if (getCommitBySha1(commitSha1) != null)
            return (Folder) repositoryMap.get(getCommitBySha1(commitSha1).getRootFolderSha1());
        else
            return null;
    }

    // -------------------------------------------------------------------

    public Commit getCurrentCommit() {
        if (doesHeadBranchHaveCommit())
            return getCommitBySha1(headBranch.getCommitSha1());
        else
            return null;
    }

    public String getCurrentCommitSha1() {
        if (doesHeadBranchHaveCommit())
            return headBranch.getCommitSha1();
        else
            return null;
    }

    public Folder getCurrentCommitRootFolder() {
        if (getCurrentCommit() != null)
            return (Folder) repositoryMap.get(getCurrentCommit().getRootFolderSha1());
        else
            return null;
    }

    // -------------------------------------------------------------------

    public RepositoryObject retrieveObjectFromCommit(boolean isMerge, String sha1) {
        GitObject res;
        Set<String> objectSet = isMerge ? mergedObjects : currentCommitObjects;

        if (sha1 != null && objectSet.contains(sha1)) {
            res = repositoryMap.get(sha1);
            if (res instanceof RepositoryObject)
                return (RepositoryObject) res;
        }
        return null;
    }

    // -------------------------------------------------------------------

    public static void updateCurrentTime() {
        currentTime = dateFormat.format(new Date());
    }

    public String getRemoteBranchNameByCommitSha1(String commitSha1) {
        for (Branch branch : getBranches())
            if (branch instanceof RemoteBranch && branch.getCommitSha1().equals(commitSha1))
                return branch.getBranchName();
        return null;
    }

    private String omitFileExtension(String fileName) {
        return fileName.substring(0, fileName.length() - 4);
    }

    //----------------------------------------------- //
    //----------- Methods for commit tree ----------- //
    //----------------------------------------------- //

    public List<Commit> getCurrentCommitsOfBranches() {
        List<String> currentCommitsOfBranches = getBranches().stream().map(branchName -> branchName.getCommitSha1()).distinct().collect(Collectors.toList());
        List<Commit> commitList = currentCommitsOfBranches.stream().map(commitSha1 -> (Commit) repositoryMap.get(commitSha1)).collect(Collectors.toList());
        while (commitList.remove(null)) ;
        return commitList;
    }


    public void getAccessibleCommitsRec(Commit com, List<Commit> commits) {
        if (com != null) {
            commits.add(com);

            Commit firstParent = getCommitBySha1(com.getFirstPrecedingSha1());
            if (firstParent != null)
                getAccessibleCommitsRec(firstParent, commits);

            Commit secondParent = getCommitBySha1(com.getSecondPrecedingSha1());
            if (secondParent != null)
                getAccessibleCommitsRec(secondParent, commits);
        }
    }


    public List<Commit> getAccessibleCommits() {
        List<Commit> accessibleCommits = new ArrayList<>();
        getCurrentCommitsOfBranches().forEach(com -> getAccessibleCommitsRec(com, accessibleCommits));
        return accessibleCommits.stream().distinct().collect(Collectors.toList());
    }


    public List<Commit> getAccessibleCommitsOfBranch(Branch branch) {
        List<Commit> branchCommits = new ArrayList<>();
        getAccessibleCommitsRec(getCommitBySha1(branch.getCommitSha1()), branchCommits);
        return branchCommits.stream().distinct().collect(Collectors.toList());
    }

    // -------------------------------------------------------------------

    public Map<String, List<Branch>> mapCommitsToBranches() {
        return getBranches().stream().collect(Collectors.groupingBy(Branch::getCommitSha1));
    }

    // -------------------------------------------------------------------

    public boolean checkCommitsParenthoodTwoSided(Commit commitA, Commit commitB) {
        return checkCommitsParenthoodOneSided(commitA, commitB) || checkCommitsParenthoodOneSided(commitB, commitA);
    }

    public boolean checkCommitsParenthoodOneSided(Commit childCommit, Commit possibleParentCommit) {
        if (childCommit == null || possibleParentCommit == null)
            return false;
        else if (childCommit.getFirstPrecedingSha1() != null && childCommit.getFirstPrecedingSha1().equals(possibleParentCommit.getSha1()))
            return true;
        else if (childCommit.getSecondPrecedingSha1() != null && childCommit.getSecondPrecedingSha1().equals(possibleParentCommit.getSha1()))
            return true;
        else
            return false;
    }

    //--------------------------------------- //
    //----------- Collaboration ------------- //
    //--------------------------------------- //

    public void fetch() throws NoRemoteRepositoryException, IOException {
        if (!hasRemoteRepository())
            throw new NoRemoteRepositoryException(null);

        // update all branches of RR in LR
        File remoteRepositoryBranchesDirectory = new File(remoteRepository.getRepositoryPath().toString(), ".magit\\Branches");
        File localRepositoryRemoteBranchesDirectory = new File(getRepositoryPath().toString(), ".magit\\Branches\\" + remoteRepository.getRepositoryName());

        if (remoteRepositoryBranchesDirectory.isDirectory()) {
            File[] branchesToFetch = remoteRepositoryBranchesDirectory.listFiles();
            if (branchesToFetch != null)
                for (File branchTextFile : branchesToFetch) {
                    if (branchTextFile.isFile() && !branchTextFile.getName().equals("head.txt"))
                        FileUtils.copyFileToDirectory(branchTextFile, localRepositoryRemoteBranchesDirectory, true);
                }
        }

        // update all commits, folders and blobs of RR in LR
        File remoteRepositoryObjectsDirectory = new File(remoteRepository.getRepositoryPath().toString(), ".magit\\Objects");
        File localRepositoryObjectsDirectory = new File(getRepositoryPath().toString(), ".magit\\Objects");
        Path pathOfCommitsFile = Paths.get(getRepositoryPath().toString(), ".magit", "Objects", "commits.txt");

        if (remoteRepositoryObjectsDirectory.isDirectory()) {
            File[] objectsToFetch = remoteRepositoryObjectsDirectory.listFiles();
            if (objectsToFetch != null)
                for (File objToFetch : objectsToFetch) {
                    // copy object file to LR (Local Repository)
                    FileUtils.copyFileToDirectory(objToFetch, localRepositoryObjectsDirectory);
                    // if the current object is a commit - then add its SHA-1 to the content of commits.txt file
                    String zipFileName = omitFileExtension(objToFetch.getName());
                    if (remoteRepository.getRepositoryMap().get(zipFileName) instanceof Commit) {
                        String preCommitDetails = new String(Files.readAllBytes(pathOfCommitsFile)).isEmpty() ? "" : "\n";
                        Files.write(pathOfCommitsFile, String.format(preCommitDetails + zipFileName).getBytes(), StandardOpenOption.APPEND);
                    }
                }
        }
    }


    void pull() throws IOException, HeadBranchNotTrackingException, IllegalPullException, CollaborationEmptyOperationException {
        if (!(headBranch instanceof RemoteTrackingBranch))
            throw new HeadBranchNotTrackingException(null);

        // check if there are non-pushed changes in RTB in LR
        Branch remoteBranch = getBranchByName(remoteRepository.getRepositoryName() + "\\" + headBranch.getBranchName());
        if (headBranch != null && !headBranch.getCommitSha1().equals(remoteBranch.getCommitSha1()))
             throw new IllegalPullException(null);

        // check if there is any data to pull
        Branch branchToPull = remoteRepository.getBranchByName(headBranch.getBranchName());
        if (branchToPull.getCommitSha1().equals(remoteBranch.getCommitSha1()))
            throw new CollaborationEmptyOperationException("pull", null);

        // update branch text files in LR (regular branch + remote branch)
        File remoteRepositoryBranchesDirectory = new File(remoteRepository.getRepositoryPath().toString(), ".magit\\Branches");
        File branchesDirectory = new File(getRepositoryPath().toString(), ".magit\\Branches");
        File remoteBranchesDirectory = new File(branchesDirectory.toString(), remoteRepository.getRepositoryName());
        File branchToPullFile = new File(remoteRepositoryBranchesDirectory.toString(), branchToPull.getBranchName() + ".txt");
        if (branchToPullFile.isFile()) {
            FileUtils.copyFileToDirectory(branchToPullFile, branchesDirectory, true);
            FileUtils.copyFileToDirectory(branchToPullFile, remoteBranchesDirectory, true);
        }
        remoteBranch.setCommitSha1(branchToPull.getCommitSha1());

        // update the text file to RTB (Remote Tracking Branch) format
        Path pathOfRTB = Paths.get(branchesDirectory.toString(), branchToPullFile.getName());
        String rbName = "\n" + remoteRepository.getRepositoryName() + "\\" + branchToPull.getBranchName();
        Files.write(pathOfRTB, rbName.getBytes(), StandardOpenOption.APPEND);

        // perform pull operation, commit by commit
        pullOrPushCommitRec(true, remoteRepository.getCurrentCommit());
    }


    void push() throws IOException, HeadBranchNotTrackingException, IllegalPushException, CollaborationEmptyOperationException {
        if (!(headBranch instanceof RemoteTrackingBranch))
            throw new HeadBranchNotTrackingException(null);

        String branchName = headBranch.getBranchName();
        Branch branchOfRR= remoteRepository.getBranchByName(branchName);
        Branch remoteBranch = getBranchByName(remoteRepository.getRepositoryName() + "\\" + branchName);

        // check if changes were made on RR since the last sync to LR
        if (branchOfRR != null && !branchOfRR.getCommitSha1().equals(remoteBranch.getCommitSha1()))
              throw new IllegalPushException(null);

        // check if there is any data to push
         if (headBranch.getCommitSha1().equals(remoteBranch.getCommitSha1()))
            throw new CollaborationEmptyOperationException("push", null);

        // get head branch text file
        String branchFileName= headBranch.getBranchName() + ".txt";
        File branchesDirectory = new File(getRepositoryPath().toString(), ".magit\\Branches");
        File branchToPushFile = new File(branchesDirectory.toString(), branchFileName);

        // update branch text files in lR (RB directory) and RR
        Path remoteRepositoryBranchPath = Paths.get(remoteRepository.getRepositoryPath().toString(), ".magit","Branches",branchFileName);
        Path remoteBranchPath = Paths.get(branchesDirectory.toString(), remoteRepository.getRepositoryName(), branchFileName);
        if (branchToPushFile.isFile()) {
            FileUtils.writeStringToFile(remoteRepositoryBranchPath.toFile(), headBranch.getCommitSha1(), Charset.defaultCharset());
            FileUtils.writeStringToFile(remoteBranchPath.toFile(), headBranch.getCommitSha1(), Charset.defaultCharset());
        }
        remoteBranch.setCommitSha1(headBranch.getCommitSha1());

        // perform push operation, commit by commit
        pullOrPushCommitRec(false, getCurrentCommit());

        // load RR (Remote Repository) after completing push operation
        this.remoteRepository = new Repository(remoteRepository.getRepositoryName(), remoteRepository.getRepositoryPath().toString());
        this.remoteRepository.loadRepositoryToMemory();

        // if the head of LR is the head of RR - then create the WC of RR
        if (headBranch.getCommitSha1().equals(remoteRepository.getHeadBranch().getCommitSha1()))
            remoteRepository.getWorkingCopy().createWorkingCopyFileSystem(remoteRepository.getCurrentCommitRootFolder());
    }


    private void pullOrPushCommitRec(boolean isPull, Commit com) throws IOException {
        boolean commitAlreadyExists = isPull ? repositoryMap.containsKey(com.getSha1()) : remoteRepository.getRepositoryMap().containsKey(com.getSha1());
        if (!commitAlreadyExists) {
            File commitObjFile, destinationDirectory;
            if (isPull) {
                commitObjFile = new File(remoteRepository.getRepositoryPath().toString(), ".magit\\Objects\\" + com.getSha1() + ".zip");
                destinationDirectory = new File(getRepositoryPath().toString(), ".magit\\Objects");
            } else {
                commitObjFile = new File(getRepositoryPath().toString(), ".magit\\Objects\\" + com.getSha1() + ".zip");
                destinationDirectory = new File(remoteRepository.getRepositoryPath().toString(), ".magit\\Objects");
            }

            if (commitObjFile.exists())
                FileUtils.copyFileToDirectory(commitObjFile, destinationDirectory);

            // add the SHA-1 of this commit to the content of commits.txt file
            Path pathOfCommitsFile;
            if (isPull)
                pathOfCommitsFile = Paths.get(getRepositoryPath().toString(), ".magit", "Objects", "commits.txt");
            else
                pathOfCommitsFile = Paths.get(getRemoteRepository().getRepositoryPath().toString(), ".magit", "Objects", "commits.txt");
            String preCommitDetails = new String(Files.readAllBytes(pathOfCommitsFile)).isEmpty() ? "" : "\n";
            Files.write(pathOfCommitsFile, String.format(preCommitDetails + com.getSha1()).getBytes(), StandardOpenOption.APPEND);

            // push blobs and folders of this commit
            pullOrPushZipFileRec(isPull, getRootFolderByCommitSha1(com.getSha1()));

            // push parent commits recursively
            Commit firstParent = getCommitBySha1(com.getFirstPrecedingSha1());
            if (firstParent != null)
                pullOrPushCommitRec(isPull, firstParent);
            Commit secondParent = getCommitBySha1(com.getSecondPrecedingSha1());
            if (secondParent != null)
                pullOrPushCommitRec(isPull, secondParent);
        }
    }


    private void pullOrPushZipFileRec(boolean isPull, RepositoryObject obj) throws IOException {
        boolean objectAlreadyExists = isPull ? repositoryMap.containsKey(obj.getSha1()) : remoteRepository.getRepositoryMap().containsKey(obj.getSha1());
        if (!objectAlreadyExists) {
            File objFile, destinationDirectory;
            if (isPull) {
                objFile = new File(remoteRepository.getRepositoryPath().toString(), ".magit\\Objects\\" + obj.getSha1() + ".zip");
                destinationDirectory = new File(getRepositoryPath().toString(), ".magit\\Objects");
            } else {
                objFile = new File(getRepositoryPath().toString(), ".magit\\Objects\\" + obj.getSha1() + ".zip");
                destinationDirectory = new File(remoteRepository.getRepositoryPath().toString(), ".magit\\Objects");
            }

            if (objFile.exists())
                FileUtils.copyFileToDirectory(objFile, destinationDirectory);

            if (obj instanceof Folder)
                for (RepositoryObject item : ((Folder) obj).getFolderItems())
                    pullOrPushZipFileRec(isPull, item);
        }
    }

}
