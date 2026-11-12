// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.msa.integration.chirpstack;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.macs.CMac;
import org.bouncycastle.crypto.params.KeyParameter;
import org.thingsboard.common.util.JacksonUtil;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Base64;

/**
 * Builds a LoRaWAN 1.0.x "unconfirmed data up" PHYPayload (with a valid MIC and
 * AES-encrypted FRMPayload) and wraps it into the ChirpStack gateway
 * {@code gw.UplinkFrame} JSON that is published to the gateway MQTT event topic
 * {@code <region>/gateway/<gatewayId>/event/up}. ChirpStack validates the MIC,
 * decrypts the FRMPayload and forwards the resulting uplink to its application
 * integrations — i.e. this produces a genuine uplink, not a faked HTTP POST.
 */
public final class LoRaWanUplinkFrameBuilder {

    private static final int UPLINK_DIRECTION = 0x00;
    private static final byte MHDR_UNCONFIRMED_UP = 0x40;
    // Standard EU868 SF7BW125 settings; matches one of the region channels.
    private static final long FREQUENCY_HZ = 868100000L;
    private static final int BANDWIDTH_HZ = 125000;
    private static final int SPREADING_FACTOR = 7;
    // Gateway-reported radio metadata; exposed so tests can assert the forwarded values.
    public static final int RSSI = -130;
    public static final double SNR = 11.5;

    private LoRaWanUplinkFrameBuilder() {
    }

    /**
     * @param devAddr  4-byte device address (big-endian / as displayed)
     * @param nwkSKey  16-byte network session key
     * @param appSKey  16-byte application session key
     * @param fCnt     uplink frame counter
     * @param fPort    frame port (1-223)
     * @param payload  decrypted application payload bytes
     * @param gatewayId 8-byte gateway EUI as a 16-char hex string
     */
    public static String buildUplinkFrameJson(byte[] devAddr, byte[] nwkSKey, byte[] appSKey,
                                              int fCnt, int fPort, byte[] payload, String gatewayId) {
        byte[] phyPayload = buildPhyPayload(devAddr, nwkSKey, appSKey, fCnt, fPort, payload);

        ObjectNode frame = JacksonUtil.newObjectNode();
        frame.put("phyPayload", Base64.getEncoder().encodeToString(phyPayload));

        ObjectNode lora = JacksonUtil.newObjectNode();
        lora.put("bandwidth", BANDWIDTH_HZ);
        lora.put("spreadingFactor", SPREADING_FACTOR);
        lora.put("codeRate", "CR_4_5");
        ObjectNode modulation = JacksonUtil.newObjectNode();
        modulation.set("lora", lora);
        ObjectNode txInfo = JacksonUtil.newObjectNode();
        txInfo.put("frequency", FREQUENCY_HZ);
        txInfo.set("modulation", modulation);
        frame.set("txInfo", txInfo);

        ObjectNode rxInfo = JacksonUtil.newObjectNode();
        rxInfo.put("gatewayId", gatewayId);
        rxInfo.put("uplinkId", 1);
        rxInfo.put("rssi", RSSI);
        rxInfo.put("snr", SNR);
        rxInfo.put("channel", 0);
        rxInfo.put("context", Base64.getEncoder().encodeToString(new byte[]{0, 0, 0, 1}));
        frame.set("rxInfo", rxInfo);

        return JacksonUtil.toString(frame);
    }

    static byte[] buildPhyPayload(byte[] devAddr, byte[] nwkSKey, byte[] appSKey,
                                  int fCnt, int fPort, byte[] payload) {
        byte[] devAddrLe = reverse(devAddr);
        byte[] fCntLe2 = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(fCnt).array();

        byte[] encryptedPayload = encryptPayload(appSKey, devAddrLe, fCnt, payload);

        // MACPayload = FHDR | FPort | FRMPayload; FHDR = DevAddr(LE) | FCtrl | FCnt(LE, 2 bytes)
        ByteArrayOutputStream macPayload = new ByteArrayOutputStream();
        macPayload.writeBytes(devAddrLe);
        macPayload.write(0x00); // FCtrl: no ADR, no FOpts
        macPayload.write(fCntLe2[0]);
        macPayload.write(fCntLe2[1]);
        macPayload.write(fPort & 0xFF);
        macPayload.writeBytes(encryptedPayload);

        ByteArrayOutputStream msg = new ByteArrayOutputStream();
        msg.write(MHDR_UNCONFIRMED_UP);
        msg.writeBytes(macPayload.toByteArray());
        byte[] msgBytes = msg.toByteArray();

        byte[] mic = computeMic(nwkSKey, devAddrLe, fCnt, msgBytes);

        ByteArrayOutputStream phy = new ByteArrayOutputStream();
        phy.writeBytes(msgBytes);
        phy.writeBytes(mic);
        return phy.toByteArray();
    }

    // FRMPayload encryption per LoRaWAN spec (AES-128 of S blocks XORed with payload).
    private static byte[] encryptPayload(byte[] appSKey, byte[] devAddrLe, int fCnt, byte[] payload) {
        byte[] result = new byte[payload.length];
        int blocks = (payload.length + 15) / 16;
        for (int i = 1; i <= blocks; i++) {
            byte[] a = buildBlock((byte) 0x01, devAddrLe, fCnt, i);
            byte[] s = aesEncryptBlock(appSKey, a);
            int offset = (i - 1) * 16;
            int len = Math.min(16, payload.length - offset);
            for (int j = 0; j < len; j++) {
                result[offset + j] = (byte) (payload[offset + j] ^ s[j]);
            }
        }
        return result;
    }

    // MIC = first 4 bytes of AES-CMAC(nwkSKey, B0 | msg).
    private static byte[] computeMic(byte[] nwkSKey, byte[] devAddrLe, int fCnt, byte[] msg) {
        byte[] b0 = buildBlock((byte) 0x49, devAddrLe, fCnt, msg.length);

        CMac cmac = new CMac(AESEngine.newInstance());
        cmac.init(new KeyParameter(nwkSKey));
        cmac.update(b0, 0, b0.length);
        cmac.update(msg, 0, msg.length);
        byte[] out = new byte[cmac.getMacSize()];
        cmac.doFinal(out, 0);

        byte[] mic = new byte[4];
        System.arraycopy(out, 0, mic, 0, 4);
        return mic;
    }

    // Builds the 16-byte A_i / B_0 block: <prefix> 00 00 00 00 dir devAddr(LE) fCnt(LE,4) 00 <last>.
    // For A_i (encryption) <last> is the block index; for B_0 (MIC) <last> is the message length.
    private static byte[] buildBlock(byte prefix, byte[] devAddrLe, int fCnt, int lastByte) {
        byte[] fCntLe4 = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(fCnt).array();
        byte[] block = new byte[16];
        block[0] = prefix;
        // bytes 1..4 are zero
        block[5] = (byte) UPLINK_DIRECTION;
        System.arraycopy(devAddrLe, 0, block, 6, 4);
        System.arraycopy(fCntLe4, 0, block, 10, 4);
        block[14] = 0x00;
        block[15] = (byte) lastByte;
        return block;
    }

    private static byte[] aesEncryptBlock(byte[] key, byte[] block) {
        AESEngine engine = (AESEngine) AESEngine.newInstance();
        engine.init(true, new KeyParameter(key));
        byte[] out = new byte[16];
        engine.processBlock(block, 0, out, 0);
        return out;
    }

    private static byte[] reverse(byte[] in) {
        byte[] out = new byte[in.length];
        for (int i = 0; i < in.length; i++) {
            out[i] = in[in.length - 1 - i];
        }
        return out;
    }
}
