package Engine;

import Exceptions.*;
import Generated.MagitRepository;
import org.apache.commons.io.FileUtils;
import xmlHandlers.xmlConverter;
import xmlHandlers.xmlValidator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


public class GitEngine {

    //-------------------------------------------------- //
    //------------------- Attributes ------------------- //
    //-------------------------------------------------- //

    protected Repository repo = null;
    protected String username;

    //------------------------------------------------- //
    //------------------ Constructor ------------------ //
    //------------------------------------------------- //

    public GitEngine() {
        this.username = "Administrator";
    }

    //------------------------------------------------- //
    //-------------- Getters and Setters -------------- //
    //------------------------------------------------- //

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Repository getRepository() {
        return repo;
    }

    //----------------------------------------------- //
    //------------------- Methods ------------------- //
    //----------------------------------------------- //

    public boolean isRepositoryActive() {
        return repo != null;
    }


    public void updateUsernameCommand(String newUsername) throws IOException {
        // this is the current username
        if (username.equals(newUsername))
            throw new IOException("The current username is already " + newUsername);
        else
            username = newUsername;
    }


    public boolean loadRepositoryFromXmlCommand(String xmlFilePath) throws LibraryContainsNonRepositoryContentException, InvalidXMLfileException, IOException {
        xmlValidator.validateFileXML(xmlFilePath);

        if (xmlValidator.isValidXML()) {
            // the XML file is valid
            MagitRepository magitXML = xmlValidator.getMagitXML();
            String repPath = magitXML.getLocation();

            // the repository path does not exist yet in the file system
            if (!Files.isDirectory(Paths.get(repPath))) {
                // load repository from XML and checkout to its head branch
                performLoadingXML(magitXML);
                return true;
            }

            // the repository path already exists in the file system
            else {
                // check which content is there
                if (!Files.isDirectory(Paths.get(repPath, ".magit")))
                    // the content in that path is not a M.A.GIT repository
                    throw new LibraryContainsNonRepositoryContentException(repPath, null);
                else
                    // the content is a M.A.GIT repository
                    return false;
            }

        } else {
            // the XML file is not valid
            throw new InvalidXMLfileException(xmlValidator.getValidationErrors(), null);
        }
    }


    public void switchRepositoryCommand(String repPath) throws RepositoryNotFoundException, RepositoryInvalidException, IOException {
        validateRepository(repPath);
        String repName = new String(Files.readAllBytes(Paths.get(repPath, ".magit", "repName.txt")));

        File remoteRepFile = new File(repPath, ".magit\\remoteRep.txt");
        if (remoteRepFile.exists()) {
            List<String> remoteRepDetails = FileUtils.readLines(remoteRepFile, "UTF-8");
            String remoteRepName = "", remoteRepPath = "";
            if (remoteRepDetails.size() == 2) {
                remoteRepName = remoteRepDetails.get(0);
                remoteRepPath = remoteRepDetails.get(1);
            }
            repo = new Repository(repName, repPath, remoteRepName, remoteRepPath);
        } else
            repo = new Repository(repName, repPath);

        // load the repository from the file system to the program memory
        repo.loadRepositoryToMemory();
    }


    public void addCommitCommand(String commitContent) throws IOException, NoOpenChangesToCommitException {
        // the WC is clean - so there is nothing to commit
        if (repo.isWorkingCopyClean(username))
            throw new NoOpenChangesToCommitException(null);
        else
            repo.addCommit(username, commitContent, "");
    }


    public void addBranchCommand(boolean isRemoteTrackingBranch, String branchNameToAdd, String commitSha1) throws BranchAlreadyExistsException, IOException {
        // there is already such a branch
        if (repo.getBranchByName(branchNameToAdd) != null)
            throw new BranchAlreadyExistsException(branchNameToAdd, null);

        else {
            repo.addBranch(isRemoteTrackingBranch, branchNameToAdd, commitSha1);
        }
    }


    public void deleteBranchCommand(String branchNameToDelete) throws BranchNotFoundException, HeadBranchForbbidenDeletionException, IOException {
        // there is no such branch
        if (repo.getBranchByName(branchNameToDelete) == null)
            throw new BranchNotFoundException(branchNameToDelete, null);

            // this is already the head branch
        else if (repo.isHeadBranch(branchNameToDelete))
            throw new HeadBranchForbbidenDeletionException(branchNameToDelete, null);

        else {
            repo.deleteBranch(branchNameToDelete);
        }
    }


