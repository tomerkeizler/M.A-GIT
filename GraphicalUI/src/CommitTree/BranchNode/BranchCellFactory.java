package CommitTree.BranchNode;

import Engine.Branches.Branch;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.util.Callback;


public class BranchCellFactory implements Callback<ListView<Branch>, ListCell<Branch>> {

    @Override
    public ListCell<Branch> call(ListView<Branch> listview) {
        return new BranchNode();
    }

}