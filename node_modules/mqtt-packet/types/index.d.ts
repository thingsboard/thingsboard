import EventEmitter = NodeJS.EventEmitter
import WritableStream = NodeJS.WritableStream

export declare type QoS = 0 | 1 | 2

export declare type PacketCmd = 'connack' |
  'connect' |
  'disconnect' |
  'pingreq' |
  'pingresp' |
  'puback' |
  'pubcomp' |
  'publish' |
  'pubrel' |
  'pubrec' |
  'suback' |
  'subscribe' |
  'unsuback' |
  'unsubscribe'

export interface IPacket {
  cmd: PacketCmd
  messageId?: number
  length?: number
}

export interface IConnectPacket extends IPacket {
  cmd: 'connect'
  clientId: string
  protocolVersion?: 4 | 5 | 3
  protocolId?: 'MQTT' | 'MQIsdp'
  clean?: boolean
  keepalive?: number
  username?: string
  password?: Buffer
  will?: {
    topic: string
    payload: Buffer
    qos?: QoS
    retain?: boolean
    properties?: {
      willDelayInterval?: number,
      payloadFormatIndicator?: number,
      messageExpiryInterval?: number,
      contentType?: string,
      responseTopic?: string,
      correlationData?: Buffer,
      userProperties?: Object
    }
  }
  properties?: {
    sessionExpiryInterval?: number,
    receiveMaximum?: number,
    maximumPacketSize?: number,
    topicAliasMaximum?: number,
    requestResponseInformation?: boolean,
    requestProblemInformation?: boolean,
    userProperties?: Object,
    authenticationMethod?: string,
    authenticationData?: Buffer
  }
}

export interface IPublishPacket extends IPacket {
  cmd: 'publish'
  qos: QoS
  dup: boolean
  retain: boolean
  topic: string
  payload: string | Buffer
  properties?: {
    payloadFormatIndicator?: boolean,
    messageExpiryInterval?: number,
    topicAlias?: number,
    responseTopic?: string,
    correlationData?: Buffer,
    userProperties?: Object,
    subscriptionIdentifier?: number,
    contentType?: string
  }
}

export interface IConnackPacket extends IPacket {
  cmd: 'connack'
  returnCode: number
  sessionPresent: boolean
  properties?: {
    sessionExpiryInterval?: number,
    receiveMaximum?: number,
    maximumQoS?: number,
    retainAvailable?: boolean,
    maximumPacketSize?: number,
    assignedClientIdentifier?: string,
    topicAliasMaximum?: number,
    reasonString?: string,
    userProperties?: Object,
    wildcardSubscriptionAvailable?: boolean,
    subscriptionIdentifiersAvailable?: boolean,
    sharedSubscriptionAvailable?: boolean,
    serverKeepAlive?: number,
    responseInformation?: string,
    serverReference?: string,
    authenticationMethod?: string,
    authenticationData?: Buffer
  }
}

export interface ISubscription {
  topic: string
  qos: QoS,
  nl?: boolean,
  rap?: boolean,
  rh?: number
}

export interface ISubscribePacket extends IPacket {
  cmd: 'subscribe'
  subscriptions: ISubscription[],
  properties?: {
    reasonString?: string,
    userProperties?: Object
  }
}

export interface ISubackPacket extends IPacket {
  cmd: 'suback',
  properties?: {
    reasonString?: string,
    userProperties?: Object
  },
  granted: number[] | Object[]
}

export interface IUnsubscribePacket extends IPacket {
  cmd: 'unsubscribe',
  properties?: {
    reasonString?: string,
    userProperties?: Object
  },
  unsubscriptions: string[]
}

export interface IUnsubackPacket extends IPacket {
  cmd: 'unsuback',
  properties?: {
    reasonString?: string,
    userProperties?: Object
  }
}

export interface IPubackPacket extends IPacket {
  cmd: 'puback',
  properties?: {
    reasonString?: string,
    userProperties?: Object
  }
}

export interface IPubcompPacket extends IPacket {
  cmd: 'pubcomp',
  properties?: {
    reasonString?: string,
    userProperties?: Object
  }
}

export interface IPubrelPacket extends IPacket {
  cmd: 'pubrel',
  properties?: {
    reasonString?: string,
    userProperties?: Object
  }
}

export interface IPubrecPacket extends IPacket {
  cmd: 'pubrec',
  properties?: {
    reasonString?: string,
    userProperties?: Object
  }
}

export interface IPingreqPacket extends IPacket {
  cmd: 'pingreq'
}

export interface IPingrespPacket extends IPacket {
  cmd: 'pingresp'
}

export interface IDisconnectPacket extends IPacket {
  cmd: 'disconnect',
    properties?: {
      sessionExpiryInterval?: number,
      reasonString?: string,
      userProperties?: Object,
      serverReference?: string
    }
}

export declare type Packet = IConnectPacket |
  IPublishPacket |
  IConnackPacket |
  ISubscribePacket |
  ISubackPacket |
  IUnsubscribePacket |
  IUnsubackPacket |
  IPubackPacket |
  IPubcompPacket |
  IPubrelPacket |
  IPingreqPacket |
  IPingrespPacket |
  IDisconnectPacket |
  IPubrecPacket

export interface Parser extends EventEmitter {
  on(event: 'packet', callback: (packet: Packet) => void): this

  on(event: 'error', callback: (error: any) => void): this

  parse(buffer: Buffer, opts?: Object): number
}

export declare function parser(opts?: Object): Parser

export declare function generate(packet: Packet, opts?: Object): Buffer

export declare function writeToStream(object: Packet, stream: WritableStream, opts?: Object): void

export declare namespace writeToStream {
  let cacheNumbers: boolean
}
