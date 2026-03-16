///
/// Copyright © 2016-2026 The Thingsboard Authors
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
  BaseMarkerShapeSettings,
  ClusterMarkerColorFunction,
  DataLayerColorSettings,
  DataLayerColorType,
  defaultBaseMarkersDataLayerSettings,
  isValidLatLng,
  loadImageWithAspect,
  MapDataLayerType,
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
  TbMapDatasource
} from '@shared/models/widget/maps/map.models';
import L, { FeatureGroup } from 'leaflet';
import { DataKey, FormattedData } from '@shared/models/widget.models';
import { forkJoin, Observable, of } from 'rxjs';
import { CompiledTbFunction } from '@shared/models/js-function.models';
import {
  deepClone,
  isDefined,
  isDefinedAndNotNull,
  isEmptyStr,
  parseTbFunction,
  safeExecuteTbFunction
} from '@core/utils';
import { catchError, map, switchMap } from 'rxjs/operators';
import tinycolor from 'tinycolor2';
import { ImagePipe } from '@shared/pipe/image.pipe';
import { TbMap } from '@home/components/widget/lib/maps/map';
import {
  createColorMarkerIconElement,
  createColorMarkerShapeURI,
  MarkerIconContainer,
  MarkerShape
} from '@shared/models/widget/maps/marker-shape.models';
import { MatIconRegistry } from '@angular/material/icon';
import { DomSanitizer } from '@angular/platform-browser';
import {
  TbLatestDataLayerItem,
  TbLatestMapDataLayer,
  UnplacedMapDataItem
} from '@home/components/widget/lib/maps/data-layer/latest-map-data-layer';
import { TbImageMap } from '@home/components/widget/lib/maps/image-map';
import { DataLayerColorProcessor, TbMapDataLayer } from '@home/components/widget/lib/maps/data-layer/map-data-layer';

export class MarkerDataProcessor<S extends MarkersDataLayerSettings = MarkersDataLayerSettings> {

  private positionFunction: CompiledTbFunction<MarkerPositionFunction>;
  private markerIconProcessor: MarkerIconProcessor<any>;

  constructor(public dataLayer: TbMapDataLayer,
              private settings: S,
              public markerOffset: L.LatLngTuple,
              public tooltipOffset: L.LatLngTuple) {
  }

  public setup(): Observable<any> {
    this.markerIconProcessor = MarkerIconProcessor.fromSettings(this, this.settings);
    const setup$: Observable<void>[] = [this.markerIconProcessor.setup()];
    if (this.dataLayer.mapType() === MapType.image) {
      setup$.push(
        parseTbFunction<MarkerPositionFunction>(this.dataLayer.getCtx().http, this.settings.positionFunction, ['origXPos', 'origYPos', 'data', 'dsData', 'aspect']).pipe(
          map((parsed) => {
            this.positionFunction = parsed;
            return null;
          })
        )
      );
    }
    return forkJoin(setup$).pipe(map(() => null));
  }

  public extractLocation(data: FormattedData<TbMapDatasource>, dsData: FormattedData<TbMapDatasource>[]): L.LatLng {
    let locationData = this.extractLocationData(data);
    if (locationData) {
      if (this.dataLayer.mapType() === MapType.image && this.positionFunction) {
        const imageMap = this.dataLayer.getMap() as TbImageMap;
        locationData = this.positionFunction.execute(locationData.x, locationData.y, data, dsData, imageMap.getAspect()) || {x: 0, y: 0};
      }
      return this.dataLayer.getMap().locationDataToLatLng(locationData);
    } else {
      return null;
    }
  }

