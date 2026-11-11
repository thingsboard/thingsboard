// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.common.util;

@FunctionalInterface
public interface TbBiFunction<T, U, R> {
    R apply(T t, U u) throws Exception;
}
