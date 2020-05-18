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

import _ from 'lodash';
import tinycolor from 'tinycolor2';

import { AfterViewInit, ChangeDetectorRef, Component, Input, OnInit, SecurityContext, ViewChild } from '@angular/core';
import { MapWidgetController, TbMapWidgetV2 } from '../lib/maps/map-widget2';
import { FormattedData, MapProviders } from '../lib/maps/map-models';
import { addCondition, addGroupInfo, addToSchema, initSchema } from '@app/core/schema-utils';
import { mapPolygonSchema, pathSchema, pointSchema, tripAnimationSchema } from '../lib/maps/schemes';
import { DomSanitizer } from '@angular/platform-browser';
import { WidgetContext } from '@app/modules/home/models/widget-component.models';
import {
  findAngle,
  getRatio,
  interpolateOnLineSegment,
  parseArray,
  parseWithTranslation,
  safeExecute
} from '../lib/maps/maps-utils';
import { JsonSettingsSchema, WidgetConfig } from '@shared/models/widget.models';
import moment from 'moment';
import { isUndefined } from '@core/utils';


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
  historicalData: FormattedData[][];
  normalizationStep: number;
  interpolatedTimeData = [];
  widgetConfig: WidgetConfig;
  settings;
  mainTooltip = '';
  visibleTooltip = false;
  activeTrip: FormattedData;
  label;
  minTime: number;
  minTimeFormat: string;
  maxTime: number;
  maxTimeFormat: string;
  anchors = [];
  useAnchors: boolean;

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
    this.useAnchors = this.settings.showPoints && this.settings.usePointAsAnchor;
    this.settings.fitMapBounds = true;
    this.normalizationStep = this.settings.normalizationStep;
    const subscription = this.ctx.subscriptions[Object.keys(this.ctx.subscriptions)[0]];
    if (subscription) {
      subscription.callbacks.onDataUpdated = () => {
        this.historicalData = parseArray(this.ctx.data);
        this.activeTrip = this.historicalData[0][0];
        this.calculateIntervals();
        this.timeUpdated(this.minTime);
        this.mapWidget.map.map?.invalidateSize();
        this.cd.detectChanges();
      }
    }
  }

  ngAfterViewInit() {
    const ctxCopy: WidgetContext = _.cloneDeep(this.ctx);
    this.mapWidget = new MapWidgetController(MapProviders.openstreet, false, ctxCopy, this.mapContainer.nativeElement);
  }

  timeUpdated(time: number) {
    const currentPosition = this.interpolatedTimeData.map(dataSource => dataSource[time]);
    if(isUndefined(currentPosition[0])){
      const timePoints = Object.keys(this.interpolatedTimeData[0]).map(item => parseInt(item, 10));
      for (let i = 1; i < timePoints.length; i++) {
        if (timePoints[i - 1] < time && timePoints[i] > time) {
          const beforePosition = this.interpolatedTimeData[0][timePoints[i - 1]];
          const afterPosition = this.interpolatedTimeData[0][timePoints[i]];
          const ratio = getRatio(timePoints[i - 1], timePoints[i], time);
          currentPosition[0] = {
            ...beforePosition,
            time,
            ...interpolateOnLineSegment(beforePosition, afterPosition, this.settings.latKeyName, this.settings.lngKeyName, ratio)
          }
          break;
        }
      }
    }
    this.activeTrip = currentPosition[0];
    this.calcLabel();
    this.calcTooltip();
    if (this.mapWidget) {
      this.mapWidget.map.updatePolylines(this.interpolatedTimeData.map(ds => _.values(ds)));
      if (this.settings.showPolygon) {
        this.mapWidget.map.updatePolygons(this.interpolatedTimeData);
      }
      if (this.settings.showPoints) {
        this.mapWidget.map.updatePoints(this.interpolatedTimeData, this.calcTooltip);
        // this.anchors = this.interpolatedTimeData
        //   .filter(data =>
        //     this.settings.usePointAsAnchor ||
        //     safeExecute(this.settings.pointAsAnchorFunction, [this.interpolatedTimeData, data, data.dsIndex])).map(data => data.time);
      }
      this.mapWidget.map.updateMarkers(currentPosition);
    }
  }

  setActiveTrip() {
  }

  calculateIntervals() {
    this.historicalData.forEach((dataSource, index) => {
      this.minTime = dataSource[0]?.time || Infinity;
      this.minTimeFormat = this.minTime !== Infinity ? moment(this.minTime).format('YYYY-MM-DD HH:mm:ss') : '';
      this.maxTime = dataSource[dataSource.length - 1]?.time || -Infinity;
      this.maxTimeFormat = this.maxTime !== -Infinity ? moment(this.maxTime).format('YYYY-MM-DD HH:mm:ss') : '';
      this.interpolatedTimeData[index] = this.interpolateArray(dataSource);
    });

  }

  calcTooltip = (point?: FormattedData, setTooltip = true) => {
    if (!point) {
      point = this.activeTrip;
    }
    const data = {
      ...this.activeTrip,
      maxTime: this.maxTimeFormat,
      minTime: this.minTimeFormat
    }
    const tooltipPattern: string = this.settings.useTooltipFunction ?
      safeExecute(this.settings.tooolTipFunction, [data, this.historicalData, point.dsIndex]) : this.settings.tooltipPattern;
    const tooltipText = parseWithTranslation.parseTemplate(tooltipPattern, data, true);
    if (setTooltip) {
      this.mainTooltip = this.sanitizer.sanitize(
        SecurityContext.HTML, tooltipText);
      this.cd.detectChanges();
    }
    return tooltipText;
  }

  calcLabel() {
    const data = {
      ...this.activeTrip,
      maxTime: this.maxTimeFormat,
      minTime: this.minTimeFormat
    }
    const labelText: string = this.settings.useLabelFunction ?
      safeExecute(this.settings.labelFunction, [data, this.historicalData, data.dsIndex]) : this.settings.label;
    this.label = (parseWithTranslation.parseTemplate(labelText, data, true));
  }

  interpolateArray(originData: FormattedData[]) {
    const result = {};
    const latKeyName = this.settings.latKeyName;
    const lngKeyName = this.settings.lngKeyName;
    for (let i = 0; i < originData.length; i++) {
      const currentTime = originData[i].time;
      const normalizeTime = this.minTime + Math.ceil((currentTime - this.minTime) / this.normalizationStep) * this.normalizationStep;
      if (i !== originData.length - 1) {
        result[normalizeTime] = {
          ...originData[i],
          rotationAngle: this.settings.rotationAngle + findAngle(originData[i], originData[i + 1], latKeyName, lngKeyName)
        };
      } else {
        result[normalizeTime] = {
          ...originData[i],
          rotationAngle: findAngle(originData[i - 1], originData[i], latKeyName, lngKeyName) + this.settings.rotationAngle
        };
      }
    }
    return result;
  }
}

export let TbTripAnimationWidget = TripAnimationComponent;
