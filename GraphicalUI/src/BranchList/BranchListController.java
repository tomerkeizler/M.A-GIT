package BranchList;

import Engine.Branches.Branch;
import Engine.Branches.RemoteBranch;
import Engine.Branches.RemoteTrackingBranch;
import Magit.InternalController;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ToolBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.Set;


public class BranchListController extends InternalController implements Initializable, PropertyChangeListener {

    //-------------------------------------------------- //
    //------------------- Attributes ------------------- //
    //-------------------------------------------------- //

   // @FXML
    //private ToolBar branchesBar;
    @FXML
    private VBox branchesBarVbox;

    private final Image checkoutImage = new Image(getClass().getResourceAsStream("/resources/branchList/checkout-icon-small.png"));
    private final Image deleteImage = new Image(getClass().getResourceAsStream("/resources/branchList/delete-icon-small.png"));
    private final Image mergeImage = new Image(getClass().getResourceAsStream("/resources/branchList/merge-icon-small.png"));
    private final Image resetImage = new Image(getClass().getResourceAsStream("/resources/branchList/reset-icon-small.png"));

    //----------------------------------------------- //
    //------------------- Methods ------------------- //
    //----------------------------------------------- //

    public void initialize(URL location, ResourceBundle resources) {
    }


    public void propertyChange(PropertyChangeEvent e) {
        branchesBarVbox.getChildren().clear();
        Set<Branch> allBranches = getRepository().getBranches();

        for (Branch branch : allBranches) {
            MenuButton branchMenuButton = new MenuButton(branch.getBranchName());
            branchesBarVbox.getChildren().add(branchMenuButton);
            branchMenuButton.prefWidthProperty().bind(Bindings.multiply(0.85, branchesBarVbox.widthProperty()));
            branchMenuButton.setMaxHeight(branchMenuButton.getPrefWidth());
            //------------------- identifying branch type ------------------- //

            // for RTB (Remote Tracking Branches)
            if (branch instanceof RemoteTrackingBranch)
                branchMenuButton.getStyleClass().add("RTB");

                // for RB (Remote Branches)
            else if (branch instanceof RemoteBranch)
                branchMenuButton.getStyleClass().add("RB");

                // for local branches
            else
                branchMenuButton.getStyleClass().add("LB");

            //------------------- identifying head branch ------------------- //

            // for head branch
            if (!(branch instanceof RemoteBranch) && getRepository().isHeadBranch(branch.getBranchName())) {
                MenuItem resetHeadOption = new MenuItem("Reset head branch", new ImageView(resetImage));
                resetHeadOption.setOnAction(event -> magitController.resetHeadBranchUI(""));
                branchMenuButton.getItems().add(resetHeadOption);
                branchMenuButton.getStyleClass().add("HEAD");
            }

            // for non-head branches
            else {
                MenuItem checkoutOption = new MenuItem("Checkout", new ImageView(checkoutImage));
                checkoutOption.setOnAction(event -> magitController.checkOutUI(branch.getBranchName()));

                MenuItem deleteOption = new MenuItem("Delete branch", new ImageView(deleteImage));
                deleteOption.setOnAction(event -> magitController.deleteBranchUI(branch.getBranchName()));

                MenuItem mergeOption = new MenuItem("Merge with head", new ImageView(mergeImage));
                mergeOption.setOnAction(event -> magitController.mergeBranchesUI(branch.getBranchName()));

                branchMenuButton.getItems().addAll(checkoutOption, deleteOption, mergeOption);
                if (branch instanceof RemoteBranch) {
                    branchMenuButton.getItems().removeAll(deleteOption, mergeOption);
                }
            }
        }
    }

}
