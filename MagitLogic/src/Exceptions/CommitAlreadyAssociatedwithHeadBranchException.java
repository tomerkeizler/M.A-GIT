package Exceptions;

public class CommitAlreadyAssociatedwithHeadBranchException extends Exception {

    public CommitAlreadyAssociatedwithHeadBranchException(Throwable err) {
        super("The commit you have chosen is already associated with the head branch", err);
    }

}
