# API Gateway — Git branching & CI/CD

- **Local (Docker Desktop):** GitHub runs **build + test** on `development`; you deploy manually.
- **AWS EKS (staging / prod):** merge to `staging` or `main` triggers **ECR push + EKS rollout** — see [EKS_DEPLOY.md](EKS_DEPLOY.md).

## Branch flow (GitFlow)

```text
feature/*  --PR-->  development  --PR-->  staging  --PR-->  main
                         |                  |              |
                    CI only            CI only         CI + Docker Hub
                    (GitHub)           (GitHub)        (GitHub)
                         |                  |              |
                    you deploy         you deploy     pull image /
                    locally            locally        deploy to prod cluster
```

| Step | GitHub Actions | Local (your PC) |
|------|----------------|-----------------|
| Work on `feature/*` | — | Run/test app as needed |
| Open PR → `development` | **Build and Test** | — |
| Merge to `development` | **Build and Test** | Optional: `.\scripts\deploy-k8s.ps1 -Environment dev` |
| PR → `staging` | **Build and Test** | — |
| Merge to `staging` | **Build and Test** + **ECR** + **EKS deploy** | Optional: local `deploy-k8s.ps1 -Environment staging` |
| PR → `main` | **Build and Test** | — |
| Merge to `main` | **Build and Test** + **ECR** + **EKS prod** + optional **Docker Hub** | Optional: local prod pull/deploy |

## Why no auto-deploy to your laptop

GitHub-hosted runners cannot reach Docker Desktop on your PC. **Staging/prod** use **EKS** via OIDC (`.github/EKS_DEPLOY.md`). Local deploy stays `scripts/deploy-k8s.ps1`.

## Secrets (Docker Hub on `main` only)

Repo **Settings → Secrets and variables → Actions**:

| Secret | Used for |
|--------|----------|
| `DOCKER_USERNAME` | Push image on merge to `main` |
| `DOCKER_PASSWORD` | Docker Hub access token |

## Manual deploy to local Kubernetes

From repo root (after `git pull` on the branch you want to run):

```powershell
.\scripts\deploy-k8s.ps1 -Environment dev
.\scripts\deploy-k8s.ps1 -Environment staging
.\scripts\deploy-k8s.ps1 -Environment prod -PullFromDockerHub
```

## PR targets

Open feature PRs into **`development`**, not `main`.

Workflow: `.github/workflows/ci-cd.yaml`
