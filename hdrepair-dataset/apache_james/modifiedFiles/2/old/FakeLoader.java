/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.api.kernel.mock;

import java.util.HashMap;
import java.util.Map;

import org.apache.james.services.AbstractJSR250InstanceFactory;

public class FakeLoader extends AbstractJSR250InstanceFactory {

    private final Map<String, Object> servicesByName;
    private final Map<String, String> mappings = new HashMap<String, String>();
    public FakeLoader() {
        servicesByName = new HashMap<String, Object>();
        servicesByName.put("org.apache.james.LoaderService", this);
        
        mappings.put("James", "org.apache.james.services.MailServer");
        mappings.put("filesystem", "org.apache.james.services.FileSystem");
        mappings.put("dnsserver", "org.apache.james.api.dnsservice.DNSService");
        mappings.put("mailstore", "org.apache.avalon.cornerstone.services.store.Store");
        mappings.put("users-store", "org.apache.james.api.user.UsersStore");
        mappings.put("localusersrepository", "org.apache.james.api.user.UsersRepository");
        mappings.put("spoolrepository", "org.apache.james.services.SpoolRepository");
        mappings.put("domainlist", "org.apache.james.api.domainlist.DomainList");
        mappings.put("sockets", "org.apache.avalon.cornerstone.services.sockets.SocketManager");
        mappings.put("scheduler", "org.apache.avalon.cornerstone.services.scheduler.TimeScheduler");
        mappings.put("database-connections", "org.apache.avalon.cornerstone.services.datasources.DataSourceSelector");
        mappings.put("defaultvirtualusertable", "org.apache.james.api.vut.VirtualUserTable");
        mappings.put("virtualusertablemanagement", "org.apache.james.api.vut.management.VirtualUserTableManagement");

        mappings.put("spoolmanager", "org.apache.james.services.SpoolManager");
        mappings.put("matcherpackages", "org.apache.james.transport.MatcherLoader");
        mappings.put("mailetpackages", "org.apache.james.transport.MailetLoader");
        mappings.put("virtualusertable-store", "org.apache.james.api.vut.VirtualUserTableStore");
        mappings.put("imapserver", "org.org.apache.jsieve.mailet.Poster");
        mappings.put("threadmanager", "org.apache.avalon.cornerstone.services.threads.ThreadManager");
        mappings.put("spoolmanagement", "org.apache.james.management.SpoolManagementService");
        mappings.put("bayesiananalyzermanagement", "org.apache.james.management.BayesianAnalyzerManagementService");
        mappings.put("processormanagement", "org.apache.james.management.ProcessorManagementService");
        mappings.put("virtualusertablemanagementservice", "org.apache.james.api.vut.management.VirtualUserTableManagementService");
        mappings.put("domainlistmanagement", "org.apache.james.management.DomainListManagementService");
        mappings.put("nntp-repository", "org.apache.james.nntpserver.repository.NNTPRepository");
    }
    

    public Object get(String name) { 
        Object service = servicesByName.get(mapName(name));
        
        return service;
    }
    
    private String mapName(String name) {
        String newName = mappings.get(name);
        if(newName == null) {
            newName = name;
        }
        return newName;
    }
   

    public void put(String role, Object service) {
        servicesByName.put(role, service);
    }


	@Override
	public Object getObjectForName(String name) {
		return get(name);
	}
}
