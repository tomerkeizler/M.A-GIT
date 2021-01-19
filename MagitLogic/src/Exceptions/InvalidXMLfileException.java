package Exceptions;

public class InvalidXMLfileException extends Exception {

    public InvalidXMLfileException(String errorsFoundInFile, Throwable err) {
        super(errorsFoundInFile, err);
    }

}
