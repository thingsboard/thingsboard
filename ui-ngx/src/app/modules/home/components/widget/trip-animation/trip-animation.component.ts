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

import { Component, OnInit, Input, ViewChild, AfterViewInit, ChangeDetectorRef } from '@angular/core';
import { MapWidgetController, TbMapWidgetV2 } from '../lib/maps/map-widget2';
import { MapProviders } from '../lib/maps/map-models';
import { parseArray } from '@app/core/utils';
import { interpolateArray } from '../lib/maps/maps-utils';
import tinycolor from "tinycolor2";
import { initSchema, addToSchema, addGroupInfo } from '@app/core/schema-utils';
import { tripAnimationSchema } from '../lib/maps/schemes';

@Component({
  selector: 'trip-animation',
  templateUrl: './trip-animation.component.html',
  styleUrls: ['./trip-animation.component.scss']
})
export class TripAnimationComponent implements OnInit, AfterViewInit {

  @Input() ctx;

  @ViewChild('map') mapContainer;

  mapWidget: MapWidgetController;
  historicalData;
  intervals;
  normalizationStep = 1000;
  interpolatedData = [];
  widgetConfig;
  settings;

  constructor(private cd: ChangeDetectorRef) { }

  ngOnInit(): void {
    this.widgetConfig = this.ctx.widgetConfig;
    const settings = {
      normalizationStep: 1000,
      buttonColor: tinycolor(this.widgetConfig.color).setAlpha(0.54).toRgbString(),
      disabledButtonColor: tinycolor(this.widgetConfig.color).setAlpha(0.3).toRgbString(),     
      rotationAngle: 0
    }
    this.settings = { ...settings, ...this.ctx.settings };
    let subscription = this.ctx.subscriptions[Object.keys(this.ctx.subscriptions)[0]];
    if (subscription) subscription.callbacks.onDataUpdated = (updated) => {
      this.historicalData = parseArray(this.ctx.data);
      this.historicalData.forEach(ds => ds.forEach(el => {
        el.longitude += (Math.random() - 0.5)
        el.latitude += (Math.random() - 0.5)
      }));
      this.calculateIntervals();
      this.timeUpdated(this.intervals[0]);
      this.mapWidget.map.map.invalidateSize();
      this.cd.detectChanges();
    }
  }

  ngAfterViewInit() {
    this.mapWidget = new MapWidgetController(MapProviders.openstreet, false, this.ctx, this.mapContainer.nativeElement);
  }

  timeUpdated(time) {
    const currentPosition = this.interpolatedData.map(dataSource => dataSource[time]);
      this.mapWidget.map.updateMarkers(currentPosition);
  }

  calculateIntervals() {
    this.historicalData.forEach((dataSource, index) => {
      this.intervals = [];
      for (let time = dataSource[0]?.time; time < dataSource[dataSource.length - 1]?.time; time += this.normalizationStep) {
        this.intervals.push(time);
      }
      this.intervals.push(dataSource[dataSource.length - 1]?.time);
      this.interpolatedData[index] = interpolateArray(dataSource, this.intervals);
    });
  }

  static getSettingsSchema() {
    let schema = initSchema();
    addToSchema(schema, TbMapWidgetV2.getProvidersSchema());
    addGroupInfo(schema, "Map Provider Settings");
    addToSchema(schema, tripAnimationSchema);
    addGroupInfo(schema, "Trip Animation Settings");
    return schema;
  }
}

export let TbTripAnimationWidget = TripAnimationComponent;