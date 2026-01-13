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

import { EntityField, entityFields } from '@shared/models/entity.models';
import { EntitySearchDirection } from '@shared/models/relation.models';
import { EntityTypeFilter } from '@shared/models/alias.models';

export enum OriginatorSource {
  CUSTOMER = 'CUSTOMER',
  TENANT = 'TENANT',
  RELATED = 'RELATED',
  ALARM_ORIGINATOR = 'ALARM_ORIGINATOR',
  ENTITY = 'ENTITY'
}

export interface OriginatorValuesDescriptions {
  value: OriginatorSource;
  name: string;
  description: string;
}

export const originatorSourceTranslations = new Map<OriginatorSource, string>(
  [
    [OriginatorSource.CUSTOMER, 'rule-node-config.originator-customer'],
    [OriginatorSource.TENANT, 'rule-node-config.originator-tenant'],
    [OriginatorSource.RELATED, 'rule-node-config.originator-related'],
    [OriginatorSource.ALARM_ORIGINATOR, 'rule-node-config.originator-alarm-originator'],
    [OriginatorSource.ENTITY, 'rule-node-config.originator-entity'],
  ]
);

export const originatorSourceDescTranslations = new Map<OriginatorSource, string>(
  [
    [OriginatorSource.CUSTOMER, 'rule-node-config.originator-customer-desc'],
    [OriginatorSource.TENANT, 'rule-node-config.originator-tenant-desc'],
    [OriginatorSource.RELATED, 'rule-node-config.originator-related-entity-desc'],
    [OriginatorSource.ALARM_ORIGINATOR, 'rule-node-config.originator-alarm-originator-desc'],
    [OriginatorSource.ENTITY, 'rule-node-config.originator-entity-by-name-pattern-desc'],
  ]
);
export const allowedOriginatorFields: EntityField[] = [
  entityFields.createdTime,
  entityFields.name,
  {value: 'type', name: 'rule-node-config.profile-name', keyName: 'originatorProfileName'},
  entityFields.firstName,
  entityFields.lastName,
  entityFields.email,
  entityFields.title,
  entityFields.country,
  entityFields.state,
  entityFields.city,
  entityFields.address,
  entityFields.address2,
  entityFields.zip,
  entityFields.phone,
  entityFields.label,
  {value: 'id', name: 'rule-node-config.id', keyName: 'id'},
  {value: 'additionalInfo', name: 'rule-node-config.additional-info', keyName: 'additionalInfo'}
];

export const OriginatorFieldsMappingValues = new Map<string, string>(
  [
    ['type', 'profileName'],
    ['createdTime', 'createdTime'],
    ['name', 'name'],
    ['firstName', 'firstName'],
    ['lastName', 'lastName'],
    ['email', 'email'],
    ['title', 'title'],
    ['country', 'country'],
    ['state', 'state'],
    ['city', 'city'],
    ['address', 'address'],
    ['address2', 'address2'],
    ['zip', 'zip'],
    ['phone', 'phone'],
    ['label', 'label'],
    ['id', 'id'],
    ['additionalInfo', 'additionalInfo'],
  ]
);

export enum PerimeterType {
  CIRCLE = 'CIRCLE',
  POLYGON = 'POLYGON'
}

export const perimeterTypeTranslations = new Map<PerimeterType, string>(
  [
    [PerimeterType.CIRCLE, 'rule-node-config.perimeter-circle'],
    [PerimeterType.POLYGON, 'rule-node-config.perimeter-polygon'],
  ]
);

export enum TimeUnit {
  MILLISECONDS = 'MILLISECONDS',
  SECONDS = 'SECONDS',
  MINUTES = 'MINUTES',
  HOURS = 'HOURS',
  DAYS = 'DAYS'
}

export const timeUnitTranslations = new Map<TimeUnit, string>(
  [
    [TimeUnit.MILLISECONDS, 'rule-node-config.time-unit-milliseconds'],
    [TimeUnit.SECONDS, 'rule-node-config.time-unit-seconds'],
    [TimeUnit.MINUTES, 'rule-node-config.time-unit-minutes'],
    [TimeUnit.HOURS, 'rule-node-config.time-unit-hours'],
    [TimeUnit.DAYS, 'rule-node-config.time-unit-days']
  ]
);

export enum RangeUnit {
  METER = 'METER',
  KILOMETER = 'KILOMETER',
  FOOT = 'FOOT',
  MILE = 'MILE',
  NAUTICAL_MILE = 'NAUTICAL_MILE'
}

