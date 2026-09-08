# Lightsail production deployment

The production workflow uses GitHub OIDC to publish an immutable image to ECR,
then uses SSM Run Command to update `palkia` and restart Nginx. No SSH key or
long-lived AWS credential is stored in GitHub. The ECR login password sent to
the server is a short-lived deployment credential.

Deployment identifiers and the GitHub OIDC role ARN are committed in
`deploy/production.json`; they are not GitHub variables or secrets.

Before enabling production deployment, configure the Lightsail server as an SSM
managed node. The server keeps `/home/ubuntu/backend/.env.production` as the
Docker runtime file. Its source of truth is the dotenv-formatted Secrets Manager
secret `poolc/prod/backend-env`; update the local runtime copy only when a
secret rotates.
