package org.thingsboard.server.transport.coap.x509;

import org.eclipse.californium.elements.auth.RawPublicKeyIdentity;
import org.eclipse.californium.elements.util.DaemonThreadFactory;
import org.eclipse.californium.elements.util.ExecutorsUtil;
import org.eclipse.californium.elements.util.NamedThreadFactory;
import org.eclipse.californium.scandium.dtls.CertificateMessage;
import org.eclipse.californium.scandium.dtls.CertificateType;
import org.eclipse.californium.scandium.dtls.CertificateVerificationResult;
import org.eclipse.californium.scandium.dtls.ConnectionId;
import org.eclipse.californium.scandium.dtls.HandshakeResultHandler;
import org.eclipse.californium.scandium.dtls.x509.AsyncNewAdvancedCertificateVerifier;
import org.eclipse.californium.scandium.dtls.x509.AsyncNewAdvancedCertificateVerifier.Builder;
import org.eclipse.californium.scandium.dtls.x509.StaticNewAdvancedCertificateVerifier;
import org.eclipse.californium.scandium.util.ServerNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.security.PublicKey;
import java.security.cert.CertPath;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TbAsyncAdvancedCertificateVerifier  extends StaticNewAdvancedCertificateVerifier{
    /**
     * @since 3.10
     */
    private static final Logger LOG = LoggerFactory.getLogger(AsyncNewAdvancedCertificateVerifier.class);

    /**
     * Thread factory.
     */
    private static final NamedThreadFactory THREAD_FACTORY = new DaemonThreadFactory("AsyncCertVerifier#", NamedThreadFactory.SCANDIUM_THREAD_GROUP);
    /**
     * Executor for asynchronous behaviour.
     */
    private final ScheduledExecutorService executorService;
    /**
     * Delay for certificate result. {@code 0} or negative delays for test with
     * synchronous blocking behaviour. Positive delays for test with
     * asynchronous none-blocking behaviour.
     */
    private volatile int delayMillis = 1;
    /**
     * Result handler set during initialization.
     *
     * @see #setResultHandler(HandshakeResultHandler)
     */
    private HandshakeResultHandler resultHandler;

    public TbAsyncAdvancedCertificateVerifier(X509Certificate[] trustedCertificates,
                                               RawPublicKeyIdentity[] trustedRPKs, List<CertificateType> supportedCertificateTypes) {
        super(trustedCertificates, trustedRPKs, supportedCertificateTypes);
        executorService = ExecutorsUtil.newSingleThreadScheduledExecutor(THREAD_FACTORY); // $NON-NLS-1$
    }

    /**
     * Set delay.
     *
     * @param delayMillis delay in milliseconds to report result. {@code 0} or
     *            negative delays using synchronous blocking behaviour. Positive
     *            delays using asynchronous none-blocking behaviour.
     * @return this certificate verifier for command chaining
     */
    public TbAsyncAdvancedCertificateVerifier setDelay(int delayMillis) {
        this.delayMillis = delayMillis;
        if (delayMillis > 0) {
            LOG.info("Asynchronous delayed certificate verifier {}ms.", delayMillis);
        } else if (delayMillis < 0) {
            LOG.info("Synchronous delayed certificate verifier {}ms.", -delayMillis);
        } else {
            LOG.info("Synchronous certificate verifier.");
        }
        return this;
    }

    /**
     * Get delay milliseconds.
     *
     * @return delay milliseconds. {@code 0} or negative delays using
     *         synchronous blocking behaviour. Positive delays using
     *         asynchronous none-blocking behaviour.
     */
    public int getDelay() {
        return delayMillis;
    }

    /**
     * Shutdown. Cleanup resources.
     */
    public void shutdown() {
        executorService.shutdown();
    }

    @Override
    public CertificateVerificationResult verifyCertificate(final ConnectionId cid, final ServerNames serverName,
                                                           final InetSocketAddress remotePeer, final boolean clientUsage, final boolean verifySubject,
                                                           final boolean truncateCertificatePath, final CertificateMessage message) {
        if (delayMillis <= 0) {
            if (delayMillis < 0) {
                try {
                    Thread.sleep(-delayMillis);
                } catch (InterruptedException e) {
                }
            }
            return super.verifyCertificate(cid, serverName, remotePeer, clientUsage, verifySubject,
                    truncateCertificatePath, message);
        } else {
            executorService.schedule(new Runnable() {

                @Override
                public void run() {
                    verifyCertificateAsynchronous(cid, serverName, remotePeer, clientUsage, verifySubject,
                            truncateCertificatePath, message);
                }
            }, delayMillis, TimeUnit.MILLISECONDS);
            return null;
        }
    }

    private void verifyCertificateAsynchronous(ConnectionId cid, ServerNames serverName, InetSocketAddress remotePeer,
                                               boolean clientUsage, boolean verifySubject, boolean truncateCertificatePath,
                                               CertificateMessage message) {
        CertificateVerificationResult result = super.verifyCertificate(cid, serverName, remotePeer, clientUsage,
                verifySubject, truncateCertificatePath, message);
        CertPath certPath = result.getCertificatePath();
        PublicKey publicKey = result.getPublicKey();
        if (certPath == null && publicKey == null) {
            LOG.info("Validation failed!");
        } else if (certPath != null) {
            LOG.info("Validation {}", certPath.getCertificates().size());
        } else if (publicKey != null) {
            LOG.info("Validation RPK");
        }
        resultHandler.apply(result);
    }

    @Override
    public void setResultHandler(HandshakeResultHandler resultHandler) {
        if (this.resultHandler != null && resultHandler != null && this.resultHandler != resultHandler) {
            throw new IllegalStateException("handshake result handler already set!");
        }
        this.resultHandler = resultHandler;
    }

    public static AsyncNewAdvancedCertificateVerifier.Builder builder() {
        return new AsyncNewAdvancedCertificateVerifier.Builder();
    }

    public static class Builder extends StaticNewAdvancedCertificateVerifier.Builder {

        public AsyncNewAdvancedCertificateVerifier build() {
            return new AsyncNewAdvancedCertificateVerifier(trustedCertificates, trustedRPKs, supportedCertificateTypes);
        }
    }

}
