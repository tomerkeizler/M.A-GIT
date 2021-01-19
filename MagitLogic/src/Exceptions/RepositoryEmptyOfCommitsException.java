package Exceptions;

public class RepositoryEmptyOfCommitsException extends Exception {

    public RepositoryEmptyOfCommitsException(Throwable err) {
        super("There are no commits in the whole repository. Please make a commit first", err);
    }

}
