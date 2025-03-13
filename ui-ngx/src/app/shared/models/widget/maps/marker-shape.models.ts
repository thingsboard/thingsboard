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
import { Element, G, SVG, Text } from '@svgdotjs/svg.js';
import { guid } from '@core/utils';

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

export enum MarkerIconContainer {
  iconContainer1 = 'iconContainer1',
  iconContainer2 = 'iconContainer2',
  iconContainer3 = 'iconContainer3',
  tripIconContainer1 = 'tripIconContainer1',
  tripIconContainer2 = 'tripIconContainer2',
  tripIconContainer3 = 'tripIconContainer3'
}

const markerShapeMap = new Map<MarkerShape, string>(
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

const markerIconContainerMap = new Map<MarkerIconContainer, string>(
  [
    [MarkerIconContainer.iconContainer1, '/assets/markers/iconContainer1.svg'],
    [MarkerIconContainer.iconContainer2, '/assets/markers/iconContainer2.svg'],
    [MarkerIconContainer.iconContainer3, '/assets/markers/iconContainer3.svg'],
    [MarkerIconContainer.tripIconContainer1, '/assets/markers/tripIconContainer1.svg'],
    [MarkerIconContainer.tripIconContainer2, '/assets/markers/tripIconContainer2.svg'],
    [MarkerIconContainer.tripIconContainer3, '/assets/markers/tripIconContainer3.svg']
  ]
);

interface MarkerIconContainerDefinition {
  iconSize: number;
  iconColor: (color: tinycolor.Instance) => tinycolor.Instance;
  iconAlpha: (color: tinycolor.Instance) => number;
  appendIcon?: (svgElement: SVGElement, iconElement: Element, iconSize: number) => void;
}

const emptyIconContainerDefinition: MarkerIconContainerDefinition = {
  iconSize: 24,
  iconColor: (color) => color,
  iconAlpha: color => color.getAlpha()
}

const defaultIconContainerDefinition: MarkerIconContainerDefinition = {
  iconSize: 12,
  iconColor: (color) => tinycolor.mix(color.clone().setAlpha(1), tinycolor('rgba(0,0,0,0.38)')),
  iconAlpha: color => color.getAlpha()
}

const defaultTripIconContainerDefinition: MarkerIconContainerDefinition = {
  iconSize: 24,
  iconColor: () => tinycolor('#000'),
  iconAlpha: () => 1,
  appendIcon: (svgElement, iconElement, iconSize) => {
    const box = iconElement.bbox();
    const cx =  iconSize/2 + box.x;
    const cy =  iconSize/2 + box.y;
    let elements = svgElement.getElementsByClassName('icon-mask-exclude');
    if (elements.length) {
      elements = elements[0].getElementsByClassName('marker-icon-container');
      if (elements.length) {
        const iconContainer = new G(elements[0] as SVGGElement);
        iconContainer.add(iconElement.clone().fill('#000').translate(-cx, -cy));
      }
    }
    elements = svgElement.getElementsByClassName('icon-mask-overlay');
    if (elements.length) {
      elements = elements[0].getElementsByClassName('marker-icon-container');
      if (elements.length) {
        const iconContainer = new G(elements[0] as SVGGElement);
        iconContainer.add(iconElement.clone().fill('#fff').translate(-cx, -cy));
      }
    }
  }
}

const markerIconContainerDefinitionMap = new Map<MarkerIconContainer, MarkerIconContainerDefinition>(
  [
    [MarkerIconContainer.iconContainer1, defaultIconContainerDefinition],
    [MarkerIconContainer.iconContainer2, defaultIconContainerDefinition],
    [MarkerIconContainer.iconContainer3, defaultIconContainerDefinition],
    [MarkerIconContainer.tripIconContainer1, defaultTripIconContainerDefinition],
    [MarkerIconContainer.tripIconContainer2, {...defaultTripIconContainerDefinition, iconSize: 16}],
    [MarkerIconContainer.tripIconContainer3, {...defaultTripIconContainerDefinition, iconSize: 16}]
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

export const markerIconContainers = [
  MarkerIconContainer.iconContainer1,
  MarkerIconContainer.iconContainer2,
  MarkerIconContainer.iconContainer3
];

export const tripMarkerIconContainers = [
  MarkerIconContainer.tripIconContainer1,
  MarkerIconContainer.tripIconContainer2,
  MarkerIconContainer.tripIconContainer3
];

const generateElementId = () => {
  const id = guid();
  const firstChar = id.charAt(0);
  if (firstChar >= '0' && firstChar <= '9') {
    return 'a' + id;
  } else {
    return id;
  }
};

const prepareSvgIds = (element: SVGElement): SVGElement => {
  let svgContent = element.outerHTML;
  const regexp = /\sid="([^"]*)"[\s>]/g;
  const unique_id_suffix = '_' + generateElementId();
  const ids: string[] = [];
  let match = regexp.exec(svgContent);
  while (match !== null) {
    ids.push(match[1]);
    match = regexp.exec(svgContent);
  }
  for (const id of ids) {
    const newId = id + unique_id_suffix;
    svgContent = svgContent.replace(new RegExp('id="'+id+'"', 'g'), 'id="'+newId+'"');
    svgContent = svgContent.replace(new RegExp('url\\(#'+id+'\\)', 'g'), 'url(#'+newId+')');
  }
  return SVG(svgContent).node;
};

const createColorMarkerShape = (iconRegistry: MatIconRegistry, domSanitizer: DomSanitizer, assetUrl: string, color: tinycolor.Instance): Observable<SVGElement> => {
  const safeUrl = domSanitizer.bypassSecurityTrustResourceUrl(assetUrl);
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
      const opacityElements = Array.from(svgElement.getElementsByClassName('marker-opacity'));
      if (svgElement.classList.contains('marker-opacity')) {
        opacityElements.push(svgElement);
      }
      opacityElements.forEach(el => {
        el.setAttribute('opacity', `${color.getAlpha()}`);
      });
      return prepareSvgIds(svgElement);
    })
  );
}


