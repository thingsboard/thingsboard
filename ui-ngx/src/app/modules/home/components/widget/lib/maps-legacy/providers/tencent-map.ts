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


import L from 'leaflet';
import LeafletMap from '../leaflet-map';
import { DEFAULT_ZOOM_LEVEL, WidgetUnitedMapSettings } from '../map-models';
import { WidgetContext } from '@home/models/widget-component.models';

export class TencentMap extends LeafletMap {
  constructor(ctx: WidgetContext, $container: HTMLElement, options: WidgetUnitedMapSettings) {
    super(ctx, $container, options);
    const txUrl = 'https://rt{s}.map.gtimg.com/realtimerender?z={z}&x={x}&y={y}&type=vector&style=0';
    const map = L.map($container, {
      doubleClickZoom: !this.options.disableDoubleClickZooming,
      zoomControl: !this.options.disableZoomControl
    }).setView(options?.parsedDefaultCenterPosition, options?.defaultZoomLevel || DEFAULT_ZOOM_LEVEL);
    const txLayer = L.tileLayer(txUrl, {
      subdomains: '0123',
      tms: true,
      attribution: '&copy;2024 Tencent - GS(2023)1171号'
    }).addTo(map);
    txLayer.addTo(map);
    super.setMap(map);
  }
}
