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

import {
  AfterViewInit,
  ChangeDetectorRef,
  Component,
  Input,
  OnDestroy,
  OnInit,
  SecurityContext,
  ViewChild
} from '@angular/core';
import { MapWidgetController, TbMapWidgetV2 } from '../lib/maps/map-widget2';
import { FormattedData, MapProviders, TripAnimationSettings } from '../lib/maps/map-models';
import { addCondition, addGroupInfo, addToSchema, initSchema } from '@app/core/schema-utils';
import { mapPolygonSchema, pathSchema, pointSchema, tripAnimationSchema } from '../lib/maps/schemes';
import { DomSanitizer } from '@angular/platform-browser';
import { WidgetContext } from '@app/modules/home/models/widget-component.models';
import {
  findAngle,
  getRatio,
  interpolateOnLineSegment,
  parseArray,
  parseFunction,
  parseWithTranslation,
  safeExecute
} from '../lib/maps/maps-utils';
import { JsonSettingsSchema, WidgetConfig } from '@shared/models/widget.models';
import moment from 'moment';
import { isUndefined } from '@core/utils';
import { ResizeObserver } from '@juggle/resize-observer';

interface dataMap {
  [key: string] : FormattedData
}

@Component({
  // tslint:disable-next-line:component-selector
  selector: 'trip-animation',
  templateUrl: './trip-animation.component.html',
  styleUrls: ['./trip-animation.component.scss']
})
export class TripAnimationComponent implements OnInit, AfterViewInit, OnDestroy {

  private mapResize$: ResizeObserver;

  constructor(private cd: ChangeDetectorRef, private sanitizer: DomSanitizer) { }

  @Input() ctx: WidgetContext;

  @ViewChild('map') mapContainer;

