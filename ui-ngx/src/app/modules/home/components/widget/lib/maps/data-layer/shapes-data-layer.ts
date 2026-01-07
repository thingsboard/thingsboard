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
  DataLayerColorSettings,
  loadImageWithAspect,
  ShapeDataLayerSettings,
  ShapeFillImageFunction,
  ShapeFillImageInfo,
  ShapeFillImageSettings,
  ShapeFillImageType,
  ShapeFillStripeSettings,
  ShapeFillType,
  TbMapDatasource
} from '@shared/models/widget/maps/map.models';
import L from 'leaflet';
import { TbMap } from '@home/components/widget/lib/maps/map';
import { forkJoin, Observable, of } from 'rxjs';
import { FormattedData } from '@shared/models/widget.models';
import { TbLatestMapDataLayer } from '@home/components/widget/lib/maps/data-layer/latest-map-data-layer';
import { DataLayerColorProcessor, TbMapDataLayer } from './map-data-layer';
import { map } from 'rxjs/operators';
import { isDefinedAndNotNull, objectHashCode, parseTbFunction, safeExecuteTbFunction } from '@core/utils';
import { CompiledTbFunction } from '@shared/models/js-function.models';
import { ImagePipe } from '@shared/pipe/image.pipe';

export type ShapePatternStorage = {
  [id: string]: {
    pattern: L.TB.Pattern;
    refCount: number;
  }
};

interface ShapePatternInfo {
  type: ShapeFillType;
  fillColor?: string;
  fillImage?: {
    image: string;
    width: number;
    height: number;
    preserveAspectRatio: boolean;
    opacity: number;
    angle: number;
    scale: number;
  };
  fillStripe?: {
    weight: number;
    color: string;
    spaceWeight: number;
    spaceColor: string;
    angle: number;
  }
}

interface PatternWithId {
  patternId: string;
  pattern: L.TB.Pattern;
}

export interface ShapeStyleInfo {
  patternId: string;
  style: L.PathOptions;
}

abstract class ShapePatternProcessor<S = any> {

  static fromSettings(dataLayer: TbMapDataLayer,
                      settings: ShapeDataLayerSettings): ShapePatternProcessor {
    switch (settings.fillType) {
      case ShapeFillType.color:
        return new ShapeColorPatternProcessor(dataLayer, settings.fillColor);
      case ShapeFillType.image:
        return new ShapeImagePatternProcessor(dataLayer, settings.fillImage);
      case ShapeFillType.stripe:
        return new ShapeStripePatternProcessor(dataLayer, settings.fillStripe);
    }
  }

  protected constructor(protected dataLayer: TbMapDataLayer,
                        protected settings: S) {}

  public abstract setup(): Observable<any>;

  protected abstract computePattern(data: FormattedData<TbMapDatasource>,
                                    dsData: FormattedData<TbMapDatasource>[]): Observable<ShapePatternInfo>;

  public processPattern(data: FormattedData<TbMapDatasource>,
                        dsData: FormattedData<TbMapDatasource>[], prevPatternId?: string): Observable<PatternWithId> {
    return this.computePattern(data, dsData).pipe(
      map((patternInfo) => this.patternFromPatternInfo(patternInfo, prevPatternId))
    );
  }

  private patternFromPatternInfo(patternInfo: ShapePatternInfo, prevPatternId?: string): PatternWithId {
    const patternId = objectHashCode(patternInfo) + '';
    let pattern = this.dataLayer.getMap().useShapePattern(patternId, prevPatternId);
    if (!pattern) {
      pattern = this.constructPattern(patternInfo);
      this.dataLayer.getMap().storeShapePattern(patternId, pattern);
    }
    return {
      pattern,
      patternId
    };
  }

