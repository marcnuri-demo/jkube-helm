JKube + Helm
============

## OpenShift

### Prepare environment

Initialize a ChartMuseum instance. This is required for the outer-loop.

```shell
jbang ./demo/PrepareOpenShiftEnvironment.java
```

### Outer Loop

No Maven profile.

First part runs on CI server.
```shell
mvn clean package k8s:build k8s:push k8s:resource k8s:helm k8s:helm-push
```
Second part executed by operator?
```shell
helm repo add dev-sandbox http://chart-museum.dev-sandbox.marcnuri.com/ --username secret --password shouldnt-be-here-use-env
helm repo update dev-sandbox
helm install jkube-helm dev-sandbox/jkube-helm --devel
helm delete jkube-helm
helm install jkube-helm dev-sandbox/jkube-helm --devel   \
  --set ingress.host=production.dev-sandbox.marcnuri.com \
  --set application.greeting="Prod Override"
helm delete jkube-helm
```

### Inner Loop

Use the `dev` and `OpenShift` Maven profiles.

Provides default values for placeholders applicable in the `dev` environment for OpenShift.

```shell
mvn -Pdev,openshift clean package oc:build oc:resource oc:apply
mvn -Pdev,openshift oc:undeploy
```

## Minikube (needs updating)

TODO: Update this section

### Inner Loop

`dev` Maven profile.

Provides default values for placeholders applicable in the dev environment.

We assume we are running on a Minikube cluster.
```shell
$ eval $(minikube docker-env)
$ mvn -Pdev clean package k8s:build k8s:resource k8s:apply -Dingress.host=local-dev.$(minikube ip).nip.io
$ mvn -Pdev k8s:undeploy
```

### Outer Loop

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

#### Demo
We assume we are running on a Minikube cluster for demo purposes.
```shell
$ eval $(minikube docker-env)
$ mvn clean package k8s:build k8s:resource k8s:helm
$ helm install jkube-helm ./target/jkube-helm-0.0.1-SNAPSHOT-helm.tar.gz         \
    --set ingress.host=production.$(minikube ip).nip.io                          \
    --set deployment.container.imagepullpolicy=IfNotPresent
$ helm delete jkube-helm
```


## Plugins

### Maven Resources Plugin

Used to interpolate Maven properties in `application.properties` file.

Since it's using the Spring Boot parent, filtered properties are delimited by `@...@`.
