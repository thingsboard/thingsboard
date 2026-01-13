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

import { ValueType } from '@shared/models/constants';
import {
  Box,
  EasingLiteral,
  Element,
  Matrix,
  MatrixExtract,
  MatrixTransformParam,
  Runner,
  Style,
  SVG,
  Svg,
  Text,
  Timeline,
  TimesParam,
  TransformData
} from '@svgdotjs/svg.js';
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
  deepClone,
  guid,
  isDefinedAndNotNull,
  isFirefox,
  isNumeric,
  isSafari,
  isUndefined,
  isUndefinedOrNull,
  mergeDeep,
  mergeDeepIgnoreArray,
  objectHashCode,
  parseFunction
} from '@core/utils';
import { BehaviorSubject, forkJoin, Observable, Observer, of, Subject } from 'rxjs';
import { ValueAction, ValueGetter, ValueSetter } from '@home/components/widget/lib/action/action-widget.models';
import { WidgetContext } from '@home/models/widget-component.models';
import {
  ColorProcessor,
  constantColor,
  Font,
  ValueFormatProcessor,
  ValueFormatSettings
} from '@shared/models/widget-settings.models';
import { AttributeScope } from '@shared/models/telemetry/telemetry.models';
import { UtilsService } from '@core/services/utils.service';
import { WidgetAction, WidgetActionType, widgetActionTypeTranslationMap } from '@shared/models/widget.models';
import { catchError, map, take, takeUntil } from 'rxjs/operators';
import { isSvgIcon, splitIconName } from '@shared/models/icon.models';
import { MatIconRegistry } from '@angular/material/icon';
import { RafService } from '@core/services/raf.service';
import {
  defaultFormPropertyValue,
  defaultPropertyValue,
  FormProperty,
  FormPropertyType
} from '@shared/models/dynamic-form.models';
import { TbUnit } from '@shared/models/unit.models';

export interface ScadaSymbolApi {
  generateElementId: () => string;
  formatValue(value: any, dec?: number, units?: string, showZeroDecimals?: boolean): string | undefined;
  formatValue(value: any, settings: ValueFormatSettings): string;
  text: (element: Element | Element[], text: string) => void;
  font: (element: Element | Element[], font: Font, color: string) => void;
  icon: (element: Element | Element[], icon: string, size?: number, color?: string, center?: boolean) => void;
  cssAnimate: (element: Element, duration: number) => ScadaSymbolAnimation;
  cssAnimation: (element: Element) => ScadaSymbolAnimation | undefined;
  resetCssAnimation: (element: Element) => void;
  finishCssAnimation: (element: Element) => void;
  connectorAnimation:(element: Element) => ConnectorScadaSymbolAnimation | undefined;
  connectorAnimate:(element: Element, path: string, reversedPath: string) => ConnectorScadaSymbolAnimation;
  resetConnectorAnimation: (element: Element) => void;
  finishConnectorAnimation: (element: Element) => void;
  disable: (element: Element | Element[]) => void;
  enable: (element: Element | Element[]) => void;
  callAction: (event: Event, behaviorId: string, value?: any, observer?: Partial<Observer<void>>) => void;
  setValue: (valueId: string, value: any) => void;
  unitSymbol: (unit: TbUnit) => string;
  convertUnitValue: (value: any, unit: TbUnit) => number;
}

export interface ScadaSymbolContext {
  api: ScadaSymbolApi;
  tags: {[id: string]: Element[]};
  values: {[id: string]: any};
  properties: {[id: string]: any};
  svg: Svg;
}

export type ScadaSymbolStateRenderFunction = (ctx: ScadaSymbolContext, svg: Svg) => void;

export type ScadaSymbolTagStateRenderFunction = (ctx: ScadaSymbolContext, element: Element) => void;

// noinspection JSUnusedGlobalSymbols
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
  group?: string;
  type: ScadaSymbolBehaviorType;
}

export interface ScadaSymbolBehaviorValue extends ScadaSymbolBehaviorBase {
  valueType: ValueType;
  defaultGetValueSettings?: GetValueSettings<any>;
  trueLabel?: string;
  falseLabel?: string;
  stateLabel?: string;
}

export interface ScadaSymbolBehaviorAction extends ScadaSymbolBehaviorBase {
  valueType: ValueType;
  defaultSetValueSettings?: SetValueSettings;
  defaultWidgetActionSettings?: WidgetAction;
}

export type ScadaSymbolBehavior = ScadaSymbolBehaviorValue & ScadaSymbolBehaviorAction;

export interface ScadaSymbolMetadata {
  title: string;
  description?: string;
  searchTags?: string[];
  widgetSizeX: number;
  widgetSizeY: number;
  stateRenderFunction?: string;
  stateRender?: ScadaSymbolStateRenderFunction;
  tags: ScadaSymbolTag[];
  behavior: ScadaSymbolBehavior[];
  properties: FormProperty[];
}

export const emptyMetadata = (width?: number, height?: number): ScadaSymbolMetadata => ({
  title: '',
  widgetSizeX: width ? Math.max(Math.round(width/100), 1) : 3,
  widgetSizeY: height ? Math.max(Math.round(height/100), 1) : 3,
  tags: [],
  behavior: [],
  properties: []
});

const svgPartsRegex = /(<svg.*?>)(.*)<\/svg>/gms;

const tbNamespaceRegex = /<svg.*(xmlns:tb="https:\/\/thingsboard.io\/svg").*>/gms;

const tbTagRegex = /tb:tag="([^"]*)"/gms;

const syncTime = Date.now();

const generateElementId = () => {
  const id = guid();
  const firstChar = id.charAt(0);
  if (firstChar >= '0' && firstChar <= '9') {
    return 'a' + id;
  } else {
    return id;
  }
};

export const applyTbNamespaceToSvgContent = (svgContent: string): string => {
  svgPartsRegex.lastIndex = 0;
  let svgRootNode: string;
  let innerSvg = '';
  let match = svgPartsRegex.exec(svgContent);
  if (match != null) {
    if (match.length > 1) {
      svgRootNode =  match[1];
    }
    if (match.length > 2) {
      innerSvg = match[2];
    }
  }
  if (!svgRootNode) {
    throw new Error('Invalid SVG document.');
  }
  tbNamespaceRegex.lastIndex = 0;
  match = tbNamespaceRegex.exec(svgRootNode);
  if (match === null || !match.length) {
    svgRootNode = svgRootNode.slice(0, -1) + ' xmlns:tb="https://thingsboard.io/svg">';
    return `${svgRootNode}\n${innerSvg}\n</svg>`;
  }
  return svgContent;
};

