// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.trendz;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.thingsboard.server.common.data.page.PageData;

import java.io.Serializable;
import java.util.List;

public record TrendzPaginationData<T> (
        @JsonProperty("content") List<T> content,
        @JsonProperty("pageNumber") int pageNumber,
        @JsonProperty("totalPages") int totalPages,
        @JsonProperty("totalElements") long totalElements
) implements Serializable {
    public PageData<T> toPageData() {
        return new PageData<>(content, totalPages, totalElements, pageNumber + 1 != totalPages);
    }
}
