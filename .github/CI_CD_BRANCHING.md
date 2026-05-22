# API Gateway — Git branching & CI/CD (Enterprise)

Branch: **`feature/enterprise-cicd-helm`** introduces Helm + full enterprise workflow. See [ENTERPRISE_CICD.md](ENTERPRISE_CICD.md).

## Branch flow

```text
feature/*  --PR-->  development  --PR-->  staging  --PR-->  main
                         |                  |              |
                    CI only            CI+ECR+Helm    CI+ECR+Helm+DockerHub retag
                    (GitHub)           EKS staging    EKS production
                         |                  |              |
                    you: Helm/kubectl   Actions        Actions
                    locally             (AWS)          (AWS)
```

| Step | GitHub Actions | Local (your PC) |
|------|----------------|-----------------|
| PR → `development` | **Build and Test** | — |
| Merge to `development` | **Build and Test** | `.\scripts\deploy-k8s.ps1 -Environment dev` (Helm) |
| Merge to `staging` | Test → ECR → Cosign → **Helm deploy staging** | Optional local staging Helm |
| Merge to `main` | Test → ECR → Helm prod → **Docker Hub retag** | Optional |

## Why no auto-deploy to your laptop

GitHub runners cannot reach Docker Desktop. **Staging/prod** use **EKS + Helm** via OIDC.

## Workflow file

`.github/workflows/ci-cd.yaml` — **CI/CD for API Gateway - Enterprise**
