/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.dm.test.annotation;

import org.apache.felix.dm.annotation.api.Service;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;

/**
 * Validate DependencyManager Factories declarared with annotations.
 */
@Service(factory=FactoryTest.Factory.class, factoryMethod="createFactoryTest")
public class FactoryTest
{
    String m_id;
    
    @ServiceDependency(filter="(test=factory)")
    Sequencer m_sequencer;
    
    public FactoryTest(String id)
    {
        m_id = id;
    }
    
    @Start
    void start() {
        if (! "factory".equals(m_id)) {
            throw new IllegalStateException();
        }
        m_sequencer.step(1);
    }
    
    public static class Factory
    {
        public FactoryTest createFactoryTest()
        {
            return new FactoryTest("factory");
        }
    }
}