  public extractLocationData(data: FormattedData<TbMapDatasource>):  {x: number; y: number} {
    if (data) {
      const xKeyVal = data[this.settings.xKey.label];
      const yKeyVal = data[this.settings.yKey.label];
      switch (this.dataLayer.mapType()) {
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

  public createMarkerIcon(data: FormattedData<TbMapDatasource>,
                          dsData: FormattedData<TbMapDatasource>[],
                          rotationAngle?: number): Observable<MarkerIconInfo> {
    return this.markerIconProcessor.createMarkerIcon(data, dsData, rotationAngle);
  }

  public createDefaultMarkerIcon(rotationAngle = 0): Observable<MarkerIconInfo> {
    const color = this.settings.markerShape?.color?.color || '#307FE5';
    return this.createColoredMarkerShape(MarkerShape.markerShape1, tinycolor(color), rotationAngle);
  }

  public createColoredMarkerShape(shape: MarkerShape, color: tinycolor.Instance, rotationAngle = 0, size = 34): Observable<MarkerIconInfo> {
    return createColorMarkerShapeURI(this.dataLayer.getCtx().$injector.get(MatIconRegistry), this.dataLayer.getCtx().$injector.get(DomSanitizer), shape, color).pipe(
      map((iconUrl) => {
        let icon: L.Icon<L.BaseIconOptions>;
        if (rotationAngle === 0) {
          icon = L.icon({
            iconUrl,
            iconSize: [size, size],
            iconAnchor: [size * this.markerOffset[0], size * this.markerOffset[1]],
            popupAnchor: [size * this.tooltipOffset[0], size * this.tooltipOffset[1]]
          });
        } else {
          const style = `background-image: url(${iconUrl}); transform: rotate(${rotationAngle}deg); height: ${size}px; width: ${size}px;`;
          icon = L.divIcon({
            html: `<div style="${style}"></div>`,
            className: 'tb-marker-div-icon',
            iconSize: [size, size],
            iconAnchor: [size * this.markerOffset[0], size * this.markerOffset[1]],
            popupAnchor: [size * this.tooltipOffset[0], size * this.tooltipOffset[1]]
          });
        }
        return {
          size: [size, size],
          icon
        }
      })
    );
  }

  public createColoredMarkerIcon(iconContainer: MarkerIconContainer,
                                 icon: string, color: tinycolor.Instance, rotationAngle = 0, size = 34): Observable<MarkerIconInfo> {
    return createColorMarkerIconElement(this.dataLayer.getCtx().$injector.get(MatIconRegistry), this.dataLayer.getCtx().$injector.get(DomSanitizer),
      iconContainer, icon, color).pipe(
      map((element) => {
        if (rotationAngle !== 0) {
          element.style.transform = `rotate(${rotationAngle}deg)`;
        }
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


abstract class MarkerIconProcessor<S> {

  static fromSettings(dataProcessor: MarkerDataProcessor,
                      settings: MarkersDataLayerSettings): MarkerIconProcessor<any> {
    switch (settings.markerType) {
      case MarkerType.shape:
        return new ShapeMarkerIconProcessor(dataProcessor, settings.markerShape);
      case MarkerType.icon:
        return new IconMarkerIconProcessor(dataProcessor, settings.markerIcon);
      case MarkerType.image:
        return new ImageMarkerIconProcessor(dataProcessor, settings.markerImage);
    }
  }

  protected constructor(protected dataProcessor: MarkerDataProcessor,
                        protected settings: S) {}

  public abstract setup(): Observable<void>;

  public abstract createMarkerIcon(data: FormattedData<TbMapDatasource>,
                                   dsData: FormattedData<TbMapDatasource>[],
                                   rotationAngle?: number): Observable<MarkerIconInfo>;

}

abstract class BaseColorMarkerShapeProcessor<S extends BaseMarkerShapeSettings> extends MarkerIconProcessor<S> {

  private colorProcessor: DataLayerColorProcessor;
  private defaultMarkerIconInfo: MarkerIconInfo;

  protected constructor(protected dataProcessor: MarkerDataProcessor,
                        protected settings: S) {
    super(dataProcessor, settings);
  }

  public setup(): Observable<void> {
    const colorSettings = this.settings.color;
    this.colorProcessor = new DataLayerColorProcessor(this.dataProcessor.dataLayer, colorSettings);
    const setup$: Observable<void>[] = [this.colorProcessor.setup()];
    if (colorSettings.type === DataLayerColorType.constant) {
      const color = tinycolor(colorSettings.color);
      setup$.push(
        this.createMarkerShape(color, 0, this.settings.size).pipe(
          map((info) => {
              this.defaultMarkerIconInfo = info;
              return null;
          }))
      );
    }
    return forkJoin(setup$).pipe(map(() => null));
  }

  public createMarkerIcon(data: FormattedData<TbMapDatasource>, dsData: FormattedData<TbMapDatasource>[], rotationAngle = 0): Observable<MarkerIconInfo> {
    const colorSettings = this.settings.color;
    if (colorSettings.type === DataLayerColorType.constant && rotationAngle === 0) {
      return of(this.defaultMarkerIconInfo);
    } else {
      const color = this.colorProcessor.processColor(data, dsData);
      return this.createMarkerShape(tinycolor(color), rotationAngle, this.settings.size);
    }
  }

  protected abstract createMarkerShape(color: tinycolor.Instance, rotationAngle: number, size: number): Observable<MarkerIconInfo>;
}

class ShapeMarkerIconProcessor extends BaseColorMarkerShapeProcessor<MarkerShapeSettings> {

  constructor(protected dataProcessor: MarkerDataProcessor,
              protected settings: MarkerShapeSettings) {
    super(dataProcessor, settings);
  }

  protected createMarkerShape(color: tinycolor.Instance, rotationAngle: number, size: number): Observable<MarkerIconInfo> {
    return this.dataProcessor.createColoredMarkerShape(this.settings.shape, color, rotationAngle, size);
  }

}

class IconMarkerIconProcessor extends BaseColorMarkerShapeProcessor<MarkerIconSettings> {

  constructor(protected dataProcessor: MarkerDataProcessor,
              protected settings: MarkerIconSettings) {
    super(dataProcessor, settings);
  }

  protected createMarkerShape(color: tinycolor.Instance, rotationAngle: number, size: number): Observable<MarkerIconInfo> {
    return this.dataProcessor.createColoredMarkerIcon(this.settings.iconContainer, this.settings.icon, color, rotationAngle, size);
  }

}

class ImageMarkerIconProcessor extends MarkerIconProcessor<MarkerImageSettings> {

  private markerImageFunction: CompiledTbFunction<MarkerImageFunction>;

  private defaultMarkerIconInfo: MarkerIconInfo;

  constructor(protected dataProcessor: MarkerDataProcessor,
              protected settings: MarkerImageSettings) {
    super(dataProcessor, settings);
  }

  public setup(): Observable<void> {
    if (this.settings.type === MarkerImageType.function) {
      return parseTbFunction<MarkerImageFunction>(this.dataProcessor.dataLayer.getCtx().http, this.settings.imageFunction, ['data', 'images', 'dsData']).pipe(
        map((parsed) => {
          this.markerImageFunction = parsed;
          return null;
        })
      );
    } else {
      const currentImage: MarkerImageInfo = {
        url: this.settings.image,
        size: this.settings.imageSize || 34
      };
      return this.loadMarkerIconInfo(currentImage, 0).pipe(
        map((iconInfo) => {
            this.defaultMarkerIconInfo = iconInfo;
            return null;
          }
        ));
    }
  }

  public createMarkerIcon(data: FormattedData<TbMapDatasource>, dsData: FormattedData<TbMapDatasource>[], rotationAngle = 0): Observable<MarkerIconInfo> {
    if (this.settings.type === MarkerImageType.function) {
      const currentImage: MarkerImageInfo = safeExecuteTbFunction(this.markerImageFunction, [data, this.settings.images, dsData]);
      return this.loadMarkerIconInfo(currentImage, rotationAngle);
    } else if (rotationAngle === 0) {
      return of(this.defaultMarkerIconInfo);
    } else {
      const currentImage: MarkerImageInfo = {
        url: this.settings.image,
        size: this.settings.imageSize || 34
      };
      return this.loadMarkerIconInfo(currentImage, rotationAngle);
    }
  }

  private loadMarkerIconInfo(image: MarkerImageInfo, rotationAngle = 0): Observable<MarkerIconInfo> {
    if (image && image.url) {
      return loadImageWithAspect(this.dataProcessor.dataLayer.getCtx().$injector.get(ImagePipe), image.url).pipe(
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
              iconAnchor = [width * this.dataProcessor.markerOffset[0], height * this.dataProcessor.markerOffset[1]];
            }
            if (!popupAnchor) {
              popupAnchor = [width * this.dataProcessor.tooltipOffset[0], height * this.dataProcessor.tooltipOffset[1]];
            }
            let icon: L.Icon<L.BaseIconOptions>;
            if (rotationAngle === 0) {
              icon = L.icon({
                iconUrl: aspectImage.url,
                iconSize: [width, height],
                iconAnchor,
                popupAnchor
              });
            } else {
              const style = `background-image: url(${aspectImage.url}); background-size: contain; transform: rotate(${rotationAngle}deg); height: ${height}px; width: ${width}px;`;
              icon = L.divIcon({
                html: `<div style="${style}"></div>`,
                className: 'tb-marker-div-icon',
                iconSize: [width, height],
                iconAnchor,
                popupAnchor
              });
            }
            const iconInfo: MarkerIconInfo = {
              size: [width, height],
              icon
            };
            return of(iconInfo);
          } else {
            return this.dataProcessor.createDefaultMarkerIcon(rotationAngle);
          }
        }),
        catchError(() => this.dataProcessor.createDefaultMarkerIcon(rotationAngle))
      );
    } else {
      return this.dataProcessor.createDefaultMarkerIcon(rotationAngle);
    }
  }
}

class TbMarkerDataLayerItem extends TbLatestDataLayerItem<MarkersDataLayerSettings, TbMarkersDataLayer, L.Marker> {

  private marker: L.Marker;
  private labelOffset: L.PointTuple;
  private iconClassList: string[];
  private moving = false;
  private dragStart: () => void;
  private dragEnd: () => void;

  constructor(data: FormattedData<TbMapDatasource>,
              dsData: FormattedData<TbMapDatasource>[],
              protected settings: MarkersDataLayerSettings,
              protected dataLayer: TbMarkersDataLayer) {
    super(data, dsData, settings, dataLayer);
  }

  public isEditing() {
    return this.moving;
  }

  public updateBubblingMouseEvents() {
    this.marker.options.bubblingMouseEvents = !this.dataLayer.isEditMode();
  }

  protected create(data: FormattedData<TbMapDatasource>, dsData: FormattedData<TbMapDatasource>[]): L.Marker {
    this.iconClassList = [];
    const location = this.dataLayer.dataProcessor.extractLocation(data, dsData);
    this.dragStart = this._dragStart.bind(this);
    this.dragEnd = this._dragEnd.bind(this);
    this.marker = L.marker(location, {
      tbMarkerData: data,
      snapIgnore: !this.dataLayer.isSnappable(),
      bubblingMouseEvents: !this.dataLayer.isEditMode()
    });
    this.updateMarkerIcon(data, dsData);
    return this.marker;
  }

  protected unbindLabel() {
    this.marker.unbindTooltip();
  }

  protected bindLabel(content: L.Content): void {
    this.marker.bindTooltip(content, { className: 'tb-marker-label', permanent: true, direction: 'top', offset: this.labelOffset });
  }

  protected doUpdate(data: FormattedData<TbMapDatasource>, dsData: FormattedData<TbMapDatasource>[]): void {
    this.marker.options.tbMarkerData = data;
    this.updateMarkerLocation(data, dsData);
    this.updateTooltip(data, dsData);
    this.updateMarkerIcon(data, dsData);
  }

  protected doInvalidateCoordinates(data: FormattedData<TbMapDatasource>, dsData: FormattedData<TbMapDatasource>[]): void {
    this.updateMarkerLocation(data, dsData);
  }

  protected addItemClass(clazz: string): void {
    if (!this.iconClassList.includes(clazz)) {
      this.iconClassList.push(clazz);
      this.marker.options.icon.options.className = this.updateIconClasses(this.marker.options.icon.options.className);
      if ((this.marker as any)._icon) {
        L.DomUtil.addClass((this.marker as any)._icon, clazz);
      }
    }
  }

  protected removeItemClass(clazz: string): void {
    const index = this.iconClassList.indexOf(clazz);
    if (index !== -1) {
      this.iconClassList.splice(index, 1);
      this.marker.options.icon.options.className = this.updateIconClasses(this.marker.options.icon.options.className, clazz);
      if ((this.marker as any)._icon) {
        L.DomUtil.removeClass((this.marker as any)._icon, clazz);
      }
    }
  }

  protected enableDrag(): void {
    this.marker.options.draggable = true;
    this.marker.dragging?.enable();
    if (!this.settings.markerClustering?.enable) {
      this.marker.pm.setOptions({
        snappable: this.dataLayer.isSnappable()
      });
      this.marker.pm.enableLayerDrag();
    }
    const evtPrefix = this.settings.markerClustering?.enable ? '' : 'pm:';
    this.marker.on(evtPrefix + 'dragstart', this.dragStart);
    this.marker.on(evtPrefix + 'dragend', this.dragEnd);
  }

  protected disableDrag(): void {
    this.marker.options.draggable = false;
    this.marker.dragging?.disable();
    if (!this.settings.markerClustering?.enable) {
      this.marker.pm.disableLayerDrag();
    }
    const evtPrefix = this.settings.markerClustering?.enable ? '' : 'pm:';
    this.marker.off(evtPrefix + 'dragstart', this.dragStart);
    this.marker.off(evtPrefix + 'dragend', this.dragEnd);
  }

  protected removeDataItemTitle(): string {
    return this.dataLayer.getCtx().translate.instant('widgets.maps.data-layer.marker.remove-marker-for', {entityName: this.data.entityName});
  }

  protected removeDataItem(): Observable<any> {
    return this.dataLayer.saveMarkerLocation(this.data, null);
  }

  private _dragStart() {
    this.moving = true;
  }

  private _dragEnd() {
    this.saveMarkerLocation();
    this.moving = false;
  }

  private saveMarkerLocation() {
    const location = this.marker.getLatLng();
    this.dataLayer.saveMarkerLocation(this.data, location).subscribe();
  }

  private updateMarkerLocation(data: FormattedData<TbMapDatasource>, dsData: FormattedData<TbMapDatasource>[]) {
    const location = this.dataLayer.dataProcessor.extractLocation(data, dsData);
    if (!this.marker.getLatLng().equals(location) && !this.moving) {
      this.marker.setLatLng(location);
    }
  }

  private updateMarkerIcon(data: FormattedData<TbMapDatasource>, dsData: FormattedData<TbMapDatasource>[]) {
    this.dataLayer.dataProcessor.createMarkerIcon(data, dsData).subscribe(
      (iconInfo) => {
        let icon: L.Icon | L.DivIcon;
        const options = deepClone(iconInfo.icon.options);
        options.className = this.updateIconClasses(options.className);
        if (iconInfo.icon instanceof L.DivIcon) {
          icon = L.divIcon(options);
        } else {
          icon = L.icon(options as L.IconOptions);
        }
        this.marker.setIcon(icon);
        const anchor = options.iconAnchor;
        if (anchor && Array.isArray(anchor)) {
          this.labelOffset = [iconInfo.size[0] / 2 - anchor[0], 10 - anchor[1]];
        } else {
          this.labelOffset = [0, -iconInfo.size[1] * this.dataLayer.markerOffset[1] + 10];
        }
        this.updateLabel(data, dsData);
        this.editModeUpdated();
      }
    );
  }

  private updateIconClasses(className: string, toRemove?: string): string {
    const classes: string[] = [];
    if (className?.length) {
      classes.push(...className.split(' '));
    }
    if (toRemove?.length) {
      const index = classes.indexOf(toRemove);
      if (index !== -1) {
        classes.splice(index, 1);
      }
    }
    this.iconClassList.forEach(clazz => {
      if (!classes.includes(clazz)) {
        classes.push(clazz);
      }
    });
    return classes.join(' ');
  }
}

export class TbMarkersDataLayer extends TbLatestMapDataLayer<MarkersDataLayerSettings, TbMarkersDataLayer> {

  public dataProcessor: MarkerDataProcessor;

  public markerOffset: L.LatLngTuple;
  public tooltipOffset: L.LatLngTuple;

  private markersClusterContainer: L.MarkerClusterGroup;
  private clusterMarkerColorFunction: CompiledTbFunction<ClusterMarkerColorFunction>;

  constructor(protected map: TbMap<any>,
              inputSettings: MarkersDataLayerSettings) {
    super(map, inputSettings);
  }

  public dataLayerType(): MapDataLayerType {
    return 'markers';
  }

  public placeItem(item: UnplacedMapDataItem, layer: L.Layer): void {
    if (layer instanceof L.Marker) {
      const position = layer.getLatLng();
      this.saveMarkerLocation(item.entity, position).subscribe(
        (converted) => {
          item.entity[this.settings.xKey.label] = converted.x;
          item.entity[this.settings.yKey.label] = converted.y;
          this.createItemFromUnplaced(item);
        }
      );
    } else {
      console.warn('Unable to place item, layer is not a marker.');
    }
  }

  public saveMarkerLocation(data: FormattedData<TbMapDatasource>, position: L.LatLng): Observable<{x: number; y: number}> {
    const converted = this.map.latLngToLocationData(position);
    const locationData = [
      {
        dataKey: this.settings.xKey,
        value: converted.x
      },
      {
        dataKey: this.settings.yKey,
        value: converted.y
      }
    ];
    return this.map.saveItemData(data.$datasource, locationData, this.settings.edit?.attributeScope).pipe(
      map(() => converted)
    );
  }

  protected createDataLayerContainer(): FeatureGroup {
    if (this.settings.markerClustering?.enable) {
      return this.createMarkersClusterContainer();
    } else {
      return super.createDataLayerContainer();
    }
  }

  protected getDataKeys(): DataKey[] {
    return [this.settings.xKey, this.settings.yKey];
  }

  protected allColorSettings(): DataLayerColorSettings[] {
    if (this.settings.markerType === MarkerType.shape) {
      return [this.settings.markerShape.color];
    } else if (this.settings.markerType === MarkerType.icon) {
      return [this.settings.markerIcon.color];
    }
    return [];
  }

  protected defaultBaseSettings(map: TbMap<any>): Partial<MarkersDataLayerSettings> {
    return defaultBaseMarkersDataLayerSettings(map.type());
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
    this.dataProcessor = new MarkerDataProcessor(this, this.settings, this.markerOffset, this.tooltipOffset);
    const setup$: Observable<void>[] = [this.dataProcessor.setup()];
    if (this.settings.markerClustering?.enable && this.settings.markerClustering.useClusterMarkerColorFunction) {
      setup$.push(
        parseTbFunction<ClusterMarkerColorFunction>(this.getCtx().http, this.settings.markerClustering.clusterMarkerColorFunction, ['data', 'childCount']).pipe(
          map((parsed) => {
            this.clusterMarkerColorFunction = parsed;
            return null;
          })
        )
      );
    }
    return forkJoin(setup$).pipe(map(() => null));
  }

  protected isValidLayerData(layerData: FormattedData<TbMapDatasource>): boolean {
    return !!this.dataProcessor.extractLocationData(layerData);
  }

  protected createLayerItem(data: FormattedData<TbMapDatasource>, dsData: FormattedData<TbMapDatasource>[]): TbMarkerDataLayerItem {
    return new TbMarkerDataLayerItem(data, dsData, this.settings, this);
  }

  protected layerItemsUpdated(updatedItems: TbLatestDataLayerItem<MarkersDataLayerSettings, TbMarkersDataLayer, L.Marker>[]) {
    if (this.settings.markerClustering?.enable) {
      this.markersClusterContainer.refreshClusters(updatedItems.map(item => item.getLayer()));
    }
    super.layerItemsUpdated(updatedItems);
  }

  private createMarkersClusterContainer(): L.FeatureGroup {
    const markerClusterOptions: L.MarkerClusterGroupOptions = {
      spiderfyOnMaxZoom: this.settings.markerClustering?.spiderfyOnMaxZoom,
      zoomToBoundsOnClick: this.settings.markerClustering?.zoomOnClick,
      showCoverageOnHover: this.settings.markerClustering?.showCoverageOnHover,
      removeOutsideVisibleBounds: this.settings.markerClustering?.lazyLoad,
      animate: this.settings.markerClustering?.zoomAnimation,
      chunkedLoading: this.settings.markerClustering?.chunkedLoad,
      snapIgnore: !this.settings.edit?.snappable
    };
    if (this.settings.markerClustering?.useClusterMarkerColorFunction) {
      markerClusterOptions.iconCreateFunction = (cluster) => {
        const childCount = cluster.getChildCount();
        const data = cluster.getAllChildMarkers().map(clusterMarker => clusterMarker.options.tbMarkerData);
        const markerColor: string = this.clusterMarkerColorFunction ?
          safeExecuteTbFunction(this.clusterMarkerColorFunction, [data, childCount]) : null;
        if (isDefinedAndNotNull(markerColor) && tinycolor(markerColor).isValid()) {
          const parsedColor = tinycolor(markerColor);
          const alpha = parsedColor.getAlpha();
          return L.divIcon({
            html: `<div style="background-color: ${parsedColor.setAlpha(alpha * 0.4).toRgbString()};" ` +
              `class="marker-cluster tb-cluster-marker-element">` +
              `<div style="background-color: ${parsedColor.setAlpha(alpha * 0.9).toRgbString()};"><span>` + childCount + '</span></div></div>',
            iconSize: new L.Point(40, 40),
            className: 'tb-cluster-marker-container'
          });
        } else {
          let c = ' marker-cluster-';
          if (childCount < 10) {
            c += 'small';
          } else if (childCount < 100) {
            c += 'medium';
          } else {
            c += 'large';
          }
          return new L.DivIcon({
            html: '<div><span>' + childCount + '</span></div>',
            className: 'marker-cluster' + c,
            iconSize: new L.Point(40, 40)
          });
        }
      }
    }
    if (this.settings.markerClustering?.maxClusterRadius && this.settings.markerClustering.maxClusterRadius > 0) {
      markerClusterOptions.maxClusterRadius = Math.floor(this.settings.markerClustering.maxClusterRadius);
    }
    if (this.settings.markerClustering?.maxZoom && this.settings.markerClustering.maxZoom >= 0 && this.settings.markerClustering.maxZoom < 19) {
      markerClusterOptions.disableClusteringAtZoom = Math.floor(this.settings.markerClustering.maxZoom);
    }
    this.markersClusterContainer = new L.MarkerClusterGroup(markerClusterOptions);
    return this.markersClusterContainer;
  }

}
