/**
 * Copyright © 2016 The Thingsboard Authors
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
package org.thingsboard.server.dao;

import com.datastax.driver.core.querybuilder.Clause;
import com.datastax.driver.core.querybuilder.Ordering;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.datastax.driver.core.querybuilder.Select.Where;
import com.datastax.driver.core.utils.UUIDs;
import org.apache.commons.lang3.StringUtils;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.model.BaseEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.SearchTextEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;

public abstract class AbstractSearchTimeDao<T extends BaseEntity<?>> extends AbstractModelDao<T> {


    protected List<T> findPageWithTimeSearch(String searchView, List<Clause> clauses, TimePageLink pageLink) {
        return findPageWithTimeSearch(searchView, clauses, Collections.emptyList(), pageLink);
    }

    protected List<T> findPageWithTimeSearch(String searchView, List<Clause> clauses, Ordering ordering, TimePageLink pageLink) {
        return findPageWithTimeSearch(searchView, clauses, Collections.singletonList(ordering), pageLink);
    }


    protected List<T> findPageWithTimeSearch(String searchView, List<Clause> clauses, List<Ordering> topLevelOrderings, TimePageLink pageLink) {
        Select select = select().from(searchView);
        Where query = select.where();
        for (Clause clause : clauses) {
            query.and(clause);
        }
        query.limit(pageLink.getLimit());
        if (pageLink.isAscOrder()) {
            if (pageLink.getIdOffset() != null) {
                query.and(QueryBuilder.gt(ModelConstants.ID_PROPERTY, pageLink.getIdOffset()));
            } else if (pageLink.getStartTime() != null) {
                final UUID startOf = UUIDs.startOf(pageLink.getStartTime());
                query.and(QueryBuilder.gte(ModelConstants.ID_PROPERTY, startOf));
            }
            if (pageLink.getEndTime() != null) {
                final UUID endOf = UUIDs.endOf(pageLink.getEndTime());
                query.and(QueryBuilder.lte(ModelConstants.ID_PROPERTY, endOf));
            }
        } else {
            if (pageLink.getIdOffset() != null) {
                query.and(QueryBuilder.lt(ModelConstants.ID_PROPERTY, pageLink.getIdOffset()));
            } else if (pageLink.getEndTime() != null) {
                final UUID endOf = UUIDs.endOf(pageLink.getEndTime());
                query.and(QueryBuilder.lte(ModelConstants.ID_PROPERTY, endOf));
            }
            if (pageLink.getStartTime() != null) {
                final UUID startOf = UUIDs.startOf(pageLink.getStartTime());
                query.and(QueryBuilder.gte(ModelConstants.ID_PROPERTY, startOf));
            }
        }
        List<Ordering> orderings = new ArrayList<>(topLevelOrderings);
        if (pageLink.isAscOrder()) {
            orderings.add(QueryBuilder.asc(ModelConstants.ID_PROPERTY));
        } else {
            orderings.add(QueryBuilder.desc(ModelConstants.ID_PROPERTY));
        }
        query.orderBy(orderings.toArray(new Ordering[orderings.size()]));
        return findListByStatement(query);
    }
}
