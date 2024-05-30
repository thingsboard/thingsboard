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
import { Box, Element, Runner, SVG, Svg, Text, Timeline } from '@svgdotjs/svg.js';
import '@svgdotjs/svg.panzoom.js';
import {
  DataToValueType,
  GetValueAction,
  GetValueSettings,
  SetValueAction,
  SetValueSettings,
  ValueToDataType
} from '@shared/models/action-widget-settings.models';
import {
  createLabelFromSubscriptionEntityInfo,
  formatValue,
  isDefinedAndNotNull,
  isUndefinedOrNull,
  mergeDeep,
  parseFunction
} from '@core/utils';
import { BehaviorSubject, forkJoin, Observable, Observer, Subject } from 'rxjs';
import { ValueAction, ValueGetter, ValueSetter } from '@home/components/widget/lib/action/action-widget.models';
import { WidgetContext } from '@home/models/widget-component.models';
import { ColorProcessor, constantColor, Font } from '@shared/models/widget-settings.models';
import { AttributeScope } from '@shared/models/telemetry/telemetry.models';
import { UtilsService } from '@core/services/utils.service';
import { WidgetAction, WidgetActionType, widgetActionTypeTranslationMap } from '@shared/models/widget.models';
import { ResizeObserver } from '@juggle/resize-observer';
import { takeUntil } from 'rxjs/operators';

export interface ScadaSymbolApi {
  formatValue: (value: any, dec?: number, units?: string, showZeroDecimals?: boolean) => string | undefined;
  text: (element: Element | Element[], text: string) => void;
  font: (element: Element | Element[], font: Font, color: string) => void;
  animate: (element: Element, duration: number) => Runner;
  disable: (element: Element | Element[]) => void;
  enable: (element: Element | Element[]) => void;
  callAction: (event: Event, behaviorId: string, value?: any, observer?: Partial<Observer<void>>) => void;
  setValue: (valueId: string, value: any) => void;
}

export interface ScadaSymbolContext {
  api: ScadaSymbolApi;
  tags: {[id: string]: Element[]};
  values: {[id: string]: any};
  properties: {[id: string]: any};
}

export type ScadaSymbolStateRenderFunction = (ctx: ScadaSymbolContext, svg: Svg) => void;

export type ScadaSymbolTagStateRenderFunction = (ctx: ScadaSymbolContext, element: Element) => void;

export type ScadaSymbolActionTrigger = 'click';

export type ScadaSymbolActionFunction = (ctx: ScadaSymbolContext, element: Element, event: Event) => void;
export interface ScadaSymbolAction {
  actionFunction?: string;
  action?: ScadaSymbolActionFunction;
}

export interface ScadaSymbolTag {
  tag: string;
  stateRenderFunction?: string;
  stateRender?: ScadaSymbolTagStateRenderFunction;
  actions?: {[trigger: string]: ScadaSymbolAction};
}

export enum ScadaSymbolBehaviorType {
  value = 'value',
  action = 'action',
  widgetAction = 'widgetAction'
}

export const scadaSymbolBehaviorTypes = Object.keys(ScadaSymbolBehaviorType) as ScadaSymbolBehaviorType[];

export const scadaSymbolBehaviorTypeTranslations = new Map<ScadaSymbolBehaviorType, string>(
  [
    [ScadaSymbolBehaviorType.value, 'scada.behavior.type-value'],
    [ScadaSymbolBehaviorType.action, 'scada.behavior.type-action'],
    [ScadaSymbolBehaviorType.widgetAction, 'scada.behavior.type-widget-action']
  ]
);


export interface ScadaSymbolBehaviorBase {
  id: string;
  name: string;
  hint?: string;
  type: ScadaSymbolBehaviorType;
}

export interface ScadaSymbolBehaviorValue extends ScadaSymbolBehaviorBase {
  valueType: ValueType;
  defaultValue: any;
  trueLabel?: string;
  falseLabel?: string;
  stateLabel?: string;
}

export interface ScadaSymbolBehaviorAction extends ScadaSymbolBehaviorBase {
  valueType: ValueType;
  valueToDataType: ValueToDataType;
  constantValue: any;
  valueToDataFunction: string;
}

