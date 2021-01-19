package Exceptions;

public class HeadBranchAlreadyActiveException extends Exception {

    public HeadBranchAlreadyActiveException(String branchName, Throwable err) {
        super("The branch \"" + branchName + "\" is already the head branch", err);
    }

}
