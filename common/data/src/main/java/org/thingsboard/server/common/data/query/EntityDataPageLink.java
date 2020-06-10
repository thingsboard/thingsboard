package org.thingsboard.server.common.data.query;

import lombok.Data;
import org.thingsboard.server.common.data.page.SortOrder;

@Data
public class EntityDataPageLink {

    private final int pageSize;
    private final int page;
    private final SortOrder sortOrder;

}
