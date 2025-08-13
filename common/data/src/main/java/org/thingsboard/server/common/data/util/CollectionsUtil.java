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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class CollectionsUtil {
    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    public static boolean isNotEmpty(Collection<?> collection) {
        return !isEmpty(collection);
    }

    /**
     * Returns new set with elements that are present in set B(new) but absent in set A(old).
     */
    public static <T> Set<T> diffSets(Set<T> a, Set<T> b) {
        return b.stream().filter(p -> !a.contains(p)).collect(Collectors.toSet());
    }

    /**
     * Returns new list with elements that are present in list B(new) but absent in list A(old).
     */
    public static <T> List<T> diffLists(List<T> a, List<T> b) {
        return b.stream().filter(p -> !a.contains(p)).collect(Collectors.toList());
    }

    public static <T> boolean contains(Collection<T> collection, T element) {
        return isNotEmpty(collection) && collection.contains(element);
    }

    public static <T> int countNonNull(T[] array) {
        int count = 0;
        for (T t : array) {
            if (t != null) count++;
        }
        return count;
    }

    @SuppressWarnings("unchecked")
    public static <T> Map<T, T> mapOf(T... kvs) {
        if (kvs.length % 2 != 0) {
            throw new IllegalArgumentException("Invalid number of parameters");
        }
        Map<T, T> map = new HashMap<>();
        for (int i = 0; i < kvs.length; i += 2) {
            T key = kvs[i];
            T value = kvs[i + 1];
            map.put(key, value);
        }
        return map;
    }

    public static <V> boolean emptyOrContains(Collection<V> collection, V element) {
        return isEmpty(collection) || collection.contains(element);
    }

    public static <V> HashSet<V> concat(Set<V> set1, Set<V> set2) {
        HashSet<V> result = new HashSet<>();
        result.addAll(set1);
        result.addAll(set2);
        return result;
    }

    public static <V> boolean isOneOf(V value, V... others) {
        if (value == null) {
            return false;
        }
        for (V other : others) {
            if (value.equals(other)) {
                return true;
            }
        }
        return false;
    }

}
