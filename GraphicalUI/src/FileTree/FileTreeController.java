package FileTree;

import Engine.GitObjects.Blob;
import Engine.GitObjects.Commit;
import Engine.GitObjects.Folder;
import Engine.GitObjects.RepositoryObject;
import FileTree.FileContent.FileContentController;
import Magit.InternalController;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.Set;


public class FileTreeController extends InternalController implements Initializable, PropertyChangeListener {

    //-------------------------------------------------- //
    //------------------- Attributes ------------------- //
    //-------------------------------------------------- //

    private String commitSha1 = null;

    @FXML private TreeView fileTree;
    @FXML private Label treeTitle;

    private final Image fileImage = new Image(getClass().getResourceAsStream("/resources/fileTree/file-icon.png"));
    private final Image folderImage = new Image(getClass().getResourceAsStream("/resources/fileTree/folder-icon.png"));

    //----------------------------------------------- //
    //------------------- Setters ------------------- //
    //----------------------------------------------- //

    public void setCommitSha1(String commitSha1) {
        this.commitSha1 = commitSha1;
    }

    //----------------------------------------------- //
    //------------------- Methods ------------------- //
    //----------------------------------------------- //

    public void initialize(URL location, ResourceBundle resources) {
        setActionWhenTreeItemSelected();
    }


    public void propertyChange(PropertyChangeEvent e) {
        initializeFileTree();
    }


    public void initializeFileTree() {
        Folder rootFolder;
        // this window is showing the content of the head branch
        if (commitSha1 == null) {
            treeTitle.setText("Head Branch: " + getRepository().getHeadBranch().getBranchName());
            rootFolder = getRepository().getCurrentCommitRootFolder();
            if (rootFolder == null)
                rootFolder = new Folder(getRepository().getRepositoryPath());
        }
        // this window is showing the content of a given commit*
        else {
            Commit com = getRepository().getCommitBySha1(commitSha1);
            treeTitle.setText("Content of " + com.toString());
            rootFolder = getRepository().getRootFolderByCommitSha1(commitSha1);
        }
        TreeItem<RepositoryObject> rootTreeItem = addTreeItemsRec(rootFolder);
        fileTree.setRoot(rootTreeItem);
    }


    private void setActionWhenTreeItemSelected() {
        fileTree.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<TreeItem<String>>() {
            @Override
            public void changed(ObservableValue<? extends TreeItem<String>> observable, TreeItem<String> oldValue, TreeItem<String> newValue) {
                try {
                    TreeItem treeItemSelected = ((TreeItem) fileTree.getSelectionModel().getSelectedItem());
                    if (treeItemSelected != null && treeItemSelected.getValue() instanceof Blob) {
                        Blob SelectedFile = (Blob) treeItemSelected.getValue();
                        String fileName = SelectedFile.getName();
                        String fileContent = SelectedFile.getFileContent();
                        showFileContent(fileName, fileContent);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }


    private TreeItem<RepositoryObject> addTreeItemsRec(RepositoryObject obj) {
        TreeItem<RepositoryObject> treeItem;
        // the current item is a folder
        if (obj instanceof Folder) {
            treeItem = new TreeItem<>(obj, new ImageView(folderImage));
            treeItem.setExpanded(true);
            for (RepositoryObject itemInsideFolder : ((Folder) obj).getFolderItems())
                treeItem.getChildren().add(addTreeItemsRec(itemInsideFolder));
        }
        // the current item is a blob
        else {
            treeItem = new TreeItem<>(obj, new ImageView(fileImage));
        }
        return treeItem;
    }


    private void showFileContent(String fileName, String fileContent) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/FileTree/FileContent/FileContent.fxml"));
        Parent root = fxmlLoader.load();

        FileContentController fileContentController = fxmlLoader.getController();
        fileContentController.setFileDetails(fileName, fileContent);

        Stage stage = new Stage();
        stage.setScene(new Scene(root));
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("File content");
        stage.show();
    }


}
