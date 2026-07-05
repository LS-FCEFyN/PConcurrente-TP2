package logger;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;

/**
 * A thread-safe, lazily-initialized singleton wrapper around
 * {@link java.util.logging.Logger} that logs application events to a file
 * ({@value #LOG_FILE}) using {@link LogFormatter} for output formatting.
 *
 * <p>Access the shared instance via {@link #getInstance()}; the singleton
 * is created on first use following the initialization-on-demand holder
 * idiom (see {@link Holder}), which guarantees thread-safe lazy
 * initialization without explicit synchronization.
 */
public final class Logger {
    private static final String LOG_FILE = "application.log";
    private final java.util.logging.Logger backend;

    /**
     * Configures the underlying {@link java.util.logging.Logger}: disables
     * parent handlers, removes any pre-existing handlers, and attaches a
     * {@link FileHandler} using {@link LogFormatter} that logs every level
     * to {@value #LOG_FILE}.
     *
     * @throws IllegalStateException if the log file handler cannot be created
     */
    private Logger() {
        backend = java.util.logging.Logger.getLogger(Logger.class.getName());
        backend.setUseParentHandlers(false);

        for (Handler handler : backend.getHandlers()) {
            backend.removeHandler(handler);
        }

        try {
            FileHandler handler = new FileHandler(LOG_FILE, true);
            handler.setLevel(Level.ALL);
            handler.setFormatter(new LogFormatter());
            backend.addHandler(handler);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to initialize log file handler", exception);
        }

        backend.setLevel(Level.ALL);
    }

    /** Holder class implementing lazy, thread-safe singleton initialization. */
    private static final class Holder {
        private static final Logger INSTANCE = new Logger();
    }

    /**
     * Returns the single shared {@code Logger} instance, creating it on
     * first call.
     *
     * @return the singleton {@code Logger} instance
     */
    public static Logger getInstance() {
        return Holder.INSTANCE;
    }

    /**
     * Logs a message at the given level.
     *
     * @param level   the severity level of the message
     * @param message the message to log
     */
    public void log(Level level, String message) {
        backend.log(level, message);
    }

    /**
     * Logs a message at {@link Level#INFO}.
     *
     * @param message the message to log
     */
    public void info(String message) {
        backend.info(message);
    }

    /**
     * Logs a message at {@link Level#WARNING}.
     *
     * @param message the message to log
     */
    public void warning(String message) {
        backend.warning(message);
    }

    /**
     * Logs a message at {@link Level#SEVERE}.
     *
     * @param message the message to log
     */
    public void severe(String message) {
        backend.severe(message);
    }
}