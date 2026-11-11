// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.citus;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.env.Environment;
import org.thingsboard.server.common.data.StringUtils;

/**
 * Appends the mandatory Citus session GUCs to the main pool's {@code connectionInitSql}.
 * <p>
 * This must run as a {@link BeanPostProcessor} rather than inside the {@code dataSource()} factory method in
 * {@code JpaDaoConfig}: the {@code spring.datasource.hikari.*} properties are bound to the {@link HikariDataSource}
 * by {@code ConfigurationPropertiesBindingPostProcessor} in {@code postProcessBeforeInitialization}, i.e. after the
 * factory method returns — init SQL set in the factory method would be overwritten by an operator-configured
 * {@code spring.datasource.hikari.connection-init-sql}, and the operator value is only visible from
 * {@code postProcessAfterInitialization} onward. Mutating the config at that point is still safe: Hikari freezes
 * its configuration only when the pool starts on the first {@code getConnection()}, and nothing can use the
 * DataSource before its own post-processing completes.
 * <p>
 * Reads {@code database.citus.enabled} from the {@link Environment} rather than injecting {@code CitusSettings}:
 * a post-processor's dependencies are instantiated before regular bean post-processing is available, which would
 * break the settings bean's own property binding.
 */
public class CitusConnectionInitSqlPostProcessor implements BeanPostProcessor {

    // citus.multi_shard_modify_mode controls how a single transaction executes a modification that spans
    // multiple shards of a DISTRIBUTED table: 'parallel' (default) opens several connections per worker and
    // writes the affected shards concurrently, while 'sequential' uses one connection per worker and writes
    // them one at a time. We force 'sequential' because Citus rejects a reference-table write that follows a
    // parallel multi-shard operation in the same transaction (the reference-table lock cannot be coordinated
    // across the already fanned-out connections), and our save flows routinely mix distributed- and
    // reference-table writes. The cost is limited: only multi-shard distributed-table writes lose their
    // parallel fan-out (higher latency for bulk writes); single-shard writes (the common OLTP path) use one
    // connection anyway, and reads are unaffected.
    //
    // citus.all_modifications_commutative controls the LOCK MODE for reference-table writes — an axis
    // orthogonal to the mode above. By default Citus takes a cluster-wide table-level ExclusiveLock on
    // reference-table UPDATE/DELETE (to keep the fully-replicated copies consistent), so concurrent writers
    // serialize globally — even single-statement updates/deletes to different rows queue up. Under 'sequential'
    // mode this is serialization, not a deadlock: the writes still complete in order, just without
    // concurrency. Enabling the flag downgrades those writes to RowExclusiveLock, restoring concurrency for
    // different-row writes. The only guarantee given up is cross-replica ordering of non-commutative writes to
    // the same row, which our version-guarded updates and idempotent relation insert/delete paths do not rely on.
    public static final String CITUS_CONNECTION_INIT_SQL = """
            SET citus.multi_shard_modify_mode TO 'sequential';
            SET citus.all_modifications_commutative TO 'on'
            """;

    private final Environment environment;
    private final String dataSourceBeanName;

    public CitusConnectionInitSqlPostProcessor(Environment environment, String dataSourceBeanName) {
        this.environment = environment;
        this.dataSourceBeanName = dataSourceBeanName;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        if (!dataSourceBeanName.equals(beanName) || !(bean instanceof HikariDataSource dataSource)
            || !environment.getProperty("database.citus.enabled", Boolean.class, false)) {
            return bean;
        }
        String existingInitSql = dataSource.getConnectionInitSql();
        if (StringUtils.isBlank(existingInitSql)) {
            dataSource.setConnectionInitSql(CITUS_CONNECTION_INIT_SQL);
        } else if (!existingInitSql.contains(CITUS_CONNECTION_INIT_SQL)) {
            // Operator-configured statements first, Citus GUCs last so they win over anything conflicting
            dataSource.setConnectionInitSql(existingInitSql + "; " + CITUS_CONNECTION_INIT_SQL);
        }
        return bean;
    }

}
