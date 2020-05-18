package org.thingsboard.server.transport.lwm2m.server;

import org.checkerframework.checker.units.qual.A;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.request.DiscoverRequest;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.response.DiscoverResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.server.californium.LeshanServer;
import org.eclipse.leshan.server.registration.Registration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

import static org.eclipse.leshan.server.bootstrap.DefaultBootstrapHandler.DEFAULT_TIMEOUT;


public class LwM2MTransportService {

    private final LeshanServer lhServer;
    private final LwM2MTransportCtx lwm2mTransportContext;


    public LwM2MTransportService(LeshanServer lhServer, LwM2MTransportCtx lwm2mTransportContext) {
        this.lhServer = lhServer;
        this.lwm2mTransportContext = lwm2mTransportContext;
    }

    // /endPoint : get client
    public Registration getClient(String clientEndpoint) {
       return  this.lhServer.getRegistrationService().getByEndpoint(clientEndpoint);
    }

    // /clients/endPoint/LWRequest/discover : do LightWeight M2M discover request on a given client.
    public DiscoverResponse getDiscover(String target, String clientEndpoint, String timeoutParam) throws InterruptedException {
        DiscoverRequest request = new DiscoverRequest(target);
        return this.lhServer.send(getClient(clientEndpoint), request, extractTimeout(timeoutParam));
    }

    /**
     * Example^ String timeoutParam = 5 (sec);
     * DEFAULT_TIMEOUT = 10000 in yml
     * @param timeoutParam
     * @return
     */
    private long extractTimeout(String timeoutParam) {
        long timeout;
        if (timeoutParam != null) {
            try {
                timeout = Long.parseLong(timeoutParam) * 1000;
            } catch (NumberFormatException e) {
                timeout = this.lwm2mTransportContext.getTimeout();
            }
        } else {
            timeout = this.lwm2mTransportContext.getTimeout();
        }
        return timeout;
    }

//    public void getClientValue(String path) {
//
//        if (registration != null) {
//            System.out.println("new device: " + registration.getEndpoint());
//            try {
//                String[] paths = path.substring(1).split("/");
//
////            ReadResponse response = this.lwServer.send(registration, new ReadRequest(Integer.valueOf(paths[0]),Integer.valueOf(paths[1]),Integer.valueOf(paths[2])));
////                ReadResponse response = this.lwServer.send(registration, new ReadRequest(Integer.valueOf(paths[0]), Integer.valueOf(paths[1])));
//                ReadResponse response = this.lhServer.send(registration, new ReadRequest(Integer.valueOf(paths[0])));
//                if (response.isSuccess()) {
////                    System.out.println("Device return: " + "\n" +
////                            "nanoTimestamp: " + ((Response) response.getCoapResponse()).getNanoTimestamp() +  "\n" +
////                            "code: " + ((Response) response.getCoapResponse()).getCode().text);
//                    String typeValue = response.getContent().getClass().getName().substring(response.getContent().getClass().getName().lastIndexOf(".") + 1);
//                    if (typeValue.equals("LwM2mSingleResource")) {
//                        System.out.println(typeValue + ": id = " + response.getContent().getId() + "\n" +
//                                "value: " + ((LwM2mSingleResource) response.getContent()).getValue());
//                    } else if (typeValue.equals("LwM2mObject")) {
//                        for (Map.Entry<Integer, LwM2mObjectInstance> entry : ((LwM2mObject) response.getContent()).getInstances().entrySet()) {
//                            System.out.println(entry.getKey());
//                            for (Map.Entry<Integer, LwM2mResource> entryRes : entry.getValue().getResources().entrySet()) {
//                                System.out.println(entryRes.getValue());
//                            }
//
//                        }
//                    } else if (typeValue.equals("LwM2mObjectInstance")) {
//                        for (Map.Entry<Integer, LwM2mResource> entry : ((LwM2mObjectInstance) response.getContent()).getResources().entrySet()) {
//                            System.out.println(entry.getValue());
//                        }
//                    }
////                ((LwM2mObject)response.getContent()).getInstances().forEach((key1, value1) -> System.out.println("LwM2mSingleResourceId: " + key1 +  " "  + value1.getResources().forEach((key, value) -> System.out.println(key + " " + value))));
////                        "lwM2mObjectInstanceId: " + ((LwM2mObject)response.getContent()).getInstance(0).getId() + "\n" +
////                        "lwM2mObjectInstanceId: " + ((LwM2mObject)response.getContent()).getInstance(0).getResources());
//                } else {
//                    System.out.println("Failed to read:" + response.getCode() + " " + response.getErrorMessage());
//                }
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
//    }
}
