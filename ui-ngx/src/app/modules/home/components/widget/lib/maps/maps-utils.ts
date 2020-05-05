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
import { FormattedData, MarkerSettings, PolygonSettings, PolylineSettings } from './map-models';
import { Datasource } from '@app/shared/models/widget.models';
import _ from 'lodash';
import { Observable, Observer, of } from 'rxjs';
import { map } from 'rxjs/operators';
import { createLabelFromDatasource, hashCode, padValue } from '@core/utils';

export function createTooltip(target: L.Layer,
    settings: MarkerSettings | PolylineSettings | PolygonSettings,
    datasource: Datasource,
    content?: string | HTMLElement
): L.Popup {
    const popup = L.popup();
    popup.setContent(content);
    target.bindPopup(popup, { autoClose: settings.autocloseTooltip, closeOnClick: false });
    if (settings.showTooltipAction === 'hover') {
        target.off('click');
        target.on('mouseover', () => {
            target.openPopup();
        });
        target.on('mouseout', () => {
            target.closePopup();
        });
    }
    target.on('popupopen', () => {
        const actions = document.getElementsByClassName('tb-custom-action');
        Array.from(actions).forEach(
            (element: HTMLElement) => {
                if (element && settings.tooltipAction[element.id]) {
                    element.addEventListener('click', ($event) => settings.tooltipAction[element.id]($event, datasource));
                }
            });
    });
    return popup;
}

export function getRatio(firsMoment: number, secondMoment: number, intermediateMoment: number): number {
    return (intermediateMoment - firsMoment) / (secondMoment - firsMoment);
}

export function findAngle(startPoint, endPoint) {
    let angle = -Math.atan2(endPoint.latitude - startPoint.latitude, endPoint.longitude - startPoint.longitude);
    angle = angle * 180 / Math.PI;
    return parseInt(angle.toFixed(2), 10);
}


export function getDefCenterPosition(position) {
    if (typeof (position) === 'string')
        return position.split(',');
    if (typeof (position) === 'object')
        return position;
    return [0, 0];
}


const imageAspectMap = {};

function imageLoader(imageUrl: string): Observable<HTMLImageElement> {
  return new Observable((observer: Observer<HTMLImageElement>) => {
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
    document.body.appendChild(image)
    image.src = imageUrl;
  });
}

export function aspectCache(imageUrl: string): Observable<number> {
  if (imageUrl?.length) {
    const hash = hashCode(imageUrl);
    let aspect = imageAspectMap[hash];
    if (aspect) {
      return of(aspect);
    }
    else return imageLoader(imageUrl).pipe(map(image => {
      aspect = image.width / image.height;
      imageAspectMap[hash] = aspect;
      return aspect;
    }))
  }
}

export type TranslateFunc = (key: string, defaultTranslation?: string) => string;

function parseTemplate(template: string, data: { $datasource?: Datasource, [key: string]: any },
                              translateFn?: TranslateFunc) {
  let res = '';
  try {
    if (template.match(/<link-act/g)) {
      template = template.replace(/<link-act/g, '<a href="#"').replace(/link-act>/g, 'a>')
        .replace(/name=(['"])(.*?)(['"])/g, `class='tb-custom-action' id='$2'`);
    }
    if (translateFn) {
      template = translateFn(template);
    }
    template = createLabelFromDatasource(data.$datasource, template);
    const formatted = template.match(/\${([^}]*):\d*}/g);
    if (formatted)
      formatted.forEach(value => {
        const [variable, digits] = value.replace('${', '').replace('}', '').split(':');
        data[variable] = padValue(data[variable], +digits);
        if (data[variable] === 'NaN') data[variable] = '';
        template = template.replace(value, '${' + variable + '}');
      });
    const variables = template.match(/\${.*?}/g);
    if (variables) {
      variables.forEach(variable => {
        variable = variable.replace('${', '').replace('}', '');
        if (!data[variable])
          data[variable] = '';
      })
    }
    const compiled = _.template(template);
    res = compiled(data);
  }
  catch (ex) {
    console.log(ex, template)
  }
  return res;
}

export const parseWithTranslation = {

  translateFn: null,

  translate(key: string, defaultTranslation?: string): string {
    if (this.translateFn) {
      return this.translateFn(key, defaultTranslation);
    } else {
      throw console.error('Translate not assigned');
    }
  },
  parseTemplate(template: string, data: object, forceTranslate = false): string {
    return parseTemplate(forceTranslate ? this.translate(template) : template, data, this.translate.bind(this));
  },
  setTranslate(translateFn: TranslateFunc) {
    this.translateFn = translateFn;
  }
}

export function parseData(input: any[]): FormattedData[] {
  return _(input).groupBy(el => el?.datasource?.entityName)
    .values().value().map((entityArray, i) => {
      const obj = {
        entityName: entityArray[0]?.datasource?.entityName,
        $datasource: entityArray[0]?.datasource as Datasource,
        dsIndex: i,
        deviceType: null
      };
      entityArray.filter(el => el.data.length).forEach(el => {
        obj[el?.dataKey?.label] = el?.data[0][1];
        obj[el?.dataKey?.label + '|ts'] = el?.data[0][0];
        if (el?.dataKey?.label === 'type') {
          obj.deviceType = el?.data[0][1];
        }
      });
      return obj;
    });
}

export function parseArray(input: any[]): any[] {
  return _(input).groupBy(el => el?.datasource?.entityName)
    .values().value().map((entityArray, dsIndex) =>
      entityArray[0].data.map((el, i) => {
        const obj = {
          entityName: entityArray[0]?.datasource?.entityName,
          $datasource: entityArray[0]?.datasource,
          dsIndex,
          time: el[0],
          deviceType: null
        };
        entityArray.filter(e => e.data.length).forEach(entity => {
          obj[entity?.dataKey?.label] = entity?.data[i][1];
          obj[entity?.dataKey?.label + '|ts'] = entity?.data[0][0];
          if (entity?.dataKey?.label === 'type') {
            obj.deviceType = entity?.data[0][1];
          }
        });
        return obj;
      })
    );
}

export function parseFunction(source: any, params: string[] = ['def']): (...args: any[]) => any {
  let res = null;
  if (source?.length) {
    try {
      res = new Function(...params, source);
    }
    catch (err) {
      res = null;
    }
  }
  return res;
}

export function safeExecute(func: (...args: any[]) => any, params = []) {
  let res = null;
  if (func && typeof (func) === 'function') {
    try {
      res = func(...params);
    }
    catch (err) {
      console.log('error in external function:', err);
      res = null;
    }
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
