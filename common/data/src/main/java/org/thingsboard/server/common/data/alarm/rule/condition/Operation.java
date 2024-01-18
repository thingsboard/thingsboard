/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.server.common.data.alarm.rule.condition;

import java.util.function.BiPredicate;

public enum Operation {

    EQUAL(String::equals, Boolean::equals, Double::equals),
    NOT_EQUAL((o1, o2) -> !o1.equals(o2), (o1, o2) -> !o1.equals(o2), (o1, o2) -> !o1.equals(o2)),

    //String
    STARTS_WITH(String::startsWith, null, null),
    ENDS_WITH(String::endsWith, null, null),
    CONTAINS(String::contains, null, null),
    NOT_CONTAINS((o1, o2) -> !o1.contains(o2), null, null),

    //Numeric
    GREATER(null, null, (o1, o2) -> o1 > o2),
    GREATER_OR_EQUAL(null, null, (o1, o2) -> o1 >= o2),
    LESS(null, null, (o1, o2) -> o1 < o2),
    LESS_OR_EQUAL(null, null, (o1, o2) -> o1 <= o2);

    private final BiPredicate<String, String> stringPredicate;
    private final BiPredicate<Boolean, Boolean> booleanPredicate;
    private final BiPredicate<Double, Double> numericPredicate;

    Operation(BiPredicate<String, String> stringPredicate, BiPredicate<Boolean, Boolean> booleanPredicate, BiPredicate<Double, Double> numericPredicate) {
        this.stringPredicate = stringPredicate;
        this.booleanPredicate = booleanPredicate;
        this.numericPredicate = numericPredicate;
    }

    public boolean process(String o1, String o2) {
        return checkArguments(o1, o2) && stringPredicate.test(o1, o2);
    }

    public boolean process(Boolean o1, Boolean o2) {
        return checkArguments(o1, o2) && booleanPredicate.test(o1, o2);
    }

    public boolean process(Double o1, Double o2) {
        return checkArguments(o1, o2) && numericPredicate.test(o1, o2);
    }

    private boolean checkArguments(Object o1, Object o2) {
        return o1 != null && o2 != null;
    }
}