export const parseScadaSymbolMetadataFromContent = (svgContent: string): ScadaSymbolMetadata => {
  try {
    svgContent = applyTbNamespaceToSvgContent(svgContent);
    const svgDoc = new DOMParser().parseFromString(svgContent, 'image/svg+xml');
    return parseScadaSymbolMetadataFromDom(svgDoc);
  } catch (_e) {
    return emptyMetadata();
  }
};

export const parseScadaSymbolsTagsFromContent = (svgContent: string): string[] => {
  const tags: string[] = [];
  tbTagRegex.lastIndex = 0;
  let tagsMatch = tbTagRegex.exec(svgContent);
  while (tagsMatch !== null) {
    tags.push(tagsMatch[1]);
    tagsMatch = tbTagRegex.exec(svgContent);
  }
  return tags.filter((v, i, arr) => arr.indexOf(v) === i);
};

const parseScadaSymbolMetadataFromDom = (svgDoc: Document): ScadaSymbolMetadata => {
  try {
    const elements = svgDoc.getElementsByTagName('tb:metadata');
    if (elements.length) {
      return JSON.parse(elements[0].textContent);
    } else {
      const svg = svgDoc.getElementsByTagName('svg')[0];
      let width = null;
      let height = null;
      if (svg.viewBox?.baseVal?.width && svg.viewBox?.baseVal?.height) {
        width = svg.viewBox.baseVal.width;
        height = svg.viewBox.baseVal.height;
      } else if (svg.width?.baseVal?.value && svg.height?.baseVal?.value) {
        width = svg.width.baseVal.value;
        height = svg.height.baseVal.value;
      }
      return emptyMetadata(width, height);
    }
  } catch (_e) {
    console.error(_e);
    return emptyMetadata();
  }
};

export const updateScadaSymbolMetadataInContent = (svgContent: string, metadata: ScadaSymbolMetadata): string => {
  svgContent = applyTbNamespaceToSvgContent(svgContent);
  const svgDoc = new DOMParser().parseFromString(svgContent, 'image/svg+xml');
  const parsererror = svgDoc.getElementsByTagName('parsererror');
  if (parsererror?.length) {
    throw Error(parsererror[0].textContent)
  }
  updateScadaSymbolMetadataInDom(svgDoc, metadata);
  return svgDoc.documentElement.outerHTML;
};

