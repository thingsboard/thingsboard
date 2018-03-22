/**
 * Copyright © 2016-2018 The Thingsboard Authors
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
package org.thingsboard.server.common.data.page;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;
import java.util.Arrays;
import java.util.UUID;

@ToString
public class TextPageLink extends BasePageLink implements Serializable {

    private static final long serialVersionUID = -4189954843653250480L;

    @Getter private final String textSearch;
    @Getter private final String textSearchBound;
    @Getter private final String textOffset;

    public TextPageLink(int limit) {
        this(limit, null, null, null);
    }

    public TextPageLink(int limit, String textSearch) {
        this(limit, textSearch, null, null);
    }

    public TextPageLink(int limit, String textSearch, UUID idOffset, String textOffset) {
        super(limit, idOffset);
        this.textSearch = textSearch != null ? textSearch.toLowerCase() : null;
        this.textSearchBound = nextSequence(this.textSearch);
        this.textOffset = textOffset != null ? textOffset.toLowerCase() : null;
    }

    @JsonCreator
    public TextPageLink(@JsonProperty("limit") int limit,
                        @JsonProperty("textSearch") String textSearch,
                        @JsonProperty("textSearchBound") String textSearchBound,
                        @JsonProperty("textOffset") String textOffset,
                        @JsonProperty("idOffset") UUID idOffset) {
        super(limit, idOffset);
        this.textSearch = textSearch;
        this.textSearchBound = textSearchBound;
        this.textOffset = textOffset;
        this.idOffset = idOffset;
    }

    private static String nextSequence(String input) {
        if (input != null && input.length() > 0) {
            char[] chars = input.toCharArray();
            int i = chars.length - 1;
            while (i >= 0 && ++chars[i--] == Character.MIN_VALUE) ;
            if (i == -1 && (chars.length == 0 || chars[0] == Character.MIN_VALUE)) {
                char buf[] = Arrays.copyOf(input.toCharArray(), input.length() + 1);
                buf[buf.length - 1] = Character.MIN_VALUE;
                return new String(buf);
            }
            return new String(chars);
        } else {
            return null;
        }
    }

}
