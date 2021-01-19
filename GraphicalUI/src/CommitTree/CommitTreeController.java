package CommitTree;

import CommitTree.layout.CommitTreeLayout;
import CommitTree.node.CommitNode;
import Engine.Branches.Branch;
import Engine.GitObjects.Blob;
import Engine.GitObjects.Commit;
import Engine.GitObjects.Folder;
import Engine.GitObjects.GitObject;
import Engine.GitObjects.RepositoryObject;
import Magit.InternalController;
import com.fxgraph.edges.Edge;
import com.fxgraph.graph.Graph;
import com.fxgraph.graph.ICell;
import com.fxgraph.graph.Model;
import com.fxgraph.graph.PannableCanvas;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ScrollPane;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;
import java.util.*;


public class CommitTreeController extends InternalController implements Initializable, PropertyChangeListener {

    //-------------------------------------------------- //
    //------------------- Attributes ------------------- //
    //-------------------------------------------------- //

    private Graph tree = new Graph();
    @FXML private ScrollPane commitTreeScrollPane;

    //----------------------------------------------- //
    //------------------- Methods ------------------- //
    //----------------------------------------------- //

    public void initialize(URL location, ResourceBundle resources) {
        PannableCanvas canvas = tree.getCanvas();
        commitTreeScrollPane.setContent(canvas);
    }

    public void propertyChange(PropertyChangeEvent e) {
        if (getRepository().getAccessibleCommits().isEmpty())
            CleanTree();
        else
            InitializeCommitTree();
    }

    public void CleanTree() {
        tree.beginUpdate();
        tree.endUpdate();
        PannableCanvas canvas = tree.getCanvas();
        commitTreeScrollPane.setContent(canvas);
    }


    public void InitializeCommitTree(){
        tree = new Graph();
        createCommitTree();
    }


    private void createCommitTree() {
        List<Commit> accessibleCommits = getRepository().getAccessibleCommits();
        Map<String,List<Branch>> commitsToBranchesMap=getRepository().mapCommitsToBranches();

        List<CommitNode> commitNodeList = new ArrayList<>();
        for (Commit commit : accessibleCommits)
            commitNodeList.add(new CommitNode(magitController, commit, commitsToBranchesMap.get(commit.getSha1())));

        final Model model = tree.getModel();
        tree.beginUpdate();
        int listSize = commitNodeList.size();

        if (listSize == 1) {
            ICell nodeA = commitNodeList.get(0);
            model.addCell(nodeA);
        } else {
            for (int i = 0; i < listSize; i++) {
                for (int j = i + 1; j < listSize; j++) {
                    ICell nodeA = commitNodeList.get(i);
                    ICell nodeB = commitNodeList.get(j);

                    Commit commitA = ((CommitNode) nodeA).getCommit();
                    Commit commitB = ((CommitNode) nodeB).getCommit();

                    if (getRepository().checkCommitsParenthoodTwoSided(commitA, commitB)) {
                        if (!model.getAddedCells().contains(nodeA))
                            model.addCell(nodeA);
                        if (!model.getAddedCells().contains(nodeB))
                            model.addCell(nodeB);
                        final Edge edgeC12 = new Edge(nodeA, nodeB);
                        model.addEdge(edgeC12);
                    }
                }
            }
        }

        tree.getModel().getAddedCells().sort(new Comparator<ICell>() {
            @Override
            public int compare(ICell o1, ICell o2) {
                Commit commit1 = ((CommitNode) o1).getCommit();
                Commit commit2 = ((CommitNode) o2).getCommit();
                return commit1.compareTo(commit2);
            }
        });

        tree.endUpdate();
        tree.layout(new CommitTreeLayout(magitController));

        PannableCanvas canvas = tree.getCanvas();
        commitTreeScrollPane.setContent(canvas);

        Platform.runLater(() -> {
            tree.getUseViewportGestures().set(false);
            tree.getUseNodeGestures().set(false);
        });
    }

}
