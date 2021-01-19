package Exceptions;

public class NoCurrentCommitException extends Exception {

    public NoCurrentCommitException(Throwable err) {
        super("There is no current commit at the moment. Please make a commit first", err);
    }

}
