package CommitTree.layout;

import java.util.*;
import Engine.GitObjects.Blob;
import Engine.GitObjects.Commit;
import Engine.GitObjects.Folder;
import Engine.GitObjects.GitObject;
import Engine.GitObjects.RepositoryObject;
import CommitTree.node.CommitNode;
import com.fxgraph.layout.Layout;
import com.fxgraph.graph.Graph;
import com.fxgraph.graph.ICell;
import Magit.MagitController;


public class CommitTreeLayout implements Layout {

    //-------------------------------------------------- //
    //------------------- Attributes ------------------- //
    //-------------------------------------------------- //

    private MagitController magitController;

    //----------------------------------------------- //
    //----------------- Constructor ----------------- //
    //----------------------------------------------- //

    public CommitTreeLayout(MagitController magitControllerArgument) {
        magitController = magitControllerArgument;
    }

    //----------------------------------------------- //
    //------------------- Methods ------------------- //
    //----------------------------------------------- //

    @Override
    public void execute(Graph graph) {
        final List<ICell> cells = graph.getModel().getAllCells();
        List<Commit> commitsPointedByBranches = magitController.getMagit().getRepository().getCurrentCommitsOfBranches();
        Collections.sort(commitsPointedByBranches);

        for (int i = 0, columCounter = 0; i < commitsPointedByBranches.size(); i++) {
            Commit pointedCommit = commitsPointedByBranches.get(i);
            CommitNode c = (CommitNode) findCellOfCommitNode(pointedCommit, cells);

            if (!c.isLocated()) {
                int YPosition = findYPosition(c, cells);
                int XPosition = columCounter * 50 + 10;
                graph.getGraphic(c).relocate(XPosition, YPosition * 70 + 70);
                c.setLocated(true);
                columCounter += parentsPosition(XPosition, c, cells, graph);
                columCounter++;
            }
        }
    }


    private int parentsPosition(int XPosition, CommitNode node, List<ICell> cells, Graph tree) {
        Commit commit = node.getCommit();
        Commit firstParent = magitController.getMagit().getRepository().getCommitBySha1(commit.getFirstPrecedingSha1());
        Commit secondParent = magitController.getMagit().getRepository().getCommitBySha1(commit.getSecondPrecedingSha1());

        // there are two parent commits
        if (firstParent != null && secondParent != null && firstParent.compareTo(secondParent) <= 0) {
            Commit temp = firstParent;
            firstParent = secondParent;
            secondParent = temp;
        }
        // there are no parent commits
        else if (firstParent == null && secondParent == null)
            return 0;

        CommitNode firstParentCommitNode = (CommitNode) findCellOfCommitNode(firstParent, cells);
        int first = 0, second = 0;

        if (!firstParentCommitNode.isLocated()) {
            int YPosition = findYPosition(firstParentCommitNode, cells);
            tree.getGraphic(firstParentCommitNode).relocate(XPosition, YPosition * 70 + 70);
            firstParentCommitNode.setLocated(true);
            first = parentsPosition(XPosition, firstParentCommitNode, cells, tree);
        }

        if (secondParent != null) {
            CommitNode secondParentCommitNode = (CommitNode) findCellOfCommitNode(secondParent, cells);
            if (!secondParentCommitNode.isLocated()) {
                int YPosition = findYPosition(secondParentCommitNode, cells);
                tree.getGraphic(secondParentCommitNode).relocate(XPosition + 50, YPosition * 70 + 70);
                secondParentCommitNode.setLocated(true);
                second = parentsPosition(XPosition + 50, secondParentCommitNode, cells, tree) + 1;
            }
        }
        return first + second;
    }


    private ICell findCellOfCommitNode(Commit commit, List<ICell> cells) {
        ICell commitNodeCell = null;
        for (ICell cell : cells)
            if (((CommitNode) cell).getCommit().equals(commit))
                commitNodeCell = cell;
        return commitNodeCell;
    }


    private int findYPosition(CommitNode c, List<ICell> cells) {
        for (int i = 0; i < cells.size(); i++)
            if (((CommitNode) cells.get(i)).equals(c))
                return i;
        return -1;
    }

}
