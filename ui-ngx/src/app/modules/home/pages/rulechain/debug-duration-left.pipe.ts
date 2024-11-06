///
/// Copyright © 2016-2024 The Thingsboard Authors
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

import { Pipe, PipeTransform } from '@angular/core';
import { MINUTE } from '@shared/models/time/time.models';

@Pipe({
  name: 'debugDurationLeft',
  pure: false,
  standalone: true,
})
export class DebugDurationLeftPipe implements PipeTransform {
  transform(lastUpdateTs: number, maxRuleNodeDebugDurationMinutes: number): string {
    const minutes = Math.max(0, Math.floor(this.getDebugTime(lastUpdateTs, maxRuleNodeDebugDurationMinutes) / MINUTE));
    return `${minutes} min left`;
  }

  getDebugTime(lastUpdateTs: number, maxRuleNodeDebugDurationMinutes: number): number {
    const maxDuration = maxRuleNodeDebugDurationMinutes * MINUTE;
    const updateDuration = lastUpdateTs ? new Date().getTime() - lastUpdateTs : 0;
    const leftTime = maxDuration - updateDuration;
    return leftTime > 0 ? leftTime : 0;
  }
}
