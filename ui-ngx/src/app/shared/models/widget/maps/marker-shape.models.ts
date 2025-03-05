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

import tinycolor from 'tinycolor2';
import { MatIconRegistry } from '@angular/material/icon';
import { DomSanitizer } from '@angular/platform-browser';
import { Observable, of, shareReplay, switchMap } from 'rxjs';
import { catchError, map, take } from 'rxjs/operators';
import { isSvgIcon, splitIconName } from '@shared/models/icon.models';
import { Element, G, Text } from '@svgdotjs/svg.js';

export enum MarkerShape {
  markerShape1 = 'markerShape1',
  markerShape2 = 'markerShape2',
  markerShape3 = 'markerShape3',
  markerShape4 = 'markerShape4',
  markerShape5 = 'markerShape5',
  markerShape6 = 'markerShape6',
  markerShape7 = 'markerShape7',
  markerShape8 = 'markerShape8',
  markerShape9 = 'markerShape9',
  markerShape10 = 'markerShape10',
  tripMarkerShape1 = 'tripMarkerShape1',
  tripMarkerShape2 = 'tripMarkerShape2',
  tripMarkerShape3 = 'tripMarkerShape3',
  tripMarkerShape4 = 'tripMarkerShape4',
  tripMarkerShape5 = 'tripMarkerShape5',
  tripMarkerShape6 = 'tripMarkerShape6',
  tripMarkerShape7 = 'tripMarkerShape7',
  tripMarkerShape8 = 'tripMarkerShape8',
  tripMarkerShape9 = 'tripMarkerShape9',
  tripMarkerShape10 = 'tripMarkerShape10'
}

export const markerShapeMap = new Map<MarkerShape, string>(
  [
    [MarkerShape.markerShape1, '/assets/markers/shape1.svg'],
    [MarkerShape.markerShape2, '/assets/markers/shape2.svg'],
    [MarkerShape.markerShape3, '/assets/markers/shape3.svg'],
    [MarkerShape.markerShape4, '/assets/markers/shape4.svg'],
    [MarkerShape.markerShape5, '/assets/markers/shape5.svg'],
    [MarkerShape.markerShape6, '/assets/markers/shape6.svg'],
    [MarkerShape.markerShape7, '/assets/markers/shape7.svg'],
    [MarkerShape.markerShape8, '/assets/markers/shape8.svg'],
    [MarkerShape.markerShape9, '/assets/markers/shape9.svg'],
    [MarkerShape.markerShape10, '/assets/markers/shape10.svg'],
    [MarkerShape.tripMarkerShape1, '/assets/markers/tripShape1.svg'],
    [MarkerShape.tripMarkerShape2, '/assets/markers/tripShape2.svg'],
    [MarkerShape.tripMarkerShape3, '/assets/markers/tripShape3.svg'],
    [MarkerShape.tripMarkerShape4, '/assets/markers/tripShape4.svg'],
    [MarkerShape.tripMarkerShape5, '/assets/markers/tripShape5.svg'],
    [MarkerShape.tripMarkerShape6, '/assets/markers/tripShape6.svg'],
    [MarkerShape.tripMarkerShape7, '/assets/markers/tripShape7.svg'],
    [MarkerShape.tripMarkerShape8, '/assets/markers/tripShape8.svg'],
    [MarkerShape.tripMarkerShape9, '/assets/markers/tripShape9.svg'],
    [MarkerShape.tripMarkerShape10, '/assets/markers/tripShape10.svg']
  ]
);

export const markerShapes = [
  MarkerShape.markerShape1,
  MarkerShape.markerShape2,
  MarkerShape.markerShape3,
  MarkerShape.markerShape4,
  MarkerShape.markerShape5,
  MarkerShape.markerShape6,
  MarkerShape.markerShape7,
  MarkerShape.markerShape8,
  MarkerShape.markerShape9,
  MarkerShape.markerShape10
];

export const tripMarkerShapes = [
  MarkerShape.tripMarkerShape1,
  MarkerShape.tripMarkerShape2,
  MarkerShape.tripMarkerShape3,
  MarkerShape.tripMarkerShape4,
  MarkerShape.tripMarkerShape5,
  MarkerShape.tripMarkerShape6,
  MarkerShape.tripMarkerShape7,
  MarkerShape.tripMarkerShape8,
  MarkerShape.tripMarkerShape9,
  MarkerShape.tripMarkerShape10
];

