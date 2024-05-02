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

import { ValueType } from '@shared/models/constants';
import { Box, Element, Runner, Svg, SVG, Timeline } from '@svgdotjs/svg.js';
import { DataToValueType, GetValueAction, GetValueSettings } from '@shared/models/action-widget-settings.models';
import { insertVariable, isDefinedAndNotNull, isNumber, isNumeric, isUndefinedOrNull, mergeDeep } from '@core/utils';
import { BehaviorSubject, forkJoin, Observable } from 'rxjs';
import { map, share } from 'rxjs/operators';
import { ValueAction, ValueGetter } from '@home/components/widget/lib/action/action-widget.models';
import { WidgetContext } from '@home/models/widget-component.models';

export type ValueMatcherType = 'any' | 'constant' | 'range';

export interface ValueMatcher {
  type: ValueMatcherType;
  value?: any;
  range?: {from: number; to: number};
}

export type ScadaObjectAttributeValueType = 'input' | 'property';

export interface ScadaObjectAttributeValue {
  type: ScadaObjectAttributeValueType;
  propertyId?: string;
}

export interface ScadaObjectAttributeState {
  name: string;
  value: ScadaObjectAttributeValue;
}

export interface ScadaObjectState {
  tag: string;
  attributes: ScadaObjectAttributeState[];
  animate?: number;
}

export interface ScadaObjectUpdateState {
  matcher: ValueMatcher;
  state: ScadaObjectState[];
}

export enum ScadaObjectBehaviorType {
  setValue = 'setValue',
  getValue = 'getValue'
}

export interface ScadaObjectBehaviorBase {
  id: string;
  name: string;
  type: ScadaObjectBehaviorType;
}

export interface ScadaObjectBehaviorGet extends ScadaObjectBehaviorBase {
  valueType: ValueType;
  defaultValue: any;
  onUpdate: ScadaObjectUpdateState[];
}

export interface ScadaObjectBehaviorSet extends ScadaObjectBehaviorBase {
  todo: any;
}

export type ScadaObjectBehavior = ScadaObjectBehaviorGet | ScadaObjectBehaviorSet;

export type ScadaObjectPropertyType = 'string' | 'number' | 'color';

export interface ScadaObjectPropertyBase {
  id: string;
  name: string;
  type: ScadaObjectPropertyType;
  default: any;
}

export interface ScadaObjectNumberProperty extends ScadaObjectPropertyBase {
  min?: number;
  max?: number;
}

export type ScadaObjectProperty = ScadaObjectPropertyBase & ScadaObjectNumberProperty;

export interface ScadaObjectMetadata {
  title: string;
  initial: ScadaObjectState[];
  behavior: ScadaObjectBehavior[];
  properties: ScadaObjectProperty[];
}

export const emptyMetadata: ScadaObjectMetadata = {
  title: '',
  initial: [],
  behavior: [],
  properties: []
};


export const parseScadaObjectMetadataFromContent = (svgContent: string): ScadaObjectMetadata => {
  try {
    const svgDoc = new DOMParser().parseFromString(svgContent, 'image/svg+xml');
    return parseScadaObjectMetadataFromDom(svgDoc);
  } catch (_e) {
    return emptyMetadata;
  }
};

const parseScadaObjectMetadataFromDom = (svgDoc: Document): ScadaObjectMetadata => {
  try {
    const elements = svgDoc.getElementsByTagName('tb:metadata');
    if (elements.length) {
      return JSON.parse(elements[0].innerHTML);
    } else {
      return emptyMetadata;
    }
  } catch (_e) {
    return emptyMetadata;
  }
};

const defaultGetValueSettings = (get: ScadaObjectBehaviorGet): GetValueSettings<any> => ({
    action: GetValueAction.DO_NOTHING,
    defaultValue: get.defaultValue,
    executeRpc: {
    method: 'getState',
      requestTimeout: 5000,
      requestPersistent: false,
      persistentPollingInterval: 1000
  },
    getAttribute: {
      key: 'state',
        scope: null
    },
    getTimeSeries: {
      key: 'state'
    },
    dataToValue: {
      type: DataToValueType.NONE,
        compareToValue: true,
        dataToValueFunction: '/* Should return boolean value */\nreturn data;'
    }
  });

