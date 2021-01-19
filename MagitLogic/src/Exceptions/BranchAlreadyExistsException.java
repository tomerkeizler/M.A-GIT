package Exceptions;

public class BranchAlreadyExistsException extends Exception {

    public BranchAlreadyExistsException(String branchName, Throwable err) {
        super("There is already a branch named \"" + branchName + "\" in the repository", err);
    }

}
