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
import { BehaviorSubject, forkJoin, Observable, Observer } from 'rxjs';
import { share } from 'rxjs/operators';
import { ValueAction, ValueGetter, ValueSetter } from '@home/components/widget/lib/action/action-widget.models';
import { WidgetContext } from '@home/models/widget-component.models';
import { ColorProcessor, constantColor, Font } from '@shared/models/widget-settings.models';
import { AttributeScope } from '@shared/models/telemetry/telemetry.models';
import { UtilsService } from '@core/services/utils.service';
import { WidgetAction, WidgetActionType, widgetActionTypeTranslationMap } from '@shared/models/widget.models';
import { ResizeObserver } from '@juggle/resize-observer';

export interface IotSvgApi {
  formatValue: (value: any, dec?: number, units?: string, showZeroDecimals?: boolean) => string | undefined;
  text: (element: Element | Element[], text: string) => void;
  font: (element: Element | Element[], font: Font, color: string) => void;
  animate: (element: Element, duration: number) => Runner;
  disable: (element: Element | Element[]) => void;
  enable: (element: Element | Element[]) => void;
  callAction: (event: Event, behaviorId: string, value?: any, observer?: Partial<Observer<void>>) => void;
  setValue: (valueId: string, value: any) => void;
}

export interface IotSvgContext {
  api: IotSvgApi;
  tags: {[id: string]: Element[]};
  values: {[id: string]: any};
  properties: {[id: string]: any};
}

export type IotSvgStateRenderFunction = (svg: Svg, ctx: IotSvgContext) => void;

export type IotSvgTagStateRenderFunction = (element: Element, ctx: IotSvgContext) => void;

export type IotSvgActionTrigger = 'click';

export type IotSvgActionFunction = (event: Event, ctx: IotSvgContext) => void;
export interface IotSvgAction {
  actionFunction?: string;
  action?: IotSvgActionFunction;
}

export interface IotSvgTag {
  tag: string;
  stateRenderFunction?: string;
  stateRender?: IotSvgTagStateRenderFunction;
  actions?: {[trigger: string]: IotSvgAction};
}

export enum IotSvgBehaviorType {
  value = 'value',
  action = 'action',
  widgetAction = 'widgetAction'
}

export const iotSvgBehaviorTypes = Object.keys(IotSvgBehaviorType) as IotSvgBehaviorType[];

export const iotSvgBehaviorTypeTranslations = new Map<IotSvgBehaviorType, string>(
  [
    [IotSvgBehaviorType.value, 'scada.behavior.type-value'],
    [IotSvgBehaviorType.action, 'scada.behavior.type-action'],
    [IotSvgBehaviorType.widgetAction, 'scada.behavior.type-widget-action']
  ]
);


export interface IotSvgBehaviorBase {
  id: string;
  name: string;
  hint?: string;
  type: IotSvgBehaviorType;
}

export interface IotSvgBehaviorValue extends IotSvgBehaviorBase {
  valueType: ValueType;
  defaultValue: any;
  trueLabel?: string;
  falseLabel?: string;
  stateLabel?: string;
}

export interface IotSvgBehaviorAction extends IotSvgBehaviorBase {
  valueType: ValueType;
  valueToDataType: ValueToDataType;
  constantValue: any;
  valueToDataFunction: string;
}

export type IotSvgBehavior = IotSvgBehaviorValue & IotSvgBehaviorAction;

export enum IotSvgPropertyType {
  text = 'text',
  number = 'number',
  switch = 'switch',
  color = 'color',
  color_settings = 'color_settings',
  font = 'font',
  units = 'units'
}

export const iotSvgPropertyTypes = Object.keys(IotSvgPropertyType) as IotSvgPropertyType[];

export const iotSvgPropertyTypeTranslations = new Map<IotSvgPropertyType, string>(
  [
    [IotSvgPropertyType.text, 'scada.property.type-text'],
    [IotSvgPropertyType.number, 'scada.property.type-number'],
    [IotSvgPropertyType.switch, 'scada.property.type-switch'],
    [IotSvgPropertyType.color, 'scada.property.type-color'],
    [IotSvgPropertyType.color_settings, 'scada.property.type-color-settings'],
    [IotSvgPropertyType.font, 'scada.property.type-font'],
    [IotSvgPropertyType.units, 'scada.property.type-units']
  ]
);

export const iotSvgPropertyRowClasses =
  ['column', 'column-xs', 'column-lt-md', 'align-start', 'no-border', 'no-gap', 'no-padding', 'same-padding'];

export const iotSvgPropertyFieldClasses =
  ['medium-width', 'flex', 'flex-xs', 'flex-lt-md'];

export interface IotSvgPropertyBase {
  id: string;
  name: string;
  type: IotSvgPropertyType;
  default: any;
  required?: boolean;
  subLabel?: string;
  divider?: boolean;
  fieldSuffix?: string;
  disableOnProperty?: string;
  rowClass?: string;
  fieldClass?: string;
}

export interface IotSvgNumberProperty extends IotSvgPropertyBase {
  min?: number;
  max?: number;
  step?: number;
}

