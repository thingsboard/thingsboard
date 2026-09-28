// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { RuleNodeType } from '@shared/models/rule-node.models';

export enum ComponentType {
  ENRICHMENT = 'ENRICHMENT',
  FILTER = 'FILTER',
  TRANSFORMATION = 'TRANSFORMATION',
  ACTION = 'ACTION',
  ANALYTICS = 'ANALYTICS',
  EXTERNAL = 'EXTERNAL',
  FLOW = 'FLOW'
}

export enum ComponentScope {
  SYSTEM = 'SYSTEM',
  TENANT = 'TENANT'
}

export enum ComponentClusteringMode {
  USER_PREFERENCE = 'USER_PREFERENCE',
  ENABLED = 'ENABLED',
  SINGLETON = 'SINGLETON'
}

export interface ComponentDescriptor {
  type: ComponentType | RuleNodeType;
  scope?: ComponentScope;
  clusteringMode: ComponentClusteringMode;
  hasQueueName?: boolean;
  name: string;
  clazz: string;
  configurationDescriptor?: any;
  actions?: string;
}