const updateScadaSymbolMetadataInDom = (svgDoc: Document, metadata: ScadaSymbolMetadata) => {
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

const tbMetadataRegex = /<tb:metadata[^>]*>.*<\/tb:metadata>/gs;

export interface ScadaSymbolContentData {
  svgRootNode: string;
  innerSvg: string;
}

export const removeScadaSymbolMetadata = (svgContent: string): string => {
  let result = svgContent;
  tbMetadataRegex.lastIndex = 0;
  const metadataMatch = tbMetadataRegex.exec(svgContent);
  if (metadataMatch !== null && metadataMatch.length) {
    const metadata = metadataMatch[0];
    result = result.replace(metadata, '');
  }
  return result;
};

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

const defaultValueForValueType = (valueType: ValueType): any => {
  if (!valueType) {
    return null;
  }
  switch (valueType) {
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
};

export const defaultGetValueSettings = (valueType: ValueType): GetValueSettings<any> => ({
  action: GetValueAction.DO_NOTHING,
  defaultValue: defaultValueForValueType(valueType),
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
  getAlarmStatus: {
    severityList: null,
    typeList: null
  },
  dataToValue: {
    type: DataToValueType.NONE,
    compareToValue: true,
    dataToValueFunction: '/* Should return boolean value */\nreturn data;'
  }
});

export const defaultSetValueSettings = (valueType: ValueType): SetValueSettings => ({
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
    type: valueType !== ValueType.BOOLEAN ? ValueToDataType.VALUE : ValueToDataType.CONSTANT,
    constantValue: false,
    valueToDataFunction:
      '/* Convert input boolean value to RPC parameters or attribute/time-series value */\nreturn value;'
  }
});

export const defaultWidgetActionSettings: WidgetAction = {
  type: WidgetActionType.doNothing,
  targetDashboardStateId: null,
  openRightLayout: false,
  setEntityId: false,
  stateEntityParamName: null
};

export const updateBehaviorDefaultSettings = (behavior: ScadaSymbolBehavior): ScadaSymbolBehavior => {
  if (behavior.type) {
    switch (behavior.type) {
      case ScadaSymbolBehaviorType.value:
        delete behavior.defaultSetValueSettings;
        delete behavior.defaultWidgetActionSettings;
        if (!behavior.defaultGetValueSettings) {
          behavior.defaultGetValueSettings = mergeDeep({} as GetValueSettings<any>, defaultGetValueSettings(behavior.valueType));
        }
        break;
      case ScadaSymbolBehaviorType.action:
        delete behavior.defaultGetValueSettings;
        delete behavior.defaultWidgetActionSettings;
        if (!behavior.defaultSetValueSettings) {
          behavior.defaultSetValueSettings = mergeDeep({} as SetValueSettings, defaultSetValueSettings(behavior.valueType));
        }
        break;
      case ScadaSymbolBehaviorType.widgetAction:
        delete behavior.defaultGetValueSettings;
        delete behavior.defaultSetValueSettings;
        if (!behavior.defaultWidgetActionSettings) {
          behavior.defaultWidgetActionSettings = mergeDeep({} as WidgetAction, defaultWidgetActionSettings);
        }
        break;
    }
  }
  return behavior;
};

export const defaultScadaSymbolObjectSettings = (metadata: ScadaSymbolMetadata): ScadaSymbolObjectSettings => {
  const settings: ScadaSymbolObjectSettings = {
    behavior: {},
    properties: {}
  };
  for (const behavior of metadata.behavior) {
    if (behavior.type === ScadaSymbolBehaviorType.value) {
      settings.behavior[behavior.id] =
        mergeDeep({} as GetValueSettings<any>,
          defaultGetValueSettings(behavior.valueType), (behavior as ScadaSymbolBehaviorValue).defaultGetValueSettings || {});
    } else if (behavior.type === ScadaSymbolBehaviorType.action) {
      settings.behavior[behavior.id] =
        mergeDeep({} as SetValueSettings,
          defaultSetValueSettings(behavior.valueType), (behavior as ScadaSymbolBehaviorAction).defaultSetValueSettings || {});
    } else if (behavior.type === ScadaSymbolBehaviorType.widgetAction) {
      settings.behavior[behavior.id] =
        mergeDeep({} as WidgetAction,
          defaultWidgetActionSettings, (behavior as ScadaSymbolBehaviorAction).defaultWidgetActionSettings || {});
    }
  }
  for (const property of metadata.properties) {
    settings.properties[property.id] = defaultFormPropertyValue(property);
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

  private readonly metadata: ScadaSymbolMetadata;
  private settings: ScadaSymbolObjectSettings;
  private context: ScadaSymbolContext;
  private cssAnimations: CssScadaSymbolAnimations;
  private connectorAnimations: ScadaSymbolFlowConnectorAnimations;

  private svgShape: Svg;
  private box: Box;

  private valueGetters: ValueGetter<any>[] = [];
  private valueActions: ValueAction[] = [];
  private valueSetters: {[behaviorId: string]: ValueSetter<any>} = {};

  private stateValueSubjects: {[id: string]: BehaviorSubject<any>} = {};

  private valueProcessor: {[id: string]: ValueFormatProcessor} = {};

  private readonly shapeResize$: ResizeObserver;
  private readonly destroy$ = new Subject<void>();

  private scale = 1;

  private performInit = true;

  constructor(private rootElement: HTMLElement,
              private ctx: WidgetContext,
              private iconRegistry: MatIconRegistry,
              private raf: RafService,
              private readonly svgContent: string,
              private inputSettings: ScadaSymbolObjectSettings,
              private callbacks: ScadaSymbolObjectCallbacks,
              private simulated: boolean) {
    this.shapeResize$ = new ResizeObserver(() => {
      this.resize();
    });
    this.svgContent = applyTbNamespaceToSvgContent(this.svgContent);
    const doc: XMLDocument = new DOMParser().parseFromString(this.svgContent, 'image/svg+xml');
    this.metadata = parseScadaSymbolMetadataFromDom(doc);
    const defaults = defaultScadaSymbolObjectSettings(this.metadata);
    this.settings = mergeDeepIgnoreArray<ScadaSymbolObjectSettings>({} as ScadaSymbolObjectSettings,
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
    if (this.context) {
      for (const tag of this.metadata.tags) {
        const elements = this.context.tags[tag.tag];
        if (elements) {
          elements.forEach(element => {
            element.timeline().stop();
            element.timeline(null);
          });
        }
      }
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
    let svgContent = doc.documentElement.innerHTML;
    const regexp = /\sid="([^"]*)"\s/g;
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
      svgContent = svgContent.replace(new RegExp('#'+id, 'g'), '#'+newId);
    }

    this.svgShape = SVG().svg(svgContent);
    this.svgShape.node.style.overflow = 'hidden';
    this.svgShape.node.style.position = 'absolute';
    this.svgShape.node.style['user-select'] = 'none';
    const origSvg = SVG(doc.documentElement.outerHTML);
    if (origSvg.type === 'svg') {
      this.box = (origSvg as Svg).viewbox();
      if (origSvg.fill()) {
        this.svgShape.fill(origSvg.fill());
      }
    } else {
      this.box = this.svgShape.bbox();
    }
    origSvg.remove();
    this.svgShape.size(this.box.width, this.box.height);
    this.svgShape.addTo(this.rootElement);
  }

  private init() {
    this.cssAnimations = new CssScadaSymbolAnimations(this.svgShape, this.raf);
    this.connectorAnimations = new ScadaSymbolFlowConnectorAnimations();
    this.context = {
      api: {
        generateElementId: () => generateElementId(),
        formatValue: this.formatValue.bind(this),
        text: this.setElementText.bind(this),
        font: this.setElementFont.bind(this),
        icon: this.setElementIcon.bind(this),
        cssAnimate: this.cssAnimate.bind(this),
        cssAnimation: this.cssAnimation.bind(this),
        resetCssAnimation: this.resetCssAnimation.bind(this),
        finishCssAnimation: this.finishCssAnimation.bind(this),
        connectorAnimation: this.connectorAnimation.bind(this),
        connectorAnimate: this.connectorAnimate.bind(this),
        resetConnectorAnimation: this.resetConnectorAnimation.bind(this),
        finishConnectorAnimation: this.finishConnectorAnimation.bind(this),
        disable: this.disableElement.bind(this),
        enable: this.enableElement.bind(this),
        callAction: this.callAction.bind(this),
        setValue: this.setValue.bind(this),
        unitSymbol: this.unitSymbol.bind(this),
        convertUnitValue: this.convertUnitValue.bind(this),
      },
      tags: {},
      properties: {},
      values: {},
      svg: this.svgShape
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
      if (property.type !== FormPropertyType.htmlSection) {
        this.context.properties[property.id] = this.getPropertyValue(this.metadata.properties, this.settings.properties, property.id);
      }
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

  private unitSymbol(unit: TbUnit): string {
    return this.ctx.unitService.getTargetUnitSymbol(unit);
  }

  private convertUnitValue(value: number, unit: TbUnit): number {
    return this.ctx.unitService.convertUnitValue(value, unit);
  }

  private formatValue(value: any, settings: ValueFormatSettings): string;
  private formatValue(value: any, dec?: number, units?: string, showZeroDecimals?: boolean): string | undefined;
  private formatValue(value: any, settingsOrDec?: ValueFormatSettings | number, units?: string, showZeroDecimals?: boolean): string {
    let valueFormatSettings: ValueFormatSettings;
    if (typeof settingsOrDec === 'object') {
      valueFormatSettings = deepClone(settingsOrDec);
    } else {
      valueFormatSettings = {
        units,
        decimals: settingsOrDec,
        showZeroDecimals
      }
    }
    const id = objectHashCode(valueFormatSettings) + '';
    if (!this.valueProcessor[id]) {
      this.valueProcessor[id] = ValueFormatProcessor.fromSettings(this.ctx.$injector, valueFormatSettings);
    }
    return this.valueProcessor[id].format(value);
  }

  private onStateValueChanged(id: string, value: any) {
    if (this.context.values[id] !== value) {
      this.context.values[id] = value;
      this.renderState();
    }
  }

  private renderState(): void {
    try {
      this.metadata.stateRender(this.context, this.svgShape);
    } catch (e) {
      console.error(e);
    }
    for (const tag of this.metadata.tags) {
      const elements = this.context.tags[tag.tag];
      if (elements) {
        elements.forEach(element => {
          try {
            tag.stateRender(this.context, element);
          } catch (e) {
            console.error(e);
          }
        });
      }
    }
  }

  private normalizeValue(value: any, type: ValueType): any {
    if (isUndefinedOrNull(value)) {
        return defaultValueForValueType(type);
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

  private setElementIcon(e: Element | Element[],
                         icon: string,
                         size = 12,
                         color = '#0000008A',
                         center = true) {
    this.elements(e).forEach(element => {
      if (element.type === 'g') {
        let skip = false;
        const firstChild = element.first();
        if (firstChild) {
          const iconData: {icon: string; size: number; color: string} = firstChild.remember('iconData');
          if (iconData && iconData.icon === icon && iconData.size === size && iconData.color === color) {
            skip = true;
          }
        }
        if (!skip) {
          element.clear();
          this.createIconElement(icon, size, color).subscribe((iconElement) => {
            if (iconElement) {
              iconElement.remember('iconData', {icon, size, color});
              element.add(iconElement);
              if (center) {
                const box = iconElement.bbox();
                iconElement.translate(-box.cx, -box.cy);
              }
            }
          });
        }
      }
    });
  }

  private createIconElement(icon: string, size: number, color: string): Observable<Element> {
    const isSvg = isSvgIcon(icon);
    if (isSvg) {
      const [namespace, iconName] = splitIconName(icon);
      return this.iconRegistry
      .getNamedSvgIcon(iconName, namespace)
      .pipe(
        take(1),
        map((svgElement) => {
          const element = new Element(svgElement.firstChild);
          const box = element.bbox();
          const scale = size / box.height;
          element.scale(scale);
          element.fill(color);
          return element;
        }),
        catchError(() => of(null)
      ));
    } else {
      const iconName = splitIconName(icon)[1];
      const textElement = this.svgShape.text(iconName);
      const fontSetClasses = (
        this.iconRegistry.getDefaultFontSetClass()
      ).filter(className => className.length > 0);
      fontSetClasses.forEach(className => textElement.addClass(className));
      textElement.font({size: `${size}px`});
      textElement.attr({
        style: `font-size: ${size}px`,
        'text-anchor': 'start'
      });
      textElement.fill(color);
      const tspan = textElement.first();
      tspan.attr({
        'dominant-baseline': 'hanging'
      });
      return of(textElement);
    }
  }

  private cssAnimate(element: Element, duration: number): ScadaSymbolAnimation {
    return this.cssAnimations.animate(element, duration);
  }

  private cssAnimation(element: Element): ScadaSymbolAnimation | undefined {
    return this.cssAnimations.animation(element);
  }

  private resetCssAnimation(element: Element) {
    this.cssAnimations.resetAnimation(element);
  }

  private finishCssAnimation(element: Element) {
    this.cssAnimations.finishAnimation(element);
  }

  private connectorAnimate(element: Element, path: string, reversedPath: string): ConnectorScadaSymbolAnimation {
    return this.connectorAnimations.animate(element, path, reversedPath);
  }

  private connectorAnimation(element: Element): ConnectorScadaSymbolAnimation | undefined {
    return this.connectorAnimations.animation(element);
  }

  private resetConnectorAnimation(element: Element) {
    this.connectorAnimations.resetAnimation(element);
  }

  private finishConnectorAnimation(element: Element) {
    this.connectorAnimations.finishAnimation(element);
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

  private getProperty(properties: FormProperty[], ...ids: string[]): FormProperty {
    let found: FormProperty;
    for (const id of ids) {
      if (properties) {
        found = properties.find(p => p.id === id);
        if (found && found.type === FormPropertyType.fieldset) {
          properties = found.properties;
        } else {
          properties = null;
        }
      } else {
        found = null;
      }
    }
    return found;
  }

  private getSettingsValue(settings: {[id: string]: any}, ...ids: string[]): any {
    let found: any;
    let properties = settings;
    for (const id of ids) {
      if (properties) {
        found = properties[id];
        if (found && typeof found === 'object') {
          properties = found;
        } else {
          properties = null;
        }
      } else {
        found = null;
      }
    }
    return found;
  }

  private getPropertyValue(properties: FormProperty[], settings: {[id: string]: any}, ...ids: string[]): any {
    const property = this.getProperty(properties, ...ids);
    if (property) {
      if (property.type === FormPropertyType.array) {
        const arrayValue = [];
        if (property.arrayItemType !== FormPropertyType.htmlSection) {
          const settingsValue = this.getSettingsValue(settings, ...ids);
          if (settingsValue && Array.isArray(settingsValue)) {
            for (const settingsElement of settingsValue) {
              let value: any;
              if (property.arrayItemType === FormPropertyType.fieldset) {
                const propertyValue: {[id: string]: any} = {};
                for (const childProperty of property.properties) {
                  if (childProperty.type !== FormPropertyType.htmlSection) {
                    propertyValue[childProperty.id] = this.getPropertyValue(property.properties, settingsElement, childProperty.id);
                  }
                }
                value = propertyValue;
              } else {
                value = this.convertPropertyValue(property, settingsElement);
              }
              arrayValue.push(value);
            }
          }
        }
        return arrayValue;
      } else if (property.type === FormPropertyType.fieldset) {
        const propertyValue: {[id: string]: any} = {};
        for (const childProperty of property.properties) {
          if (childProperty.type !== FormPropertyType.htmlSection) {
            propertyValue[childProperty.id] = this.getPropertyValue(properties, settings, ...ids, childProperty.id);
          }
        }
        return propertyValue;
      } else {
        const value = this.getSettingsValue(settings, ...ids);
        return this.convertPropertyValue(property, value);
      }
    } else {
      return '';
    }
  }

  private convertPropertyValue(property: FormProperty, value: any): any {
    if (isDefinedAndNotNull(value)) {
      if (property.type === FormPropertyType.color_settings) {
        return ColorProcessor.fromSettings(value);
      } else if ([FormPropertyType.text, FormPropertyType.textarea].includes(property.type)) {
        const result = this.ctx.utilsService.customTranslation(value, value);
        const entityInfo = this.ctx.defaultSubscription.getFirstEntityInfo();
        return createLabelFromSubscriptionEntityInfo(entityInfo, result);
      }
      return value;
    } else {
      switch (property.type) {
        case FormPropertyType.color_settings:
          return ColorProcessor.fromSettings(constantColor('#000'));
        case FormPropertyType.font:
        case FormPropertyType.units:
        case FormPropertyType.icon:
          return null;
        default:
          return defaultPropertyValue(property.type);
      }
    }
  }
}

const scadaSymbolAnimationId = 'scadaSymbolAnimation';

interface ScadaSymbolAnimation {

  running(): boolean;

  play(): void;
  pause(): void;
  stop(): void;
  finish(): void;

  speed(speed: number): ScadaSymbolAnimation;
  ease(easing: string): ScadaSymbolAnimation;
  loop(times?: number, swing?: boolean): ScadaSymbolAnimation;

  transform(transform: MatrixTransformParam, relative?: boolean): ScadaSymbolAnimation;
  rotate(r: number, cx?: number, cy?: number): ScadaSymbolAnimation;
  x(x: number): ScadaSymbolAnimation;
  y(y: number): ScadaSymbolAnimation;
  size(width: number, height: number): ScadaSymbolAnimation;
  width(width: number): ScadaSymbolAnimation;
  height(height: number): ScadaSymbolAnimation;
  move(x: number, y: number): ScadaSymbolAnimation;
  dmove(dx: number, dy: number): ScadaSymbolAnimation;
  relative(x: number, y: number): ScadaSymbolAnimation;
  scale(x: number, y?: number, cx?: number, cy?: number): ScadaSymbolAnimation;
  attr(attr: string | object, value?: any): ScadaSymbolAnimation;

}

const scadaSymbolConnectorFlowAnimationId = 'scadaSymbolConnectorFlowAnimation';

type StrokeLineCap = 'butt' | 'round '| 'square';

interface ConnectorScadaSymbolAnimation {
  play(): void;
  stop(): void;
  finish(): void;

  flowAppearance(width: number, color: string, lineCap: StrokeLineCap, dashWidth: number, dashGap: number): ConnectorScadaSymbolAnimation;
  duration(speed: number): ConnectorScadaSymbolAnimation;
  direction(direction: boolean): ConnectorScadaSymbolAnimation;
}

class CssScadaSymbolAnimations {
  constructor(private svgShape: Svg,
              private raf: RafService) {}

  public animate(element: Element, duration = 1000): ScadaSymbolAnimation {
    this.checkOldAnimation(element);
    return this.setupAnimation(element, this.createAnimation(element, duration));
  }

  public animation(element: Element): ScadaSymbolAnimation | undefined {
    return element.remember(scadaSymbolAnimationId);
  }

  public resetAnimation(element: Element) {
    const animation: ScadaSymbolAnimation = element.remember(scadaSymbolAnimationId);
    if (animation) {
      animation.stop();
      element.remember(scadaSymbolAnimationId, null);
    }
  }

  public finishAnimation(element: Element) {
    const animation: ScadaSymbolAnimation = element.remember(scadaSymbolAnimationId);
    if (animation) {
      animation.finish();
      element.remember(scadaSymbolAnimationId, null);
    }
  }

  private checkOldAnimation(element: Element) {
    const previousAnimation: ScadaSymbolAnimation = element.remember(scadaSymbolAnimationId);
    if (previousAnimation) {
      previousAnimation.finish();
    }
  }

  private setupAnimation(element: Element, animation: ScadaSymbolAnimation): ScadaSymbolAnimation {
    element.remember(scadaSymbolAnimationId, animation);
    return animation;
  }

  private createAnimation(element: Element, duration: number): ScadaSymbolAnimation {
    const fallbackToJs = (isSafari() || isFirefox()) && element.type === 'pattern';
    if (fallbackToJs) {
      return new JsScadaSymbolAnimation(element, duration);
    } else {
      return new CssScadaSymbolAnimation(this.svgShape, this.raf, element, duration);
    }
  }
}

class ScadaSymbolFlowConnectorAnimations {
  constructor() {}

  public animate(element: Element, path = '', reversedPath = ''): ConnectorScadaSymbolAnimation {
    this.checkOldAnimation(element);
    return this.setupAnimation(element, this.createAnimation(element, path, reversedPath));
  }

  public animation(element: Element): ConnectorScadaSymbolAnimation | undefined {
    return element.remember(scadaSymbolConnectorFlowAnimationId);
  }

  public resetAnimation(element: Element) {
    const animation: ConnectorScadaSymbolAnimation = element.remember(scadaSymbolConnectorFlowAnimationId);
    if (animation) {
      animation.stop();
      element.remember(scadaSymbolConnectorFlowAnimationId, null);
    }
  }

  public finishAnimation(element: Element) {
    const animation: ConnectorScadaSymbolAnimation = element.remember(scadaSymbolConnectorFlowAnimationId);
    if (animation) {
      animation.finish();
      element.remember(scadaSymbolConnectorFlowAnimationId, null);
    }
  }

  private setupAnimation(element: Element, animation: ConnectorScadaSymbolAnimation): ConnectorScadaSymbolAnimation {
    element.remember(scadaSymbolConnectorFlowAnimationId, animation);
    return animation;
  }

  private checkOldAnimation(element: Element) {
    const previousAnimation: ConnectorScadaSymbolAnimation = element.remember(scadaSymbolConnectorFlowAnimationId);
    if (previousAnimation) {
      previousAnimation.finish();
    }
  }

  private createAnimation(element: Element, path: string, reversedPath: string): ConnectorScadaSymbolAnimation {
    return new FlowConnectorAnimation(element, path, reversedPath);
  }
}

class FlowConnectorAnimation implements ConnectorScadaSymbolAnimation {

  private readonly _path: string;
  private readonly _reversedPath: string;
  private readonly _animation: Element;

  private _duration: number = 1;
  private _lineColor: string = '#C8DFF7';
  private _lineWidth: number = 4;
  private _strokeLineCap: StrokeLineCap = 'butt';
  private _dashWidth: number = 10;
  private _dashGap: number = 10;
  private _direction: boolean = true;

  constructor(private element: Element,
              path: string,
              pathReversed: string) {
    this._path = path;
    this._reversedPath = pathReversed;

    const dashArray = `${this._dashWidth} ${this._dashGap}`;
    const values = `${this._dashWidth + this._dashGap};0`;

    this._animation = SVG(
      `<path d="${this._path}" stroke-dasharray="${dashArray}" stroke-linecap="${this._strokeLineCap}" fill="none" stroke="${this._lineColor}" stroke-width="${this._lineWidth}">` +
      `<animate attributeName="stroke-dashoffset" values="${values}" dur="${this._duration}s" begin="indefinite" calcMode="linear" repeatCount="indefinite"></animate></path>`
    );
  }

  public play() {
    if (!this.element.node.childElementCount) {
      this.element.add(this._animation);
    }
    const animateElement = this.element.node.getElementsByTagName('animate')[0];
    const offset = ((Date.now() - syncTime) % 1000) * -1;
    (animateElement as SVGAnimationElement).beginElementAt(offset);
  }

  public stop() {
    const animateElement = this.element.node.getElementsByTagName('animate')[0];
    (animateElement as SVGAnimationElement)?.endElement();
  }

  public finish() {
    this.element.findOne('path')?.remove();
  }

  public flowAppearance(width: number, color: string, linecap: StrokeLineCap, dashWidth: number, dashGap: number): this {
    const totalLength = (this._animation.node as SVGPathElement).getTotalLength();
    let offset = 0;
    if ((totalLength % 100) !== 0) {
      const clientWidth = totalLength < 100 ? 100 : this.element.node.ownerSVGElement.clientWidth;
      const clientWidthDash = clientWidth  / (dashWidth + dashGap);
      const totalLengthDash = totalLength / clientWidthDash;
      offset = ((dashWidth + dashGap) - totalLengthDash) / 2;
    }
    this._lineColor = color;
    this._lineWidth = width;
    this._strokeLineCap = linecap;
    this._dashWidth = dashWidth - offset;
    this._dashGap = dashGap - offset;
    const dashArray = `${this._dashWidth} ${this._dashGap}`;
    const values = `${this._dashWidth + (this._dashGap || this._dashWidth)};0`;
    this._animation.stroke({width, color, linecap, dasharray: dashArray});
    this._animation.findOne('animate').attr('values', values);
    return this;
  }

  public duration(speed: number): this {
    this._duration = speed;
    this._animation.findOne('animate').attr('dur', `${speed}s`);
    return this;
  }

  public direction(direction: boolean): this {
    this._direction = direction;
    this._animation.attr('d', direction ? this._path : this._reversedPath);
    return this;
  }
}

interface ScadaSymbolAnimationKeyframe {
  stop: string;
  style: any;
}

class CssScadaSymbolAnimation implements ScadaSymbolAnimation {

  private _animationName: string;
  private _animationStyle: Style;

  private _active = false;
  private _running = true;
  private _speed = 1;
  private readonly _duration: number = 1000;
  private _easing = 'linear';
  private _times = 1;
  private _swing = false;

  private _hasAnimations = false;
  private _transform: MatrixTransformParam;
  private _relative: boolean;
  private _initialTransform: MatrixExtract;
  private _transformOriginX: number = null;
  private _transformOriginY: number = null;
  private _attrs: any;

  private _startAttrs: any;
  private _endAttrs: any;

  private _caf = null;

  constructor(private svgShape: Svg,
              private raf: RafService,
              private element: Element,
              duration = 1000)  {
    this._duration = duration;
    this.fixPatternAnimationForChrome();
  }

  private fixPatternAnimationForChrome(): void {
    try {
      const userAgent = window.navigator.userAgent;
      if (+(/Chrome\/(\d+)/i.exec(userAgent)[1]) > 0) {
        if (this.svgShape.defs().findOne('pattern')  && !this.svgShape.defs().findOne('pattern.empty-animation')) {
          this.svgShape.defs().add(SVG('<pattern class="empty-animation"></pattern>'));
          this.svgShape.style()
            .rule('.' + 'empty-animation',
              {'animation-name': 'empty-animation', 'animation-duration': '1000ms', 'animation-iteration-count': 'infinite'})
            .addText('@keyframes empty-animation {0% {<!--opacity:1;-->}100% {<!--opacity:1;-->}}');
        }
      }
    } catch (e) {}
  }

  public running(): boolean {
    return this._active && this._running;
  }

  public play() {
    if (!this._running) {
      this._running = true;
      this.updateAnimationStyle('animation-play-state', this.playStateStyle());
    }
  }

  public pause() {
    if (this._running) {
      this._running = false;
      this.updateAnimationStyle('animation-play-state', this.playStateStyle());
    }
  }

  public stop() {
    this._running = false;
    if (this._hasAnimations) {
      this.destroy();
      this.applyStartAttrs();
    }
  }

  public finish() {
    this._running = false;
    if (this._hasAnimations) {
      this.destroy();
    }
  }

  public speed(speed: number): this {
    this._speed = speed;
    this.updateAnimationStyle('animation-duration',
      this.durationStyle());
    this.updateAnimationStyle('animation-play-state', this.playStateStyle());
    return this;
  }

  public ease(easing: string | EasingLiteral): this {
    this._easing = this.easingLiteralToCssEasing(easing);
    this.updateAnimationStyle('animation-timing-function', this._easing);
    return this;
  }

  public loop(times = 0, swing = false): this {
    this._times = times;
    this._swing = swing;
    if (this._animationStyle) {
      this.createOrUpdateAnimation();
    }
    return this;
  }

  public transform(transform: MatrixTransformParam, relative = false): this {
    this._hasAnimations = true;
    for (const key of Object.keys(transform)) {
      const val = transform[key];
      if (!isFinite(val) && !Array.isArray(val)) {
        delete transform[key];
      }
    }
    if (this._transform) {
      this._transform = Object.assign(this._transform, transform);
    } else {
      this._transform = deepClone(transform);
    }
    this._relative = relative;
    this.createOrUpdateAnimation();
    return this;
  }

  public rotate(r: number, cx?: number, cy?: number): this {
    return this.transform({rotate: r, ox: cx, oy: cy}, true);
  }

  public x(x: number): this {
    return this.transform({translateX: x});
  }

  public y(y: number): this {
    return this.transform({translateY: y});
  }

  public size(width: number, height: number): this {
    const box = this.element.bbox();
    if (width == null || height == null) {
      if (width == null) {
        width = box.width / box.height * height;
      } else if (height == null) {
        height = box.height / box.width * width;
      }
    }
    const scaleX = width / box.width;
    const scaleY = height / box.height;
    return this.scale(scaleX, scaleY);
  }

  public width(width: number): this {
    return this.size(width, this.element.bbox().height);
  }

  public height(height: number): this {
    return this.size(this.element.bbox().width, height);
  }

  public move(x: number, y: number): this {
    const box = this.element.bbox();
    const dx = x - box.x;
    const dy = y - box.y;
    return this.dmove(dx, dy);
  }

  public dmove(dx: number, dy: number): this {
    return this.transform({translateX: dx, translateY: dy}, true);
  }

  public relative(x: number, y: number): this {
    return this.transform({translateX: x, translateY: y}, true);
  }

  public scale(x: number, y?: number, cx?: number, cy?: number): this {
    return this.transform({scaleX: x, scaleY: isUndefined(y) ? x : y, ox: cx, oy: cy}, true);
  }

  public attr(attr: string | object, value?: any): this {
    this._hasAnimations = true;
    if (!this._attrs) {
      this._attrs = {};
    }
    if (typeof attr === 'object') {
      for (const key of Object.keys(attr)) {
        this._attrs[key] = attr[key];
      }
    } else {
      this._attrs[attr] = value;
    }
    this.createOrUpdateAnimation();
    return this;
  }

  private createOrUpdateAnimation() {
    this.destroy();
    this._caf = this.raf.raf(() => this.prepareAnimation());
  }

  private prepareAnimation() {
    this._active = true;
    this.prepareTransform();
    this.prepareStartEndAttrs();
    this._animationName = 'animation_' + generateElementId();
    this.element.on('animationend', (evt) => {
      if ((evt as any).animationName === this._animationName) {
        this.destroy();
      }
    });
    this._animationStyle = this.svgShape.style();
    const styles = {
        'animation-name': this._animationName,
        'animation-duration': this.durationStyle(),
        'animation-timing-function': this._easing,
        'animation-iteration-count': this._times === 0 ? 'infinite' : this._times,
        'animation-play-state': this.playStateStyle()
    };
    this._animationStyle.rule('.' + this._animationName, styles);

    const keyframes = this.animationKeyframes();
    let keyframesCss = `\n@keyframes ${this._animationName} {\n`;
    for (const keyframe of keyframes) {
      let keyframeCss = `  ${keyframe.stop} {\n`;
      for (const i of Object.keys(keyframe.style)) {
        keyframeCss += '    ' + i + ':' + keyframe.style[i] + ';\n';
      }
      keyframeCss += '  }\n';
      keyframesCss += keyframeCss;
    }
    keyframesCss += '}';
    this._animationStyle.addText(keyframesCss);
    setTimeout(() => {
      this.element.addClass(this._animationName);
      if (!this._swing) {
        this.applyEndAttrs();
      }
    }, 0);
  }

  private destroy() {
    this.element.off('animationend');
    this._active = false;
    if (this._caf) {
      this._caf();
      this._caf = null;
    }
    if (this._animationStyle) {
      this._animationStyle.remove();
      this.element.removeClass(this._animationName);
      this._animationStyle = null;
      this._animationName = null;
    }
  }

  private updateAnimationStyle(attrName: string, value: any) {
    if (this._animationStyle) {
      const styleText = this._animationStyle.node.innerHTML;
      const attrValueRegex = new RegExp(`${attrName}:([^;]+);`);
      this._animationStyle.node.innerHTML = styleText.replace(attrValueRegex, `${attrName}:${value};`);
    }
  }

  private durationStyle(): string {
    return (this._speed > 0 && this._duration > 0) ? Math.round(
      (this._duration / this._speed) * (this._swing ? 2 : 1)
    ) + 'ms' : '1000ms';
  }

  private playStateStyle(): string {
    return (this._running && this._speed > 0) ? 'running' : 'paused';
  }

  private animationKeyframes(): ScadaSymbolAnimationKeyframe[] {
    const keyframes: ScadaSymbolAnimationKeyframe[] = [];
    let startStyle: any = {};
    let endStyle: any = {};
    if (this._transform) {
      const transformed = this.transformedData();
      startStyle = this.cssTransform();
      endStyle = this.cssTransform(transformed);
    }
    if (this._attrs) {
      startStyle = {...startStyle, ...this.currentCssAttrs()};
      endStyle = {...endStyle, ...this.toCssAttrs(this._attrs)};
    }
    keyframes.push({
      stop: '0%',
      style: startStyle
    });
    if (this._swing) {
      keyframes.push(...[
        {
          stop: '50%',
          style: endStyle
        },
        {
          stop: '100%',
          style: startStyle
        }]
      );
    } else {
      keyframes.push({
        stop: '100%',
        style: endStyle
      });
    }
    return keyframes;
  }

  private prepareStartEndAttrs() {
    if (this._attrs) {
      this._startAttrs = {...this._startAttrs, ...this.currentSvgAttrs()};
      this._endAttrs = {...this._endAttrs, ...this._attrs};
    }
  }

  private applyStartAttrs() {
    if (this._initialTransform) {
      this.element.transform(this._initialTransform);
    }
    if (this._startAttrs) {
      this.element.attr(this._startAttrs);
    }
  }

  private applyEndAttrs() {
    if (this._transform) {
      this.element.transform(this._transform, this._relative);
    }
    if (this._endAttrs) {
      this.element.attr(this._endAttrs);
    }
  }

  private prepareTransform() {
    if (this._transform) {
      this._transformOriginX = this.element.cx();
      this._transformOriginY = this.element.cy();
      if (isDefinedAndNotNull(this._transform.originX)) {
        this._transformOriginX = this._transform.originX;
      } else if (isDefinedAndNotNull(this._transform.ox)) {
        this._transformOriginX = this._transform.ox;
      }
      if (isDefinedAndNotNull(this._transform.originY)) {
        this._transformOriginY = this._transform.originY;
      } else if (isDefinedAndNotNull(this._transform.oy)) {
        this._transformOriginX = this._transform.oy;
      }

      this._transformOriginX = this.normFloat(this._transformOriginX);
      this._transformOriginY = this.normFloat(this._transformOriginY);

      const transformValue: string = this.element.attr('transform');
      const hasMatrixTransform = transformValue && transformValue.startsWith('matrix');
      if (hasMatrixTransform) {
        const matrix = new Matrix(this.element);
        this._initialTransform = matrix.decompose(this._transformOriginX, this._transformOriginY);
      } else {
        this._initialTransform = this.element.transform();
      }
      this._initialTransform.originX = this._transformOriginX;
      this._initialTransform.originY = this._transformOriginY;
      for (const key of ['translateX', 'translateY', 'scaleX', 'scaleY', 'rotate']) {
        this._initialTransform[key] = this.normFloat(this._initialTransform[key]);
      }
      for (const key of ['b', 'c']) {
        this._initialTransform[key] = this.normFloat(this._initialTransform[key], 0);
      }
    }
  }

  private transformedData(): TransformData {
    const transformed: TransformData = {};
    const transform = this._initialTransform;
    for (const key of Object.keys(this._transform)) {
      if (this._relative) {
        if (['scaleX', 'scaleY'].includes(key)) {
          transformed[key] = this.normFloat(transform[key] * this._transform[key]);
        } else {
          transformed[key] = this.normFloat(transform[key] + this._transform[key]);
        }
      } else {
        transformed[key] = this.normFloat(this._transform[key]);
      }
    }
    return transformed;
  }

  private currentCssAttrs(): any {
    const cssAttrs = {};
    const computed = getComputedStyle(this.element.node);
    for (const key of Object.keys(this._attrs)) {
      const value = computed.getPropertyValue(key);
      if (isDefinedAndNotNull(value)) {
        cssAttrs[key] = value;
      }
    }
    return cssAttrs;
  }

  private currentSvgAttrs(): any {
    const svgAttrs = {};
    for (const key of Object.keys(this._attrs)) {
      const value = this.element.attr(key);
      if (isDefinedAndNotNull(value)) {
        svgAttrs[key] = value;
      }
    }
    return svgAttrs;
  }

  private toCssAttrs(attrs: any): any {
    const cssAttrs: any = {};
    for (const key of Object.keys(attrs)) {
      let val = attrs[key];
      if (['x', 'y', 'width', 'height'].includes(key)) {
        if (isNumeric(val)) {
          val += 'px';
        }
      }
      cssAttrs[key] = val;
    }
    return cssAttrs;
  }

  private cssTransform(inputTransform?: TransformData): any {
    let transform = this._initialTransform || this.element.transform();
    if (inputTransform) {
      transform = deepClone(transform);
      Object.assign(transform, inputTransform);
    }

    return {
      'transform-origin': `${transform.originX}px ${transform.originY}px`,
      transform: `translate(${transform.translateX}px, ${transform.translateY}px) ` +
                 `scale(${transform.scaleX}, ${transform.scaleY}) ` +
                 `rotate(${transform.rotate}deg)`};
  }

  private normFloat(num: number, digits = 2): number {
    const factor = Math.pow(10, digits);
    return Math.round((num + Number.EPSILON) * factor) / factor;
  }

  private easingLiteralToCssEasing(easing: string | EasingLiteral): string {
    switch (easing) {
      case '<>':
        return 'ease-in-out';
      case '-':
        return 'linear';
      case '>':
        return 'ease-out';
      case '<':
        return 'ease-in';
      default:
        return easing;
    }
  }

}

class JsScadaSymbolAnimation implements ScadaSymbolAnimation {

  private readonly _runner: Runner;
  private _timeline: Timeline;
  private _running = true;

  constructor(private element: Element,
              duration = 1000) {
    this._timeline = this.element.timeline();
    this._runner = this.element.animate(duration, 0, 'now').ease('-');
  }

  public runner(): Runner {
    return this._runner;
  }

  public running(): boolean {
    return this._running;
  }

  public play() {
    if (!this._running) {
      this._timeline.play();
      this._running = true;
    }
  }

  public pause() {
    if (this._running) {
      this._timeline.pause();
      this._running = false;
    }
  }

  public stop() {
    this._running = false;
    this._timeline.stop();
    this._timeline = new Timeline();
    this.element.timeline(this._timeline);
  }

  public finish() {
    this._running = false;
    this._timeline.finish();
    this._timeline = new Timeline();
    this.element.timeline(this._timeline);
  }

  public speed(speed: number): this {
    this._timeline.speed(speed);
    return this;
  }

  // Runner methods

  public ease(easing: string): this {
    this._runner.ease(easing as EasingLiteral);
    return this;
  }

  public loop(times: number | TimesParam, swing?: boolean, wait?: number): this {
    if (typeof times === 'object') {
      this._runner.loop(times);
    } else {
      this._runner.loop(times, swing, wait);
    }
    return this;
  }

  public transform(transform: MatrixTransformParam, relative?: boolean): this {
    this._runner.transform(transform, relative);
    return this;
  }

  public rotate(_r: number, _cx?: number, _cy?: number): this {
    (this._runner as any).rotate(...arguments);
    return this;
  }

  public x(x: number): this {
    this._runner.x(x);
    return this;
  }

  public y(y: number): this {
    this._runner.y(y);
    return this;
  }

  public size(width: number, height: number): this {
    this._runner.size(width, height);
    return this;
  }

  public width(width: number): this {
    this._runner.width(width);
    return this;
  }

  public height(height: number): this {
    this._runner.height(height);
    return this;
  }

  public move(x: number, y: number): this {
    this._runner.move(x, y);
    return this;
  }

  public dmove(dx: number, dy: number): this {
    this._runner.dmove(dx, dy);
    return this;
  }

  public relative(_x: number, _y: number): this {
    (this._runner as any).relative(...arguments);
    return this;
  }

  public scale(_x: number, _y?: number, _cx?: number, _cy?: number): this {
    (this._runner as any).scale(...arguments);
    return this;
  }

  public attr(a: string | object, v?: string): this {
    this._runner.attr(a, v);
    return this;
  }

}
