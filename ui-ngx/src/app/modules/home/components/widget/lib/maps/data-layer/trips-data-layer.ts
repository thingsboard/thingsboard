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
  DataLayerColorProcessor,
  DataLayerPatternProcessor,
  MapDataLayerType, TbDataLayerItem,
  TbMapDataLayer,
  UnplacedMapDataItem
} from '@home/components/widget/lib/maps/data-layer/map-data-layer';
import {
  BaseMarkerShapeSettings,
  calculateInterpolationRatio, calculateLastPoints,
  ClusterMarkerColorFunction,
  DataLayerColorType,
  defaultBaseMarkersDataLayerSettings,
  defaultBaseTripsDataLayerSettings,
  findRotationAngle,
  interpolateLineSegment, isValidLatLng,
  loadImageWithAspect,
  MapStringFunction,
  MapType,
  MarkerIconInfo,
  MarkerIconSettings,
  MarkerImageFunction,
  MarkerImageInfo,
  MarkerImageSettings,
  MarkerImageType,
  MarkerPositionFunction,
  MarkersDataLayerSettings,
  MarkerShapeSettings,
  MarkerType,
  TbMapDatasource,
  TripsDataLayerSettings
} from '@home/components/widget/lib/maps/models/map.models';
import { forkJoin, Observable, of } from 'rxjs';
import { FormattedData } from '@shared/models/widget.models';
import {
  createColorMarkerIconElement,
  createColorMarkerShapeURI,
  MarkerShape
} from '@home/components/widget/lib/maps/models/marker-shape.models';
import tinycolor from 'tinycolor2';
import { MatIconRegistry } from '@angular/material/icon';
import { DomSanitizer } from '@angular/platform-browser';
import { catchError, map, switchMap } from 'rxjs/operators';
import L, { PolylineDecorator } from 'leaflet';
import { CompiledTbFunction } from '@shared/models/js-function.models';
import {
  deepClone,
  isDefined,
  isDefinedAndNotNull,
  isEmptyStr,
  isUndefined,
  parseTbFunction,
  safeExecuteTbFunction
} from '@core/utils';
import { ImagePipe } from '@shared/pipe/image.pipe';
import { TbMap } from '@home/components/widget/lib/maps/map';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import moment from 'moment/moment';
import { TbImageMap } from '@home/components/widget/lib/maps/image-map';
import { createTooltip, updateTooltip } from '@home/components/widget/lib/maps/data-layer/data-layer-utils';

type TripRouteData = {[time: number]: FormattedData<TbMapDatasource>};

class TbTripDataItem {

  private tripRouteData: TripRouteData;

  private layer: L.FeatureGroup;

  private marker: L.Marker;
  private markerTooltip: L.Popup;
  private labelOffset: L.PointTuple;

  private polyline: L.Polyline;
  private polylineDecorator: PolylineDecorator;
  private points: L.FeatureGroup;

  private currentTime: number;
  private currentPositionData: FormattedData<TbMapDatasource>;

  constructor(private rawRouteData: FormattedData<TbMapDatasource>[],
              private latestData: FormattedData<TbMapDatasource>,
              private settings: TripsDataLayerSettings,
              private dataLayer: TbTripsDataLayer) {
    this.tripRouteData = this.prepareTripRouteData();
    this.create();
  }

  private create() {
    this.updateCurrentPosition();
    this.layer = L.featureGroup();
    let pointData = this.currentPositionData;
    if (this.latestData) {
      pointData = {...pointData, ...this.latestData};
    }
    this.createMarker(pointData);
    try {
      this.dataLayer.getDataLayerContainer().addLayer(this.layer);
    } catch (e) {
      console.warn(e);
    }
  }

  public update(rawRouteData: FormattedData<TbMapDatasource>[]) {
    this.rawRouteData = rawRouteData;
    this.tripRouteData = this.prepareTripRouteData();
    this.updateCurrentPosition(true);
    let pointData = this.currentPositionData;
    if (this.latestData) {
      pointData = {...pointData, ...this.latestData};
    }
    this.updateMarker(pointData);
  }

  public updateLatestData(latestData: FormattedData<TbMapDatasource>) {
    this.latestData = latestData;
    this.updateAppearance();
  }

  public updateAppearance() {
    let data = this.currentPositionData;
    if (this.latestData) {
      data = {...data, ...this.latestData};
    }
    const dsData = this.dataLayer.getMap().getData();
    if (this.settings.tooltip.show) {
      updateTooltip(this.dataLayer.getMap(), this.markerTooltip,
        this.settings.tooltip, this.dataLayer.dataLayerTooltipProcessor, data, dsData);
    }
    this.updateMarkerIcon(data, dsData);
  }

