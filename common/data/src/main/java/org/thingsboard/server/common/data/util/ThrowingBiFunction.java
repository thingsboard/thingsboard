package org.thingsboard.server.common.data.util;

@FunctionalInterface
public interface ThrowingBiFunction<T, U, R> {

    R apply(T t, U u) throws Exception;

}
