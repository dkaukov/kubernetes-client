/*
 * Copyright (C) 2015 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.openshift.client.server.mock;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.dsl.NonDeletingOperation;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.fabric8.openshift.api.model.SecurityContextConstraints;
import io.fabric8.openshift.api.model.SecurityContextConstraintsBuilder;
import io.fabric8.openshift.api.model.SecurityContextConstraintsList;
import io.fabric8.openshift.api.model.SecurityContextConstraintsListBuilder;
import io.fabric8.openshift.client.OpenShiftClient;
import org.junit.jupiter.api.Test;

import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@EnableKubernetesMockClient
class SecurityContextConstraintsTest {

  KubernetesMockServer server;
  OpenShiftClient client;

  @Test
  void testCreateOrReplace() {
    // Given
    SecurityContextConstraints scc = new SecurityContextConstraintsBuilder()
        .withNewMetadata().withName("scc1").endMetadata()
        .withAllowPrivilegedContainer(true)
        .withNewRunAsUser().withType("RunAsAny").endRunAsUser()
        .withNewSeLinuxContext().withType("RunAsAny").endSeLinuxContext()
        .withUsers("admin")
        .withGroups("admin-group")
        .build();
    server.expect().post().withPath("/apis/security.openshift.io/v1/securitycontextconstraints")
        .andReturn(HttpURLConnection.HTTP_OK, scc)
        .once();

    // When
    scc = client.securityContextConstraints().resource(scc).createOr(NonDeletingOperation::update);

    // Then
    assertThat(scc)
        .isNotNull()
        .hasFieldOrPropertyWithValue("metadata.name", "scc1")
        .hasFieldOrPropertyWithValue("users", Collections.singletonList("admin"))
        .hasFieldOrPropertyWithValue("groups", Collections.singletonList("admin-group"));
  }

  @Test
  void testLoad() {
    // Given
    server.expect().post().withPath("/apis/security.openshift.io/v1/securitycontextconstraints")
        .andReturn(HttpURLConnection.HTTP_OK, new SecurityContextConstraintsBuilder().build())
        .once();

    // When
    List<HasMetadata> items = client.load(getClass().getResourceAsStream("/test-scc.yml")).createOrReplace();

    // Then
    assertNotNull(items);
    assertEquals(1, items.size());
    assertInstanceOf(SecurityContextConstraints.class, items.get(0));
  }

  @Test
  void testList() {
    server.expect().withPath("/apis/security.openshift.io/v1/securitycontextconstraints")
        .andReturn(200, new SecurityContextConstraintsListBuilder()
            .addNewItem().endItem()
            .build())
        .once();

    SecurityContextConstraintsList sccList = client.securityContextConstraints().list();
    assertNotNull(sccList);
    assertEquals(1, sccList.getItems().size());
  }

  @Test
  void testDelete() {
    server.expect().withPath("/apis/security.openshift.io/v1/securitycontextconstraints/scc1")
        .andReturn(200, new SecurityContextConstraintsBuilder().build()).once();
    server.expect().withPath("/apis/security.openshift.io/v1/securitycontextconstraints/scc2")
        .andReturn(200, new SecurityContextConstraintsBuilder().build()).once();

    client.securityContextConstraints().withName("scc1").delete();

    assertNotEquals(1, client.securityContextConstraints().withName("scc1").delete().size());

    assertEquals(1, client.securityContextConstraints().withName("scc2").delete().size());
  }

  @Test
  void testEdit() {
    server.expect().withPath("/apis/security.openshift.io/v1/securitycontextconstraints/scc1")
        .andReturn(200, new SecurityContextConstraintsBuilder().withNewMetadata().withName("scc1").and().build()).once();
    server.expect().patch().withPath("/apis/security.openshift.io/v1/securitycontextconstraints/scc1")
        .andReturn(200, new SecurityContextConstraintsBuilder().withNewMetadata().withName("scc1").and()
            .withAllowedCapabilities(Collections.singletonList("allowed")).build())
        .once();

    SecurityContextConstraints scc = client.securityContextConstraints().withName("scc1")
        .edit(s -> new SecurityContextConstraintsBuilder(s).withAllowedCapabilities(Collections.singletonList("allowed"))
            .build());
    assertThat(scc)
        .isNotNull()
        .hasFieldOrPropertyWithValue("allowedCapabilities", Collections.singletonList("allowed"));
  }

}
