# Enterprise CI/CD (api-gateway)

Production-style pipeline aligned with `cicd-pipeline-docs.pdf`.

## Jobs

| Job | When | What |
|-----|------|------|
| **Build and Test** | All branches + PRs | `mvn verify` |
| **Build, scan, sign, push (ECR)** | `staging`, `main`, `v*` tags, manual dispatch | Trivy (non-blocking) → Docker push → **Cosign** → digest output |
| **Deploy to EKS (Helm atomic)** | Same as ECR job | `helm upgrade --install` with `image.digest=@sha256:...`, `--atomic` |
| **Publish to Docker Hub** | `main` + `v*` tags only | **Pull ECR by digest**, retag — no second build |

## Branches

| Branch | CI | ECR + Helm deploy |
|--------|----|-------------------|
| `development` | Yes | No |
| `staging` | Yes | Yes (staging values) |
| `main` | Yes | Yes (production values) |
| PR | Yes | No |

## Repository layout

```text
.github/workflows/ci-cd.yaml
helm/api-gateway/
  Chart.yaml
  values.yaml
  values-staging.yaml
  values-production.yaml
  values-local.yaml
  templates/
```

## GitHub setup

**Environments:** `staging`, `production` — each with secret `AWS_ROLE_ARN`.

**Variables:** `AWS_REGION`, `ECR_REPOSITORY`, `K8S_NAMESPACE`, `EKS_CLUSTER_NAME`.

**Secrets (prod Docker Hub job):** `DOCKER_USERNAME`, `DOCKER_PASSWORD`.

See also [EKS_DEPLOY.md](EKS_DEPLOY.md) for OIDC and cluster access.

## Local deploy (Helm)

```powershell
docker build -t api-gateway:dev .
helm upgrade --install api-gateway ./helm/api-gateway `
  -f helm/api-gateway/values-local.yaml `
  --namespace saloon-system `
  --create-namespace
```

Or: `.\scripts\deploy-k8s.ps1 -Environment dev`

## Manual dispatch

Actions → **CI/CD for API Gateway - Enterprise** → **Run workflow** → choose `staging` or `production`.

## Verify Cosign signature

```bash
cosign verify \
  --certificate-identity-regexp=https://github.com/janindu333 \
  --certificate-oidc-issuer=https://token.actions.githubusercontent.com \
  <account>.dkr.ecr.<region>.amazonaws.com/api-gateway@sha256:...
```
