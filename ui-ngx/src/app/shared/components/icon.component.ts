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

import { CanColor, mixinColor } from '@angular/material/core';
import {
  AfterContentInit,
  AfterViewChecked,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  ErrorHandler,
  Inject,
  OnDestroy,
  Renderer2,
  ViewChild,
  ViewEncapsulation
} from '@angular/core';
import { MAT_ICON_LOCATION, MatIconLocation, MatIconRegistry } from '@angular/material/icon';
import { Subscription } from 'rxjs';
import { take } from 'rxjs/operators';
import { isSvgIcon, splitIconName } from '@shared/models/icon.models';
import { ContentObserver } from '@angular/cdk/observers';

const _TbIconBase = mixinColor(
  class {
    constructor(public _elementRef: ElementRef) {}
  },
);

const funcIriAttributes = [
  'clip-path',
  'color-profile',
  'src',
  'cursor',
  'fill',
  'filter',
  'marker',
  'marker-start',
  'marker-mid',
  'marker-end',
  'mask',
  'stroke',
];

const funcIriAttributeSelector = funcIriAttributes.map(attr => `[${attr}]`).join(', ');

const funcIriPattern = /^url\(['"]?#(.*?)['"]?\)$/;

@Component({
    template: '<span style="display: none;" #iconNameContent><ng-content></ng-content></span>',
    selector: 'tb-icon',
    exportAs: 'tbIcon',
    styleUrls: [],
    // eslint-disable-next-line @angular-eslint/no-inputs-metadata-property
    inputs: ['color'],
    // eslint-disable-next-line @angular-eslint/no-host-metadata-property
    host: {
        role: 'img',
        class: 'mat-icon notranslate',
        '[attr.data-mat-icon-type]': '!_useSvgIcon ? "font" : "svg"',
        '[attr.data-mat-icon-name]': '_svgName',
        '[attr.data-mat-icon-namespace]': '_svgNamespace',
        '[class.mat-icon-no-color]': 'color !== "primary" && color !== "accent" && color !== "warn"',
    },
    encapsulation: ViewEncapsulation.None,
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class TbIconComponent extends _TbIconBase
                             implements AfterContentInit, AfterViewChecked, CanColor, OnDestroy {

  @ViewChild('iconNameContent', {static: true})
  _iconNameContent: ElementRef;

  private icon: string;

  get viewValue(): string {
    return (this._iconNameContent?.nativeElement.textContent || '').trim();
  }

  private _contentChanges = Subscription.EMPTY;
  private _previousFontSetClass: string[] = [];

  _useSvgIcon = false;
  _svgName: string | null;
  _svgNamespace: string | null;

  private _textElement = null;

  private _previousPath?: string;

  private _elementsWithExternalReferences?: Map<Element, {name: string; value: string}[]>;

  private _currentIconFetch = Subscription.EMPTY;

  constructor(elementRef: ElementRef<HTMLElement>,
              private contentObserver: ContentObserver,
              private renderer: Renderer2,
              private _iconRegistry: MatIconRegistry,
              @Inject(MAT_ICON_LOCATION) private _location: MatIconLocation,
              private readonly _errorHandler: ErrorHandler) {
    super(elementRef);
  }

  ngAfterContentInit(): void {
    this.icon = this.viewValue;
    this._updateIcon();
    this._contentChanges = this.contentObserver.observe(this._iconNameContent.nativeElement)
      .subscribe(() => {
       const content = this.viewValue;
        if (this.icon !== content) {
          this.icon = content;
          this._updateIcon();
        }
      });
  }

  ngAfterViewChecked() {
    const cachedElements = this._elementsWithExternalReferences;
    if (cachedElements && cachedElements.size) {
      const newPath = this._location.getPathname();
      if (newPath !== this._previousPath) {
        this._previousPath = newPath;
        this._prependPathToReferences(newPath);
      }
    }
  }

  ngOnDestroy() {
    this._contentChanges.unsubscribe();
    this._currentIconFetch.unsubscribe();
    if (this._elementsWithExternalReferences) {
      this._elementsWithExternalReferences.clear();
    }
  }

  private _updateIcon() {
    const useSvgIcon = isSvgIcon(this.icon);
    if (this._useSvgIcon !== useSvgIcon) {
      this._useSvgIcon = useSvgIcon;
      if (!this._useSvgIcon) {
        this._updateSvgIcon(undefined);
      } else {
        this._updateFontIcon(undefined);
      }
    }
    if (this._useSvgIcon) {
      this._updateSvgIcon(this.icon);
    } else {
      this._updateFontIcon(this.icon);
    }
  }

  private _updateFontIcon(rawName: string | undefined) {
    if (rawName) {
      this._clearFontIcon();
      const iconName = splitIconName(rawName)[1];
      this._textElement = this.renderer.createText(iconName);
      const elem: HTMLElement = this._elementRef.nativeElement;
      this.renderer.insertBefore(elem, this._textElement, this._iconNameContent.nativeElement);
      const fontSetClasses = (
        this._iconRegistry.getDefaultFontSetClass()
      ).filter(className => className.length > 0);
      fontSetClasses.forEach(className => elem.classList.add(className));
      this._previousFontSetClass = fontSetClasses;
    } else {
      this._clearFontIcon();
    }
  }

  private _clearFontIcon() {
    const elem: HTMLElement = this._elementRef.nativeElement;
    if (this._textElement !== null) {
      this.renderer.removeChild(elem, this._textElement);
      this._textElement = null;
    }
    this._previousFontSetClass.forEach(className => elem.classList.remove(className));
    this._previousFontSetClass = [];
  }

  private _updateSvgIcon(rawName: string | undefined) {
    this._svgNamespace = null;
    this._svgName = null;
    this._currentIconFetch.unsubscribe();

    if (rawName) {
      const [namespace, iconName] = splitIconName(rawName);
      if (namespace) {
        this._svgNamespace = namespace;
      }
      if (iconName) {
        this._svgName = iconName;
      }
      this._iconRegistry.getDefaultFontSetClass();
      this._currentIconFetch = this._iconRegistry
        .getNamedSvgIcon(iconName, namespace)
        .pipe(take(1))
        .subscribe({
          next: (svg) => this._setSvgElement(svg),
          error: (err: Error) => {
            const errorMessage = `Error retrieving icon ${namespace}:${iconName}! ${err.message}`;
            this._errorHandler.handleError(new Error(errorMessage));
          }
        });
    } else {
      this._clearSvgElement();
    }
  }

  private _setSvgElement(svg: SVGElement) {
    this._clearSvgElement();
    const path = this._location.getPathname();
    this._previousPath = path;
    this._cacheChildrenWithExternalReferences(svg);
    this._prependPathToReferences(path);
    this.renderer.insertBefore(this._elementRef.nativeElement, svg, this._iconNameContent.nativeElement);
  }

  private _clearSvgElement() {
    const layoutElement: HTMLElement = this._elementRef.nativeElement;
    let childCount = layoutElement.childNodes.length;
    if (this._elementsWithExternalReferences) {
      this._elementsWithExternalReferences.clear();
    }
    while (childCount--) {
      const child = layoutElement.childNodes[childCount];
      if (child.nodeType !== 1 || child.nodeName.toLowerCase() === 'svg') {
        child.remove();
      }
    }
  }

  private _cacheChildrenWithExternalReferences(element: SVGElement) {
    const elementsWithFuncIri = element.querySelectorAll(funcIriAttributeSelector);
    const elements = (this._elementsWithExternalReferences = this._elementsWithExternalReferences || new Map());
    elementsWithFuncIri.forEach(
      (elementWithFuncIri) => {
        funcIriAttributes.forEach(attr => {
          const elementWithReference = elementWithFuncIri;
          const value = elementWithReference.getAttribute(attr);
          const match = value ? value.match(funcIriPattern) : null;

          if (match) {
            let attributes = elements.get(elementWithReference);

            if (!attributes) {
              attributes = [];
              elements.set(elementWithReference, attributes);
            }

            attributes.push({name: attr, value: match[1]});
          }
        });
      }
    );
  }

  private _prependPathToReferences(path: string) {
    const elements = this._elementsWithExternalReferences;
    if (elements) {
      elements.forEach((attrs, element) => {
        attrs.forEach(attr => {
          element.setAttribute(attr.name, `url('${path}#${attr.value}')`);
        });
      });
    }
  }

}
