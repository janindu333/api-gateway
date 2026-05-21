# Distributed tracing (architecture doc)

**Flow:** Spring Boot services → **OTLP** → **Tempo** → **Grafana Explore** (correlate with **Loki** logs via trace id).

## Prerequisites

- **Tempo** and **Loki** running in namespace `observability` (you already have these).
- Services built with `micrometer-tracing-bridge-otel` and `application-k8s` profile.

## 1) Provision Grafana datasources (Tempo + Loki)

From repo root:

```powershell
kubectl apply -f api-gateway/k8s/grafana-datasources-configmap.yaml
kubectl apply -f api-gateway/k8s/grafana-deployment.yaml
kubectl rollout restart deployment/grafana -n saloon-system
```

Open **http://grafana.saloon.local** → **Connections → Data sources** — you should see **Tempo** and **Loki** (provisioned).

## 2) Rebuild and roll out services (OTLP + gateway tracing)

Rebuild images after pulling latest code (gateway now exports OTLP; booking no longer uses Zipkin):

```powershell
# Example per service (adjust paths/tags as you usually build)
cd e:\Tutorials\spring_boot\baber-booking\api-gateway
mvn -DskipTests package
docker build -t api-gateway:local .

# Repeat for identity-service, booking-service, saloon-service, payment-service, notification-service
```

Apply deployments:

```powershell
kubectl apply -f api-gateway/k8s/api-gateway-deployment.yaml
kubectl apply -f identity-service/k8s/deployment.yaml
kubectl apply -f booking-service/k8s/deployment.yaml
kubectl apply -f saloon-service/k8s/deployment.yaml
kubectl apply -f payment-service/k8s/deployment.yaml
kubectl apply -f notification-service/k8s/deployment.yaml
```

Rollouts:

```powershell
kubectl rollout status deployment/api-gateway -n saloon-system
kubectl rollout status deployment/identity-service -n saloon-system
```

## 3) Generate a distributed trace

1. Call the API through the **gateway** (not directly to a pod), e.g. Swagger at `http://api.saloon.local` or your frontend.
2. Hit a path that touches multiple services (e.g. booking → payment / saloon / identity).

W3C headers (`traceparent`) are propagated automatically when `management.tracing.propagation.type=w3c`.

## 4) View traces in Grafana

1. **Explore** → datasource **Tempo**.
2. **Search** tab → Service name e.g. `api-gateway` or `identity-service` → **Run query**.
3. Open a trace — you should see spans from **multiple services** on one trace id.
4. In the trace view, use **Logs for this trace** (tracesToLogs) to jump to Loki with the same trace id.

**LogQL shortcut** (same as logs guide):

```logql
{job="kubernetes-pods"} |= "<trace-id-from-tempo>"
```

## 5) Verify from the cluster

```powershell
# Recent traces in Tempo
kubectl exec -n observability deploy/tempo -- wget -qO- "http://localhost:3200/api/search?limit=5"

# Gateway exporting (after redeploy)
kubectl exec -n observability deploy/tempo -- wget -qO- "http://localhost:3200/api/search?tags=service.name%3Dapi-gateway&limit=3"
```

## Production note (doc)

Staging/prod may use **AWS X-Ray** or an **OpenTelemetry Collector** in front of Tempo/X-Ray. Local/dev uses **direct OTLP → Tempo** (`http://tempo.observability.svc.cluster.local:4318/v1/traces`).

## Troubleshooting

| Symptom | Fix |
|--------|-----|
| Only `identity-service` in Tempo, no gateway | Rebuild/redeploy **api-gateway** with OTLP deps; call API via gateway. |
| Broken trace (gaps between services) | Ensure all services use **k8s** profile and W3C; remove `SPRING_ZIPKIN_*` env overrides. |
| No Tempo datasource in Grafana | Apply `grafana-datasources-configmap.yaml` and restart Grafana. |
| Logs ↔ trace link missing | Use provisioned Loki **derived field** or LogQL `|= traceId` as above. |
