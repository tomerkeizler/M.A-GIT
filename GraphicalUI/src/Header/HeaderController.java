package Header;

import Magit.InternalController;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import javax.naming.Binding;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;
import java.util.ResourceBundle;


public class HeaderController extends InternalController implements Initializable, PropertyChangeListener {

    //-------------------------------------------------- //
    //------------------- Attributes ------------------- //
    //-------------------------------------------------- //

    private SimpleBooleanProperty isRepositoryActive = new SimpleBooleanProperty(false);
    private SimpleBooleanProperty hasRemoteRepository = new SimpleBooleanProperty(false);

    @FXML private Menu branchMenu;
    @FXML private MenuItem fetchButton;
    @FXML private MenuItem pullButton;
    @FXML private MenuItem pushButton;

    @FXML private Label localRepDetailsLabel;
    @FXML private Label remoteRepDetailsLabel;

    @FXML private MenuButton usernameButton;
    @FXML private Button wcButton;
    @FXML private Button commitButton;
    @FXML private Button mergeButton;

    //----------------------------------------------- //
    //------------------- Methods ------------------- //
    //----------------------------------------------- //

    public void initialize(URL location, ResourceBundle resources) {
        Image userImage = new Image(getClass().getResourceAsStream("/resources/mainToolbar/user-icon.png"));
        usernameButton.setGraphic(new ImageView(userImage));

        Image wcImage = new Image(getClass().getResourceAsStream("/resources/mainToolbar/wc-icon.png"));
        wcButton.setGraphic(new ImageView(wcImage));

        Image commitImage = new Image(getClass().getResourceAsStream("/resources/mainToolbar/commit-icon.png"));
        commitButton.setGraphic(new ImageView(commitImage));

        Image mergeImage = new Image(getClass().getResourceAsStream("/resources/mainToolbar/merge-icon.png"));
        mergeButton.setGraphic(new ImageView(mergeImage));

        branchMenu.disableProperty().bind(Bindings.not(isRepositoryActive));
        wcButton.disableProperty().bind(Bindings.not(isRepositoryActive));
        commitButton.disableProperty().bind(Bindings.not(isRepositoryActive));
        mergeButton.disableProperty().bind(Bindings.not(isRepositoryActive));

        fetchButton.disableProperty().bind(Bindings.not(hasRemoteRepository));
        pullButton.disableProperty().bind(Bindings.not(hasRemoteRepository));
        pushButton.disableProperty().bind(Bindings.not(hasRemoteRepository));
    }

    public void propertyChange(PropertyChangeEvent e) {
        String localRepName = getRepository().getRepositoryName();
        String localRepPath = getRepository().getRepositoryPath().toString();
        String localRepDetails = String.format("Repository: %s (%s)", localRepName, localRepPath);
        String remoteRepDetails;

        if (getRepository().getRemoteRepository() == null) {
            remoteRepDetails = "There is no RR (Remote Repository)";
        } else {
            localRepDetails = "Local " + localRepDetails;
            String remoteRepName = getRepository().getRemoteRepository().getRepositoryName();
            String remoteRepPath = getRepository().getRemoteRepository().getRepositoryPath().toString();
            remoteRepDetails = String.format("Remote Repository: %s (%s)", remoteRepName, remoteRepPath);
        }

        localRepDetailsLabel.textProperty().setValue(localRepDetails);
        remoteRepDetailsLabel.textProperty().setValue(remoteRepDetails);
        isRepositoryActive.setValue(true);
        hasRemoteRepository.setValue(getRepository().hasRemoteRepository());
    }

    public void updateUsernameLabel(String username) {
        usernameButton.textProperty().setValue(username);
    }

    //---------------------------------------------- //
    //-------------- General menu -------------- //
    //---------------------------------------------- //

    @FXML
    public void updateUsernameUI() {
        magitController.updateUsernameUI();
    }

    @FXML
    public void exitApp() {
        Platform.exit();
    }

    //--------------------------------------------- //
    //-------------- Repository menu -------------- //
    //--------------------------------------------- //

    @FXML
    public void createRepositoryUI() {
        magitController.createRepositoryUI();
    }

    @FXML
    public void switchRepositoryUI() {
        magitController.switchRepositoryUI();
    }

    @FXML
    public void loadRepositoryFromXmlUI() {
        magitController.loadRepositoryFromXmlUI();
    }

    //--------------------------------------------- //
    //--------------- Branches menu --------------- //
    //--------------------------------------------- //

    @FXML
    public void addBranchOnCurrentCommitUI() {
        magitController.addBranchUI(getRepository().getCurrentCommitSha1());
    }

    @FXML
    public void addBranchOnCommitUI() {
        magitController.addBranchUI("");
    }

    @FXML
    public void deleteBranchUI() {
        magitController.deleteBranchUI("");
    }

    @FXML
    public void checkOutUI() {
        magitController.checkOutUI("");
    }

    @FXML
    public void resetHeadBranchUI() {
        magitController.resetHeadBranchUI("");
    }

    //----------------------------------- //
    //------------- Toolbar ------------- //
    //----------------------------------- //

    @FXML
    public void addCommitUI() {
        magitController.addCommitUI();
    }

    @FXML
    public void mergeBranchesUI() {
        magitController.mergeBranchesUI("");
    }

    @FXML
    public void showWorkingCopyStatusUI() {
        magitController.showWorkingCopyStatusUI();
    }

    //-------------------------------------------- //
    //------------ Collaboration menu ------------ //
    //-------------------------------------------- //

    @FXML
    public void cloneRepositoryUI() {
        magitController.cloneRepositoryUI();
    }

    @FXML
    public void fetchUI() {
        magitController.fetchUI();
    }

    @FXML
    public void pullUI() {
        magitController.pullUI();
    }

    @FXML
    public void pushUI() {
        magitController.pushUI();
    }

}
