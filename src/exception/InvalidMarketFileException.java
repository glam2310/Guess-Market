package exception;

public class InvalidMarketFileException extends RuntimeException {
    public InvalidMarketFileException(String message) {
        super(message);
    }
}
