/*******************************************************************************
 * Copyright (c) 2018 Sierra Wireless and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 * 
 * Contributors:
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package org.thingsboard.server.transport.lwm2m.integration.tests;

import org.eclipse.californium.scandium.dtls.PskPublicInformation;
import org.eclipse.californium.scandium.dtls.pskstore.PskStore;
import org.eclipse.californium.scandium.util.SecretUtil;
import org.eclipse.californium.scandium.util.ServerNames;

import javax.crypto.SecretKey;
import java.net.InetSocketAddress;

public class SinglePSKStore implements PskStore {

    private PskPublicInformation identity;
    private SecretKey key;

    public SinglePSKStore(PskPublicInformation identity, byte[] key) {
        this.identity = identity;
        this.key = SecretUtil.create(key, "PSK");
    }

    public SinglePSKStore(PskPublicInformation identity, SecretKey key) {
        this.identity = identity;
        this.key = key;
    }

    @Override
    public SecretKey getKey(PskPublicInformation identity) {
        return SecretUtil.create(key);
    }

    @Override
    public SecretKey getKey(ServerNames serverName, PskPublicInformation identity) {
        // we do not support SNI
        return getKey(identity);
    }

    @Override
    public PskPublicInformation getIdentity(InetSocketAddress inetAddress) {
        return identity;
    }

    @Override
    public PskPublicInformation getIdentity(InetSocketAddress peerAddress, ServerNames virtualHost) {
        throw new UnsupportedOperationException();
    }

    public void setKey(byte[] key) {
        this.key = SecretUtil.create(key, "PSK");
    }

    public void setIdentity(String identity) {
        this.identity = new PskPublicInformation(identity);
    }
}
