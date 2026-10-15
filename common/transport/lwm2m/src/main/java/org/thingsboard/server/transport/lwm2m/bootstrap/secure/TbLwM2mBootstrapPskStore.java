// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.transport.lwm2m.bootstrap.secure;

import org.eclipse.californium.scandium.dtls.ConnectionId;
import org.eclipse.californium.scandium.dtls.PskPublicInformation;
import org.eclipse.californium.scandium.dtls.PskSecretResult;
import org.eclipse.californium.scandium.util.SecretUtil;
import org.eclipse.californium.scandium.util.ServerNames;
import org.eclipse.leshan.server.californium.bootstrap.LwM2mBootstrapPskStore;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.thingsboard.server.transport.lwm2m.bootstrap.store.LwM2MBootstrapSecurityStore;
import org.thingsboard.server.transport.lwm2m.secure.TbLwM2mDtlsHandshakeListener;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2MTransportUnavailableException;

import javax.crypto.SecretKey;

public class TbLwM2mBootstrapPskStore extends LwM2mBootstrapPskStore {

    private final LwM2MBootstrapSecurityStore bsSecurityStore;
    private final TbLwM2mDtlsHandshakeListener handshakeListener;

    public TbLwM2mBootstrapPskStore(LwM2MBootstrapSecurityStore bsSecurityStore, TbLwM2mDtlsHandshakeListener handshakeListener) {
        super(bsSecurityStore);
        this.bsSecurityStore = bsSecurityStore;
        this.handshakeListener = handshakeListener;
    }

    @Override
    public PskSecretResult requestPskSecretResult(ConnectionId cid, ServerNames serverName, PskPublicInformation identity, String hmacAlgorithm,
                                                  SecretKey otherSecret, byte[] seed, boolean useExtendedMasterSecret) {
        SecurityInfo info;
        try {
            info = bsSecurityStore.getByPskIdentity(identity.getPublicInfoAsString());
        } catch (LwM2MTransportUnavailableException e) {
            return new PskSecretResult(cid, identity, null);
        }
        if (info == null || info.getPreSharedKey() == null) {
            handshakeListener.onUnknownPskIdentity(cid);
            return new PskSecretResult(cid, identity, null);
        }
        return new PskSecretResult(cid, identity, SecretUtil.create(info.getPreSharedKey(), "PSK"));
    }

}