  public updateCurrentTime() {
    this.updateCurrentPosition();
    let pointData = this.currentPositionData;
    if (this.latestData) {
      pointData = {...pointData, ...this.latestData};
    }
    this.updateMarker(pointData);
  }

  public remove() {
    this.dataLayer.getDataLayerContainer().removeLayer(this.layer);
    this.layer.off();
  }

  public getLayer(): L.Layer {
    return this.layer;
  }

  private createMarker(data: FormattedData<TbMapDatasource>) {
    const dsData = this.dataLayer.getMap().getData();
    const location = this.dataLayer.extractLocation(data, dsData);
    this.marker = L.marker(location, {
      tbMarkerData: data,
      snapIgnore: true
    });
    this.marker.addTo(this.layer);
    this.updateMarkerIcon(data, dsData);
    if (this.settings.tooltip?.show) {
      this.markerTooltip = createTooltip(this.dataLayer.getMap(),
        this.marker, this.settings.tooltip, data, () => true);
      updateTooltip(this.dataLayer.getMap(), this.markerTooltip,
        this.settings.tooltip, this.dataLayer.dataLayerTooltipProcessor, data, dsData);
    }
  }

  private updateMarker(data: FormattedData<TbMapDatasource>) {
    const dsData = this.dataLayer.getMap().getData();
    this.marker.options.tbMarkerData = data;
    this.updateMarkerLocation(data, dsData);
    if (this.settings.tooltip.show) {
      updateTooltip(this.dataLayer.getMap(), this.markerTooltip,
        this.settings.tooltip, this.dataLayer.dataLayerTooltipProcessor, data, dsData);
    }
    this.updateMarkerIcon(data, dsData);
  }

  private updateMarkerLocation(data: FormattedData<TbMapDatasource>, dsData: FormattedData<TbMapDatasource>[]) {
    const location = this.dataLayer.extractLocation(data, dsData);
    if (!this.marker.getLatLng().equals(location)) {
      this.marker.setLatLng(location);
    }
  }

