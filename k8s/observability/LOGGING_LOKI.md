# Centralized logging (architecture doc)

Flow: **applications → stdout → node log files → Fluent Bit (DaemonSet) → Loki → Grafana**.

Spring Boot services use `application-k8s.properties` with a console pattern that includes **`traceId`** and **`spanId`** (Micrometer MDC) so you can correlate logs with traces in Tempo.

## Prerequisites

- Namespace **`observability`** (same as Tempo in this project).
- **Loki** reachable in that namespace as **`loki:3100`** (Service name `loki`). If your Loki uses another name or namespace, edit `fluent-bit.yaml` and change the `[OUTPUT]` `Host` (and optionally `Port`).

## Deploy Fluent Bit

```powershell
kubectl apply -f api-gateway/k8s/observability/fluent-bit.yaml
```

Check pods:

```powershell
kubectl get pods -n observability -l app.kubernetes.io/name=fluent-bit
kubectl logs -n observability -l app.kubernetes.io/name=fluent-bit --tail=50
```

## Grafana (Loki)

In Grafana → Explore → Loki, try:

```logql
{job="fluent-bit"}
```

Or filter by namespace (labels come from Kubernetes metadata when `auto_kubernetes_labels` is on):

```logql
{namespace_name="saloon-system"}
```

## Production note (doc)

For production, the reference architecture often prefers **CloudWatch** (or a managed log stack) instead of self-hosted Loki. This manifest targets **local/dev** Loki behind Fluent Bit.

## If `observability` namespace does not exist

```powershell
kubectl create namespace observability
```

Then install or point Fluent Bit at your Loki Service URL.
