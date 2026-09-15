// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.page;

public class PageDataIterable<T> extends BasePageDataIterable<T> {

    private final FetchFunction<T> function;

    public PageDataIterable(FetchFunction<T> function, int fetchSize) {
        super(fetchSize);
        this.function = function;
    }

    @Override
    PageData<T> fetchPageData(PageLink link) {
        return function.fetch(link);
    }

    public interface FetchFunction<T> {
        PageData<T> fetch(PageLink link);
    }
}
