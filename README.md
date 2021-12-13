JKube + Helm
============

## Inner Loop

`dev` Maven profile.

Provides default values for placeholders applicable in the dev environment.

We assume we are running on a Minikube cluster.
```shell
$ eval $(minikube docker-env)
$ mvn -Pdev clean package k8s:build k8s:resource k8s:apply -DapplicationHost=local-dev.$(minikube ip).nip.io
$ mvn -Pdev k8s:undeploy
```

## Outer Loop

No Maven profile.

Values for placeholders are provided from the `helm-variables-template.yml` file.

First part runs on CI server.
```shell
$ mvn clean package k8s:build k8s:push k8s:resource k8s:helm k8s:helm-push
```
Second part executed by operator?
```shell
$ helm install ...
```

### Demo
We assume we are running on a Minikube cluster for demo purposes.
```shell
$ eval $(minikube docker-env)
$ mvn clean package k8s:build k8s:resource k8s:helm
$ helm install jkube-helm ./target/jkube-helm-0.0.1-SNAPSHOT-helm.tar.gz            \
    --set applicationhost=production.$(minikube ip).nip.io                          \
    --set imagepullpolicy=IfNotPresent
$ helm delete jkube-helm
```


## Plugins

### Maven Resources Plugin

Used to interpolate Maven properties in `application.properties` file.

Since it's using the Spring Boot parent, filtered properties are delimited by `@...@`.
