# Grafana behind nginx Ingress (same pattern as `api.saloon.local`)

Use a **dedicated host** `grafana.saloon.local` on the **same** ingress controller and the **same** LB IP in your Windows `hosts` file.

## Prerequisites

- Ingress NGINX running in the cluster (you already use it for `api-gateway-ingress`).
- Namespace `saloon-system` exists.

## 1) Create admin password secret (once)

Replace `YOUR_PASSWORD` with a strong value:

```powershell
kubectl create secret generic grafana-admin `
  --namespace saloon-system `
  --from-literal=admin-user=admin `
  --from-literal=admin-password=YOUR_PASSWORD `
  --dry-run=client -o yaml | kubectl apply -f -
```

## 2) Deploy Grafana + Service

From repo root:

```powershell
kubectl apply -f api-gateway/k8s/grafana-deployment.yaml
```

Wait until ready:

```powershell
kubectl rollout status deployment/grafana -n saloon-system
```

## 3) Expose with Ingress

```powershell
kubectl apply -f api-gateway/k8s/grafana-ingress.yaml
```

## 4) Windows hosts file

1. Get ingress LB IP (same command you used for API):

   ```powershell
   kubectl get ingress -n saloon-system
   ```

2. Edit `C:\Windows\System32\drivers\etc\hosts` as Administrator and add (use **your** IP from the column `ADDRESS` for the nginx ingress):

   ```text
   <INGRESS_LB_IP>  api.saloon.local
   <INGRESS_LB_IP>  grafana.saloon.local
   ```

## 5) Open Grafana

Browser: **http://grafana.saloon.local/**

- User: `admin`
- Password: the value you set in the secret (`YOUR_PASSWORD`).

## Production / hardening (later)

- Do not commit real passwords; use Sealed Secrets or External Secrets.
- Add TLS (cert-manager + Ingress `tls` block).
- Restrict network policies; use a real persistence volume for Grafana data instead of `emptyDir`.
- Prefer the **kube-prometheus-stack** Helm chart if you want Prometheus + dashboards in one go; this minimal setup is for **local / simple** exposure.

## Troubleshooting

| Symptom | Check |
|--------|--------|
| 404 nginx | Ingress applied? `kubectl describe ingress grafana-ingress -n saloon-system` |
| 502 | Pods running? `kubectl get pods -n saloon-system -l app=grafana` — Service port must match Deployment (3000). |
| Wrong links / redirects | `GF_SERVER_ROOT_URL` must match the URL you use in the browser (including host). |

## Logs (Loki + Fluent Bit)

To ship pod logs to Loki with Fluent Bit, see **`api-gateway/k8s/observability/LOGGING_LOKI.md`** and apply `fluent-bit.yaml` in the `observability` namespace.

## Traces (Tempo)

Provision Tempo + Loki datasources and view distributed traces:

```powershell
kubectl apply -f api-gateway/k8s/grafana-datasources-configmap.yaml
kubectl apply -f api-gateway/k8s/grafana-deployment.yaml
kubectl rollout restart deployment/grafana -n saloon-system
```

Full steps: **`api-gateway/k8s/observability/TRACING_TEMPO.md`**.
