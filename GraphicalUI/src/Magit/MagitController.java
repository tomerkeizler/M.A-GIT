package Magit;

import BranchList.BranchListController;
import CommitTree.CommitTreeController;
import ConflictSolver.ConflictSolverController;
import Delta.DeltaController;
import Engine.Branches.RemoteBranch;
import Engine.GitEngine;
import Engine.MergeObjects.Conflict;
import Exceptions.*;
import FileTree.FileTreeController;
import Header.HeaderController;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import xmlHandlers.xmlValidator;
import Engine.Branches.Branch;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;


public class MagitController implements Initializable {

    //-------------------------------------------------- //
    //------------------- Attributes ------------------- //
    //-------------------------------------------------- //

    private GitEngine magit;

    @FXML
    private BorderPane mainComponent;

    @FXML
    private BorderPane headerComponent;
    @FXML
    private HeaderController headerComponentController;

    @FXML
    private ScrollPane branchListComponent;
    @FXML
    private BranchListController branchListComponentController;

    @FXML
    private ScrollPane commitTreeComponent;
    @FXML
    private CommitTreeController commitTreeComponentController;

    @FXML
    private ScrollPane fileTreeComponent;
    @FXML
    private FileTreeController fileTreeComponentController;

    private PropertyChangeSupport onRepositoryLoading;
    private PropertyChangeSupport onBranchUpdate;
    private PropertyChangeSupport onCommitOrMergeOrFetch;
    private PropertyChangeSupport onCheckout;
    private PropertyChangeSupport onResetHeadOrPull;
    private PropertyChangeSupport onPush;

    private final PropertyChangeEvent event = new PropertyChangeEvent(this, "", null, null);

    //----------------------------------------------- //
    //------------------- Getters ------------------- //
    //----------------------------------------------- //

    public GitEngine getMagit() {
        return magit;
    }

    public HeaderController getHeaderComponentController() {
        return headerComponentController;
    }

    public BranchListController getBranchListComponentController() {
        return branchListComponentController;
    }

    public CommitTreeController getCommitTreeComponentController() {
        return commitTreeComponentController;
    }

    public FileTreeController getFileTreeComponentController() {
        return fileTreeComponentController;
    }


    //----------------------------------------------- //
    //------------------- Methods ------------------- //
    //----------------------------------------------- //

    @FXML
    public void initialize(URL location, ResourceBundle resources) {
        magit = new GitEngine();
        headerComponentController.updateUsernameLabel(magit.getUsername());

        if (headerComponentController != null && branchListComponentController != null && commitTreeComponentController != null && fileTreeComponentController != null) {
            headerComponentController.setMainController(this);
            branchListComponentController.setMainController(this);
            commitTreeComponentController.setMainController(this);
            fileTreeComponentController.setMainController(this);
        }

        // connect listeners for event: loading repository
        onRepositoryLoading = new PropertyChangeSupport(this);
        onRepositoryLoading.addPropertyChangeListener(headerComponentController);
        onRepositoryLoading.addPropertyChangeListener(branchListComponentController);
        onRepositoryLoading.addPropertyChangeListener(commitTreeComponentController);
        onRepositoryLoading.addPropertyChangeListener(fileTreeComponentController);

        // connect listeners for event: adding/deleting a branch
        onBranchUpdate = new PropertyChangeSupport(this);
        onBranchUpdate.addPropertyChangeListener(branchListComponentController);
        onBranchUpdate.addPropertyChangeListener(commitTreeComponentController);

        // connect listeners for event: commit/merge/fetch
        onCommitOrMergeOrFetch = new PropertyChangeSupport(this);
        onCommitOrMergeOrFetch.addPropertyChangeListener(branchListComponentController);
        onCommitOrMergeOrFetch.addPropertyChangeListener(commitTreeComponentController);
        onCommitOrMergeOrFetch.addPropertyChangeListener(fileTreeComponentController);

        // connect listeners for event: checkout to a different head branch
        onCheckout = new PropertyChangeSupport(this);
        onCheckout.addPropertyChangeListener(branchListComponentController);
        onCheckout.addPropertyChangeListener(commitTreeComponentController);
        onCheckout.addPropertyChangeListener(fileTreeComponentController);

        // connect listeners for event: resetting head branch OR pull
        onResetHeadOrPull = new PropertyChangeSupport(this);
        onResetHeadOrPull.addPropertyChangeListener(commitTreeComponentController);
        onResetHeadOrPull.addPropertyChangeListener(fileTreeComponentController);

        // connect listeners for event: push
        onPush = new PropertyChangeSupport(this);
        onPush.addPropertyChangeListener(commitTreeComponentController);

        // define minimal width of commit tree and file tree components
        commitTreeComponent.minWidthProperty().bind(Bindings.multiply(0.3, mainComponent.widthProperty()));
        fileTreeComponent.minWidthProperty().bind(Bindings.multiply(0.15, mainComponent.widthProperty()));
    }

