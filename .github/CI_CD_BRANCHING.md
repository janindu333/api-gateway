# API Gateway — Git branching & CI/CD

Industry-style pipeline for a **local** dev setup: GitHub runs **build + test only**; you deploy to Docker Desktop K8s **manually** on your machine.

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
| Merge to `staging` | **Build and Test** | Optional: `.\scripts\deploy-k8s.ps1 -Environment staging` |
| PR → `main` | **Build and Test** | — |
| Merge to `main` | **Build and Test** + **Docker Hub push** | Optional: deploy prod image locally or to cloud |

## Why no auto-deploy from Actions to your laptop

Shared **DEV/STAGING** in industry is usually a **remote cluster** (EKS, AKS, etc.), not each developer’s Docker Desktop. GitHub-hosted runners cannot reach your local Kubernetes, so auto-deploy to a personal machine is avoided here.

When you add a **shared cloud DEV** cluster later, add a deploy job that uses `kubectl`/Helm with cluster credentials in GitHub Secrets—not a self-hosted runner on your laptop.

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
