package org.thingsboard.server.transport.lwm2m.bootstrap.secure;

import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.server.bootstrap.BootstrapConfig;
import org.eclipse.leshan.server.bootstrap.EditableBootstrapConfigStore;
import org.eclipse.leshan.server.bootstrap.InvalidConfigurationException;
import org.eclipse.leshan.server.security.BootstrapSecurityStore;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.thingsboard.server.transport.lwm2m.bootstrap.LwM2MTransportContextBootstrap;
import org.thingsboard.server.transport.lwm2m.secure.LwM2MGetSecurityInfo;
import org.thingsboard.server.transport.lwm2m.secure.LwM2MSecurityMode;
import org.thingsboard.server.transport.lwm2m.secure.ReadResultSecurityStore;
import org.thingsboard.server.transport.lwm2m.utils.TypeServer;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Component("LwM2MBootstrapSecurityStore")
@ConditionalOnExpression("'${service.type:null}'=='tb-transport' || ('${service.type:null}'=='monolith' && '${transport.lwm2m.enabled}'=='true'&& '${transport.lwm2m.bootstrap.enable}'=='true')")
public class LwM2MBootstrapSecurityStore implements BootstrapSecurityStore {

    private final EditableBootstrapConfigStore bootstrapConfigStore;
    private final String endpointSubIdent = "_identity";

    @Autowired
    LwM2MGetSecurityInfo lwM2MGetSecurityInfo;

    @Autowired
    LwM2MTransportContextBootstrap contextBS;

    public LwM2MBootstrapSecurityStore(EditableBootstrapConfigStore bootstrapConfigStore) {
        this.bootstrapConfigStore = bootstrapConfigStore;
    }

