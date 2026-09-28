// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { BaseData, ExportableEntity } from '@shared/models/base-data';
import { TenantId } from '@shared/models/id/tenant-id';
import { ConverterId } from '@shared/models/id/converter-id';
import { ContentType } from '@shared/models/constants';
import { ActivatedRouteSnapshot } from '@angular/router';
import { IntegrationType } from '@shared/models/integration.models';
import { ScriptLanguage } from '@shared/models/rule-node.models';
import { HasEntityDebugSettings } from '@shared/models/entity.models';
import { EntityType } from '@shared/models/entity-type.models';

export enum ConverterType {
  UPLINK = 'UPLINK',
  DOWNLINK = 'DOWNLINK'
}

export const IntegrationTbelDefaultConvertersUrl = new Map<IntegrationType, string>([
  [IntegrationType.CHIRPSTACK, '/assets/converters/tbel-chirpstack-decoder.raw'],
  [IntegrationType.LORIOT, '/assets/converters/tbel-loriot-decoder.raw'],
  [IntegrationType.TTI,'/assets/converters/tbel-tti-decoder.raw'],
  [IntegrationType.TTN, '/assets/converters/tbel-ttn-decoder.raw'],
  [IntegrationType.SIGFOX, '/assets/converters/tbel-sigfox-decoder.raw'],
  [IntegrationType.AZURE_IOT_HUB, '/assets/converters/tbel-azure-decoder.raw'],
  [IntegrationType.AZURE_EVENT_HUB, '/assets/converters/tbel-azure-decoder.raw'],
  [IntegrationType.AZURE_SERVICE_BUS, '/assets/converters/tbel-azure-decoder.raw'],
  [IntegrationType.AWS_IOT, '/assets/converters/tbel-aws-iot-decoder.raw'],
  [IntegrationType.KPN, '/assets/converters/tbel-kpn-decoder.raw'],
  [IntegrationType.THINGPARK, '/assets/converters/tbel-thingpark-decoder.raw'],
  [IntegrationType.TPE, '/assets/converters/tbel-tpe-decoder.raw'],
  [IntegrationType.MQTT, '/assets/converters/tbel-mqtt-decoder.raw']
]);

export const IntegrationJSDefaultConvertersUrl = new Map<IntegrationType, string>([
  [IntegrationType.CHIRPSTACK, '/assets/converters/js-chirpstack-decoder.raw'],
  [IntegrationType.LORIOT, '/assets/converters/js-loriot-decoder.raw'],
  [IntegrationType.TTI,'/assets/converters/js-tti-decoder.raw'],
  [IntegrationType.TTN, '/assets/converters/js-ttn-decoder.raw'],
  [IntegrationType.THINGPARK, '/assets/converters/js-thingpark-decoder.raw'],
  [IntegrationType.TPE, '/assets/converters/js-tpe-decoder.raw'],
  [IntegrationType.MQTT, '/assets/converters/js-mqtt-decoder.raw']
]);

export const IntegrationTbelDefaultEncodersUrl = new Map<IntegrationType, string>([
  [IntegrationType.OPC_UA, '/assets/converters/tbel-opc-ua-encoder.raw']
])

export const jsDefaultConvertersUrl = new Map<ConverterType, string>([
  [ConverterType.UPLINK, '/assets/converters/js-decoder.raw' ],
  [ConverterType.DOWNLINK, '/assets/converters/js-encoder.raw'],
]);

export const jsDefaultConvertersV2Url = new Map<ConverterType, string>([
  [ConverterType.UPLINK, '/assets/converters/js-decoder-v2.raw'],
  [ConverterType.DOWNLINK, '/assets/converters/js-encoder.raw'],
]);

export const tbelDefaultConvertersUrl = new Map<ConverterType, string>([
  [ConverterType.UPLINK, '/assets/converters/tbel-decoder.raw' ],
  [ConverterType.DOWNLINK, '/assets/converters/tbel-encoder.raw'],
]);

export const tbelDefaultConvertersV2Url = new Map<ConverterType, string>([
  [ConverterType.UPLINK, '/assets/converters/tbel-decoder.raw' ],
  [ConverterType.DOWNLINK, '/assets/converters/tbel-encoder.raw'],
]);

