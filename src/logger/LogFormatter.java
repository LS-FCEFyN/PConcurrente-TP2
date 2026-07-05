package logger;

import java.util.logging.LogRecord;
import java.util.logging.Formatter;

/**
 * A minimal {@link Formatter} that renders log records as a single line
 * of the form {@code [LEVEL] loggerName: message}, without timestamps or
 * stack-trace formatting overhead.
 */
public class LogFormatter extends Formatter {
    /**
     * Formats a log record into a single, human-readable line.
     *
     * @param record the log record to format
     * @return the formatted line, terminated with a platform-specific newline
     */
    public String format(LogRecord record) {
        return String.format("[%s] %s: %s%n", record.getLevel().getName(), record.getLoggerName(),
                record.getMessage());
    }

}