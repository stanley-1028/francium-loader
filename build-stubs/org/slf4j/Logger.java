package org.slf4j;

/**
 * Minimal SLF4J Logger stub for lightweight builds without external dependencies.
 * Delegates to System.err for simplicity.
 */
public interface Logger {
    void info(String msg);
    void info(String format, Object arg);
    void info(String format, Object... args);
    void warn(String msg);
    void warn(String format, Object arg);
    void warn(String format, Object... args);
    void error(String msg);
    void error(String format, Object arg);
    void error(String format, Object... args);
    void debug(String msg);
    void debug(String format, Object arg);
    void trace(String msg);
    boolean isDebugEnabled();
    boolean isTraceEnabled();
}
