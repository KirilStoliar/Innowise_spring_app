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
kubectl apply -f k8s/deployments/
kubectl apply -f k8s/services/
kubectl apply -f k8s/ingress/

Проверка подов в реальном времени:
kubectl get pods -n innowise -w

Проверка создания таблиц для сервисов
kubectl exec order-db-0 -n innowise -- psql -U postgres -d order_service -c "\dt"
kubectl exec user-db-0 -n innowise -- psql -U postgres -d user_service -c "\dt"
kubectl exec auth-db-0 -n innowise -- psql -U postgres -d auth_service -c "\dt"
kubectl exec payment-db-0 -n innowise -- psql -U postgres -d payment_service -c "\dt"

Создание туннеля:
kubectl port-forward svc/api-gateway 8083:8083 -n innowise

Удаляние подов:
kubectl delete all -n innowise --all
kubectl delete pvc -n innowise --all
kubectl delete configmap,secret -n innowise --all

Удаление контейнера:
minikube stop
minikube delete