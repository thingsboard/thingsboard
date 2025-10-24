///
/// Copyright © 2016-2025 The Thingsboard Authors
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

import {
  calculateInterpolationRatio,
  calculateLastPoints, DataLayerColorSettings, DataLayerColorType,
  defaultBaseTripsDataLayerSettings,
  findRotationAngle,
  interpolateLineSegment,
  MapDataLayerType, MarkerType,
  TbMapDatasource,
  TripsDataLayerSettings
} from '@shared/models/widget/maps/map.models';
import { forkJoin, Observable } from 'rxjs';
import { DataKey, FormattedData, WidgetActionType } from '@shared/models/widget.models';
import { map } from 'rxjs/operators';
import L from 'leaflet';
import { deepClone, isDefined, isUndefined } from '@core/utils';
import { TbMap } from '@home/components/widget/lib/maps/map';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import moment from 'moment/moment';
import { createTooltip, updateTooltip } from '@home/components/widget/lib/maps/data-layer/data-layer-utils';
import _ from 'lodash';
import {
  DataLayerColorProcessor,
  DataLayerPatternProcessor,
  TbDataLayerItem,
  TbMapDataLayer
} from '@home/components/widget/lib/maps/data-layer/map-data-layer';
import { MarkerDataProcessor } from '@home/components/widget/lib/maps/data-layer/markers-data-layer';

type TripRouteData = {[time: number]: FormattedData<TbMapDatasource>};

export interface PointItem {
  point: L.CircleMarker;
  tooltip?: L.Popup;
}

class TbTripDataItem extends TbDataLayerItem<TripsDataLayerSettings, TbTripsDataLayer, L.FeatureGroup> {

  private tripRouteData: TripRouteData;

  private marker: L.Marker;
  private markerTooltip: L.Popup;
  private labelOffset: L.PointTuple;

  private polyline: L.Polyline;
  private polylineDecorator: L.PolylineDecorator;
  private pointsContainer: L.FeatureGroup;
  private points = new Map<string, PointItem>();

  private currentTime: number;
  private currentPositionData: FormattedData<TbMapDatasource>;
  private pointData: FormattedData<TbMapDatasource>;

  constructor(private rawRouteData: FormattedData<TbMapDatasource>[],
              private latestData: FormattedData<TbMapDatasource>,
              settings: TripsDataLayerSettings,
              dataLayer: TbTripsDataLayer) {
    super(settings, dataLayer);
    this.tripRouteData = this.prepareTripRouteData();
    this.create();
  }

  public update(rawRouteData: FormattedData<TbMapDatasource>[]) {
    this.rawRouteData = rawRouteData;
    this.tripRouteData = this.prepareTripRouteData();
    this.updateCurrentPosition(true);
    this.pointData = this.currentPositionData;
    if (this.latestData) {
      this.pointData = {...this.pointData, ...this.latestData};
    }
    this.updatePath();
    this.updatePoints();
    this.updateMarker();
  }

  public updateLatestData(latestData: FormattedData<TbMapDatasource>) {
    this.latestData = latestData;
    this.pointData = this.currentPositionData;
    if (this.latestData) {
      this.pointData = {...this.pointData, ...this.latestData};
    }
    this.updateAppearance();
  }

  public updateAppearance() {
    this.updatePathAppearance();
    const dsData = this.dataLayer.getMap().getData();
    if (this.settings.showMarker && this.settings.tooltip?.show) {
      updateTooltip(this.dataLayer.getMap(), this.markerTooltip,
        this.settings.tooltip, this.dataLayer.dataLayerTooltipProcessor, this.pointData, dsData);
    }
    this.updatePoints();
    this.updateMarkerIcon(this.pointData, dsData);
  }

  public updateCurrentTime() {
    this.updateCurrentPosition();
    this.pointData = this.currentPositionData;
    if (this.latestData) {
      this.pointData = {...this.pointData, ...this.latestData};
    }
    this.updatePath();
    this.updateMarker();
  }

  public invalidateCoordinates(): void {
    this.tripRouteData = this.prepareTripRouteData();
    this.updatePath();
    this.updatePoints();
    this.updateMarker();
  }

  public remove() {
    if (this.marker) {
      this.layer.removeLayer(this.marker);
      this.marker.off();
    }
    this.points.forEach(pointItem => {
      pointItem.point.off();
    });
    this.dataLayer.getDataLayerContainer().removeLayer(this.layer);
    this.layer.off();
  }

