package Exceptions;

public class NoActiveRepositoryException extends Exception {

    public NoActiveRepositoryException(Throwable err) {
        super("There is no active repository at the moment. Please activate a repository first", err);
    }

}
