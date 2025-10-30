# Security Policy


## Supported Versions

TariffSheriff uses semantic versioning for releases. Current Git tags in this repository (as of this update) are:

- `v0.2.0` (latest)
- `v0.1.0`

We recommend the following support policy (common practice): support the latest minor series and the previous minor series for security fixes.

| Version | Supported |
| ------- | --------- |
| v0.2.x | Yes (current stable) |
| v0.1.x | Yes (security fixes only) |
| < v0.1 | No (unsupported) |

If your installation is running an unsupported version, we recommend upgrading to the nearest supported release (for example, upgrade from `v0.1.x` to `v0.2.x`).

---

## Reporting a Vulnerability

Please report suspected vulnerabilities privately to the security team at: security@yourdomain.example (replace with an actual address / GitHub Security Advisory via the repository settings).

When reporting, include the following information (as applicable):

- A short summary of the issue and potential impact
- Affected versions or components (e.g. `apps/frontend`, `apps/backend`, `packages/shared-utils`)
- Proof-of-concept (PoC) or reproduction steps (commands, minimal code, or sample payloads)
- Any mitigations or temporary workarounds you used
- Your contact information for follow-up (email or GitHub handle)

We will acknowledge receipt within 72 hours and provide an initial triage within 7 business days.

If you prefer to use GitHub's security advisory workflow, you can open a draft security advisory through the repository's Security tab — this allows us to coordinate privately and publish advisories and CVEs when appropriate.

---

## Triage and Response

Our typical response timeline is as follows:

- Acknowledgement: within 72 hours
- Initial Triage: within 7 business days
- Fix or mitigation plan: typically within 30 days depending on severity and complexity
- Public advisory / CVE assignment: coordinated after a fix is available, where applicable

Severities are handled in line with industry best practices (CVSS scoring, exploitability, impact on confidentiality/integrity/availability).

---

## Coordination and Disclosure

We prefer coordinated disclosure. Once a valid issue is confirmed:

1. We'll create an internal issue and assign an engineer to handle the fix.
2. We'll work with you to confirm a timeline for an upstream fix and public disclosure.
3. For critical vulnerabilities that affect many users, we may issue an advisory and request immediate upgrades.

If you reported the vulnerability via GitHub's Security Advisory flow, we will update the advisory as we progress.

---

## CVE requests

If the issue warrants a CVE, we will request a CVE ID on your behalf (or you may request one via MITRE/your CNA). Please indicate if you require a CVE in your initial report.

---

## PGP / Encryption

If you need to send sensitive PoC details via email, please encrypt them using our PGP key. (Add PGP key or alternatively ask to use GitHub Security Advisories for private disclosure.)

PGP key fingerprint: <replace-with-key-or-instructions>

---

## Contact

security@yourdomain.example (replace with the actual security contact email)

Thank you for helping keep TariffSheriff secure.
