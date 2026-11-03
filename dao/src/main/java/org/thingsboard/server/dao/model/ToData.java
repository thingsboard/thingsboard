// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.model;

/**
 * The interface To dto.
 *
 * @param <T> the type parameter
 */
public interface ToData<T> {

    /**
     * This method convert domain model object to data transfer object.
     *
     * @return the dto object
     */
    T toData();

}