  private constructPattern(patternInfo: ShapePatternInfo): L.TB.Pattern {
    let pattern: L.TB.Pattern;
    if (patternInfo.type === ShapeFillType.color) {
      pattern = new L.TB.Pattern({width: 1, height: 1});
      const fillRect = new L.TB.PatternRect({x: 0, y: 0, width: 1, height: 1,
        fillOpacity: 1, stroke: false, fill: true, fillColor: patternInfo.fillColor});
      pattern.addElement(fillRect);
    } else if (patternInfo.type === ShapeFillType.image) {
      const patternOptions: L.TB.PatternOptions = {
        width: 1,
        height: 1,
        patternUnits: 'objectBoundingBox',
        patternContentUnits: 'objectBoundingBox',
        viewBox: [0,0,patternInfo.fillImage.width,patternInfo.fillImage.height]
      };
      if (patternInfo.fillImage.preserveAspectRatio) {
        patternOptions.preserveAspectRatioAlign = 'xMidYMid';
        patternOptions.preserveAspectRatioMeetOrSlice = 'slice';
      } else {
        patternOptions.preserveAspectRatioAlign = 'none';
      }
      pattern = new L.TB.Pattern(patternOptions);
      const imagePatternOptions: L.TB.PatternImageOptions = {
        imageUrl: patternInfo.fillImage.image,
        width: patternInfo.fillImage.width,
        height: patternInfo.fillImage.height,
        opacity: patternInfo.fillImage.opacity,
        angle: patternInfo.fillImage.angle,
        scale: patternInfo.fillImage.scale
      };
      if (patternInfo.fillImage.preserveAspectRatio) {
        imagePatternOptions.preserveAspectRatioAlign = 'xMidYMid';
        imagePatternOptions.preserveAspectRatioMeetOrSlice = 'slice';
      } else {
        imagePatternOptions.preserveAspectRatioAlign = 'none';
      }
      const imagePatternShape = new L.TB.PatternImage(imagePatternOptions);
      pattern.addElement(imagePatternShape);
    } else if (patternInfo.type === ShapeFillType.stripe) {
      const stripeInfo = patternInfo.fillStripe;
      const height = stripeInfo.weight + stripeInfo.spaceWeight;
      pattern = new L.TB.Pattern({width: 8, height, angle: stripeInfo.angle});
      const stripePattern = new L.TB.PatternPath({
        d: 'M0 ' + stripeInfo.weight / 2 + ' H ' + 8,
        stroke: true,
        weight: stripeInfo.weight,
        color: stripeInfo.color,
        opacity: 1
      });
      pattern.addElement(stripePattern);
      const spacePattern = new L.TB.PatternPath({
        d: 'M0 ' + (stripeInfo.weight + stripeInfo.spaceWeight / 2) + ' H ' + 8,
        stroke: true,
        weight: stripeInfo.spaceWeight,
        color: stripeInfo.spaceColor,
        opacity: 1
      });
      pattern.addElement(spacePattern);
    }
    return pattern;
  }

}

class ShapeColorPatternProcessor extends ShapePatternProcessor<DataLayerColorSettings> {

  private fillColorProcessor: DataLayerColorProcessor;

  constructor(protected dataLayer: TbMapDataLayer,
              protected settings: DataLayerColorSettings) {
    super(dataLayer, settings);
  }

  public setup(): Observable<any> {
    this.fillColorProcessor = new DataLayerColorProcessor(this.dataLayer, this.settings);
    return this.fillColorProcessor.setup();
  }

  protected computePattern(data: FormattedData<TbMapDatasource>, dsData: FormattedData<TbMapDatasource>[]): Observable<ShapePatternInfo> {
    const fillColor = this.fillColorProcessor.processColor(data, dsData);
    const shapePatternInfo: ShapePatternInfo = {
      type: ShapeFillType.color,
      fillColor
    };
    return of(shapePatternInfo);
  }

}

class ShapeImagePatternProcessor extends ShapePatternProcessor<ShapeFillImageSettings> {

  private shapeFillImageFunction: CompiledTbFunction<ShapeFillImageFunction>;

  constructor(protected dataLayer: TbMapDataLayer,
              protected settings: ShapeFillImageSettings) {
    super(dataLayer, settings);
  }

  public setup(): Observable<any> {
    if (this.settings.type === ShapeFillImageType.function) {
      return parseTbFunction<ShapeFillImageFunction>(this.dataLayer.getCtx().http, this.settings.imageFunction, ['data', 'images', 'dsData']).pipe(
        map((parsed) => {
          this.shapeFillImageFunction = parsed;
          return null;
        })
      );
    } else {
      return of(null);
    }
  }

  protected computePattern(data: FormattedData<TbMapDatasource>, dsData: FormattedData<TbMapDatasource>[]): Observable<ShapePatternInfo> {
    let currentImage: ShapeFillImageInfo;
    if (this.settings.type === ShapeFillImageType.function) {
      currentImage = safeExecuteTbFunction(this.shapeFillImageFunction, [data, this.settings.images, dsData]);
    }
    if (!currentImage?.url) {
      currentImage = {
        url: this.settings.image,
        preserveAspectRatio: this.settings.preserveAspectRatio,
        opacity: this.settings.opacity,
        angle: this.settings.angle,
        scale: this.settings.scale
      };
    }
    return this.loadPatternInfoFromImage(currentImage);
  }