    @Override
    public List<SecurityInfo> getAllByEndpoint(String endpoint) {
        String endpointKey = endpoint;
        ReadResultSecurityStore store = lwM2MGetSecurityInfo.getSecurityInfo(endpointKey, TypeServer.BOOTSTRAP);
        if (store.getBootstrapJson() == null) {
            endpointKey = endpoint + this.endpointSubIdent;
            store = lwM2MGetSecurityInfo.getSecurityInfo(endpointKey, TypeServer.BOOTSTRAP);
        }
        if (store.getBootstrapJson() != null) {
            /** add value to store  from BootstrapJson */
            setBootstrapConfig (store);
            BootstrapConfig bsConfigNew = store.getLwM2MBootstrapConfig();
            try {
                bootstrapConfigStore.add(endpoint, bsConfigNew);
            } catch (InvalidConfigurationException e) {
                e.printStackTrace();
            }
            return Arrays.asList(store.getSecurityInfo());
        }
        return null;
//        String jsonString =
//                "{\"server\":{\"x509\":{\"endpoint\":\"client_lwm2m_psk\",\"identity\":\"client_psk_identity\",\"key\":\"67f6aad1db5e9bdb9778a35e7f4f24f221c8646ce23cb8cf852fedee029cda9c\"}},\"bootstrap\":{\"client_lwm2m_psk\":{\"toDelete\":[\"/0\",\"/1\"],\"servers\":{\"0\":{\"shortId\":123,\"lifetime\":300,\"defaultMinPeriod\":1,\"notifIfDisabled\":true,\"binding\":\"U\"}},\"security\":{\"0\":{\"uri\":\"coaps://localhost:5688\",\"bootstrapServer\":true,\"securityMode\":\"PSK\",\"publicKeyOrId\":[99,108,105,101,110,116,95,108,119,109,50,109,95,112,115,107,95,105,100,101,110,116,105,116,121],\"serverPublicKey\":[],\"secretKey\":[1,27,105,-87,107,-91,-128,-11,123,60,-88,113,-115,-96,59,-112,110,-93,-101,58,-35,-103,-116,76,-32,-30,84,100,-66,18,73,77],\"smsSecurityMode\":\"NO_SEC\",\"smsBindingKeyParam\":[],\"smsBindingKeySecret\":[],\"serverSmsNumber\":\"\",\"serverId\":111,\"clientOldOffTime\":1,\"bootstrapServerAccountTimeout\":0},\"1\":{\"uri\":\"coaps://localhost:5686\",\"bootstrapServer\":false,\"securityMode\":\"PSK\",\"publicKeyOrId\":[99,108,105,101,110,116,95,108,119,109,50,109,95,112,115,107,95,105,100,101,110,116,105,116,121],\"serverPublicKey\":[],\"secretKey\":[1,27,105,-87,107,-91,-128,-11,123,60,-88,113,-115,-96,59,-112,110,-93,-101,58,-35,-103,-116,76,-32,-30,84,100,-66,18,73,77],\"smsSecurityMode\":\"NO_SEC\",\"smsBindingKeyParam\":[],\"smsBindingKeySecret\":[],\"serverSmsNumber\":\"\",\"serverId\":123,\"clientOldOffTime\":1,\"bootstrapServerAccountTimeout\":0}},\"acls\":{}}}}";
//            JSONObject jsonObject = new JSONObject(jsonString);
//            String json = "{ \"name\": \"Baeldung\", \"java\": true }";
//        JsonObject credentialDevice = new JsonParser().parse(jsonString).getAsJsonObject();
//        JsonObject bootstrapJson = credentialDevice.get("bootstrap").getAsJsonObject();
//        JsonObject identityJson = bootstrapJson.get("client_lwm2m_psk").getAsJsonObject();
//        Gson gson = new Gson ();
//        BootstrapConfig bsConfigNew = gson.fromJson(identityJson, BootstrapConfig.class);
//        try {
//            bootstrapConfigStore.add(endpoint, bsConfigNew);
//        } catch (InvalidConfigurationException e) {
//            e.printStackTrace();
//        }
//        BootstrapConfig bsConfig = bootstrapConfigStore.get(endpoint, null, null);
//
//        if (bsConfig == null || bsConfig.security == null)
//            return null;
//
//        for (Map.Entry<Integer, BootstrapConfig.ServerSecurity> bsEntry : bsConfig.security.entrySet()) {
//            BootstrapConfig.ServerSecurity value = bsEntry.getValue();

            // Extract PSK security info
//            if (store.getSecurityMode() == LwM2MSecurityMode.PSK.code) {
////                SecurityInfo securityInfo = SecurityInfo.newPreSharedKeyInfo(endpoint,
////                        new String(value.publicKeyOrId, StandardCharsets.UTF_8), value.secretKey);
//                return Arrays.asList(store.getSecurityInfo());
//            }
//            // Extract RPK security info
//            else if (store.getSecurityMode() == LwM2MSecurityMode.RPK.code) {
//                try {
//                    SecurityInfo securityInfo = SecurityInfo.newRawPublicKeyInfo(endpoint,
//                            SecurityUtil.publicKey.decode(value.publicKeyOrId));
//                    return Arrays.asList(securityInfo);
//                } catch (IOException | GeneralSecurityException e) {
//                    log.error("Unable to decode Client public key for [{}]  [{}]", endpoint, e.getMessage());
//                    return null;
//                }
//            }
//            // Extract X509 security info
//            else if (store.getSecurityMode() == LwM2MSecurityMode.X509.code) {
//                SecurityInfo securityInfo = SecurityInfo.newX509CertInfo(endpoint);
//                return Arrays.asList(securityInfo);
//            }
//        }

    }