export const rangeUnitTranslations = new Map<RangeUnit, string>(
  [
    [RangeUnit.METER, 'rule-node-config.range-unit-meter'],
    [RangeUnit.KILOMETER, 'rule-node-config.range-unit-kilometer'],
    [RangeUnit.FOOT, 'rule-node-config.range-unit-foot'],
    [RangeUnit.MILE, 'rule-node-config.range-unit-mile'],
    [RangeUnit.NAUTICAL_MILE, 'rule-node-config.range-unit-nautical-mile']
  ]
);

export enum EntityDetailsField {
  ID = 'ID',
  TITLE = 'TITLE',
  COUNTRY = 'COUNTRY',
  STATE = 'STATE',
  CITY = 'CITY',
  ZIP = 'ZIP',
  ADDRESS = 'ADDRESS',
  ADDRESS2 = 'ADDRESS2',
  PHONE = 'PHONE',
  EMAIL = 'EMAIL',
  ADDITIONAL_INFO = 'ADDITIONAL_INFO'
}

export interface SvMapOption {
  name: string;
  value: any;
}

export const entityDetailsTranslations = new Map<EntityDetailsField, string>(
  [
    [EntityDetailsField.ID, 'rule-node-config.entity-details-id'],
    [EntityDetailsField.TITLE, 'rule-node-config.entity-details-title'],
    [EntityDetailsField.COUNTRY, 'rule-node-config.entity-details-country'],
    [EntityDetailsField.STATE, 'rule-node-config.entity-details-state'],
    [EntityDetailsField.CITY, 'rule-node-config.entity-details-city'],
    [EntityDetailsField.ZIP, 'rule-node-config.entity-details-zip'],
    [EntityDetailsField.ADDRESS, 'rule-node-config.entity-details-address'],
    [EntityDetailsField.ADDRESS2, 'rule-node-config.entity-details-address2'],
    [EntityDetailsField.PHONE, 'rule-node-config.entity-details-phone'],
    [EntityDetailsField.EMAIL, 'rule-node-config.entity-details-email'],
    [EntityDetailsField.ADDITIONAL_INFO, 'rule-node-config.entity-details-additional_info']
  ]
);

export enum FetchMode {
  FIRST = 'FIRST',
  LAST = 'LAST',
  ALL = 'ALL'
}

export const deduplicationStrategiesTranslations = new Map<FetchMode, string>(
  [
    [FetchMode.FIRST, 'rule-node-config.first'],
    [FetchMode.LAST, 'rule-node-config.last'],
    [FetchMode.ALL, 'rule-node-config.all']
  ]
);

export const deduplicationStrategiesHintTranslations = new Map<FetchMode, string>(
  [
    [FetchMode.FIRST, 'rule-node-config.first-mode-hint'],
    [FetchMode.LAST, 'rule-node-config.last-mode-hint'],
    [FetchMode.ALL, 'rule-node-config.all-mode-hint']
  ]
);

export enum SamplingOrder {
  ASC = 'ASC',
  DESC = 'DESC'
}

export enum DataToFetch {
  ATTRIBUTES = 'ATTRIBUTES',
  LATEST_TELEMETRY = 'LATEST_TELEMETRY',
  FIELDS = 'FIELDS'
}

export const dataToFetchTranslations = new Map<DataToFetch, string>(
  [
    [DataToFetch.ATTRIBUTES, 'rule-node-config.attributes'],
    [DataToFetch.LATEST_TELEMETRY, 'rule-node-config.latest-telemetry'],
    [DataToFetch.FIELDS, 'rule-node-config.fields']
  ]
);

export const msgMetadataLabelTranslations = new Map<DataToFetch, string>(
  [
    [DataToFetch.ATTRIBUTES, 'rule-node-config.add-mapped-attribute-to'],
    [DataToFetch.LATEST_TELEMETRY, 'rule-node-config.add-mapped-latest-telemetry-to'],
    [DataToFetch.FIELDS, 'rule-node-config.add-mapped-fields-to']
  ]
);

export const samplingOrderTranslations = new Map<SamplingOrder, string>(
  [
    [SamplingOrder.ASC, 'rule-node-config.ascending'],
    [SamplingOrder.DESC, 'rule-node-config.descending']
  ]
);

export enum SqsQueueType {
  STANDARD = 'STANDARD',
  FIFO = 'FIFO'
}

