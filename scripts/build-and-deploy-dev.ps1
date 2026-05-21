<#
.SYNOPSIS
    Local Kubernetes dev: Maven package, Docker build (:local tags), kubectl apply in safe order.

.DESCRIPTION
    Run from repo: api-gateway/scripts/
    Default Root is the parent folder that contains api-gateway, booking-service, etc. (baber-booking).

.PARAMETER Root
    Override if your layout differs.

.PARAMETER SkipMaven / SkipDocker / SkipApply
.PARAMETER ApplyHpaPdb
    Apply HPA and PDB (needs metrics-server; may scale replicas > 1).
#>
[CmdletBinding()]
param(
    [string] $Root = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path,
    [switch] $SkipMaven,
    [switch] $SkipDocker,
    [switch] $SkipApply,
    [switch] $ApplyHpaPdb
)

$ErrorActionPreference = "Stop"
$ns = "saloon-system"

function Invoke-MavenPackage {
    param([string] $ServiceDir)
    Push-Location $ServiceDir
    try {
        Write-Host "`n>>> mvn package: $ServiceDir" -ForegroundColor Cyan
        & .\mvnw.cmd -q -DskipTests package
        if ($LASTEXITCODE -ne 0) { throw "Maven failed in $ServiceDir" }
    }
    finally { Pop-Location }
}

function Invoke-DockerBuild {
    param([string] $ServiceDir, [string] $ImageTag)
    Push-Location $ServiceDir
    try {
        Write-Host "`n>>> docker build: $ImageTag ($ServiceDir)" -ForegroundColor Cyan
        docker build -t $ImageTag .
        if ($LASTEXITCODE -ne 0) { throw "docker build failed: $ImageTag" }
    }
    finally { Pop-Location }
}

$mavenServices = @(
    "booking-service",
    "identity-service",
    "notification-service",
    "payment-service",
    "saloon-service",
    "api-gateway"
)

if (-not $SkipMaven) {
    foreach ($s in $mavenServices) {
        $dir = Join-Path $Root $s
        if (-not (Test-Path $dir)) { Write-Warning "Skip missing: $dir"; continue }
        Invoke-MavenPackage $dir
    }
}

$dockerImages = @(
    @{ Service = "booking-service";      Tag = "booking-service:local" },
    @{ Service = "identity-service";     Tag = "identity-service:local" },
    @{ Service = "notification-service"; Tag = "notification-service:local" },
    @{ Service = "payment-service";    Tag = "payment-service:local" },
    @{ Service = "saloon-service";     Tag = "saloon-service:local" },
    @{ Service = "api-gateway";        Tag = "api-gateway:local" }
)

if (-not $SkipDocker) {
    foreach ($row in $dockerImages) {
        $dir = Join-Path $Root $row.Service
        if (-not (Test-Path $dir)) { Write-Warning "Skip missing: $dir"; continue }
        Invoke-DockerBuild $dir $row.Tag
    }
}

if ($SkipApply) {
    Write-Host "`nSkipApply: done after builds." -ForegroundColor Yellow
    exit 0
}

$gw = Join-Path $Root "api-gateway\k8s"

Write-Host "`n>>> kubectl apply (namespace + secrets + mysql)" -ForegroundColor Cyan
kubectl apply -f (Join-Path $gw "namespace.yaml")
kubectl apply -f (Join-Path $gw "secrets.yaml")
$devSecrets = Join-Path $gw "secrets-dev-local.yaml"
if (Test-Path $devSecrets) {
    kubectl apply -f $devSecrets
} else {
    Write-Host "(no secrets-dev-local.yaml - optional api-gateway-secrets)" -ForegroundColor DarkGray
}
kubectl apply -f (Join-Path $gw "mysql-deployment.yaml")

$kafkaDev = Join-Path $gw "kafka-dev.yaml"
if (Test-Path $kafkaDev) {
    Write-Host "`n>>> kubectl apply kafka (dev Redpanda as kafka-service:9092)" -ForegroundColor Cyan
    kubectl apply -f $kafkaDev
}

function Apply-ServiceK8s {
    param([string] $ServiceName)
    $kd = Join-Path $Root "$ServiceName\k8s"
    if (-not (Test-Path $kd)) { Write-Warning "No k8s folder: $kd"; return }
    $files = Get-ChildItem $kd -Filter "*.yaml" | Sort-Object Name
    foreach ($f in $files) {
        if (-not $ApplyHpaPdb -and ($f.Name -match '^(hpa|pdb)\.yaml$')) { continue }
        Write-Host "  apply $($f.FullName)" -ForegroundColor DarkGray
        kubectl apply -f $f.FullName
    }
}

$applyOrder = @(
    "booking-service",
    "identity-service",
    "notification-service",
    "payment-service",
    "saloon-service"
)

Write-Host "`n>>> kubectl apply microservices" -ForegroundColor Cyan
foreach ($s in $applyOrder) { Apply-ServiceK8s $s }

Write-Host "`n>>> kubectl apply api-gateway" -ForegroundColor Cyan
kubectl apply -f (Join-Path $gw "api-gateway-service.yaml")
kubectl apply -f (Join-Path $gw "api-gateway-deployment.yaml")

Write-Host "`n>>> rollout status" -ForegroundColor Cyan
$deploys = @(
    "booking-service","identity-service","notification-service",
    "payment-service","saloon-service","api-gateway"
)
foreach ($d in $deploys) {
    kubectl rollout status "deployment/$d" -n $ns --timeout=180s *> $null
    if ($LASTEXITCODE -ne 0) {
        Write-Warning "rollout status timeout or missing: $d"
    }
}

Write-Host "`nDone. Namespace $ns (environment=development). Kafka: kafka-dev.yaml (if applied)." -ForegroundColor Green
kubectl get pods -n $ns -o wide
