/*
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jayway.restassured.itest.java;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.builder.RequestSpecBuilder;
import com.jayway.restassured.config.RestAssuredConfig;
import com.jayway.restassured.filter.Filter;
import com.jayway.restassured.filter.FilterContext;
import com.jayway.restassured.internal.filter.FormAuthFilter;
import com.jayway.restassured.itest.java.support.SpookyGreetJsonResponseFilter;
import com.jayway.restassured.itest.java.support.WithJetty;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.specification.FilterableRequestSpecification;
import com.jayway.restassured.specification.FilterableResponseSpecification;
import com.jayway.restassured.specification.RequestSpecification;

import org.apache.commons.io.output.WriterOutputStream;
import org.junit.Test;

import java.io.PrintStream;
import java.io.StringWriter;

import static com.jayway.restassured.RestAssured.expect;
import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.filter.log.ErrorLoggingFilter.logErrorsTo;
import static com.jayway.restassured.filter.log.ResponseLoggingFilter.logResponseTo;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class FilterITest extends WithJetty {

    @Test
    public void filterWorks() throws Exception {
        final FormAuthFilter filter = new FormAuthFilter();
        filter.setUserName("John");
        filter.setPassword("Doe");

        given().
                filter(filter).
        expect().
                statusCode(200).
                body(equalTo("OK")).
        when().
                get("/formAuth");
    }

    @Test
    public void supportsSpecifyingDefaultFilters() throws Exception {
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);
        RestAssured.filters(asList(logErrorsTo(captor), logResponseTo(captor)));
        try {
            expect().body(equalTo("ERROR")).when().get("/409");
        }  finally {
            RestAssured.reset();
        }
        String lineSeparator = System.getProperty("line.separator");
        assertThat(writer.toString(), is("HTTP/1.1 409 Conflict\nContent-Type=text/plain; charset=utf-8\nContent-Length=5\nServer=Jetty(6.1.14)\n\nERROR" + lineSeparator + "HTTP/1.1 409 Conflict\nContent-Type=text/plain; charset=utf-8\nContent-Length=5\nServer=Jetty(6.1.14)\n\nERROR" + lineSeparator));
    }

    @Test
    public void filtersCanAlterResponseBeforeValidation() throws Exception {
       given().
               filter(new SpookyGreetJsonResponseFilter()).
               queryParam("firstName", "John").
               queryParam("lastName", "Doe").
       expect().
                body("greeting.firstName", equalTo("Spooky")).
                body("greeting.lastName", equalTo("Doe")).
       when().
                get("/greetJSON");
    }
    
    /** 
    Regression Test for 197
    */
    @Test
    public void testDefaultfiltersDontAccumluate() {
           CountingFilter myFilter = new CountingFilter();
           RestAssured.config = RestAssuredConfig.newConfig();
           RestAssured.filters(myFilter);

           RequestSpecification spec = new RequestSpecBuilder().build();

           given().get("/greetJSON");
           assertThat (myFilter.counter, equalTo(1)); 

           given().spec(spec).get("/greetJSON");
           assertThat (myFilter.counter, equalTo(2));
    }

       public static class CountingFilter implements Filter {

		public int counter = 0;
           
           public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
               counter++;
               return ctx.next (requestSpec, responseSpec);
           }       
           
            
       }
    
}
