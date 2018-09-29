package org.kie.server.services.impl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.drools.compiler.kie.builder.impl.InternalKieContainer;
import org.drools.compiler.kie.builder.impl.InternalKieScanner;
import org.drools.core.command.impl.GenericCommand;
import org.drools.core.command.runtime.BatchExecutionCommandImpl;
import org.kie.api.KieServices;
import org.kie.api.builder.Message.Level;
import org.kie.api.builder.Results;
import org.kie.api.command.Command;
import org.kie.api.runtime.ExecutionResults;
import org.kie.api.runtime.KieSession;
import org.kie.server.api.KieServerEnvironment;
import org.kie.server.api.Version;
import org.kie.server.api.commands.CallContainerCommand;
import org.kie.server.api.commands.CommandScript;
import org.kie.server.api.commands.CreateContainerCommand;
import org.kie.server.api.commands.DisposeContainerCommand;
import org.kie.server.api.commands.ListContainersCommand;
import org.kie.server.api.model.KieContainerResource;
import org.kie.server.api.model.KieContainerResourceList;
import org.kie.server.api.model.KieContainerStatus;
import org.kie.server.api.model.KieScannerResource;
import org.kie.server.api.model.KieScannerStatus;
import org.kie.server.api.model.KieServerCommand;
import org.kie.server.api.model.KieServerInfo;
import org.kie.server.api.model.ReleaseId;
import org.kie.server.api.model.ServiceResponse;
import org.kie.server.api.model.ServiceResponse.ResponseType;
import org.kie.server.services.rest.KieServerRestImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uberfire.io.IOService;
import org.uberfire.io.impl.IOServiceNio2WrapperImpl;
import org.uberfire.java.nio.IOException;
import org.uberfire.java.nio.file.DirectoryStream;
import org.uberfire.java.nio.file.DirectoryStream.Filter;
import org.uberfire.java.nio.file.FileSystem;
import org.uberfire.java.nio.file.Files;
import org.uberfire.java.nio.file.Path;

import com.thoughtworks.xstream.XStream;

public class KieServerImpl {

    private static final String             CONTAINER_STATE_FILE = "container.xml";
    private static final Pattern            LOOKUP               = Pattern.compile("[\"']?lookup[\"']?\\s*[:=]\\s*[\"']([^\"']+)[\"']");
    private static final Logger             logger               = LoggerFactory.getLogger(KieServerRestImpl.class);

    private final KieContainersRegistryImpl context;
    private IOService                       ios                  = null;
    private FileSystem                      fs                   = null;

    public KieServerImpl() {
        //ios = initializeIOService();
        //fs = initializeSystemFS(ios);
        this.context = new KieContainersRegistryImpl();
        //restoreContainers();
    }

    private IOService initializeIOService() {
        IOService ios = new IOServiceNio2WrapperImpl();
        return ios;
    }

    private FileSystem initializeSystemFS(IOService ios) {
        URI uri = URI.create("git://system-repo");
        FileSystem fs = null;
        try {
            fs = ios.getFileSystem(uri);
            if (fs == null) {
                // new filesystem. create.
                fs = ios.newFileSystem(uri, new HashMap<String, Object>() {

                    {
                        put("init", true);
                    }
                });
            }
        } catch (Exception e) {
            // new filesystem. create.
            fs = ios.newFileSystem(uri, new HashMap<String, Object>() {

                {
                    put("init", true);
                }
            });
        }
        return fs;
    }

    public List<ServiceResponse<? extends Object>> executeScript(CommandScript commands) {
        List<ServiceResponse<? extends Object>> response = new ArrayList<ServiceResponse<? extends Object>>();
        if( commands != null ) {
            for (KieServerCommand command : commands.getCommands()) {
                if (command instanceof CreateContainerCommand) {
                    response.add(createContainer(((CreateContainerCommand) command).getContainer().getContainerId(), ((CreateContainerCommand) command).getContainer()));
                } else if (command instanceof ListContainersCommand) {
                    response.add(listContainers());
                } else if (command instanceof CallContainerCommand) {
                    response.add(callContainer(((CallContainerCommand) command).getContainerId(), ((CallContainerCommand) command).getPayload()));
                } else if (command instanceof DisposeContainerCommand) {
                    response.add(disposeContainer(((DisposeContainerCommand) command).getContainerId()));
                }
            }
        }
        return response;
    }

