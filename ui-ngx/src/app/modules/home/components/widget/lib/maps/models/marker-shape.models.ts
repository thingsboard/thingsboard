///
/// Copyright © 2016-2024 The Thingsboard Authors
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
import { Observable, of, switchMap } from 'rxjs';
import { catchError, map, take } from 'rxjs/operators';
import { isSvgIcon, splitIconName } from '@shared/models/icon.models';
import { Element, Text, G } from '@svgdotjs/svg.js';

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
  markerShape10 = 'markerShape10'
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
    [MarkerShape.markerShape10, '/assets/markers/shape10.svg']
  ]
);

const createColorMarkerShape = (iconRegistry: MatIconRegistry, domSanitizer: DomSanitizer, shape: MarkerShape, color: tinycolor.Instance): Observable<SVGElement> => {
  const markerAssetUrl = markerShapeMap.get(shape);
  const safeUrl = domSanitizer.bypassSecurityTrustResourceUrl(markerAssetUrl);
  return iconRegistry.getSvgIconFromUrl(safeUrl).pipe(
    map((svgElement) => {
      const colorElements = Array.from(svgElement.getElementsByClassName('marker-color'));
      colorElements.forEach(el => {
        el.setAttribute('fill', '#'+color.toHex());
        el.setAttribute('fill-opacity', `${color.getAlpha()}`);
      });
      const strokeElements = Array.from(svgElement.getElementsByClassName('marker-stroke'));
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

const createIconElement = (iconRegistry: MatIconRegistry, icon: string, size: number, color: tinycolor.Instance): Observable<Element> => {
  const isSvg = isSvgIcon(icon);
  const iconAlpha = color.getAlpha();
  const iconColor = tinycolor.mix(color.clone().setAlpha(1), tinycolor('rgba(0,0,0,0.38)'));
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

export const createColorMarkerIconElement = (iconRegistry: MatIconRegistry, domSanitizer: DomSanitizer, icon: string, color: tinycolor.Instance): Observable<SVGElement> => {
  return createColorMarkerShape(iconRegistry, domSanitizer, MarkerShape.markerShape6, color).pipe(
    switchMap((svgElement) => {
      return createIconElement(iconRegistry, icon, 12, color).pipe(
        map((iconElement) => {
          let elements = svgElement.getElementsByClassName('marker-icon-container');
          if (iconElement && elements.length) {
            const iconContainer = new G(elements[0] as SVGGElement);
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

