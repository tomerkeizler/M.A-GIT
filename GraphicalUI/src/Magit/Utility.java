package Magit;

import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import java.io.File;
import java.nio.file.Path;
import java.util.*;


public class Utility {

    //------------------------------------------------------ //
    //---------- Methods for file/folder choosing ---------- //
    //------------------------------------------------------ //

    public static File LocateFileXML() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Load Repository from XML file");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML Files", "*.xml"));
        File xmlFile = fileChooser.showOpenDialog(null);
        return xmlFile;
    }


    public static File LocateDirectory(String title) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle(title);
        File dir = directoryChooser.showDialog(null);
        return dir;
    }

    //------------------------------------------------ //
    //-------------- Methods for alerts -------------- //
    //------------------------------------------------ //

    public static String ShowConfirmationDialog(String title, String header, String content) {
        Alert dialog = new Alert(Alert.AlertType.CONFIRMATION);
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        dialog.setContentText(content);

        dialog.getButtonTypes().setAll(new ButtonType("Proceed"), new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE));
        Optional<ButtonType> choice = dialog.showAndWait();
        return choice.get().getText();
    }


    public static String ShowConfirmationDialogThreeOptions(String title, String header, String content, String firstOption, String secondOption) {
        Alert dialog = new Alert(Alert.AlertType.CONFIRMATION);
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        dialog.setContentText(content);

        dialog.getButtonTypes().setAll(new ButtonType(firstOption), new ButtonType(secondOption), new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE));
        Optional<ButtonType> choice = dialog.showAndWait();
        return choice.get().getText();
    }

    // -------------------------------------------------------------------

    public static void ShowDialog(Alert.AlertType alertType, String title, String header, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }


    public static void ShowInfoDialog(String header, String content) {
        ShowDialog(Alert.AlertType.INFORMATION, "M.A.GIT system", header, content);
    }


    public static void ShowErrorDialog(String errorContent) {
        ShowDialog(Alert.AlertType.ERROR, "Error", "An error has occurred!", errorContent);
    }

    // -------------------------------------------------------------------

    public static String ShowTextDialog(String title, String header, String content, boolean isValidityCheckNeeded, boolean isCancelPossible) {
        TextInputDialog dialog = new TextInputDialog("");
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        dialog.setContentText(content);
        Optional<String> result = dialog.showAndWait();

        // the dialog was confirmed by the user
        if (result.isPresent()) {

            // the text is empty
            if (result.get().isEmpty()) {
                Utility.ShowErrorDialog("Please type at least one char");
                return ShowTextDialog(title, header, content, isValidityCheckNeeded, isCancelPossible);

                // the text is not valid
            } else if (isValidityCheckNeeded && !result.get().matches("[^\\\\/:*?\"<>|]+")) {
                Utility.ShowErrorDialog("Invalid input! The following chars are not allowed: \\ / : * ? \" < > |");
                return ShowTextDialog(title, header, content, isValidityCheckNeeded, isCancelPossible);

            } else
                return result.get();

            // the dialog was not confirmed by the user (exit dialog)
        } else {
            if (isCancelPossible)
                return null;
            else
                return ShowTextDialog(title, header, content, isValidityCheckNeeded, isCancelPossible);
        }
    }

    // -------------------------------------------------------------------

    public static String ShowChoiceDialog(String title, String header, List<String> choices) {
        ChoiceDialog<String> dialog = new ChoiceDialog<>("", choices);
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        Optional<String> result = dialog.showAndWait();

        if (result.isPresent()) // the dialog was confirmed by the user
            return result.get();
        else // the dialog was not confirmed by the user (exit dialog)
            return null;
    }

}
