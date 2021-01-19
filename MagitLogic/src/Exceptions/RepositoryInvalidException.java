package Exceptions;

public class RepositoryInvalidException extends Exception {

    public RepositoryInvalidException(String repPath, Throwable err) {
        super("The path " + repPath + " is not a M.A.Git repository", err);
    }

}
