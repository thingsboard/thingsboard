package org.thingsboard.server.http;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.server.TransportObserver;
import org.thingsboard.server.TransportType;

@Component
public class HttpObserver implements TransportObserver {

    @Value("${http.monitoring_rate}")
    private int monitoringRate;

    @Value("${http.timeout}")
    private int timeout;

    @Override
    public String pingTransport(String payload) {
//        HttpGet getMethod = new HttpGet("http://localhost:8080/httpclient-simple/api/bars/1");
//
//        int hardTimeout = 5; // seconds
//        TimerTask task = new TimerTask() {
//            @Override
//            public void run() {
//                if (getMethod != null) {
//                    getMethod.abort();
//                }
//            }
//        };
//        new Timer(true).schedule(task, hardTimeout * 1000);
//
//        RequestConfig config = RequestConfig.custom()
//                .setConnectTimeout(timeout * 1000)
//                .setConnectionRequestTimeout(timeout * 1000)
//                .setSocketTimeout(timeout * 1000).build();
//
//        CloseableHttpClient client = HttpClientBuilder.create().setDefaultRequestConfig(config).build();
//
//        HttpResponse response = null;
//        try {
//            response = client.execute(getMethod);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        System.out.println(
//                "HTTP Status of response: " + response.getStatusLine().getStatusCode());
//
//
        return "";
    }

    @Override
    public int getMonitoringRate() {
        return monitoringRate;
    }

    @Override
    public TransportType getTransportType(String msg) {
        TransportType mqtt = TransportType.HTTP;
        mqtt.setInfo(msg);
        return mqtt;
    }
}
