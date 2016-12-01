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
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.datastax.driver.core.querybuilder.Select.Where;
import org.apache.commons.lang3.StringUtils;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.dao.model.SearchTextEntity;
import org.thingsboard.server.dao.model.ModelConstants;

import java.util.List;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.gt;
import static com.datastax.driver.core.querybuilder.QueryBuilder.gte;
import static com.datastax.driver.core.querybuilder.QueryBuilder.lt;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;

public abstract class AbstractSearchTextDao<T extends SearchTextEntity<?>> extends AbstractModelDao<T> {

    public T save(T entity) {
        entity.setSearchText(entity.getSearchTextSource().toLowerCase());
        return super.save(entity);
    }
    
    protected List<T> findPageWithTextSearch(String searchView, List<Clause> clauses, TextPageLink pageLink) {
        Select select = select().from(searchView);
        Where query = select.where();
        for (Clause clause : clauses) {
            query.and(clause);
        }       
        query.limit(pageLink.getLimit());
        if (!StringUtils.isEmpty(pageLink.getTextOffset())) {
            query.and(eq(ModelConstants.SEARCH_TEXT_PROPERTY, pageLink.getTextOffset()));
            query.and(QueryBuilder.lt(ModelConstants.ID_PROPERTY, pageLink.getIdOffset()));
            List<T> result = findListByStatement(query);
            if (result.size() < pageLink.getLimit()) {
                select = select().from(searchView);
                query = select.where();
                for (Clause clause : clauses) {
                    query.and(clause);
                }      
                query.and(QueryBuilder.gt(ModelConstants.SEARCH_TEXT_PROPERTY, pageLink.getTextOffset()));
                if (!StringUtils.isEmpty(pageLink.getTextSearch())) {
                    query.and(QueryBuilder.lt(ModelConstants.SEARCH_TEXT_PROPERTY, pageLink.getTextSearchBound()));
                }
                int limit = pageLink.getLimit() - result.size();
                query.limit(limit);
                result.addAll(findListByStatement(query));
            }
            return result;
        } else if (!StringUtils.isEmpty(pageLink.getTextSearch())) {
            query.and(QueryBuilder.gte(ModelConstants.SEARCH_TEXT_PROPERTY, pageLink.getTextSearch()));
            query.and(QueryBuilder.lt(ModelConstants.SEARCH_TEXT_PROPERTY, pageLink.getTextSearchBound()));
            return findListByStatement(query);
        } else {
            return findListByStatement(query);
        }
    }


}
