package CommitTree.BranchNode;

import Engine.Branches.Branch;
import javafx.scene.control.ListCell;


public class BranchNode extends ListCell<Branch> {

    //-------------------------------------------------- //
    //------------------- Attributes ------------------- //
    //-------------------------------------------------- //

    private String branchName;

    //----------------------------------------------- //
    //------------------- Methods ------------------- //
    //----------------------------------------------- //

    public BranchNode() {
    }


    @Override
    protected void updateItem(Branch item, boolean empty) {
        super.updateItem(item, empty);
        this.setText((item == null || empty) ? "" : item.getBranchName());
    }


    @Override
    public String toString(){
        return branchName;
    }

}