    //------------------------------------------------- //
    //-------------- Repository commands -------------- //
    //------------------------------------------------- //

    public void updateUsernameUI() {
        String title = "Switch Username";
        String header = "New username:";
        String newUsername = Utility.ShowTextDialog(title, header, "", false, true);

        boolean res = true;
        try {
            if (newUsername == null)
                res = false;
            else
                magit.updateUsernameCommand(newUsername);

        } catch (IOException e) {
            res = false;
            Utility.ShowErrorDialog(e.getMessage());
        }

        if (res) // successful operation
        {
            headerComponentController.updateUsernameLabel(magit.getUsername());
            Utility.ShowInfoDialog(String.format("Username was successfully updated to %s", newUsername), "");
        }
    }


    public void loadRepositoryFromXmlUI() {
        File xmlFilePath = Utility.LocateFileXML();
        boolean res = true;
        try {
            if (xmlFilePath == null) {
                res = false;
            } else {
                 if (!magit.loadRepositoryFromXmlCommand(xmlFilePath.getAbsolutePath())) {
                    // the repository was not loaded from the XML file yet
                    // the XML file is valid, but there is an existing repository currently located in the destination
                    String repPath = xmlValidator.getMagitXML().getLocation();

                    String title = "Load Repository from XML";
                    String header = String.format("There is an existing repository currently located in \"%s\"", repPath);
                    String content = "Please choose an option:\n" +
                            "Proceed - to delete the existing repository and load a new one from XML\n" +
                            "or Cancel - to return to main screen";
                    String chosenOption = Utility.ShowConfirmationDialog(title, header, content);

                    if (chosenOption.equals("Proceed")) {
                        // Delete the repository that already exists
                        magit.cleanRepositoryFileSystem(repPath);
                        // load repository from XML and checkout to its head branch
                        magit.performLoadingXML(xmlValidator.getMagitXML());
                    } else {
                        // return to main menu
                        res = false;
                    }
                }
            }

        } catch (LibraryContainsNonRepositoryContentException | IllegalArgumentException | InvalidXMLfileException | IOException e) {
            res = false;
            Utility.ShowErrorDialog(e.getMessage());
        }

        if (res) // successful operation
        {
            onRepositoryLoading.firePropertyChange(event);
            Utility.ShowInfoDialog(String.format("Repository \"%s\" was loaded successfully to \"%s\"", xmlValidator.getMagitXML().getName(), xmlValidator.getMagitXML().getLocation()), "");
        }
    }


    public void switchRepositoryUI() {
        File dir = Utility.LocateDirectory("Load Repository from PC");
        boolean res = true;
        try {
            if (dir == null)
                res = false;
            else
                magit.switchRepositoryCommand(dir.getAbsolutePath());

        } catch (RepositoryNotFoundException | RepositoryInvalidException | IOException e) {
            res = false;
            Utility.ShowErrorDialog(e.getMessage());
        }

        if (res) // successful operation
        {
            onRepositoryLoading.firePropertyChange(event);
            Utility.ShowInfoDialog(String.format("Repository \"%s\" was loaded successfully from \"%s\"", magit.getRepository().getRepositoryName(), dir.getAbsolutePath()), "");
        }
    }


