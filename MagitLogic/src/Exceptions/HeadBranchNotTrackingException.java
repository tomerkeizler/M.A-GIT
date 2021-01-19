package Exceptions;

public class HeadBranchNotTrackingException extends Exception {

    public HeadBranchNotTrackingException(Throwable err) {
        super("The head branch has to be RTB (Remote Tracking Branch)", err);
    }

}
