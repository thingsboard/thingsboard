// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import L from 'leaflet';
import LeafletMap from '../leaflet-map';
import { DEFAULT_ZOOM_LEVEL, WidgetUnitedMapSettings } from '../map-models';
import 'leaflet.gridlayer.googlemutant';
import { ResourcesService } from '@core/services/resources.service';
import { WidgetContext } from '@home/models/widget-component.models';

const gmGlobals: GmGlobal = {};

interface GmGlobal {
  [key: string]: boolean;
}

export class GoogleMap extends LeafletMap {
  private resource: ResourcesService;

  constructor(ctx: WidgetContext, $container: HTMLElement, options: WidgetUnitedMapSettings) {
    super(ctx, $container, options);
    let mapUuid: string;
    if (this.ctx.reportService.reportView) {
      mapUuid = this.ctx.reportService.onWaitForMap();
    }
    this.resource = ctx.$injector.get(ResourcesService);
    this.loadGoogle(() => {
      const map = L.map($container, {
        attributionControl: false,
        doubleClickZoom: !this.options.disableDoubleClickZooming,
        zoomControl: !this.options.disableZoomControl,
        fadeAnimation: !ctx.reportService.reportView
      }).setView(options?.parsedDefaultCenterPosition, options?.defaultZoomLevel || DEFAULT_ZOOM_LEVEL);
      const tileLayer = (L.gridLayer as any).googleMutant({
        type: options?.gmDefaultMapType || 'roadmap'
      });
      tileLayer.addTo(map);
      if (this.ctx.reportService.reportView) {
        tileLayer.once('load', () => {
          this.ctx.reportService.onMapLoaded(mapUuid);
        });
      }
      super.setMap(map);
    }, options.gmApiKey);
  }

  private loadGoogle(callback: () => void, apiKey = 'AIzaSyDoEx2kaGz3PxwbI9T7ccTSg5xjdw8Nw8Q') {
    if (gmGlobals[apiKey]) {
      callback();
    } else {
      this.resource.loadResource(`https://maps.googleapis.com/maps/api/js?key=${apiKey}`).subscribe({
        next: () => {
          gmGlobals[apiKey] = true;
          callback();
        },
        error: (error) => {
          gmGlobals[apiKey] = false;
          console.error(`Google map api load failed!`, error);
        }
      });
    }
  }
}
