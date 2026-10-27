// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.page;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Schema
@EqualsAndHashCode
@ToString
public class PageData<T> implements Serializable {

    public static final PageData EMPTY_PAGE_DATA = new PageData<>();

    private final List<T> data;
    private final int totalPages;
    private final long totalElements;
    private final boolean hasNext;

    public PageData() {
        this(Collections.emptyList(), 0, 0, false);
    }

    @JsonCreator
    public PageData(@JsonProperty("data") List<T> data,
                    @JsonProperty("totalPages") int totalPages,
                    @JsonProperty("totalElements") long totalElements,
                    @JsonProperty("hasNext") boolean hasNext) {
        this.data = data;
        this.totalPages = totalPages;
        this.totalElements = totalElements;
        this.hasNext = hasNext;
    }

    @SuppressWarnings("unchecked")
    public static <T> PageData<T> emptyPageData() {
        return (PageData<T>) EMPTY_PAGE_DATA;
    }

    @Schema(description = "Array of the entities", accessMode = Schema.AccessMode.READ_ONLY)
    public List<T> getData() {
        return data;
    }

    @Schema(description = "Total number of available pages. Calculated based on the 'pageSize' request parameter and total number of entities that match search criteria", accessMode = Schema.AccessMode.READ_ONLY)
    public int getTotalPages() {
        return totalPages;
    }

    @Schema(description = "Total number of elements in all available pages", accessMode = Schema.AccessMode.READ_ONLY)
    public long getTotalElements() {
        return totalElements;
    }

    @Schema(description = "'false' value indicates the end of the result set", accessMode = Schema.AccessMode.READ_ONLY)
    @JsonProperty("hasNext")
    public boolean hasNext() {
        return hasNext;
    }

    public <D> PageData<D> mapData(Function<T, D> mapper) {
        return new PageData<>(getData().stream().map(mapper).collect(Collectors.toList()), getTotalPages(), getTotalElements(), hasNext());
    }

}
