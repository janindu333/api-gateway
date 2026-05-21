# Deploy api-gateway to AWS EKS (GitHub Actions)

Staging and production run on **EKS**. GitHub Actions builds once per merge, pushes to **ECR**, and rolls out with `kubectl`. Local Docker Desktop stays **manual** (`scripts/deploy-k8s.ps1`).

## Pipeline

```text
development  →  CI only (no EKS deploy)
staging      →  CI → build/push ECR → deploy EKS staging
main         →  CI → build/push ECR → deploy EKS prod (+ optional Docker Hub)
```

## 1. AWS — ECR repository

Create one repo per service (example):

```bash
aws ecr create-repository --repository-name api-gateway --region ap-southeast-1
```

Note the URI: `<account>.dkr.ecr.<region>.amazonaws.com/api-gateway`

## 2. AWS — OIDC trust for GitHub (no long-lived access keys)

Create an IAM OIDC provider for `token.actions.githubusercontent.com` (once per account), then two roles (recommended):

| Role | Used by | Trust `sub` claim (example) |
|------|---------|-----------------------------|
| `github-api-gateway-staging` | `deploy-staging-eks` | `repo:janindu333/api-gateway:environment:staging` |
| `github-api-gateway-production` | `deploy-production-eks` | `repo:janindu333/api-gateway:environment:production` |

Attach policies (tighten per your org):

- `AmazonEC2ContainerRegistryPowerUser` (or scoped ECR push to `api-gateway` only)
- Custom policy allowing `eks:DescribeCluster` on your cluster(s)
- Map the role in **EKS access entries** (or `aws-auth` ConfigMap) with a Kubernetes group that can patch deployments in `saloon-system`

Example trust policy snippet:

```json
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Principal": {
      "Federated": "arn:aws:iam::<ACCOUNT_ID>:oidc-provider/token.actions.githubusercontent.com"
    },
    "Action": "sts:AssumeRoleWithWebIdentity",
    "Condition": {
      "StringEquals": {
        "token.actions.githubusercontent.com:aud": "sts.amazonaws.com"
      },
      "StringLike": {
        "token.actions.githubusercontent.com:sub": "repo:janindu333/api-gateway:environment:staging"
      }
    }
  }]
}
```

Use `environment:production` for the prod role.

## 3. GitHub — Environments

Repo **Settings → Environments**:

| Environment | Secrets | Variables (optional) |
|-------------|---------|----------------------|
| `staging` | `AWS_ROLE_ARN` | `AWS_REGION`, `EKS_CLUSTER_NAME`, `ECR_REPOSITORY` |
| `production` | `AWS_ROLE_ARN` | same (prod cluster name / role) |

Enable **Required reviewers** on `production` for manual approval before deploy.

Repository-level **Variables** (fallback if not set on environment):

| Variable | Example |
|----------|---------|
| `AWS_REGION` | `ap-southeast-1` |
| `ECR_REPOSITORY` | `api-gateway` |
| `EKS_CLUSTER_NAME_STAGING` | `saloon-staging` |
| `EKS_CLUSTER_NAME_PRODUCTION` | `saloon-prod` |
| `K8S_NAMESPACE` | `saloon-system` |

## 4. Cluster prerequisites

Before the first Actions deploy:

1. Namespace and secrets exist (`k8s/namespace.yaml`, cluster secrets — not committed).
2. Apply base manifests once (ingress, service, deployment skeleton):

   ```bash
   kubectl apply -f k8s/namespace.yaml
   kubectl apply -f k8s/api-gateway-service.yaml
   kubectl apply -f k8s/api-gateway-deployment.yaml
   kubectl apply -f k8s/api-gateway-ingress.yaml
   ```

3. Observability (Loki/Tempo/Grafana) if you use the same stack as local — adjust endpoints in `application-k8s.properties` for AWS DNS if needed.

Actions only updates the **image tag** (`github.sha`) and waits for rollout.

## 5. Local vs EKS

| Target | How |
|--------|-----|
| Docker Desktop | `.\scripts\deploy-k8s.ps1 -Environment dev` |
| EKS staging | Merge to `staging` → workflow deploys |
| EKS prod | Merge to `main` → workflow deploys (after approval if configured) |

Workflow file: `.github/workflows/ci-cd.yaml`
