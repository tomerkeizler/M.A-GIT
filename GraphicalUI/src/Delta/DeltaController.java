package Delta;

import Engine.Delta;
import Magit.InternalController;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.Set;


public class DeltaController extends InternalController implements Initializable {

    //-------------------------------------------------- //
    //------------------- Attributes ------------------- //
    //-------------------------------------------------- //

    @FXML private TreeView fileTree;
    @FXML private Label treeTitle;

    private Delta deltaA = new Delta();
    private Delta deltaB = new Delta();

    private final Image fileCreatedImage = new Image(getClass().getResourceAsStream("/resources/workingCopyChanges/file-created-icon.png"));
    private final Image fileUpdatedImage = new Image(getClass().getResourceAsStream("/resources/workingCopyChanges/folder-updated-icon.png"));
    private final Image fileDeletedImage = new Image(getClass().getResourceAsStream("/resources/workingCopyChanges/file-deleted-icon.png"));

    //----------------------------------------------- //
    //------------------- Setters ------------------- //
    //----------------------------------------------- //

    public void setDeltaA(Delta delta) {
        this.deltaA = delta;
    }

    public void setDeltaB(Delta delta) {
        this.deltaB = delta;
    }

    //----------------------------------------------- //
    //------------------- Methods ------------------- //
    //----------------------------------------------- //

    public void initialize(URL location, ResourceBundle resources) {
    }

    public void setTreeTitle(String treeTitle) {
        this.treeTitle.setText(treeTitle);
    }


    public void initializeWorkingCopyDelta() {
        TreeItem<String> rootTreeItem = addDeltaCategories("Working Copy", deltaA);
        fileTree.setRoot(rootTreeItem);
    }


    public void initializeCommitsDelta(String sha1OfParentA, String sha1OfParentB) {
        TreeItem<String> rootTreeItem = new TreeItem<>("Commit parents");
        rootTreeItem.setExpanded(true);
        fileTree.setRoot(rootTreeItem);

        if (deltaA.isDeltaNotEmpty()) {
            TreeItem<String> deltaTreeItemA = addDeltaCategories(getCommitDescription(sha1OfParentA), deltaA);
            rootTreeItem.getChildren().add(deltaTreeItemA);
        }

        if (deltaB.isDeltaNotEmpty()) {
            TreeItem<String> deltaTreeItemB = addDeltaCategories(getCommitDescription(sha1OfParentB), deltaB);
            rootTreeItem.getChildren().add(deltaTreeItemB);
        }
    }


    public String getCommitDescription(String sha1) {
        return getRepository().getCommitBySha1(sha1).toString();
    }


    private TreeItem<String> addDeltaCategories(String rootTitle, Delta delta) {
        TreeItem<String> rootTreeItem = new TreeItem<>(rootTitle);
        rootTreeItem.setExpanded(true);

        addFilesToCategory(rootTreeItem, "New files", delta.getCreatedFilesPaths(), fileCreatedImage);
        addFilesToCategory(rootTreeItem, "Updated files", delta.getUpdatedFilesPaths(), fileUpdatedImage);
        addFilesToCategory(rootTreeItem, "Deleted files", delta.getDeletedFilesPaths(), fileDeletedImage);

        return rootTreeItem;
    }


    private void addFilesToCategory(TreeItem<String> rootTreeItem, String filesCategory, Set<String> files, Image categoryImage) {
        // add this category to the root of the main tree
        TreeItem<String> CategoryTreeItem = new TreeItem<>(filesCategory, new ImageView(categoryImage));
        CategoryTreeItem.setExpanded(true);
        rootTreeItem.getChildren().add(CategoryTreeItem);

        // add files of this category
        if (!files.isEmpty()) {
            for (String filePath : files)
                CategoryTreeItem.getChildren().add(new TreeItem<>(filePath));
        } else {
            CategoryTreeItem.getChildren().add(new TreeItem<>("No files"));
        }
    }

}
