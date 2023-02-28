///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 17+
//DEPS io.fabric8:openshift-client:6.4.0
//DEPS org.apache.commons:commons-compress:1.22

import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServiceSpecBuilder;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpecBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.IngressBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.IngressSpecBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.ServerSideApplicable;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class PrepareOpenShiftEnvironment {

  private static final String APP = "app";
  private static final String CHART_MUSEUM = "chart-museum";
  public static void main(String... args) {
    try (var kc = new KubernetesClientBuilder().build()) {
      deployChartMuseum(kc);
    }
  }

  private static void deployChartMuseum(KubernetesClient kc) {
    delete(
      kc.apps().deployments().withName(CHART_MUSEUM),
      kc.persistentVolumeClaims().withName(CHART_MUSEUM),
      kc.apps().deployments().withName(CHART_MUSEUM),
      kc.services().withName(CHART_MUSEUM),
      kc.network().v1().ingresses().withName(CHART_MUSEUM)
    );
    final var persistentVolumeClaim = new PersistentVolumeClaimBuilder()
      .withMetadata(new ObjectMetaBuilder()
        .withName(CHART_MUSEUM)
        .addToLabels(APP, CHART_MUSEUM)
        .build())
      .withNewSpec()
      .withAccessModes("ReadWriteOnce")
      .withNewResources()
      .addToRequests("storage", Quantity.parse("256Mi"))
      .endResources()
      .endSpec()
      .build();
    final var deployment = new DeploymentBuilder()
      .withMetadata(new ObjectMetaBuilder()
        .withName(CHART_MUSEUM)
        .addToLabels(APP, CHART_MUSEUM)
        .build()
      )
      .withSpec(new DeploymentSpecBuilder()
        .withReplicas(1)
        .withNewSelector()
        .addToMatchLabels(APP, CHART_MUSEUM)
        .endSelector()
        .withNewTemplate()
        .withNewMetadata()
        .addToLabels(APP, CHART_MUSEUM)
        .endMetadata()
        .withNewSpec()
        .addToContainers(new ContainerBuilder()
          .withName(CHART_MUSEUM)
          .withImage("ghcr.io/helm/chartmuseum:v0.15.0")
          .addToEnv(new EnvVar("STORAGE", "LOCAL", null))
          .addToEnv(new EnvVar("STORAGE_LOCAL_ROOTDIR", "/charts", null))
          .addToEnv(new EnvVar("ALLOW_OVERWRITE", "true", null))
          .addToEnv(new EnvVar("BASIC_AUTH_USER", "secret", null))
          .addToEnv(new EnvVar("BASIC_AUTH_PASS", "shouldnt-be-here-use-env", null))
          .addNewPort().withContainerPort(8080).endPort()
          .addNewVolumeMount()
          .withName("data")
          .withMountPath("/charts")
          .endVolumeMount()
          .build())
        .addToVolumes(new VolumeBuilder()
          .withName("data")
          .withNewPersistentVolumeClaim()
          .withClaimName(CHART_MUSEUM)
          .endPersistentVolumeClaim()
          .build())
        .endSpec()
        .endTemplate()
        .build())
      .build();
    final var ingress = new IngressBuilder()
      .withMetadata(new ObjectMetaBuilder()
        .withName(CHART_MUSEUM)
        .addToLabels(APP, CHART_MUSEUM)
        .build()
      )
      .withSpec(new IngressSpecBuilder()
        .addNewRule()
        .withHost("chart-museum.dev-sandbox.marcnuri.com")
        .withNewHttp().addNewPath()
        .withNewBackend().withNewService()
        .withName(CHART_MUSEUM)
        .withNewPort().withNumber(8080).endPort()
        .endService().endBackend()
        .withPath("/")
        .withPathType("ImplementationSpecific")
        .endPath().endHttp()
        .endRule()
        .build())
      .build();
    apply(kc, persistentVolumeClaim, deployment, service(CHART_MUSEUM, CHART_MUSEUM, 8080), ingress);
  }

  private static void apply(KubernetesClient kc, HasMetadata... resources) {
    Stream.of(resources).map(kc::resource).forEach(ServerSideApplicable::serverSideApply);
  }

  private static void delete(Resource<?>... resources) {
    Stream.of(resources).forEach(r -> {
      r.withGracePeriod(0L).delete();
      r.waitUntilCondition(Objects::isNull, 10, TimeUnit.SECONDS);
    });
  }

  private static Service service(String name, String app, int port) {
    return new ServiceBuilder()
      .withMetadata(new ObjectMetaBuilder()
        .withName(name)
        .addToLabels(APP, app)
        .build())
      .withSpec(new ServiceSpecBuilder()
        .addToSelector(APP, app)
        .addNewPort().withPort(port).endPort()
        .build())
      .build();
  }
}
