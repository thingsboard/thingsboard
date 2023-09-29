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
package org.thingsboard.server.dao.util.mapping;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class ArrayUtil {

    public static <T> T deepCopy(Object originalArray) {
        Class arrayClass = originalArray.getClass();

        if (boolean[].class.equals(arrayClass)) {
            boolean[] array = (boolean[]) originalArray;
            return (T) Arrays.copyOf(array, array.length);
        } else if (byte[].class.equals(arrayClass)) {
            byte[] array = (byte[]) originalArray;
            return (T) Arrays.copyOf(array, array.length);
        } else if (short[].class.equals(arrayClass)) {
            short[] array = (short[]) originalArray;
            return (T) Arrays.copyOf(array, array.length);
        } else if (int[].class.equals(arrayClass)) {
            int[] array = (int[]) originalArray;
            return (T) Arrays.copyOf(array, array.length);
        } else if (long[].class.equals(arrayClass)) {
            long[] array = (long[]) originalArray;
            return (T) Arrays.copyOf(array, array.length);
        } else if (float[].class.equals(arrayClass)) {
            float[] array = (float[]) originalArray;
            return (T) Arrays.copyOf(array, array.length);
        } else if (double[].class.equals(arrayClass)) {
            double[] array = (double[]) originalArray;
            return (T) Arrays.copyOf(array, array.length);
        } else if (char[].class.equals(arrayClass)) {
            char[] array = (char[]) originalArray;
            return (T) Arrays.copyOf(array, array.length);
        } else {
            Object[] array = (Object[]) originalArray;
            return (T) Arrays.copyOf(array, array.length);
        }
    }

    public static Object[] wrapArray(Object originalArray) {
        Class arrayClass = originalArray.getClass();

        if (boolean[].class.equals(arrayClass)) {
            boolean[] fromArray = (boolean[]) originalArray;
            Boolean[] array = new Boolean[fromArray.length];
            for (int i = 0; i < fromArray.length; i++) {
                array[i] = fromArray[i];
            }
            return array;
        } else if (byte[].class.equals(arrayClass)) {
            byte[] fromArray = (byte[]) originalArray;
            Byte[] array = new Byte[fromArray.length];
            for (int i = 0; i < fromArray.length; i++) {
                array[i] = fromArray[i];
            }
            return array;
        } else if (short[].class.equals(arrayClass)) {
            short[] fromArray = (short[]) originalArray;
            Short[] array = new Short[fromArray.length];
            for (int i = 0; i < fromArray.length; i++) {
                array[i] = fromArray[i];
            }
            return array;
        } else if (int[].class.equals(arrayClass)) {
            int[] fromArray = (int[]) originalArray;
            Integer[] array = new Integer[fromArray.length];
            for (int i = 0; i < fromArray.length; i++) {
                array[i] = fromArray[i];
            }
            return array;
        } else if (long[].class.equals(arrayClass)) {
            long[] fromArray = (long[]) originalArray;
            Long[] array = new Long[fromArray.length];
            for (int i = 0; i < fromArray.length; i++) {
                array[i] = fromArray[i];
            }
            return array;
        } else if (float[].class.equals(arrayClass)) {
            float[] fromArray = (float[]) originalArray;
            Float[] array = new Float[fromArray.length];
            for (int i = 0; i < fromArray.length; i++) {
                array[i] = fromArray[i];
            }
            return array;
        } else if (double[].class.equals(arrayClass)) {
            double[] fromArray = (double[]) originalArray;
            Double[] array = new Double[fromArray.length];
            for (int i = 0; i < fromArray.length; i++) {
                array[i] = fromArray[i];
            }
            return array;
        } else if (char[].class.equals(arrayClass)) {
            char[] fromArray = (char[]) originalArray;
            Character[] array = new Character[fromArray.length];
            for (int i = 0; i < fromArray.length; i++) {
                array[i] = fromArray[i];
            }
            return array;
        } else if (originalArray instanceof Collection) {
            return ((Collection) originalArray).toArray();
        } else {
            return (Object[]) originalArray;
        }
    }

    public static <T> T unwrapArray(Object[] originalArray, Class<T> arrayClass) {

        if (boolean[].class.equals(arrayClass)) {
            boolean[] array = new boolean[originalArray.length];
            for (int i = 0; i < originalArray.length; i++) {
                array[i] = originalArray[i] != null ? (Boolean) originalArray[i] : Boolean.FALSE;
            }
            return (T) array;
        } else if (byte[].class.equals(arrayClass)) {
            byte[] array = new byte[originalArray.length];
            for (int i = 0; i < originalArray.length; i++) {
                array[i] = originalArray[i] != null ? (Byte) originalArray[i] : 0;
            }
            return (T) array;
        } else if (short[].class.equals(arrayClass)) {
            short[] array = new short[originalArray.length];
            for (int i = 0; i < originalArray.length; i++) {
                array[i] = originalArray[i] != null ? (Short) originalArray[i] : 0;
            }
            return (T) array;
        } else if (int[].class.equals(arrayClass)) {
            int[] array = new int[originalArray.length];
            for (int i = 0; i < originalArray.length; i++) {
                array[i] = originalArray[i] != null ? (Integer) originalArray[i] : 0;
            }
            return (T) array;
        } else if (long[].class.equals(arrayClass)) {
            long[] array = new long[originalArray.length];
            for (int i = 0; i < originalArray.length; i++) {
                array[i] = originalArray[i] != null ? (Long) originalArray[i] : 0L;
            }
            return (T) array;
        } else if (float[].class.equals(arrayClass)) {
            float[] array = new float[originalArray.length];
            for (int i = 0; i < originalArray.length; i++) {
                array[i] = originalArray[i] != null ? ((Number) originalArray[i]).floatValue() : 0f;
            }
            return (T) array;
        } else if (double[].class.equals(arrayClass)) {
            double[] array = new double[originalArray.length];
            for (int i = 0; i < originalArray.length; i++) {
                array[i] = originalArray[i] != null ? (Double) originalArray[i] : 0d;
            }
            return (T) array;
        } else if (char[].class.equals(arrayClass)) {
            char[] array = new char[originalArray.length];
            for (int i = 0; i < originalArray.length; i++) {
                array[i] = originalArray[i] != null ? (Character) originalArray[i] : 0;
            }
            return (T) array;
        } else if (Enum[].class.isAssignableFrom(arrayClass)) {
            T array = arrayClass.cast(Array.newInstance(arrayClass.getComponentType(), originalArray.length));
            for (int i = 0; i < originalArray.length; i++) {
                Object objectValue = originalArray[i];
                if (objectValue != null) {
                    String stringValue = (objectValue instanceof String) ? (String) objectValue : String.valueOf(objectValue);
                    objectValue = Enum.valueOf((Class) arrayClass.getComponentType(), stringValue);
                }
                Array.set(array, i, objectValue);
            }
            return array;
        } else if (java.time.LocalDate[].class.equals(arrayClass) && java.sql.Date[].class.equals(originalArray.getClass())) {
            // special case because conversion is neither with ctor nor valueOf
            Object[] array = (Object[]) Array.newInstance(java.time.LocalDate.class, originalArray.length);
            for (int i = 0; i < array.length; ++i) {
                array[i] = originalArray[i] != null ? ((java.sql.Date) originalArray[i]).toLocalDate() : null;
            }
            return (T) array;
        } else if (java.time.LocalDateTime[].class.equals(arrayClass) && java.sql.Timestamp[].class.equals(originalArray.getClass())) {
            // special case because conversion is neither with ctor nor valueOf
            Object[] array = (Object[]) Array.newInstance(java.time.LocalDateTime.class, originalArray.length);
            for (int i = 0; i < array.length; ++i) {
                array[i] = originalArray[i] != null ? ((java.sql.Timestamp) originalArray[i]).toLocalDateTime() : null;
            }
            return (T) array;
        } else if(arrayClass.getComponentType() != null && arrayClass.getComponentType().isArray()) {
            int arrayLength = originalArray.length;
            Object[] array = (Object[]) Array.newInstance(arrayClass.getComponentType(), arrayLength);
            if (arrayLength > 0) {
                for (int i = 0; i < originalArray.length; i++) {
                    array[i] = unwrapArray((Object[]) originalArray[i], arrayClass.getComponentType());
                }
            }
            return (T) array;
        } else {
            if (arrayClass.isInstance(originalArray)) {
                return (T) originalArray;
            } else {
                return (T) Arrays.copyOf(originalArray, originalArray.length, (Class) arrayClass);
            }
        }
    }

    public static <T> T fromString(String string, Class<T> arrayClass) {
        String stringArray = string.replaceAll("[\\[\\]]", "");
        String[] tokens = stringArray.split(",");

        int length = tokens.length;

        if (boolean[].class.equals(arrayClass)) {
            boolean[] array = new boolean[length];
            for (int i = 0; i < tokens.length; i++) {
                array[i] = Boolean.valueOf(tokens[i]);
            }
            return (T) array;
        } else if (byte[].class.equals(arrayClass)) {
            byte[] array = new byte[length];
            for (int i = 0; i < tokens.length; i++) {
                array[i] = Byte.valueOf(tokens[i]);
            }
            return (T) array;
        } else if (short[].class.equals(arrayClass)) {
            short[] array = new short[length];
            for (int i = 0; i < tokens.length; i++) {
                array[i] = Short.valueOf(tokens[i]);
            }
            return (T) array;
        } else if (int[].class.equals(arrayClass)) {
            int[] array = new int[length];
            for (int i = 0; i < tokens.length; i++) {
                array[i] = Integer.valueOf(tokens[i]);
            }
            return (T) array;
        } else if (long[].class.equals(arrayClass)) {
            long[] array = new long[length];
            for (int i = 0; i < tokens.length; i++) {
                array[i] = Long.valueOf(tokens[i]);
            }
            return (T) array;
        } else if (float[].class.equals(arrayClass)) {
            float[] array = new float[length];
            for (int i = 0; i < tokens.length; i++) {
                array[i] = Float.valueOf(tokens[i]);
            }
            return (T) array;
        } else if (double[].class.equals(arrayClass)) {
            double[] array = new double[length];
            for (int i = 0; i < tokens.length; i++) {
                array[i] = Double.valueOf(tokens[i]);
            }
            return (T) array;
        } else if (char[].class.equals(arrayClass)) {
            char[] array = new char[length];
            for (int i = 0; i < tokens.length; i++) {
                array[i] = tokens[i].length() > 0 ? tokens[i].charAt(0) : Character.MIN_VALUE;
            }
            return (T) array;
        } else {
            return (T) tokens;
        }
    }

    public static boolean isEquals(Object firstArray, Object secondArray) {
        if (firstArray.getClass() != secondArray.getClass()) {
            return false;
        }
        Class arrayClass = firstArray.getClass();

        if (boolean[].class.equals(arrayClass)) {
            return Arrays.equals((boolean[]) firstArray, (boolean[]) secondArray);
        } else if (byte[].class.equals(arrayClass)) {
            return Arrays.equals((byte[]) firstArray, (byte[]) secondArray);
        } else if (short[].class.equals(arrayClass)) {
            return Arrays.equals((short[]) firstArray, (short[]) secondArray);
        } else if (int[].class.equals(arrayClass)) {
            return Arrays.equals((int[]) firstArray, (int[]) secondArray);
        } else if (long[].class.equals(arrayClass)) {
            return Arrays.equals((long[]) firstArray, (long[]) secondArray);
        } else if (float[].class.equals(arrayClass)) {
            return Arrays.equals((float[]) firstArray, (float[]) secondArray);
        } else if (double[].class.equals(arrayClass)) {
            return Arrays.equals((double[]) firstArray, (double[]) secondArray);
        } else if (char[].class.equals(arrayClass)) {
            return Arrays.equals((char[]) firstArray, (char[]) secondArray);
        } else {
            return Arrays.equals((Object[]) firstArray, (Object[]) secondArray);
        }
    }

    public static <T> Class<T[]> toArrayClass(Class<T> arrayElementClass) {

        if (boolean.class.equals(arrayElementClass)) {
            return (Class) boolean[].class;
        } else if (byte.class.equals(arrayElementClass)) {
            return (Class) byte[].class;
        } else if (short.class.equals(arrayElementClass)) {
            return (Class) short[].class;
        } else if (int.class.equals(arrayElementClass)) {
            return (Class) int[].class;
        } else if (long.class.equals(arrayElementClass)) {
            return (Class) long[].class;
        } else if (float.class.equals(arrayElementClass)) {
            return (Class) float[].class;
        } else if (double[].class.equals(arrayElementClass)) {
            return (Class) double[].class;
        } else if (char[].class.equals(arrayElementClass)) {
            return (Class) char[].class;
        } else {
            Object array = Array.newInstance(arrayElementClass, 0);
            return (Class<T[]>) array.getClass();
        }
    }

    public static <T> List<T> asList(T[] array) {
        List<T> list = new ArrayList<T>(array.length);
        for (int i = 0; i < array.length; i++) {
            list.add(i, array[i]);
        }
        return list;
    }
}
