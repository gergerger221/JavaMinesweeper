package org.slf4j;

public final class LoggerFactory {
    private LoggerFactory() {
    }

    private static final Logger NOP = new NopLogger();

    public static Logger getLogger(String name) {
        return NOP;
    }

    public static Logger getLogger(Class<?> clazz) {
        return NOP;
    }

    private static final class NopLogger implements Logger {
        @Override
        public String getName() {
            return "NOP";
        }
    }
}
