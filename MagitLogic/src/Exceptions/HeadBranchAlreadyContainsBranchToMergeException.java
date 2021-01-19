package Exceptions;

public class HeadBranchAlreadyContainsBranchToMergeException extends Exception {

    public HeadBranchAlreadyContainsBranchToMergeException(String oursBranchName, String theirsBrandhName, Throwable err) {
        super("The head branch \"" + oursBranchName + "\" already contains branch \"" + theirsBrandhName + "\", so there is nothing to merge", err);
    }

}