  public calculateAnchors(): number[] {
    const entries = Object.entries(this.tripRouteData);
    const dsData = this.dataLayer.getMap().getData();
    return entries.filter(data => this.dataLayer.getMap().getLocationSnapFilterFunction().execute(data[1], dsData))
    .map(data => parseInt(data[0], 10));
  }

  private create() {
    this.updateCurrentPosition();
    this.layer = L.featureGroup();
    this.pointData = this.currentPositionData;
    if (this.latestData) {
      this.pointData = {...this.pointData, ...this.latestData};
    }
    this.createPath();
    this.updatePoints();
    this.createMarker();
    try {
      this.dataLayer.getDataLayerContainer().addLayer(this.layer);
    } catch (e) {
      console.warn(e);
    }
  }

  private createMarker() {
    if (this.settings.showMarker) {
      const dsData = this.dataLayer.getMap().getData();
      const location = this.dataLayer.dataProcessor.extractLocation(this.pointData, dsData);
      this.marker = L.marker(location, {
        tbMarkerData: this.pointData,
        snapIgnore: true
      });
      this.marker.addTo(this.layer);
      this.updateMarkerIcon(this.pointData, dsData);
      if (this.settings.tooltip?.show) {
        this.markerTooltip = createTooltip(this.dataLayer.getMap(),
          this.marker, this.settings.tooltip, this.pointData, () => true);
        updateTooltip(this.dataLayer.getMap(), this.markerTooltip,
          this.settings.tooltip, this.dataLayer.dataLayerTooltipProcessor, this.pointData, dsData);
      }
      const clickAction = this.settings.click;
      if (clickAction && clickAction.type !== WidgetActionType.doNothing) {
        this.marker.on('click', (event) => {
          this.dataLayer.getMap().dataItemClick(event.originalEvent, clickAction, this.pointData);
        });
      }
    }
  }

  private updateMarker() {
    if (this.settings.showMarker) {
      const dsData = this.dataLayer.getMap().getData();
      this.marker.options.tbMarkerData = this.pointData;
      this.updateMarkerLocation(this.pointData, dsData);
      if (this.settings.tooltip.show) {
        updateTooltip(this.dataLayer.getMap(), this.markerTooltip,
          this.settings.tooltip, this.dataLayer.dataLayerTooltipProcessor, this.pointData, dsData);
      }
      this.updateMarkerIcon(this.pointData, dsData);
    }
  }

  private createPath() {
    if (this.settings.showPath) {
      const formattedRouteData = _.values(this.tripRouteData);
      const dsData = this.dataLayer.getMap().getData();
      const locations = formattedRouteData.map(data => this.dataLayer.dataProcessor.extractLocation(data, dsData));
      const pathStyle = this.dataLayer.getPathStyle(this.pointData, dsData);
      this.polyline = L.polyline(locations, pathStyle);
      this.polyline.addTo(this.layer);
      if (this.settings.usePathDecorator) {
        this.polylineDecorator = new L.PolylineDecorator(this.polyline, this.dataLayer.getPathDecoratorStyle(pathStyle.color));
        this.polylineDecorator.addTo(this.layer);
      }
    }
  }

  private updatePath() {
    if (this.settings.showPath) {
      const formattedRouteData = _.values(this.tripRouteData);
      const dsData = this.dataLayer.getMap().getData();
      const locations = formattedRouteData.map(data => this.dataLayer.dataProcessor.extractLocation(data, dsData));
      this.polyline.setLatLngs(locations);
      if (this.settings.usePathDecorator) {
        this.polylineDecorator.setPaths(this.polyline);
      }
      this.updatePathAppearance();
    }
  }

