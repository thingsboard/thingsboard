// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.ToData;
import org.thingsboard.server.dao.sql.citus.CitusSettings;
import org.thingsboard.server.dao.sql.customer.CustomerRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Single home for the "list an entity's *_info_view rows including its sub-customers" branching that the device,
 * asset and entity-view info DAOs would otherwise each repeat.
 * <p>
 * On plain PostgreSQL the recursive sub-customer walk is inlined into the info-view query and executed directly.
 * On Citus that recursive CTE cannot be pushed into the distributed info-view query, so the sub-customer ids are
 * resolved on the coordinator first ({@link CustomerRepository#findSubCustomerIdsOrEmpty}) and bound as a single
 * typed {@code uuid[]} array parameter to the {@code IN (SELECT unnest(:customerIds))} predicate of the
 * {@code *InCustomerIds} query variant — one JDBC placeholder regardless of subtree size, so a very large
 * sub-customer tree cannot hit the pgjdbc 32767 bind-parameter cap. An empty {@link Optional} means there is
 * nothing to query, so the result short-circuits to an empty page. Keeping the choice here means "where is Citus
 * checked for these listings" resolves to one place.
 * <p>
 * A {@code @Component} so its {@link CitusSettings} / {@link CustomerRepository} collaborators are injected once
 * here instead of being threaded through each info DAO purely to pass them in.
 */
@Component
@RequiredArgsConstructor
public class CitusSubCustomerInfoQuery {

    private final CitusSettings citusSettings;
    private final CustomerRepository customerRepository;

    /**
     * Runs {@code citusInCustomerIds} (against the resolved sub-customer id list, bound as a typed {@code uuid[]}
     * array) when Citus is enabled and the subtree is non-empty, otherwise runs {@code plainIncludingSubCustomers}.
     * Returns an empty page when Citus is enabled but the customer has no resolvable subtree.
     */
    public <T> PageData<T> findIncludingSubCustomers(
            UUID tenantId,
            UUID customerId,
            Function<UUID[], Page<? extends ToData<T>>> citusInCustomerIds,
            Supplier<Page<? extends ToData<T>>> plainIncludingSubCustomers) {
        if (citusSettings.isEnabled()) {
            Optional<List<UUID>> customerIds = customerRepository.findSubCustomerIdsOrEmpty(tenantId, customerId);
            if (customerIds.isEmpty()) {
                return PageData.emptyPageData();
            }
            return DaoUtil.toPageData(citusInCustomerIds.apply(customerIds.get().toArray(new UUID[0])));
        }
        return DaoUtil.toPageData(plainIncludingSubCustomers.get());
    }
}
