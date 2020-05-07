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

import { AfterViewInit, ChangeDetectorRef, Component, Input, OnInit, SecurityContext, ViewChild } from '@angular/core';
import { MapWidgetController, TbMapWidgetV2 } from '../lib/maps/map-widget2';
import { MapProviders, FormattedData } from '../lib/maps/map-models';
import { initSchema, addToSchema, addGroupInfo, addCondition } from '@app/core/schema-utils';
import { tripAnimationSchema, mapPolygonSchema, pathSchema, pointSchema } from '../lib/maps/schemes';
import { DomSanitizer } from '@angular/platform-browser';
import { WidgetContext } from '@app/modules/home/models/widget-component.models';
import { findAngle, getRatio, parseArray, parseWithTranslation, safeExecute } from '../lib/maps/maps-utils';
import { JsonSettingsSchema, WidgetConfig } from '@shared/models/widget.models';
import moment from 'moment';


@Component({
  // tslint:disable-next-line:component-selector
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
  label;
  minTime;
  maxTime;
  anchors = [];
  useAnchors = false;

  static getSettingsSchema(): JsonSettingsSchema {
    const schema = initSchema();
    addToSchema(schema, TbMapWidgetV2.getProvidersSchema(null, true));
    addGroupInfo(schema, 'Map Provider Settings');
    addToSchema(schema, tripAnimationSchema);
    addGroupInfo(schema, 'Trip Animation Settings');
    addToSchema(schema, pathSchema);
    addGroupInfo(schema, 'Path Settings');
    addToSchema(schema, addCondition(pointSchema, 'model.showPoints === true', ['showPoints']));
    addGroupInfo(schema, 'Path Points Settings');
    addToSchema(schema, addCondition(mapPolygonSchema, 'model.showPolygon === true', ['showPolygon']));
    addGroupInfo(schema, 'Polygon Settings');
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
    this.useAnchors = this.settings.usePointAsAnchor && this.settings.showPoints;
    this.settings.fitMapBounds = true;
    this.normalizationStep = this.settings.normalizationStep;
    const subscription = this.ctx.subscriptions[Object.keys(this.ctx.subscriptions)[0]];
    if (subscription) subscription.callbacks.onDataUpdated = () => {
      this.historicalData = parseArray(this.ctx.data);
      this.activeTrip = this.historicalData[0][0];
      this.calculateIntervals();
      this.timeUpdated(this.intervals[0]);
      this.mapWidget.map.map?.invalidateSize();
      this.cd.detectChanges();
    }
  }

  ngAfterViewInit() {
    const ctxCopy: WidgetContext = _.cloneDeep(this.ctx);
    this.mapWidget = new MapWidgetController(MapProviders.openstreet, false, ctxCopy, this.mapContainer.nativeElement);
  }

  timeUpdated(time: number) {
    const currentPosition = this.interpolatedData.map(dataSource => dataSource[time]);
    this.activeTrip = currentPosition[0];
    this.minTime = moment(this.intervals[this.intervals.length - 1]).format('YYYY-MM-DD HH:mm:ss')
    this.maxTime = moment(this.intervals[0]).format('YYYY-MM-DD HH:mm:ss')
    this.calcLabel();
    this.calcTooltip();
    if (this.mapWidget) {
      this.mapWidget.map.updatePolylines(this.interpolatedData.map(ds => _.values(ds)));
      if (this.settings.showPolygon) {
        this.mapWidget.map.updatePolygons(this.interpolatedData);
      }
      if (this.settings.showPoints) {
        this.mapWidget.map.updatePoints(this.historicalData[0], this.calcTooltip);
        this.anchors = this.historicalData[0]
          .filter(data =>
            this.settings.usePointAsAnchor ||
            safeExecute(this.settings.pointAsAnchorFunction, [this.historicalData, data, data.dsIndex])).map(data => data.time);
      }
      this.mapWidget.map.updateMarkers(currentPosition);
    }
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

  calcTooltip = (point?: FormattedData, setTooltip = true) => {
    if (!point) {
      point = this.activeTrip;
    }
    const data = { ...point, maxTime: this.maxTime, minTime: this.minTime }
    const tooltipPattern: string = this.settings.useTooltipFunction ?
      safeExecute(this.settings.tooolTipFunction, [data, this.historicalData, 0]) : this.settings.tooltipPattern;
    const tooltipText = parseWithTranslation.parseTemplate(tooltipPattern, data, true);
    if (setTooltip) {
      this.mainTooltip = this.sanitizer.sanitize(
        SecurityContext.HTML, tooltipText);
      this.cd.detectChanges();
    }
    return tooltipText;
  }

  calcLabel() {
    const data = { ...this.activeTrip, maxTime: this.maxTime, minTime: this.minTime }
    const labelText: string = this.settings.useLabelFunction ?
      safeExecute(this.settings.labelFunction, [data, this.historicalData, 0]) : this.settings.label;
    this.label = (parseWithTranslation.parseTemplate(labelText, data, true));
  }

  interpolateArray(originData, interpolatedIntervals) {
    const result = {};
    for (let i = 1, j = 0; i < originData.length && j < interpolatedIntervals.length;) {
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

