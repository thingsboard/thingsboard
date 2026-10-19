// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.setup.LicenseChangeResult;
import org.thingsboard.server.common.data.setup.LicenseClaimInfo;
import org.thingsboard.server.common.data.setup.LicenseClaimResult;
import org.thingsboard.server.common.data.setup.LicenseClaimStatus;
import org.thingsboard.server.common.data.setup.LicenseKeyRequest;
import org.thingsboard.server.common.data.setup.SetupInfo;
import org.thingsboard.server.common.data.setup.SystemSetupRequest;
import org.thingsboard.server.common.data.setup.SystemSetupState;
import org.thingsboard.server.common.data.subscription.SubscriptionException;
import org.thingsboard.server.common.data.subscription.SubscriptionInfo;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.license.NonProductionConfirmationService;
import org.thingsboard.server.service.setup.SystemSetupService;

/**
 * Core-only, like every other controller: it extends {@link BaseController}, whose dependencies are themselves
 * core-only, so leaving this one ungated aborts the context of a rule-engine node. The setup lock loses nothing
 * by it - a non-core node maps no {@code /api} endpoint for {@code SystemSetupFilter} to block, and it converges
 * on a licence change through the cluster broadcast rather than through this controller.
 */
@RestController
@TbCoreComponent
@RequestMapping("/api")
@RequiredArgsConstructor
public class SystemSetupController extends BaseController {

    private static final String SETUP_STATE_DESCRIPTION = "Reports what the instance still needs before it is " +
            "ready to use: a license, an initial system administrator account, or nothing at all. Answered in " +
            "every state, including while the setup lock is in force.";

    private static final String CLAIM_POLL_DESCRIPTION = "Reports whether an automatic license activation is " +
            "outstanding, and completes it when the portal says the user has activated. Polled by the setup " +
            "wizard. Throttled and idempotent: repeated polls collapse into at most one outbound portal call " +
            "per interval, and the endpoint self-disarms once no claim token is stored - after which it answers " +
            "without ever calling out." +
            "\n\nA status carries what we understand, an error status what we do not, and between them they " +
            "tell the client whether the automatic path is over:" +
            "\n\n- `EXPIRED` - terminal, and the one failure with a known cause. Either the portal refused the " +
            "claim outright and will refuse it again - the token's window has closed, the claim was already " +
            "spent, or the account behind it can no longer resolve a licence for this installation - or the " +
            "`claimToken` this poll named has been superseded by a later claim request. Either way this " +
            "client's claim is dead and no further poll of it can ever succeed. Stop polling and say so " +
            "specifically. A refusal is delivered on exactly one poll, since retiring the token makes every " +
            "later poll answer `NOT_REQUESTED` - which, to a client that only polls after a claim request, " +
            "means the same dead end. A superseded token is answered on every poll instead, since the claim " +
            "that replaced it is alive and its token is left untouched." +
            "\n\n- 429 and 5xx - retryable, and between them they are every error status this endpoint emits. " +
            "Rate limiting, an unreachable portal and a failure to apply an already claimed secret all " +
            "deliberately leave the claim token stored, precisely so that a later poll can still complete the " +
            "activation. Show the message, but keep polling with a backoff instead of abandoning the automatic " +
            "path over one transient failure. There is deliberately no terminal error status here: a refusal the " +
            "portal has actually judged is reported as the `EXPIRED` status above, on a 200, so nothing on this " +
            "endpoint asks the client to read a 4xx as the end of the automatic path." +
            "\n\nThe manual key form stays reachable from every state regardless, since a user may always prefer " +
            "to paste a key rather than wait." +
            "\n\nPass the `claimToken` that POST /api/noauth/setup/claim answered with. Only one claim is " +
            "outstanding per installation, so a second browser session starting the activation replaces the " +
            "first session's token; naming it is what lets this endpoint tell that session its link is dead " +
            "rather than report on a claim it does not hold. Optional: a poll that names no token reports on " +
            "whatever claim is stored, which is the behaviour of every client written before the parameter " +
            "existed. `ACTIVATED` is answered ahead of the check either way - once a licence is in place, " +
            "whichever claim delivered it, every session is told to move on.";

    private final SystemSetupService systemSetupService;
    private final NonProductionConfirmationService nonProductionConfirmationService;

    @ApiOperation(value = "Get setup state (getState)", notes = SETUP_STATE_DESCRIPTION)
    @GetMapping("/noauth/setup/state")
    public SetupInfo getState() {
        return systemSetupService.getSetupInfo();
    }

