// relative path uses package.json {"types":"types/index.d.ts", ...}
import {IClientOptions, Client, connect, IConnackPacket} from '../..'
const BROKER = 'test.mosquitto.org'

const PAYLOAD = 'hello from TS'
const TOPIC = 'typescript-test-' + Math.random().toString(16).substr(2)
const opts: IClientOptions = {}

console.log(`connect(${JSON.stringify(BROKER)})`)
const client:Client = connect(`mqtt://${BROKER}`, opts)

client.subscribe({[TOPIC]: {qos: 2}}, (err, granted) => {
    granted.forEach(({topic, qos}) => {
        console.log(`subscribed to ${topic} with qos=${qos}`)
    })
    client.publish(TOPIC, PAYLOAD, {qos: 2})
}).on('message', (topic: string, payload: Buffer) => {
    console.log(`message from ${topic}: ${payload}`)
    client.end()
}).on('connect', (packet: IConnackPacket) => {
    console.log('connected!', JSON.stringify(packet))
})
