/*
 * Copyright 2012 JBoss Inc
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

package org.drools.examples.carinsurance.rule;

import org.drools.examples.carinsurance.domain.Car;
import org.drools.examples.carinsurance.domain.CarType;
import org.drools.examples.carinsurance.domain.Driver;
import org.drools.examples.carinsurance.domain.policy.CoverageType;
import org.drools.examples.carinsurance.domain.request.CoverageRequest;
import org.drools.examples.carinsurance.domain.request.PolicyRequest;
import org.drools.examples.carinsurance.workflow.SimulateTestBase;
import org.drools.fluent.simulation.SimulationFluent;
import org.drools.fluent.simulation.impl.DefaultSimulationFluent;
import org.joda.time.LocalDate;
import org.junit.Test;
import org.kie.builder.ReleaseId;
import org.kie.io.ResourceType;

import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;

public class PolicyApprovalRulesTest extends SimulateTestBase {

    @Test
    public void approvePolicyRequest() throws IOException {
        SimulationFluent simulationFluent = new DefaultSimulationFluent();

        Driver john = new Driver("John", "Smith", new LocalDate(1970, 1, 1));
        Car mini = new Car("MINI-01", CarType.SMALL, false, new BigDecimal("10000.00"));
        PolicyRequest johnMiniPolicyRequest = new PolicyRequest(john, mini);
        johnMiniPolicyRequest.addCoverageRequest(new CoverageRequest(CoverageType.COLLISION));
        johnMiniPolicyRequest.addCoverageRequest(new CoverageRequest(CoverageType.COMPREHENSIVE));

        String rules = readInputStreamReaderAsString( new InputStreamReader( getClass().getResourceAsStream( "policyRequestApprovalRules.drl" ) ) );
        ReleaseId releaseId = createKJarWithMultipleResources( "KBase1", new String[]{rules}, new ResourceType[] {ResourceType.DRL} );

        // @formatter:off          
        simulationFluent
        .newKieSession(releaseId, "KBase1.KSession1")
            .insert(john).set("john")
            .insert(mini).set("mini")
            .insert(johnMiniPolicyRequest).set("johnMiniPolicyRequest")
            .fireAllRules()
            .assertRuleFired("rejectMinors", 0)
            .test("johnMiniPolicyRequest.automaticallyRejected == false")
            .test("johnMiniPolicyRequest.rejectedMessageList.size() == 0")
            .end()
        .runSimulation();
        // @formatter:on
    }

    @Test
    public void rejectMinors() throws IOException {
        SimulationFluent simulationFluent = new DefaultSimulationFluent();

        Driver john = new Driver("John", "Smith", new LocalDate().minusYears(10));
        Car mini = new Car("MINI-01", CarType.SMALL, false, new BigDecimal("10000.00"));
        PolicyRequest johnMiniPolicyRequest = new PolicyRequest(john, mini);
        johnMiniPolicyRequest.addCoverageRequest(new CoverageRequest(CoverageType.COLLISION));
        johnMiniPolicyRequest.addCoverageRequest(new CoverageRequest(CoverageType.COMPREHENSIVE));

        String rules = readInputStreamReaderAsString( new InputStreamReader( getClass().getResourceAsStream( "policyRequestApprovalRules.drl" ) ) );
        ReleaseId releaseId = createKJarWithMultipleResources( "KBase1", new String[]{rules}, new ResourceType[] {ResourceType.DRL} );

        // @formatter:off
        simulationFluent
        .newKieSession(releaseId, "KBase1.KSession1")
            .insert(john).set("john")
            .insert(mini).set("mini")
            .insert(johnMiniPolicyRequest).set("johnMiniPolicyRequest")
            .fireAllRules()
            .assertRuleFired("rejectMinors", 1)
            .test("johnMiniPolicyRequest.automaticallyRejected == true")
            .test("johnMiniPolicyRequest.rejectedMessageList.size() == 1")
            .end()
        .runSimulation();
        // @formatter:on
    }

    @Test(expected = AssertionError.class)
    public void rejectMinorsFailingAssertion() throws IOException {
        SimulationFluent simulationFluent = new DefaultSimulationFluent();

        Driver john = new Driver("John", "Smith", new LocalDate().minusYears(10));
        Car mini = new Car("MINI-01", CarType.SMALL, false, new BigDecimal("10000.00"));
        PolicyRequest johnMiniPolicyRequest = new PolicyRequest(john, mini);
        johnMiniPolicyRequest.addCoverageRequest(new CoverageRequest(CoverageType.COLLISION));
        johnMiniPolicyRequest.addCoverageRequest(new CoverageRequest(CoverageType.COMPREHENSIVE));

        String rules = readInputStreamReaderAsString( new InputStreamReader( getClass().getResourceAsStream( "policyRequestApprovalRules.drl" ) ) );
        ReleaseId releaseId = createKJarWithMultipleResources( "KBase1", new String[]{rules}, new ResourceType[] {ResourceType.DRL} );

        // @formatter:off
        simulationFluent
        .newKieSession(releaseId, "KBase1.KSession1")
            .insert(john).set("john")
            .insert(mini).set("mini")
            .insert(johnMiniPolicyRequest).set("johnMiniPolicyRequest")
            .fireAllRules()
            .assertRuleFired("rejectMinors", 0)
            .test("johnMiniPolicyRequest.automaticallyRejected == false")
            .end()
        .runSimulation();
        // @formatter:on
    }

}
