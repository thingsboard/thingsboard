import { IClientOptions, MqttClient } from '../client'
/**
 * connect - connect to an MQTT broker.
 *
 * @param {String} [brokerUrl] - url of the broker, optional
 * @param {Object} opts - see MqttClient#constructor
 */
declare function connect (brokerUrl?: string | any, opts?: IClientOptions): MqttClient
export { connect }
export { MqttClient }
