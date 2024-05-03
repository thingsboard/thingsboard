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
import { Box, Element, Runner, Svg, SVG, Timeline, Text } from '@svgdotjs/svg.js';
import { DataToValueType, GetValueAction, GetValueSettings } from '@shared/models/action-widget-settings.models';
import {
  formatValue,
  insertVariable,
  isDefinedAndNotNull,
  isNumber,
  isNumeric,
  isUndefinedOrNull,
  mergeDeep,
  parseFunction
} from '@core/utils';
import { BehaviorSubject, forkJoin, Observable } from 'rxjs';
import { map, share } from 'rxjs/operators';
import { ValueAction, ValueGetter } from '@home/components/widget/lib/action/action-widget.models';
import { WidgetContext } from '@home/models/widget-component.models';
import { Font } from '@shared/models/widget-settings.models';

export type ValueMatcherType = 'any' | 'constant' | 'range';

export interface ValueMatcher {
  type: ValueMatcherType;
  value?: any;
  range?: {from: number; to: number};
}

const valueMatches = (matcher: ValueMatcher, val: any): boolean => {
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
};

export type ScadaObjectValueType = 'input' | 'constant' | 'property' | 'function' | 'valueFormat';

export interface ScadaObjectValueBase {
  type: ScadaObjectValueType;
}

export interface ScadaObjectValueConstant extends ScadaObjectValueBase {
  constantValue?: any;
}

export interface ScadaObjectValueProperty extends ScadaObjectValueBase {
  propertyId?: string;
  computedPropertyValue?: any;
}

export interface ScadaObjectValueFunction extends ScadaObjectValueBase {
  valueConvertFunction?: string;
  valueConverter?: (val: any) => any;
}

export interface ScadaObjectValueFormat extends ScadaObjectValueBase {
  units?: ScadaObjectValue;
  decimals?: ScadaObjectValue;
  computedUnits?: string;
  computedDecimals?: number;
}

export type ScadaObjectValue = ScadaObjectValueProperty & ScadaObjectValueConstant & ScadaObjectValueFunction & ScadaObjectValueFormat;

export interface ScadaObjectAttribute {
  name: string;
  value: ScadaObjectValue;
}

export interface ScadaObjectText {
  content?: ScadaObjectValue;
  font?: ScadaObjectValue;
  color?: ScadaObjectValue;
}

export interface ScadaObjectElementState {
  tag: string;
  show?: ScadaObjectValue;
  text?: ScadaObjectText;
  attributes?: ScadaObjectAttribute[];
  animate?: number;
}

export interface ScadaObjectState {
  initial?: boolean;
  value?: any;
  state: ScadaObjectElementState[];
}

export interface ScadaObjectUpdateState {
  matcher: ValueMatcher;
  stateId: string;
}

const filterUpdateStates = (states: ScadaObjectUpdateState[], val: any): ScadaObjectUpdateState[] =>
  states.filter(s => valueMatches(s.matcher, val));

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

export type ScadaObjectPropertyType = 'string' | 'number' | 'color' | 'font' | 'units' | 'switch';

export interface ScadaObjectPropertyBase {
  id: string;
  name: string;
  type: ScadaObjectPropertyType;
  default: any;
  required?: boolean;
  subLabel?: string;
  divider?: boolean;
  fieldSuffix?: string;
  disableOnProperty?: string;
}

export interface ScadaObjectNumberProperty extends ScadaObjectPropertyBase {
  min?: number;
  max?: number;
  step?: number;
}

export type ScadaObjectProperty = ScadaObjectPropertyBase & ScadaObjectNumberProperty;

export interface ScadaObjectMetadata {
  title: string;
  states: {[id: string]: ScadaObjectState};
  behavior: ScadaObjectBehavior[];
  properties: ScadaObjectProperty[];
}