  private updatePoints() {
    if (this.settings.showPoints) {
      if (!this.pointsContainer) {
        this.pointsContainer = L.featureGroup();
        this.pointsContainer.addTo(this.layer);
      }
      const formattedRouteData = _.values(this.tripRouteData);
      const dsData = this.dataLayer.getMap().getData();
      const pointsData = formattedRouteData.map(data => ({
        location: this.dataLayer.dataProcessor.extractLocation(data, dsData),
        data
      })).filter(pData => !!pData.location);
      const toDelete = new Set(Array.from(this.points.keys()));
      for (const pData of pointsData) {
        let pointData = pData.data;
        if (this.latestData) {
          pointData = {...pointData, ...this.latestData};
        }
        const pointLocation = pData.location;
        const pointColor = this.dataLayer.pointColorProcessor.processColor(pointData, dsData);
        const pointStyle = {
          stroke: false,
          fillOpacity: 1,
          fillColor: pointColor,
          radius: this.settings.pointSize
        };
        const pointKey = `${pointLocation.lat}_${pointLocation.lng}`;
        let pointItem = this.points.get(pointKey);
        if (pointItem) {
          pointItem.point.setStyle(pointStyle);
          if (this.settings.pointTooltip?.show) {
            updateTooltip(this.dataLayer.getMap(), pointItem.tooltip,
              this.settings.pointTooltip, this.dataLayer.pointTooltipProcessor, pointData, dsData);
          }
        } else {
          pointItem = {
            point: L.circleMarker(pointLocation, pointStyle)
          };
          pointItem.point.addTo(this.pointsContainer);
          if (this.settings.pointTooltip?.show) {
            pointItem.tooltip = createTooltip(this.dataLayer.getMap(),
              pointItem.point, this.settings.pointTooltip, pointData, () => true);
            updateTooltip(this.dataLayer.getMap(), pointItem.tooltip,
              this.settings.pointTooltip, this.dataLayer.pointTooltipProcessor, pointData, dsData);
          }
          this.points.set(pointKey, pointItem);
        }
        toDelete.delete(pointKey);
      }
      toDelete.forEach(pointKey => {
        const pointItem = this.points.get(pointKey);
        if (pointItem) {
          this.pointsContainer.removeLayer(pointItem.point);
          pointItem.point.off();
          this.points.delete(pointKey);
        }
      });
    }
  }

  private updatePathAppearance() {
    if (this.settings.showPath) {
      const dsData = this.dataLayer.getMap().getData();
      const pathStyle = this.dataLayer.getPathStyle(this.pointData, dsData);
      this.polyline.setStyle(pathStyle);
      if (this.settings.usePathDecorator) {
        this.polylineDecorator.setPatterns(this.dataLayer.getPathDecoratorStyle(pathStyle.color).patterns)
      }
    }
  }

  private updateMarkerLocation(data: FormattedData<TbMapDatasource>, dsData: FormattedData<TbMapDatasource>[]) {
    const location = this.dataLayer.dataProcessor.extractLocation(data, dsData);
    if (!this.marker.getLatLng().equals(location)) {
      this.marker.setLatLng(location);
    }
  }

  private updateMarkerIcon(data: FormattedData<TbMapDatasource>, dsData: FormattedData<TbMapDatasource>[]) {
    if (this.settings.showMarker) {
      this.dataLayer.dataProcessor.createMarkerIcon(data, dsData, data.rotationAngle).subscribe(
        (iconInfo) => {
          const options = deepClone(iconInfo.icon.options);
          this.marker.setIcon(iconInfo.icon);
          const anchor = options.iconAnchor;
          if (anchor && Array.isArray(anchor)) {
            this.labelOffset = [iconInfo.size[0] / 2 - anchor[0], 10 - anchor[1]];
          } else {
            this.labelOffset = [0, -iconInfo.size[1] * this.dataLayer.markerOffset[1] + 10];
          }
          this.updateMarkerLabel(data, dsData);
        }
      );
    }
  }

  private updateMarkerLabel(data: FormattedData<TbMapDatasource>, dsData: FormattedData<TbMapDatasource>[]) {
    if (this.settings.label.show) {
      this.marker.unbindTooltip();
      const label = this.dataLayer.dataLayerLabelProcessor.processPattern(data, dsData);
      const labelColor = this.dataLayer.getCtx().widgetConfig.color;
      const content: L.Content = `<div style="color: ${labelColor};"><b>${label}</b></div>`;
      this.marker.bindTooltip(content, { className: 'tb-marker-label', permanent: true, direction: 'top', offset: this.labelOffset });
    }
  }