export const sqsQueueTypeTranslations = new Map<SqsQueueType, string>(
  [
    [SqsQueueType.STANDARD, 'rule-node-config.sqs-queue-standard'],
    [SqsQueueType.FIFO, 'rule-node-config.sqs-queue-fifo'],
  ]
);

export type credentialsType = 'anonymous' | 'basic' | 'cert.PEM';
export const credentialsTypes: credentialsType[] = ['anonymous', 'basic', 'cert.PEM'];

export const credentialsTypeTranslations = new Map<credentialsType, string>(
  [
    ['anonymous', 'rule-node-config.credentials-anonymous'],
    ['basic', 'rule-node-config.credentials-basic'],
    ['cert.PEM', 'rule-node-config.credentials-pem']
  ]
);

export type AzureIotHubCredentialsType = 'sas' | 'cert.PEM';
export const azureIotHubCredentialsTypes: AzureIotHubCredentialsType[] = ['sas', 'cert.PEM'];

export const azureIotHubCredentialsTypeTranslations = new Map<AzureIotHubCredentialsType, string>(
  [
    ['sas', 'rule-node-config.credentials-sas'],
    ['cert.PEM', 'rule-node-config.credentials-pem']
  ]
);

export enum HttpRequestType {
  GET = 'GET',
  POST = 'POST',
  PUT = 'PUT',
  DELETE = 'DELETE'
}

export const ToByteStandartCharsetTypes = [
  'US-ASCII',
  'ISO-8859-1',
  'UTF-8',
  'UTF-16BE',
  'UTF-16LE',
  'UTF-16'
];

export const ToByteStandartCharsetTypeTranslations = new Map<string, string>(
  [
    ['US-ASCII', 'rule-node-config.charset-us-ascii'],
    ['ISO-8859-1', 'rule-node-config.charset-iso-8859-1'],
    ['UTF-8', 'rule-node-config.charset-utf-8'],
    ['UTF-16BE', 'rule-node-config.charset-utf-16be'],
    ['UTF-16LE', 'rule-node-config.charset-utf-16le'],
    ['UTF-16', 'rule-node-config.charset-utf-16'],
  ]
);

export interface RelationsQuery {
  fetchLastLevelOnly: boolean;
  direction: EntitySearchDirection;
  maxLevel?: number;
  filters?: EntityTypeFilter[];
}

export interface FunctionData {
  value: MathFunction;
  name: string;
  description: string;
  minArgs: number;
  maxArgs: number;
}

export enum MathFunction {
  CUSTOM = 'CUSTOM',
  ADD = 'ADD',
  SUB = 'SUB',
  MULT = 'MULT',
  DIV = 'DIV',
  SIN = 'SIN',
  SINH = 'SINH',
  COS = 'COS',
  COSH = 'COSH',
  TAN = 'TAN',
  TANH = 'TANH',
  ACOS = 'ACOS',
  ASIN = 'ASIN',
  ATAN = 'ATAN',
  ATAN2 = 'ATAN2',
  EXP = 'EXP',
  EXPM1 = 'EXPM1',
  SQRT = 'SQRT',
  CBRT = 'CBRT',
  GET_EXP = 'GET_EXP',
  HYPOT = 'HYPOT',
  LOG = 'LOG',
  LOG10 = 'LOG10',
  LOG1P = 'LOG1P',
  CEIL = 'CEIL',
  FLOOR = 'FLOOR',
  FLOOR_DIV = 'FLOOR_DIV',
  FLOOR_MOD = 'FLOOR_MOD',
  ABS = 'ABS',
  MIN = 'MIN',
  MAX = 'MAX',
  POW = 'POW',
  SIGNUM = 'SIGNUM',
  RAD = 'RAD',
  DEG = 'DEG',
}

