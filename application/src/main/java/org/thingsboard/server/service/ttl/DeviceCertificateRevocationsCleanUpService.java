// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ttl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.server.dao.device.DeviceCertificateRevocationService;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.concurrent.TimeUnit;

/**
 * Removes revocations of certificates that expired more than the TTL ago: every transport rejects an expired certificate,
 * so such a revocation no longer blocks anything.
 * <p>
 * That holds only while transports check the validity period of client certificates. A transport configured to skip the
 * check accepts expired certificates, and removing their revocations would make them usable again. Skip flags of transports
 * running in this process are detected and stop the cleanup; standalone transport services must be accounted for by
 * disabling this TTL.
 */
@Slf4j
@Service
@TbCoreComponent
@ConditionalOnExpression("${sql.ttl.device_certificate_revocations.enabled:true} && ${sql.ttl.device_certificate_revocations.ttl:0} > 0")
public class DeviceCertificateRevocationsCleanUpService extends AbstractCleanUpService {

    public static final String RANDOM_DELAY_INTERVAL_MS_EXPRESSION =
            "#{T(org.apache.commons.lang3.RandomUtils).nextLong(0, ${sql.ttl.device_certificate_revocations.checking_interval_ms})}";

    private final DeviceCertificateRevocationService revocationService;

    @Value("${sql.ttl.device_certificate_revocations.ttl:0}")
    private long ttlInSec;

    @Value("${transport.mqtt.ssl.skip_validity_check_for_client_cert:false}")
    private boolean mqttSkipValidityCheck;
    @Value("${coap.dtls.x509.skip_validity_check_for_client_cert:false}")
    private boolean coapSkipValidityCheck;
    @Value("${transport.lwm2m.server.security.skip_validity_check_for_client_cert:false}")
    private boolean lwm2mSkipValidityCheck;

    public DeviceCertificateRevocationsCleanUpService(PartitionService partitionService, DeviceCertificateRevocationService revocationService) {
        super(partitionService);
        this.revocationService = revocationService;
    }

    @Scheduled(
            initialDelayString = RANDOM_DELAY_INTERVAL_MS_EXPRESSION,
            fixedDelayString = "${sql.ttl.device_certificate_revocations.checking_interval_ms:86400000}"
    )
    public void cleanUp() {
        if (!isSystemTenantPartitionMine()) {
            return;
        }
        if (mqttSkipValidityCheck || coapSkipValidityCheck || lwm2mSkipValidityCheck) {
            log.warn("Revoked device certificates are not removed: a transport skips the validity check of client certificates, " +
                    "so an expired certificate whose revocation is removed would be accepted again. Disable 'sql.ttl.device_certificate_revocations'.");
            return;
        }
        long threshold = System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(ttlInSec);
        int deleted = revocationService.deleteRevocationsExpiredBefore(threshold);
        if (deleted > 0) {
            log.info("Revoked device certificates cleanup removed {} revocations of certificates expired before {}", deleted, threshold);
        }
    }

}