  private prepareTripRouteData(): TripRouteData {
    const result: TripRouteData = {};
    const minTime = this.dataLayer.getMap().getMinTime();
    const maxTime = this.dataLayer.getMap().getMaxTime();
    const timeStep = this.dataLayer.getMap().getTimeStep();
    const timeline = this.dataLayer.getMap().hasTimeline();
    const dsData = this.dataLayer.getMap().getData();
    for (const data of this.rawRouteData) {
      const currentTime = data.time;
      const normalizeTime = timeline ? (minTime
        + Math.ceil((currentTime - minTime) / timeStep) * timeStep) : currentTime;
      result[normalizeTime] = {
        ...data,
        minTime: minTime !== Infinity ? moment(minTime).format('YYYY-MM-DD HH:mm:ss') : '',
        maxTime: maxTime !== -Infinity ? moment(maxTime).format('YYYY-MM-DD HH:mm:ss') : '',
        rotationAngle: this.settings.rotateMarker ? this.settings.offsetAngle : 0
      };
    }
    const timeStamp = Object.keys(result);
    if (timeline) {
      const xKey = this.settings.xKey.label;
      const yKey = this.settings.yKey.label;
      for (let i = 0; i < timeStamp.length - 1; i++) {
        if (isUndefined(result[timeStamp[i + 1]][xKey]) || isUndefined(result[timeStamp[i + 1]][yKey])) {
          for (let j = i + 2; j < timeStamp.length - 1; j++) {
            if (isDefined(result[timeStamp[j]][xKey]) || isDefined(result[timeStamp[j]][yKey])) {
              const ratio = calculateInterpolationRatio(Number(timeStamp[i]), Number(timeStamp[j]), Number(timeStamp[i + 1]));
              result[timeStamp[i + 1]] = {
                ...interpolateLineSegment(result[timeStamp[i]], result[timeStamp[j]], xKey, yKey, ratio),
                ...result[timeStamp[i + 1]],
              };
              break;
            }
          }
        }
        if (this.settings.rotateMarker) {
          const startPoint = this.dataLayer.dataProcessor.extractLocation(result[timeStamp[i]], dsData);
          const endPoint = this.dataLayer.dataProcessor.extractLocation(result[timeStamp[i + 1]], dsData);
          result[timeStamp[i]].rotationAngle += findRotationAngle(startPoint, endPoint);
        }
      }
      if (this.settings.rotateMarker && timeStamp.length > 1) {
        result[timeStamp[timeStamp.length - 1]].rotationAngle = result[timeStamp[timeStamp.length - 2]].rotationAngle;
      }
    } else if (this.settings.rotateMarker && timeStamp.length > 1) {
      const startPoint = this.dataLayer.dataProcessor.extractLocation(result[timeStamp[timeStamp.length - 2]], dsData);
      const endPoint = this.dataLayer.dataProcessor.extractLocation(result[timeStamp[timeStamp.length - 1]], dsData);
      result[timeStamp[timeStamp.length - 1]].rotationAngle += findRotationAngle(startPoint, endPoint);
    }
    return result;
  }

  private updateCurrentPosition(force = false) {
    if (this.currentTime !== this.dataLayer.getMap().getCurrentTime() || force) {
      this.currentTime = this.dataLayer.getMap().getCurrentTime();
      let currentPosition = this.tripRouteData[this.currentTime];
      if (!currentPosition) {
        const timePoints = Object.keys(this.tripRouteData).map(item => parseInt(item, 10));
        for (let i = 1; i < timePoints.length; i++) {
          if (timePoints[i - 1] < this.currentTime && timePoints[i] > this.currentTime) {
            const beforePosition = this.tripRouteData[timePoints[i - 1]];
            const afterPosition = this.tripRouteData[timePoints[i]];
            const ratio = calculateInterpolationRatio(timePoints[i - 1], timePoints[i], this.currentTime);
            currentPosition = {
              ...beforePosition,
              time: this.currentTime,
              ...interpolateLineSegment(beforePosition, afterPosition, this.settings.xKey.label, this.settings.yKey.label, ratio)
            };
            break;
          }
        }
      }
      if (!currentPosition) {
        currentPosition = calculateLastPoints(this.tripRouteData, this.currentTime);
      }
      this.currentPositionData = currentPosition;
    }
  }

}

export class TbTripsDataLayer extends TbMapDataLayer<TripsDataLayerSettings, TbTripDataItem> {

  public dataProcessor: MarkerDataProcessor;

  public markerOffset: L.LatLngTuple;
  public tooltipOffset: L.LatLngTuple;

  public pathStrokeColorProcessor: DataLayerColorProcessor;
  public pointColorProcessor: DataLayerColorProcessor;
  public pointTooltipProcessor: DataLayerPatternProcessor;

