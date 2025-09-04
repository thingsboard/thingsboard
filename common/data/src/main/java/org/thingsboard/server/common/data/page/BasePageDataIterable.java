/**
 * Copyright © 2016-2025 The Thingsboard Authors
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public abstract class BasePageDataIterable<T> implements Iterable<T>, Iterator<T> {

    private final int fetchSize;

    private List<T> currentItems;
    private int currentIdx;
    private boolean hasNextPack;
    private PageLink nextPackLink;
    private boolean initialized;

    public BasePageDataIterable(int fetchSize) {
        super();
        this.fetchSize = fetchSize;
    }

    @Override
    public Iterator<T> iterator() {
        return this;
    }

    @Override
    public boolean hasNext() {
        if (!initialized) {
            fetch(new PageLink(fetchSize));
            initialized = true;
        }
        if (currentIdx == currentItems.size()) {
            if (hasNextPack) {
                fetch(nextPackLink);
            }
        }
        return currentIdx < currentItems.size();
    }

    @Override
    public T next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        return currentItems.get(currentIdx++);
    }

    private void fetch(PageLink link) {
        PageData<T> pageData = fetchPageData(link);
        currentIdx = 0;
        currentItems = pageData != null ? pageData.getData() : new ArrayList<>();
        hasNextPack = pageData != null && pageData.hasNext();
        nextPackLink = link.nextPageLink();
    }

    abstract PageData<T> fetchPageData(PageLink link);
}