  private loadPatternInfoFromImage(image: ShapeFillImageInfo): Observable<ShapePatternInfo> {
    const imageUrl = image?.url || '/assets/widget-preview-empty.svg';
    const preserveAspectRatio = isDefinedAndNotNull(image?.preserveAspectRatio) ? image.preserveAspectRatio : true;
    const opacity = isDefinedAndNotNull(image?.opacity) ? image.opacity : 1;
    const imagePipe = this.dataLayer.getCtx().$injector.get(ImagePipe);
    return loadImageWithAspect(imagePipe, imageUrl).pipe(
      map((res) => {
        const shapePatternInfo: ShapePatternInfo = {
          type: ShapeFillType.image,
          fillImage: {
            image: res.url,
            width: res.width,
            height: res.height,
            preserveAspectRatio,
            opacity,
            angle: image?.angle,
            scale: image?.scale
          }
        };
        return shapePatternInfo;
      })
    );
  }
}

class ShapeStripePatternProcessor extends ShapePatternProcessor<ShapeFillStripeSettings> {

  private colorProcessor: DataLayerColorProcessor;
  private spaceColorProcessor: DataLayerColorProcessor;

  constructor(protected dataLayer: TbMapDataLayer,
              protected settings: ShapeFillStripeSettings) {
    super(dataLayer, settings);
  }

  public setup(): Observable<any> {
    this.colorProcessor = new DataLayerColorProcessor(this.dataLayer, this.settings.color);
    this.spaceColorProcessor = new DataLayerColorProcessor(this.dataLayer, this.settings.spaceColor);
    return forkJoin([this.colorProcessor.setup(), this.spaceColorProcessor.setup()]);
  }

  protected computePattern(data: FormattedData<TbMapDatasource>, dsData: FormattedData<TbMapDatasource>[]): Observable<ShapePatternInfo> {
    const color = this.colorProcessor.processColor(data, dsData);
    const spaceColor = this.spaceColorProcessor.processColor(data, dsData);
    return of({
      type: ShapeFillType.stripe,
      fillStripe: {
        color,
        spaceColor,
        angle: this.settings.angle,
        weight: this.settings.weight,
        spaceWeight: this.settings.spaceWeight
      }
    });
  }

}

export abstract class TbShapesDataLayer<S extends ShapeDataLayerSettings, L extends TbLatestMapDataLayer<S,L>> extends TbLatestMapDataLayer<S, L> {

  private shapePatternProcessor: ShapePatternProcessor;
  private strokeColorProcessor: DataLayerColorProcessor;

  protected constructor(protected map: TbMap<any>,
                        inputSettings: S) {
    super(map, inputSettings);
  }

  public getStrokeStyle(data: FormattedData<TbMapDatasource>, dsData: FormattedData<TbMapDatasource>[], fillPatternId: string): Observable<ShapeStyleInfo> {
    return this.shapePatternProcessor.processPattern(data, dsData, fillPatternId).pipe(
      map((patternWithId) => {
        const stroke = this.strokeColorProcessor.processColor(data, dsData);
        const style: L.PathOptions = {
          color: stroke,
          weight: this.settings.strokeWeight,
          opacity: 1
        };
        return {
          patternId: patternWithId.patternId,
          style
        }
      })
    );
  }

  public getShapeStyle(data: FormattedData<TbMapDatasource>, dsData: FormattedData<TbMapDatasource>[], fillPatternId: string): Observable<ShapeStyleInfo> {
    return this.shapePatternProcessor.processPattern(data, dsData, fillPatternId).pipe(
      map((patternWithId) => {
        const stroke = this.strokeColorProcessor.processColor(data, dsData);
        const style: L.PathOptions = {
          fill: true,
          fillPattern: patternWithId.pattern,
          color: stroke,
          weight: this.settings.strokeWeight,
          fillOpacity: 1,
          opacity: 1
        };
        return {
          patternId: patternWithId.patternId,
          style
        }
      })
    );
  }

  protected allColorSettings(): DataLayerColorSettings[] {
    const colorSettings:  DataLayerColorSettings[] = [this.settings.strokeColor];
    if (this.settings.fillType === ShapeFillType.color) {
      colorSettings.push(this.settings.fillColor)
    } else if (this.settings.fillType === ShapeFillType.stripe) {
      if (this.settings.fillStripe?.color) {
        colorSettings.push(this.settings.fillStripe.color);
      }
      if (this.settings.fillStripe?.spaceColor) {
        colorSettings.push(this.settings.fillStripe.spaceColor);
      }
    }
    return colorSettings;
  }

  protected doSetup(): Observable<any> {
    this.shapePatternProcessor = ShapePatternProcessor.fromSettings(this, this.settings);
    this.strokeColorProcessor = new DataLayerColorProcessor(this, this.settings.strokeColor);
    return forkJoin([this.shapePatternProcessor.setup(), this.strokeColorProcessor.setup()]);
  }

}
