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
  BaseMarkerShapeSettings, ClusterMarkerColorFunction,
  DataLayerColorType, defaultBaseMarkersDataLayerSettings, defaultBaseTripsDataLayerSettings,
  loadImageWithAspect,
  MapStringFunction, MapType,
  MarkerIconInfo,
  MarkerIconSettings,
  MarkerImageFunction,
  MarkerImageInfo,
  MarkerImageSettings,
  MarkerImageType,
  MarkerPositionFunction, MarkersDataLayerSettings,
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
import L from 'leaflet';
import { CompiledTbFunction } from '@shared/models/js-function.models';
import { isDefined, isDefinedAndNotNull, parseTbFunction, safeExecuteTbFunction } from '@core/utils';
import { ImagePipe } from '@shared/pipe/image.pipe';
import { TbMap } from '@home/components/widget/lib/maps/map';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';

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
            const style = `background-image: url(${aspectImage.url}); transform: rotate(${rotationAngle}deg); height: ${height}px; width: ${width}px;`;
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
      tripsData.filter(d => d[0].$datasource.mapDataIds.includes(this.mapDataId)).map(
        item => this.clearIncorrectFirsLastDatapoint(item)).filter(arr => arr.length);
    this.latestTripsData = tripsLatestData.filter(d => d.$datasource.mapDataIds.includes(this.mapDataId));
    this.rawTripsData.forEach((dataSource) => {
      minTime = Math.min(dataSource[0].time, minTime);
      maxTime = Math.max(dataSource[dataSource.length - 1].time, maxTime);
    });

    return {minTime, maxTime};
  }

  public updateTrips(minTime: number, maxTime: number) {
    console.log(`Update trips: min(${minTime}), max(${maxTime})`);
  }

  public updateTripsLatestData(tripsLatestData: FormattedData<TbMapDatasource>[]) {
    this.latestTripsData = tripsLatestData.filter(d => d.$datasource.mapDataIds.includes(this.mapDataId));
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
    return createColorMarkerIconElement(this.getCtx().$injector.get(MatIconRegistry), this.getCtx().$injector.get(DomSanitizer), icon, color).pipe(
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

}
