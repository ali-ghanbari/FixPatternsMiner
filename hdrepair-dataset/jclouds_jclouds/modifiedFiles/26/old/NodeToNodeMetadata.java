/**
 * Licensed to jclouds, Inc. (jclouds) under one or more
 * contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  jclouds licenses this file
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
package org.jclouds.byon.functions;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.jclouds.byon.Node;
import org.jclouds.collect.Memoized;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.NodeMetadataBuilder;
import org.jclouds.compute.domain.NodeState;
import org.jclouds.compute.domain.OperatingSystem;
import org.jclouds.compute.domain.OsFamily;
import org.jclouds.compute.reference.ComputeServiceConstants;
import org.jclouds.domain.Credentials;
import org.jclouds.domain.Location;
import org.jclouds.logging.Logger;
import org.jclouds.util.Strings2;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

/**
 * @author Adrian Cole
 */
@Singleton
public class NodeToNodeMetadata implements Function<Node, NodeMetadata> {
   @Resource
   @Named(ComputeServiceConstants.COMPUTE_LOGGER)
   protected Logger logger = Logger.NULL;

   private final Supplier<Location> location;
   private final Supplier<Set<? extends Location>> locations;
   private final Map<String, Credentials> credentialStore;
   private final Function<URI, InputStream> slurp;

   @Inject
   NodeToNodeMetadata(Supplier<Location> location, @Memoized Supplier<Set<? extends Location>> locations,
            Function<URI, InputStream> slurp, Map<String, Credentials> credentialStore) {
      this.location = checkNotNull(location, "location");
      this.locations = checkNotNull(locations, "locations");
      this.credentialStore = checkNotNull(credentialStore, "credentialStore");
      this.slurp = checkNotNull(slurp, "slurp");
   }

   @Override
   public NodeMetadata apply(Node from) {
      NodeMetadataBuilder builder = new NodeMetadataBuilder();
      builder.ids(from.getId());
      builder.name(from.getName());
      builder.hostname(from.getHostname());
      builder.location(findLocationWithId(from.getLocationId()));
      builder.group(from.getGroup());
      builder.tags(from.getTags());
      builder.operatingSystem(OperatingSystem.builder().arch(from.getOsArch()).family(
               OsFamily.fromValue(from.getOsFamily())).description(from.getOsDescription())
               .version(from.getOsVersion()).build());
      builder.state(NodeState.RUNNING);
      builder.publicAddresses(ImmutableSet.<String> of(from.getHostname()));

      if (from.getUsername() != null) {
         Credentials creds = null;
         if (from.getCredentialUrl() != null) {
            try {
               creds = new Credentials(from.getUsername(), Strings2.toStringAndClose(slurp.apply(from
                        .getCredentialUrl())));
            } catch (IOException e) {
               logger.error(e, "URI could not be read: %s", from.getCredentialUrl());
            }
         } else if (from.getCredential() != null) {
            creds = new Credentials(from.getUsername(), from.getCredential());
         }
         if (creds != null)
            builder.credentials(creds);
         credentialStore.put("node#" + from.getId(), creds);
      }

      if (from.getSudoPassword() != null)
         builder.adminPassword(from.getSudoPassword());
      return builder.build();
   }

   private Location findLocationWithId(final String locationId) {
      if (locationId == null)
         return location.get();
      try {
         Location location = Iterables.find(locations.get(), new Predicate<Location>() {

            @Override
            public boolean apply(Location input) {
               return input.getId().equals(locationId);
            }

         });
         return location;

      } catch (NoSuchElementException e) {
         logger.debug("couldn't match instance location %s in: %s", locationId, locations.get());
         return location.get();
      }
   }
}