export const DefaultUpdateOnlyKeysValue = ['manufacturer'];
export type DefaultUpdateOnlyKeys = {[key in IntegrationType]?: Array<string>};

export const converterTypeTranslationMap = new Map<ConverterType, string>(
  [
    [ConverterType.UPLINK, 'converter.type-uplink'],
    [ConverterType.DOWNLINK, 'converter.type-downlink'],
  ]
);

export const converterTypeTitleTranslationMap = new Map<ConverterType, string>(
  [
    [ConverterType.UPLINK, 'integration.uplink-converter'],
    [ConverterType.DOWNLINK, 'integration.downlink-converter'],
  ]
);

export type ConverterVersion = 1 | 2;

export interface Converter extends BaseData<ConverterId>, ExportableEntity<ConverterId>, HasEntityDebugSettings {
  tenantId?: TenantId;
  name: string;
  type: ConverterType;
  configuration: ConverterConfig & Partial<ConverterConfigV2>;
  additionalInfo?: any;
  edgeTemplate: boolean;
  integrationType?: IntegrationType;
  converterVersion: ConverterVersion;
}

export interface ConverterConfig {
  scriptLang: ScriptLanguage;
  decoder: string;
  tbelDecoder: string;
  encoder: string;
  tbelEncoder: string;
  updateOnlyKeys: string[];
}

export interface ConverterConfigV2 extends ConverterConfig {
  type: EntityType.DEVICE | EntityType.ASSET;
  name: string;
  profile: string;
  label: string;
  customer: string;
  group: string;
  attributes: string[];
  telemetry: string[];
}

export interface ConverterMsg {
  payload: any;
  metadata: {[key: string]: string};
}

export interface ConvertedInputMsgParams {
  metadata: {[key: string]: string};
  payload: {[key: string]: string};
}

export interface ConvertedInputMsgResult {
  payload: any;
  metadata: {[key: string]: string};
  contentType: ContentType;
  stringContent?: string;
}

export interface TestUpLinkInputParams {
  metadata: {[key: string]: string};
  payload: string;
  decoder: string;
  converter: Converter;
}

export interface TestDownLinkInputParams {
  metadata: {[key: string]: string};
  msg: string;
  msgType: string;
  integrationMetadata: {[key: string]: string};
  encoder: string;
}

export interface LatestConverterParameters {
  converterVersion?: ConverterVersion;
  converterType?: ConverterType;
  integrationType?: IntegrationType;
  integrationName?: string;
}

export type TestConverterInputParams = TestUpLinkInputParams & TestDownLinkInputParams;

export interface TestConverterResult {
  output: string;
  outputMsg?: Record<string, any>;
  error: string;
}

export interface ConverterDebugInput {
  inContentType: ContentType;
  inContent: string;
  inMetadata: string;
  inMsgType: string;
  inIntegrationMetadata: string;
}

export enum ConverterSourceType {
  NEW = 'new',
  EXISTING = 'existing',
  LIBRARY = 'library',
  SKIP = 'skip',
}

export interface ConverterLibraryInfo {
  vendor: string;
  model: string;
}

export interface Vendor {
  name: string;
  logo: string;
}

export interface Model {
  name: string;
  photo: string;
  info: {
    description: string;
    label: string;
    url: string;
  };
  searchText?: string;
}

export function getConverterHelpLink(converter: Converter) {
  let link = 'converters';
  if (converter && converter.type) {
    if (converter.type === ConverterType.UPLINK) {
      link = 'uplinkConverters';
    } else {
      link = 'downlinkConverters';
    }
  }
  return link;
}

export interface ConverterParams {
  converterScope: string;
}

export function resolveConverterParams(route: ActivatedRouteSnapshot): ConverterParams {
  return {
    converterScope: route.data.convertersType ? route.data.convertersType : 'tenant'
  };
}

const getJsTemplateUrl = (
  converterType: ConverterType,
  converterVersion: ConverterVersion
): string => {
  const url =
    converterVersion === 2
      ? jsDefaultConvertersV2Url.get(converterType)
      : jsDefaultConvertersUrl.get(converterType);
  if (!url) {
    throw new Error(
      `JS template URL not found for converterType: ${converterType} and converterVersion: ${converterVersion}`
    );
  }
  return url;
};

