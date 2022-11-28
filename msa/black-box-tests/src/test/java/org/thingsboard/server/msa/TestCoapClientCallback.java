package org.thingsboard.server.msa;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;

import java.util.concurrent.CountDownLatch;

@Slf4j
@Data
public class TestCoapClientCallback implements CoapHandler {

    protected final CountDownLatch latch;
    protected Integer observe;
    protected byte[] payloadBytes;
    protected CoAP.ResponseCode responseCode;

    public TestCoapClientCallback() {
        this.latch = new CountDownLatch(1);
    }

    public TestCoapClientCallback(int subscribeCount) {
        this.latch = new CountDownLatch(subscribeCount);
    }

    public Integer getObserve() {
        return observe;
    }

    public byte[] getPayloadBytes() {
        return payloadBytes;
    }

    public CoAP.ResponseCode getResponseCode() {
        return responseCode;
    }

    @Override
    public void onLoad(CoapResponse response) {
        observe = response.getOptions().getObserve();
        payloadBytes = response.getPayload();
        responseCode = response.getCode();
        latch.countDown();
    }

    @Override
    public void onError() {
        log.warn("Command Response Ack Error, No connect");
    }

}