    public void createRepositoryUI() {
        String title = "Create new Repository";
        String header = "Name of the new Repository:";
        String repName = Utility.ShowTextDialog(title, header, "", true, true);

        if (repName != null) {
            File repPath = null;
            boolean isRepPathEmpty = false;

            while (!isRepPathEmpty) {
                repPath = Utility.LocateDirectory(String.format("Choose a location for the new Repository \"%s\"", repName));
                if (repPath == null)
                    isRepPathEmpty = true;
                else if (repPath.isDirectory() && repPath.list().length == 0)
                    isRepPathEmpty = true;
                else
                    Utility.ShowErrorDialog("This directory is not empty!\nPlease choose an empty directory or create a new one");
            }

            boolean res = true;
            try {
                if (repPath == null)
                    res = false;
                else
                    magit.createRepositoryCommand(repName, repPath.getAbsolutePath());

            } catch (IOException e) {
                // the directory already exists
                res = false;
                Utility.ShowErrorDialog(e.getMessage());
            }

            if (res) // successful operation
            {
                onRepositoryLoading.firePropertyChange(event);
                Utility.ShowInfoDialog(String.format("New repository \"%s\" was created successfully in %s", repName, repPath.getAbsolutePath()), "");
            }
        }
    }

    //--------------------------------------------- //
    //-------------- Branch commands -------------- //
    //--------------------------------------------- //

    public void addBranchUI(String commitSha1) {
        boolean res = false;
        boolean proceedAddBranch = true;
        boolean addRemoteTrackingBranch = false;
        String branchNameToAdd = "";

        try {
            // check if there is an active repository
            if (!magit.isRepositoryActive())
                throw new NoActiveRepositoryException(null);

            // get a commit sha-1 that the new branch will refer to
            if (commitSha1.equals("")) {
                // check if there is at least one commit in the whole repository
                List<String> allCommitsSha1s = magit.getRepository().getAllCommitsSha1s();
                if (allCommitsSha1s.isEmpty())
                    throw new RepositoryEmptyOfCommitsException(null);
                String title = "Create new Branch";
                String header = "Choose a commit SHA-1 for the new branch:";
                commitSha1 = Utility.ShowChoiceDialog(title, header, allCommitsSha1s);
                if (commitSha1 == null)
                    proceedAddBranch = false;
            }

            // if the chosen commit sha-1 is pointed by another RB (Remote Branch)
            if (proceedAddBranch && magit.getRepository().hasRemoteRepository()) {
                String remoteBranchName = magit.getRepository().getRemoteBranchNameByCommitSha1(commitSha1);
                if (remoteBranchName != null) {
                    String title = "Create new Branch";
                    String header = "The commit SHA-1 you have chosen is referred by another RB (Remote Branch):\n" + remoteBranchName;
                    String optionCreateRTB = "Create RTB";
                    String optionCreateRegularBranch = "Create regular branch";
                    String content = "Please choose an option:\n" +
                            optionCreateRTB + " - to create a Remote Tracking Branch that tracks the mentioned RB\n" +
                            "or " + optionCreateRegularBranch + " - to create a regular branch\n" +
                            "or Cancel - to return to main screen";
                    String chosenOption = Utility.ShowConfirmationDialogThreeOptions(title, header, content, optionCreateRTB, optionCreateRegularBranch);

                    if (chosenOption.equals("Cancel"))
                        proceedAddBranch = false;
                    else
                        addRemoteTrackingBranch = chosenOption.equals(optionCreateRTB);

                    if (addRemoteTrackingBranch)
                        branchNameToAdd = ((RemoteBranch)magit.getRepository().getBranchByName(remoteBranchName)).getOriginalBranchName();
                }
            }

            // if the user chose to add a regular branch - then get its name from the user
            if (proceedAddBranch) {
                if (!addRemoteTrackingBranch) {
                    String title = "Create new Branch";
                    String header = "Name of the new regular Branch:";
                    branchNameToAdd = Utility.ShowTextDialog(title, header, "", true, true);
                    if (branchNameToAdd == null)
                        proceedAddBranch = false;
                }
            }

            // perform the actual branch adding (for both cases - RTB or regular branch)
            if (proceedAddBranch) {
                res = true;
                magit.addBranchCommand(addRemoteTrackingBranch, branchNameToAdd, commitSha1);
            }

        } catch (NoActiveRepositoryException | RepositoryEmptyOfCommitsException | BranchAlreadyExistsException | IOException e) {
            res = false;
            Utility.ShowErrorDialog(e.getMessage());
        }

        if (res) // successful operation
        {
            onBranchUpdate.firePropertyChange(event);
            Utility.ShowInfoDialog(String.format("Branch \"%s\" was created successfully", branchNameToAdd), "");
        }
    }


