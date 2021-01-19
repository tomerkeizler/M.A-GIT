package Exceptions;

public class IllegalPullException extends Exception {

    public IllegalPullException(Throwable err) {
        super("Pull operation is illegal - because there are unpushed changes in the RTB in LR ", err);
    }

}
