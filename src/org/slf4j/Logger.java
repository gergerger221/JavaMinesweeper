package org.slf4j;

public interface Logger {
    String getName();

    default boolean isTraceEnabled() {
        return false;
    }

    default void trace(String msg) {
    }

    default void trace(String format, Object arg) {
    }

    default void trace(String format, Object arg1, Object arg2) {
    }

    default void trace(String format, Object... arguments) {
    }

    default void trace(String msg, Throwable t) {
    }

    default boolean isDebugEnabled() {
        return false;
    }

    default void debug(String msg) {
    }

    default void debug(String format, Object arg) {
    }

    default void debug(String format, Object arg1, Object arg2) {
    }

    default void debug(String format, Object... arguments) {
    }

    default void debug(String msg, Throwable t) {
    }

    default boolean isInfoEnabled() {
        return false;
    }

    default void info(String msg) {
    }

    default void info(String format, Object arg) {
    }

    default void info(String format, Object arg1, Object arg2) {
    }

    default void info(String format, Object... arguments) {
    }

    default void info(String msg, Throwable t) {
    }

    default boolean isWarnEnabled() {
        return false;
    }

    default void warn(String msg) {
    }

    default void warn(String format, Object arg) {
    }

    default void warn(String format, Object arg1, Object arg2) {
    }

    default void warn(String format, Object... arguments) {
    }

    default void warn(String msg, Throwable t) {
    }

    default boolean isErrorEnabled() {
        return false;
    }

    default void error(String msg) {
    }

    default void error(String format, Object arg) {
    }

    default void error(String format, Object arg1, Object arg2) {
    }

    default void error(String format, Object... arguments) {
    }

    default void error(String msg, Throwable t) {
    }
}
