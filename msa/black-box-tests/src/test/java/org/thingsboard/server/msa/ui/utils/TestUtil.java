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
package org.thingsboard.server.msa.ui.utils;

import lombok.SneakyThrows;

import java.util.Random;

public class TestUtil {

    @SneakyThrows
    public static void sleep(int seconds) {
        Thread.sleep(seconds * 1000L);
    }

    public static long getRandomNumber() {
        return System.currentTimeMillis();
    }

    public static char randomSymbol() {
        Random rand = new Random();
        String s = "~`!@#$^&*()_+=-";
        return s.charAt(rand.nextInt(s.length()));
    }
}