export const MathFunctionMap = new Map<MathFunction, FunctionData>(
  [
    [
      MathFunction.CUSTOM,
      {
        value: MathFunction.CUSTOM,
        name: 'Custom Function',
        description: 'Use this function to specify complex mathematical expression.',
        minArgs: 1,
        maxArgs: 16
      }
    ],
    [
      MathFunction.ADD,
      {
        value: MathFunction.ADD,
        name: 'Addition',
        description: 'x + y',
        minArgs: 2,
        maxArgs: 2
      }
    ],
    [
      MathFunction.SUB,
      {
        value: MathFunction.SUB,
        name: 'Subtraction',
        description: 'x - y',
        minArgs: 2,
        maxArgs: 2
      }
    ],
    [
      MathFunction.MULT,
      {
        value: MathFunction.MULT,
        name: 'Multiplication',
        description: 'x * y',
        minArgs: 2,
        maxArgs: 2
      }
    ],
    [
      MathFunction.DIV,
      {
        value: MathFunction.DIV,
        name: 'Division',
        description: 'x / y',
        minArgs: 2,
        maxArgs: 2
      }
    ],
    [
      MathFunction.SIN,
      {
        value: MathFunction.SIN,
        name: 'Sine',
        description: 'Returns the trigonometric sine of an angle in radians.',
        minArgs: 1,
        maxArgs: 1
      }
    ],
    [
      MathFunction.SINH,
      {
        value: MathFunction.SINH,
        name: 'Hyperbolic sine',
        description: 'Returns the hyperbolic sine of an argument.',
        minArgs: 1,
        maxArgs: 1
      }
    ],
    [
      MathFunction.COS,
      {
        value: MathFunction.COS,
        name: 'Cosine',
        description: 'Returns the trigonometric cosine of an angle in radians.',
        minArgs: 1,
        maxArgs: 1
      }
    ],
    [
      MathFunction.COSH,
      {
        value: MathFunction.COSH,
        name: 'Hyperbolic cosine',
        description: 'Returns the hyperbolic cosine of an argument.',
        minArgs: 1,
        maxArgs: 1
      }
    ],
    [
      MathFunction.TAN,
      {
        value: MathFunction.TAN,
        name: 'Tangent',
        description: 'Returns the trigonometric tangent of an angle in radians',
        minArgs: 1,
        maxArgs: 1
      }
    ],
    [
      MathFunction.TANH,
      {
        value: MathFunction.TANH,
        name: 'Hyperbolic tangent',
        description: 'Returns the hyperbolic tangent of an argument',
        minArgs: 1,
        maxArgs: 1
      }
    ],
    [
      MathFunction.ACOS,
      {
        value: MathFunction.ACOS,
        name: 'Arc cosine',
        description: 'Returns the arc cosine of an argument',
        minArgs: 1,
        maxArgs: 1
      }
    ],
    [
      MathFunction.ASIN,
      {
        value: MathFunction.ASIN,
        name: 'Arc sine',
        description: 'Returns the arc sine of an argument',
        minArgs: 1,
        maxArgs: 1
      }
    ],
    [
      MathFunction.ATAN,
      {
        value: MathFunction.ATAN,
        name: 'Arc tangent',
        description: 'Returns the arc tangent of an argument',
        minArgs: 1,
        maxArgs: 1
      }
    ],
    [
      MathFunction.ATAN2,
      {
        value: MathFunction.ATAN2,
        name: '2-argument arc tangent',
        description: 'Returns the angle theta from the conversion of rectangular coordinates (x, y) to polar coordinates (r, theta)',
        minArgs: 2,
        maxArgs: 2
      }
    ],
    [
      MathFunction.EXP,
      {
        value: MathFunction.EXP,
        name: 'Exponential',
        description: 'Returns Euler\'s number e raised to the power of an argument',
        minArgs: 1,
        maxArgs: 1
      }
    ],
    [
      MathFunction.EXPM1,
      {
        value: MathFunction.EXPM1,
        name: 'Exponential minus one',
        description: 'Returns Euler\'s number e raised to the power of an argument minus one',
        minArgs: 1,
        maxArgs: 1
      }
    ],
    [
      MathFunction.SQRT,
      {
        value: MathFunction.SQRT,
        name: 'Square',
        description: 'Returns the correctly rounded positive square root of an argument',
        minArgs: 1,
        maxArgs: 1
      }
    ],
    [
      MathFunction.CBRT,
      {
        value: MathFunction.CBRT,
        name: 'Cube root',
        description: 'Returns the cube root of an argument',
        minArgs: 1,
        maxArgs: 1
      }
    ],
    [
      MathFunction.GET_EXP,
      {
        value: MathFunction.GET_EXP,
        name: 'Get exponent',
        description: 'Returns the unbiased exponent used in the representation of an argument',
        minArgs: 1,
        maxArgs: 1
      }
    ],
    [
      MathFunction.HYPOT,
      {
        value: MathFunction.HYPOT,
        name: 'Square root',
        description: 'Returns the square root of the squares of the arguments',
        minArgs: 2,
        maxArgs: 2
      }
    ],
    [
      MathFunction.LOG,
      {
        value: MathFunction.LOG,
        name: 'Logarithm',
        description: 'Returns the natural logarithm of an argument',
        minArgs: 1,
        maxArgs: 1
      }
    ],
    [
      MathFunction.LOG10,
      {
        value: MathFunction.LOG10,
        name: 'Base 10 logarithm',
        description: 'Returns the base 10 logarithm of an argument',
        minArgs: 1,
        maxArgs: 1
      }
    ],
    [
      MathFunction.LOG1P,
      {
        value: MathFunction.LOG1P,
        name: 'Logarithm of the sum',
        description: 'Returns the natural logarithm of the sum of an argument',
        minArgs: 1,
        maxArgs: 1
      }
    ],
    [
      MathFunction.CEIL,
      {
        value: MathFunction.CEIL,
        name: 'Ceiling',
        description: 'Returns the smallest (closest to negative infinity) of an argument',
        minArgs: 1,
        maxArgs: 1
      }
    ],
    [
      MathFunction.FLOOR,
      {
        value: MathFunction.FLOOR,
        name: 'Floor',
        description: 'Returns the largest (closest to positive infinity) of an argument',
        minArgs: 1,
        maxArgs: 1
      }
    ],
    [
      MathFunction.FLOOR_DIV,
      {
        value: MathFunction.FLOOR_DIV,
        name: 'Floor division',
        description: 'Returns the largest (closest to positive infinity) of the arguments',
        minArgs: 2,
        maxArgs: 2
      }
    ],
    [
      MathFunction.FLOOR_MOD,
      {
        value: MathFunction.FLOOR_MOD,
        name: 'Floor modulus',
        description: 'Returns the floor modulus of the arguments',
        minArgs: 2,
        maxArgs: 2
      }
    ],
    [
      MathFunction.ABS,
      {
        value: MathFunction.ABS,
        name: 'Absolute',
        description: 'Returns the absolute value of an argument',
        minArgs: 1,
        maxArgs: 1
      }
    ],
    [
      MathFunction.MIN,
      {
        value: MathFunction.MIN,
        name: 'Min',
        description: 'Returns the smaller of the arguments',
        minArgs: 2,
        maxArgs: 2
      }
    ],
    [
      MathFunction.MAX,
      {
        value: MathFunction.MAX,
        name: 'Max',
        description: 'Returns the greater of the arguments',
        minArgs: 2,
        maxArgs: 2
      }
    ],
    [
      MathFunction.POW,
      {
        value: MathFunction.POW,
        name: 'Raise to a power',
        description: 'Returns the value of the first argument raised to the power of the second argument',
        minArgs: 2,
        maxArgs: 2
      }
    ],
    [
      MathFunction.SIGNUM,
      {
        value: MathFunction.SIGNUM,
        name: 'Sign of a real number',
        description: 'Returns the signum function of the argument',
        minArgs: 1,
        maxArgs: 1
      }
    ],
    [
      MathFunction.RAD,
      {
        value: MathFunction.RAD,
        name: 'Radian',
        description: 'Converts an angle measured in degrees to an approximately equivalent angle measured in radians',
        minArgs: 1,
        maxArgs: 1
      }
    ],
    [
      MathFunction.DEG,
      {
        value: MathFunction.DEG,
        name: 'Degrees',
        description: 'Converts an angle measured in radians to an approximately equivalent angle measured in degrees.',
        minArgs: 1,
        maxArgs: 1
      }
    ],
  ]);

