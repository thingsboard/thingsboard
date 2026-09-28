// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.subscription;

import org.thingsboard.license.shared.exception.LicenseErrorCode;
import org.thingsboard.license.shared.exception.LicenseException;

/**
 * Why a licence key was refused, in a sentence the system administrator who pasted it can act on.
 * <p>
 * The licence server already tells the causes apart and the code survives the wire intact, so collapsing them
 * into one message throws away an answer we are handed: a truncated paste is copied again, a spent slot has to
 * be freed in the portal, and a licence past its updates horizon can only be renewed.
 * <p>
 * Context-free on purpose. Three screens ask this question and the server cannot know which; a screen with a
 * sign-up link on it adds its own sentence rather than having one guessed for it here.
 */
public final class LicenseKeyRejections {

    static final String NOT_RECOGNIZED =
            "That key wasn't recognized. Copy it from the ThingsBoard portal again, end to end.";
    static final String BOUND_ELSEWHERE =
            "That key belongs to a different ThingsBoard deployment. A license binds to the deployment that " +
            "activates it - deactivate that instance in the portal to release it, or use a different key.";
    static final String NOT_ACTIVE =
            "That license is no longer active. It was canceled, or its term has ended. Check it in your " +
            "ThingsBoard portal account.";
    static final String NO_AVAILABLE_INSTANCE =
            "That license has no available instances. Deactivate one in the portal, or add instances to the license.";
    static final String VERSION_NOT_COVERED =
            "That license doesn't cover this version of ThingsBoard. Its updates period ended before this " +
            "release. Renew it in the portal, or install a release from within the covered period.";
    static final String ALREADY_OFFLINE =
            "That license has been converted to an offline license. Use the offline license file instead of this key.";
    static final String PORTAL_UNREACHABLE =
            "This server couldn't reach the ThingsBoard portal, so the key couldn't be checked. Check outbound " +
            "access, or use an offline license key.";
    static final String STALE_ACTIVATION =
            "This deployment's activation for that license is no longer valid on the ThingsBoard portal. " +
            "Clear the license here first, then enter the key again to activate afresh.";
    static final String CHECK_NOT_COMPLETED =
            "This server couldn't complete the license check, so the key was neither accepted nor refused. " +
            "Check that its license instance data file is writable, and that nothing between this server and " +
            "the ThingsBoard portal is answering in the portal's place.";
    static final String NO_CLUSTER_IDENTITY =
            "This server hasn't established its cluster identity yet, so a license can't be bound to it. " +
            "Nothing is wrong with the key - wait for the server to finish starting up and try again.";

    private LicenseKeyRejections() {
    }

    public static String messageFor(Throwable error) {
        LicenseErrorCode code = error instanceof LicenseException licenseException
                ? licenseException.getErrorCode() : null;
        if (code == null) {
            return NOT_RECOGNIZED;
        }
        return switch (code) {
            // A wrong key and a truncated one need the same remedy, so they get the same sentence even though
            // the licence server can tell them apart.
            case INVALID_LICENSE_SECRET, SUBSCRIPTION_NOT_FOUND, INVALID_OFFLINE_LICENSE_DATA_CHECK -> NOT_RECOGNIZED;
            case CLUSTER_ID_MISMATCH, INVALID_CLUSTER_ID_CHECK -> BOUND_ELSEWHERE;
            case SUBSCRIPTION_NOT_ACTIVE -> NOT_ACTIVE;
            // Neutral between production and development: the licence server distinguishes them in its own
            // text but returns one code, and this node cannot tell which half of a subscription it holds.
            case ACTIVE_INSTANCES_CAPACITY_EXCEEDED -> NO_AVAILABLE_INSTANCE;
            case UNSUPPORTED_SOFTWARE_VERSION -> VERSION_NOT_COVERED;
            case OFFLINE_LICENSE_ALREADY_ISSUED -> ALREADY_OFFLINE;
            // Not a verdict on the key either. This deployment still holds an activation record for it, and
            // applying the same key re-checks that record rather than activating anew - so "copy it again"
            // would send the operator round a loop that cannot end. Only clearing the licence here breaks it.
            // INVALID_LICENSE_CHECK_REQUEST joins them because the licence server raises it for the instance
            // id this deployment sent: the activation record it was read from is unusable, not the key.
            case INSTANCE_NOT_ACTIVE, INSTANCE_NOT_FOUND, INVALID_LICENSE_CHECK_SECRET,
                 INVALID_LICENSE_CHECK_REQUEST -> STALE_ACTIVATION;
            // GENERAL_SERVER_ERROR is the portal answering without a verdict, which leaves the operator the
            // same thing to check as a portal that could not be reached at all.
            case CONNECTION_ERROR, INVALID_SERVER_CERTIFICATE, GENERAL_SERVER_ERROR -> PORTAL_UNREACHABLE;
            // The check never completed at all: raised for an instance data file this server cannot write,
            // and for any 4xx whose body is not a licence error - a proxy or firewall error page, typically.
            case GENERAL_ERROR -> CHECK_NOT_COMPLETED;
            // The only rejection that is not a verdict on the key at all: this node has no cluster identity to
            // bind one to. Saying "copy it again" would send the operator after a paste that was fine.
            case CLUSTER_ID_NOT_FOUND -> NO_CLUSTER_IDENTITY;
            default -> NOT_RECOGNIZED;
        };
    }

}
