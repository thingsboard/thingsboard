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
package org.thingsboard.server.common.data;

import org.apache.commons.lang3.ArrayUtils;
import org.springframework.lang.Nullable;

public class StringUtils {
    public static String defaultString(String str, String defaultStr) {
        return str == null ? defaultStr : str;
    }

    public static boolean isEmpty(String source) {
        return source == null || source.isEmpty();
    }

    public static boolean isBlank(String source) {
        return source == null || source.isEmpty() || source.trim().isEmpty();
    }

    public static boolean isNotEmpty(String source) {
        return source != null && !source.isEmpty();
    }

    public static boolean isNotBlank(String source) {
        return source != null && !source.isEmpty() && !source.trim().isEmpty();
    }
    public static boolean isNoneBlank(CharSequence... css) {
        return !isAnyBlank(css);
    }

    public static boolean isAnyBlank(CharSequence... css) {
        if (ArrayUtils.isEmpty(css)) {
            return true;
        } else {
            CharSequence[] arr$ = css;
            int len$ = css.length;

            for(int i$ = 0; i$ < len$; ++i$) {
                CharSequence cs = arr$[i$];
                if (isBlank(cs)) {
                    return true;
                }
            }

            return false;
        }
    }

    public static boolean isBlank(CharSequence cs) {
        int strLen;
        if (cs != null && (strLen = cs.length()) != 0) {
            for(int i = 0; i < strLen; ++i) {
                if (!Character.isWhitespace(cs.charAt(i))) {
                    return false;
                }
            }

            return true;
        } else {
            return true;
        }
    }

    public static String join(String[] keyArray, String sep) {
        return null;
    }

    public static String trim(String str) {
        return str == null ? null : str.trim();
    }

    public static String trimToNull(String str) {
        String ts = trim(str);
        return isEmpty(ts) ? null : ts;
    }

    public static boolean isNoneEmpty(CharSequence... css) {
        return !isAnyEmpty(css);
    }

    public static boolean isAnyEmpty(CharSequence... css) {
        if (ArrayUtils.isEmpty(css)) {
            return true;
        } else {
            CharSequence[] arr$ = css;
            int len$ = css.length;

            for (int i$ = 0; i$ < len$; ++i$) {
                CharSequence cs = arr$[i$];
                if (isEmpty(cs)) {
                    return true;
                }
            }

            return false;
        }
    }

    public static boolean isEmpty(CharSequence cs) {
        return cs == null || cs.length() == 0;
    }

    public static boolean equalsIgnoreCase(String str1, String str2) {
        return str1 == null ? str2 == null : str1.equalsIgnoreCase(str2);
    }

    public static boolean endsWith(CharSequence str, CharSequence suffix) {
        return endsWith(str, suffix, false);
    }

    public static boolean endsWithIgnoreCase(CharSequence str, CharSequence suffix) {
        return endsWith(str, suffix, true);
    }

    private static boolean endsWith(CharSequence str, CharSequence suffix, boolean ignoreCase) {
        if (str != null && suffix != null) {
            if (suffix.length() > str.length()) {
                return false;
            } else {
                int strOffset = str.length() - suffix.length();
                return regionMatches(str, ignoreCase, strOffset, suffix, 0, suffix.length());
            }
        } else {
            return str == null && suffix == null;
        }
    }

    public static boolean hasLength(@Nullable String str) {
        return str != null && !str.isEmpty();
    }

    private static boolean regionMatches(CharSequence cs, boolean ignoreCase, int thisStart, CharSequence substring, int start, int length) {
        if (cs instanceof String && substring instanceof String) {
            return ((String) cs).regionMatches(ignoreCase, thisStart, (String) substring, start, length);
        } else {
            int index1 = thisStart;
            int index2 = start;
            int var8 = length;

            while (var8-- > 0) {
                char c1 = cs.charAt(index1++);
                char c2 = substring.charAt(index2++);
                if (c1 != c2) {
                    if (!ignoreCase) {
                        return false;
                    }

                    if (Character.toUpperCase(c1) != Character.toUpperCase(c2) && Character.toLowerCase(c1) != Character.toLowerCase(c2)) {
                        return false;
                    }
                }
            }

            return true;
        }
    }

    public static String substringBetween(String str, String tag) {
        return substringBetween(str, tag, tag);
    }

    public static String substringBetween(String str, String open, String close) {
        if (str != null && open != null && close != null) {
            int start = str.indexOf(open);
            if (start != -1) {
                int end = str.indexOf(close, start + open.length());
                if (end != -1) {
                    return str.substring(start + open.length(), end);
                }
            }

            return null;
        } else {
            return null;
        }
    }

    public static boolean hasText(@Nullable CharSequence str) {
        return str != null && str.length() > 0 && containsText(str);
    }

    public static boolean hasText(@Nullable String str) {
        return str != null && !str.isEmpty() && containsText(str);
    }

    private static boolean containsText(CharSequence str) {
        int strLen = str.length();

        for(int i = 0; i < strLen; ++i) {
            if (!Character.isWhitespace(str.charAt(i))) {
                return true;
            }
        }

        return false;
    }
}
