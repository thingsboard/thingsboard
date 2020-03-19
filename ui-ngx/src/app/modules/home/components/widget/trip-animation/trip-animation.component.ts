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
import _ from 'lodash';
import tinycolor from 'tinycolor2';
import { interpolateOnPointSegment } from 'leaflet-geometryutil';

import { Component, OnInit, Input, ViewChild, AfterViewInit, ChangeDetectorRef, SecurityContext } from '@angular/core';
import { MapWidgetController, TbMapWidgetV2 } from '../lib/maps/map-widget2';
import { MapProviders } from '../lib/maps/map-models';
import { parseArray, parseTemplate, safeExecute } from '@app/core/utils';
import { initSchema, addToSchema, addGroupInfo } from '@app/core/schema-utils';
import { tripAnimationSchema } from '../lib/maps/schemes';
import { DomSanitizer } from '@angular/platform-browser';
import { WidgetConfig, JsonSchema, JsonSettingsSchema } from '@app/shared/public-api';
import { WidgetContext } from '@app/modules/home/models/widget-component.models';
import { getRatio, findAngle } from '../lib/maps/maps-utils';


@Component({
  selector: 'trip-animation',
  templateUrl: './trip-animation.component.html',
  styleUrls: ['./trip-animation.component.scss']
})
export class TripAnimationComponent implements OnInit, AfterViewInit {

  constructor(private cd: ChangeDetectorRef, private sanitizer: DomSanitizer) { }

  @Input() ctx: WidgetContext;

  @ViewChild('map') mapContainer;

  mapWidget: MapWidgetController;
  historicalData;
  intervals;
  normalizationStep = 1000;
  interpolatedData = [];
  widgetConfig: WidgetConfig;
  settings;
  mainTooltip = '';
  visibleTooltip = false;
  activeTrip;

  static getSettingsSchema(): JsonSettingsSchema {
    const schema = initSchema();
    addToSchema(schema, TbMapWidgetV2.getProvidersSchema());
    addGroupInfo(schema, 'Map Provider Settings');
    addToSchema(schema, tripAnimationSchema);
    addGroupInfo(schema, 'Trip Animation Settings');
    return schema;
  }

  ngOnInit(): void {
    this.widgetConfig = this.ctx.widgetConfig;
    const settings = {
      normalizationStep: 1000,
      showLabel: false,
      buttonColor: tinycolor(this.widgetConfig.color).setAlpha(0.54).toRgbString(),
      disabledButtonColor: tinycolor(this.widgetConfig.color).setAlpha(0.3).toRgbString(),
      rotationAngle: 0
    }
    this.settings = { ...settings, ...this.ctx.settings };
    const subscription = this.ctx.subscriptions[Object.keys(this.ctx.subscriptions)[0]];
    if (subscription) subscription.callbacks.onDataUpdated = (updated) => {
      this.historicalData = parseArray(this.ctx.data);
      this.activeTrip = this.historicalData[0][0];
      this.calculateIntervals();
      this.timeUpdated(this.intervals[0]);
      this.mapWidget.map.updatePolylines(this.interpolatedData.map(ds => _.values(ds)));

      this.mapWidget.map.map?.invalidateSize();
      this.cd.detectChanges();
    }
  }

  ngAfterViewInit() {
    const ctxCopy: WidgetContext = _.cloneDeep(this.ctx);
    ctxCopy.settings.showLabel = false;
    ctxCopy.settings.showTooltip = false;
    this.mapWidget = new MapWidgetController(MapProviders.openstreet, false, ctxCopy, this.mapContainer.nativeElement);
  }

  timeUpdated(time: number) {
    const currentPosition = this.interpolatedData.map(dataSource => dataSource[time]);
    this.activeTrip = currentPosition[0];
    console.log("TripAnimationComponent -> timeUpdated -> this.interpolatedData", this.interpolatedData)
    if (this.settings.showPolygon) {
      this.mapWidget.map.updatePolygons(this.interpolatedData);
    }
    this.mapWidget.map.updateMarkers(currentPosition);
  }

  setActiveTrip() {

  }

  calculateIntervals() {
    this.historicalData.forEach((dataSource, index) => {
      this.intervals = [];
      for (let time = dataSource[0]?.time; time < dataSource[dataSource.length - 1]?.time; time += this.normalizationStep) {
        this.intervals.push(time);
      }
      this.intervals.push(dataSource[dataSource.length - 1]?.time);
      this.interpolatedData[index] = this.interpolateArray(dataSource, this.intervals);
    });
  }

  showHideTooltip() {
    const tooltipText: string = this.settings.useTooltipFunction ? safeExecute(this.settings.tooolTipFunction, [this.activeTrip, this.historicalData, 0])
      : this.settings.tooltipPattern;

    this.mainTooltip = this.sanitizer.sanitize(SecurityContext.HTML, parseTemplate(tooltipText, this.activeTrip))
    console.log("TripAnimationComponent -> showHideTooltip -> this.mainTooltip", this.mainTooltip)
    this.visibleTooltip = !this.visibleTooltip;
  }

  interpolateArray(originData, interpolatedIntervals) {

    const result = {};

    for (let i = 1, j = 0; i < originData.length, j < interpolatedIntervals.length;) {
      const currentTime = interpolatedIntervals[j];
      while (originData[i].time < currentTime) i++;
      const before = originData[i - 1];
      const after = originData[i];
      const interpolation = interpolateOnPointSegment(
        new L.Point(before.latitude, before.longitude),
        new L.Point(after.latitude, after.longitude),
        getRatio(before.time, after.time, currentTime));
      result[currentTime] = ({
        ...originData[i],
        rotationAngle: findAngle(before, after) + this.settings.rotationAngle,
        latitude: interpolation.x,
        longitude: interpolation.y
      });
      j++;
    }
    return result;
  }
}

export let TbTripAnimationWidget = TripAnimationComponent;

