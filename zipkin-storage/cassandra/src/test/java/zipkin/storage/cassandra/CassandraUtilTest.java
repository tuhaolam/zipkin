/**
 * Copyright 2015-2016 The OpenZipkin Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package zipkin.storage.cassandra;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import zipkin.Constants;
import zipkin.Span;
import zipkin.TestObjects;
import zipkin.TraceKeys;
import zipkin.storage.QueryRequest;

import static org.assertj.core.api.Assertions.assertThat;

public class CassandraUtilTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void annotationKeys_emptyRequest() {
    assertThat(CassandraUtil.annotationKeys(QueryRequest.builder().build()))
        .isEmpty();
  }

  @Test
  public void annotationKeys_serviceNameRequired() {
    thrown.expect(IllegalArgumentException.class);

    CassandraUtil.annotationKeys(QueryRequest.builder().addAnnotation("sr").build());
  }

  @Test
  public void annotationKeys() {
    assertThat(CassandraUtil.annotationKeys(QueryRequest.builder()
        .serviceName("service")
        .addAnnotation(Constants.ERROR)
        .addBinaryAnnotation(TraceKeys.HTTP_METHOD, "GET").build()))
        .containsExactly("service:error", "service:http.method:GET");
  }

  @Test
  public void annotationKeys_dedupes() {
    assertThat(CassandraUtil.annotationKeys(QueryRequest.builder()
        .serviceName("service")
        .addAnnotation(Constants.ERROR)
        .addAnnotation(Constants.ERROR).build()))
        .containsExactly("service:error");
  }

  @Test
  public void annotationKeys_skipsCoreAndAddressAnnotations() throws Exception {
    Span span = TestObjects.TRACE.get(1);

    assertThat(span.annotations)
        .extracting(a -> a.value)
        .matches(Constants.CORE_ANNOTATIONS::containsAll);

    assertThat(span.binaryAnnotations)
        .extracting(b -> b.key)
        .containsOnly(Constants.SERVER_ADDR, Constants.CLIENT_ADDR);

    assertThat(CassandraUtil.annotationKeys(span))
        .isEmpty();
  }
}
