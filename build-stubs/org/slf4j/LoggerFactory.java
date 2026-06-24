package org.slf4j;

/**
 * Minimal SLF4J LoggerFactory stub for lightweight builds.
 * Creates simple loggers that print to System.err.
 */
public class LoggerFactory {
    public static Logger getLogger(Class<?> clazz) {
        return getLogger(clazz.getName());
    }

    public static Logger getLogger(String name) {
        return new SimpleLogger(name);
    }

    private static class SimpleLogger implements Logger {
        private final String name;

        SimpleLogger(String name) {
            this.name = name;
        }

        private String format(String msg, Object... args) {
            if (args == null || args.length == 0) return msg;
            StringBuilder sb = new StringBuilder();
            int argIdx = 0;
            int i = 0;
            while (i < msg.length()) {
                if (i < msg.length() - 1 && msg.charAt(i) == '{' && msg.charAt(i + 1) == '}') {
                    if (argIdx < args.length) {
                        sb.append(args[argIdx++]);
                    } else {
                        sb.append("{}");
                    }
                    i += 2;
                } else {
                    sb.append(msg.charAt(i++));
                }
            }
            return sb.toString();
        }

        private void log(String level, String msg) {
            System.err.println("[" + level + "] " + name + " - " + msg);
        }

        @Override public void info(String msg) { log("INFO", msg); }
        @Override public void info(String format, Object arg) { log("INFO", format(format, arg)); }
        @Override public void info(String format, Object... args) { log("INFO", format(format, args)); }
        @Override public void warn(String msg) { log("WARN", msg); }
        @Override public void warn(String format, Object arg) { log("WARN", format(format, arg)); }
        @Override public void warn(String format, Object... args) { log("WARN", format(format, args)); }
        @Override public void error(String msg) { log("ERROR", msg); }
        @Override public void error(String format, Object arg) { log("ERROR", format(format, arg)); }
        @Override public void error(String format, Object... args) { log("ERROR", format(format, args)); }
        @Override public void debug(String msg) { /* no-op for lightweight build */ }
        @Override public void debug(String format, Object arg) { /* no-op */ }
        @Override public void trace(String msg) { /* no-op */ }
        @Override public boolean isDebugEnabled() { return false; }
        @Override public boolean isTraceEnabled() { return false; }
    }
}