  private updateMarkerIcon(data: FormattedData<TbMapDatasource>, dsData: FormattedData<TbMapDatasource>[]) {
    this.dataLayer.tripMarkerIconProcessor.createMarkerIcon(data, dsData).subscribe(
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
    if (timeline) {
      const xKey = this.settings.xKey.label;
      const yKey = this.settings.yKey.label;
      const timeStamp = Object.keys(result);
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
          result[timeStamp[i]].rotationAngle += findRotationAngle(result[timeStamp[i]], result[timeStamp[i + 1]], xKey, yKey);
        }
      }
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

abstract class TripMarkerIconProcessor<S> {

  static fromSettings(dataLayer: TbTripsDataLayer,
                      settings: TripsDataLayerSettings): TripMarkerIconProcessor<any> {
    switch (settings.markerType) {
      case MarkerType.shape:
        return new ShapeTripMarkerIconProcessor(dataLayer, settings.markerShape);
      case MarkerType.icon:
        return new IconTripMarkerIconProcessor(dataLayer, settings.markerIcon);
      case MarkerType.image:
        return new ImageTripMarkerIconProcessor(dataLayer, settings.markerImage);
    }
  }

  protected constructor(protected dataLayer: TbTripsDataLayer,
                        protected settings: S) {}

  public abstract setup(): Observable<void>;

  public abstract createMarkerIcon(data: FormattedData<TbMapDatasource>,
                                   dsData: FormattedData<TbMapDatasource>[]): Observable<MarkerIconInfo>;

}

abstract class BaseColorTripMarkerShapeProcessor<S extends BaseMarkerShapeSettings> extends TripMarkerIconProcessor<S> {

  private markerColorFunction: CompiledTbFunction<MapStringFunction>;

  protected constructor(protected dataLayer: TbTripsDataLayer,
                        protected settings: S) {
    super(dataLayer, settings);
  }

  public setup(): Observable<void> {
    const colorSettings = this.settings.color;
    if (colorSettings.type === DataLayerColorType.function) {
      return parseTbFunction<MapStringFunction>(this.dataLayer.getCtx().http, colorSettings.colorFunction, ['data', 'dsData']).pipe(
        map((parsed) => {
          this.markerColorFunction = parsed;
          return null;
        })
      );
    } else {
      return of(null);
    }
  }

  public createMarkerIcon(data: FormattedData<TbMapDatasource>, dsData: FormattedData<TbMapDatasource>[]): Observable<MarkerIconInfo> {
    const colorSettings = this.settings.color;
    let color: tinycolor.Instance;
    if (colorSettings.type === DataLayerColorType.function) {
      const functionColor = safeExecuteTbFunction(this.markerColorFunction, [data, dsData]);
      if (isDefinedAndNotNull(functionColor)) {
        color = tinycolor(functionColor);
      } else {
        color = tinycolor(colorSettings.color);
      }
    } else {
      color = tinycolor(colorSettings.color);
    }
    return this.createMarkerShape(color, data.rotationAngle, this.settings.size);
  }

  protected abstract createMarkerShape(color: tinycolor.Instance, rotationAngle: number, size: number): Observable<MarkerIconInfo>;
}

class ShapeTripMarkerIconProcessor extends BaseColorTripMarkerShapeProcessor<MarkerShapeSettings> {

  constructor(protected dataLayer: TbTripsDataLayer,
              protected settings: MarkerShapeSettings) {
    super(dataLayer, settings);
  }

  protected createMarkerShape(color: tinycolor.Instance, rotationAngle: number, size: number): Observable<MarkerIconInfo> {
    return this.dataLayer.createColoredMarkerShape(this.settings.shape, color, rotationAngle, size);
  }

}

class IconTripMarkerIconProcessor extends BaseColorTripMarkerShapeProcessor<MarkerIconSettings> {

  constructor(protected dataLayer: TbTripsDataLayer,
              protected settings: MarkerIconSettings) {
    super(dataLayer, settings);
  }

  protected createMarkerShape(color: tinycolor.Instance, rotationAngle: number, size: number): Observable<MarkerIconInfo> {
    return this.dataLayer.createColoredMarkerIcon(this.settings.icon, color, rotationAngle, size);
  }

}

class ImageTripMarkerIconProcessor extends TripMarkerIconProcessor<MarkerImageSettings> {

  private markerImageFunction: CompiledTbFunction<MarkerImageFunction>;

  constructor(protected dataLayer: TbTripsDataLayer,
              protected settings: MarkerImageSettings) {
    super(dataLayer, settings);
  }

  public setup(): Observable<void> {
    if (this.settings.type === MarkerImageType.function) {
      return parseTbFunction<MarkerImageFunction>(this.dataLayer.getCtx().http, this.settings.imageFunction, ['data', 'images', 'dsData']).pipe(
        map((parsed) => {
          this.markerImageFunction = parsed;
          return null;
        })
      );
    } else {
      return of(null);
    }
  }

  public createMarkerIcon(data: FormattedData<TbMapDatasource>, dsData: FormattedData<TbMapDatasource>[]): Observable<MarkerIconInfo> {
    if (this.settings.type === MarkerImageType.function) {
      const currentImage: MarkerImageInfo = safeExecuteTbFunction(this.markerImageFunction, [data, this.settings.images, dsData]);
      return this.loadMarkerIconInfo(currentImage, data.rotationAngle);
    } else {
      const currentImage: MarkerImageInfo = {
        url: this.settings.image,
        size: this.settings.imageSize || 34
      };
      return this.loadMarkerIconInfo(currentImage, data.rotationAngle);
    }
  }

  private loadMarkerIconInfo(image: MarkerImageInfo, rotationAngle = 0): Observable<MarkerIconInfo> {
    if (image && image.url) {
      return loadImageWithAspect(this.dataLayer.getCtx().$injector.get(ImagePipe), image.url).pipe(
        switchMap((aspectImage) => {
          if (aspectImage?.aspect) {
            let width: number;
            let height: number;
            if (aspectImage.aspect > 1) {
              width = image.size;
              height = image.size / aspectImage.aspect;
            } else {
              width = image.size * aspectImage.aspect;
              height = image.size;
            }
            let iconAnchor = image.markerOffset;
            let popupAnchor = image.tooltipOffset;
            if (!iconAnchor) {
              iconAnchor = [width * this.dataLayer.markerOffset[0], height * this.dataLayer.markerOffset[1]];
            }
            if (!popupAnchor) {
              popupAnchor = [width * this.dataLayer.tooltipOffset[0], height * this.dataLayer.tooltipOffset[1]];
            }
            const style = `background-image: url(${aspectImage.url}); background-size: contain; transform: rotate(${rotationAngle}deg); height: ${height}px; width: ${width}px;`;
            const icon = L.divIcon({
              html: `<div style="${style}"></div>`,
              className: 'tb-marker-div-icon',
              iconSize: [width, height],
              iconAnchor,
              popupAnchor
            });
            const iconInfo: MarkerIconInfo = {
              size: [width, height],
              icon
            };
            return of(iconInfo);
          } else {
            return this.dataLayer.createDefaultMarkerIcon(rotationAngle);
          }
        }),
        catchError(() => this.dataLayer.createDefaultMarkerIcon(rotationAngle))
      );
    } else {
      return this.dataLayer.createDefaultMarkerIcon(rotationAngle);
    }
  }
}

export class TbTripsDataLayer extends TbMapDataLayer<TripsDataLayerSettings, TbTripsDataLayer> {

  public tripMarkerIconProcessor: TripMarkerIconProcessor<any>;

  public markerOffset: L.LatLngTuple;
  public tooltipOffset: L.LatLngTuple;

  public pathStrokeColorProcessor: DataLayerColorProcessor;
  public pointColorProcessor: DataLayerColorProcessor;
  public pointTooltipProcessor: DataLayerPatternProcessor;

  private tripItems = new Map<string,TbTripDataItem>();

  private positionFunction: CompiledTbFunction<MarkerPositionFunction>;

  private rawTripsData: FormattedData<TbMapDatasource>[][];
  private latestTripsData: FormattedData<TbMapDatasource>[];

  constructor(protected map: TbMap<any>,
              inputSettings: TripsDataLayerSettings) {
    super(map, inputSettings);
  }

  public dataLayerType(): MapDataLayerType {
    return MapDataLayerType.trip;
  }

  public placeItem(item: UnplacedMapDataItem, layer: L.Layer): void {
    throw new Error('Not implemented!');
  }

  protected setupDatasource(datasource: TbMapDatasource): TbMapDatasource {
    datasource.dataKeys = [this.settings.xKey, this.settings.yKey];
    if (this.settings.additionalDataKeys?.length) {
      const tsKeys = this.settings.additionalDataKeys.filter(key => key.type === DataKeyType.timeseries);
      const latestKeys = this.settings.additionalDataKeys.filter(key => key.type !== DataKeyType.timeseries);
      datasource.dataKeys.push(...tsKeys);
      if (latestKeys.length) {
        datasource.latestDataKeys = latestKeys;
      }
    }
    return datasource;
  }

  protected defaultBaseSettings(map: TbMap<any>): Partial<TripsDataLayerSettings> {
    return defaultBaseTripsDataLayerSettings(map.type());
  }

  protected doSetup(): Observable<void> {
    this.markerOffset = [
      isDefined(this.settings.markerOffsetX) ? this.settings.markerOffsetX : 0.5,
      isDefined(this.settings.markerOffsetY) ? this.settings.markerOffsetY : 1,
    ];
    this.tooltipOffset = [
      isDefined(this.settings.tooltip?.offsetX) ? this.settings.tooltip?.offsetX : 0,
      isDefined(this.settings.tooltip?.offsetY) ? this.settings.tooltip?.offsetY : -1,
    ];
    this.tripMarkerIconProcessor = TripMarkerIconProcessor.fromSettings(this, this.settings);
    const setup$: Observable<void>[] = [];
    if (this.map.type() === MapType.image) {
      setup$.push(
        parseTbFunction<MarkerPositionFunction>(this.getCtx().http, this.settings.positionFunction, ['origXPos', 'origYPos', 'data', 'dsData', 'aspect']).pipe(
          map((parsed) => {
            this.positionFunction = parsed;
            return null;
          })
        )
      );
    }
    setup$.push(this.tripMarkerIconProcessor.setup());

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

  protected isValidLayerData(_layerData: FormattedData<TbMapDatasource>): boolean {
    throw new Error('Not implemented!');
  }

  protected createLayerItem(_data: FormattedData<TbMapDatasource>, _dsData: FormattedData<TbMapDatasource>[]): TbDataLayerItem<TripsDataLayerSettings,TbTripsDataLayer> {
    throw new Error('Not implemented!');
  }

  public updateData(_dsData: FormattedData<TbMapDatasource>[]) {
    throw new Error('Not implemented!');
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
    const minTime = this.map.getMinTime();
    const maxTime = this.map.getMaxTime();
    const currentTime = this.map.getCurrentTime();
    console.log(`Update trips: min(${minTime}), max(${maxTime}), current(${currentTime})`);
    const toDelete = new Set(Array.from(this.tripItems.keys()));
    this.rawTripsData.forEach(rawTripData => {
      const entityId = rawTripData[0].entityId;
      let tripItem = this.tripItems.get(entityId);
      if (tripItem) {
        tripItem.update(rawTripData);
      } else {
        const latestData = this.latestTripsData.find(d => d.entityId === entityId);
        tripItem = new TbTripDataItem(rawTripData, latestData, this.settings, this);
        this.tripItems.set(entityId, tripItem);
      }
      toDelete.delete(entityId);
    });
    toDelete.forEach((key) => {
      this.removeItem(key);
    });
  }

  public removeItem(key: string): void {
    const item = this.tripItems.get(key);
    if (item) {
      item.remove();
      this.tripItems.delete(key);
    }
  }

  public updateTripsLatestData(tripsLatestData: FormattedData<TbMapDatasource>[]) {
    this.latestTripsData = tripsLatestData.filter(d => d.$datasource.mapDataIds.includes(this.mapDataId));
    console.log(`Update trips latest data`);
    this.tripItems.forEach((item, entityId) => {
      const latestData = this.latestTripsData.find(d => d.entityId === entityId);
      item.updateLatestData(latestData);
    });
  }

  public updateCurrentTime() {
    const currentTime = this.map.getCurrentTime();
    console.log(`Update trips current time: current(${currentTime})`);
    this.tripItems.forEach(item => {
      item.updateCurrentTime();
    });
  }

  public updateAppearance() {
    console.log(`Update trips appearance`);
    this.tripItems.forEach(item => {
      item.updateAppearance();
    });
  }

  public calculateAnchors(): number[] {
    return [];
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

  public createDefaultMarkerIcon(rotationAngle = 0): Observable<MarkerIconInfo> {
    const color = this.settings.markerShape?.color?.color || '#307FE5';
    return this.createColoredMarkerShape(MarkerShape.markerShape1, tinycolor(color), rotationAngle);
  }

  public createColoredMarkerShape(shape: MarkerShape, color: tinycolor.Instance, rotationAngle = 0, size = 34): Observable<MarkerIconInfo> {
    return createColorMarkerShapeURI(this.getCtx().$injector.get(MatIconRegistry), this.getCtx().$injector.get(DomSanitizer), shape, color).pipe(
      map((iconUrl) => {
        const style = `background-image: url(${iconUrl}); transform: rotate(${rotationAngle}deg); height: ${size}px; width: ${size}px;`;
        return {
          size: [size, size],
          icon: L.divIcon({
            html: `<div style="${style}"></div>`,
            className: 'tb-marker-div-icon',
            iconSize: [size, size],
            iconAnchor: [size * this.markerOffset[0], size * this.markerOffset[1]],
            popupAnchor: [size * this.tooltipOffset[0], size * this.tooltipOffset[1]]
          })
        };
      })
    );
  }

  public createColoredMarkerIcon(icon: string, color: tinycolor.Instance, rotationAngle = 0, size = 34): Observable<MarkerIconInfo> {
    return createColorMarkerIconElement(this.getCtx().$injector.get(MatIconRegistry), this.getCtx().$injector.get(DomSanitizer), icon, color, true).pipe(
      map((element) => {
        element.style.transform = `rotate(${rotationAngle}deg)`;
        return {
          size: [size, size],
          icon: L.divIcon({
            html: element.outerHTML,
            className: 'tb-marker-div-icon',
            iconSize: [size, size],
            iconAnchor: [size * this.markerOffset[0], size * this.markerOffset[1]],
            popupAnchor: [size * this.tooltipOffset[0], size * this.tooltipOffset[1]]
          })
        };
      })
    );
  }

  private extractLocationData(data: FormattedData<TbMapDatasource>):  {x: number; y: number} {
    if (data) {
      const xKeyVal = data[this.settings.xKey.label];
      const yKeyVal = data[this.settings.yKey.label];
      switch (this.mapType()) {
        case MapType.geoMap:
          if (!isValidLatLng(xKeyVal, yKeyVal)) {
            return null;
          }
          break;
        case MapType.image:
          if (!isDefinedAndNotNull(xKeyVal) || isEmptyStr(xKeyVal) || isNaN(xKeyVal) || !isDefinedAndNotNull(yKeyVal) || isEmptyStr(yKeyVal) || isNaN(yKeyVal)) {
            return null;
          }
          break;
      }
      return {x: xKeyVal, y: yKeyVal};
    } else {
      return null;
    }
  }

  public extractLocation(data: FormattedData<TbMapDatasource>, dsData: FormattedData<TbMapDatasource>[]): L.LatLng {
    let locationData = this.extractLocationData(data);
    if (locationData) {
      if (this.map.type() === MapType.image && this.positionFunction) {
        const imageMap = this.map as TbImageMap;
        locationData = this.positionFunction.execute(locationData.x, locationData.y, data, dsData, imageMap.getAspect()) || {x: 0, y: 0};
      }
      return this.map.locationDataToLatLng(locationData);
    } else {
      return null;
    }
  }

}