export enum ArgumentType {
  MESSAGE_BODY = 'MESSAGE_BODY',
  MESSAGE_METADATA = 'MESSAGE_METADATA',
  ATTRIBUTE = 'ATTRIBUTE',
  TIME_SERIES = 'TIME_SERIES',
  CONSTANT = 'CONSTANT'
}

export enum ArgumentTypeResult {
  MESSAGE_BODY = 'MESSAGE_BODY',
  MESSAGE_METADATA = 'MESSAGE_METADATA',
  ATTRIBUTE = 'ATTRIBUTE',
  TIME_SERIES = 'TIME_SERIES'
}

export enum FetchTo {
  DATA = 'DATA',
  METADATA = 'METADATA'
}

export const FetchFromToTranslation = new Map<FetchTo, string>([
  [FetchTo.DATA, 'rule-node-config.message-to-metadata'],
  [FetchTo.METADATA, 'rule-node-config.metadata-to-message'],
]);

export const FetchFromTranslation = new Map<FetchTo, string>([
  [FetchTo.DATA, 'rule-node-config.from-message'],
  [FetchTo.METADATA, 'rule-node-config.from-metadata'],
]);

export const FetchToTranslation = new Map<FetchTo, string>([
  [FetchTo.DATA, 'rule-node-config.message'],
  [FetchTo.METADATA, 'rule-node-config.metadata'],
]);

