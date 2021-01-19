package Exceptions;

public class IllegalPushException extends Exception {

    public IllegalPushException(Throwable err) {
        super("Push operation is illegal - because changes were made in RR since the last time it was synced to LR", err);
    }

}