export const defaultScadaObjectSettings = (metadata: ScadaObjectMetadata): ScadaObjectSettings => {
  const settings: ScadaObjectSettings = {};
  for (const behaviour of metadata.behavior) {
    //behaviour.id
    if (behaviour.type === ScadaObjectBehaviorType.getValue) {
      settings[behaviour.id] = defaultGetValueSettings(behaviour as ScadaObjectBehaviorGet);
    } else if (behaviour.type === ScadaObjectBehaviorType.setValue) {
      // TODO:
    }
  }
  for (const property of metadata.properties) {
    settings[property.id] = property.default;
  }
  return settings;
};

export type ScadaObjectSettings = {[id: string]: any};

export class ScadaObject {

  private metadata: ScadaObjectMetadata;
  private settings: ScadaObjectSettings;

  private rootElement: HTMLElement;
  private svgShape: Svg;
  private box: Box;
  private targetWidth: number;
  private targetHeight: number;

  private loadingSubject = new BehaviorSubject(false);
  private valueGetters: ValueGetter<any>[] = [];
  private valueActions: ValueAction[] = [];

  private animationTimeline: Timeline;

  loading$ = this.loadingSubject.asObservable().pipe(share());

  constructor(private ctx: WidgetContext,
              private svgPath: string,
              private inputSettings: ScadaObjectSettings) {}

  public init(): Observable<any> {
    return this.ctx.http.get(this.svgPath, {responseType: 'text'}).pipe(
      map((inputSvgContent) => {
        const doc: XMLDocument = new DOMParser().parseFromString(inputSvgContent, 'image/svg+xml');
        this.metadata = parseScadaObjectMetadataFromDom(doc);
        const defaults = defaultScadaObjectSettings(this.metadata);
        this.settings = mergeDeep<ScadaObjectSettings>({}, defaults, this.inputSettings || {});
        this.prepareSvgShape(doc);
        this.prepareStates();
      })
    );
  }

  public addTo(element: HTMLElement) {
    this.rootElement = element;
    if (this.svgShape) {
      this.svgShape.addTo(element);
    }
  }

  public destroy() {
    this.valueActions.forEach(v => v.destroy());
    this.loadingSubject.complete();
    this.loadingSubject.unsubscribe();
  }

  public setSize(targetWidth: number, targetHeight: number) {
    this.targetWidth = targetWidth;
    this.targetHeight = targetHeight;
    if (this.svgShape) {
      this.resize();
    }
  }

  private prepareSvgShape(doc: XMLDocument) {
    const elements = doc.getElementsByTagName('tb:metadata');
    for (let i=0;i<elements.length;i++) {
      elements.item(i).remove();
    }
    let svgContent = doc.documentElement.innerHTML;
    for (const property of this.metadata.properties) {
      const value = this.settings[property.id] || '';
      svgContent = insertVariable(svgContent, property.id, value);
    }
    this.svgShape = SVG().svg(svgContent);
    this.svgShape.node.style.overflow = 'visible';
    this.svgShape.node.style['user-select'] = 'none';
    this.box = this.svgShape.bbox();
    this.svgShape.size(this.box.width, this.box.height);
    if (this.rootElement) {
      this.svgShape.addTo(this.rootElement);
    }
    if (this.targetWidth && this.targetHeight) {
      this.resize();
    }
  }