    /**
     * Deliberately carries no {@code checkSetupState} guard: guarding this one on
     * {@link SystemSetupState#LICENSE_REQUIRED} would make the wizard's own confirming poll fail with a 400
     * immediately after a successful activation moved the state on. On an activated instance it short-circuits
     * before the claim token is read and answers {@link LicenseClaimStatus#ACTIVATED} with no outbound call.
     */
    @ApiOperation(value = "Poll license claim (getClaim)", notes = CLAIM_POLL_DESCRIPTION)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The current claim status."),
            @ApiResponse(responseCode = "429", description = "Rate limited by the license portal. Retryable: keep polling with a backoff."),
            @ApiResponse(responseCode = "500", description = "The license portal was unreachable, or an already claimed secret could not be applied. Retryable: the claim token is kept, so a later poll can still complete the activation.")
    })
    @GetMapping("/noauth/setup/claim")
    public LicenseClaimInfo getClaim(
            @Parameter(description = "The claim token this client's own activation was started with, as " +
                    "returned by POST /api/noauth/setup/claim. Answered EXPIRED once a later claim request " +
                    "has superseded it. Omitted by a client that holds no token, which reports on the stored claim.")
            @RequestParam(required = false) String claimToken) throws ThingsboardException {
        return systemSetupService.pollClaim(claimToken);
    }

    @ApiOperation(value = "Apply license key during setup (applySetupLicense)",
            notes = "Installs the license key pasted into the setup wizard and answers with the resulting setup " +
                    "state, so the caller learns whether the instance is now ready or still needs an initial " +
                    "system administrator account. Accepted only while the license is the missing step.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The key was accepted; the body carries the resulting setup state."),
            @ApiResponse(responseCode = "400", description = "The license is not the step this instance is waiting for, or the key was refused; the message says why.")
    })
    @PostMapping("/noauth/setup/license")
    public SetupInfo applySetupLicense(@RequestBody LicenseKeyRequest request) throws ThingsboardException {
        checkSetupState(SystemSetupState.LICENSE_REQUIRED);
        try {
            systemSetupService.applyLicenseKey(request.getSecret());
        } catch (SubscriptionException e) {
            // This is the screen most keys are pasted into, so a refusal has to arrive as the operator's own
            // mistake and not as a server fault - the same 400 the two admin endpoints answer with.
            throw refusedKey(e);
        }
        return systemSetupService.getSetupInfo();
    }

    @ApiOperation(value = "Request license claim (requestClaim)",
            notes = "Begins the automatic activation: mints a claim token, records it, and answers with the " +
                    "portal sign-up URL the operator opens. Poll GET /api/noauth/setup/claim afterwards until " +
                    "the portal reports the license activated. Accepted only while the license is the missing step.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The claim was requested; the body carries the portal sign-up URL."),
            @ApiResponse(responseCode = "400", description = "The license is not the step this instance is waiting for."),
            @ApiResponse(responseCode = "429", description = "Another claim request for this node was made a moment ago, or one is still in flight: minting is rate-limited. Retryable: wait a moment and request again."),
            @ApiResponse(responseCode = "500", description = "The claim token could not be stored, or the portal sign-up URL could not be built. Retryable.")
    })
    @PostMapping("/noauth/setup/claim")
    public LicenseClaimResult requestClaim() throws ThingsboardException {
        checkSetupState(SystemSetupState.LICENSE_REQUIRED);
        return systemSetupService.requestClaim();
    }

    @ApiOperation(value = "Complete setup (completeSetup)",
            notes = "Creates the initial system administrator account, optionally loading the demo data, and " +
                    "lifts the setup lock. Accepted only while the instance has no system administrator - which " +
                    "is what stops an anonymous caller taking over a provisioned instance.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The account was created and the instance is ready."),
            @ApiResponse(responseCode = "400", description = "The account is not the step this instance is waiting for, or the submitted email or password is not acceptable.")
    })
    @PostMapping("/noauth/setup/complete")
    public void completeSetup(@Valid @RequestBody SystemSetupRequest request) throws ThingsboardException {
        checkSetupState(SystemSetupState.ACCOUNT_REQUIRED);
        systemSetupService.completeSetup(request.getEmail(), request.getPassword(), request.isLoadDemo());
    }

    @ApiOperation(value = "Change license key (changeLicenseKey)",
            notes = "Replaces the license key of the whole deployment and answers with the resulting setup " +
                    "state and the license now in force, so the caller needs no second request. Available to " +
                    "a system administrator in every state, including while the instance is locked.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The key was accepted; the body carries the resulting setup state and license."),
            @ApiResponse(responseCode = "400", description = "The key was refused; the message says why.")
    })
    @PostMapping("/admin/license/key")
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    public LicenseChangeResult changeLicenseKey(@RequestBody LicenseKeyRequest request) throws ThingsboardException {
        LicenseChangeResult result;
        try {
            // Only the apply half can refuse: applyLicenseKeyAndReport swallows a failure to read back the
            // licence, so a key that took effect is never reported to the operator as a refusal.
            result = systemSetupService.applyLicenseKeyAndReport(request.getSecret());
        } catch (SubscriptionException e) {
            throw refusedKey(e);
        }
        if (result.getSubscription() != null) {
            enrichSubscriptionInfo(result.getSubscription());
        }
        return result;
    }

    @ApiOperation(value = "Preview a license key (previewLicenseKey)",
            notes = "Reports what a license key would install, without installing it. Nothing is activated, no " +
                    "instance slot is spent and the deployment keeps the license it has. Answers 400 with an " +
                    "operator-facing message when the key is refused.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The key is valid; the body carries the license it would install."),
            @ApiResponse(responseCode = "400", description = "The key was refused; the message says why.")
    })
    @PostMapping("/admin/license/preview")
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    public SubscriptionInfo previewLicenseKey(@RequestBody LicenseKeyRequest request) throws ThingsboardException {
        try {
            // Deliberately not enriched: used AI credits are this deployment's spend against the licence it is
            // running, not something the previewed key promises. The key's own AI grant is read off its plan
            // data, so the body still describes the key and nothing else.
            return systemSetupService.previewLicenseKey(request.getSecret());
        } catch (SubscriptionException e) {
            throw refusedKey(e);
        }
    }

    /**
     * Turns a refusal into the 400 the key endpoints document. The message is the operator-facing sentence the
     * licence layer produced and is passed through untouched; only the status changes, and only here, so no
     * other endpoint that raises a {@link SubscriptionException} is affected.
     */
    private static ThingsboardException refusedKey(SubscriptionException e) {
        return new ThingsboardException(e.getMessage(), ThingsboardErrorCode.BAD_REQUEST_PARAMS);
    }

    @ApiOperation(value = "Clear license (clearLicense)",
            notes = "Removes the license from the whole deployment and returns it to the unactivated state, so " +
                    "the activation flow can be run again. Answers with the resulting setup state. Available to " +
                    "a system administrator in every state, including while the instance is locked. Unset the " +
                    "TB_LICENSE_SECRET environment variable on every node first: a node that still carries it " +
                    "re-activates from it and stores the key back for the whole cluster, undoing the clear.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The license was cleared; the body carries the resulting setup state.")
    })
    @PostMapping("/admin/license/clear")
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    public SetupInfo clearLicense() {
        log.warn("A system administrator cleared the license of the whole deployment; it is no longer activated.");
        systemSetupService.clearLicense();
        return systemSetupService.getSetupInfo();
    }

    /**
     * The declaration is the system administrator's to make: {@link NonProductionConfirmationService#confirm}
     * is called with the caller's own identity. Its unchecked {@link IllegalStateException} on a missing
     * {@code tb_cluster} row is deliberately left uncaught and becomes a generic 500 - not a leak, since this
     * endpoint is reachable only by an authenticated system administrator.
     */
    @ApiOperation(value = "Confirm non-production use (confirmNonProduction)",
            notes = "Records the system administrator's declaration that this instance is not used in " +
                    "production, which is what a non-production license requires. Available in every state, " +
                    "including while the instance is locked for a lapsed confirmation.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The declaration was recorded.")
    })
    @PostMapping("/admin/nonProduction/confirm")
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    public void confirmNonProduction() throws ThingsboardException {
        nonProductionConfirmationService.confirm(getCurrentUser());
    }

    /**
     * Guards the unauthenticated setup endpoints: every step is accepted only in the state it belongs to. The
     * account step is refused as soon as a sysadmin exists, which is what keeps an anonymous caller from taking
     * over a provisioned instance. The license steps stay open for as long as the license is missing, since a
     * sysadmin may already exist by then: on a CE to PE upgrade the key is entered from the setup UI of an
     * otherwise provisioned instance.
     */
    private void checkSetupState(SystemSetupState requiredState) throws ThingsboardException {
        SystemSetupState state = systemSetupService.getState();
        if (state != requiredState) {
            throw new ThingsboardException("This setup step is not applicable in the " + state + " state",
                    ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
    }

}
