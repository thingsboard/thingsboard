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
  createLabelFromDatasource,
  hashCode,
  isDefined,
  isDefinedAndNotNull,
  isFunction,
  isNumber,
  isUndefined,
  padValue
} from '@core/utils';
import { Observable, Observer, of, switchMap } from 'rxjs';
import { map } from 'rxjs/operators';
import { FormattedData } from '@shared/models/widget.models';
import L from 'leaflet';
import { ImagePipe } from '@shared/pipe/image.pipe';
import { CompiledTbFunction, GenericFunction } from '@shared/models/js-function.models';

export function getRatio(firsMoment: number, secondMoment: number, intermediateMoment: number): number {
  return (intermediateMoment - firsMoment) / (secondMoment - firsMoment);
}

export function interpolateOnLineSegment(
  pointA: FormattedData,
  pointB: FormattedData,
  latKeyName: string,
  lngKeyName: string,
  ratio: number
): { [key: string]: number } {
  return {
    [latKeyName]: (pointA[latKeyName] + (pointB[latKeyName] - pointA[latKeyName]) * ratio),
    [lngKeyName]: (pointA[lngKeyName] + (pointB[lngKeyName] - pointA[lngKeyName]) * ratio)
  };
}

export function findAngle(startPoint: FormattedData, endPoint: FormattedData, latKeyName: string, lngKeyName: string): number {
  if (isUndefined(startPoint) || isUndefined(endPoint)) {
    return 0;
  }
  let angle = -Math.atan2(endPoint[latKeyName] - startPoint[latKeyName], endPoint[lngKeyName] - startPoint[lngKeyName]);
  angle = angle * 180 / Math.PI;
  return parseInt(angle.toFixed(2), 10);
}


export function getDefCenterPosition(position: string | [number, number]): [number, number] {
  if (typeof (position) === 'string') {
    const parts = position.split(',');
    if (parts.length === 2) {
      return [Number(parts[0]), Number(parts[1])];
    }
  }
  if (typeof (position) === 'object') {
    return position;
  }
  return [0, 0];
}


const imageAspectMap: {[key: string]: ImageWithAspect} = {};

const imageLoader = (imageUrl: string): Observable<HTMLImageElement> => new Observable((observer: Observer<HTMLImageElement>) => {
  const image = document.createElement('img'); // support IE
  image.style.position = 'absolute';
  image.style.left = '-99999px';
  image.style.top = '-99999px';
  image.onload = () => {
    observer.next(image);
    document.body.removeChild(image);
    observer.complete();
  };
  image.onerror = err => {
    observer.error(err);
    document.body.removeChild(image);
    observer.complete();
  };
  document.body.appendChild(image);
  image.src = imageUrl;
});

const loadImageAspect = (imageUrl: string): Observable<number> =>
  imageLoader(imageUrl).pipe(map(image => image.width / image.height));

export interface ImageWithAspect {
  url: string;
  aspect: number;
}

export const loadImageWithAspect = (imagePipe: ImagePipe, imageUrl: string): Observable<ImageWithAspect> => {
  if (imageUrl?.length) {
    const hash = hashCode(imageUrl);
    let imageWithAspect = imageAspectMap[hash];
    if (imageWithAspect) {
      return of(imageWithAspect);
    } else {
      return imagePipe.transform(imageUrl, {asString: true, ignoreLoadingImage: true}).pipe(
        switchMap((res) => {
          const url = res as string;
          return loadImageAspect(url).pipe(
            map((aspect) => {
              imageWithAspect = {url, aspect};
              imageAspectMap[hash] = imageWithAspect;
              return imageWithAspect;
            })
          );
        })
      );
    }
  } else {
    return of(null);
  }
};

export type TranslateFunc = (key: string, defaultTranslation?: string) => string;

const linkActionRegex = /<link-act name=['"]([^['"]*)['"]>([^<]*)<\/link-act>/g;
const buttonActionRegex = /<button-act name=['"]([^['"]*)['"]>([^<]*)<\/button-act>/g;

function createLinkElement(actionName: string, actionText: string): string {
  return `<a href="javascript:void(0);" class="tb-custom-action" data-action-name="${actionName}">${actionText}</a>`;
}

function createButtonElement(actionName: string, actionText: string) {
  return `<button mat-button class="tb-custom-action" data-action-name="${actionName}">${actionText}</button>`;
}

