// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { defaultMapSettings, MapSetting, MapType } from '@shared/models/widget/maps/map.models';
import { BackgroundSettings, BackgroundType } from '@shared/models/widget-settings.models';
import { mergeDeep } from '@core/utils';
import type { WidgetContext } from '@home/models/widget-component.models';
import { DeepPartial } from '@shared/models/common';
import type { TbMap } from '@home/components/widget/lib/maps/map';

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

export const createMap = async (ctx: WidgetContext,
                                inputSettings: DeepPartial<MapSetting>,
                                mapElement: HTMLElement): Promise<TbMap<MapSetting>> => {
  switch (inputSettings.mapType) {
    case MapType.geoMap: {
      const { TbGeoMap } = await import('@home/components/widget/lib/maps/geo-map');
      return new TbGeoMap(ctx, inputSettings, mapElement);
    }
    case MapType.image: {
      const { TbImageMap } = await import('@home/components/widget/lib/maps/image-map');
      return new TbImageMap(ctx, inputSettings, mapElement);
    }
  }
}
