# Lightsail production deployment

Before the first production deployment, create `/home/ubuntu/backend/.env.production`
on Lightsail from `.env.production.example` and set its permissions to `600`.

The production workflow publishes an immutable GHCR image for each master commit,
then updates the `palkia` container and restarts Nginx on Lightsail.

Required GitHub secrets:

- `LIGHTSAIL_HOST`
- `LIGHTSAIL_USER`
- `LIGHTSAIL_SSH_KEY`
