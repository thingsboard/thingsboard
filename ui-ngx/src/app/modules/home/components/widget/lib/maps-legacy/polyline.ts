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

// @ts-ignore
import 'leaflet-polylinedecorator';
import L, { PolylineDecorator, PolylineDecoratorOptions, Symbol } from 'leaflet';

import { WidgetPolylineSettings } from './map-models';
import { functionValueCalculator } from '@home/components/widget/lib/maps-legacy/common-maps-utils';
import { FormattedData } from '@shared/models/widget.models';

export class Polyline {

  leafletPoly: L.Polyline;
  polylineDecorator: PolylineDecorator;

  constructor(private map: L.Map,
              locations: L.LatLng[],
              private data: FormattedData,
              private dataSources: FormattedData[],
              settings: Partial<WidgetPolylineSettings>) {

    this.leafletPoly = L.polyline(locations,
      this.getPolyStyle(settings)
    ).addTo(this.map);

    if (settings.usePolylineDecorator) {
      this.polylineDecorator = new PolylineDecorator(this.leafletPoly, this.getDecoratorSettings(settings)).addTo(this.map);
    }
  }

  getDecoratorSettings(settings: Partial<WidgetPolylineSettings>): PolylineDecoratorOptions {
    return {
      patterns: [
        {
          offset: settings.decoratorOffset,
          endOffset: settings.endDecoratorOffset,
          repeat: settings.decoratorRepeat,
          symbol: Symbol[settings.decoratorSymbol]({
            pixelSize: settings.decoratorSymbolSize,
            polygon: false,
            pathOptions: {
              color: settings.useDecoratorCustomColor ? settings.decoratorCustomColor : this.getPolyStyle(settings).color,
              stroke: true
            }
          })
        }
      ]
    };
  }

  updatePolyline(locations: L.LatLng[], data: FormattedData, dataSources: FormattedData[], settings: Partial<WidgetPolylineSettings>) {
    this.data = data;
    this.dataSources = dataSources;
    this.leafletPoly.setLatLngs(locations);
    this.leafletPoly.setStyle(this.getPolyStyle(settings));
    if (this.polylineDecorator) {
      this.polylineDecorator.setPaths(this.leafletPoly);
    }
  }

  getPolyStyle(settings: Partial<WidgetPolylineSettings>): L.PolylineOptions {
    return {
      interactive: false,
      color: functionValueCalculator(settings.useColorFunction, settings.parsedColorFunction,
        [this.data, this.dataSources, this.data.dsIndex], settings.color),
      opacity: functionValueCalculator(settings.useStrokeOpacityFunction, settings.parsedStrokeOpacityFunction,
        [this.data, this.dataSources, this.data.dsIndex], settings.strokeOpacity),
      weight: functionValueCalculator(settings.useStrokeWeightFunction, settings.parsedStrokeWeightFunction,
        [this.data, this.dataSources, this.data.dsIndex], settings.strokeWeight),
      pmIgnore: true
    };
  }

  removePolyline() {
    this.map.removeLayer(this.leafletPoly);
  }

  getPolylineLatLngs() {
    return this.leafletPoly.getLatLngs();
  }
}
