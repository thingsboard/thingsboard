/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.service.lwm2m;


import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.model.ObjectModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.lwm2m.*;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.service.Validator;
import org.thingsboard.server.transport.lwm2m.bootstrap.LwM2MTransportContextBootstrap;
import org.thingsboard.server.transport.lwm2m.secure.LwM2MSecurityMode;
import org.thingsboard.server.transport.lwm2m.server.LwM2MTransportContextServer;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.thingsboard.server.dao.service.Validator.validateId;

@Slf4j
@Service
@ConditionalOnExpression("('${(service.type:null}'=='tb-transport' && '${transport.lwm2m.enabled}'=='true') || '${service.type:null}'=='monolith' || '${service.type:null}'=='tb-core'")
public class LwM2MModelsRepository {

    private static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";

    @Autowired
    LwM2MTransportContextServer contextS;

    @Autowired
    LwM2MTransportContextBootstrap contextBs;

    /**
     * @param objectIds
     * @param textSearch
     * @return list of LwM2mObject
     * Filter by Predicate (uses objectIds, if objectIds is null then it uses textSearch,
     * if textSearch is null then it uses AllList from  List<ObjectModel>)
     */
    public List<LwM2mObject> getLwm2mObjects(int[] objectIds, String textSearch) {
        return getLwm2mObjects((objectIds != null && objectIds.length > 0) ?
                (ObjectModel element) -> IntStream.of(objectIds).anyMatch(x -> x == element.id) :
                (textSearch != null && !textSearch.isEmpty()) ? (ObjectModel element) -> element.name.contains(textSearch) : null);
    }

    /**
     * @param predicate
     * @return list of LwM2mObject
     */
    private List<LwM2mObject> getLwm2mObjects(Predicate<? super ObjectModel> predicate) {
        List<LwM2mObject> lwM2mObjects = new ArrayList<>();
        List<ObjectModel> listObjects = (predicate == null) ? contextS.getModelsValue() :
                contextS.getModelsValue().stream()
                        .filter(predicate)
                        .collect(Collectors.toList());
        listObjects.forEach(obj -> {
            LwM2mObject lwM2mObject = new LwM2mObject();
            lwM2mObject.setId(obj.id);
            lwM2mObject.setName(obj.name);
            LwM2mInstance instance = new LwM2mInstance();
            instance.setId(0);
            List<LwM2mResource> resources = new ArrayList<>();
            obj.resources.forEach((k, v) -> {
                if (!v.operations.isExecutable()) {
                    LwM2mResource resource = new LwM2mResource(k, v.name, false, false, false);
                    resources.add(resource);
                }
            });
            instance.setResources(resources.stream().toArray(LwM2mResource[]::new));
            lwM2mObject.setInstances(new LwM2mInstance[]{instance});
            lwM2mObjects.add(lwM2mObject);
        });
        return lwM2mObjects;
    }

    /**
     * @param tenantId
     * @param pageLink
     * @return List of LwM2mObject in PageData format
     */
    public PageData<LwM2mObject> findDeviceLwm2mObjects(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findDeviceProfileInfos tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validatePageLink(pageLink);
        return this.findLwm2mListObjects(pageLink);
    }

    /**
     * @param pageLink
     * @return List of LwM2mObject in PageData format, filter == TextSearch
     * PageNumber = 1, PageSize = List<LwM2mObject>.size()
     */
    public PageData<LwM2mObject> findLwm2mListObjects(PageLink pageLink) {
        PageImpl page = new PageImpl(getLwm2mObjects(null, pageLink.getTextSearch()));
        PageData pageData = new PageData(page.getContent(), page.getTotalPages(), page.getTotalElements(), page.hasNext());
        return pageData;
    }

    public BootstrapSecurityConfig getLwm2mBootstrapSecurityKey(String securityMode) {
        LwM2MSecurityMode lwM2MSecurityMode = LwM2MSecurityMode.fromSecurityMode(securityMode.toLowerCase());
        switch (lwM2MSecurityMode) {
            case RPK:
                return  getBootstrapSecurityInfoRPK();
            case X509:
                return getBootstrapSecurityInfoX509();
            default:
                break;
        }
        return null;
    }

    private BootstrapSecurityConfig getBootstrapSecurityInfoRPK() {
        BootstrapSecurityConfig bsConf = new BootstrapSecurityConfig();
        bsConf.setBootstrapServer(getBootstrapServer (true));
        bsConf.setLwm2mServer(getBootstrapServer (false));
        return bsConf;

    }

