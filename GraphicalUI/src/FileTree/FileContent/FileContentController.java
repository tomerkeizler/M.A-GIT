package FileTree.FileContent;

import Engine.MergeObjects.Conflict;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;


public class FileContentController implements Initializable {

    //-------------------------------------------------- //
    //------------------- Attributes ------------------- //
    //-------------------------------------------------- //

    @FXML private Label fileName;
    @FXML private TextArea fileContent;

    //----------------------------------------------- //
    //------------------- Methods ------------------- //
    //----------------------------------------------- //

    public void initialize(URL location, ResourceBundle resources) {
    }


    public void setFileDetails(String fileName, String fileContent) {
        this.fileName.setText(fileName);
        this.fileContent.setText(fileContent);
    }






}