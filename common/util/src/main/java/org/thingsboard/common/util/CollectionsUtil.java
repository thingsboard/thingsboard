/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.common.util;

import java.util.Collection;
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

}
