# Lightsail production deployment

The production workflow uses GitHub OIDC to publish an immutable image to ECR,
then uses SSM Run Command to update `palkia` and restart Nginx. No SSH key or
long-lived AWS credential is stored in GitHub.

Required GitHub Actions variables:

- `AWS_DEPLOY_ROLE_ARN`
- `ECR_BACKEND_REPOSITORY`
- `BACKEND_SECRET_ID`
- `SSM_BACKEND_INSTANCE_ID`

Before enabling production deployment, configure the Lightsail server as an SSM
managed node. Its AWS identity needs permission to pull from the ECR repository
and read the named Secrets Manager secret. Store the backend runtime environment
as a dotenv-formatted Secret Manager value; the SSM command writes it to
`/home/ubuntu/backend/.env.production` with restrictive permissions immediately
before restarting Docker Compose.
