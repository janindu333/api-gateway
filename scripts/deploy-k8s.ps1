# Deploy api-gateway to local Kubernetes (Docker Desktop).
# Used by GitHub Actions self-hosted runner on push to development / staging / main.
param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('dev', 'staging', 'prod')]
    [string]$Environment,

    [string]$Namespace = 'saloon-system',

    [switch]$SkipBuild,

    [switch]$PullFromDockerHub
)

$ErrorActionPreference = 'Stop'
$RepoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $RepoRoot

$imageTag = switch ($Environment) {
    'dev' { 'dev' }
    'staging' { 'staging' }
    'prod' { 'prod' }
}
$localImage = "api-gateway:$imageTag"

Write-Host "==> Environment: $Environment | Image: $localImage | Namespace: $Namespace"

if ($PullFromDockerHub) {
    $remote = 'janindu3/api-gateway:latest'
    Write-Host "==> Pull $remote"
    docker pull $remote
    docker tag $remote $localImage
} elseif (-not $SkipBuild) {
    Write-Host '==> docker build'
    docker build -t $localImage .
}

Write-Host '==> kubectl apply deployment'
kubectl apply -f k8s/api-gateway-deployment.yaml -n $Namespace

Write-Host "==> kubectl set image"
kubectl set image "deployment/api-gateway" "api-gateway=$localImage" -n $Namespace

Write-Host '==> rollout status'
kubectl rollout status deployment/api-gateway -n $Namespace --timeout=300s

Write-Host '==> Done'
kubectl get pods -n $Namespace -l app=api-gateway
