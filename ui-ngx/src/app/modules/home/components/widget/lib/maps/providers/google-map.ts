///
/// Copyright © 2016-2020 The Thingsboard Authors
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
import { DEFAULT_ZOOM_LEVEL, UnitedMapSettings } from '../map-models';
import 'leaflet.gridlayer.googlemutant';
import { ResourcesService } from '@core/services/resources.service';
import { WidgetContext } from '@home/models/widget-component.models';

const gmGlobals: GmGlobal = {};

interface GmGlobal {
  [key: string]: boolean;
}

export class GoogleMap extends LeafletMap {
  private resource: ResourcesService;

  constructor(ctx: WidgetContext, $container, options: UnitedMapSettings) {
    super(ctx, $container, options);
    this.resource = ctx.$injector.get(ResourcesService);
    super.initSettings(options);
    this.loadGoogle(() => {
      const map = L.map($container, {
        attributionControl: false,
        editable: !!options.editablePolygon
      }).setView(options?.defaultCenterPosition, options?.defaultZoomLevel || DEFAULT_ZOOM_LEVEL);
      (L.gridLayer as any).googleMutant({
        type: options?.gmDefaultMapType || 'roadmap'
      }).addTo(map);
      super.setMap(map);
    }, options.gmApiKey);
  }

  private loadGoogle(callback, apiKey = 'AIzaSyDoEx2kaGz3PxwbI9T7ccTSg5xjdw8Nw8Q') {
    if (gmGlobals[apiKey]) {
      callback();
    } else {
      this.resource.loadResource(`https://maps.googleapis.com/maps/api/js?key=${apiKey}`).subscribe(
        () => {
          gmGlobals[apiKey] = true;
          callback();
        },
        (error) => {
          gmGlobals[apiKey] = false;
          console.error(`Google map api load failed!`, error);
        }
      );
    }
  }
}
