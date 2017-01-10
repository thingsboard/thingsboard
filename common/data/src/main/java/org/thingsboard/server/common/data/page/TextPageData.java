/**
 * Copyright © 2016-2017 The Thingsboard Authors
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

import java.util.List;
import java.util.UUID;

import org.thingsboard.server.common.data.SearchTextBased;
import org.thingsboard.server.common.data.id.UUIDBased;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class TextPageData<T extends SearchTextBased<? extends UUIDBased>> {

    private final List<T> data;
    private final TextPageLink nextPageLink;
    private final boolean hasNext;

    public TextPageData(List<T> data, TextPageLink pageLink) {
        super();
        this.data = data;
        int limit = pageLink.getLimit();
        if (data != null && data.size() == limit) {
            int index = data.size()-1;
            UUID idOffset = data.get(index).getId().getId();
            String textOffset = data.get(index).getSearchText();
            nextPageLink = new TextPageLink(limit, pageLink.getTextSearch(), idOffset, textOffset);
            hasNext = true;
        } else {
            nextPageLink = null;
            hasNext = false;
        }
    }
    
    @JsonCreator
    public TextPageData(@JsonProperty("data") List<T> data,
                        @JsonProperty("nextPageLink") TextPageLink nextPageLink,
                        @JsonProperty("hasNext") boolean hasNext) {
        this.data = data;
        this.nextPageLink = nextPageLink;
        this.hasNext = hasNext;
    }

    public List<T> getData() {
        return data;
    }

    @JsonProperty("hasNext")
    public boolean hasNext() {
        return hasNext;
    }
    
    public TextPageLink getNextPageLink() {
        return nextPageLink;
    }

}