export const emptyMetadata: ScadaObjectMetadata = {
  title: '',
  states: {},
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
    console.error(_e);
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
        this.prepareMetadata();
        this.prepareSvgShape(doc);
        this.initStates();
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

  private prepareMetadata() {
    for (const stateId of Object.keys(this.metadata.states)) {
      const state = this.metadata.states[stateId];
      for (const elementState of state.state) {
        this.prepareValue(elementState.show);
        this.prepareValue(elementState.text?.content);
        this.prepareValue(elementState.text?.font);
        this.prepareValue(elementState.text?.color);
        if (elementState.attributes) {
          for (const attribute of elementState.attributes) {
            this.prepareValue(attribute.value);
          }
        }
      }
    }
  }

  private prepareValue(value: ScadaObjectValue) {
    if (value) {
      if (value.type === 'function' && value.valueConvertFunction) {
        try {
          value.valueConverter = parseFunction(this.insertVariables(value.valueConvertFunction), ['value']);
        } catch (e) {
          value.valueConverter = (v) => v;
        }
      } else if (value.type === 'property') {
        value.computedPropertyValue = this.getPropertyValue(value.propertyId);
      } else if (value.type === 'valueFormat') {
        if (value.units) {
          this.prepareValue(value.units);
        }
        if (value.decimals) {
          this.prepareValue(value.decimals);
        }
      }
    }
  }

  private insertVariables(content: string): string {
    for (const property of this.metadata.properties) {
      const value = this.getPropertyValue(property.id);
      content = insertVariable(content, property.id, value);
    }
    return content;
  }

  private prepareSvgShape(doc: XMLDocument) {
    const elements = doc.getElementsByTagName('tb:metadata');
    for (let i=0;i<elements.length;i++) {
      elements.item(i).remove();
    }
    this.svgShape = SVG().svg(doc.documentElement.innerHTML);
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

  private initStates() {
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
    const initialState = Object.values(this.metadata.states).find(s => s.initial);
    if (initialState) {
      this.updateState(initialState);
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
    const updateStates = filterUpdateStates(getBehavior.onUpdate, value);
    if (this.animationTimeline) {
      this.animationTimeline.finish();
    }
    for (const updateState of updateStates) {
      const state = this.metadata.states[updateState.stateId];
      this.updateState(state, value);
    }
  }

  private updateState(state: ScadaObjectState, value?: any) {
    if (state) {
      for (const elementState of state.state) {
        const tag = elementState.tag;
        const elements = this.svgShape.find(`[tb\\:tag="${tag}"]`);
        if (elements.length) {
          if (elementState.show) {
            const show: boolean = this.computeValue(elementState.show, value);
            elements.forEach(e => {
              if (show) {
                e.show();
              } else {
                e.hide();
              }
            });
          }
          if (elementState.attributes) {
            const attrs = this.computeAttributes(elementState.attributes, value);
            elements.forEach(e => {
              this.setElementAttributes(e, attrs, elementState.animate);
            });
          }
          if (elementState.text) {
            if (elementState.text.content) {
              const text: string = this.computeValue(elementState.text.content, value);
              elements.forEach(e => {
                this.setElementText(e, text);
              });
            }
            if (elementState.text.font || elementState.text.color) {
              let font: Font = this.computeValue(elementState.text.font, value);
              if (typeof font !== 'object') {
                font = undefined;
              }
              let color: string = this.computeValue(elementState.text.color, value);
              if (typeof color !== 'string') {
                color = undefined;
              }
              elements.forEach(e => {
                this.setElementFont(e, font, color);
              });
            }
          }
        }
      }
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

  private computeAttributes(attributes: ScadaObjectAttribute[], value: any): {[attr: string]: any} {
    const res: {[attr: string]: any} = {};
    for (const attribute of attributes) {
      const attr = attribute.name;
      res[attr] = this.computeValue(attribute.value, value);
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

  private setElementText(element: Element, text: string) {
    let textElement: Text;
    if (element.type === 'text') {
      const children = element.children();
      if (children.length && children[0].type === 'tspan') {
        textElement = children[0] as Text;
      } else {
        textElement = element as Text;
      }
    } else if (element.type === 'tspan') {
      textElement = element as Text;
    }
    if (textElement) {
      textElement.text(text);
    }
  }

  private setElementFont(element: Element, font: Font, color: string) {
    if (element.type === 'text') {
      const textElement = element as Text;
      if (font) {
        textElement.font({
          family: font.family,
          size: (isDefinedAndNotNull(font.size) && isDefinedAndNotNull(font.sizeUnit)) ?
            font.size + font.sizeUnit : null,
          weight: font.weight,
          style: font.style
        });
      }
      if (color) {
        textElement.fill(color);
      }
    }
  }

  private animation(element: Element, duration: number): Runner {
    if (!this.animationTimeline) {
      this.animationTimeline = new Timeline();
    }
    element.timeline(this.animationTimeline);
    return element.animate(duration, 0, 'now');
  }

  private computeValue(objectValue: ScadaObjectValue, value: any): any {
    if (objectValue) {
      switch (objectValue.type) {
        case 'input':
          return value;
        case 'constant':
          return objectValue.constantValue;
        case 'property':
          return objectValue.computedPropertyValue;
        case 'function':
          try {
            return objectValue.valueConverter(value);
          } catch (_e) {
            return value;
          }
        case 'valueFormat':
          let units = '';
          let decimals = 0;
          if (objectValue.units) {
            units = this.computeValue(objectValue.units, value);
          }
          if (objectValue.decimals) {
            decimals = this.computeValue(objectValue.decimals, value);
          }
          return formatValue(value, decimals, units, false);
      }
    } else {
      return '';
    }
  }

  private getPropertyValue(id: string): any {
    const property = this.metadata.properties.find(p => p.id === id);
    if (property) {
      const value = this.settings[id];
      if (isDefinedAndNotNull(value)) {
        return value;
      } else {
        switch (property.type) {
          case 'string':
            return '';
          case 'number':
            return 0;
          case 'color':
            return '#000';
        }
      }
    } else {
      return '';
    }
  }
}