export const FetchToRenameTranslation = new Map<FetchTo, string>([
  [FetchTo.DATA, 'rule-node-config.message'],
  [FetchTo.METADATA, 'rule-node-config.message-metadata'],
]);

export interface ArgumentTypeData {
  name: string;
  description: string;
}

export const ArgumentTypeMap = new Map<ArgumentType, ArgumentTypeData>([
  [
    ArgumentType.MESSAGE_BODY,
    {
      name: 'rule-node-config.message-body-type',
      description: 'rule-node-config.message-body-type-description'
    }
  ],
  [
    ArgumentType.MESSAGE_METADATA,
    {
      name: 'rule-node-config.message-metadata-type',
      description: 'rule-node-config.message-metadata-type-description'
    }
  ],
  [
    ArgumentType.ATTRIBUTE,
    {
      name: 'rule-node-config.attribute-type',
      description: 'rule-node-config.attribute-type-description'
    }
  ],
  [
    ArgumentType.TIME_SERIES,
    {
      name: 'rule-node-config.time-series-type',
      description: 'rule-node-config.time-series-type-description'
    }
  ],
  [
    ArgumentType.CONSTANT,
    {
      name: 'rule-node-config.constant-type',
      description: 'rule-node-config.constant-type-description'
    }
  ]
]);

export const ArgumentTypeResultMap = new Map<ArgumentTypeResult, ArgumentTypeData>([
  [
    ArgumentTypeResult.MESSAGE_BODY,
    {
      name: 'rule-node-config.message-body-type',
      description: 'rule-node-config.message-body-type-result-description'
    }
  ],
  [
    ArgumentTypeResult.MESSAGE_METADATA,
    {
      name: 'rule-node-config.message-metadata-type',
      description: 'rule-node-config.message-metadata-result-description'
    }
  ],
  [
    ArgumentTypeResult.ATTRIBUTE,
    {
      name: 'rule-node-config.attribute-type',
      description: 'rule-node-config.attribute-type-result-description'
    }
  ],
  [
    ArgumentTypeResult.TIME_SERIES,
    {
      name: 'rule-node-config.time-series-type',
      description: 'rule-node-config.time-series-type-result-description'
    }
  ]
]);

export const ArgumentName = ['x', 'y', 'z', 'a', 'b', 'c', 'd', 'k', 'l', 'm', 'n', 'o', 'p', 'r', 's', 't'];

export enum AttributeScope {
  SHARED_SCOPE = 'SHARED_SCOPE',
  SERVER_SCOPE = 'SERVER_SCOPE',
  CLIENT_SCOPE = 'CLIENT_SCOPE'
}

export enum AttributeScopeResult {
  SHARED_SCOPE = 'SHARED_SCOPE',
  SERVER_SCOPE = 'SERVER_SCOPE'
}

export const AttributeScopeMap = new Map<AttributeScope, string>([
  [AttributeScope.SHARED_SCOPE, 'rule-node-config.shared-scope'],
  [AttributeScope.SERVER_SCOPE, 'rule-node-config.server-scope'],
  [AttributeScope.CLIENT_SCOPE, 'rule-node-config.client-scope']
]);

export enum PresenceMonitoringStrategy {
  ON_FIRST_MESSAGE = 'ON_FIRST_MESSAGE',
  ON_EACH_MESSAGE = 'ON_EACH_MESSAGE'
}

export interface PresenceMonitoringStrategyData {
  value: boolean;
  name: string;
}

export const PresenceMonitoringStrategiesData = new Map<PresenceMonitoringStrategy, PresenceMonitoringStrategyData>([
  [
    PresenceMonitoringStrategy.ON_EACH_MESSAGE,
    {
      value: true,
      name: 'rule-node-config.presence-monitoring-strategy-on-each-message'
    }
  ],
  [
    PresenceMonitoringStrategy.ON_FIRST_MESSAGE,
    {
      value: false,
      name: 'rule-node-config.presence-monitoring-strategy-on-first-message'
    }
  ]
]);

export const IntLimit = 2147483648;