    public void deleteBranchUI(String branchToDelete) {
        try {
            // check if there is an active repository
            if (!magit.isRepositoryActive())
                throw new NoActiveRepositoryException(null);

            if (branchToDelete.equals("")) {
                String title = "Delete Branch";
                String header = "Name of a branch to delete:";
                branchToDelete = Utility.ShowTextDialog(title, header, "", true, true);
            }

            if (branchToDelete != null) {
                boolean res = true;
                try {
                    magit.deleteBranchCommand(branchToDelete);

                } catch (HeadBranchForbbidenDeletionException | BranchNotFoundException | IOException e) {
                    res = false;
                    Utility.ShowErrorDialog(e.getMessage());
                }

                if (res) // successful operation
                {
                    onBranchUpdate.firePropertyChange(event);
                    Utility.ShowInfoDialog(String.format("Branch \"%s\" was deleted successfully", branchToDelete), "");
                }
            }

        } catch (NoActiveRepositoryException e) {
            Utility.ShowErrorDialog(e.getMessage());
        }
    }


    public void checkOutUI(String branchNameForCheckout) {
        boolean res = false;
        boolean proceedCheckOut = true;
        try {
            // check if there is an active repository
            if (!magit.isRepositoryActive())
                throw new NoActiveRepositoryException(null);

            // if the working copy is not clean (there are unsaved changes)
            if (!magit.getRepository().isWorkingCopyClean(magit.getUsername())) {
                proceedCheckOut = false;
                String title = "Checkout";
                String header = "There are unsaved changes in the repository !!";
                String content = "Please choose an option:\n" +
                        "Proceed - to perform a checkout operation (and ignore the changes)\n" +
                        "or Cancel - to return to main screen (where you can commit the changes)";
                String chosenOption = Utility.ShowConfirmationDialog(title, header, content);

                // Continue with the checkout operation
                if (chosenOption.equals("Proceed"))
                    proceedCheckOut = true;
            }

            // get input of branch name for checkout operation
            if (proceedCheckOut) {
                if (branchNameForCheckout.equals("")) {
                    String title = "Checkout";
                    String header = "Name of a branch to checkout to:";
                    branchNameForCheckout = Utility.ShowTextDialog(title, header, "", true, true);
                    if (branchNameForCheckout == null)
                        proceedCheckOut = false;
                }
            }

            // detect whether the requested branch for checkout is RB (Remote Branch)
            if (proceedCheckOut && magit.getRepository().hasRemoteRepository()) {
                Branch branchForCheckout = magit.getRepository().getBranchByName(branchNameForCheckout);
                if (branchForCheckout instanceof RemoteBranch) {
                    proceedCheckOut = false;
                    String title = "Checkout";
                    String header = "The requested branch for checkout is RB (Remote Branch)";
                    String content = "Please choose an option:\n" +
                            "Proceed - to create RTB (Remote Tracking Branch) and checkout to it\n" +
                            "or Cancel - to return to main screen";
                    String chosenOption = Utility.ShowConfirmationDialog(title, header, content);

                    // continue operation - by creating RTB
                    if (chosenOption.equals("Proceed")) {
                        proceedCheckOut = true;
                        branchNameForCheckout = ((RemoteBranch) branchForCheckout).getOriginalBranchName();
                        magit.addBranchCommand(true, branchNameForCheckout, branchForCheckout.getCommitSha1());
                    }
                }
            }

            // perform the actual checkout
            if (proceedCheckOut) {
                res = true;
                magit.checkOutCommand(branchNameForCheckout);
            }

        } catch (NoActiveRepositoryException | BranchNotFoundException | HeadBranchAlreadyActiveException | BranchAlreadyExistsException | IOException e) {
            res = false;
            Utility.ShowErrorDialog(e.getMessage());
        }

        if (res) // successful operation
        {
            onCheckout.firePropertyChange(event);
            Utility.ShowInfoDialog(String.format("Checkout operation to branch \"%s\" done successfully", branchNameForCheckout), "");
        }
    }


