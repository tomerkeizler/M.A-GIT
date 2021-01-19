package Exceptions;

public class CollaborationEmptyOperationException extends Exception {

    public CollaborationEmptyOperationException(String operation, Throwable err) {
        super("There is nothing to " + operation, err);
    }

}
