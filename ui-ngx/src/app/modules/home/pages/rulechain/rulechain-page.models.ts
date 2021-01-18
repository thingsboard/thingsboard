///
/// Copyright © 2016-2021 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { FcModel } from 'ngx-flowchart/dist/ngx-flowchart';
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
