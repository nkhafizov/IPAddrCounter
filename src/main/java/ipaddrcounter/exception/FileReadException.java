package ipaddrcounter.exception;

public class FileReadException extends RuntimeException {
    private static final String ERROR_MESSAGE = "Error while reading file";

    public FileReadException(Throwable cause) {
        super(ERROR_MESSAGE, cause);
    }
}
