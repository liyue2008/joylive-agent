# Joylive Demo Kubernetes Gateway

## Getting Started

这个Gateway是基于Spring Cloud Gateway构建的，主要用于将请求路由到不同的Provider应用。

### 前置条件

1. 安装了 Kubernetes 集群，本例使用Minikube演示。
2. 集群中部署了[joylive-demo-kubernetes-provider](../joylive-demo-kubernetes-provider/README.md)应用。

### 部署 Gateway 应用

编译项目：

```bash
mvn clean package -DskipTests
```

进入joylive-demo-kubernetes-gateway目录，构建Docker镜像：

```bash
cd joylive-agent/joylive-demo/joylive-demo-kubernetes/joylive-demo-kubernetes-gateway
docker build -t joylive-demo-kubernetes-gateway:1.5.0 .
```

将镜像加载到Minikube中：

```bash
minikube image load joylive-demo-kubernetes-gateway:1.5.0
```

部署应用并确保svc和pods都正常启动：

```bash
kubectl apply -f deployment.yaml

# 查看svc
kubectl get svc
NAME                              TYPE        CLUSTER-IP    EXTERNAL-IP   PORT(S)        AGE
joylive-demo-kubernetes-gateway   NodePort    10.110.36.2   <none>        80:31827/TCP   5m1s
kubernetes                        ClusterIP   10.96.0.1     <none>        443/TCP        13d

# 查看pods
kubectl get pods
NAME                                               READY   STATUS    RESTARTS   AGE
joylive-demo-kubernetes-gateway-6c5c5b4b69-pq2mz   1/1     Running   0          5m4s

```

将Gateway应用暴露到外部：

```bash
kubectl port-forward svc/joylive-demo-kubernetes-gateway 8081:80
Forwarding from 127.0.0.1:8081 -> 80
Forwarding from [::1]:8081 -> 80
```

### 测试Gateway

使用curl访问，可以看到当请求被路由到joylive-demo-kubernetes-provider服务上：

```bash
curl  "http://127.0.0.1:8081/echo/abc" -H "User: unit2"
{
  "code" : 200,
  "traces" : [ {
    "service" : "service-provider",
    "location" : {
      "liveSpaceId" : "v4bEh4kd6Jvu5QBX09qYq-qlbcs",
      "unit" : "unit2",
      "cell" : "cell2",
      "ruleId" : "rule2",
      "laneSpaceId" : "2",
      "lane" : "production",
      "ip" : "10.244.0.48"
    },
    "transmission" : {
      "carrier" : "header"
    }
  } ],
  "data" : "abc"
}
```