    public ServiceResponse<KieServerInfo> getInfo() {
        try {
            Version version = KieServerEnvironment.getVersion();
            String versionStr = version != null ? version.toString() : "Unknown-Version";
            return new ServiceResponse<KieServerInfo>(ServiceResponse.ResponseType.SUCCESS, "Kie Server info", new KieServerInfo(versionStr));
        } catch (Exception e) {
            logger.error("Error retrieving server info:", e);
            return new ServiceResponse<KieServerInfo>(ServiceResponse.ResponseType.FAILURE, "Error retrieving kie server info: " +
                    e.getClass().getName() + ": " + e.getMessage());
        }
    }

    public ServiceResponse<KieContainerResource> createContainer(String containerId, KieContainerResource container) {
        if (container == null || container.getReleaseId() == null) {
            logger.error("Error creating container. Release Id is null: " + container);
            return new ServiceResponse<KieContainerResource>(ServiceResponse.ResponseType.FAILURE, "Failed to create container " + containerId + ". Release Id is null: " + container + ".");
        }
        ReleaseId releaseId = container.getReleaseId();
        try {
            KieContainerInstance ci = new KieContainerInstance(containerId, KieContainerStatus.CREATING);
            KieContainerInstance previous = null;
            // have to synchronize on the ci or a concurrent call to dispose may create inconsistencies
            synchronized (ci) {
                previous = context.addIfDoesntExist(containerId, ci);
                if (previous == null) {
                    try {
                        KieServices ks = KieServices.Factory.get();
                        InternalKieContainer kieContainer = (InternalKieContainer) ks.newKieContainer(releaseId);
                        if (kieContainer != null) {
                            ci.setKieContainer(kieContainer);
                            ci.getResource().setStatus(KieContainerStatus.STARTED);
                            return new ServiceResponse<KieContainerResource>(ServiceResponse.ResponseType.SUCCESS, "Container " + containerId + " successfully deployed with module " + releaseId + ".", ci.getResource());
                        } else {
                            ci.getResource().setStatus(KieContainerStatus.FAILED);
                            return new ServiceResponse<KieContainerResource>(ServiceResponse.ResponseType.FAILURE, "Failed to create container " + containerId + " with module " + releaseId + ".");
                        }
                    } catch (Exception e) {
                        logger.error("Error creating container '" + containerId + "' for module '" + releaseId + "'", e);
                        ci.getResource().setStatus(KieContainerStatus.FAILED);
                        return new ServiceResponse<KieContainerResource>(ServiceResponse.ResponseType.FAILURE, "Failed to create container " + containerId + " with module " + releaseId + ": " + e.getClass().getName() + ": " + e.getMessage());
                    } finally {
                        persistContainer(ci);
                    }
                } else {
                    return new ServiceResponse<KieContainerResource>(ServiceResponse.ResponseType.FAILURE, "Container " + containerId + " already exists.", previous.getResource());
                }
            }
        } catch (Exception e) {
            logger.error("Error creating container '" + containerId + "' for module '" + releaseId + "'", e);
            return new ServiceResponse<KieContainerResource>(ServiceResponse.ResponseType.FAILURE, "Error creating container " + containerId +
                    " with module " + releaseId + ": " + e.getClass().getName() + ": " + e.getMessage());
        }
    }

    public ServiceResponse<KieContainerResourceList> listContainers() {
        try {
            List<KieContainerResource> containers = new ArrayList<KieContainerResource>();
            for (KieContainerInstance instance : context.getContainers()) {
                containers.add(instance.getResource());
            }
            KieContainerResourceList cil = new KieContainerResourceList(containers);
            return new ServiceResponse<KieContainerResourceList>(ServiceResponse.ResponseType.SUCCESS, "List of created containers", cil);
        } catch (Exception e) {
            logger.error("Error retrieving list of containers", e);
            return new ServiceResponse<KieContainerResourceList>(ServiceResponse.ResponseType.FAILURE, "Error listing containers: " +
                    e.getClass().getName() + ": " + e.getMessage());
        }
    }

