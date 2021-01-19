package ConflictSolver;

import Engine.MergeObjects.Conflict;
import Magit.Utility;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import javax.swing.*;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;


public class ConflictSolverController implements Initializable {

    //-------------------------------------------------- //
    //------------------- Attributes ------------------- //
    //-------------------------------------------------- //

    // Logical information
    private List<Conflict> conflicts;

    // Graphical components
    @FXML private ToolBar conflictList;
    @FXML private VBox conflictListVbox;

    @FXML private Label conflictTitle;
    @FXML private Label conflictDetails;

    @FXML private Label oursBranchLabel;
    @FXML private Label oursActionLabel;

    @FXML private Label theirsBranchLabel;
    @FXML private Label theirsActionLabel;

    @FXML private TextArea ancestorContent;
    @FXML private TextArea oursContent;
    @FXML private TextArea theirsContent;
    @FXML private TextArea finalContent;

    @FXML private Button saveButton;
    @FXML private Button deleteButton;

    //----------------------------------------------- //
    //------------------- Methods ------------------- //
    //----------------------------------------------- //

    public void initialize(URL location, ResourceBundle resources) {
    }


    public void setMergeDetails(List<Conflict> conflicts, String oursBranch, String theirsBranch) {
        // show name of the two branches
        this.oursBranchLabel.setText("Ours branch: " + oursBranch);
        this.theirsBranchLabel.setText("Theirs branch: " + theirsBranch);

        // create a button for each conflict
        conflictListVbox.getChildren().clear();
        this.conflicts = conflicts;
        for (Conflict con : conflicts) {
            String fileName = con.getFilePath().getFileName().toString();
            Button conflictButton = new Button(fileName);
            conflictButton.setOnAction(event -> showConflict(con));
            conflictListVbox.getChildren().add(conflictButton);
            conflictButton.minWidthProperty().bind(Bindings.multiply(0.8, conflictListVbox.widthProperty()));
        }
        if (!conflicts.isEmpty())
            showConflict(conflicts.get(0));
    }


    public void showConflict(Conflict con) {
        // adjust the screen to the conflict (already solved or not)
        if (con.isSolved()) {
            adjustScreenToConflict(true);
        } else {
            adjustScreenToConflict(false);
        }

        // show content of conflict
        conflictDetails.setText(con.toString());
        oursActionLabel.setText("file was " + con.getOursAction());
        theirsActionLabel.setText("file was " + con.getTheirsAction());

        ancestorContent.setText(con.getAncestorContent());
        oursContent.setText(con.getOursContent());
        theirsContent.setText(con.getTheirsContent());
        finalContent.clear();

        // set action of buttons
        saveButton.setOnAction(event -> handleSolving(con,true, finalContent.textProperty().getValue()));
        deleteButton.setOnAction(event -> handleSolving(con,false, ""));
    }


    public void handleSolving(Conflict con, boolean saveFile, String finalContent) {
        con.solveManually(saveFile, finalContent);
        adjustScreenToConflict(true);
        if (conflicts.stream().allMatch(Conflict::isSolved)) {
            Utility.ShowInfoDialog("All conflicts were solved!", "");
            ((Stage) this.saveButton.getScene().getWindow()).close();
        }
    }


    public void adjustScreenToConflict(boolean solved) {
        String title;
        Background green = new Background(new BackgroundFill(Color.GREEN, CornerRadii.EMPTY, Insets.EMPTY));
        Background red = new Background(new BackgroundFill(Color.RED, CornerRadii.EMPTY, Insets.EMPTY));
        if (solved) {
            title = "This conflict is solved";
            conflictTitle.setBackground(green);
            conflictDetails.setBackground(green);
        } else {
            title = "This conflict was not solved yet";
            conflictTitle.setBackground(red);
            conflictDetails.setBackground(red);
        }
        conflictTitle.setText(title);
        saveButton.setDisable(solved);
        deleteButton.setDisable(solved);
        finalContent.setEditable(!solved);
    }






}