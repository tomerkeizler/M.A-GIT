package Exceptions;

public class HeadBranchForbbidenDeletionException extends Exception {

    public HeadBranchForbbidenDeletionException(String branchName, Throwable err) {
        super("The branch \"" + branchName + "\" is the current head branch - so it cannot be deleted", err);
    }

}
