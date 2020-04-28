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

import { safeExecute } from '@app/core/utils';
import { PolylineSettings } from './map-models';

export class Polyline {

    leafletPoly: L.Polyline;
    polylineDecorator: L.PolylineDecorator;
    dataSources;
    data;

    constructor(private map: L.Map, locations, data, dataSources, settings: PolylineSettings) {
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
            ],
            interactive: false,
        } as PolylineDecoratorOptions
    }

    updatePolyline(settings, data, dataSources) {
        this.data = data;
        this.dataSources = dataSources;
        this.leafletPoly.setStyle(this.getPolyStyle(settings));
        //  this.setPolylineLatLngs(data);
        if (this.polylineDecorator)
            this.polylineDecorator.setPaths(this.leafletPoly);
    }

    getPolyStyle(settings: PolylineSettings): L.PolylineOptions {
        return {
            color: settings.useColorFunction ?
                safeExecute(settings.colorFunction,
                    [this.data, this.dataSources, this.dataSources[0]?.dsIndex]) : settings.color,
            opacity: settings.useStrokeOpacityFunction ?
                safeExecute(settings.strokeOpacityFunction,
                    [this.data, this.dataSources, this.dataSources[0]?.dsIndex]) : settings.strokeOpacity,
            weight: settings.useStrokeWeightFunction ?
                safeExecute(settings.strokeWeightFunction,
                    [this.data, this.dataSources, this.dataSources[0]?.dsIndex]) : settings.strokeWeight,
        }
    }

    removePolyline() {
        this.map.removeLayer(this.leafletPoly);
    }

    getPolylineLatLngs() {
        return this.leafletPoly.getLatLngs();
    }

    setPolylineLatLngs(latLngs) {
        this.leafletPoly.setLatLngs(latLngs);
    }
}