export type IotSvgProperty = IotSvgPropertyBase & IotSvgNumberProperty;

export interface IotSvgMetadata {
  title: string;
  stateRenderFunction?: string;
  stateRender?: IotSvgStateRenderFunction;
  tags: IotSvgTag[];
  behavior: IotSvgBehavior[];
  properties: IotSvgProperty[];
}

export const emptyMetadata = (): IotSvgMetadata => ({
  title: '',
  tags: [],
  behavior: [],
  properties: []
});


export const parseIotSvgMetadataFromContent = (svgContent: string): IotSvgMetadata => {
  try {
    const svgDoc = new DOMParser().parseFromString(svgContent, 'image/svg+xml');
    return parseIotSvgMetadataFromDom(svgDoc);
  } catch (_e) {
    return emptyMetadata();
  }
};

const parseIotSvgMetadataFromDom = (svgDoc: Document): IotSvgMetadata => {
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

export const updateIotSvgMetadataInContent = (svgContent: string, metadata: IotSvgMetadata): string => {
  const svgDoc = new DOMParser().parseFromString(svgContent, 'image/svg+xml');
  updateIotSvgMetadataInDom(svgDoc, metadata);
  return svgDoc.documentElement.outerHTML;
};

const updateIotSvgMetadataInDom = (svgDoc: Document, metadata: IotSvgMetadata) => {
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

export interface IoTSvgContentData {
  svgRootNode: string;
  innerSvg: string;
}

export const iotSvgContentData = (svgContent: string): IoTSvgContentData => {
  const result: IoTSvgContentData = {
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

const defaultGetValueSettings = (get: IotSvgBehaviorValue): GetValueSettings<any> => ({
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

const defaultSetValueSettings = (set: IotSvgBehaviorAction): SetValueSettings => ({
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

const defaultWidgetActionSettings = (widgetAction: IotSvgBehavior): WidgetAction => ({
  type: WidgetActionType.updateDashboardState,
  targetDashboardStateId: null,
  openRightLayout: false,
  setEntityId: true,
  stateEntityParamName: null
});

export const defaultIotSvgObjectSettings = (metadata: IotSvgMetadata): IotSvgObjectSettings => {
  const settings: IotSvgObjectSettings = {
    behavior: {},
    properties: {}
  };
  for (const behavior of metadata.behavior) {
    if (behavior.type === IotSvgBehaviorType.value) {
      settings.behavior[behavior.id] = defaultGetValueSettings(behavior as IotSvgBehaviorValue);
    } else if (behavior.type === IotSvgBehaviorType.action) {
      settings.behavior[behavior.id] = defaultSetValueSettings(behavior as IotSvgBehaviorAction);
    } else if (behavior.type === IotSvgBehaviorType.widgetAction) {
      settings.behavior[behavior.id] = defaultWidgetActionSettings(behavior);
    }
  }
  for (const property of metadata.properties) {
    settings.properties[property.id] = property.default;
  }
  return settings;
};

export type IotSvgObjectSettings = {
  behavior: {[id: string]: any};
  properties: {[id: string]: any};
};

const parseError = (ctx: WidgetContext, err: any): string =>
  ctx.$injector.get(UtilsService).parseException(err).message || 'Unknown Error';

export interface IotSvgObjectCallbacks {
  onSvgObjectError: (error: string) => void;
  onSvgObjectMessage: (message: string) => void;
}

export class IotSvgObject {

  private metadata: IotSvgMetadata;
  private settings: IotSvgObjectSettings;
  private context: IotSvgContext;

  private svgShape: Svg;
  private box: Box;

  private loadingSubject = new BehaviorSubject(false);
  private valueGetters: ValueGetter<any>[] = [];
  private valueActions: ValueAction[] = [];
  private valueSetters: {[behaviorId: string]: ValueSetter<any>} = {};

  private stateValueSubjects: {[id: string]: BehaviorSubject<any>} = {};

  private readonly shapeResize$: ResizeObserver;
  private scale = 1;

  private performInit = true;

  loading$ = this.loadingSubject.asObservable().pipe(share());

  constructor(private rootElement: HTMLElement,
              private ctx: WidgetContext,
              private svgContent: string,
              private inputSettings: IotSvgObjectSettings,
              private callbacks: IotSvgObjectCallbacks,
              private simulated: boolean) {
    this.shapeResize$ = new ResizeObserver(() => {
      this.resize();
    });
    const doc: XMLDocument = new DOMParser().parseFromString(this.svgContent, 'image/svg+xml');
    this.metadata = parseIotSvgMetadataFromDom(doc);
    const defaults = defaultIotSvgObjectSettings(this.metadata);
    this.settings = mergeDeep<IotSvgObjectSettings>({} as IotSvgObjectSettings,
      defaults, this.inputSettings || {} as IotSvgObjectSettings);
    this.prepareMetadata();
    this.prepareSvgShape(doc);
    this.shapeResize$.observe(this.rootElement);
  }

  public destroy() {
    if (this.shapeResize$) {
      this.shapeResize$.disconnect();
    }
    for (const stateValueId of Object.keys(this.stateValueSubjects)) {
      this.stateValueSubjects[stateValueId].complete();
      this.stateValueSubjects[stateValueId].unsubscribe();
    }
    this.valueActions.forEach(v => v.destroy());
    this.loadingSubject.complete();
    this.loadingSubject.unsubscribe();
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
    this.metadata.stateRender = parseFunction(this.metadata.stateRenderFunction, ['svg', 'ctx']) || (() => {});
    for (const tag of this.metadata.tags) {
      tag.stateRender = parseFunction(tag.stateRenderFunction, ['element', 'ctx']) || (() => {});
      if (tag.actions) {
        for (const trigger of Object.keys(tag.actions)) {
          const action = tag.actions[trigger];
          action.action = parseFunction(action.actionFunction, ['event', 'ctx']) || (() => {});
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
              action.action(event, this.context);
            });
          });
        }
      }
    }
    for (const behavior of this.metadata.behavior) {
      if (behavior.type === IotSvgBehaviorType.value) {
        const getBehavior = behavior as IotSvgBehaviorValue;
        let getValueSettings: GetValueSettings<any> = this.settings.behavior[getBehavior.id];
        getValueSettings = {...getValueSettings, actionLabel:
            this.ctx.utilsService.customTranslation(getBehavior.name, getBehavior.name)};
        const stateValueSubject = new BehaviorSubject<any>(getValueSettings.defaultValue);
        this.stateValueSubjects[getBehavior.id] = stateValueSubject;
        this.context.values[getBehavior.id] = getValueSettings.defaultValue;
        stateValueSubject.subscribe((value) => {
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
      } else if (behavior.type === IotSvgBehaviorType.action) {
        const setBehavior = behavior as IotSvgBehaviorAction;
        let setValueSettings: SetValueSettings = this.settings.behavior[setBehavior.id];
        setValueSettings = {...setValueSettings, actionLabel:
            this.ctx.utilsService.customTranslation(setBehavior.name, setBehavior.name)};
        const valueSetter = ValueSetter.fromSettings<any>(this.ctx, setValueSettings, this.simulated);
        this.valueSetters[setBehavior.id] = valueSetter;
        this.valueActions.push(valueSetter);
      } else if (behavior.type === IotSvgBehaviorType.widgetAction) {
        // TODO:
      }
    }
    this.renderState();
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

  private onError(error: string) {
    this.callbacks.onSvgObjectError(error);
  }

  private onMessage(message: string) {
    this.callbacks.onSvgObjectMessage(message);
  }

  private callAction(event: Event, behaviorId: string, value?: any, observer?: Partial<Observer<void>>) {
    const behavior = this.metadata.behavior.find(b => b.id === behaviorId);
    if (behavior) {
      if (behavior.type === IotSvgBehaviorType.action) {
        const valueSetter = this.valueSetters[behaviorId];
        if (valueSetter) {
          this.loadingSubject.next(true);
          valueSetter.setValue(value).subscribe(
            {
              next: () => {
                if (observer?.next) {
                  observer.next();
                }
                this.loadingSubject.next(false);
              },
              error: (err) => {
                this.loadingSubject.next(false);
                if (observer?.error) {
                  observer.error(err);
                }
                const message = parseError(this.ctx, err);
                this.onError(message);
              }
            }
          );
        }
      } else if (behavior.type === IotSvgBehaviorType.widgetAction) {
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
        let scale: number;
        if (targetWidth < targetHeight) {
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
    const valueBehavior = this.metadata.behavior.find(b => b.id === id) as IotSvgBehaviorValue;
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
    this.metadata.stateRender(this.svgShape, this.context);
    for (const tag of this.metadata.tags) {
      const elements = this.context.tags[tag.tag];// this.svgShape.find(`[tb\\:tag="${tag.tag}"]`);
      elements.forEach(element => {
        tag.stateRender(element, this.context);
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

  private getProperty(id: string): IotSvgProperty {
    return this.metadata.properties.find(p => p.id === id);
  }

  private getPropertyValue(id: string): any {
    const property = this.getProperty(id);
    if (property) {
      const value = this.settings.properties[id];
      if (isDefinedAndNotNull(value)) {
        if (property.type === IotSvgPropertyType.color_settings) {
          return ColorProcessor.fromSettings(value);
        } else if (property.type === IotSvgPropertyType.text) {
          const result =  this.ctx.utilsService.customTranslation(value, value);
          const entityInfo = this.ctx.defaultSubscription.getFirstEntityInfo();
          return createLabelFromSubscriptionEntityInfo(entityInfo, result);
        }
        return value;
      } else {
        switch (property.type) {
          case IotSvgPropertyType.text:
            return '';
          case IotSvgPropertyType.number:
            return 0;
          case IotSvgPropertyType.color:
            return '#000';
          case IotSvgPropertyType.color_settings:
            return ColorProcessor.fromSettings(constantColor('#000'));
        }
      }
    } else {
      return '';
    }
  }
}
