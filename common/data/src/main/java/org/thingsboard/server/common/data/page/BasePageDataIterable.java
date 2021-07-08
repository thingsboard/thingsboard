package org.thingsboard.server.common.data.page;

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
        currentItems = pageData.getData();
        hasNextPack = pageData.hasNext();
        nextPackLink = link.nextPageLink();
    }

    abstract PageData<T> fetchPageData(PageLink link);
}
