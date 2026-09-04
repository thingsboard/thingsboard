// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
    this.resource = ctx.$injector.get(ResourcesService);
    this.loadGoogle(() => {
      const map = L.map($container, {
        attributionControl: false,
        doubleClickZoom: !this.options.disableDoubleClickZooming,
        zoomControl: !this.options.disableZoomControl
      }).setView(options?.parsedDefaultCenterPosition, options?.defaultZoomLevel || DEFAULT_ZOOM_LEVEL);
      (L.gridLayer as any).googleMutant({
        type: options?.gmDefaultMapType || 'roadmap'
      }).addTo(map);
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
