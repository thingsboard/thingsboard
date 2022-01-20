///
/// Copyright © 2016-2021 The Thingsboard Authors
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

import { Widget, WidgetTypeDetails } from '@app/shared/models/widget.models';
import { DashboardLayoutId } from '@shared/models/dashboard.models';
import { WidgetsBundle } from '@shared/models/widgets-bundle.model';

export interface ImportWidgetResult {
  widget: Widget;
  layoutId: DashboardLayoutId;
}

export interface WidgetsBundleItem {
  widgetsBundle: WidgetsBundle;
  widgetTypes: WidgetTypeDetails[];
}

export interface CsvToJsonConfig {
  delim?: string;
  header?: boolean;
}

export interface CsvToJsonResult {
  headers?: string[];
  rows?: any[][];
}

export type CSVDelimiter = ',' | ';' | '|' | '\t';

export enum ImportEntityColumnType {
  name = 'NAME',
  type = 'TYPE',
  label = 'LABEL',
  clientAttribute = 'CLIENT_ATTRIBUTE',
  sharedAttribute = 'SHARED_ATTRIBUTE',
  serverAttribute = 'SERVER_ATTRIBUTE',
  timeseries = 'TIMESERIES',
  accessToken = 'ACCESS_TOKEN',
  x509 = 'X509',
  mqttClientId = 'MQTT_CLIENT_ID',
  mqttUserName = 'MQTT_USER_NAME',
  mqttPassword = 'MQTT_PASSWORD',
  lwm2mClientEndpoint = 'LWM2M_CLIENT_ENDPOINT',
  lwm2mClientSecurityConfigMode = 'LWM2M_CLIENT_SECURITY_CONFIG_MODE',
  lwm2mClientIdentity = 'LWM2M_CLIENT_IDENTITY',
  lwm2mClientKey = 'LWM2M_CLIENT_KEY',
  lwm2mClientCert = 'LWM2M_CLIENT_CERT',
  lwm2mBootstrapServerSecurityMode = 'LWM2M_BOOTSTRAP_SERVER_SECURITY_MODE',
  lwm2mBootstrapServerClientPublicKeyOrId = 'LWM2M_BOOTSTRAP_SERVER_PUBLIC_KEY_OR_ID',
  lwm2mBootstrapServerClientSecretKey = 'LWM2M_BOOTSTRAP_SERVER_SECRET_KEY',
  lwm2mServerSecurityMode = 'LWM2M_SERVER_SECURITY_MODE',
  lwm2mServerClientPublicKeyOrId = 'LWM2M_SERVER_CLIENT_PUBLIC_KEY_OR_ID',
  lwm2mServerClientSecretKey = 'LWM2M_SERVER_CLIENT_SECRET_KEY',
  isGateway = 'IS_GATEWAY',
  description = 'DESCRIPTION',
  edgeLicenseKey = 'EDGE_LICENSE_KEY',
  cloudEndpoint = 'CLOUD_ENDPOINT',
  routingKey = 'ROUTING_KEY',
  secret = 'SECRET'
}

export const importEntityObjectColumns =
  [ImportEntityColumnType.name, ImportEntityColumnType.type, ImportEntityColumnType.accessToken];

export const importEntityColumnTypeTranslations = new Map<ImportEntityColumnType, string>(
  [
    [ImportEntityColumnType.name, 'import.column-type.name'],
    [ImportEntityColumnType.type, 'import.column-type.type'],
    [ImportEntityColumnType.label, 'import.column-type.label'],
    [ImportEntityColumnType.clientAttribute, 'import.column-type.client-attribute'],
    [ImportEntityColumnType.sharedAttribute, 'import.column-type.shared-attribute'],
    [ImportEntityColumnType.serverAttribute, 'import.column-type.server-attribute'],
    [ImportEntityColumnType.timeseries, 'import.column-type.timeseries'],
    [ImportEntityColumnType.accessToken, 'import.column-type.access-token'],
    [ImportEntityColumnType.x509, 'import.column-type.x509'],
    [ImportEntityColumnType.mqttClientId, 'import.column-type.mqtt.client-id'],
    [ImportEntityColumnType.mqttUserName, 'import.column-type.mqtt.user-name'],
    [ImportEntityColumnType.mqttPassword, 'import.column-type.mqtt.password'],
    [ImportEntityColumnType.lwm2mClientEndpoint, 'import.column-type.lwm2m.client-endpoint'],
    [ImportEntityColumnType.lwm2mClientSecurityConfigMode, 'import.column-type.lwm2m.security-config-mode'],
    [ImportEntityColumnType.lwm2mClientIdentity, 'import.column-type.lwm2m.client-identity'],
    [ImportEntityColumnType.lwm2mClientKey, 'import.column-type.lwm2m.client-key'],
    [ImportEntityColumnType.lwm2mClientCert, 'import.column-type.lwm2m.client-cert'],
    [ImportEntityColumnType.lwm2mBootstrapServerSecurityMode, 'import.column-type.lwm2m.bootstrap-server-security-mode'],
    [ImportEntityColumnType.lwm2mBootstrapServerClientPublicKeyOrId, 'import.column-type.lwm2m.bootstrap-server-public-key-id'],
    [ImportEntityColumnType.lwm2mBootstrapServerClientSecretKey, 'import.column-type.lwm2m.bootstrap-server-secret-key'],
    [ImportEntityColumnType.lwm2mServerSecurityMode, 'import.column-type.lwm2m.lwm2m-server-security-mode'],
    [ImportEntityColumnType.lwm2mServerClientPublicKeyOrId, 'import.column-type.lwm2m.lwm2m-server-public-key-id'],
    [ImportEntityColumnType.lwm2mServerClientSecretKey, 'import.column-type.lwm2m.lwm2m-server-secret-key'],
    [ImportEntityColumnType.isGateway, 'import.column-type.isgateway'],
    [ImportEntityColumnType.description, 'import.column-type.description'],
    [ImportEntityColumnType.edgeLicenseKey, 'import.column-type.edge-license-key'],
    [ImportEntityColumnType.cloudEndpoint, 'import.column-type.cloud-endpoint'],
    [ImportEntityColumnType.routingKey, 'import.column-type.routing-key'],
    [ImportEntityColumnType.secret, 'import.column-type.secret']
  ]
);

