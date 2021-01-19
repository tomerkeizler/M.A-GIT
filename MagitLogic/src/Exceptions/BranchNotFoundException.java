package Exceptions;

public class BranchNotFoundException extends Exception {

    public BranchNotFoundException(String branchName, Throwable err) {
        super("There is no branch named \"" + branchName + "\" in the repository", err);
    }

}