  private rawTripsData: FormattedData<TbMapDatasource>[][];
  private latestTripsData: FormattedData<TbMapDatasource>[];

  constructor(protected map: TbMap<any>,
              inputSettings: TripsDataLayerSettings) {
    super(map, inputSettings);
  }

  public dataLayerType(): MapDataLayerType {
    return 'trips';
  }

  public showMarker(): boolean {
    return this.settings.showMarker;
  }

  public prepareTripsData(tripsData: FormattedData<TbMapDatasource>[][], tripsLatestData: FormattedData<TbMapDatasource>[]): {minTime: number; maxTime: number} {
    let minTime = Infinity;
    let maxTime = -Infinity;
    this.rawTripsData =
      tripsData.filter(d => !!d.length && d[0].$datasource.mapDataIds.includes(this.mapDataId)).map(
        item => this.clearIncorrectFirsLastDatapoint(item)).filter(arr => arr.length);
    this.latestTripsData = tripsLatestData.filter(d => d.$datasource.mapDataIds.includes(this.mapDataId));
    this.rawTripsData.forEach((dataSource) => {
      minTime = Math.min(dataSource[0].time, minTime);
      maxTime = Math.max(dataSource[dataSource.length - 1].time, maxTime);
    });
    return {minTime, maxTime};
  }

  public updateTrips() {
    const toDelete = new Set(Array.from(this.layerItems.keys()));
    this.rawTripsData.forEach(rawTripData => {
      const entityId = rawTripData[0].entityId;
      let tripItem = this.layerItems.get(entityId);
      if (tripItem) {
        tripItem.update(rawTripData);
      } else {
        const latestData = this.latestTripsData.find(d => d.entityId === entityId);
        tripItem = new TbTripDataItem(rawTripData, latestData, this.settings, this);
        this.layerItems.set(entityId, tripItem);
      }
      toDelete.delete(entityId);
    });
    toDelete.forEach((key) => {
      this.removeItem(key);
    });
  }

  public updateTripsLatestData(tripsLatestData: FormattedData<TbMapDatasource>[]) {
    this.latestTripsData = tripsLatestData.filter(d => d.$datasource.mapDataIds.includes(this.mapDataId));
    this.layerItems.forEach((item, entityId) => {
      const latestData = this.latestTripsData.find(d => d.entityId === entityId);
      item.updateLatestData(latestData);
    });
  }

  public updateCurrentTime() {
    this.layerItems.forEach(item => {
      item.updateCurrentTime();
    });
  }

  public updateAppearance() {
    this.layerItems.forEach(item => {
      item.updateAppearance();
    });
  }

  public calculateAnchors(): number[] {
    let anchors: number[] = [];
    this.layerItems.forEach(item => {
      const tripAnchors = item.calculateAnchors();
      anchors = [...new Set([...anchors, ...tripAnchors])];
    });
    return anchors;
  }

  public getPathStyle(data: FormattedData<TbMapDatasource>, dsData: FormattedData<TbMapDatasource>[]): L.PolylineOptions {
    const pathStroke = this.pathStrokeColorProcessor.processColor(data, dsData);
    return {
      interactive: false,
      color: pathStroke,
      opacity: 1,
      weight: this.settings.pathStrokeWeight,
      pmIgnore: true
    };
  }

  public getPathDecoratorStyle(pathStroke: string): L.PolylineDecoratorOptions {
    const symbolConstructor = L.Symbol[this.settings.pathDecoratorSymbol];
    return {
      patterns: [
        {
          offset: this.settings.pathDecoratorOffset,
          endOffset: this.settings.pathEndDecoratorOffset,
          repeat: this.settings.pathDecoratorRepeat,
          symbol: symbolConstructor({
            pixelSize: this.settings.pathDecoratorSymbolSize,
            polygon: false,
            pathOptions: {
              color: this.settings.pathDecoratorSymbolColor ? this.settings.pathDecoratorSymbolColor : pathStroke,
              stroke: true
            }
          })
        }
      ]
    };
  }

