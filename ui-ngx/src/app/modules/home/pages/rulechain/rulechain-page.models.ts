// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { FcModel } from 'ngx-flowchart';
import { FcRuleEdge, FcRuleNode, FcRuleNodeType } from '@shared/models/rule-node.models';

export interface FcRuleNodeTypeModel extends FcModel {
  nodes: Array<FcRuleNodeType>;
}

export interface FcRuleNodeModel extends FcModel {
  nodes: Array<FcRuleNode>;
  edges: Array<FcRuleEdge>;
}

export interface RuleChainMenuItem {
  action?: ($event: MouseEvent) => void;
  enabled?: boolean;
  value?: string;
  icon?: string;
  shortcut?: string;
  divider?: boolean;
}

export interface RuleChainMenuContextInfo {
  headerClass: string;
  icon: string;
  iconUrl?: string;
  title: string;
  subtitle: string;
  menuItems: RuleChainMenuItem[];
}