    public void resetHeadBranchUI(String chosenCommitSha1) {
        boolean proceedReset = true;
        try {
            // check if there is an active repository
            if (!magit.isRepositoryActive())
                throw new NoActiveRepositoryException(null);

            // check if there is at least one commit in the whole repository
            List<String> allCommitsSha1s = magit.getRepository().getAllCommitsSha1s();
            if (allCommitsSha1s.isEmpty())
                throw new RepositoryEmptyOfCommitsException(null);

            // if the working copy is not clean (there are unsaved changes)
            if (!magit.getRepository().isWorkingCopyClean(magit.getUsername())) {
                proceedReset = false;

                String title = "Reset head branch";
                String header = "There are unsaved changes in the repository !!";
                String content = "Please choose an option:\n" +
                        "Proceed - to reset the head branch (and ignore the changes)\n" +
                        "or Cancel - to return to main screen (where you can commit the changes)";
                String chosenOption = Utility.ShowConfirmationDialog(title, header, content);

                // Continue with the reset head branch operation
                if (chosenOption.equals("Proceed"))
                    proceedReset = true;
            }

            if (proceedReset) {
                if (chosenCommitSha1.equals("")) {
                    String title = "Reset head branch";
                    String header = "Choose a new commit SHA-1 for the head branch:";
                    chosenCommitSha1 = Utility.ShowChoiceDialog(title, header, allCommitsSha1s);
                }

                if (chosenCommitSha1 != null) {
                    boolean res = true;
                    try {
                        magit.resetHeadBranchCommand(chosenCommitSha1);

                    } catch (CommitAlreadyAssociatedwithHeadBranchException | IOException e) {
                        res = false;
                        Utility.ShowErrorDialog(e.getMessage());
                    }

                    if (res) // successful operation
                    {
                        onResetHeadOrPull.firePropertyChange(event);
                        Utility.ShowInfoDialog("Reset head branch operation done successfully", "");
                    }
                }
            }

        } catch (NoActiveRepositoryException | RepositoryEmptyOfCommitsException | IOException e) {
            Utility.ShowErrorDialog(e.getMessage());
        }
    }

    //--------------------------------------------- //
    //-------------- Commit commands -------------- //
    //--------------------------------------------- //

    public void addCommitUI() {
        try {
            // check if there is an active repository
            if (!magit.isRepositoryActive())
                throw new NoActiveRepositoryException(null);

            if (magit.getRepository().isWorkingCopyClean(magit.getUsername()))
                Utility.ShowInfoDialog("There are no open changes in the WC at the moment", "");
            else {
                String title = "Commit";
                String header = "Content of the new commit";
                String commitContent = Utility.ShowTextDialog(title, header, "", false, true);

                if (commitContent != null) {
                    boolean res = true;
                    try {
                        magit.addCommitCommand(commitContent);

                    } catch (NoOpenChangesToCommitException | IOException e) {
                        res = false;
                        Utility.ShowErrorDialog(e.getMessage());
                    }

                    if (res) // successful operation
                    {
                        onCommitOrMergeOrFetch.firePropertyChange(event);
                        Utility.ShowInfoDialog("Commit done successfully", "");
                    }
                }
            }
        } catch (NoActiveRepositoryException | IOException e) {
            Utility.ShowErrorDialog(e.getMessage());
        }
    }


