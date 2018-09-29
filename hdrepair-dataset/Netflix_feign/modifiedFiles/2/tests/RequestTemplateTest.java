/*
 * Copyright 2013 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package feign;

import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static feign.RequestTemplate.expand;
import static feign.assertj.FeignAssertions.assertThat;
import static java.util.Arrays.asList;
import static org.assertj.core.data.MapEntry.entry;

public class RequestTemplateTest {

  /**
   * Avoid depending on guava solely for map literals.
   */
  private static Map<String, Object> mapOf(String key, Object val) {
    Map<String, Object> result = new LinkedHashMap<String, Object>();
    result.put(key, val);
    return result;
  }

  private static Map<String, Object> mapOf(String k1, Object v1, String k2, Object v2) {
    Map<String, Object> result = mapOf(k1, v1);
    result.put(k2, v2);
    return result;
  }

  private static Map<String, Object> mapOf(String k1, Object v1, String k2, Object v2, String k3,
                                           Object v3) {
    Map<String, Object> result = mapOf(k1, v1, k2, v2);
    result.put(k3, v3);
    return result;
  }

  @Test
  public void expandNotUrlEncoded() {
    for (String val : Arrays.asList("apples", "sp ace", "unic???de", "qu?stion")) {
      assertThat(expand("/users/{user}", mapOf("user", val)))
          .isEqualTo("/users/" + val);
    }
  }

  @Test
  public void expandMultipleParams() {
    assertThat(expand("/users/{user}/{repo}", mapOf("user", "unic???de", "repo", "foo")))
        .isEqualTo("/users/unic???de/foo");
  }

  @Test
  public void expandParamKeyHyphen() {
    assertThat(expand("/{user-dir}", mapOf("user-dir", "foo")))
        .isEqualTo("/foo");
  }

  @Test
  public void expandMissingParamProceeds() {
    assertThat(expand("/{user-dir}", mapOf("user_dir", "foo")))
        .isEqualTo("/{user-dir}");
  }

  @Test
  public void resolveTemplateWithParameterizedPathSkipsEncodingSlash() {
    RequestTemplate template = new RequestTemplate().method("GET")
        .append("{zoneId}");

    template.resolve(mapOf("zoneId", "/hostedzone/Z1PA6795UKMFR9"));

    assertThat(template)
        .hasUrl("/hostedzone/Z1PA6795UKMFR9");
  }

  @Test
  public void canInsertAbsoluteHref() {
    RequestTemplate template = new RequestTemplate().method("GET")
        .append("/hostedzone/Z1PA6795UKMFR9");

    template.insert(0, "https://route53.amazonaws.com/2012-12-12");

    assertThat(template)
        .hasUrl("https://route53.amazonaws.com/2012-12-12/hostedzone/Z1PA6795UKMFR9");
  }

  @Test
  public void resolveTemplateWithBaseAndParameterizedQuery() {
    RequestTemplate template = new RequestTemplate().method("GET")
        .append("/?Action=DescribeRegions").query("RegionName.1", "{region}");

    template.resolve(mapOf("region", "eu-west-1"));

    assertThat(template)
        .hasQueries(
            entry("Action", asList("DescribeRegions")),
            entry("RegionName.1", asList("eu-west-1"))
        );
  }

  @Test
  public void resolveTemplateWithBaseAndParameterizedIterableQuery() {
    RequestTemplate template = new RequestTemplate().method("GET")
        .append("/?Query=one").query("Queries", "{queries}");

    template.resolve(mapOf("queries", Arrays.asList("us-east-1", "eu-west-1")));

    assertThat(template)
        .hasQueries(
            entry("Query", asList("one")),
            entry("Queries", asList("us-east-1", "eu-west-1"))
        );
  }

  @Test
  public void resolveTemplateWithHeaderSubstitutions() {
    RequestTemplate template = new RequestTemplate().method("GET")
        .header("Auth-Token", "{authToken}");

    template.resolve(mapOf("authToken", "1234"));

    assertThat(template)
        .hasHeaders(entry("Auth-Token", asList("1234")));
  }

  @Test
  public void resolveTemplateWithMixedRequestLineParams() throws Exception {
    RequestTemplate template = new RequestTemplate().method("GET")//
        .append("/domains/{domainId}/records")//
        .query("name", "{name}")//
        .query("type", "{type}");

    template = template.resolve(
        mapOf("domainId", 1001, "name", "denominator.io", "type", "CNAME")
    );

    assertThat(template)
        .hasUrl("/domains/1001/records")
        .hasQueries(
            entry("name", asList("denominator.io")),
            entry("type", asList("CNAME"))
        );
  }

  @Test
  public void insertHasQueryParams() throws Exception {
    RequestTemplate template = new RequestTemplate().method("GET")//
        .append("/domains/1001/records")//
        .query("name", "denominator.io")//
        .query("type", "CNAME");

    template.insert(0, "https://host/v1.0/1234?provider=foo");

    assertThat(template)
        .hasUrl("https://host/v1.0/1234/domains/1001/records")
        .hasQueries(
            entry("provider", asList("foo")),
            entry("name", asList("denominator.io")),
            entry("type", asList("CNAME"))
        );
  }

  @Test
  public void resolveTemplateWithBodyTemplateSetsBodyAndContentLength() {
    RequestTemplate template = new RequestTemplate().method("POST")
        .bodyTemplate(
            "%7B\"customer_name\": \"{customer_name}\", \"user_name\": \"{user_name}\", " +
            "\"password\": \"{password}\"%7D");

    template = template.resolve(
        mapOf(
            "customer_name", "netflix",
            "user_name", "denominator",
            "password", "password"
        )
    );

    assertThat(template)
        .hasBody(
            "{\"customer_name\": \"netflix\", \"user_name\": \"denominator\", \"password\": \"password\"}")
        .hasHeaders(
            entry("Content-Length", asList(String.valueOf(template.body().length)))
        );
  }

  @Test
  public void skipUnresolvedQueries() throws Exception {
    RequestTemplate template = new RequestTemplate().method("GET")//
        .append("/domains/{domainId}/records")//
        .query("optional", "{optional}")//
        .query("name", "{nameVariable}");

    template = template.resolve(mapOf(
                                    "domainId", 1001,
                                    "nameVariable", "denominator.io"
                                )
    );

    assertThat(template)
        .hasUrl("/domains/1001/records")
        .hasQueries(
            entry("name", asList("denominator.io"))
        );
  }

  @Test
  public void allQueriesUnresolvable() throws Exception {
    RequestTemplate template = new RequestTemplate().method("GET")//
        .append("/domains/{domainId}/records")//
        .query("optional", "{optional}")//
        .query("optional2", "{optional2}");

    template = template.resolve(mapOf("domainId", 1001));

    assertThat(template)
        .hasUrl("/domains/1001/records")
        .hasQueries();
  }
}