    public void checkOutCommand(String newHeadBranchName) throws BranchNotFoundException, HeadBranchAlreadyActiveException, IOException {
        // there is no such branch
        if (repo.getBranchByName(newHeadBranchName) == null)
            throw new BranchNotFoundException(newHeadBranchName, null);

            // this is already the head branch
        else if (repo.isHeadBranch(newHeadBranchName))
            throw new HeadBranchAlreadyActiveException(newHeadBranchName, null);

        else {
            repo.checkOut(newHeadBranchName);
        }
    }


    public void createRepositoryCommand(String repName, String repPath) throws IOException {
        // create infrastructure of the new repository
        createRepositoryInfrastructure(repName, repPath, "master");

        // create master branch
        repo.addBranch(false,"master", null);
        repo.setHeadBranch("master");
    }


    public void resetHeadBranchCommand(String chosenCommitSha1) throws CommitAlreadyAssociatedwithHeadBranchException, IOException {
        // this is already the current commit of the head branch
        if (repo.getCurrentCommitSha1() != null && repo.getCurrentCommitSha1().equals(chosenCommitSha1))
            throw new CommitAlreadyAssociatedwithHeadBranchException(null);

        else {
            repo.resetHeadBranch(chosenCommitSha1);
        }
    }

    //--------------------------------------------------- //
    //------------------ Collaboration ------------------ //
    //--------------------------------------------------- //

    public void validateRepository(String repPath) throws RepositoryNotFoundException, RepositoryInvalidException {
        if (!Files.isDirectory(Paths.get(repPath)))
            throw new RepositoryNotFoundException(repPath, null);

        else if (!Files.isDirectory(Paths.get(repPath, ".magit")))
            throw new RepositoryInvalidException(repPath, null);
    }


    public void cloneRepositoryCommand(String remoteRepPath, String localRepName, String localRepPath) throws IOException, RepositoryInvalidException, RepositoryNotFoundException {
        String remoteRepName = new String(Files.readAllBytes(Paths.get(remoteRepPath, ".magit", "repName.txt")));
        String remoteRepHeadBranchName = new String(Files.readAllBytes(Paths.get(remoteRepPath, ".magit", "Branches", "head.txt")));

        File remoteRepDirectory = new File(remoteRepPath);
        File localRepDirectory = new File(localRepPath);

        // copy all files of RR to LR
        FileUtils.copyDirectory(remoteRepDirectory, localRepDirectory);

        // update the text file with the LR (local Repository) name
        Path localRepNameFilePath = Paths.get(localRepPath, ".magit", "repName.txt");
        Files.write(localRepNameFilePath, localRepName.getBytes());

        // create a text file for holding the RR (Remote Repository) details
        Path remoteDetailsFilePath = Paths.get(localRepPath, ".magit", "remoteRep.txt");
        new File(remoteDetailsFilePath.toString()).createNewFile();
        String remoteDetails = remoteRepName + "\n" + remoteRepPath;
        Files.write(remoteDetailsFilePath, remoteDetails.getBytes());

        // create a directory to store all the RB (Remote Branches)
        File branchesDirectory = new File(localRepPath, ".magit\\Branches");
        File remoteBranchesDirectory = new File(branchesDirectory, remoteRepName);
        remoteBranchesDirectory.mkdir();
        File[] branchItems = branchesDirectory.listFiles();
        if (branchItems != null)
            for (File branchTextFile : branchItems)
                if (branchTextFile.isFile() && !branchTextFile.getName().equals("head.txt"))
                    FileUtils.moveFileToDirectory(branchTextFile, remoteBranchesDirectory, true);

        // copy the actual head branch file to Branches directory and make it RTB (Remote Tracking Branch)
        File headBranchFile = new File(remoteBranchesDirectory, remoteRepHeadBranchName + ".txt");
        FileUtils.copyFileToDirectory(headBranchFile, branchesDirectory, true);

        Path pathOfHeadRTB = Paths.get(branchesDirectory.toString(), remoteRepHeadBranchName + ".txt");
        String headNameRB = "\n" + remoteRepName + "\\" + remoteRepHeadBranchName;
        Files.write(pathOfHeadRTB, headNameRB.getBytes(), StandardOpenOption.APPEND);

        // load LR (Local Repository)
        switchRepositoryCommand(localRepDirectory.toString());
        // checkout to the head branch
        repo.checkOut(repo.getHeadBranch().getBranchName());
    }


