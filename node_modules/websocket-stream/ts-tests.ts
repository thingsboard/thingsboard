import * as WebSocket from 'ws';
import * as WebSocketStream from './';

{
    let ws = new WebSocket('ws://www.host.com/path');
    const stream = WebSocketStream(ws);
    ws = stream.socket;

    stream.setEncoding("utf8");
    stream.write("hello world");

    const message = stream.read(10);
    const message2 = stream.read();
}

{
    const stream = WebSocketStream('ws://www.host.com/path');
}

{
    // stream - url target with subprotocol
    const stream = WebSocketStream('ws://www.host.com/path', 'appProtocol-v1');
}

{
    // stream - url target with subprotocols, no options
    const stream = WebSocketStream('ws://www.host.com/path', ['appProtocol-v1', 'appProtocol-v2']);
}

{
    // stream - url target with options, no subprotocols
    const stream = WebSocketStream('ws://www.host.com/path', { maxPayload: 1024 });
}

{
    // stream - url target with subprotocol and options
    const stream = WebSocketStream(
        'ws://www.host.com/path',
        ['appProtocol-v1', 'appProtocol-v2'],
        { maxPayload: 1024 },
    );
}

{
    // stream - url target with subprotocols and options
    const stream = WebSocketStream(
        'ws://www.host.com/path',
        ['appProtocol-v1', 'appProtocol-v2'],
        { maxPayload: 1024 },
    );
}

{
    // dot server
    const wss = new WebSocketStream.Server({port: 8081});
    wss.on('stream', (stream, req) => {
      stream.write(stream.read());
      stream.end();
    });
}

{
    // dot createServer
    const wss = WebSocketStream.createServer({port: 8081});
    wss.on('stream', (stream, req) => {
      stream.write(stream.read());
      stream.end(); // closes underlying socket
    });
}
