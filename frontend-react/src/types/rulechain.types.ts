/**
 * Rule Chain Type Definitions
 * Mirrors ThingsBoard Angular implementation
 */

// ==================== Basic Types ====================

export interface EntityId {
  id: string
  entityType: string
}

export interface RuleChainId extends EntityId {
  entityType: 'RULE_CHAIN'
}

export interface RuleNodeId extends EntityId {
  entityType: 'RULE_NODE'
}

// ==================== Rule Chain ====================

export interface RuleChain {
  id: RuleChainId
  createdTime?: number
  tenantId?: EntityId
  name: string
  firstRuleNodeId?: RuleNodeId
  root: boolean
  debugMode: boolean
  type: RuleChainType
  configuration?: any
  additionalInfo?: {
    description?: string
  }
}

export enum RuleChainType {
  CORE = 'CORE',
  EDGE = 'EDGE',
}

// ==================== Rule Node ====================

export enum RuleNodeType {
  FILTER = 'FILTER',
  ENRICHMENT = 'ENRICHMENT',
  TRANSFORMATION = 'TRANSFORMATION',
  ACTION = 'ACTION',
  EXTERNAL = 'EXTERNAL',
  FLOW = 'FLOW',
  INPUT = 'INPUT',
  UNKNOWN = 'UNKNOWN',
}

export interface RuleNodeTypeDescriptor {
  value: RuleNodeType
  name: string
  details: string
  nodeClass: string
  icon: string
  color: string
  special?: boolean
}

// Exact color scheme from Angular ThingsBoard (rule-node-colors.scss)
export const ruleNodeTypeDescriptors: Record<RuleNodeType, RuleNodeTypeDescriptor> = {
  [RuleNodeType.FILTER]: {
    value: RuleNodeType.FILTER,
    name: 'Filter',
    details: 'Filter incoming messages',
    nodeClass: 'tb-filter-type',
    icon: 'FilterList',
    color: '#f1e861', // Exact Angular color
  },
  [RuleNodeType.ENRICHMENT]: {
    value: RuleNodeType.ENRICHMENT,
    name: 'Enrichment',
    details: 'Enrich message with additional data',
    nodeClass: 'tb-enrichment-type',
    icon: 'PlaylistAdd',
    color: '#cdf14e', // Exact Angular color
  },
  [RuleNodeType.TRANSFORMATION]: {
    value: RuleNodeType.TRANSFORMATION,
    name: 'Transformation',
    details: 'Transform message format',
    nodeClass: 'tb-transformation-type',
    icon: 'Transform',
    color: '#79cef1', // Exact Angular color
  },
  [RuleNodeType.ACTION]: {
    value: RuleNodeType.ACTION,
    name: 'Action',
    details: 'Execute action on message',
    nodeClass: 'tb-action-type',
    icon: 'FlashOn',
    color: '#f1928f', // Exact Angular color
  },
  [RuleNodeType.EXTERNAL]: {
    value: RuleNodeType.EXTERNAL,
    name: 'External',
    details: 'Send to external system',
    nodeClass: 'tb-external-type',
    icon: 'CloudUpload',
    color: '#fbc766', // Exact Angular color
  },
  [RuleNodeType.FLOW]: {
    value: RuleNodeType.FLOW,
    name: 'Flow',
    details: 'Control message flow',
    nodeClass: 'tb-flow-type',
    icon: 'SettingsEthernet',
    color: '#d6c4f1', // Exact Angular color
  },
  [RuleNodeType.INPUT]: {
    value: RuleNodeType.INPUT,
    name: 'Input',
    details: 'Rule chain input',
    nodeClass: 'tb-input-type',
    icon: 'Input',
    color: '#a3eaa9', // Exact Angular color
    special: true,
  },
  [RuleNodeType.UNKNOWN]: {
    value: RuleNodeType.UNKNOWN,
    name: 'Unknown',
    details: 'Unknown node type',
    nodeClass: 'tb-unknown-type',
    icon: 'HelpOutline',
    color: '#f16c29', // Exact Angular color
  },
}

export interface RuleNodeConfiguration {
  [key: string]: any
}

export interface RuleNodeDefinition {
  description: string
  details: string
  inEnabled: boolean
  outEnabled: boolean
  relationTypes: string[]
  customRelations: boolean
  ruleChainNode?: boolean
  defaultConfiguration: RuleNodeConfiguration
  icon?: string
  iconUrl?: string
  docUrl?: string
}

export interface RuleNodeComponentDescriptor {
  type: RuleNodeType
  name: string
  clazz: string
  configurationVersion: number
  configurationDescriptor?: {
    nodeDefinition: RuleNodeDefinition
  }
}