    public void mergeBranchesUI(String branchToMerge) {
        boolean res = true;
        try {
            // check if there is an active repository
            if (!magit.isRepositoryActive())
                throw new NoActiveRepositoryException(null);

            // check if the working copy is not clean (there are unsaved changes)
            if (!magit.getRepository().isWorkingCopyClean(magit.getUsername()))
                throw new WorkingCopyNotCleanException("merge", null);

            // check if there is a current commit
            if (magit.getRepository().getCurrentCommit() == null)
                throw new NoCurrentCommitException(null);

            // the working copy is clean - so perform a merge
            // choosing a branch to merge with the head branch
            if (branchToMerge.equals("")) {
                List<String> branchesNames = new ArrayList<>(magit.getRepository().getBranchNamesWithoutHead());
                String title = "Merge branches";
                String header = "Choose a branch to merge with the head branch:";
                branchToMerge = Utility.ShowChoiceDialog(title, header, branchesNames);
            }

            if (branchToMerge == null)
                res = false;
            else {
                // calculate deltas of the merge
                List<Conflict> conflicts = new ArrayList<>();
                boolean isStandardMerge = magit.getRepository().calculateMerge(branchToMerge, conflicts);

                // standard merge (not FF merge)
                if (isStandardMerge) {
                    // solve merge conflicts (if there are any)
                    if (conflicts.isEmpty())
                        Utility.ShowInfoDialog("There are no conflicts to solve", "");
                    else {
                        Utility.ShowInfoDialog("Let us solve the merge conflicts!", "");
                        String headBranch = getMagit().getRepository().getHeadBranch().getBranchName();
                        solveConflictsUI(conflicts, headBranch, branchToMerge);
                    }
                }

                // commit the merge into the repository
                String title = "Merge branches";
                String header = "Content of the new merge commit:";
                String commitContent = Utility.ShowTextDialog(title, header, "", false, false);

                // it is guaranteed that commitContent will not be null
                magit.getRepository().commitMerge(magit.getUsername(), commitContent, branchToMerge, isStandardMerge);
            }

        } catch (HeadBranchAlreadyContainsBranchToMergeException | WorkingCopyNotCleanException e) {
            res = false;
            Utility.ShowInfoDialog(e.getMessage(), "");
        } catch (NoActiveRepositoryException | NoCurrentCommitException | IOException e) {
            res = false;
            Utility.ShowErrorDialog(e.getMessage());
        }

        if (res) // successful operation
        {
            onCommitOrMergeOrFetch.firePropertyChange(event);
            Utility.ShowInfoDialog("Merge branches operation done successfully", "");
        }
    }