export const createColorMarkerShapeURI = (iconRegistry: MatIconRegistry, domSanitizer: DomSanitizer, shape: MarkerShape, color: tinycolor.Instance): Observable<string> => {
  const assetUrl = markerShapeMap.get(shape);
  return createColorMarkerShape(iconRegistry, domSanitizer, assetUrl, color).pipe(
    map((svgElement) => {
      const svg = svgElement.outerHTML;
      return 'data:image/svg+xml;base64,' + btoa(svg);
    })
  );
}

const createIconElement = (iconRegistry: MatIconRegistry, icon: string, size: number, iconColor: tinycolor.Instance, iconAlpha: number): Observable<Element> => {
  const isSvg = isSvgIcon(icon);
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

export const createColorMarkerIconElement = (iconRegistry: MatIconRegistry, domSanitizer: DomSanitizer,
                                             iconContainer: MarkerIconContainer, icon: string, color: tinycolor.Instance): Observable<SVGElement> => {
  const markerShape$: Observable<SVGElement> = iconContainer ?
    createColorMarkerShape(iconRegistry, domSanitizer, markerIconContainerMap.get(iconContainer), color) : of(null);
  return markerShape$.pipe(
    switchMap((svgElement) => {
      const definition = iconContainer ? markerIconContainerDefinitionMap.get(iconContainer) : emptyIconContainerDefinition;
      const iconSize = definition.iconSize;
      const iconColor = definition.iconColor(color);
      const iconAlpha = definition.iconAlpha(color);
      return createIconElement(iconRegistry, icon, iconSize, iconColor, iconAlpha).pipe(
        map((iconElement) => {
          if (svgElement) {
            if (iconElement) {
              if (definition.appendIcon) {
                definition.appendIcon(svgElement, iconElement, iconSize);
              } else {
                const elements = svgElement.getElementsByClassName('marker-icon-container');
                if (elements.length) {
                  const iconContainer = new G(elements[0] as SVGGElement);
                  iconContainer.add(iconElement);
                  const box = iconElement.bbox();
                  iconElement.translate(-(iconSize/2 + box.x), -(iconSize/2 + box.y));
                }
              }
            }
            return svgElement;
          } else {
            const svg = SVG();
            svg.viewbox(0,0,iconSize,iconSize);
            const iconContainer = new G();
            iconContainer.translate(iconSize/2,iconSize/2);
            iconContainer.add(iconElement);
            const box = iconElement.bbox();
            iconElement.translate(-(iconSize/2 + box.x), -(iconSize/2 + box.y));
            svg.add(iconContainer);
            return svg.node;
          }
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