    public ServiceResponse<KieContainerResource> getContainerInfo(String id) {
        try {
            KieContainerInstance ci = context.getContainer(id);
            if (ci != null) {
                return new ServiceResponse<KieContainerResource>(ServiceResponse.ResponseType.SUCCESS, "Info for container " + id, ci.getResource());
            }
            return new ServiceResponse<KieContainerResource>(ServiceResponse.ResponseType.FAILURE, "Container " + id + " is not instantiated.");
        } catch (Exception e) {
            logger.error("Error retrieving info for container '" + id + "'", e);
            return new ServiceResponse<KieContainerResource>(ServiceResponse.ResponseType.FAILURE, "Error retrieving container info: " +
                    e.getClass().getName() + ": " + e.getMessage());
        }
    }

    public ServiceResponse<String> callContainer(String containerId, String payload) {
        if( payload == null ) {
            return new ServiceResponse<String>(ServiceResponse.ResponseType.FAILURE, "Error calling container " + containerId + ". Empty payload. ");
        }
        try {
            KieContainerInstance kci = (KieContainerInstance) context.getContainer(containerId);
            // the following code is subject to a concurrent call to dispose(), but the cost of synchronizing it
            // would likely not be worth it. At this point a decision was made to fail the execution if a concurrent 
            // call do dispose() is executed.
            if (kci != null && kci.getKieContainer() != null) {
                String sessionId = null;
                // this is a weak way of finding the lookup, but it is the same used in kie-camel. Will keep it for now. 
                Matcher m = LOOKUP.matcher(payload);
                if (m.find()) {
                    sessionId = m.group(1);
                }

                KieSession ks = null;
                if (sessionId != null) {
                    ks = kci.getKieContainer().getKieSession(sessionId);
                } else {
                    ks = kci.getKieContainer().getKieSession();
                }
                if (ks != null) {
                    ClassLoader moduleClassLoader = kci.getKieContainer().getClassLoader();
                    XStream xs = XStreamXml.newXStreamMarshaller(moduleClassLoader);
                    Command<?> cmd = (Command<?>) xs.fromXML(payload);

                    if (cmd == null) {
                        return new ServiceResponse<String>(ServiceResponse.ResponseType.FAILURE, "Body of in message not of the expected type '" + Command.class.getName() + "'");
                    }
                    if (!(cmd instanceof BatchExecutionCommandImpl)) {
                        cmd = new BatchExecutionCommandImpl(Arrays.asList(new GenericCommand<?>[]{(GenericCommand<?>) cmd}));
                    }

                    ExecutionResults results = ks.execute((BatchExecutionCommandImpl) cmd);
                    String result = xs.toXML(results);
                    return new ServiceResponse<String>(ServiceResponse.ResponseType.SUCCESS, "Container " + containerId + " successfully called.", result);
                } else {
                    return new ServiceResponse<String>(ServiceResponse.ResponseType.FAILURE, "Session '" + sessionId + "' not found on container '" + containerId + "'.");
                }
            } else {
                return new ServiceResponse<String>(ServiceResponse.ResponseType.FAILURE, "Container " + containerId + " is not instantiated.");
            }
        } catch (Exception e) {
            logger.error("Error calling container '" + containerId + "'", e);
            return new ServiceResponse<String>(ServiceResponse.ResponseType.FAILURE, "Error calling container " + containerId + ": " +
                    e.getClass().getName() + ": " + e.getMessage());
        }
    }

