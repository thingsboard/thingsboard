/**
 * Copyright © 2016-2025 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.common.data.util;

import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;

import java.util.Optional;

/**
 * Utility class for common validation operations.
 * Provides methods to check for null values and throw ThingsboardException when validation fails.
 */
public class ValidationUtils {

    private ValidationUtils() {
        // Private constructor to prevent instantiation
    }

    /**
     * Checks that the specified reference is not null.
     *
     * @param reference the reference to check
     * @param <T> the type of the reference
     * @return the reference if not null
     * @throws ThingsboardException if the reference is null
     */
    public static <T> T checkNotNull(T reference) throws ThingsboardException {
        return checkNotNull(reference, "Requested item wasn't found!");
    }

    /**
     * Checks that the specified reference is not null.
     *
     * @param reference the reference to check
     * @param notFoundMessage the message to use in the exception if the reference is null
     * @param <T> the type of the reference
     * @return the reference if not null
     * @throws ThingsboardException if the reference is null
     */
    public static <T> T checkNotNull(T reference, String notFoundMessage) throws ThingsboardException {
        if (reference == null) {
            throw new ThingsboardException(notFoundMessage, ThingsboardErrorCode.ITEM_NOT_FOUND);
        }
        return reference;
    }

    /**
     * Checks that the specified Optional contains a value.
     *
     * @param reference the Optional to check
     * @param <T> the type of the value
     * @return the value if present
     * @throws ThingsboardException if the Optional is empty
     */
    public static <T> T checkNotNull(Optional<T> reference) throws ThingsboardException {
        return checkNotNull(reference, "Requested item wasn't found!");
    }

    /**
     * Checks that the specified Optional contains a value.
     *
     * @param reference the Optional to check
     * @param notFoundMessage the message to use in the exception if the Optional is empty
     * @param <T> the type of the value
     * @return the value if present
     * @throws ThingsboardException if the Optional is empty
     */
    public static <T> T checkNotNull(Optional<T> reference, String notFoundMessage) throws ThingsboardException {
        if (reference.isPresent()) {
            return reference.get();
        } else {
            throw new ThingsboardException(notFoundMessage, ThingsboardErrorCode.ITEM_NOT_FOUND);
        }
    }
}