function parseTemplate(template: string, data: FormattedData,
                       translateFn?: TranslateFunc) {
  let res = '';
  try {
    if (translateFn) {
      template = translateFn(template);
    }
    template = createLabelFromDatasource(data.$datasource, template);

    let match = /\${([^}]*)}/g.exec(template);
    while (match !== null) {
      const variable = match[0];
      let label = match[1];
      let valDec = 2;
      const splitValues = label.split(':');
      if (splitValues.length > 1) {
        label = splitValues[0];
        valDec = parseFloat(splitValues[1]);
      }

      if (label.startsWith('#')) {
        const keyIndexStr = label.substring(1);
        const n = Math.floor(Number(keyIndexStr));
        if (String(n) === keyIndexStr && n >= 0) {
          label = data.$datasource.dataKeys[n].label;
        }
      }

      const value = data[label] || '';
      let textValue: string;
      if (isNumber(value)) {
        textValue = padValue(value, valDec);
      } else {
        textValue = value;
      }
      template = template.replace(variable, textValue);
      match = /\${([^}]*)}/g.exec(template);
    }

    let actionTags: string;
    let actionText: string;
    let actionName: string;
    let action: string;

    match = linkActionRegex.exec(template);
    while (match !== null) {
      [actionTags, actionName, actionText] = match;
      action = createLinkElement(actionName, actionText);
      template = template.replace(actionTags, action);
      match = linkActionRegex.exec(template);
    }

    match = buttonActionRegex.exec(template);
    while (match !== null) {
      [actionTags, actionName, actionText] = match;
      action = createButtonElement(actionName, actionText);
      template = template.replace(actionTags, action);
      match = buttonActionRegex.exec(template);
    }

    // const compiled = _.template(template);
    // res = compiled(data);
    res = template;
  } catch (ex) {
    console.log(ex, template);
  }
  return res;
}

function prepareProcessPattern(template: string, translateFn?: TranslateFunc): string {
  if (translateFn) {
    template = translateFn(template);
  }
  let actionTags: string;
  let actionText: string;
  let actionName: string;
  let action: string;

  let match = linkActionRegex.exec(template);
  while (match !== null) {
    [actionTags, actionName, actionText] = match;
    action = createLinkElement(actionName, actionText);
    template = template.replace(actionTags, action);
    match = linkActionRegex.exec(template);
  }

  match = buttonActionRegex.exec(template);
  while (match !== null) {
    [actionTags, actionName, actionText] = match;
    action = createButtonElement(actionName, actionText);
    template = template.replace(actionTags, action);
    match = buttonActionRegex.exec(template);
  }
  return template;
}

export const parseWithTranslation = {

  translateFn: null,

  translate(key: string, defaultTranslation?: string): string {
    if (this.translateFn) {
      return this.translateFn(key, defaultTranslation);
    } else {
      console.error('Translate not assigned');
      throw Error('Translate not assigned');
    }
  },
  parseTemplate(template: string, data: FormattedData, forceTranslate = false): string {
    return parseTemplate(forceTranslate ? this.translate(template) : template, data, this.translate.bind(this));
  },
  prepareProcessPattern(template: string, forceTranslate = false): string {
    return prepareProcessPattern(forceTranslate ? this.translate(template) : template, this.translate.bind(this));
  },
  setTranslate(translateFn: TranslateFunc) {
    this.translateFn = translateFn;
  }
};

export function functionValueCalculator<T>(useFunction: boolean, func: CompiledTbFunction<GenericFunction>, params = [], defaultValue: T): T {
  let res: T;
  if (useFunction && isDefinedAndNotNull(func)) {
    try {
      res = func.execute(...params);
      if (!isDefinedAndNotNull(res) || res === '') {
        res = defaultValue;
      }
    } catch (err) {
      res = defaultValue;
      console.log('error in external function:', err);
    }
  } else {
    res = defaultValue;
  }
  return res;
}

export function calculateNewPointCoordinate(coordinate: number, imageSize: number): number {
  let pointCoordinate = coordinate / imageSize;
  if (pointCoordinate < 0) {
    pointCoordinate = 0;
  } else if (pointCoordinate > 1) {
    pointCoordinate = 1;
  }
  return pointCoordinate;
}

export function createLoadingDiv(loadingText: string): JQuery<HTMLElement> {
  return $(`
    <div style="
          z-index: 12;
          position: absolute;
          top: 0;
          bottom: 0;
          left: 0;
          right: 0;
          flex-direction: column;
          align-content: center;
          align-items: center;
          justify-content: center;
          display: flex;
          background: rgba(255,255,255,0.7);
          font-size: 16px;
          font-family: Roboto;
          font-weight: 400;
          text-transform:  uppercase;
        ">
        <span>${loadingText}</span>
    </div>
  `);
}

export function checkLngLat(point: L.LatLng, southWest: L.LatLng, northEast: L.LatLng, offset = 0): L.LatLng {
  const maxLngMap = northEast.lng - offset;
  const minLngMap = southWest.lng + offset;
  const maxLatMap = northEast.lat - offset;
  const minLatMap = southWest.lat + offset;
  if (point.lng > maxLngMap) {
    point.lng = maxLngMap;
  } else if (point.lng < minLngMap) {
    point.lng = minLngMap;
  }
  if (point.lat > maxLatMap) {
    point.lat = maxLatMap;
  } else if (point.lat < minLatMap) {
    point.lat = minLatMap;
  }
  return point;
}