export interface RuleNode {
  id?: RuleNodeId
  ruleChainId?: RuleChainId
  type: string
  name: string
  debugMode?: boolean
  singletonMode: boolean
  queueName?: string
  configurationVersion?: number
  configuration: RuleNodeConfiguration
  additionalInfo?: {
    layoutX?: number
    layoutY?: number
    description?: string
  }
}

// ==================== Rule Chain Metadata ====================

export interface NodeConnectionInfo {
  fromIndex: number
  toIndex: number
  type: string // relation type label
}

export interface RuleChainMetadata {
  ruleChainId: RuleChainId
  firstNodeIndex?: number
  nodes: RuleNode[]
  connections: NodeConnectionInfo[]
}

export interface RuleChainImport {
  ruleChain: RuleChain
  metadata: RuleChainMetadata
}

// ==================== Predefined Rule Nodes ====================

export const RULE_NODE_COMPONENTS: RuleNodeComponentDescriptor[] = [
  // FILTER nodes
  {
    type: RuleNodeType.FILTER,
    name: 'Script Filter',
    clazz: 'org.thingsboard.rule.engine.filter.TbJsFilterNode',
    configurationVersion: 0,
    configurationDescriptor: {
      nodeDefinition: {
        description: 'Filter messages using JavaScript',
        details: 'Executes JavaScript function to filter messages',
        inEnabled: true,
        outEnabled: true,
        relationTypes: ['True', 'False'],
        customRelations: false,
        defaultConfiguration: {
          jsScript: 'return msg.temperature > 20;',
        },
      },
    },
  },
  {
    type: RuleNodeType.FILTER,
    name: 'Switch',
    clazz: 'org.thingsboard.rule.engine.filter.TbJsSwitchNode',
    configurationVersion: 0,
    configurationDescriptor: {
      nodeDefinition: {
        description: 'Route messages based on script',
        details: 'Routes messages to different paths based on JavaScript',
        inEnabled: true,
        outEnabled: true,
        relationTypes: [],
        customRelations: true,
        defaultConfiguration: {
          jsScript: 'return ["route1", "route2"];',
        },
      },
    },
  },
  {
    type: RuleNodeType.FILTER,
    name: 'Message Type Filter',
    clazz: 'org.thingsboard.rule.engine.filter.TbMsgTypeFilterNode',
    configurationVersion: 0,
    configurationDescriptor: {
      nodeDefinition: {
        description: 'Filter by message type',
        details: 'Filters messages based on message type',
        inEnabled: true,
        outEnabled: true,
        relationTypes: ['True', 'False'],
        customRelations: false,
        defaultConfiguration: {
          messageTypes: ['POST_TELEMETRY_REQUEST'],
        },
      },
    },
  },

  // ENRICHMENT nodes
  {
    type: RuleNodeType.ENRICHMENT,
    name: 'Originator Attributes',
    clazz: 'org.thingsboard.rule.engine.metadata.TbGetAttributesNode',
    configurationVersion: 0,
    configurationDescriptor: {
      nodeDefinition: {
        description: 'Fetch originator attributes',
        details: 'Enriches message with originator attributes',
        inEnabled: true,
        outEnabled: true,
        relationTypes: ['Success', 'Failure'],
        customRelations: false,
        defaultConfiguration: {
          attrMapping: {
            temperature: 'temp',
          },
        },
      },
    },
  },
  {
    type: RuleNodeType.ENRICHMENT,
    name: 'Related Attributes',
    clazz: 'org.thingsboard.rule.engine.metadata.TbGetRelatedAttributeNode',
    configurationVersion: 0,
    configurationDescriptor: {
      nodeDefinition: {
        description: 'Fetch related entity attributes',
        details: 'Enriches message with related entity attributes',
        inEnabled: true,
        outEnabled: true,
        relationTypes: ['Success', 'Failure'],
        customRelations: false,
        defaultConfiguration: {
          relationsQuery: {},
        },
      },
    },
  },
  {
    type: RuleNodeType.ENRICHMENT,
    name: 'Customer Attributes',
    clazz: 'org.thingsboard.rule.engine.metadata.TbGetCustomerAttributeNode',
    configurationVersion: 0,
    configurationDescriptor: {
      nodeDefinition: {
        description: 'Fetch customer attributes',
        details: 'Enriches message with customer attributes',
        inEnabled: true,
        outEnabled: true,
        relationTypes: ['Success', 'Failure'],
        customRelations: false,
        defaultConfiguration: {},
      },
    },
  },

  // TRANSFORMATION nodes
  {
    type: RuleNodeType.TRANSFORMATION,
    name: 'Script Transformation',
    clazz: 'org.thingsboard.rule.engine.transform.TbTransformMsgNode',
    configurationVersion: 0,
    configurationDescriptor: {
      nodeDefinition: {
        description: 'Transform message using script',
        details: 'Transforms message format using JavaScript',
        inEnabled: true,
        outEnabled: true,
        relationTypes: ['Success', 'Failure'],
        customRelations: false,
        defaultConfiguration: {
          jsScript: 'return {msg: msg, metadata: metadata, msgType: msgType};',
        },
      },
    },
  },
  {
    type: RuleNodeType.TRANSFORMATION,
    name: 'Change Originator',
    clazz: 'org.thingsboard.rule.engine.transform.TbChangeOriginatorNode',
    configurationVersion: 0,
    configurationDescriptor: {
      nodeDefinition: {
        description: 'Change message originator',
        details: 'Changes the originator of the message',
        inEnabled: true,
        outEnabled: true,
        relationTypes: ['Success', 'Failure'],
        customRelations: false,
        defaultConfiguration: {
          originatorSource: 'CUSTOMER',
        },
      },
    },
  },
  {
    type: RuleNodeType.TRANSFORMATION,
    name: 'To Email',
    clazz: 'org.thingsboard.rule.engine.mail.TbMsgToEmailNode',
    configurationVersion: 0,
    configurationDescriptor: {
      nodeDefinition: {
        description: 'Convert message to email',
        details: 'Converts message to email format',
        inEnabled: true,
        outEnabled: true,
        relationTypes: ['Success', 'Failure'],
        customRelations: false,
        defaultConfiguration: {
          fromTemplate: 'info@example.com',
          toTemplate: '${userEmail}',
          subjectTemplate: 'Alert',
          bodyTemplate: 'Temperature: ${temperature}',
        },
      },
    },
  },

  // ACTION nodes
  {
    type: RuleNodeType.ACTION,
    name: 'Save Timeseries',
    clazz: 'org.thingsboard.rule.engine.telemetry.TbMsgTimeseriesNode',
    configurationVersion: 0,
    configurationDescriptor: {
      nodeDefinition: {
        description: 'Save as timeseries',
        details: 'Saves message data as timeseries',
        inEnabled: true,
        outEnabled: true,
        relationTypes: ['Success', 'Failure'],
        customRelations: false,
        defaultConfiguration: {
          defaultTTL: 0,
        },
      },
    },
  },
  {
    type: RuleNodeType.ACTION,
    name: 'Save Attributes',
    clazz: 'org.thingsboard.rule.engine.telemetry.TbMsgAttributesNode',
    configurationVersion: 0,
    configurationDescriptor: {
      nodeDefinition: {
        description: 'Save as attributes',
        details: 'Saves message data as attributes',
        inEnabled: true,
        outEnabled: true,
        relationTypes: ['Success', 'Failure'],
        customRelations: false,
        defaultConfiguration: {
          scope: 'SERVER_SCOPE',
        },
      },
    },
  },
  {
    type: RuleNodeType.ACTION,
    name: 'Create Alarm',
    clazz: 'org.thingsboard.rule.engine.action.TbCreateAlarmNode',
    configurationVersion: 0,
    configurationDescriptor: {
      nodeDefinition: {
        description: 'Create alarm',
        details: 'Creates or updates alarm',
        inEnabled: true,
        outEnabled: true,
        relationTypes: ['Created', 'Updated', 'False'],
        customRelations: false,
        defaultConfiguration: {
          alarmType: 'General Alarm',
          severity: 'CRITICAL',
          propagate: false,
          useMessageAlarmData: false,
        },
      },
    },
  },
  {
    type: RuleNodeType.ACTION,
    name: 'Clear Alarm',
    clazz: 'org.thingsboard.rule.engine.action.TbClearAlarmNode',
    configurationVersion: 0,
    configurationDescriptor: {
      nodeDefinition: {
        description: 'Clear alarm',
        details: 'Clears existing alarm',
        inEnabled: true,
        outEnabled: true,
        relationTypes: ['Cleared', 'False'],
        customRelations: false,
        defaultConfiguration: {
          alarmType: 'General Alarm',
        },
      },
    },
  },
  {
    type: RuleNodeType.ACTION,
    name: 'RPC Call Request',
    clazz: 'org.thingsboard.rule.engine.rpc.TbSendRPCRequestNode',
    configurationVersion: 0,
    configurationDescriptor: {
      nodeDefinition: {
        description: 'Send RPC request',
        details: 'Sends RPC request to device',
        inEnabled: true,
        outEnabled: true,
        relationTypes: ['Success', 'Failure'],
        customRelations: false,
        defaultConfiguration: {
          timeoutInSeconds: 60,
        },
      },
    },
  },
  {
    type: RuleNodeType.ACTION,
    name: 'Log',
    clazz: 'org.thingsboard.rule.engine.action.TbLogNode',
    configurationVersion: 0,
    configurationDescriptor: {
      nodeDefinition: {
        description: 'Log message to console',
        details: 'Logs message to console',
        inEnabled: true,
        outEnabled: true,
        relationTypes: ['Success', 'Failure'],
        customRelations: false,
        defaultConfiguration: {
          jsScript: 'return "\\nIncoming message:\\n" + JSON.stringify(msg) + "\\nIncoming metadata:\\n" + JSON.stringify(metadata);',
        },
      },
    },
  },

  // EXTERNAL nodes
  {
    type: RuleNodeType.EXTERNAL,
    name: 'REST API Call',
    clazz: 'org.thingsboard.rule.engine.rest.TbRestApiCallNode',
    configurationVersion: 0,
    configurationDescriptor: {
      nodeDefinition: {
        description: 'Make REST API call',
        details: 'Sends HTTP request to external REST API',
        inEnabled: true,
        outEnabled: true,
        relationTypes: ['Success', 'Failure'],
        customRelations: false,
        defaultConfiguration: {
          restEndpointUrlPattern: 'http://localhost:8080/api',
          requestMethod: 'POST',
          useSimpleClientHttpFactory: false,
          readTimeoutMs: 0,
        },
      },
    },
  },
  {
    type: RuleNodeType.EXTERNAL,
    name: 'Send Email',
    clazz: 'org.thingsboard.rule.engine.mail.TbSendEmailNode',
    configurationVersion: 0,
    configurationDescriptor: {
      nodeDefinition: {
        description: 'Send email',
        details: 'Sends email via SMTP',
        inEnabled: true,
        outEnabled: true,
        relationTypes: ['Success', 'Failure'],
        customRelations: false,
        defaultConfiguration: {
          useSystemSmtpSettings: true,
        },
      },
    },
  },
  {
    type: RuleNodeType.EXTERNAL,
    name: 'Kafka',
    clazz: 'org.thingsboard.rule.engine.kafka.TbKafkaNode',
    configurationVersion: 0,
    configurationDescriptor: {
      nodeDefinition: {
        description: 'Publish to Kafka',
        details: 'Publishes message to Kafka topic',
        inEnabled: true,
        outEnabled: true,
        relationTypes: ['Success', 'Failure'],
        customRelations: false,
        defaultConfiguration: {
          topicPattern: 'my-topic',
          bootstrapServers: 'localhost:9092',
        },
      },
    },
  },
  {
    type: RuleNodeType.EXTERNAL,
    name: 'MQTT',
    clazz: 'org.thingsboard.rule.engine.mqtt.TbMqttNode',
    configurationVersion: 0,
    configurationDescriptor: {
      nodeDefinition: {
        description: 'Publish to MQTT',
        details: 'Publishes message to MQTT broker',
        inEnabled: true,
        outEnabled: true,
        relationTypes: ['Success', 'Failure'],
        customRelations: false,
        defaultConfiguration: {
          topicPattern: 'my-topic',
          host: 'localhost',
          port: 1883,
        },
      },
    },
  },

  // FLOW nodes
  {
    type: RuleNodeType.FLOW,
    name: 'Rule Chain',
    clazz: 'org.thingsboard.rule.engine.flow.TbRuleChainInputNode',
    configurationVersion: 0,
    configurationDescriptor: {
      nodeDefinition: {
        description: 'Invoke another rule chain',
        details: 'Routes message to another rule chain',
        inEnabled: true,
        outEnabled: true,
        relationTypes: ['Success', 'Failure'],
        customRelations: false,
        ruleChainNode: true,
        defaultConfiguration: {
          ruleChainId: null,
        },
      },
    },
  },
  {
    type: RuleNodeType.FLOW,
    name: 'Checkpoint',
    clazz: 'org.thingsboard.rule.engine.flow.TbCheckpointNode',
    configurationVersion: 0,
    configurationDescriptor: {
      nodeDefinition: {
        description: 'Queue checkpoint',
        details: 'Creates checkpoint for message processing',
        inEnabled: true,
        outEnabled: true,
        relationTypes: ['Success', 'Failure'],
        customRelations: false,
        defaultConfiguration: {
          queueName: 'Main',
        },
      },
    },
  },
]

// Input node (special)
export const INPUT_NODE: RuleNodeComponentDescriptor = {
  type: RuleNodeType.INPUT,
  name: 'Input',
  clazz: 'tb.internal.Input',
  configurationVersion: 0,
  configurationDescriptor: {
    nodeDefinition: {
      description: 'Rule chain input node',
      details: 'Entry point for rule chain',
      inEnabled: false,
      outEnabled: true,
      relationTypes: ['Success'],
      customRelations: false,
      defaultConfiguration: {},
    },
  },
}
