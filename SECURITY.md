# Security Policy


## Supported Versions

TariffSheriff uses semantic versioning for releases. We recommend the following support policy: support the latest minor series and the previous minor series for security fixes.

| Version | Supported |
| ------- | --------- |
| v1.0.x | ✅ Yes (current stable) |
| v0.9.x | ✅ Yes (security fixes only) |
| v0.8.x | ⚠️ Limited (critical security fixes only) |
| < v0.8 | ❌ No (unsupported) |

**Note**: If your installation is running an unsupported version, we strongly recommend upgrading to the latest v1.0.x release for the best security posture and latest features.

---

## Reporting a Vulnerability

Please report suspected vulnerabilities privately using one of these methods:

1. **GitHub Security Advisories** (Recommended): Navigate to the Security tab in our repository and click "Report a vulnerability"
2. **Email**: security@tariffsheriff.org
3. **Direct Contact**: Contact maintainers via GitHub (@SaaiAravindhRaja)

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

## Security Best Practices

When deploying TariffSheriff:

1. **Environment Variables**: Never commit sensitive credentials to version control
   - Use `.env` files locally (already in `.gitignore`)
   - Use secure secret management in production (AWS Secrets Manager, Vault, etc.)

2. **Database Security**:
   - Use SSL/TLS for all database connections (`sslmode=require`)
   - Rotate database passwords regularly
   - Use least-privilege database users

3. **API Security**:
   - Rotate JWT secrets regularly
   - Use strong, randomly generated JWT secrets (minimum 256 bits)

4. **Dependencies**:
   - Regularly update dependencies to patch known vulnerabilities
   - Monitor GitHub Dependabot alerts
   - Run `npm audit` and `mvn dependency-check` regularly

5. **Network Security**:
   - Use HTTPS for all production deployments
   - Configure CORS appropriately for your domain
   - Enable security headers (CSP, HSTS, X-Frame-Options)

---

## Contact

**Security Email**: security@tariffsheriff.org
**GitHub Security Advisories**: https://github.com/SaaiAravindhRaja/TariffSheriff/security/advisories

Thank you for helping keep TariffSheriff secure.