export type ScadaSymbolBehavior = ScadaSymbolBehaviorValue & ScadaSymbolBehaviorAction;

export enum ScadaSymbolPropertyType {
  text = 'text',
  number = 'number',
  switch = 'switch',
  color = 'color',
  color_settings = 'color_settings',
  font = 'font',
  units = 'units'
}

export const scadaSymbolPropertyTypes = Object.keys(ScadaSymbolPropertyType) as ScadaSymbolPropertyType[];

export const scadaSymbolPropertyTypeTranslations = new Map<ScadaSymbolPropertyType, string>(
  [
    [ScadaSymbolPropertyType.text, 'scada.property.type-text'],
    [ScadaSymbolPropertyType.number, 'scada.property.type-number'],
    [ScadaSymbolPropertyType.switch, 'scada.property.type-switch'],
    [ScadaSymbolPropertyType.color, 'scada.property.type-color'],
    [ScadaSymbolPropertyType.color_settings, 'scada.property.type-color-settings'],
    [ScadaSymbolPropertyType.font, 'scada.property.type-font'],
    [ScadaSymbolPropertyType.units, 'scada.property.type-units']
  ]
);

export const scadaSymbolPropertyRowClasses =
  ['column', 'column-xs', 'column-lt-md', 'align-start', 'no-border', 'no-gap', 'no-padding', 'same-padding'];

export const scadaSymbolPropertyFieldClasses =
  ['medium-width', 'flex', 'flex-xs', 'flex-lt-md'];

export interface ScadaSymbolPropertyBase {
  id: string;
  name: string;
  type: ScadaSymbolPropertyType;
  default: any;
  required?: boolean;
  subLabel?: string;
  divider?: boolean;
  fieldSuffix?: string;
  disableOnProperty?: string;
  rowClass?: string;
  fieldClass?: string;
}

export interface ScadaSymbolNumberProperty extends ScadaSymbolPropertyBase {
  min?: number;
  max?: number;
  step?: number;
}

export type ScadaSymbolProperty = ScadaSymbolPropertyBase & ScadaSymbolNumberProperty;

export interface ScadaSymbolMetadata {
  title: string;
  stateRenderFunction?: string;
  stateRender?: ScadaSymbolStateRenderFunction;
  tags: ScadaSymbolTag[];
  behavior: ScadaSymbolBehavior[];
  properties: ScadaSymbolProperty[];
}

export const emptyMetadata = (): ScadaSymbolMetadata => ({
  title: '',
  tags: [],
  behavior: [],
  properties: []
});


export const parseScadaSymbolMetadataFromContent = (svgContent: string): ScadaSymbolMetadata => {
  try {
    const svgDoc = new DOMParser().parseFromString(svgContent, 'image/svg+xml');
    return parseScadaSymbolMetadataFromDom(svgDoc);
  } catch (_e) {
    return emptyMetadata();
  }
};

const parseScadaSymbolMetadataFromDom = (svgDoc: Document): ScadaSymbolMetadata => {
  try {
    const elements = svgDoc.getElementsByTagName('tb:metadata');
    if (elements.length) {
      return JSON.parse(elements[0].textContent);
    } else {
      return emptyMetadata();
    }
  } catch (_e) {
    console.error(_e);
    return emptyMetadata();
  }
};

export const updateScadaSymbolMetadataInContent = (svgContent: string, metadata: ScadaSymbolMetadata): string => {
  const svgDoc = new DOMParser().parseFromString(svgContent, 'image/svg+xml');
  updateScadaSymbolMetadataInDom(svgDoc, metadata);
  return svgDoc.documentElement.outerHTML;
};

