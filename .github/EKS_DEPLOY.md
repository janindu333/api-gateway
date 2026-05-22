# Deploy api-gateway to AWS EKS (Enterprise Helm)

GitHub Actions deploys with **Helm** (`helm/api-gateway/`) using an **immutable ECR digest** (`image@sha256:...`).

## Pipeline

```text
development  →  CI only
staging      →  CI → Trivy → ECR → Cosign → Helm (values-staging.yaml)
main         →  CI → ECR → Cosign → Helm (values-production.yaml) → Docker Hub retag
```

Full details: [ENTERPRISE_CICD.md](ENTERPRISE_CICD.md)

## GitHub Environments

| Environment | Secret | Variables |
|-------------|--------|-----------|
| `staging` | `AWS_ROLE_ARN` | `AWS_REGION`, `EKS_CLUSTER_NAME`, `ECR_REPOSITORY`, `K8S_NAMESPACE` |
| `production` | `AWS_ROLE_ARN` | same (prod cluster name) |

Optional for Docker Hub retag on `main`: `DOCKER_USERNAME`, `DOCKER_PASSWORD`.

## One-time cluster setup

```bash
kubectl apply -f k8s/namespace.yaml
# Ensure saloon-secrets exists in saloon-system (not committed)
```

Helm creates/updates the api-gateway release; no need to apply `k8s/api-gateway-deployment.yaml` on EKS when using this pipeline.

## Local

```powershell
.\scripts\deploy-k8s.ps1 -Environment dev
```

Legacy kubectl manifests: `.\scripts\deploy-k8s.ps1 -Environment dev -UseKubectlManifests`