  protected calculateDataKeys(): DataKey[] {
    const dataKeys = [this.settings.xKey, this.settings.yKey];
    const additionalKeys = this.allColorSettings().filter(settings => settings.type === DataLayerColorType.range && settings.rangeKey)
    .map(settings => settings.rangeKey);
    if (this.settings.additionalDataKeys?.length) {
      additionalKeys.push(...this.settings.additionalDataKeys);
    }
    if (additionalKeys.length) {
      const tsKeys = additionalKeys.filter(key => key.type === DataKeyType.timeseries);
      dataKeys.push(...tsKeys);
    }
    return dataKeys;
  }

  protected calculateLatestDataKeys(): DataKey[] {
    const additionalKeys = this.allColorSettings().filter(settings => settings.type === DataLayerColorType.range && settings.rangeKey)
    .map(settings => settings.rangeKey);
    if (this.settings.additionalDataKeys?.length) {
      additionalKeys.push(...this.settings.additionalDataKeys);
    }
    if (additionalKeys.length) {
      const latestKeys = additionalKeys.filter(key => key.type !== DataKeyType.timeseries);
      if (latestKeys.length) {
        return latestKeys;
      }
    }
    return [];
  }

  protected allColorSettings(): DataLayerColorSettings[] {
    const colorSettings: DataLayerColorSettings[] = [];
    if (this.settings.showMarker) {
      if (this.settings.markerType === MarkerType.shape) {
        colorSettings.push(this.settings.markerShape.color);
      } else if (this.settings.markerType === MarkerType.icon) {
        colorSettings.push(this.settings.markerIcon.color);
      }
    }
    if (this.settings.showPath) {
      colorSettings.push(this.settings.pathStrokeColor);
    }
    if (this.settings.showPoints) {
      colorSettings.push(this.settings.pointColor);
    }
    return colorSettings;
  }

  protected defaultBaseSettings(map: TbMap<any>): Partial<TripsDataLayerSettings> {
    return defaultBaseTripsDataLayerSettings(map.type());
  }

  protected doSetup(): Observable<void> {
    this.markerOffset = [
      isDefined(this.settings.markerOffsetX) ? this.settings.markerOffsetX : 0.5,
      isDefined(this.settings.markerOffsetY) ? this.settings.markerOffsetY : 0.5,
    ];
    this.tooltipOffset = [
      isDefined(this.settings.tooltip?.offsetX) ? this.settings.tooltip?.offsetX : 0,
      isDefined(this.settings.tooltip?.offsetY) ? this.settings.tooltip?.offsetY : -0.5,
    ];
    this.dataProcessor = new MarkerDataProcessor(this, this.settings, this.markerOffset, this.tooltipOffset);
    const setup$: Observable<void>[] = [this.dataProcessor.setup()];
    if (this.settings.showPath) {
      this.pathStrokeColorProcessor = new DataLayerColorProcessor(this, this.settings.pathStrokeColor);
      setup$.push(this.pathStrokeColorProcessor.setup());
    }
    if (this.settings.showPoints) {
      this.pointColorProcessor = new DataLayerColorProcessor(this, this.settings.pointColor);
      setup$.push(this.pointColorProcessor.setup());
      if (this.settings.pointTooltip?.show) {
        this.pointTooltipProcessor = new DataLayerPatternProcessor(this, this.settings.pointTooltip);
        setup$.push(this.pointTooltipProcessor.setup());
      }
    }
    return forkJoin(setup$).pipe(map(() => null));
  }

  private clearIncorrectFirsLastDatapoint(dataSource: FormattedData<TbMapDatasource>[]): FormattedData<TbMapDatasource>[] {
    const firstHistoricalDataIndexCoordinate = dataSource.findIndex(this.findFirstHistoricalDataIndexCoordinate);
    if (firstHistoricalDataIndexCoordinate === -1) {
      return [];
    }
    let lastIndex = dataSource.length - 1;
    for (lastIndex; lastIndex > 0; lastIndex--) {
      if (this.findFirstHistoricalDataIndexCoordinate(dataSource[lastIndex])) {
        lastIndex++;
        break;
      }
    }
    if (firstHistoricalDataIndexCoordinate > 0 || lastIndex < dataSource.length) {
      return dataSource.slice(firstHistoricalDataIndexCoordinate, lastIndex);
    }
    return dataSource;
  }

  private findFirstHistoricalDataIndexCoordinate = (item: FormattedData<TbMapDatasource>): boolean => {
    return isDefined(item[this.settings.xKey.label]) && isDefined(item[this.settings.yKey.label]);
  }

}