const updateScadaSymbolMetadataInDom = (svgDoc: Document, metadata: ScadaSymbolMetadata) => {
  svgDoc.documentElement.setAttribute('xmlns:tb', 'https://thingsboard.io/svg');
  let metadataElement: Node;
  const elements = svgDoc.getElementsByTagName('tb:metadata');
  if (elements?.length) {
    metadataElement = elements[0];
    metadataElement.textContent = '';
  } else {
    metadataElement = svgDoc.createElement('tb:metadata');
    svgDoc.documentElement.insertBefore(metadataElement, svgDoc.documentElement.firstChild);
  }
  const content = JSON.stringify(metadata, null, 2);
  const cdata = svgDoc.createCDATASection(content);
  metadataElement.appendChild(cdata);
};

const svgPartsRegex = /(<svg .*?>)(.*)<\/svg>/gms;
const tbMetadataRegex = /<tb:metadata>.*<\/tb:metadata>/gs;

export interface ScadaSymbolContentData {
  svgRootNode: string;
  innerSvg: string;
}

export const scadaSymbolContentData = (svgContent: string): ScadaSymbolContentData => {
  const result: ScadaSymbolContentData = {
    svgRootNode: '',
    innerSvg: ''
  };
  svgPartsRegex.lastIndex = 0;
  const match = svgPartsRegex.exec(svgContent);
  if (match != null) {
    if (match.length > 1) {
      result.svgRootNode = match[1];
    }
    if (match.length > 2) {
      let innerSvgContent = match[2];
      tbMetadataRegex.lastIndex = 0;
      const metadataMatch = tbMetadataRegex.exec(svgContent);
      if (metadataMatch !== null && metadataMatch.length) {
        const metadata = metadataMatch[0];
        innerSvgContent = innerSvgContent.replace(metadata, '');
      }
      result.innerSvg = innerSvgContent;
    }
  }
  return result;
};