    @Override
    public SecurityInfo getByIdentity(String identity) {
//        byte[] identityBytes = identity.getBytes(StandardCharsets.UTF_8);

        ReadResultSecurityStore store = lwM2MGetSecurityInfo.getSecurityInfo(identity, TypeServer.BOOTSTRAP);

        if (store.getSecurityMode() < LwM2MSecurityMode.DEFAULT_MODE.code) {
            return store.getSecurityInfo();
        }

//        String jsonString =
//                "{\"server\":{\"psk\":{\"endpoint\":\"client_lwm2m_psk\",\"identity\":\"client_psk_identity\",\"key\":\"67f6aad1db5e9bdb9778a35e7f4f24f221c8646ce23cb8cf852fedee029cda9c\"}},\"bootstrap\":{\"client_lwm2m_psk\":{\"toDelete\":[\"/0\",\"/1\"],\"servers\":{\"0\":{\"shortId\":123,\"lifetime\":300,\"defaultMinPeriod\":1,\"notifIfDisabled\":true,\"binding\":\"U\"}},\"security\":{\"0\":{\"uri\":\"coaps://localhost:5688\",\"bootstrapServer\":true,\"securityMode\":\"PSK\",\"publicKeyOrId\":[99,108,105,101,110,116,95,108,119,109,50,109,95,112,115,107,95,105,100,101,110,116,105,116,121],\"serverPublicKey\":[],\"secretKey\":[1,27,105,-87,107,-91,-128,-11,123,60,-88,113,-115,-96,59,-112,110,-93,-101,58,-35,-103,-116,76,-32,-30,84,100,-66,18,73,77],\"smsSecurityMode\":\"NO_SEC\",\"smsBindingKeyParam\":[],\"smsBindingKeySecret\":[],\"serverSmsNumber\":\"\",\"serverId\":111,\"clientOldOffTime\":1,\"bootstrapServerAccountTimeout\":0},\"1\":{\"uri\":\"coaps://localhost:5686\",\"bootstrapServer\":false,\"securityMode\":\"PSK\",\"publicKeyOrId\":[99,108,105,101,110,116,95,108,119,109,50,109,95,112,115,107,95,105,100,101,110,116,105,116,121],\"serverPublicKey\":[],\"secretKey\":[1,27,105,-87,107,-91,-128,-11,123,60,-88,113,-115,-96,59,-112,110,-93,-101,58,-35,-103,-116,76,-32,-30,84,100,-66,18,73,77],\"smsSecurityMode\":\"NO_SEC\",\"smsBindingKeyParam\":[],\"smsBindingKeySecret\":[],\"serverSmsNumber\":\"\",\"serverId\":123,\"clientOldOffTime\":1,\"bootstrapServerAccountTimeout\":0}},\"acls\":{}}}}";
//            JSONObject jsonObject = new JSONObject(jsonString);
//            String json = "{ \"name\": \"Baeldung\", \"java\": true }";
//        JsonObject credentialDevice = new JsonParser().parse(jsonString).getAsJsonObject();
//        JsonObject bootstrapJson = credentialDevice.get("bootstrap").getAsJsonObject();
//        JsonObject identityJson = bootstrapJson.get("client_lwm2m_psk").getAsJsonObject();
//        Gson gson = new Gson ();
//        BootstrapConfig bsConfig = gson.fromJson(identityJson, BootstrapConfig.class);
//        log.info("bsConfig: [{}]", bsConfig);
//        if (bsConfig.security != null) {
//            for (Map.Entry<Integer, BootstrapConfig.ServerSecurity> ec : bsConfig.security.entrySet()) {
//                BootstrapConfig.ServerSecurity serverSecurity = ec.getValue();
//                if (serverSecurity.bootstrapServer && serverSecurity.securityMode == SecurityMode.PSK
//                        && Arrays.equals(serverSecurity.publicKeyOrId, identityBytes)) {
//                    return SecurityInfo.newPreSharedKeyInfo("client_lwm2m_psk", identity, serverSecurity.secretKey);
//                }
//            }
//        }

//            ObjectMapper mapper = new ObjectMapper();
//
//            BootstrapConfig bsConfig = mapper.readValue(bootstrapJson, BootstrapConfig.class);

        //        BootstrapConfig bsConfig;
//        BootstrapConfig bsConfig = e.getValue();
        return null;
    }

    private void setBootstrapConfig (ReadResultSecurityStore store) {
        JsonObject object = store.getBootstrapJson();
//        store.setPortBootstrapBs();
//        store.setHostBootstrapBs();
//        store.setServerPublicBootstrapBs();
    }

}
