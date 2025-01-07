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
    [OriginatorSource.CUSTOMER, 'tb.rulenode.originator-customer'],
    [OriginatorSource.TENANT, 'tb.rulenode.originator-tenant'],
    [OriginatorSource.RELATED, 'tb.rulenode.originator-related'],
    [OriginatorSource.ALARM_ORIGINATOR, 'tb.rulenode.originator-alarm-originator'],
    [OriginatorSource.ENTITY, 'tb.rulenode.originator-entity'],
  ]
);

export const originatorSourceDescTranslations = new Map<OriginatorSource, string>(
  [
    [OriginatorSource.CUSTOMER, 'tb.rulenode.originator-customer-desc'],
    [OriginatorSource.TENANT, 'tb.rulenode.originator-tenant-desc'],
    [OriginatorSource.RELATED, 'tb.rulenode.originator-related-entity-desc'],
    [OriginatorSource.ALARM_ORIGINATOR, 'tb.rulenode.originator-alarm-originator-desc'],
    [OriginatorSource.ENTITY, 'tb.rulenode.originator-entity-by-name-pattern-desc'],
  ]
);
export const allowedOriginatorFields: EntityField[] = [
  entityFields.createdTime,
  entityFields.name,
  {value: 'type', name: 'tb.rulenode.profile-name', keyName: 'originatorProfileName'},
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
  {value: 'id', name: 'tb.rulenode.id', keyName: 'id'},
  {value: 'additionalInfo', name: 'tb.rulenode.additional-info', keyName: 'additionalInfo'}
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
    [PerimeterType.CIRCLE, 'tb.rulenode.perimeter-circle'],
    [PerimeterType.POLYGON, 'tb.rulenode.perimeter-polygon'],
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
    [TimeUnit.MILLISECONDS, 'tb.rulenode.time-unit-milliseconds'],
    [TimeUnit.SECONDS, 'tb.rulenode.time-unit-seconds'],
    [TimeUnit.MINUTES, 'tb.rulenode.time-unit-minutes'],
    [TimeUnit.HOURS, 'tb.rulenode.time-unit-hours'],
    [TimeUnit.DAYS, 'tb.rulenode.time-unit-days']
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
    [RangeUnit.METER, 'tb.rulenode.range-unit-meter'],
    [RangeUnit.KILOMETER, 'tb.rulenode.range-unit-kilometer'],
    [RangeUnit.FOOT, 'tb.rulenode.range-unit-foot'],
    [RangeUnit.MILE, 'tb.rulenode.range-unit-mile'],
    [RangeUnit.NAUTICAL_MILE, 'tb.rulenode.range-unit-nautical-mile']
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
    [EntityDetailsField.ID, 'tb.rulenode.entity-details-id'],
    [EntityDetailsField.TITLE, 'tb.rulenode.entity-details-title'],
    [EntityDetailsField.COUNTRY, 'tb.rulenode.entity-details-country'],
    [EntityDetailsField.STATE, 'tb.rulenode.entity-details-state'],
    [EntityDetailsField.CITY, 'tb.rulenode.entity-details-city'],
    [EntityDetailsField.ZIP, 'tb.rulenode.entity-details-zip'],
    [EntityDetailsField.ADDRESS, 'tb.rulenode.entity-details-address'],
    [EntityDetailsField.ADDRESS2, 'tb.rulenode.entity-details-address2'],
    [EntityDetailsField.PHONE, 'tb.rulenode.entity-details-phone'],
    [EntityDetailsField.EMAIL, 'tb.rulenode.entity-details-email'],
    [EntityDetailsField.ADDITIONAL_INFO, 'tb.rulenode.entity-details-additional_info']
  ]
);

export enum FetchMode {
  FIRST = 'FIRST',
  LAST = 'LAST',
  ALL = 'ALL'
}

export const deduplicationStrategiesTranslations = new Map<FetchMode, string>(
  [
    [FetchMode.FIRST, 'tb.rulenode.first'],
    [FetchMode.LAST, 'tb.rulenode.last'],
    [FetchMode.ALL, 'tb.rulenode.all']
  ]
);

export const deduplicationStrategiesHintTranslations = new Map<FetchMode, string>(
  [
    [FetchMode.FIRST, 'tb.rulenode.first-mode-hint'],
    [FetchMode.LAST, 'tb.rulenode.last-mode-hint'],
    [FetchMode.ALL, 'tb.rulenode.all-mode-hint']
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
    [DataToFetch.ATTRIBUTES, 'tb.rulenode.attributes'],
    [DataToFetch.LATEST_TELEMETRY, 'tb.rulenode.latest-telemetry'],
    [DataToFetch.FIELDS, 'tb.rulenode.fields']
  ]
);

export const msgMetadataLabelTranslations = new Map<DataToFetch, string>(
  [
    [DataToFetch.ATTRIBUTES, 'tb.rulenode.add-mapped-attribute-to'],
    [DataToFetch.LATEST_TELEMETRY, 'tb.rulenode.add-mapped-latest-telemetry-to'],
    [DataToFetch.FIELDS, 'tb.rulenode.add-mapped-fields-to']
  ]
);

export const samplingOrderTranslations = new Map<SamplingOrder, string>(
  [
    [SamplingOrder.ASC, 'tb.rulenode.ascending'],
    [SamplingOrder.DESC, 'tb.rulenode.descending']
  ]
);

