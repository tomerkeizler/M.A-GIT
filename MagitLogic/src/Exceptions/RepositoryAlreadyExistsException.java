package Exceptions;

public class RepositoryAlreadyExistsException extends Exception {

    public RepositoryAlreadyExistsException(String repPath, Throwable err) {
        super("The path " + repPath + " already exists in the file system", err);
    }

}
