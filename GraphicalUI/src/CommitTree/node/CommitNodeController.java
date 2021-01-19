package CommitTree.node;

import CommitTree.BranchNode.BranchCellFactory;
import CommitTree.BranchNode.BranchNode;
import Delta.DeltaController;
import Engine.Branches.Branch;
import Engine.Branches.RemoteBranch;
import Engine.Delta;
import FileTree.FileTreeController;
import Engine.GitObjects.Blob;
import Engine.GitObjects.Commit;
import Engine.GitObjects.Folder;
import Engine.GitObjects.GitObject;
import Engine.GitObjects.RepositoryObject;
import Magit.InternalController;
import Magit.MagitController;
import Magit.Utility;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Border;
import javafx.scene.layout.GridPane;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;

import javax.swing.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class CommitNodeController extends InternalController {

    //-------------------------------------------------- //
    //------------------- Attributes ------------------- //
    //-------------------------------------------------- //

    private CommitNode commitNode;

    @FXML private GridPane commitDisplayNode;
    @FXML private ListView branchesList;
    @FXML private Label commitTimeStampLabel;
    @FXML private Label committerLabel;
    @FXML private Label messageLabel;
    @FXML private Circle CommitCircle;

    //--------------------------------------------------- //
    //--------------- Getters and Setters --------------- //
    //--------------------------------------------------- //

    public Commit getCommit() {
        return commitNode.getCommit();
    }

    public String getCommitParents() {
        String parentsSha1 = "";
        if (getCommit().getFirstPrecedingSha1().isEmpty())
            parentsSha1 = "This commit has no parents";
        else if (getCommit().getSecondPrecedingSha1().isEmpty())
            parentsSha1 = "Single parent SHA-1: " + getCommit().getFirstPrecedingSha1();
        else
            parentsSha1 = "First parent SHA-1: " + getCommit().getFirstPrecedingSha1() + "\nSecond parent SHA-1: " + getCommit().getSecondPrecedingSha1();
        return parentsSha1;
    }

    // -------------------------------------------------------------------

    public void setBranchList(List<Branch> referringBranches) {
        if (referringBranches == null)
            referringBranches = new ArrayList<>();

        branchesList.getItems().addAll(referringBranches);
        branchesList.setCellFactory(new BranchCellFactory());

        branchesList.addEventHandler(MouseEvent.MOUSE_ENTERED, event -> {
            highlightBranchList();
            branchesList.setMinWidth(350);
        });

        branchesList.addEventHandler(MouseEvent.MOUSE_EXITED, event -> cancelHighlightBranchList());
    }

    public void setCommitTimeStamp(String timeStamp) {
        commitTimeStampLabel.setText(timeStamp);
        commitTimeStampLabel.setTooltip(new Tooltip(timeStamp));
    }

    public void setCommitter(String committerName) {
        committerLabel.setText(committerName);
        committerLabel.setTooltip(new Tooltip(committerName));
    }

    public void setCommitMessage(String commitMessage) {
        messageLabel.setText(commitMessage);
        messageLabel.setTooltip(new Tooltip(commitMessage));
    }

    public int getCircleRadius() {
        return (int)CommitCircle.getRadius();
    }

    //----------------------------------------------- //
    //------------------- Methods ------------------- //
    //----------------------------------------------- //

    public void highlightBranchList() {
        branchesList.setStyle("-fx-border-color: black; -fx-border-insets: 0; -fx-border-width: 5; -fx-border-style: solid");
        branchesList.setMinHeight(80);
    }


    public void cancelHighlightBranchList() {
        branchesList.setStyle("-fx-border-width: 0");
        branchesList.setMinHeight(50);
        branchesList.setMinWidth(250);
    }


    public void setCommitNode(CommitNode commitNode) {
        this.commitNode = commitNode;
        Commit commit = commitNode.getCommit();
        setBranchList(commitNode.getReferringBranches());
        setCommitTimeStamp(commit.getLastModifyDate());
        setCommitter(commit.getLastModifier());
        setCommitMessage(commit.getCommitContent());
        setContextMenu();

        commitDisplayNode.addEventHandler(MouseEvent.MOUSE_EXITED, event -> cancelHighlightBranchList());
    }


    public void setContextMenu() {
        commitDisplayNode.setOnContextMenuRequested(new EventHandler<ContextMenuEvent>() {
            @Override
            public void handle(ContextMenuEvent event) {
                ContextMenu contextMenu = createContextMenu();
                contextMenu.show(commitDisplayNode, event.getScreenX(), event.getScreenY());
                event.consume();
            }
        });
    }


    public ContextMenu createContextMenu() {
        ContextMenu contextMenu = new ContextMenu();

        // Option #1 - show commit information (SHA1, message, commiter, date)
        MenuItem commitInfoMenuItem = new MenuItem("Show commit Information");
        commitInfoMenuItem.setOnAction(event -> Utility.ShowInfoDialog("Commit information", getCommit().getCompleteInformation()));


        // Option #2 - show SHA1 of parent commits
        MenuItem parentsSha1MenuItem = new MenuItem("Show SHA1 of parents");
        parentsSha1MenuItem.setOnAction(event -> Utility.ShowInfoDialog("Commit information", getCommitParents()));


        // Option #3 - show changes from previous (parent) commits
        MenuItem changesFromParentsMenuItem = new MenuItem("Show changes from parent commits");
        changesFromParentsMenuItem.setOnAction(event -> openChangesWindow());


        // Option #4 - show content of current commit
        MenuItem commitContentMenuItem = new MenuItem("Show commit content");
        commitContentMenuItem.setOnAction(event -> openCommitContentWindow());


        // Bonus: Option #5 - add a new branch pointing on this commit
        MenuItem addBranchMenuItem = new MenuItem("Add branch on this commit");
        addBranchMenuItem.setOnAction(event -> magitController.addBranchUI(getCommit().getSha1()));


        // Bonus: Option #6 - reset head branch to this commit
        MenuItem resetHeadBranchMenuItem = new MenuItem("Reset head branch to here");
        resetHeadBranchMenuItem.setOnAction(event -> magitController.resetHeadBranchUI(getCommit().getSha1()));


        // Bonus: Option #7 - merge the branch pointing on this commit with the head branch
        MenuItem mergeMenuItem = new MenuItem("Merge selected branch with head");
        mergeMenuItem.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                Branch branchSelected = (Branch) branchesList.getSelectionModel().getSelectedItem();
                if (branchSelected instanceof RemoteBranch)
                    Utility.ShowErrorDialog("It is not allowed to merge RB (Remote Branch)");
                else {
                    if (branchSelected != null)
                        magitController.mergeBranchesUI(branchSelected.getBranchName());
                    else
                        promptUserToSelectBranch();
                }
            }
        });


        // Bonus: Option #8 - delet a branch pointing on this commit
        MenuItem deleteBranchMenuItem = new MenuItem("Delete selected branch");
        deleteBranchMenuItem.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                Branch branchSelected = (Branch) branchesList.getSelectionModel().getSelectedItem();
                if (branchSelected instanceof RemoteBranch)
                    Utility.ShowErrorDialog("It is not allowed to delete RB (Remote Branch)");
                else {
                    if (branchSelected != null)
                        magitController.deleteBranchUI(branchSelected.getBranchName());
                    else
                        promptUserToSelectBranch();
                }
            }
        });


        contextMenu.getItems().addAll(commitInfoMenuItem, parentsSha1MenuItem, changesFromParentsMenuItem, commitContentMenuItem);
        contextMenu.getItems().addAll(new SeparatorMenuItem(), addBranchMenuItem, resetHeadBranchMenuItem, mergeMenuItem, deleteBranchMenuItem);
        if (getCommit().isOrphanCommit())
            contextMenu.getItems().removeAll(parentsSha1MenuItem, changesFromParentsMenuItem);
        return contextMenu;
    }

    // -------------------------------------------------------------------

    public void promptUserToSelectBranch() {
        Utility.ShowErrorDialog("Please select a branch first...");
        highlightBranchList();
    }

    // -------------------------------------------------------------------

    public void openChangesWindow() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/Delta/Delta.fxml"));
            Parent root = fxmlLoader.load();
            DeltaController parentChangesController = fxmlLoader.getController();
            parentChangesController.setMainController(magitController);

            // get blobs of current commit
            Folder currentCommitRoot = getRepository().getRootFolderByCommitSha1(getCommit().getSha1());
            Set<Blob> currentCommitBlobs = getRepository().getBlobsByRoot(currentCommitRoot);

            // generate Delta for first parent commit
            if(!getCommit().getFirstPrecedingSha1().isEmpty()) {
                Delta firstDelta = generateDeltaForParent(currentCommitBlobs, getCommit().getFirstPrecedingSha1());
                parentChangesController.setDeltaA(firstDelta);
            }

            // generate Delta for second parent commit
            if(!getCommit().getSecondPrecedingSha1().isEmpty()) {
                Delta secondDelta = generateDeltaForParent(currentCommitBlobs, getCommit().getSecondPrecedingSha1());
                parentChangesController.setDeltaB(secondDelta);
            }

            // initialize new stage
            parentChangesController.initializeCommitsDelta(getCommit().getFirstPrecedingSha1(), getCommit().getSecondPrecedingSha1());
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            String title = "Changes from previous commits";
            parentChangesController.setTreeTitle(title);
            stage.setTitle(title);
            stage.show();

        } catch (IOException e) {
            Utility.ShowErrorDialog(e.getMessage());
        }
    }


    public Delta generateDeltaForParent(Set<Blob> currentCommitBlobs, String parentSha1) {
        // get blobs of this parent commit
        Folder parentRoot = getRepository().getRootFolderByCommitSha1(parentSha1);
        Set<Blob> parentBlobs = getRepository().getBlobsByRoot(parentRoot);

        // generate Delta for this parent commit
        Delta parentDelta = new Delta();
        parentDelta.evaluateChanges(currentCommitBlobs, parentBlobs);

        return parentDelta;
    }

        // -------------------------------------------------------------------

    public void openCommitContentWindow() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/FileTree/FileTree.fxml"));
            Parent root = fxmlLoader.load();
            FileTreeController fileTreeController = fxmlLoader.getController();
            fileTreeController.setMainController(magitController);
            fileTreeController.setCommitSha1(getCommit().getSha1());
            fileTreeController.initializeFileTree();

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Commit content");
            stage.show();

        } catch (IOException e) {
            Utility.ShowErrorDialog(e.getMessage());
        }
    }






}
