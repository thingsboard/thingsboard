/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.dao.nosql;

import com.datastax.driver.core.querybuilder.Clause;
import com.datastax.driver.core.querybuilder.Ordering;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.datastax.driver.core.querybuilder.Select.Where;
import com.datastax.driver.core.utils.UUIDs;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.model.BaseEntity;
import org.thingsboard.server.dao.model.ModelConstants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static com.datastax.driver.core.querybuilder.QueryBuilder.select;

public abstract class CassandraAbstractSearchTimeDao<E extends BaseEntity<D>, D> extends CassandraAbstractModelDao<E, D> {


    protected List<E> findPageWithTimeSearch(TenantId tenantId, String searchView, List<Clause> clauses, TimePageLink pageLink) {
        return findPageWithTimeSearch(tenantId, searchView, clauses, Collections.emptyList(), pageLink);
    }

    protected List<E> findPageWithTimeSearch(TenantId tenantId, String searchView, List<Clause> clauses, Ordering ordering, TimePageLink pageLink) {
        return findPageWithTimeSearch(tenantId, searchView, clauses, Collections.singletonList(ordering), pageLink);
    }

    protected List<E> findPageWithTimeSearch(TenantId tenantId, String searchView, List<Clause> clauses, List<Ordering> topLevelOrderings, TimePageLink pageLink) {
        return findPageWithTimeSearch(tenantId, searchView, clauses, topLevelOrderings, pageLink, ModelConstants.ID_PROPERTY);
    }

    protected List<E> findPageWithTimeSearch(TenantId tenantId, String searchView, List<Clause> clauses, TimePageLink pageLink, String idColumn) {
        return findPageWithTimeSearch(tenantId, searchView, clauses, Collections.emptyList(), pageLink, idColumn);
    }

    protected List<E> findPageWithTimeSearch(TenantId tenantId, String searchView, List<Clause> clauses, List<Ordering> topLevelOrderings, TimePageLink pageLink, String idColumn) {
        return findListByStatement(tenantId, buildQuery(searchView, clauses, topLevelOrderings, pageLink, idColumn));
    }

    public static Where buildQuery(String searchView, List<Clause> clauses, TimePageLink pageLink, String idColumn) {
        return buildQuery(searchView, clauses, Collections.emptyList(), pageLink, idColumn);
    }

    public static Where buildQuery(String searchView, List<Clause> clauses, Ordering order, TimePageLink pageLink, String idColumn) {
        return buildQuery(searchView, clauses, Collections.singletonList(order), pageLink, idColumn);
    }

    public static Where buildQuery(String searchView, List<Clause> clauses, List<Ordering> topLevelOrderings, TimePageLink pageLink, String idColumn) {
        Select select = select().from(searchView);
        Where query = select.where();
        for (Clause clause : clauses) {
            query.and(clause);
        }
        query.limit(pageLink.getLimit());
        if (pageLink.isAscOrder()) {
            if (pageLink.getIdOffset() != null) {
                query.and(QueryBuilder.gt(idColumn, pageLink.getIdOffset()));
            } else if (pageLink.getStartTime() != null) {
                final UUID startOf = UUIDs.startOf(pageLink.getStartTime());
                query.and(QueryBuilder.gte(idColumn, startOf));
            }
            if (pageLink.getEndTime() != null) {
                final UUID endOf = UUIDs.endOf(pageLink.getEndTime());
                query.and(QueryBuilder.lte(idColumn, endOf));
            }
        } else {
            if (pageLink.getIdOffset() != null) {
                query.and(QueryBuilder.lt(idColumn, pageLink.getIdOffset()));
            } else if (pageLink.getEndTime() != null) {
                final UUID endOf = UUIDs.endOf(pageLink.getEndTime());
                query.and(QueryBuilder.lte(idColumn, endOf));
            }
            if (pageLink.getStartTime() != null) {
                final UUID startOf = UUIDs.startOf(pageLink.getStartTime());
                query.and(QueryBuilder.gte(idColumn, startOf));
            }
        }
        List<Ordering> orderings = new ArrayList<>(topLevelOrderings);
        if (pageLink.isAscOrder()) {
            orderings.add(QueryBuilder.asc(idColumn));
        } else {
            orderings.add(QueryBuilder.desc(idColumn));
        }
        query.orderBy(orderings.toArray(new Ordering[orderings.size()]));
        return query;
    }

}
