# API Gateway — Git branching & CI/CD

## Branch flow (GitFlow)

```text
feature/*  --PR-->  development  --PR-->  staging  --PR-->  main
                         |                  |              |
                    auto deploy         auto deploy    Docker Hub +
                    Dev K8s             Staging K8s    optional prod K8s
                    (self-hosted)       (self-hosted)  (self-hosted)
```

| Step | What happens |
|------|----------------|
| Work on `feature/*` | Local dev only |
| Open PR → `development` | **CI** runs (build + `mvn verify`) on GitHub |
| Merge to `development` | **CI** + **Deploy to Dev** (self-hosted → Docker Desktop K8s) |
| PR `development` → `staging` | **CI** only (manual PR by lead) |
| Merge to `staging` | **CI** + **Deploy to Staging** (self-hosted K8s) |
| PR `staging` → `main` | **CI** only (manual PR) |
| Merge to `main` | **CI** + push `janindu3/api-gateway` to **Docker Hub** + optional prod K8s deploy |

## One-time setup: self-hosted runner (required for Dev/Staging auto-deploy)

GitHub’s cloud runners **cannot** reach Kubernetes on your PC. Dev/Staging deploy jobs use `runs-on: self-hosted`.

1. On the machine that runs **Docker Desktop** with Kubernetes enabled:
   - Repo → **Settings** → **Actions** → **Runners** → **New self-hosted runner**
   - Follow GitHub’s Windows steps (download, configure, run as a service).

2. Labels: default is fine; jobs use `self-hosted`.

3. Ensure `kubectl` and `docker` work in PowerShell on that machine.

## Secrets (Production / Docker Hub)

In repo **Settings → Secrets and variables → Actions**:

| Secret | Used for |
|--------|----------|
| `DOCKER_USERNAME` | Push image on merge to `main` |
| `DOCKER_PASSWORD` | Docker Hub token |

## Manual deploy (without waiting for CI)

From `api-gateway` repo root:

```powershell
./scripts/deploy-k8s.ps1 -Environment dev
./scripts/deploy-k8s.ps1 -Environment staging
./scripts/deploy-k8s.ps1 -Environment prod -PullFromDockerHub
```

## PR targets

Open feature PRs into **`development`**, not `main`, to match this pipeline.

Workflow file: `.github/workflows/ci-cd.yaml`
