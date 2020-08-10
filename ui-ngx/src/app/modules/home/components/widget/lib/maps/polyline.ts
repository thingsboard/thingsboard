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

import L, { PolylineDecoratorOptions } from 'leaflet';
import 'leaflet-polylinedecorator';

import { FormattedData, PolylineSettings } from './map-models';
import { functionValueCalculator } from '@home/components/widget/lib/maps/maps-utils';

export class Polyline {

  leafletPoly: L.Polyline;
  polylineDecorator: L.PolylineDecorator;
  dataSources: FormattedData[];
  data: FormattedData;

  constructor(private map: L.Map, locations: L.LatLng[], data: FormattedData, dataSources: FormattedData[], settings: PolylineSettings) {
    this.dataSources = dataSources;
    this.data = data;

    this.leafletPoly = L.polyline(locations,
      this.getPolyStyle(settings)
    ).addTo(this.map);

    if (settings.usePolylineDecorator) {
      this.polylineDecorator = L.polylineDecorator(this.leafletPoly, this.getDecoratorSettings(settings)).addTo(this.map);
    }
  }

  getDecoratorSettings(settings: PolylineSettings): PolylineDecoratorOptions {
    return {
      patterns: [
        {
          offset: settings.decoratorOffset,
          endOffset: settings.endDecoratorOffset,
          repeat: settings.decoratorRepeat,
          symbol: L.Symbol[settings.decoratorSymbol]({
            pixelSize: settings.decoratorSymbolSize,
            polygon: false,
            pathOptions: {
              color: settings.useDecoratorCustomColor ? settings.decoratorCustomColor : this.getPolyStyle(settings).color,
              stroke: true
            }
          })
        }
      ]
    }
  }

  updatePolyline(locations: L.LatLng[], data: FormattedData, dataSources: FormattedData[], settings: PolylineSettings) {
    this.data = data;
    this.dataSources = dataSources;
    this.leafletPoly.setLatLngs(locations);
    this.leafletPoly.setStyle(this.getPolyStyle(settings));
    if (this.polylineDecorator)
      this.polylineDecorator.setPaths(this.leafletPoly);
  }

  getPolyStyle(settings: PolylineSettings): L.PolylineOptions {
    return {
      interactive: false,
      color: functionValueCalculator(settings.useColorFunction, settings.colorFunction,
        [this.data, this.dataSources, this.data.dsIndex], settings.color),
      opacity: functionValueCalculator(settings.useStrokeOpacityFunction, settings.strokeOpacityFunction,
        [this.data, this.dataSources, this.data.dsIndex], settings.strokeOpacity),
      weight: functionValueCalculator(settings.useStrokeWeightFunction, settings.strokeWeightFunction,
        [this.data, this.dataSources, this.data.dsIndex], settings.strokeWeight)
    }
  }

  removePolyline() {
    this.map.removeLayer(this.leafletPoly);
  }

  getPolylineLatLngs() {
    return this.leafletPoly.getLatLngs();
  }
}
