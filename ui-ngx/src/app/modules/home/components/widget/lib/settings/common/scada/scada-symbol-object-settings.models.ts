///
/// Copyright © 2016-2026 The Thingsboard Authors
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

import {
  ScadaSymbolBehavior
} from '@home/components/widget/lib/scada/scada-symbol.models';

export interface ScadaSymbolBehaviorGroup {
  title?: string;
  behaviors: ScadaSymbolBehavior[];
}

export const toBehaviorGroups = (behaviors: ScadaSymbolBehavior[]): ScadaSymbolBehaviorGroup[] => {
  const result: ScadaSymbolBehaviorGroup[] = [];
  for (const behavior of behaviors) {
    if (!behavior.group) {
      result.push({
        title: null,
        behaviors: [behavior]
      });
    } else {
      let behaviorGroup = result.find(g => g.title === behavior.group);
      if (!behaviorGroup) {
        behaviorGroup = {
          title: behavior.group,
          behaviors: []
        };
        result.push(behaviorGroup);
      }
      behaviorGroup.behaviors.push(behavior);
    }
  }
  return result;
};