const createColorMarkerShape = (iconRegistry: MatIconRegistry, domSanitizer: DomSanitizer, shape: MarkerShape, color: tinycolor.Instance): Observable<SVGElement> => {
  const markerAssetUrl = markerShapeMap.get(shape);
  const safeUrl = domSanitizer.bypassSecurityTrustResourceUrl(markerAssetUrl);
  return iconRegistry.getSvgIconFromUrl(safeUrl).pipe(
    map((svgElement) => {
      const colorElements = Array.from(svgElement.getElementsByClassName('marker-color'));
      if (svgElement.classList.contains('marker-color')) {
        colorElements.push(svgElement);
      }
      colorElements.forEach(el => {
        el.setAttribute('fill', '#'+color.toHex());
        el.setAttribute('fill-opacity', `${color.getAlpha()}`);
      });
      const strokeElements = Array.from(svgElement.getElementsByClassName('marker-stroke'));
      if (svgElement.classList.contains('marker-stroke')) {
        strokeElements.push(svgElement);
      }
      strokeElements.forEach(el => {
        el.setAttribute('stroke', '#'+color.toHex());
        el.setAttribute('stroke-opacity', `${color.getAlpha()}`);
      });
      return svgElement;
    })
  );
}


export const createColorMarkerShapeURI = (iconRegistry: MatIconRegistry, domSanitizer: DomSanitizer, shape: MarkerShape, color: tinycolor.Instance): Observable<string> => {
  return createColorMarkerShape(iconRegistry, domSanitizer, shape, color).pipe(
    map((svgElement) => {
      const svg = svgElement.outerHTML;
      return 'data:image/svg+xml;base64,' + btoa(svg);
    })
  );
}

const createIconElement = (iconRegistry: MatIconRegistry, icon: string, size: number, color: tinycolor.Instance, trip = false): Observable<Element> => {
  const isSvg = isSvgIcon(icon);
  const iconAlpha = color.getAlpha();
  const iconColor = trip ? color : tinycolor.mix(color.clone().setAlpha(1), tinycolor('rgba(0,0,0,0.38)'));
  if (isSvg) {
    const [namespace, iconName] = splitIconName(icon);
    return iconRegistry
    .getNamedSvgIcon(iconName, namespace)
    .pipe(
      take(1),
      map((svgElement) => {
        const element = new Element(svgElement.firstChild);
        element.fill('#'+iconColor.toHex());
        element.attr('fill-opacity', iconAlpha);
        const scale = size / 24;
        element.scale(scale);
        return element;
      }),
      catchError(() => of(null))
    );
  } else {
    const iconName = splitIconName(icon)[1];
    const textElement = new Text(document.createElementNS('http://www.w3.org/2000/svg', 'text'));
    const fontSetClasses = (
      iconRegistry.getDefaultFontSetClass()
    ).filter(className => className.length > 0);
    fontSetClasses.forEach(className => textElement.addClass(className));
    textElement.font({size: `${size}px`});
    textElement.attr({
      style: `font-size: ${size}px`,
      'text-anchor': 'start'
    });
    textElement.fill('#'+iconColor.toHex());
    textElement.attr('fill-opacity', iconAlpha);
    const tspan = textElement.tspan(iconName);
    tspan.attr({
      'dominant-baseline': 'hanging'
    });
    return of(textElement);
  }
}

const markerIconShape = MarkerShape.markerShape6;
const tripMarkerIconShape = MarkerShape.tripMarkerShape2;

export const createColorMarkerIconElement = (iconRegistry: MatIconRegistry, domSanitizer: DomSanitizer, icon: string, color: tinycolor.Instance,
                                             trip = false): Observable<SVGElement> => {
  return createColorMarkerShape(iconRegistry, domSanitizer, trip ? tripMarkerIconShape : markerIconShape, color).pipe(
    switchMap((svgElement) => {
      return createIconElement(iconRegistry, icon, trip ? 24 : 12, trip ? tinycolor('#fff') : color, trip).pipe(
        map((iconElement) => {
          let elements = svgElement.getElementsByClassName('marker-icon-container');
          if (iconElement && elements.length) {
            const iconContainer = new G(elements[0] as SVGGElement);
            iconContainer.clear();
            iconContainer.add(iconElement);
            const box = iconElement.bbox();
            iconElement.translate(-box.cx, -box.cy);
          }
          elements = svgElement.getElementsByClassName('marker-icon-background');
          if (elements.length) {
            (elements[0] as SVGGElement).style.display = '';
            (elements[0] as SVGGElement).setAttribute('fill-opacity', `${color.getAlpha()}`);
          }
          return svgElement;
        })
      );
    })
  );
}

let placeItemIconURI$: Observable<string>;

export const createPlaceItemIcon = (iconRegistry: MatIconRegistry, domSanitizer: DomSanitizer): Observable<string> => {
  if (placeItemIconURI$) {
    return placeItemIconURI$;
  }
  placeItemIconURI$ = createColorMarkerShapeURI(iconRegistry, domSanitizer, MarkerShape.markerShape1, tinycolor('rgba(255,255,255,0.75)')).pipe(
    shareReplay({refCount: true, bufferSize: 1})
  );
  return placeItemIconURI$;
}
