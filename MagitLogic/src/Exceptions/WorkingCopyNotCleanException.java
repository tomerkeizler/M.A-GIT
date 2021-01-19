package Exceptions;

public class WorkingCopyNotCleanException extends Exception {

    public WorkingCopyNotCleanException(String operation, Throwable err) {
        super("The Working Copy " + (operation.equals("push") ? "of RR (Remote Repository) " : "") + "must be clean before performing a " + operation + " operation", err);
    }

}
