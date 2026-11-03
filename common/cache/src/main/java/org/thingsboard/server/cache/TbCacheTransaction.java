// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.cache;

public interface TbCacheTransaction<K, V> {

    void put(K key, V value);

    boolean commit();

    void rollback();

}