const getTbelTemplateUrl = (
  converterType: ConverterType,
  converterVersion: ConverterVersion
): string => {
  const url =
    converterVersion === 2
      ? tbelDefaultConvertersV2Url.get(converterType)
      : tbelDefaultConvertersUrl.get(converterType);
  if (!url) {
    throw new Error(
      `Tbel template URL not found for converterType: ${converterType} and converterVersion: ${converterVersion}`
    );
  }
  return url;
};

export const getTargetField =
  (converterType: ConverterType, scriptLang: ScriptLanguage): string => {
    return scriptLang === ScriptLanguage.TBEL
      ? (converterType === ConverterType.UPLINK ? 'tbelDecoder' : 'tbelEncoder')
      : (converterType === ConverterType.UPLINK ? 'decoder' : 'encoder');
  }

export const getTargetTemplateUrl =
  (converterType: ConverterType, scriptLang: ScriptLanguage,
   integrationType: IntegrationType, converterVersion: ConverterVersion = 1): string => {
    if (scriptLang === ScriptLanguage.JS && converterType === ConverterType.UPLINK && IntegrationJSDefaultConvertersUrl.has(integrationType)) {
      return IntegrationJSDefaultConvertersUrl.get(integrationType)
    } else if (scriptLang === ScriptLanguage.JS) {
      return getJsTemplateUrl(converterType, converterVersion);
    } else if (converterType === ConverterType.UPLINK && IntegrationTbelDefaultConvertersUrl.has(integrationType)) {
      return IntegrationTbelDefaultConvertersUrl.get(integrationType);
    } else if (converterType === ConverterType.DOWNLINK && IntegrationTbelDefaultEncodersUrl.has(integrationType)) {
      return IntegrationTbelDefaultEncodersUrl.get(integrationType);
    }
    return getTbelTemplateUrl(converterType, converterVersion);
  }

const getTbelConverterFunctionHeldId = (converterType: ConverterType, converterVersion: ConverterVersion): string => {
  return converterVersion === 2
    ? (converterType === ConverterType.UPLINK ? 'converter/tbel/decoder_fn_v2' : 'converter/tbel/encoder_fn')
    : (converterType === ConverterType.UPLINK ? 'converter/tbel/decoder_fn' : 'converter/tbel/encoder_fn')
}

const getJsConverterFunctionHeldId = (converterType: ConverterType, converterVersion: ConverterVersion): string => {
  return converterVersion === 2
    ? (converterType === ConverterType.UPLINK ? 'converter/decoder_fn_v2' : 'converter/encoder_fn')
    : (converterType === ConverterType.UPLINK ? 'converter/decoder_fn' : 'converter/encoder_fn')
}

export const getConverterFunctionHeldId =
  (converterType: ConverterType, scriptLang: ScriptLanguage, converterVersion: ConverterVersion): string => {
    return scriptLang === ScriptLanguage.TBEL
      ? getTbelConverterFunctionHeldId(converterType, converterVersion)
      : getJsConverterFunctionHeldId(converterType, converterVersion);
  }

export const getConverterFunctionHeldPopupStyle =
  (converterType: ConverterType): Record<string, string> => {
    return converterType === ConverterType.DOWNLINK ? {width: '700px'} : {width: '1300px'};
  }

export const getConverterFunctionName =
  (converterType: ConverterType, converterVersion: ConverterVersion): string => {
  return converterType === ConverterType.UPLINK
    ? converterVersion === 2 ? 'payloadDecoder' : 'decoder'
    : 'encoder';
}

export const getConverterTestFunctionName =
  (converterType: ConverterType, converterVersion: ConverterVersion): string => {
    return converterType === ConverterType.UPLINK
      ? converterVersion === 2 ? 'converter.test-payload-decoder' : 'converter.test-decoder-fuction'
      : 'converter.test-encoder-fuction';
  }

const decoderArgs = ['payload', 'metadata'];
const encoderArgs = ['msg', 'metadata', 'msgType', 'integrationMetadata'];

export const getConverterFunctionArgs =
  (converterType: ConverterType): string[] => converterType === ConverterType.UPLINK ? decoderArgs : encoderArgs;
