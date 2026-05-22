# Deploy api-gateway to Kubernetes.
# - Local (Docker Desktop): Helm with values-local.yaml (default)
# - Legacy: plain kubectl manifests (use -UseKubectlManifests)
param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('dev', 'staging', 'prod')]
    [string]$Environment,

    [string]$Namespace = 'saloon-system',

    [switch]$SkipBuild,

    [switch]$PullFromDockerHub,

    [switch]$UseKubectlManifests
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

Write-Host "==> Environment: $Environment | Namespace: $Namespace"

if ($UseKubectlManifests) {
    Write-Host '==> Legacy kubectl deploy (k8s/*.yaml)'
    if ($PullFromDockerHub) {
        $remote = 'janindu3/api-gateway:latest'
        docker pull $remote
        docker tag $remote $localImage
    } elseif (-not $SkipBuild) {
        docker build -t $localImage .
    }
    kubectl apply -f k8s/api-gateway-deployment.yaml -n $Namespace
    kubectl set image "deployment/api-gateway" "api-gateway=$localImage" -n $Namespace
    kubectl rollout status deployment/api-gateway -n $Namespace --timeout=300s
    kubectl get pods -n $Namespace -l app=api-gateway
    exit 0
}

# Helm (enterprise chart) — recommended for local
$valuesFile = switch ($Environment) {
    'dev' { 'helm/api-gateway/values-local.yaml' }
    'staging' { 'helm/api-gateway/values-staging.yaml' }
    'prod' { 'helm/api-gateway/values-production.yaml' }
}

if (-not $SkipBuild) {
    Write-Host "==> docker build -t $localImage"
    docker build -t $localImage .
}

$helmArgs = @(
    'upgrade', '--install', 'api-gateway', './helm/api-gateway',
    '-f', $valuesFile,
    '--namespace', $Namespace,
    '--create-namespace',
    '--set', "image.digest=$localImage",
    '--set', 'image.pullPolicy=IfNotPresent',
    '--timeout', '10m'
)

if ($PullFromDockerHub) {
    $remote = 'janindu3/api-gateway:latest'
    Write-Host "==> Pull $remote"
    docker pull $remote
    $helmArgs += '--set'
    $helmArgs += "image.digest=$remote"
}

Write-Host "==> helm $($helmArgs -join ' ')"
& helm @helmArgs

Write-Host '==> Done'
kubectl get pods -n $Namespace -l app.kubernetes.io/name=api-gateway
