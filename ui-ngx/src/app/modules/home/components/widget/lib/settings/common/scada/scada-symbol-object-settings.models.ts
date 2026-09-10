// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
