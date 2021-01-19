package Exceptions;

public class LibraryContainsNonRepositoryContentException extends Exception {

    public LibraryContainsNonRepositoryContentException(String path, Throwable err) {
        super("The path " + path + " contains other content which is not a M.A.Git repository", err);
    }

}
