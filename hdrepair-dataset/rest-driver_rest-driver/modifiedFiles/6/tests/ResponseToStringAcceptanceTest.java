/**
 * Copyright � 2010-2011 Nokia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.restdriver.serverdriver.acceptance;

import com.github.restdriver.clientdriver.ClientDriverRequest;
import com.github.restdriver.clientdriver.ClientDriverResponse;
import com.github.restdriver.clientdriver.example.ClientDriverUnitTest;
import com.github.restdriver.serverdriver.http.response.Response;
import org.junit.Before;
import org.junit.Test;

import static com.github.restdriver.serverdriver.RestServerDriver.get;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

/**
 * User: mjg
 * Date: 07/05/11
 * Time: 21:29
 */
public class ResponseToStringAcceptanceTest
        extends ClientDriverUnitTest {

    private String baseUrl;

    @Before
    public void getServerDetails() {
        baseUrl = super.getClientDriver().getBaseUrl();
    }

    @Test
    public void testToStringWithoutResponseBody() {
        getClientDriver().addExpectation(new ClientDriverRequest("/"), new ClientDriverResponse("").withStatus(400));

        Response response = get(baseUrl);

        String expectedResponse = "HTTP/1.1 400 Bad Request\n";
        expectedResponse += "Content-Type: text/plain;charset=ISO-8859-1\n" +
                            "Content-Length: 0\n" +
                            "Server: Jetty(7.3.1.v20110307)";

        assertThat(response.toString(), is(equalTo(expectedResponse)));

    }

    @Test
    public void testToStringWithResponseBody() {
        getClientDriver().addExpectation(new ClientDriverRequest("/"), new ClientDriverResponse("This is the content"));

        Response response = get(baseUrl);

        String expectedResponse = "HTTP/1.1 200 OK\n";
        expectedResponse += "Content-Type: text/plain;charset=ISO-8859-1\n" +
                            "Content-Length: 19\n" +
                            "Server: Jetty(7.3.1.v20110307)\n" +
                            "\n" +
                            "This is the content";

        assertThat(response.toString(), is(equalTo(expectedResponse)));

    }


}
