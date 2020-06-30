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
package org.thingsboard.server.dao.sql;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.springframework.data.jpa.domain.Specification;
import org.thingsboard.server.common.data.UUIDConverter;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.model.BaseEntity;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by Valerii Sosliuk on 5/4/2017.
 */
public abstract class JpaAbstractSearchTimeDao<E extends BaseEntity<D>, D> extends JpaAbstractDao<E, D> {

    public static <T> Specification<T> getTimeSearchPageSpec(TimePageLink pageLink, String idColumn) {
        return new Specification<T>() {
            @Override
            public Predicate toPredicate(Root<T> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicates = new ArrayList<>();
                if (pageLink.getStartTime() != null) {
                    UUID startOf = Uuids.startOf(pageLink.getStartTime());
                    Predicate lowerBound = criteriaBuilder.greaterThanOrEqualTo(root.get(idColumn), UUIDConverter.fromTimeUUID(startOf));
                    predicates.add(lowerBound);
                }
                if (pageLink.getEndTime() != null) {
                    UUID endOf = Uuids.endOf(pageLink.getEndTime());
                    Predicate upperBound = criteriaBuilder.lessThanOrEqualTo(root.get(idColumn), UUIDConverter.fromTimeUUID(endOf));
                    predicates.add(upperBound);
                }
                return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
            }
        };
    }
}
