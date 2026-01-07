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

import { defaultMapSettings, MapSetting, MapType } from '@shared/models/widget/maps/map.models';
import { BackgroundSettings, BackgroundType } from '@shared/models/widget-settings.models';
import { mergeDeep } from '@core/utils';
import { WidgetContext } from '@home/models/widget-component.models';
import { DeepPartial } from '@shared/models/common';
import { TbMap } from '@home/components/widget/lib/maps/map';
import { TbGeoMap } from '@home/components/widget/lib/maps/geo-map';
import { TbImageMap } from '@home/components/widget/lib/maps/image-map';

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

export const createMap = (ctx: WidgetContext,
                          inputSettings: DeepPartial<MapSetting>,
                          mapElement: HTMLElement): TbMap<MapSetting> => {
  switch (inputSettings.mapType) {
    case MapType.geoMap:
      return new TbGeoMap(ctx, inputSettings, mapElement);
    case MapType.image:
      return new TbImageMap(ctx, inputSettings, mapElement);
  }
}