    public ServiceResponse<Void> disposeContainer(String containerId) {
        try {
            KieContainerInstance kci = (KieContainerInstance) context.removeContainer(containerId);
            if (kci != null) {
                synchronized (kci) {
                    kci.setStatus(KieContainerStatus.DISPOSING); // just in case
                    if (kci.getKieContainer() != null) {
                        InternalKieContainer kieContainer = kci.getKieContainer();
                        kci.setKieContainer(null); // helps reduce concurrent access issues
                        try {
                            // this may fail, but we already removed the container from the registry
                            kieContainer.dispose();
                        } catch (Exception e) {
                            logger.warn("Container '" + containerId + "' disposed, but an unnexpected exception was raised", e);
                            return new ServiceResponse<Void>(ServiceResponse.ResponseType.SUCCESS, "Container " + containerId +
                                    " disposed, but exception was raised: " + e.getClass().getName() + ": " + e.getMessage());
                        }
                        return new ServiceResponse<Void>(ServiceResponse.ResponseType.SUCCESS, "Container " + containerId + " successfully disposed.");
                    } else {
                        return new ServiceResponse<Void>(ServiceResponse.ResponseType.SUCCESS, "Container " + containerId + " was not instantiated.");
                    }
                }
            } else {
                return new ServiceResponse<Void>(ServiceResponse.ResponseType.SUCCESS, "Container " + containerId + " was not instantiated.");
            }
        } catch (Exception e) {
            logger.error("Error disposing Container '" + containerId + "'", e);
            return new ServiceResponse<Void>(ServiceResponse.ResponseType.FAILURE, "Error disposing container " + containerId + ": " +
                    e.getClass().getName() + ": " + e.getMessage());
        }
    }

    public ServiceResponse<KieScannerResource> getScannerInfo(String id) {
        try {
            KieContainerInstance kci = (KieContainerInstance) context.getContainer(id);
            if (kci != null && kci.getKieContainer() != null) {
                InternalKieScanner scanner = kci.getScanner();
                KieScannerResource info = null;
                if (scanner != null) {
                    info = new KieScannerResource(mapStatus(scanner.getStatus()));
                } else {
                    info = new KieScannerResource(KieScannerStatus.DISPOSED);
                }
                return new ServiceResponse<KieScannerResource>(ServiceResponse.ResponseType.SUCCESS, "Scanner info successfully retrieved", info);
            } else {
                return new ServiceResponse<KieScannerResource>(ServiceResponse.ResponseType.FAILURE,
                        "Unknown container " + id + ".");
            }
        } catch (Exception e) {
            logger.error("Error retrieving scanner info for container '" + id + "'.", e);
            return new ServiceResponse<KieScannerResource>(ServiceResponse.ResponseType.FAILURE, "Error retrieving scanner info for container '" + id + "': " +
                    e.getClass().getName() + ": " + e.getMessage());
        }
    }

    public ServiceResponse<KieScannerResource> updateScanner(String id, KieScannerResource resource) {
        if (resource == null || resource.getStatus() == null) {
            logger.error("Error updating scanner for container " + id + ". Status is null: " + resource);
            return new ServiceResponse<KieScannerResource>(ServiceResponse.ResponseType.FAILURE, "Error updating scanner for container " + id + ". Status is null: " + resource);
        }
        KieScannerStatus status = resource.getStatus();
        try {
            KieContainerInstance kci = (KieContainerInstance) context.getContainer(id);
            if (kci != null && kci.getKieContainer() != null) {
                switch (status) {
                    case CREATED:
                        // create the scanner
                        return createScanner(id, kci);
                    case STARTED:
                        // start the scanner
                        return startScanner(id, resource, kci);
                    case STOPPED:
                        // stop the scanner
                        return stopScanner(id, resource, kci);
                    case SCANNING:
                        // scan now
                        return scanNow(id, resource, kci);
                    case DISPOSED:
                        // dispose
                        return disposeScanner(id, resource, kci);
                    default:
                        // error
                        return new ServiceResponse<KieScannerResource>(ServiceResponse.ResponseType.FAILURE,
                                "Unknown status '" + status + "' for scanner on container " + id + ".");
                }
            } else {
                return new ServiceResponse<KieScannerResource>(ServiceResponse.ResponseType.FAILURE,
                        "Unknown container " + id + ".");
            }
        } catch (Exception e) {
            logger.error("Error updating scanner for container '" + id + "': " + resource, e);
            return new ServiceResponse<KieScannerResource>(ServiceResponse.ResponseType.FAILURE, "Error updating scanner for container '" + id +
                    "': " + resource + ": " + e.getClass().getName() + ": " + e.getMessage());
        }
    }

