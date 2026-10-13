// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import L from 'leaflet';
import LeafletMap from '../leaflet-map';
import { DEFAULT_ZOOM_LEVEL, WidgetUnitedMapSettings } from '../map-models';
import { WidgetContext } from '@home/models/widget-component.models';

export class TencentMap extends LeafletMap {
  constructor(ctx: WidgetContext, $container: HTMLElement, options: WidgetUnitedMapSettings) {
    super(ctx, $container, options);
    let mapUuid: string;
    if (this.ctx.reportService.reportView) {
      mapUuid = this.ctx.reportService.onWaitForMap();
    }
    const txUrl = 'https://rt{s}.map.gtimg.com/realtimerender?z={z}&x={x}&y={y}&type=vector&style=0';
    const map = L.map($container, {
      doubleClickZoom: !this.options.disableDoubleClickZooming,
      zoomControl: !this.options.disableZoomControl,
      fadeAnimation: !ctx.reportService.reportView
    }).setView(options?.parsedDefaultCenterPosition, options?.defaultZoomLevel || DEFAULT_ZOOM_LEVEL);
    const txLayer = L.tileLayer(txUrl, {
      subdomains: '0123',
      tms: true,
      attribution: '&copy;2024 Tencent - GS(2023)1171号'
    }).addTo(map);
    txLayer.addTo(map);
    if (this.ctx.reportService.reportView) {
      txLayer.once('load', () => {
        this.ctx.reportService.onMapLoaded(mapUuid);
      });
    }
    super.setMap(map);
  }
}
