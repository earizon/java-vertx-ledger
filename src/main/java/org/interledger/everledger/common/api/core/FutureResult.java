package org.interledger.ilp.common.api.core;

import io.vertx.core.AsyncResult;

/**
 * Represents a custom inmutable future result
 *
 * @author mrmx
 */
public class FutureResult<T> implements AsyncResult<T> {

    private final T result;
    private final boolean succeeded;

    private FutureResult(T result, boolean succeeded) {
        this.result = result;
        this.succeeded = succeeded;
    }

    public static <T> FutureResult<T> succeeded(T result) {
        return new FutureResult<>(result, true);
    }

    public static <T> FutureResult<T> failed(T result) {
        return new FutureResult<>(result, false);
    }

    @Override
    public T result() {
        return result;
    }

    @Override
    public Throwable cause() {
        return null;
    }

    @Override
    public boolean succeeded() {
        return succeeded;
    }

    @Override
    public boolean failed() {
        return !succeeded;
    }

}
