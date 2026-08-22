package exception;

public class InvalidEventException extends RuntimeException {
    public InvalidEventException() {
        super();
    }

    public InvalidEventException(String message) {
        super(message);
    }
}
