package net.survivalboom.sbds.api.utils.sixseven;

public class DinosaurDeathException extends RuntimeException {

    public DinosaurDeathException() {
        super("*TIMURishche died from cringe*");
    }

    public DinosaurDeathException(String message) {
        super(message);
    }

    public DinosaurDeathException(String message, Throwable cause) {
        super(message, cause);
    }

}
