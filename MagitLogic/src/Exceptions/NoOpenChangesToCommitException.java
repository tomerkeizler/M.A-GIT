package Exceptions;

public class NoOpenChangesToCommitException extends Exception {

    public NoOpenChangesToCommitException(Throwable err) {
        super("There are no changes to commit right now", err);
    }

}