    private ServiceResponse<KieScannerResource> startScanner(String id, KieScannerResource resource, KieContainerInstance kci) {
        if (kci.getScanner() == null) {
            ServiceResponse<KieScannerResource> response = createScanner(id, kci);
            if (ResponseType.FAILURE.equals(response.getType())) {
                return response;
            }
        }
        if (KieScannerStatus.STOPPED.equals(mapStatus(kci.getScanner().getStatus())) &&
                resource.getPollInterval() != null) {
            kci.getScanner().start(resource.getPollInterval());
            return new ServiceResponse<KieScannerResource>(ServiceResponse.ResponseType.SUCCESS,
                    "Kie scanner successfuly created.",
                    new KieScannerResource(mapStatus(kci.getScanner().getStatus())));
        } else if (!KieScannerStatus.STOPPED.equals(mapStatus(kci.getScanner().getStatus()))) {
            return new ServiceResponse<KieScannerResource>(ServiceResponse.ResponseType.FAILURE,
                    "Invalid kie scanner status: " + mapStatus(kci.getScanner().getStatus()),
                    new KieScannerResource(mapStatus(kci.getScanner().getStatus())));
        } else if (resource.getPollInterval() == null) {
            return new ServiceResponse<KieScannerResource>(ServiceResponse.ResponseType.FAILURE,
                    "Invalid polling interval: " + resource.getPollInterval(),
                    new KieScannerResource(mapStatus(kci.getScanner().getStatus())));
        }
        return new ServiceResponse<KieScannerResource>(ServiceResponse.ResponseType.FAILURE,
                "Unknown error starting scanner. Scanner was not started." + resource,
                new KieScannerResource(mapStatus(kci.getScanner().getStatus())));
    }

    private ServiceResponse<KieScannerResource> stopScanner(String id, KieScannerResource resource, KieContainerInstance kci) {
        if (kci.getScanner() == null) {
            return new ServiceResponse<KieScannerResource>(ServiceResponse.ResponseType.FAILURE,
                    "Invalid call. Scanner is not instantiated. ",
                    new KieScannerResource(KieScannerStatus.DISPOSED));
        }
        if (KieScannerStatus.STARTED.equals(mapStatus(kci.getScanner().getStatus())) ||
                KieScannerStatus.SCANNING.equals(mapStatus(kci.getScanner().getStatus()))) {
            kci.getScanner().stop();
            return new ServiceResponse<KieScannerResource>(ServiceResponse.ResponseType.SUCCESS,
                    "Kie scanner successfuly stopped.",
                    new KieScannerResource(mapStatus(kci.getScanner().getStatus())));
        } else {
            return new ServiceResponse<KieScannerResource>(ServiceResponse.ResponseType.FAILURE,
                    "Invalid kie scanner status: " + mapStatus(kci.getScanner().getStatus()),
                    new KieScannerResource(mapStatus(kci.getScanner().getStatus())));
        }
    }

    private ServiceResponse<KieScannerResource> scanNow(String id, KieScannerResource resource, KieContainerInstance kci) {
        if (kci.getScanner() == null) {
            return new ServiceResponse<KieScannerResource>(ServiceResponse.ResponseType.FAILURE,
                    "Invalid call. Scanner is not instantiated. ",
                    new KieScannerResource(KieScannerStatus.DISPOSED));
        }
        if (KieScannerStatus.STOPPED.equals(mapStatus(kci.getScanner().getStatus()))) {
            kci.getScanner().scanNow();
            return new ServiceResponse<KieScannerResource>(ServiceResponse.ResponseType.SUCCESS,
                    "Scan successfully executed.",
                    new KieScannerResource(mapStatus(kci.getScanner().getStatus())));
        } else {
            return new ServiceResponse<KieScannerResource>(ServiceResponse.ResponseType.FAILURE,
                    "Invalid kie scanner status: " + mapStatus(kci.getScanner().getStatus()),
                    new KieScannerResource(mapStatus(kci.getScanner().getStatus())));
        }
    }