  private prepareStates() {
    for (const behavior of this.metadata.behavior) {
      if (behavior.type === ScadaObjectBehaviorType.getValue) {
        const getBehavior = behavior as ScadaObjectBehaviorGet;
        let getValueSettings: GetValueSettings<any> = this.settings[getBehavior.id];
        getValueSettings = {...getValueSettings, actionLabel: getBehavior.name};
        const valueGetter =
          ValueGetter.fromSettings(this.ctx, getValueSettings, getBehavior.valueType, {
            next: (val) => {this.onValue(getBehavior.id, val);},
            error: (e) => {}
          });
        this.valueGetters.push(valueGetter);
        this.valueActions.push(valueGetter);
      }
    }
    if (this.metadata.initial) {
      this.updateState(this.metadata.initial);
    }
    if (this.valueGetters.length) {
      const getValueObservables: Array<Observable<any>> = [];
      this.valueGetters.forEach(valueGetter => {
        getValueObservables.push(valueGetter.getValue());
      });
      this.loadingSubject.next(true);
      forkJoin(getValueObservables).subscribe(
        {
          next: () => {
            this.loadingSubject.next(false);
          },
          error: () => {
            this.loadingSubject.next(false);
          }
        }
      );
    }
  }

  private resize() {
    let scale: number;
    if (this.targetWidth < this.targetHeight) {
      scale = this.targetWidth / this.box.width;
    } else {
      scale = this.targetHeight / this.box.height;
    }
    this.svgShape.node.style.transform = `scale(${scale})`;
  }

  private onValue(id: string, value: any) {
    const getBehavior = this.metadata.behavior.find(b => b.id === id) as ScadaObjectBehaviorGet;
    value = this.normalizeValue(value, getBehavior.valueType);
    const updateStates = this.filterUpdateStates(getBehavior.onUpdate, value);
    if (this.animationTimeline) {
      this.animationTimeline.finish();
    }
    for (const updateState of updateStates) {
      this.updateState(updateState.state, value);
    }
  }

  private updateState(state: ScadaObjectState[], value?: any) {
    for (const stateEntry of state) {
      const tag = stateEntry.tag;
      const elements = this.svgShape.find(`[tb\\:tag="${tag}"]`);
      const attrs = this.computeAttributes(stateEntry.attributes, value);
      elements.forEach(e => {
        this.setElementAttributes(e, attrs, stateEntry.animate);
      });
    }
  }

  private normalizeValue(value: any, type: ValueType): any {
    if (isUndefinedOrNull(value)) {
      switch (type) {
        case ValueType.STRING:
          return '';
        case ValueType.INTEGER:
        case ValueType.DOUBLE:
          return 0;
        case ValueType.BOOLEAN:
          return false;
        case ValueType.JSON:
          return {};
      }
    } else {
      return value;
    }
  }

  private computeAttributes(attributes: ScadaObjectAttributeState[], value: any): {[attr: string]: any} {
    const res: {[attr: string]: any} = {};
    for (const attribute of attributes) {
      const attr = attribute.name;
      res[attr] = this.getAttributeValue(attribute, value);
    }
    return res;
  }

  private setElementAttributes(element: Element, attrs: {[attr: string]: any}, animate?: number) {
    if (isDefinedAndNotNull(animate)) {
      this.animation(element, animate).attr(attrs);
    } else {
      element.attr(attrs);
    }
  }

  private animation(element: Element, duration: number): Runner {
    if (!this.animationTimeline) {
      this.animationTimeline = new Timeline();
    }
    element.timeline(this.animationTimeline);
    return element.animate(duration, 0, 'now');
  }

  private getAttributeValue(attribute: ScadaObjectAttributeState, value: any): any {
    if (attribute.value.type === 'input') {
      return value;
    } else if (attribute.value.type === 'property') {
      const id = attribute.value.propertyId;
      return this.settings[id] || '';
    } else {
      return '';
    }
  }

  private filterUpdateStates(states: ScadaObjectUpdateState[], val: any): ScadaObjectUpdateState[] {
    return states.filter(s => this.valueMatches(s.matcher, val));
  }

  private valueMatches(matcher: ValueMatcher, val: any): boolean {
    switch (matcher.type) {
      case 'any':
        return true;
      case 'constant':
        return matcher.value === val;
      case 'range':
        if (isDefinedAndNotNull(val) && isNumeric(val)) {
          const num = Number(val);
          const range = matcher.range;
          return ((!isNumber(range.from) || num >= range.from) && (!isNumber(range.to) || num < range.to));
        } else {
          return false;
        }
    }
  }
}