const defaultGetValueSettings = (get: ScadaSymbolBehaviorValue): GetValueSettings<any> => ({
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

const defaultSetValueSettings = (set: ScadaSymbolBehaviorAction): SetValueSettings => ({
  action: SetValueAction.EXECUTE_RPC,
  executeRpc: {
    method: 'setState',
    requestTimeout: 5000,
    requestPersistent: false,
    persistentPollingInterval: 1000
  },
  setAttribute: {
    key: 'state',
    scope: AttributeScope.SERVER_SCOPE
  },
  putTimeSeries: {
    key: 'state'
  },
  valueToData: {
    type: set.valueToDataType,
    constantValue: set.constantValue,
    valueToDataFunction: set.valueToDataFunction ? set.valueToDataFunction :
      '/* Convert input boolean value to RPC parameters or attribute/time-series value */\nreturn value;'
  }
});

const defaultWidgetActionSettings = (widgetAction: ScadaSymbolBehavior): WidgetAction => ({
  type: WidgetActionType.updateDashboardState,
  targetDashboardStateId: null,
  openRightLayout: false,
  setEntityId: true,
  stateEntityParamName: null
});

export const defaultScadaSymbolObjectSettings = (metadata: ScadaSymbolMetadata): ScadaSymbolObjectSettings => {
  const settings: ScadaSymbolObjectSettings = {
    behavior: {},
    properties: {}
  };
  for (const behavior of metadata.behavior) {
    if (behavior.type === ScadaSymbolBehaviorType.value) {
      settings.behavior[behavior.id] = defaultGetValueSettings(behavior as ScadaSymbolBehaviorValue);
    } else if (behavior.type === ScadaSymbolBehaviorType.action) {
      settings.behavior[behavior.id] = defaultSetValueSettings(behavior as ScadaSymbolBehaviorAction);
    } else if (behavior.type === ScadaSymbolBehaviorType.widgetAction) {
      settings.behavior[behavior.id] = defaultWidgetActionSettings(behavior);
    }
  }
  for (const property of metadata.properties) {
    settings.properties[property.id] = property.default;
  }
  return settings;
};

export type ScadaSymbolObjectSettings = {
  behavior: {[id: string]: any};
  properties: {[id: string]: any};
};

const parseError = (ctx: WidgetContext, err: any): string =>
  ctx.$injector.get(UtilsService).parseException(err).message || 'Unknown Error';

export interface ScadaSymbolObjectCallbacks {
  onScadaSymbolObjectLoadingState: (loading: boolean) => void;
  onScadaSymbolObjectError: (error: string) => void;
  onScadaSymbolObjectMessage: (message: string) => void;
}

export class ScadaSymbolObject {

  private metadata: ScadaSymbolMetadata;
  private settings: ScadaSymbolObjectSettings;
  private context: ScadaSymbolContext;

  private svgShape: Svg;
  private box: Box;

  private valueGetters: ValueGetter<any>[] = [];
  private valueActions: ValueAction[] = [];
  private valueSetters: {[behaviorId: string]: ValueSetter<any>} = {};

  private stateValueSubjects: {[id: string]: BehaviorSubject<any>} = {};

  private readonly shapeResize$: ResizeObserver;
  private readonly destroy$ = new Subject<void>();

  private scale = 1;

  private performInit = true;

  constructor(private rootElement: HTMLElement,
              private ctx: WidgetContext,
              private svgContent: string,
              private inputSettings: ScadaSymbolObjectSettings,
              private callbacks: ScadaSymbolObjectCallbacks,
              private simulated: boolean) {
    this.shapeResize$ = new ResizeObserver(() => {
      this.resize();
    });
    const doc: XMLDocument = new DOMParser().parseFromString(this.svgContent, 'image/svg+xml');
    this.metadata = parseScadaSymbolMetadataFromDom(doc);
    const defaults = defaultScadaSymbolObjectSettings(this.metadata);
    this.settings = mergeDeep<ScadaSymbolObjectSettings>({} as ScadaSymbolObjectSettings,
      defaults, this.inputSettings || {} as ScadaSymbolObjectSettings);
    this.prepareMetadata();
    this.prepareSvgShape(doc);
    this.shapeResize$.observe(this.rootElement);
  }

  public destroy() {
    this.destroy$.next();
    this.destroy$.complete();
    if (this.shapeResize$) {
      this.shapeResize$.disconnect();
    }
    for (const stateValueId of Object.keys(this.stateValueSubjects)) {
      this.stateValueSubjects[stateValueId].complete();
      this.stateValueSubjects[stateValueId].unsubscribe();
    }
    this.valueActions.forEach(v => v.destroy());
    for (const tag of this.metadata.tags) {
      const elements = this.context.tags[tag.tag];
      elements.forEach(element => {
        element.timeline().finish();
        element.timeline(null);
      });
    }
    if (this.svgShape) {
      this.svgShape.remove();
    }
  }

  private prepareMetadata() {
    this.metadata.stateRender = parseFunction(this.metadata.stateRenderFunction, ['ctx', 'svg']) || (() => {});
    for (const tag of this.metadata.tags) {
      tag.stateRender = parseFunction(tag.stateRenderFunction, ['ctx', 'element']) || (() => {});
      if (tag.actions) {
        for (const trigger of Object.keys(tag.actions)) {
          const action = tag.actions[trigger];
          action.action = parseFunction(action.actionFunction, ['ctx', 'element', 'event']) || (() => {});
        }
      }
    }
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
    this.svgShape.addTo(this.rootElement);
  }

  private init() {
    this.context = {
      api: {
        formatValue,
        text: this.setElementText.bind(this),
        font: this.setElementFont.bind(this),
        animate: this.animate.bind(this),
        disable: this.disableElement.bind(this),
        enable: this.enableElement.bind(this),
        callAction: this.callAction.bind(this),
        setValue: this.setValue.bind(this)
      },
      tags: {},
      properties: {},
      values: {}
    };
    const taggedElements = this.svgShape.find(`[tb\\:tag]`);
    for (const element of taggedElements) {
      const tag: string = element.attr('tb:tag');
      let elements = this.context.tags[tag];
      if (!elements) {
        elements = [];
        this.context.tags[tag] = elements;
      }
      elements.push(element);
    }
    for (const property of this.metadata.properties) {
      this.context.properties[property.id] = this.getPropertyValue(property.id);
    }
    for (const tag of this.metadata.tags) {
      if (tag.actions) {
        const elements = this.svgShape.find(`[tb\\:tag="${tag.tag}"]`);
        for (const trigger of Object.keys(tag.actions)) {
          const action = tag.actions[trigger];
          elements.forEach(element => {
            element.attr('cursor', 'pointer');
            element.on(trigger, (event) => {
              action.action(this.context, element, event);
            });
          });
        }
      }
    }
    for (const behavior of this.metadata.behavior) {
      if (behavior.type === ScadaSymbolBehaviorType.value) {
        const getBehavior = behavior as ScadaSymbolBehaviorValue;
        let getValueSettings: GetValueSettings<any> = this.settings.behavior[getBehavior.id];
        getValueSettings = {...getValueSettings, actionLabel:
            this.ctx.utilsService.customTranslation(getBehavior.name, getBehavior.name)};
        const stateValueSubject = new BehaviorSubject<any>(getValueSettings.defaultValue);
        this.stateValueSubjects[getBehavior.id] = stateValueSubject;
        this.context.values[getBehavior.id] = getValueSettings.defaultValue;
        stateValueSubject.pipe(takeUntil(this.destroy$)).subscribe((value) => {
          this.onStateValueChanged(getBehavior.id, value);
        });
        const valueGetter =
          ValueGetter.fromSettings(this.ctx, getValueSettings, getBehavior.valueType, {
            next: (val) => {this.onValue(getBehavior.id, val);},
            error: (err) => {
              const message = parseError(this.ctx, err);
              this.onError(message);
            }
          }, this.simulated);
        this.valueGetters.push(valueGetter);
        this.valueActions.push(valueGetter);
      } else if (behavior.type === ScadaSymbolBehaviorType.action) {
        const setBehavior = behavior as ScadaSymbolBehaviorAction;
        let setValueSettings: SetValueSettings = this.settings.behavior[setBehavior.id];
        setValueSettings = {...setValueSettings, actionLabel:
            this.ctx.utilsService.customTranslation(setBehavior.name, setBehavior.name)};
        const valueSetter = ValueSetter.fromSettings<any>(this.ctx, setValueSettings, this.simulated);
        this.valueSetters[setBehavior.id] = valueSetter;
        this.valueActions.push(valueSetter);
      } else if (behavior.type === ScadaSymbolBehaviorType.widgetAction) {
        // TODO:
      }
    }
    this.renderState();
    if (this.valueGetters.length) {
      const getValueObservables: Array<Observable<any>> = [];
      this.valueGetters.forEach(valueGetter => {
        getValueObservables.push(valueGetter.getValue());
      });
      this.onLoadingState(true);
      forkJoin(getValueObservables).pipe(takeUntil(this.destroy$)).subscribe(
        {
          next: () => {
            this.onLoadingState(false);
          },
          error: () => {
            this.onLoadingState(false);
          }
        }
      );
    }
  }

  private onLoadingState(loading: boolean) {
    this.callbacks.onScadaSymbolObjectLoadingState(loading);
  }

  private onError(error: string) {
    this.callbacks.onScadaSymbolObjectError(error);
  }

  private onMessage(message: string) {
    this.callbacks.onScadaSymbolObjectMessage(message);
  }

  private callAction(event: Event, behaviorId: string, value?: any, observer?: Partial<Observer<void>>) {
    const behavior = this.metadata.behavior.find(b => b.id === behaviorId);
    if (behavior) {
      if (behavior.type === ScadaSymbolBehaviorType.action) {
        const valueSetter = this.valueSetters[behaviorId];
        if (valueSetter) {
          this.onLoadingState(true);
          valueSetter.setValue(value).pipe(takeUntil(this.destroy$)).subscribe(
            {
              next: () => {
                if (observer?.next) {
                  observer.next();
                }
                this.onLoadingState(false);
              },
              error: (err) => {
                this.onLoadingState(false);
                if (observer?.error) {
                  observer.error(err);
                }
                const message = parseError(this.ctx, err);
                this.onError(message);
              }
            }
          );
        }
      } else if (behavior.type === ScadaSymbolBehaviorType.widgetAction) {
        const widgetAction: WidgetAction = this.settings.behavior[behavior.id];
        if (this.simulated) {
          const translatedType = this.ctx.translate.instant(widgetActionTypeTranslationMap.get(widgetAction.type));
          const message = this.ctx.translate.instant('scada.preview-widget-action-text', {type: translatedType});
          this.onMessage(message);
        } else {
          this.ctx.actionsApi.onWidgetAction(event, widgetAction);
        }
        if (observer?.next) {
          observer.next();
        }
      }
    }
  }

  private resize() {
    if (this.svgShape) {
      const targetWidth = this.rootElement.getBoundingClientRect().width;
      const targetHeight = this.rootElement.getBoundingClientRect().height;
      if (targetWidth && targetHeight) {
        const svgAspect = this.box.width / this.box.height;
        const shapeAspect = targetWidth / targetHeight;
        let scale: number;
        if (svgAspect > shapeAspect) {
          scale = targetWidth / this.box.width;
        } else {
          scale = targetHeight / this.box.height;
        }
        if (this.scale !== scale) {
          this.scale = scale;
          this.svgShape.node.style.transform = `scale(${this.scale})`;
        }
        if (this.performInit) {
          this.performInit = false;
          this.init();
        }
      }
    }
  }

  private onValue(id: string, value: any) {
    const valueBehavior = this.metadata.behavior.find(b => b.id === id) as ScadaSymbolBehaviorValue;
    value = this.normalizeValue(value, valueBehavior.valueType);
    this.setValue(valueBehavior.id, value);
  }

  private setValue(valueId: string, value: any) {
    const stateValueSubject = this.stateValueSubjects[valueId];
    if (stateValueSubject && stateValueSubject.value !== value) {
      stateValueSubject.next(value);
    }
  }

  private onStateValueChanged(id: string, value: any) {
    if (this.context.values[id] !== value) {
      this.context.values[id] = value;
      this.renderState();
    }
  }

  private renderState(): void {
    this.metadata.stateRender(this.context, this.svgShape);
    for (const tag of this.metadata.tags) {
      const elements = this.context.tags[tag.tag];// this.svgShape.find(`[tb\\:tag="${tag.tag}"]`);
      elements.forEach(element => {
        tag.stateRender(this.context, element);
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

  private setElementText(e: Element | Element[], text: string) {
    this.elements(e).forEach(element => {
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
    });
  }

  private setElementFont(e: Element | Element[], font: Font, color: string) {
    this.elements(e).forEach(element => {
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
    });
  }

  private animate(element: Element, duration: number): Runner {
    element.timeline().finish();
    element.timeline(new Timeline());
    return element.animate(duration, 0, 'now');
  }

  private disableElement(e: Element | Element[]) {
    this.elements(e).forEach(element => {
      element.attr({'pointer-events': 'none'});
    });
  }

  private enableElement(e: Element | Element[]) {
    this.elements(e).forEach(element => {
      element.attr({'pointer-events': null});
    });
  }

  private elements(element: Element | Element[]): Element[] {
    return Array.isArray(element) ? element : [element];
  }

  private getProperty(id: string): ScadaSymbolProperty {
    return this.metadata.properties.find(p => p.id === id);
  }

  private getPropertyValue(id: string): any {
    const property = this.getProperty(id);
    if (property) {
      const value = this.settings.properties[id];
      if (isDefinedAndNotNull(value)) {
        if (property.type === ScadaSymbolPropertyType.color_settings) {
          return ColorProcessor.fromSettings(value);
        } else if (property.type === ScadaSymbolPropertyType.text) {
          const result =  this.ctx.utilsService.customTranslation(value, value);
          const entityInfo = this.ctx.defaultSubscription.getFirstEntityInfo();
          return createLabelFromSubscriptionEntityInfo(entityInfo, result);
        }
        return value;
      } else {
        switch (property.type) {
          case ScadaSymbolPropertyType.text:
            return '';
          case ScadaSymbolPropertyType.number:
            return 0;
          case ScadaSymbolPropertyType.color:
            return '#000';
          case ScadaSymbolPropertyType.color_settings:
            return ColorProcessor.fromSettings(constantColor('#000'));
        }
      }
    } else {
      return '';
    }
  }
}
