package ipaddrcounter.exception;

import java.text.MessageFormat;

public class UniqueIPV4CounterException extends RuntimeException {
    private static final String ERROR_MESSAGE = "Error while processing IP address: {0}";
    public UniqueIPV4CounterException(String message, Throwable cause) {
        super(MessageFormat.format(ERROR_MESSAGE, message), cause);
    }
}