export interface CsvColumnParam {
  type: ImportEntityColumnType;
  key: string;
  sampleData: any;
}

export interface ColumnMapping {
  type: ImportEntityColumnType;
  key?: string;
}

export interface BulkImportRequest {
  file: string;
  mapping: {
    columns: Array<ColumnMapping>;
    delimiter: CSVDelimiter;
    header: boolean;
    update: boolean;
  };
}

export interface BulkImportResult {
  created: number;
  updated: number;
  errors: number;
  errorsList: Array<string>;
}

export interface FileType {
  mimeType: string;
  extension: string;
}

export const JSON_TYPE: FileType = {
  mimeType: 'text/json',
  extension: 'json'
};

export const ZIP_TYPE: FileType = {
  mimeType: 'application/zip',
  extension: 'zip'
};

export function convertCSVToJson(csvdata: string, config: CsvToJsonConfig,
                                 onError: (messageId: string, params?: any) => void): CsvToJsonResult | number {
  config = config || {};
  const delim = config.delim || ',';
  const header = config.header || false;
  const result: CsvToJsonResult = {};
  const csvlines = csvdata.split(/[\r\n]+/);
  const csvheaders = splitCSV(csvlines[0], delim);
  if (csvheaders.length < 2) {
    onError('import.import-csv-number-columns-error');
    return -1;
  }
  const csvrows = header ? csvlines.slice(1, csvlines.length) : csvlines;
  result.headers = csvheaders;
  result.rows = [];
  for (const row of csvrows) {
    if (row.length === 0) {
      break;
    }
    const rowitems: any[] = splitCSV(row, delim);
    if (rowitems.length !== result.headers.length) {
      onError('import.import-csv-invalid-format-error', {line: (header ? result.rows.length + 2 : result.rows.length + 1)});
      return -1;
    }
    for (let i = 0; i < rowitems.length; i++) {
      rowitems[i] = convertStringToJSType(rowitems[i]);
    }
    result.rows.push(rowitems);
  }
  return result;
}

function splitCSV(str: string, sep: string): string[] {
  let foo: string[];
  let x: number;
  let tl: string;
  for (foo = str.split(sep = sep || ','), x = foo.length - 1, tl; x >= 0; x--) {
    if (foo[x].replace(/"\s+$/, '"').charAt(foo[x].length - 1) === '"') {
      if ((tl = foo[x].replace(/^\s+"/, '"')).length > 1 && tl.charAt(0) === '"') {
        foo[x] = foo[x].replace(/^\s*"|"\s*$/g, '').replace(/""/g, '"');
      } else if (x) {
        foo.splice(x - 1, 2, [foo[x - 1], foo[x]].join(sep));
      } else {
        foo = foo.shift().split(sep).concat(foo);
      }
    } else {
      foo[x].replace(/""/g, '"');
    }
  }
  return foo;
}

function isNumeric(str: any): boolean {
  str = str.replace(',', '.');
  return (str - parseFloat(str) + 1) >= 0 && Number(str).toString() === str;
}

function convertStringToJSType(str: string): any {
  if (isNumeric(str.replace(',', '.'))) {
    return parseFloat(str.replace(',', '.'));
  }
  if (str.search(/^(true|false)$/im) === 0) {
    return str.toLowerCase() === 'true';
  }
  if (str === '') {
    return null;
  }
  return str;
}