    public void pullCommand() throws IOException, HeadBranchNotTrackingException, NoRemoteRepositoryException, NoActiveRepositoryException, WorkingCopyNotCleanException, RepositoryInvalidException, RepositoryNotFoundException, IllegalPullException, CollaborationEmptyOperationException {
        // check if there is an active repository
        if (!isRepositoryActive())
            throw new NoActiveRepositoryException(null);

        if (!getRepository().hasRemoteRepository())
            throw new NoRemoteRepositoryException(null);

        // check if the working copy is not clean (there are unsaved changes)
         if (!getRepository().isWorkingCopyClean(getUsername()))
            throw new WorkingCopyNotCleanException("pull", null);

        // perform pull operation
        getRepository().pull();

        // load repository to memory after completing pull operation
        switchRepositoryCommand(getRepository().getRepositoryPath().toString());

        // create the WC according to the commit of the head branch
        getRepository().getWorkingCopy().createWorkingCopyFileSystem(getRepository().getCurrentCommitRootFolder());
    }


    public void pushCommand() throws IOException, HeadBranchNotTrackingException, NoRemoteRepositoryException, NoActiveRepositoryException, WorkingCopyNotCleanException, IllegalPushException, CollaborationEmptyOperationException {
        // check if there is an active repository
        if (!isRepositoryActive())
            throw new NoActiveRepositoryException(null);

        if (!getRepository().hasRemoteRepository())
            throw new NoRemoteRepositoryException(null);

        // check if the working copy of RR is not clean (there are unsaved changes)
          if (!getRepository().getRemoteRepository().isWorkingCopyClean(getUsername()))
            throw new WorkingCopyNotCleanException("push", null);

        // perform pull operation
        getRepository().push();
    }

    //--------------------------------------------------------- //
    //--------- Helper methods for loading repository --------- //
    //--------------------------------------------------------- //

    private void createRepositoryInfrastructure(String repName, String repPath, String headBranchName) throws IOException {
        // create repository main directory
        new File(repPath).mkdir();

        // create repository core folders
        new File(repPath, ".magit\\Objects").mkdirs();
        new File(repPath, ".magit\\Branches").mkdirs();

        // create a text file for holding the head branch
        Path headFilePath = Paths.get(repPath, ".magit", "Branches", "head.txt");
        new File(headFilePath.toString()).createNewFile();
        Files.write(headFilePath, headBranchName.getBytes());

        // create a text file for holding the repository name
        Path repNameFilePath = Paths.get(repPath, ".magit", "repName.txt");
        new File(repNameFilePath.toString()).createNewFile();
        Files.write(repNameFilePath, repName.getBytes());

        // create an empty text file for commits
        Path commitsFilePath = Paths.get(repPath, ".magit", "Objects", "commits.txt");
        new File(commitsFilePath.toString()).createNewFile();

        // create new Repository object
        repo = new Repository(repName, repPath);
    }


    public void cleanRepositoryFileSystem(String pathToClean) throws IOException {
        FileUtils.cleanDirectory(new File(pathToClean));
    }


    public void performLoadingXML(MagitRepository magitXML) throws IOException {
        // create infrastructure of the new repository
        createRepositoryInfrastructure(magitXML.getName(), magitXML.getLocation(), magitXML.getMagitBranches().getHead());

        // load repository from XML
        xmlConverter.convertRepositoryFromXML(magitXML);

        // load the new repository from the file system to the program memory
        repo.loadRepositoryToMemory();

        // checkout to the head branch
        repo.checkOut(repo.getHeadBranch().getBranchName());
    }


    //-------------------------------------------- //
    //---------------- Zip Method ---------------- //
    //-------------------------------------------- //

    public static void zipFile(Path filePath) throws IOException {
        File fileToZip = new File(filePath.toString());
        Path zipPath = Paths.get(filePath.toString().substring(0, filePath.toString().length() - 4) + ".zip");

        FileOutputStream fos = new FileOutputStream(zipPath.toString());
        ZipOutputStream zipOut = new ZipOutputStream(fos);
        FileInputStream fis = new FileInputStream(fileToZip);
        ZipEntry zipEntry = new ZipEntry(fileToZip.getName());
        zipOut.putNextEntry(zipEntry);

        byte[] bytes = new byte[1024];
        int length;
        while ((length = fis.read(bytes)) >= 0) {
            zipOut.write(bytes, 0, length);
        }
        zipOut.close();
        fis.close();
        fos.close();
    }

}