# Dia-Smart: Secret Rotation Checklist

> [!WARNING]
> **Manual Action Required by Project Owner**
> During previous commits, sensitive configuration values (database credential, JWT signing secret, encryption key) were committed directly to repository configuration and documentation.
> Although configuration files have now been externalized to require environment variables without default values in production, the previously committed credentials must be considered compromised and manually rotated in all upstream systems.
>
> **Do not add or reproduce any secret values in this document or in the repository.**

---

## Required Manual Rotation Steps

The project owner or security administrator must execute the following procedures:

- [ ] **1. Rotate the Exposed Database Credential**
  - Connect to the production database instance (e.g., AWS RDS PostgreSQL) via an administrative console.
  - Update the database user password to a newly generated, cryptographically random password meeting security policies.
  - Update database connection parameters in secret managers (e.g., AWS Secrets Manager / Parameter Store).
  - Verify that the old password can no longer authenticate against the database.

- [ ] **2. Rotate the Exposed JWT Signing Secret**
  - Generate a new, high-entropy secret (at least 256 bits / 32 characters long) for HMAC-SHA256 signing.
  - Configure the new secret in the deployment environment (`JWT_SECRET`).
  - Deploy the updated configuration.

- [ ] **3. Rotate Any Exposed Encryption Key**
  - Generate a new 32-character AES/encryption key for sensitive payload encryption (`ENCRYPTION_KEY`).
  - Re-encrypt existing encrypted columns or data records using the new key where applicable.
  - Update the deployment environment variable with the new key.

- [ ] **4. Update Deployment Environment Variables**
  - Verify that all production deployment environments (e.g., AWS ECS, EC2, Lambda, Docker runtime, CI/CD runners) supply:
    - `DB_PASSWORD`
    - `DB_URL` (if overriding)
    - `DB_USERNAME`
    - `JWT_SECRET`
    - `ENCRYPTION_KEY`
    - `AI_INTERNAL_SERVICE_TOKEN`
  - Confirm the application starts correctly when all required variables are set and fails fast if any required variable is omitted.

- [ ] **5. Invalidate Active Tokens Signed with Old JWT Secret**
  - Existing user sessions signed with the previous JWT secret will fail signature verification once the new secret is active.
  - Notify active users if necessary, or force a re-login flow for all active sessions.
  - If a distributed token blacklist or session cache is in use, clear expired entries.

- [ ] **6. Check Logs and Deployment Systems for Copied Values**
  - Review centralized logging solutions (AWS CloudWatch, log aggregators) to ensure previously exposed credentials were not output in cleartext.
  - Inspect CI/CD build logs, configuration backup snapshots, and terminal histories for copied values.

- [ ] **7. Plan Git-History Cleanup Separately**
  - Note: Changing files in the current commit does not purge secrets from historical Git commits.
  - Coordinate with all contributors to plan an intentional, coordinated history rewrite (using tools such as `git-filter-repo` or BFG Repo-Cleaner) followed by force-pushes and fresh repository clones.
  - Ensure all active branches and pull requests are rebased cleanly after the history purge.
