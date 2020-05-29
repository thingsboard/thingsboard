package org.thingsboard.server.transport.lwm2m.server;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.server.security.InMemorySecurityStore;
import org.eclipse.leshan.server.security.NonUniqueSecurityInfoException;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.lwm2m.server.adaptors.LwM2MCredentials;

@Slf4j
public class LwM2mInMemorySecurityStore extends InMemorySecurityStore {

//    @Autowired
//    private LwM2MTransportCtx context;

    @SneakyThrows
    @Override
    public SecurityInfo getByIdentity(String identity) {
        SecurityInfo info = getPSK (identity);
        super.add(info);


        return null;
    }

    private SecurityInfo getPSK (String identity) {
        final SecurityInfo[] result = {null};
        LwM2MTransportCtx context = new LwM2MTransportCtx();
        TransportProtos.ValidateDeviceLwM2MCredentialsRequestMsg msg = TransportProtos.ValidateDeviceLwM2MCredentialsRequestMsg.newBuilder().setCredentialsId(identity).build();
        context.getTransportService().process(msg, new TransportServiceCallback<TransportProtos.ValidateDeviceLwM2MCredentialsResponseMsg>() {
            @Override
            public void onSuccess(TransportProtos.ValidateDeviceLwM2MCredentialsResponseMsg msg) {
                log.info("msg: [{}]", msg);
                String ingfosStr = "{\"endpoint\":\"client1\",\"psk\":{\"identity\":\"client1\",\"key\":\"67f6aad1db5e9bdb9778a35e7f4f24f221c8646ce23cb8cf852fedee029cda9c\"},\"rpk\":null,\"x509\":false}";
                LwM2MCredentials lwM2MCredentials = new LwM2MCredentials();
                result[0] = lwM2MCredentials.getSecurityInfo(ingfosStr);

            }

            @Override
            public void onError(Throwable e) {
                log.trace("[{}] Failed to process credentials PSK: {}", identity, e);
                result[0] = null;
            }
        });
        return result[0];

    }

}
