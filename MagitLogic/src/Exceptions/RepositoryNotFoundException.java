package Exceptions;

public class RepositoryNotFoundException extends Exception {

    public RepositoryNotFoundException(String repPath, Throwable err) {
        super("The path " + repPath + " does not exist in the file system", err);
    }

}
