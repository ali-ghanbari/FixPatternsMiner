/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.sts.cache;

import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.sts.IdentityMapper;
import org.apache.wss4j.common.principal.CustomTokenPrincipal;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;

/**
 * A simple in-memory HashMap based cache to cache identities in different realms where
 * the relationship is of type FederateIdentity.
 */
@ManagedResource()
public class MemoryIdentityCache implements IdentityCache, IdentityMapper {
    
    private static final Logger LOG = LogUtils.getL7dLogger(MemoryIdentityCache.class);
    
    private final Map<String, Map<String, String>> cache =
            Collections.synchronizedMap(new HashMap<String, Map<String, String>>());
    
    private long maxCacheItems = 10000L;
    
    private IdentityMapper identityMapper;
    
    private MemoryIdentityCacheStatistics statistics;
    

    protected MemoryIdentityCache() {
        
    }
    
    public MemoryIdentityCache(IdentityMapper identityMapper) {
        this.identityMapper = identityMapper;
    }
    
    public MemoryIdentityCacheStatistics getStatistics() {
        if (statistics == null) {
            this.statistics = new MemoryIdentityCacheStatistics();
        }
        return statistics;
    }

    public void setStatistics(MemoryIdentityCacheStatistics stats) {
        this.statistics = stats;
    }

    public long getMaxCacheItems() {
        return maxCacheItems;
    }

    public void setMaxCacheItems(long maxCacheItems) {
        this.maxCacheItems = maxCacheItems;
    }

    @Override
    public void add(String user, String realm, Map<String, String> identities) {
        if (cache.size() >= maxCacheItems) {
            cache.clear();
        }
        cache.put(user + "@" + realm, identities);
    }

    @ManagedOperation()
    @Override
    public Map<String, String> get(String user, String realm) {
        return cache.get(user + "@" + realm);
    }

    @Override
    public void remove(String user, String realm) {
        cache.remove(user + "@" + realm);       
    }
    
    @ManagedOperation()
    @Override
    public void clear() {
        cache.clear();  
    }
    
    @ManagedOperation()
    @Override
    public int size() {
        return cache.size();
    }
    
    @ManagedOperation()
    public String getContent() {
        return this.cache.toString();
    }

    @Override
    public Principal mapPrincipal(String sourceRealm,
            Principal sourcePrincipal, String targetRealm) {
        
        Principal targetPrincipal = null;
        Map<String, String> identities = this.get(sourcePrincipal.getName(), sourceRealm);
        if (identities != null) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("Identities found for '" + sourcePrincipal.getName() + "@" + sourceRealm + "'");
            }
            // Identities object found for key sourceUser@sourceRealm
            String targetUser = identities.get(targetRealm);
            if (targetUser == null) {
                getStatistics().increaseCacheMiss();
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("No mapping found for realm " + targetRealm + " of user '"
                             + sourcePrincipal.getName() + "@" + sourceRealm + "'");
                }
                // User identity of target realm not cached yet
                targetPrincipal = this.identityMapper.mapPrincipal(
                        sourceRealm, sourcePrincipal, targetRealm);
                // Add the identity for target realm to the cached entry 
                identities.put(targetRealm, targetPrincipal.getName());
                
                // Verify whether target user has cached some identities already
                Map<String, String> cachedItem = this.get(targetPrincipal.getName(), targetRealm);
                if (cachedItem != null) {
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.fine("Merging mappings for '" + sourcePrincipal.getName() + "@" + sourceRealm + "'");
                    }
                    //Identites already cached for targetUser@targetRealm key pair
                    //Merge into identities object
                    this.mergeMap(identities, cachedItem);
                }
                this.add(targetPrincipal.getName(), targetRealm, identities);
            } else {
                getStatistics().increaseCacheHit();
                if (LOG.isLoggable(Level.INFO)) {
                    LOG.info("Mapping '" + sourcePrincipal.getName() + "@" + sourceRealm + "' to '"
                             + targetUser + "@" + targetRealm + "' cached");
                }
                targetPrincipal = new CustomTokenPrincipal(targetUser);
            }
            
        } else {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("No mapping found for realm " + targetRealm + " of user '"
                        + sourcePrincipal.getName() + "@" + sourceRealm + "'");
            }
            getStatistics().increaseCacheMiss();
            
            // Identities object NOT found for key sourceUser@sourceRealm
            targetPrincipal = this.identityMapper.mapPrincipal(
                    sourceRealm, sourcePrincipal, targetRealm);
            identities = new HashMap<String, String>();
            identities.put(sourceRealm, sourcePrincipal.getName());
            identities.put(targetRealm, targetPrincipal.getName());
            this.add(targetPrincipal.getName(), targetRealm, identities);
            this.add(sourcePrincipal.getName(), sourceRealm, identities);
        }
        System.out.println("Cache content: " + this.cache.toString());
        return targetPrincipal;
    }
    
    
    
    private void mergeMap(Map<String, String> to, Map<String, String> from) {
        for (String key : from.keySet()) {
            to.put(key, from.get(key));
        }
        for (String key : to.keySet()) {
            from.put(key, to.get(key));
        }
    }
}

