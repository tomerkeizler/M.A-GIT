package Exceptions;

public class NoRemoteRepositoryException extends Exception {

    public NoRemoteRepositoryException(Throwable err) {
        super("Operation illegal!\nThere is no RR (Remote Repository) in the current repository", err);
    }

}
