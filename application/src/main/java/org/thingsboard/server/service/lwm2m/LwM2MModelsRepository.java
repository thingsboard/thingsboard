/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
import org.eclipse.leshan.core.util.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.lwm2m.*;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.transport.lwm2m.LwM2MTransportConfigBootstrap;
import org.thingsboard.server.common.transport.lwm2m.LwM2MTransportConfigServer;
import org.thingsboard.server.dao.service.Validator;
import org.thingsboard.server.transport.lwm2m.secure.LwM2MSecurityMode;

import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.ECPoint;
import java.security.spec.KeySpec;
import java.util.List;
import java.util.ArrayList;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.thingsboard.server.dao.service.Validator.validateId;

@Slf4j
@Service
@ConditionalOnExpression("('${service.type:null}'=='tb-transport' && '${transport.lwm2m.enabled:false}'=='true') || '${service.type:null}'=='monolith' || '${service.type:null}'=='tb-core'")
public class LwM2MModelsRepository {

    private static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";

    @Autowired
    LwM2MTransportConfigServer contextServer;


    @Autowired
    LwM2MTransportConfigBootstrap contextBootStrap;

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
        List<ObjectModel> listObjects = (predicate == null) ? this.contextServer.getModelsValue() :
                contextServer.getModelsValue().stream()
                        .filter(predicate)
                        .collect(Collectors.toList());
        listObjects.forEach(obj -> {
            LwM2mObject lwM2mObject = new LwM2mObject();
            lwM2mObject.setId(obj.id);
            lwM2mObject.setName(obj.name);
            lwM2mObject.setMultiple(obj.multiple);
            lwM2mObject.setMandatory(obj.mandatory);
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

    /**
     *
     * @param securityMode
     * @param bootstrapServerIs
     * @return ServerSecurityConfig more value is default: Important - port, host, publicKey
     */
    public ServerSecurityConfig getBootstrapSecurityInfo(String securityMode, boolean bootstrapServerIs) {
        LwM2MSecurityMode lwM2MSecurityMode = LwM2MSecurityMode.fromSecurityMode(securityMode.toLowerCase());
        return getBootstrapServer(bootstrapServerIs, lwM2MSecurityMode);
    }

    /**
     *
     * @param bootstrapServerIs
     * @param mode
     * @return ServerSecurityConfig more value is default: Important - port, host, publicKey
     */
    private ServerSecurityConfig getBootstrapServer(boolean bootstrapServerIs, LwM2MSecurityMode mode) {
        ServerSecurityConfig bsServ = new ServerSecurityConfig();
        if (bootstrapServerIs) {
            switch (mode) {
                case NO_SEC:
                    bsServ.setHost(contextBootStrap.getBootstrapHost());
                    bsServ.setPort(contextBootStrap.getBootstrapPort());
                    bsServ.setServerPublicKey("");
                    break;
                case PSK:
                    bsServ.setHost(contextBootStrap.getBootstrapSecureHost());
                    bsServ.setPort(contextBootStrap.getBootstrapSecurePort());
                    bsServ.setServerPublicKey("");
                    break;
                case RPK:
                    bsServ.setHost(contextBootStrap.getBootstrapSecureHost());
                    bsServ.setPort(contextBootStrap.getBootstrapSecurePort());
                    bsServ.setServerPublicKey(getRPKPublicKey(this.contextBootStrap.getBootstrapPublicX(), this.contextBootStrap.getBootstrapPublicY()));
                    break;
                case X509:
                    bsServ.setHost(contextBootStrap.getBootstrapSecureHost());
                    bsServ.setPort(contextBootStrap.getBootstrapSecurePortCert());
                    bsServ.setServerPublicKey(getServerPublicKeyX509(contextBootStrap.getBootstrapAlias()));
                    break;
                default:
                    break;
            }
        } else {
            bsServ.setBootstrapServerIs(bootstrapServerIs);
            bsServ.setServerId(123);
            switch (mode) {
                case NO_SEC:
                    bsServ.setHost(contextServer.getServerHost());
                    bsServ.setPort(contextServer.getServerPort());
                    bsServ.setServerPublicKey("");
                    break;
                case PSK:
                    bsServ.setHost(contextServer.getServerSecureHost());
                    bsServ.setPort(contextServer.getServerSecurePort());
                    bsServ.setServerPublicKey("");
                    break;
                case RPK:
                    bsServ.setHost(contextServer.getServerSecureHost());
                    bsServ.setPort(contextServer.getServerSecurePort());
                    bsServ.setServerPublicKey(getRPKPublicKey(this.contextServer.getServerPublicX(), this.contextServer.getServerPublicY()));
                    break;
                case X509:
                    bsServ.setHost(contextServer.getServerSecureHost());
                    bsServ.setPort(contextServer.getServerSecurePortCert());
                    bsServ.setServerPublicKey(getServerPublicKeyX509(contextServer.getServerAlias()));
                    break;
                default:
                    break;
            }
        }
        return bsServ;
    }

    /**
     *
     * @param alias
     * @return PublicKey format HexString or null
     */
    private String getServerPublicKeyX509 (String alias) {
        try {
            X509Certificate  serverCertificate = (X509Certificate) contextServer.getKeyStoreValue().getCertificate(alias);
            return  Hex.encodeHexString(serverCertificate.getEncoded());
        } catch (CertificateEncodingException | KeyStoreException e) {
            e.printStackTrace();
        }
        return  null;
    }

    /**
     *
     * @param publicServerX
     * @param publicServerY
     * @return  PublicKey format HexString or null
     */
    private String getRPKPublicKey(String publicServerX, String publicServerY) {
        try {
            /** Get Elliptic Curve Parameter spec for secp256r1 */
            AlgorithmParameters algoParameters = AlgorithmParameters.getInstance("EC");
            algoParameters.init(new ECGenParameterSpec("secp256r1"));
            ECParameterSpec parameterSpec = algoParameters.getParameterSpec(ECParameterSpec.class);
            if (publicServerX != null && !publicServerX.isEmpty() && publicServerY != null && !publicServerY.isEmpty()) {
                /** Get point values */
                byte[] publicX = Hex.decodeHex(publicServerX.toCharArray());
                byte[] publicY = Hex.decodeHex(publicServerY.toCharArray());
                /** Create key specs */
                KeySpec publicKeySpec = new ECPublicKeySpec(new ECPoint(new BigInteger(publicX), new BigInteger(publicY)),
                        parameterSpec);
                /** Get keys */
                PublicKey  publicKey = KeyFactory.getInstance("EC").generatePublic(publicKeySpec);
                if (publicKey != null && publicKey.getEncoded().length > 0 ) {
                   return Hex.encodeHexString(publicKey.getEncoded());
                }
            }
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            log.error("[{}] Failed generate Server RPK for profile", e.getMessage());
            throw new RuntimeException(e);
        }
        return null;
    }
}

