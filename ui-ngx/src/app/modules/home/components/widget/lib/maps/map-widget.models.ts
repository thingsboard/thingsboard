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

import { defaultMapSettings, MapSetting } from '@home/components/widget/lib/maps/map.models';
import { BackgroundSettings, BackgroundType } from '@shared/models/widget-settings.models';
import { mergeDeep } from '@core/utils';

export interface MapWidgetSettings extends MapSetting {
  background: BackgroundSettings;
  padding: string;
}

export const mapWidgetDefaultSettings: MapWidgetSettings =
  mergeDeep({} as MapWidgetSettings, defaultMapSettings as MapWidgetSettings, {
    background: {
      type: BackgroundType.color,
      color: '#fff',
      overlay: {
        enabled: false,
        color: 'rgba(255,255,255,0.72)',
        blur: 3
      }
    },
    padding: '8px'
} as MapWidgetSettings);