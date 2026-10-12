// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.transport.lwm2m.secure;

import org.eclipse.californium.scandium.dtls.ConnectionId;
import org.eclipse.californium.scandium.dtls.PskPublicInformation;
import org.eclipse.californium.scandium.dtls.PskSecretResult;
import org.eclipse.californium.scandium.util.SecretUtil;
import org.eclipse.californium.scandium.util.ServerNames;
import org.eclipse.leshan.server.californium.LwM2mPskStore;
import org.eclipse.leshan.server.registration.RegistrationStore;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2MTransportUnavailableException;
import org.thingsboard.server.transport.lwm2m.server.store.TbSecurityStore;

import javax.crypto.SecretKey;

/**
 * Counts unknown pre-shared key identities as failed authentication of the client's address.
 */
public class TbLwM2mPskStore extends LwM2mPskStore {

    private final TbSecurityStore securityStore;
    private final TbLwM2mDtlsHandshakeListener handshakeListener;

    public TbLwM2mPskStore(TbSecurityStore securityStore, RegistrationStore registrationStore, TbLwM2mDtlsHandshakeListener handshakeListener) {
        super(securityStore, registrationStore);
        this.securityStore = securityStore;
        this.handshakeListener = handshakeListener;
    }

    @Override
    public PskSecretResult requestPskSecretResult(ConnectionId cid, ServerNames serverName, PskPublicInformation identity, String hmacAlgorithm,
                                                  SecretKey otherSecret, byte[] seed, boolean useExtendedMasterSecret) {
        SecurityInfo info;
        try {
            info = securityStore.getByPskIdentity(identity.getPublicInfoAsString());
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
