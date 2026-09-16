// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { TenantId } from '@shared/models/id/tenant-id';
import { RpcId } from '@shared/models/id/rpc-id';
import { DeviceId } from '@shared/models/id/device-id';
import { TableCellButtonActionDescriptor } from '@home/components/widget/lib/table-widget.models';

export enum RpcStatus {
  QUEUED = 'QUEUED',
  DELIVERED = 'DELIVERED',
  SUCCESSFUL = 'SUCCESSFUL',
  TIMEOUT = 'TIMEOUT',
  FAILED = 'FAILED',
  SENT = 'SENT',
  EXPIRED = 'EXPIRED'
}

export const rpcStatusColors = new Map<RpcStatus, string>(
  [
    [RpcStatus.QUEUED, 'black'],
    [RpcStatus.DELIVERED, 'green'],
    [RpcStatus.SUCCESSFUL, 'green'],
    [RpcStatus.TIMEOUT, 'orange'],
    [RpcStatus.FAILED, 'red'],
    [RpcStatus.SENT, 'green'],
    [RpcStatus.EXPIRED, 'red']
  ]
);

export const rpcStatusTranslation = new Map<RpcStatus, string>(
  [
    [RpcStatus.QUEUED, 'widgets.persistent-table.rpc-status.QUEUED'],
    [RpcStatus.DELIVERED, 'widgets.persistent-table.rpc-status.DELIVERED'],
    [RpcStatus.SUCCESSFUL, 'widgets.persistent-table.rpc-status.SUCCESSFUL'],
    [RpcStatus.TIMEOUT, 'widgets.persistent-table.rpc-status.TIMEOUT'],
    [RpcStatus.FAILED, 'widgets.persistent-table.rpc-status.FAILED'],
    [RpcStatus.SENT, 'widgets.persistent-table.rpc-status.SENT'],
    [RpcStatus.EXPIRED, 'widgets.persistent-table.rpc-status.EXPIRED']
  ]
);

export interface PersistentRpc {
  id: RpcId;
  createdTime: number;
  expirationTime: number;
  status: RpcStatus;
  response: any;
  request: {
    id: string;
    oneway: boolean;
    body: {
      method: string;
      params: string;
    };
    retries: null | number;
  };
  deviceId: DeviceId;
  tenantId: TenantId;
  additionalInfo?: string;
}

export interface PersistentRpcData extends PersistentRpc {
  actionCellButtons?: TableCellButtonActionDescriptor[];
  hasActions?: boolean;
}

export interface RequestData {
  method?: string;
  oneWayElseTwoWay?: boolean;
  persistentPollingInterval?: number;
  retries?: number;
  params?: object;
  additionalInfo?: object;
}
