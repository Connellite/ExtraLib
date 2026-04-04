package io.github.connellite.exception;

/**
 * Invalid format string or specifier (similar in role to fmt::format_error).
 */
public final class FormatException extends RuntimeException {

    public FormatException(String message) {
        super(message);
    }

    public FormatException(String message, Throwable cause) {
        super(message, cause);
    }
}