    private ServiceResponse<KieScannerResource> disposeScanner(String id, KieScannerResource resource, KieContainerInstance kci) {
        if (kci.getScanner() == null) {
            return new ServiceResponse<KieScannerResource>(ServiceResponse.ResponseType.SUCCESS,
                    "Invalid call. Scanner already disposed. ",
                    new KieScannerResource(KieScannerStatus.DISPOSED));
        }
        if (KieScannerStatus.STARTED.equals(mapStatus(kci.getScanner().getStatus())) ||
                KieScannerStatus.SCANNING.equals(mapStatus(kci.getScanner().getStatus()))) {
            ServiceResponse<KieScannerResource> response = stopScanner(id, resource, kci);
            if (ResponseType.FAILURE.equals(response.getType())) {
                return response;
            }
        }
        kci.getScanner().shutdown();
        kci.setScanner(null);
        return new ServiceResponse<KieScannerResource>(ServiceResponse.ResponseType.SUCCESS,
                "Kie scanner successfuly shutdown.",
                new KieScannerResource(KieScannerStatus.DISPOSED));
    }

    private ServiceResponse<KieScannerResource> createScanner(String id, KieContainerInstance kci) {
        if (kci.getScanner() == null) {
            InternalKieScanner scanner = (InternalKieScanner) KieServices.Factory.get().newKieScanner(kci.getKieContainer());
            kci.setScanner(scanner);
            return new ServiceResponse<KieScannerResource>(ServiceResponse.ResponseType.SUCCESS,
                    "Kie scanner successfuly created.",
                    new KieScannerResource(mapStatus(scanner.getStatus())));
        } else {
            return new ServiceResponse<KieScannerResource>(ServiceResponse.ResponseType.FAILURE,
                    "Error creating the scanner for container " + id + ". Scanner already exists.");

        }
    }

    public ServiceResponse<ReleaseId> getContainerReleaseId(String id) {
        try {
            KieContainerInstance ci = context.getContainer(id);
            if (ci != null) {
                return new ServiceResponse<ReleaseId>(ServiceResponse.ResponseType.SUCCESS, "ReleaseId for container " + id, ci.getResource().getReleaseId());
            }
            return new ServiceResponse<ReleaseId>(ServiceResponse.ResponseType.FAILURE, "Container " + id + " is not instantiated.");
        } catch (Exception e) {
            logger.error("Error retrieving releaseId for container '" + id + "'", e);
            return new ServiceResponse<ReleaseId>(ServiceResponse.ResponseType.FAILURE, "Error retrieving container releaseId: " +
                    e.getClass().getName() + ": " + e.getMessage());
        }
    }

    public ServiceResponse<ReleaseId> updateContainerReleaseId(String id, ReleaseId releaseId) {
        if( releaseId == null ) {
            logger.error("Error updating releaseId for container '" + id + "'. ReleaseId is null.");
            return new ServiceResponse<ReleaseId>(ServiceResponse.ResponseType.FAILURE, "Error updating releaseId for container " + id + ". ReleaseId is null. ");
        }
        try {
            KieContainerInstance kci = (KieContainerInstance) context.getContainer(id);
            // the following code is subject to a concurrent call to dispose(), but the cost of synchronizing it
            // would likely not be worth it. At this point a decision was made to fail the execution if a concurrent 
            // call do dispose() is executed.
            if (kci != null && kci.getKieContainer() != null) {
                Results results = kci.getKieContainer().updateToVersion(releaseId);
                if (results.hasMessages(Level.ERROR)) {
                    logger.error("Error updating releaseId for container " + id + " to version " + releaseId + "\nMessages: " + results.getMessages());
                    return new ServiceResponse<ReleaseId>(ServiceResponse.ResponseType.FAILURE, "Error updating release id on container " + id + " to " + releaseId, kci.getResource().getReleaseId());
                } else {
                    return new ServiceResponse<ReleaseId>(ServiceResponse.ResponseType.SUCCESS, "Release id successfuly updated.", kci.getResource().getReleaseId());
                }
            } else {
                return new ServiceResponse<ReleaseId>(ServiceResponse.ResponseType.FAILURE, "Container " + id + " is not instantiated.");
            }
        } catch (Exception e) {
            logger.error("Error updating releaseId for container '" + id + "'", e);
            return new ServiceResponse<ReleaseId>(ServiceResponse.ResponseType.FAILURE, "Error updating releaseId for container " + id + ": " +
                    e.getClass().getName() + ": " + e.getMessage());
        }
    }