    private BootstrapSecurityConfig getBootstrapSecurityInfoX509() {
        BootstrapSecurityConfig bsConf = new BootstrapSecurityConfig();

        return bsConf;

    }

    private ServerSecurityConfig getBootstrapServer (boolean bootstrapServerIs) {
        ServerSecurityConfig bsServ = new ServerSecurityConfig();
        if (bootstrapServerIs) {

        }
        else {
            bsServ.setBootstrapServerIs(bootstrapServerIs);
            bsServ.setServerId(123);
        }
        return bsServ;
    }

    /**
     * export const BOOTSTRAP_PUBLIC_KEY_RPK = '3059301306072A8648CE3D020106082A8648CE3D03010703420004993EF2B698C6A9C0C1D8BE78B13A9383C0854C7C7C7A504D289B403794648183267412D5FC4E5CEB2257CB7FD7F76EBDAC2FA9AA100AFB162E990074CC0BFAA2';
     * export const LWM2M_SERVER_PUBLIC_KEY_RPK = '3059301306072A8648CE3D020106082A8648CE3D03010703420004405354EA8893471D9296AFBC8B020A5C6201B0BB25812A53B849D4480FA5F06930C9237E946A3A1692C1CAFAA01A238A077F632C99371348337512363F28212B';
     * export const BOOTSTRAP_PUBLIC_KEY_X509 = '30820249308201eca003020102020439d220d5300c06082a8648ce3d04030205003076310b3009060355040613025553310b3009060355040813024341310b300906035504071302534631143012060355040a130b5468696e6773626f61726431143012060355040b130b5468696e6773626f6172643121301f060355040313186e69636b2d5468696e6773626f6172642020726f6f7443413020170d3230303632343039313230395a180f32313230303533313039313230395a308197310b3009060355040613025553310b3009060355040813024341310b300906035504071302534631143012060355040a130b5468696e6773626f61726431143012060355040b130b5468696e6773626f61726431423040060355040313396e69636b2d5468696e6773626f61726420626f6f74737472617020736572766572204c774d324d207369676e656420627920726f6f742043413059301306072a8648ce3d020106082a8648ce3d03010703420004cf870030ce976dd3d1b034f135ef299fbbb288b0c54af5a5aef08239c635d615577f37fb8282f0ce1706db2bd83bb46eea05584b6db04ce0f08494875153d140a3423040301f0603551d23041830168014330c72547f0c8ae50332260ee1d29e172cdcbde7301d0603551d0e041604143ee7b65fef5f50da8b026b10ab0a4835e9db0aec300c06082a8648ce3d04030205000349003046022100a2c5a3617f9315d10782e3911519b7c9a27b6bbc87c8ca7aad2c5978a88cf8ad022100bd6682c9f87e09d94f498d277d2e8b86b35c4c0b0f3541305ed3f4e8c30d971f';
     * export const LWM2M_SERVER_PUBLIC_KEY_X509 = '3082023f308201e2a003020102020452e452ab300c06082a8648ce3d04030205003076310b3009060355040613025553310b3009060355040813024341310b300906035504071302534631143012060355040a130b5468696e6773626f61726431143012060355040b130b5468696e6773626f6172643121301f060355040313186e69636b2d5468696e6773626f6172642020726f6f7443413020170d3230303632343039313230385a180f32313230303533313039313230385a30818d310b3009060355040613025553310b3009060355040813024341310b300906035504071302534631143012060355040a130b5468696e6773626f61726431143012060355040b130b5468696e6773626f617264313830360603550403132f6e69636b2d5468696e6773626f61726420736572766572204c774d324d207369676e656420627920726f6f742043413059301306072a8648ce3d020106082a8648ce3d0301070342000461cde351cfeca5e4c65957d538982226b6625d2e456f0c7e993d8be8f23d5779441ba34cffe84d34acd4ba67d100861100edd0e77e70c0582324f4ed335c171ca3423040301f0603551d23041830168014330c72547f0c8ae50332260ee1d29e172cdcbde7301d0603551d0e0416041490eff1d5323fcd5620da145a7cfd27eeefb8d34a300c06082a8648ce3d04030205000349003046022100b21c02023ed29382441d2b4fcba2d28dbfad6f7e37349594819acc87dd7600d4022100c053000ef668187f6ab567c3401e8a67206e97a534c0db8400d819151a9c0f2e';
     */
}

