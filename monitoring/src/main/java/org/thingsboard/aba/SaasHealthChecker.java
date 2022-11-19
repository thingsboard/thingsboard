/**
 * Copyright © 2016-2022 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.aba;

import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.crawler.license.service.NotifyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.support.HttpRequestWrapper;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.thingsboard.rest.client.RestClient;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.security.DeviceCredentials;

import javax.annotation.PostConstruct;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Component
@Slf4j
public class SaasHealthChecker {

    //    private String url = "";
    private String username = "paromskiy@gmail.com";
    private String pass = "123test123";
    private String deviceName = "healthcheckDevice";
    private String dashboardId = "";

    @Value("${saas.host}")
    private String host;

    @Value("${saas.check.interval.sec}")
    private long refreshIntervalSec;

    @Value("${saas.notifyThreshold.ms}")
    private long notifyThreshold;

    @Autowired
    private NotifyService notifyService;

    @Autowired
    private SaasApi saasApi;

    private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private AtomicLong counter = new AtomicLong();
    private long logStatsIntervalMs = TimeUnit.HOURS.toMillis(12);
    private long lastLogStatsTime = System.currentTimeMillis();
    private long lastLatencyNotification = 0;
    private boolean inFailedState = false;

    private List<LatencyMsg> latestLatencies = Lists.newArrayList();
    private long failsCount = 0;

    @PostConstruct
    public void init() throws URISyntaxException {
        check();
        executor.scheduleWithFixedDelay(() -> {
            try {
                log.info("Start saas healthcheck");
                check();
                counter.incrementAndGet();
                log.info("Finish saas healthcheck");
//                if (Calendar.getInstance().get(Calendar.HOUR_OF_DAY) == 1) {
//                    notifyService.sendInSlackSaasHealth("I AM ALIVE. Checks performed: " + counter.get());
//                }
                if(System.currentTimeMillis() - lastLogStatsTime > logStatsIntervalMs) {
                    lastLogStatsTime = System.currentTimeMillis();
                    logIntervalStats();
                    notifyService.sendInSlackSaasHealth("I AM ALIVE. Checks performed: " + counter.get());
                }
            } catch (Exception ex) {
                log.error("Scheduled error", ex);
            }

        }, refreshIntervalSec, refreshIntervalSec, TimeUnit.SECONDS);
    }

    private void check() throws URISyntaxException {
        try {
            // load dashboard
            LatencyMsg latency = new LatencyMsg();
            RestClient restClient = buildClient(latency);
            Device device = getDevice(restClient, latency);
            DeviceCredentials deviceCred = getDeviceCred(restClient, device, latency);


            saasApi.checkMqtt(device, deviceCred, restClient, latency);
            saasApi.checkHttp(device, deviceCred, restClient, latency);


            processLatencyObj(latency, restClient, deviceCred);
            latestLatencies.add(latency);
            if (inFailedState) {
                inFailedState = false;
                notifyService.sendInSlackSaasHealth("service restored");
            }
        } catch (Exception ex) {
            failsCount++;
            log.error("Error while check Saas health", ex);
            if (!inFailedState || System.currentTimeMillis() - lastLatencyNotification > TimeUnit.MINUTES.toHours(15)) {
                String msg = "########################## \n ERROR \n " + ex.getMessage();
                lastLatencyNotification = System.currentTimeMillis();
                inFailedState = true;
                notifyService.sendInSlackSaasHealth(msg);
            }
        }
    }

    private void processLatencyObj(LatencyMsg latency, RestClient restClient, DeviceCredentials credentials) throws URISyntaxException {
        System.out.println();
        System.out.println(latency);
        System.out.println();


        JsonObject values = new JsonObject();
        values.addProperty("restClientLoginLatency", latency.getRestClientLoginLatency());
        values.addProperty("deviceCredLatency", latency.getDeviceCredLatency());
        values.addProperty("getDeviceLatency", latency.getGetDeviceLatency());
        values.addProperty("wsSubInitLatency", latency.getWsSubInitLatency());
        values.addProperty("mqttConnectLatency", latency.getMqttConnectLatency());
        values.addProperty("mqttSendLatency", latency.getMqttSendLatency());
        values.addProperty("mqttTotalLatency", latency.getMqttTotalLatency());
        values.addProperty("httpSendLatency", latency.getHttpSendLatency());
        values.addProperty("httpTotalLatency", latency.getHttpTotalLatency());
        values.addProperty("mqttErrors", latency.getMqttErrors());
        values.addProperty("mqttReconnects", latency.getMqttReconnects());

        JsonObject payload = new JsonObject();
        payload.addProperty("ts", System.currentTimeMillis());
        payload.add("values", values);

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setInterceptors(Collections.singletonList((httpRequest, bytes, clientHttpRequestExecution) -> {
            HttpRequest wrapper = new HttpRequestWrapper(httpRequest);
            wrapper.getHeaders().set("X-Authorization", "Bearer " + restClient.getToken());
            return clientHttpRequestExecution.execute(wrapper, bytes);
        }));


        restTemplate.postForEntity("https://" + host + "/api/v1/" + credentials.getCredentialsId() + "/telemetry", payload.toString(), String.class);

        if (latency.hasLongLatency(notifyThreshold) && System.currentTimeMillis() - lastLatencyNotification > TimeUnit.MINUTES.toHours(60)) {
            lastLatencyNotification = System.currentTimeMillis();
            notifyService.sendInSlackSaasHealth("Some SaaS latencies are upper threshold [" + notifyThreshold + "ms] \n " + latency);
        }
    }

    private Device getDevice(RestClient restClient, LatencyMsg latencyMsg) {
        long start = System.currentTimeMillis();
        Optional<Device> tenantDevice = restClient.getTenantDevice(deviceName);
        Device device = tenantDevice.orElseThrow(() -> new IllegalStateException("Device [" + deviceName + "] was not found"));
        latencyMsg.setGetDeviceLatency(System.currentTimeMillis() - start);
        log.info("Device loaded");
        return device;
    }

    private DeviceCredentials getDeviceCred(RestClient client, Device device, LatencyMsg latencyMsg) {
        long start = System.currentTimeMillis();
        Optional<DeviceCredentials> credentialsOptional = client.getDeviceCredentialsByDeviceId(device.getId());
        DeviceCredentials credentials = credentialsOptional.orElseThrow(() -> new IllegalStateException("Could not load device credentials"));
        latencyMsg.setDeviceCredLatency(System.currentTimeMillis() - start);
        log.info("Device cred loaded");
        return credentials;
    }

    private RestClient buildClient(LatencyMsg latencyMsg) {
        try {
            long start = System.currentTimeMillis();
            RestClient client = new RestClient("https://" + host);
            client.login(username, pass);
            latencyMsg.setRestClientLoginLatency(System.currentTimeMillis() - start);
            log.info("Rest client ready");
            return client;
        } catch (Exception ex) {
            throw new IllegalStateException("Could not login: " + ex.getMessage(), ex);
        }
    }

    private void logIntervalStats() throws URISyntaxException {
        List<Long> httpTotal = latestLatencies.stream().map(LatencyMsg::getHttpTotalLatency).collect(Collectors.toList());
        List<Long> login = latestLatencies.stream().map(LatencyMsg::getRestClientLoginLatency).collect(Collectors.toList());
        List<Long> getDevice = latestLatencies.stream().map(LatencyMsg::getDeviceCredLatency).collect(Collectors.toList());
        long mqttErrorsCnt = latestLatencies.stream().mapToLong(LatencyMsg::getMqttErrors).sum();
        long mqttReconectCnt = latestLatencies.stream().mapToLong(LatencyMsg::getMqttReconnects).sum();
        String msg = "[Interval stats] Fails count: " + failsCount + " ok count: " + latestLatencies.size() + "\n";
        if (latestLatencies.size() > 0) {
            msg += "\t http send -receive dalay: " + toPercentile(httpTotal) + "\n";
            msg += "\t http login: " + toPercentile(login) + "\n";
            msg += "\t get device: " + toPercentile(getDevice) + "\n";
            msg += "\t mqtt errors: " + mqttErrorsCnt + "\n";
            msg += "\t mqtt reconnects: " + mqttReconectCnt + "\n";
        }
        failsCount = 0;
        latestLatencies.clear();

        log.info("Interval stats: {}" , msg);
        notifyService.sendInSlackSaasHealth(msg);
    }

    private String toPercentile(List<Long> values) {
        return "75%: " + percentile(values, 75) + "ms; \t90%:" + percentile(values, 90) + "ms; \t95%:" + percentile(values, 95) + "ms; \tmin: " + percentile(values, 1) + "ms; \tmax: " + percentile(values, 99.99);
    }

    private static Long percentile(List<Long> values, double percentile) {
        Collections.sort(values);
        int index = (int) Math.ceil(percentile / 100.0 * values.size());
        return (long) ((Math.round(values.get(index - 1) * 100)) / 100d);
    }
}