    private KieScannerStatus mapStatus(InternalKieScanner.Status status) {
        switch (status) {
            case STARTING:
                return KieScannerStatus.CREATED;
            case RUNNING:
                return KieScannerStatus.STARTED;
            case SCANNING:
            case UPDATING:
                return KieScannerStatus.SCANNING;
            case STOPPED:
                return KieScannerStatus.STOPPED;
            case SHUTDOWN:
                return KieScannerStatus.DISPOSED;
            default:
                return KieScannerStatus.UNKNOWN;
        }
    }

    public static class KieContainersRegistryImpl implements KieContainersRegistry {

        private final ConcurrentMap<String, KieContainerInstance> containers;

        public KieContainersRegistryImpl() {
            this.containers = new ConcurrentHashMap<String, KieContainerInstance>();
        }

        public KieContainerInstance addIfDoesntExist(String containerId, KieContainerInstance ci) {
            synchronized ( containers ) {
                KieContainerInstance kci = containers.putIfAbsent(containerId, ci);
                if( kci != null && kci.getStatus() == KieContainerStatus.FAILED ) {
                    // if previous container filed, allow override
                    containers.put(containerId, ci);
                    return null;
                }
                return kci;
            }
        }

        public List<KieContainerInstance> getContainers() {
            // instantiating a new array list to prevent iteration problems when concurrently changing the map 
            return new ArrayList<KieContainerInstance>(containers.values());
        }

        public KieContainerInstance getContainer(String containerId) {
            return containers.get(containerId);
        }

        public KieContainerInstance removeContainer(String containerId) {
            return containers.remove(containerId);
        }
    }

    private void restoreContainers() {
        DirectoryStream<Path> ds = null;
        try {
            Path containersPath = fs.getPath("/containers");
            if (ios.exists(containersPath)) {
                ds = ios.newDirectoryStream(containersPath, new Filter<Path>() {

                    @Override
                    public boolean accept(Path entry) throws IOException {
                        return Files.isDirectory(entry);
                    }
                });
                if (ds != null) {
                    XStream xs = XStreamXml.newXStreamMarshaller(KieServerImpl.class.getClassLoader());
                    for (Path entry : ds) {
                        BufferedReader reader = null;
                        try {
                            logger.info("Restoring state of kie container '" + entry.getFileName() + "'");
                            reader = Files.newBufferedReader(entry.resolve(CONTAINER_STATE_FILE), Charset.forName("UTF-8"));
                            KieContainerResource resource = (KieContainerResource) xs.fromXML(reader);
                            restore(resource);
                        } catch (Exception e) {
                            logger.error("Error restoring kie container state", e);
                        } finally {
                            if (reader != null) {
                                try {
                                    reader.close();
                                } catch (java.io.IOException e) {
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error restoring kie server state", e);
        } finally {
            if (ds != null)
                ds.close();
        }
    }

    private void persistContainer(KieContainerInstance ci) {
        if( fs != null ) {
            BufferedWriter writer = null;
            try {
                logger.info("Persisting state for kie container '" + ci.getContainerId() + "'");
                XStream xs = XStreamXml.newXStreamMarshaller(KieServerImpl.class.getClassLoader());
                Path file = fs.getPath("/containers/" + ci.getContainerId() + "/" + CONTAINER_STATE_FILE);
                writer = Files.newBufferedWriter(file, Charset.forName("UTF-8"));
                xs.toXML(ci.getResource(), writer);
            } catch (Exception e) {
                logger.error("Error persisting state for kie container '" + ci.getContainerId() + "'", e);
            } finally {
                if (writer != null) {
                    try {
                        writer.close();
                    } catch (java.io.IOException e) {
                    }
                }
            }
        }
    }

    private void restore(KieContainerResource resource) {
        if( fs != null ) {
            System.out.println(">>>>>>>>> RESTORING STATE FOR = " + resource);
        }
    }

}