    public void solveConflictsUI(List<Conflict> conflicts, String headBranch, String mergeBranch) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/ConflictSolver/ConflictSolver.fxml"));
        Parent root = fxmlLoader.load();
        ConflictSolverController conflictSolverController = fxmlLoader.getController();
        conflictSolverController.setMergeDetails(conflicts, headBranch, mergeBranch);
        Stage stage = new Stage();
        stage.setScene(new Scene(root));
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Conflict solver");
        stage.showAndWait();
    }


    public void showWorkingCopyStatusUI() {
        try {
            // check if there is an active repository
            if (!magit.isRepositoryActive())
                throw new NoActiveRepositoryException(null);

            if (magit.getRepository().isWorkingCopyClean(magit.getUsername()))
                Utility.ShowInfoDialog("There are no open changes in the WC at the moment", "");
            else {
                magit.getRepository().evaluateWorkingCopyChanges();
                FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/Delta/Delta.fxml"));
                Parent root = fxmlLoader.load();

                DeltaController wcChangesController = fxmlLoader.getController();
                wcChangesController.setMainController(this);
                wcChangesController.setDeltaA(magit.getRepository().getWorkingCopy().getDelta());
                wcChangesController.initializeWorkingCopyDelta();

                Stage stage = new Stage();
                stage.setScene(new Scene(root));
                stage.initModality(Modality.APPLICATION_MODAL);
                String title = "Changes in the working Copy";
                wcChangesController.setTreeTitle(title);
                stage.setTitle(title);
                stage.show();
            }

        } catch (NoActiveRepositoryException | IOException e) {
            Utility.ShowErrorDialog(e.getMessage());
        }
    }

    //------------------------------------------------ //
    //------------ Collaboration commands ------------ //
    //------------------------------------------------ //

    public void cloneRepositoryUI() {
        String localRepName = "", localRepPath = "";
        boolean res = true;
        try {
            File remoteRepPath = Utility.LocateDirectory("choose a Repository to clone from PC");
            if (remoteRepPath == null)
                res = false;

            if (res) {
                magit.validateRepository(remoteRepPath.toString());
                localRepName = Utility.ShowTextDialog("Clone Repository", "Name of the new cloned repository:", "", true, true);
                if (localRepName == null)
                    res = false;
            }

            if (res) {
                File localRepDirectory = getNonEmptyLocationFromUser("Choose a location for the cloned Repository");
                if (localRepDirectory != null) {
                    magit.cloneRepositoryCommand(remoteRepPath.toString(), localRepName, localRepDirectory.toString());
                    onRepositoryLoading.firePropertyChange(event);
                    Utility.ShowInfoDialog("Clone repository operation done successfully", "");
                }
            }

        } catch (RepositoryNotFoundException | RepositoryInvalidException | IOException e) {
            Utility.ShowErrorDialog(e.getMessage());
        }
    }


    public void fetchUI() {
        boolean res = true;
        try {
            // check if there is an active repository
            if (!magit.isRepositoryActive())
                throw new NoActiveRepositoryException(null);

            // perform fetch operation
            magit.getRepository().fetch();
            // load repository to memory after completing fetch operation
            magit.switchRepositoryCommand(magit.getRepository().getRepositoryPath().toString());

        } catch (RepositoryNotFoundException | RepositoryInvalidException | NoRemoteRepositoryException | NoActiveRepositoryException | IOException e) {
            res = false;
            Utility.ShowErrorDialog(e.getMessage());
        }

        if (res) // successful operation
        {
            onCommitOrMergeOrFetch.firePropertyChange(event);
            Utility.ShowInfoDialog("Fetch operation done successfully", "");
        }
    }


    public void pullUI() {
        boolean res = true;
        try {
            magit.pullCommand();
        } catch (HeadBranchNotTrackingException | IllegalPullException | CollaborationEmptyOperationException e) {
            res = false;
            Utility.ShowInfoDialog(e.getMessage(), "");
        } catch (RepositoryNotFoundException | RepositoryInvalidException | NoRemoteRepositoryException | NoActiveRepositoryException | WorkingCopyNotCleanException | IOException e) {
            res = false;
            Utility.ShowErrorDialog(e.getMessage());
        }
        if (res) // successful operation
        {
            onResetHeadOrPull.firePropertyChange(event);
            Utility.ShowInfoDialog("Pull operation done successfully", "");
        }
    }


    public void pushUI() {
        boolean res = true;
        try {
            magit.pushCommand();
        } catch (HeadBranchNotTrackingException | IllegalPushException | CollaborationEmptyOperationException e) {
            res = false;
            Utility.ShowInfoDialog(e.getMessage(), "");
        } catch (NoRemoteRepositoryException | NoActiveRepositoryException | WorkingCopyNotCleanException | IOException e) {
            res = false;
            Utility.ShowErrorDialog(e.getMessage());
        }
        if (res) // successful operation
        {
            onPush.firePropertyChange(event);
            Utility.ShowInfoDialog("Push operation done successfully", "");
        }
    }

    //---------------------------------------- //
    //------------ Helper mathods ------------ //
    //---------------------------------------- //

    public File getNonEmptyLocationFromUser (String title) {
        File repPath = null;
        boolean isRepPathEmpty = false;
        while (!isRepPathEmpty) {
            repPath = Utility.LocateDirectory(title);
            if (repPath == null)
                isRepPathEmpty = true;
            else if (repPath.isDirectory() && repPath.list().length == 0)
                isRepPathEmpty = true;
            else
                Utility.ShowErrorDialog("This directory is not empty!\nPlease choose an empty directory or create a new one");
        }
        return repPath;
    }


}