package CommitTree.node;

import Engine.Branches.Branch;
import Engine.GitObjects.Blob;
import Engine.GitObjects.Commit;
import Engine.GitObjects.Folder;
import Engine.GitObjects.GitObject;
import Engine.GitObjects.RepositoryObject;
import Magit.MagitController;
import com.fxgraph.cells.AbstractCell;
import com.fxgraph.graph.Graph;
import com.fxgraph.graph.IEdge;
import javafx.beans.binding.DoubleBinding;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;

import java.io.IOException;
import java.net.URL;
import java.util.List;

public class CommitNode extends AbstractCell {

    //-------------------------------------------------- //
    //------------------- Attributes ------------------- //
    //-------------------------------------------------- //

    private MagitController magitController;
    private CommitNodeController commitNodeController;
    private Commit commit;
    private boolean isLocated = false;
    private List<Branch> referringBranches;

    //--------------------------------------- //
    //------------- Constructor ------------- //
    //--------------------------------------- //

    public CommitNode(MagitController magitController, Commit commit, List<Branch> referringBranches) {
        this.magitController = magitController;
        this.commit = commit;
        this.referringBranches = referringBranches;
    }

    //----------------------------------------------- //
    //------------- Getters ans Setters ------------- //
    //----------------------------------------------- //

    public boolean isLocated() {
        return isLocated;
    }

    public void setLocated(boolean located) {
        isLocated = located;
    }

    public Commit getCommit() {
        return commit;
    }

    public String getTimeStamp()
    {
        return commit.getLastModifyDate();
    }

    public List<Branch> getReferringBranches() {
        return referringBranches;
    }

    //----------------------------------------------- //
    //------------------- Methods ------------------- //
    //----------------------------------------------- //

    @Override
    public Region getGraphic(Graph graph) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader();
            URL url = getClass().getResource("/CommitTree/node/commitNode.fxml");
            fxmlLoader.setLocation(url);
            GridPane root = fxmlLoader.load(url.openStream());

            commitNodeController = fxmlLoader.getController();
            commitNodeController.setMainController(magitController);
            commitNodeController.setCommitNode(this);
            return root;

        } catch (IOException e) {
            return new Label("Error when tried to create graphic node !");
        }
    }


    @Override
    public DoubleBinding getXAnchor(Graph graph, IEdge edge) {
        final Region graphic = graph.getGraphic(this);
        return graphic.layoutXProperty().add(commitNodeController.getCircleRadius());
    }

    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CommitNode that = (CommitNode) o;

        return getTimeStamp() != null ? getTimeStamp().equals(that.getTimeStamp()) : that.getTimeStamp() == null;
    }


    @Override
    public int hashCode() {
        return getTimeStamp() != null ? getTimeStamp().hashCode() : 0;
    }

}
