// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.sql.query;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.type.descriptor.jdbc.UUIDJdbcType;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.permission.QueryContext;

import java.sql.Array;
import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
public class SqlQueryContext implements SqlParameterSource {
    private static final UUIDJdbcType UUID_TYPE = UUIDJdbcType.INSTANCE;

    @Getter
    private final QueryContext securityCtx;
    private final StringBuilder query;
    private final Map<String, Parameter> params;

    /**
     * Deployment-wide Citus flag, carried on the context (which is already threaded through every query-build
     * step) so the latest-value join builders can read it from here instead of taking it as a parameter.
     */
    @Getter
    private final boolean citusEnabled;

    /**
     * True when the entity-data query is eligible for the Citus co-located pushdown rewrite (single entity table
     * FROM and Citus enabled). Computed once in findEntityDataByQuery and read by the latest-join builders.
     */
    @Getter
    @Setter
    private boolean pushdownEligible;

    /**
     * Monotonic sequence used to mint unique parameter names for values that are resolved in Java during
     * query-building (e.g. Citus reference-table recursions pre-resolved to id lists). A single query may
     * splice several such resolved fragments, so each needs a distinct bind-parameter name.
     */
    private int resolvedParamSeq;

    /**
     * Memoizes reference-table recursions resolved during query-building: recursive SQL text -> the spliced
     * {@code (select unnest(:param))} (or {@code (null)}) fragment. A single query build can request the same recursion (notably
     * the sub-customers subtree) many times, and its result depends only on ctx params that are fixed for the
     * whole build, so the first resolution is reused. The context is built and consumed by a single thread per
     * query, so a plain HashMap suffices.
     */
    @Getter
    private final Map<String, String> resolvedRecursions = new HashMap<>();

    /**
     * Returns a unique bind-parameter name with the given prefix for a value resolved during query-building.
     */
    public String nextResolvedParamName(String prefix) {
        return prefix + "_" + (resolvedParamSeq++);
    }

    /**
     * @param citusEnabled must be the real {@code database.citus.enabled} value for the calling component. The
     *                     query-build helpers branch on {@link #isCitusEnabled()}, so a context built with a wrong
     *                     {@code false} here silently emits plain-mode SQL (e.g. an embedded {@code WITH RECURSIVE})
     *                     inside a distributed query. There is deliberately no defaulting overload.
     */
    public SqlQueryContext(org.thingsboard.server.common.data.permission.QueryContext securityCtx, boolean citusEnabled) {
        this.securityCtx = securityCtx;
        this.citusEnabled = citusEnabled;
        query = new StringBuilder();
        params = new HashMap<>();
    }

    void addParameter(String name, Object value, int type, String typeName) {
        Parameter newParam = new Parameter(value, type, typeName);
        Parameter oldParam = params.put(name, newParam);
        if (oldParam != null && oldParam.value != null && !oldParam.value.equals(newParam.value)) {
            throw new RuntimeException("Parameter with name: " + name + " was already registered!");
        }
        if (value == null) {
            log.warn("[{}][{}][{}] Trying to set null value", getTenantId(), getCustomerId(), name);
        }
    }

    public void append(String s) {
        query.append(s);
    }

    @Override
    public boolean hasValue(String paramName) {
        return params.containsKey(paramName);
    }

    @Override
    public Object getValue(String paramName) throws IllegalArgumentException {
        return checkParameter(paramName).value;
    }

    @Override
    public int getSqlType(String paramName) {
        return checkParameter(paramName).type;
    }

    private Parameter checkParameter(String paramName) {
        Parameter param = params.get(paramName);
        if (param == null) {
            throw new RuntimeException("Parameter with name: " + paramName + " is not set!");
        }
        return param;
    }

    @Override
    public String getTypeName(String paramName) {
        return params.get(paramName).name;
    }

    @Override
    public String[] getParameterNames() {
        return params.keySet().toArray(new String[]{});
    }

    public void addUuidParameter(String name, UUID value) {
        addParameter(name, value, UUID_TYPE.getJdbcTypeCode(), UUID_TYPE.getFriendlyName());
    }

    public void addStringParameter(String name, String value) {
        addParameter(name, value, Types.VARCHAR, "VARCHAR");
    }

    public void addDoubleParameter(String name, double value) {
        addParameter(name, value, Types.DOUBLE, "DOUBLE");
    }

    public void addLongParameter(String name, long value) {
        addParameter(name, value, Types.BIGINT, "BIGINT");
    }

    public void addStringListParameter(String name, List<String> value) {
        addParameter(name, value, Types.VARCHAR, "VARCHAR");
    }

    public void addBooleanParameter(String name, boolean value) {
        addParameter(name, value, Types.BOOLEAN, "BOOLEAN");
    }

    public void addUuidListParameter(String name, List<UUID> value) {
        addParameter(name, value, UUID_TYPE.getJdbcTypeCode(), UUID_TYPE.getFriendlyName());
    }

    /**
     * Binds a pre-built SQL {@link java.sql.Array} (e.g. {@code uuid[]} / {@code varchar[]} / {@code bigint[]} / {@code int[]})
     * as a single {@link Types#ARRAY} parameter. Used for {@code unnest(:param)} fan-outs so large value lists are bound
     * rather than inlined as SQL literals.
     */
    public void addArrayParameter(String name, Array value) {
        addParameter(name, value, Types.ARRAY, "ARRAY");
    }

    public String getQuery() {
        return query.toString();
    }

    public boolean isTenantUser() {
        return securityCtx.isTenantUser();
    }

    public UUID getOwnerId() {
        if (isTenantUser()) {
            return securityCtx.getTenantId().getId();
        } else {
            return securityCtx.getCustomerId().getId();
        }
    }

    public EntityId getStateEntityOwnerId() {
        return securityCtx.getOwnerId();
    }

    public static class Parameter {
        private final Object value;
        private final int type;
        private final String name;

        public Parameter(Object value, int type, String name) {
            this.value = value;
            this.type = type;
            this.name = name;
        }
    }

    public TenantId getTenantId() {
        return securityCtx.getTenantId();
    }

    public CustomerId getCustomerId() {
        return securityCtx.getCustomerId();
    }

    public EntityType getEntityType() {
        return securityCtx.getEntityType();
    }

    public boolean isIgnorePermissionCheck() {
        return securityCtx.isIgnorePermissionCheck();
    }
}
