minikube start
minikube addons enable ingress
minikube docker-env | Invoke-Expression
@"
echo "Building Docker images in Minikube..."
docker build -t api-gateway:latest -f api-gateway/Dockerfile .
docker build -t auth-service:latest -f auth-service/Dockerfile .
docker build -t user-service:latest -f user-service/Dockerfile .
docker build -t order-service:latest -f order-service/Dockerfile .
docker build -t payment-service:latest -f payment-service/Dockerfile .
echo "Images built successfully!"
"@ | Out-File -FilePath build-images.ps1 -Encoding UTF8
.\build-images.ps1

Деплой:
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/configs/
kubectl apply -f k8s/databases/
kubectl apply -f k8s/infrastructure/
kubectl apply -f k8s/ingress/
kubectl apply -f k8s/monitoring/alloy.yaml -n innowise
kubectl apply -f otel-java-agent/templates/configmap.yaml -n innowise

Запуск сервисов сразу с мониторингом:
kubectl apply -f k8s/monitoring/otel-configmap.yaml
kubectl apply -k k8s

Проверка подов в реальном времени:
kubectl get pods -n innowise -w

Проверка создания таблиц для сервисов
kubectl exec order-db-0 -n innowise -- psql -U postgres -d order_service -c "\dt"
kubectl exec user-db-0 -n innowise -- psql -U postgres -d user_service -c "\dt"
kubectl exec auth-db-0 -n innowise -- psql -U postgres -d auth_service -c "\dt"
kubectl exec payment-db-0 -n innowise -- psql -U postgres -d payment_service -c "\dt"

Создание туннеля:
kubectl port-forward svc/api-gateway 8083:8083 -n innowise

Рестарт сервиса:
kubectl rollout restart deployment api-gateway -n innowise

Удаление подов:
kubectl delete all -n innowise --all
kubectl delete pvc -n innowise --all
kubectl delete configmap,secret -n innowise --all

Удаление контейнера:
minikube stop
minikube delete



Monitoring:

helm repo add grafana https://grafana.github.io/helm-charts
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update

Установка в namespace:
helm upgrade --install grafana grafana/grafana `
  -n innowise `
  --create-namespace

Узнать пароль для доступа в grafana:
login: admin
kubectl get secret grafana -n innowise `
  -o jsonpath="{.data.admin-password}" |
  %{ [System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String($_)) }

Тоннель для grafana:
kubectl port-forward svc/grafana 3000:80 -n innowise

Проверка:
helm repo list

Loki:
helm upgrade --install loki grafana/loki `
  -n innowise `
  -f k8s/monitoring/loki-values.yaml

Проверка:
kubectl get pods -n innowise | findstr loki

Тоннель для Loki:
kubectl port-forward svc/loki 3100:3100 -n innowise

Promtail:
helm upgrade --install promtail grafana/promtail `
  -n innowise `
  -f k8s/monitoring/promtail-values.yaml

Проверка:
kubectl get pods -n innowise | findstr promtail

Prometheus:
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update
helm upgrade --install prometheus prometheus-community/prometheus `
  -n innowise `
  --set server.service.type=ClusterIP

Tempo:
helm upgrade --install tempo grafana/tempo `
  -n innowise `
  -f k8s/monitoring/tempo.yaml

#Helm chart:
#helm create otel-java-agent
#helm template otel-java-agent ./otel-java-agent -n innowise > otel-agent-rendered.yaml
#Удалить:
#Remove-Item otel-java-agent/templates/deployment.yaml
#Remove-Item otel-java-agent/templates/service.yaml
#Remove-Item otel-java-agent/templates/ingress.yaml



Grafana - Data Source:
Prometheus: URL
            http://prometheus-server.innowise.svc.cluster.local
Loki: URL
      http://loki.innowise.svc.cluster.local:3100
      Derived fields:
        Name	trace_id
        Type	Regex in log line
        Regex	"trace_id":"([a-f0-9]+)"
        URL	${__value.raw}
        Internal link ON
        Datasource	Tempo
Tempo: URL
       http://tempo.innowise.svc.cluster.local:3200
       Datasource: Loki
       Datasource: Prometheus