  mapWidget: MapWidgetController;
  historicalData: FormattedData[][];
  normalizationStep: number;
  interpolatedTimeData = [];
  widgetConfig: WidgetConfig;
  settings: TripAnimationSettings;
  mainTooltips = [];
  visibleTooltip = false;
  activeTrip: FormattedData;
  label: string;
  minTime: number;
  maxTime: number;
  anchors: number[] = [];
  useAnchors: boolean;
  currentTime: number;

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
    this.settings.pointAsAnchorFunction = parseFunction(this.settings.pointAsAnchorFunction, ['data', 'dsData', 'dsIndex']);
    this.settings.tooltipFunction = parseFunction(this.settings.tooltipFunction, ['data', 'dsData', 'dsIndex']);
    this.settings.labelFunction = parseFunction(this.settings.labelFunction, ['data', 'dsData', 'dsIndex']);
    this.normalizationStep = this.settings.normalizationStep;
    const subscription = this.ctx.defaultSubscription;
    subscription.callbacks.onDataUpdated = () => {
      this.historicalData = parseArray(this.ctx.data).filter(arr => arr.length);
      if (this.historicalData.length) {
        this.calculateIntervals();
        this.timeUpdated(this.minTime);
      }
      this.mapWidget.map.map?.invalidateSize();
      this.cd.detectChanges();
    }
  }

  ngAfterViewInit() {
    this.mapWidget = new MapWidgetController(MapProviders.openstreet, false, this.ctx, this.mapContainer.nativeElement);
    this.mapResize$ = new ResizeObserver(() => {
      this.mapWidget.resize();
    });
    this.mapResize$.observe(this.mapContainer.nativeElement);
  }

  ngOnDestroy() {
    if (this.mapResize$) {
      this.mapResize$.disconnect();
    }
  }

  timeUpdated(time: number) {
    this.currentTime = time;
    const currentPosition = this.interpolatedTimeData
      .map(dataSource => dataSource[time])
    for(let j = 0; j < this.interpolatedTimeData.length; j++) {
      if (isUndefined(currentPosition[j])) {
        const timePoints = Object.keys(this.interpolatedTimeData[j]).map(item => parseInt(item, 10));
        for (let i = 1; i < timePoints.length; i++) {
          if (timePoints[i - 1] < time && timePoints[i] > time) {
            const beforePosition = this.interpolatedTimeData[j][timePoints[i - 1]];
            const afterPosition = this.interpolatedTimeData[j][timePoints[i]];
            const ratio = getRatio(timePoints[i - 1], timePoints[i], time);
            currentPosition[j] = {
              ...beforePosition,
              time,
              ...interpolateOnLineSegment(beforePosition, afterPosition, this.settings.latKeyName, this.settings.lngKeyName, ratio)
            }
            break;
          }
        }
      }
    }
    for(let j = 0; j < this.interpolatedTimeData.length; j++) {
      if (isUndefined(currentPosition[j])) {
        currentPosition[j] = this.calculateLastPoints(this.interpolatedTimeData[j], time);
      }
    }
    this.calcLabel(currentPosition);
    this.calcTooltip(currentPosition, true);
    if (this.mapWidget && this.mapWidget.map && this.mapWidget.map.map) {
      const formattedInterpolatedTimeData = this.interpolatedTimeData.map(ds => _.values(ds));
      this.mapWidget.map.updatePolylines(formattedInterpolatedTimeData, true);
      if (this.settings.showPolygon) {
        this.mapWidget.map.updatePolygons(this.interpolatedTimeData);
      }
      if (this.settings.showPoints) {
        this.mapWidget.map.updatePoints(formattedInterpolatedTimeData.map(ds => _.union(ds)), this.calcTooltip);
      }
      this.mapWidget.map.updateMarkers(currentPosition, true, (trip) => {
        this.activeTrip = trip;
        this.timeUpdated(this.currentTime)
      });
    }
  }

  setActiveTrip() {
  }

  private calculateLastPoints(dataSource: dataMap, time: number): FormattedData {
    const timeArr = Object.keys(dataSource);
    let index = timeArr.findIndex((dtime, index) => {
      return Number(dtime) >= time;
    });

    if(index !== -1) {
      if(Number(timeArr[index]) !== time && index !== 0) {
        index--;
      }
    } else {
      index = timeArr.length - 1;
    }

    return dataSource[timeArr[index]];
  }

  calculateIntervals() {
    this.historicalData.forEach((dataSource, index) => {
      this.minTime = dataSource[0]?.time || Infinity;
      this.maxTime = dataSource[dataSource.length - 1]?.time || -Infinity;
      this.interpolatedTimeData[index] = this.interpolateArray(dataSource);
    });
    if(!this.activeTrip){
      this.activeTrip = this.interpolatedTimeData.map(dataSource => dataSource[this.minTime]).filter(ds => ds)[0];
    }
    if (this.useAnchors) {
      const anchorDate = Object.entries(_.union(this.interpolatedTimeData)[0]);
      this.anchors = anchorDate
        .filter((data: [string, FormattedData]) => safeExecute(this.settings.pointAsAnchorFunction, [data[1], anchorDate, data[1].dsIndex]))
        .map(data => parseInt(data[0], 10));
    }
  }

  calcTooltip = (points?: FormattedData[], isMainTooltip: boolean = false): string => {
    let tooltipText;
    if(isMainTooltip) {
      this.mainTooltips = []
    }
    for (let point of points) {
      const data = point ? point : this.activeTrip;
      const tooltipPattern: string = this.settings.useTooltipFunction ?
        safeExecute(this.settings.tooltipFunction, [data, this.historicalData, point.dsIndex]) : this.settings.tooltipPattern;
      tooltipText = parseWithTranslation.parseTemplate(tooltipPattern, data, true);
      if(isMainTooltip) {
        this.mainTooltips.push(this.sanitizer.sanitize(SecurityContext.HTML, tooltipText));
      }
    }
    this.cd.detectChanges();
    return tooltipText;
  }

  calcLabel(formattedDataArr: FormattedData[]) {
    let labelToSet = '';
    for (let formattedData of formattedDataArr) {
      const labelText: string = this.settings.useLabelFunction ?
        safeExecute(this.settings.labelFunction, [formattedData, this.historicalData, formattedData.dsIndex]) : this.settings.label;
      const label = (parseWithTranslation.parseTemplate(labelText, formattedData, true));
      labelToSet = labelToSet.length ? labelToSet + ',' + label : label;
    }
    this.label = labelToSet;
  }

  interpolateArray(originData: FormattedData[]) {
    const result = {};
    const latKeyName = this.settings.latKeyName;
    const lngKeyName = this.settings.lngKeyName;
    for (const data of originData) {
      const currentTime = data.time;
      const normalizeTime = this.minTime + Math.ceil((currentTime - this.minTime) / this.normalizationStep) * this.normalizationStep;
      result[normalizeTime] = {
        ...data,
        minTime: this.minTime !== Infinity ? moment(this.minTime).format('YYYY-MM-DD HH:mm:ss') : '',
        maxTime: this.maxTime !== -Infinity ? moment(this.maxTime).format('YYYY-MM-DD HH:mm:ss') : '',
        rotationAngle: this.settings.rotationAngle
      };
    }
    const timeStamp = Object.keys(result);
    for (let i = 0; i < timeStamp.length - 1; i++) {
      result[timeStamp[i]].rotationAngle += findAngle(result[timeStamp[i]], result[timeStamp[i + 1]], latKeyName, lngKeyName)
    }
    return result;
  }
}

export let TbTripAnimationWidget = TripAnimationComponent;
