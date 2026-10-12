// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.common.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.thingsboard.server.common.data.id.ApiUsageStateId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;

import java.io.Serial;

@ToString
@EqualsAndHashCode(callSuper = true)
@Getter
@Setter
public class ApiUsageState extends BaseData<ApiUsageStateId> implements TenantEntity, HasVersion {

    @Serial
    private static final long serialVersionUID = 8250339805336035966L;

    private TenantId tenantId;
    private EntityId entityId;
    private ApiUsageStateValue transportState;
    private ApiUsageStateValue dbStorageState;
    private ApiUsageStateValue reExecState;
    private ApiUsageStateValue jsExecState;
    private ApiUsageStateValue tbelExecState;
    private ApiUsageStateValue emailExecState;
    private ApiUsageStateValue smsExecState;
    private ApiUsageStateValue alarmExecState;
    private ApiUsageStateValue edgeState;
    private ApiUsageStateValue reportExecState;
    private ApiUsageStateValue integrationState;
    private ApiUsageStateValue aiState;
    private Long version;

    public ApiUsageState() {
        super();
    }

    public ApiUsageState(ApiUsageStateId id) {
        super(id);
    }

    public ApiUsageState(ApiUsageState ur) {
        super(ur);
        this.tenantId = ur.getTenantId();
        this.entityId = ur.getEntityId();
        this.transportState = ur.getTransportState();
        this.dbStorageState = ur.getDbStorageState();
        this.reExecState = ur.getReExecState();
        this.jsExecState = ur.getJsExecState();
        this.tbelExecState = ur.getTbelExecState();
        this.emailExecState = ur.getEmailExecState();
        this.smsExecState = ur.getSmsExecState();
        this.alarmExecState = ur.getAlarmExecState();
        this.edgeState = ur.getEdgeState();
        this.reportExecState = ur.getReportExecState();
        this.integrationState = ur.getIntegrationState();
        this.aiState = ur.getAiState();
        this.version = ur.getVersion();
    }

    public boolean isTransportEnabled() {
        return transportState != ApiUsageStateValue.DISABLED;
    }

    public boolean isReExecEnabled() {
        return reExecState != ApiUsageStateValue.DISABLED;
    }

    public boolean isDbStorageEnabled() {
        return dbStorageState != ApiUsageStateValue.DISABLED;
    }

    public boolean isJsExecEnabled() {
        return jsExecState != ApiUsageStateValue.DISABLED;
    }

    public boolean isTbelExecEnabled() {
        return tbelExecState != ApiUsageStateValue.DISABLED;
    }

    public boolean isEmailSendEnabled() {
        return emailExecState != ApiUsageStateValue.DISABLED;
    }

    public boolean isSmsSendEnabled() {
        return smsExecState != ApiUsageStateValue.DISABLED;
    }

    public boolean isAlarmCreationEnabled() {
        return alarmExecState != ApiUsageStateValue.DISABLED;
    }

    public boolean isEdgeEnabled() {
        return edgeState != ApiUsageStateValue.DISABLED;
    }

    public boolean isReportCreationEnabled() {
        return reportExecState != ApiUsageStateValue.DISABLED;
    }

    public boolean isIntegrationEnabled() {
        return integrationState != ApiUsageStateValue.DISABLED;
    }

    public boolean isAiEnabled() {
        return aiState != ApiUsageStateValue.DISABLED;
    }

    @Override
    @JsonIgnore
    public EntityType getEntityType() {
        return EntityType.API_USAGE_STATE;
    }

}