export enum SqsQueueType {
  STANDARD = 'STANDARD',
  FIFO = 'FIFO'
}

export const sqsQueueTypeTranslations = new Map<SqsQueueType, string>(
  [
    [SqsQueueType.STANDARD, 'tb.rulenode.sqs-queue-standard'],
    [SqsQueueType.FIFO, 'tb.rulenode.sqs-queue-fifo'],
  ]
);

export type credentialsType = 'anonymous' | 'basic' | 'cert.PEM';
export const credentialsTypes: credentialsType[] = ['anonymous', 'basic', 'cert.PEM'];

export const credentialsTypeTranslations = new Map<credentialsType, string>(
  [
    ['anonymous', 'tb.rulenode.credentials-anonymous'],
    ['basic', 'tb.rulenode.credentials-basic'],
    ['cert.PEM', 'tb.rulenode.credentials-pem']
  ]
);

export type AzureIotHubCredentialsType = 'sas' | 'cert.PEM';
export const azureIotHubCredentialsTypes: AzureIotHubCredentialsType[] = ['sas', 'cert.PEM'];

export const azureIotHubCredentialsTypeTranslations = new Map<AzureIotHubCredentialsType, string>(
  [
    ['sas', 'tb.rulenode.credentials-sas'],
    ['cert.PEM', 'tb.rulenode.credentials-pem']
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
    ['US-ASCII', 'tb.rulenode.charset-us-ascii'],
    ['ISO-8859-1', 'tb.rulenode.charset-iso-8859-1'],
    ['UTF-8', 'tb.rulenode.charset-utf-8'],
    ['UTF-16BE', 'tb.rulenode.charset-utf-16be'],
    ['UTF-16LE', 'tb.rulenode.charset-utf-16le'],
    ['UTF-16', 'tb.rulenode.charset-utf-16'],
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
  [FetchTo.DATA, 'tb.rulenode.message-to-metadata'],
  [FetchTo.METADATA, 'tb.rulenode.metadata-to-message'],
]);

export const FetchFromTranslation = new Map<FetchTo, string>([
  [FetchTo.DATA, 'tb.rulenode.from-message'],
  [FetchTo.METADATA, 'tb.rulenode.from-metadata'],
]);

export const FetchToTranslation = new Map<FetchTo, string>([
  [FetchTo.DATA, 'tb.rulenode.message'],
  [FetchTo.METADATA, 'tb.rulenode.metadata'],
]);

export const FetchToRenameTranslation = new Map<FetchTo, string>([
  [FetchTo.DATA, 'tb.rulenode.message'],
  [FetchTo.METADATA, 'tb.rulenode.message-metadata'],
]);

export interface ArgumentTypeData {
  name: string;
  description: string;
}

export const ArgumentTypeMap = new Map<ArgumentType, ArgumentTypeData>([
  [
    ArgumentType.MESSAGE_BODY,
    {
      name: 'tb.rulenode.message-body-type',
      description: 'Fetch argument value from incoming message'
    }
  ],
  [
    ArgumentType.MESSAGE_METADATA,
    {
      name: 'tb.rulenode.message-metadata-type',
      description: 'Fetch argument value from incoming message metadata'
    }
  ],
  [
    ArgumentType.ATTRIBUTE,
    {
      name: 'tb.rulenode.attribute-type',
      description: 'Fetch attribute value from database'
    }
  ],
  [
    ArgumentType.TIME_SERIES,
    {
      name: 'tb.rulenode.time-series-type',
      description: 'Fetch latest time-series value from database'
    }
  ],
  [
    ArgumentType.CONSTANT,
    {
      name: 'tb.rulenode.constant-type',
      description: 'Define constant value'
    }
  ]
]);

export const ArgumentTypeResultMap = new Map<ArgumentTypeResult, ArgumentTypeData>([
  [
    ArgumentTypeResult.MESSAGE_BODY,
    {
      name: 'tb.rulenode.message-body-type',
      description: 'Add result to the outgoing message'
    }
  ],
  [
    ArgumentTypeResult.MESSAGE_METADATA,
    {
      name: 'tb.rulenode.message-metadata-type',
      description: 'Add result to the outgoing message metadata'
    }
  ],
  [
    ArgumentTypeResult.ATTRIBUTE,
    {
      name: 'tb.rulenode.attribute-type',
      description: 'Store result as an entity attribute in the database'
    }
  ],
  [
    ArgumentTypeResult.TIME_SERIES,
    {
      name: 'tb.rulenode.time-series-type',
      description: 'Store result as an entity time-series in the database'
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
  [AttributeScope.SHARED_SCOPE, 'tb.rulenode.shared-scope'],
  [AttributeScope.SERVER_SCOPE, 'tb.rulenode.server-scope'],
  [AttributeScope.CLIENT_SCOPE, 'tb.rulenode.client-scope']
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
      name: 'tb.rulenode.presence-monitoring-strategy-on-each-message'
    }
  ],
  [
    PresenceMonitoringStrategy.ON_FIRST_MESSAGE,
    {
      value: false,
      name: 'tb.rulenode.presence-monitoring-strategy-on-first-message'
    }
  ]
]);

export const IntLimit = 2147483648;
