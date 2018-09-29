/*
 * Copyright 2012 Shared Learning Collaborative, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.slc.sli.api.resources.generic.config;

import org.codehaus.jackson.map.ObjectMapper;
import org.slc.sli.api.resources.generic.util.ResourceHelper;
import org.slc.sli.api.resources.generic.util.ResourceTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads in different nameSpaced resource end points and loads them
 *
 * @author srupasinghe
 *
 */
@Component
public class ResourceEndPoint {

    private static final String BASE_RESOURCE = "org.slc.sli.api.resources.generic.DefaultResource";
    private static final String THREE_PART_RESOURCE = "org.slc.sli.api.resources.generic.ThreePartResource";
    private static final String FOUR_PART_RESOURCE = "org.slc.sli.api.resources.generic.FourPartResource";

    private Map<String, String> resourceEndPoints = new HashMap<String, String>();

    @Autowired
    private ResourceHelper resourceHelper;

    @PostConstruct
    public void load() throws IOException {
        loadNameSpace(getClass().getResourceAsStream("/wadl/v1_resources.json"));
    }

    protected ApiNameSpace loadNameSpace(InputStream fileStream) throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        ApiNameSpace apiNameSpace = mapper.readValue(fileStream, ApiNameSpace.class);
        String nameSpace = apiNameSpace.getNameSpace();

        List<ResourceEndPointTemplate> resources = apiNameSpace.getResources();
        for (ResourceEndPointTemplate resource : resources) {
            resourceEndPoints.putAll(buildEndPoints(nameSpace, "", resource));
        }

        return apiNameSpace;
    }

    protected Map<String, String> buildEndPoints(String nameSpace, String resourcePath, ResourceEndPointTemplate template) {
        Map<String, String> resources = new HashMap<String, String>();
        String fullPath =  nameSpace + resourcePath + template.getPath();

        resources.put(fullPath, getResourceClass("/rest/" + fullPath, template));

        List<ResourceEndPointTemplate> subResources = template.getSubResources();

        if (subResources != null) {
            for (ResourceEndPointTemplate subTemplate : subResources) {
                resources.putAll(buildEndPoints(nameSpace, resourcePath + template.getPath(), subTemplate));
            }
        }

        return resources;
    }

    protected String getResourceClass(final String resourcePath, ResourceEndPointTemplate template) {

        if (template.getResourceClass() != null) {
            return template.getResourceClass();
        }

        //use the brute force method for now, we should move to looking up this information from the class itself
        String resourceClass = bruteForceMatch(resourcePath);

        return resourceClass;
    }

    protected String bruteForceMatch(final String resourcePath) {

        if (resourceHelper.resolveResourcePath(resourcePath, ResourceTemplate.FOUR_PART)) {
            return FOUR_PART_RESOURCE;
        } else if (resourceHelper.resolveResourcePath(resourcePath, ResourceTemplate.THREE_PART)) {
            return THREE_PART_RESOURCE;
        }else if (resourceHelper.resolveResourcePath(resourcePath, ResourceTemplate.ONE_PART) ||
            resourceHelper.resolveResourcePath(resourcePath, ResourceTemplate.TWO_PART)) {
            return BASE_RESOURCE;
        }

        throw new RuntimeException("Cannot resolve resource handler class");

    }

    public Map<String, String> getResources() {
        return resourceEndPoints;
    }
}
