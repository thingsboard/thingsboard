# Restricted tenant profiles: email abuse controls

Tenants on a *restricted* tenant profile may no longer use the system mail relay to reach arbitrary
addresses, and their users must verify their own email changes with a one-time code. The feature is
driven entirely by one setting, `security.restricted_tenant_profiles`.

## Required configuration

- `security.restricted_tenant_profiles` (env `SECURITY_RESTRICTED_TENANT_PROFILES`) is a
  comma-separated list of tenant profile **names**. It is matched exactly and case-sensitively
  against the profile name, so `Free` does not match a profile named `free`.
- **It must be set identically on every node of the cluster.** Enforcement happens on whichever node
  serves the request, so nodes that disagree produce inconsistent behaviour: the same operation
  succeeds or is refused depending on which node handled it.
- `thingsboard.yml` ships with the value `Free`, but the fallback baked into the code is **empty**.
  An upgraded node that keeps its existing `thingsboard.yml` therefore gets **no enforcement at all**.
  Deployments that want enforcement must set the value explicitly (in their own `thingsboard.yml` or
  via the environment variable) rather than relying on the shipped file.
- Volume enforcement is a separate setting: `mail.per_tenant_rate_limits`
  (env `MAIL_PER_TENANT_RATE_LIMITS`) ships **empty**, which means unlimited. Until an operator sets
  it, a restricted tenant's user-invite and password-reset mail is unmetered.
  Anyone who sets it should know that the unauthenticated `POST /api/noauth/resetPasswordByEmail`
  endpoint shares the same per-tenant bucket, so a third party who knows a single address of a
  tenant can drain that tenant's mail allowance.
  Once the limit is set, a request that trips it is answered with `429 TOO_MANY_REQUESTS` rather than
  a generic `500`. On `POST /api/user` the newly created user is still rolled back, so a retry after
  the limit refills starts from a clean state.
- Email-change verification is tuned by `security.email_verification.code_lifetime_seconds`,
  `security.email_verification.max_verification_failures` and
  `security.email_verification.min_resend_period_seconds`. The `cache.specs.emailVerificationCodes`
  TTL must stay above `code_lifetime_seconds`, otherwise an expired code is reported as a missing
  pending change instead of an expired code.

## Behaviour changes for tenants on a restricted profile

- System-relayed mail may only be addressed to **activated users of that tenant** — in `to`, `cc` and
  `bcc` alike. Tenants sending through their own SMTP server are exempt from the recipient policy,
  but are still subject to the volume limit.
- The display-name recipient form (`Ops Team <a@b.c>`) is no longer accepted. Use the bare address.
- Notification-centre email addressed to an invited-but-not-yet-activated user now fails with
  `PERMISSION_DENIED` instead of being sent.
- Activation links are no longer readable through the API (`GET /api/user/{id}/activationLink` and
  `/activationLinkInfo` return `PERMISSION_DENIED`), and `userCredentialsEnabled` cannot be turned on
  for a user who has never activated. Re-enabling an **already activated** user still works, so
  lockout recovery is unaffected.
- Creating a user always sends the activation mail, regardless of the `sendActivationMail` request
  parameter — the emailed link is the only way to invite someone.
- A tenant administrator can no longer change a user's email address through `POST /api/user`.
  Note this also blocks a SYSADMIN from correcting a typo'd address on a restricted tenant through
  the validating save path: support must move the tenant off the restricted profile, or delete and
  recreate the user.

## New behaviour for all users

- Changing your own email address goes through `POST /api/user/email`. On a restricted tenant this
  now mails a one-time code to the **new** address, and the change is applied only once that code is
  confirmed via `POST /api/user/email/verify`. On an unrestricted tenant the change still applies
  immediately.
- Completing an email change signs the user out of all sessions; they sign back in with the new
  address.
- There is currently **no audit-log entry** for a self-service email change, and **no notice sent to
  the old address**. Both are tracked as follow-ups.
