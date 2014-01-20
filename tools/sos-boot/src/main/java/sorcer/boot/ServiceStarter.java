/**
 * Copyright 2013, 2014 Sorcersoft.com S.A.
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
package sorcer.boot;

import com.sun.jini.start.ServiceDescriptor;
import net.jini.config.Configuration;
import net.jini.config.ConfigurationException;
import net.jini.config.ConfigurationProvider;
import net.jini.config.EmptyConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.AbstractFileFilter;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.rioproject.impl.opstring.OpStringLoader;
import org.rioproject.opstring.OperationalString;
import org.rioproject.opstring.ServiceElement;
import org.rioproject.resolver.Artifact;
import org.rioproject.start.RioServiceDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import sorcer.core.DestroyAdmin;
import sorcer.core.SorcerConstants;
import sorcer.core.SorcerEnv;
import sorcer.resolver.Resolver;
import sorcer.util.IOUtils;
import sorcer.util.JavaSystemProperties;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static sorcer.com.sun.jini.start.ServiceStarter.Result;
import static sorcer.provider.boot.AbstractServiceDescriptor.Created;

/**
 * @author Rafał Krupiński
 */
public class ServiceStarter {
    final private static Logger log = LoggerFactory.getLogger(ServiceStarter.class);
	public static final String CONFIG_RIVER = "config/services.config";
    private static final String SUFFIX_RIVER = "config";
    final public static File SORCER_DEFAULT_CONFIG = new File(SorcerEnv.getHomeDir(), "configs/sorcer-boot.config");

    protected List<Result> services = new LinkedList<Result>();

    public static void main(String[] args) throws Exception {
        //redirect java.util.logging to slf4j/logback
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        System.setSecurityManager(new RMISecurityManager());
        ServiceStarter serviceStarter = new ServiceStarter();

        Runtime.getRuntime().addShutdownHook(serviceStarter.new ServiceStopper(Thread.currentThread(), "SORCER service destroyer"));

        serviceStarter.doMain(args);
	}

    class ServiceStopper extends Thread {
        private WeakReference<Thread> main;

        public ServiceStopper(Thread main, String name) {
            super(name);
            this.main = new WeakReference<Thread>(main);
        }

        @Override
        public void run() {
            Thread main = this.main.get();
            if (main != null) {
                log.debug("Interrupting {}",main);
                main.interrupt();
            }
            ArrayList<Result> services = new ArrayList<Result>(ServiceStarter.this.services);
            Collections.reverse(services);
            for (Result service : services) {
                if (service.result instanceof Created) {
                    Created created = (Created) service.result;
                    stop(created.impl);
                }else if (service.result instanceof RioServiceDescriptor.Created){
                    RioServiceDescriptor.Created created = (RioServiceDescriptor.Created) service.result;
                    stop(created.impl);
                } else {
                    log.warn("Don't know how to stop {}", service.result);
                }
            }
        }

        private void stop(Object impl) {
            if (impl instanceof DestroyAdmin) {
                DestroyAdmin da = (DestroyAdmin) impl;
                try {
                    log.debug("Stopping {}", da);
                    da.destroy();
                } catch (RemoteException e) {
                    log.warn("Error", e);
                }
            } else if (impl instanceof com.sun.jini.admin.DestroyAdmin) {
                com.sun.jini.admin.DestroyAdmin da = (com.sun.jini.admin.DestroyAdmin) impl;
                try {
                    log.info("Stopping {}", da);
                    da.destroy();
                } catch (RemoteException e) {
                    log.warn("Error", e);
                }
            } else {
                log.warn("Unable to stop {}", impl);
            }
        }
    }

	private void doMain(String[] args) throws Exception {
		loadDefaultProperties();
        List<String> configs = new LinkedList<String>(Arrays.asList(args));
		if (configs.isEmpty()) {
            configs.add(SORCER_DEFAULT_CONFIG.getPath());
		}
		start(configs);
	}

	private void loadDefaultProperties() {
		String sorcerHome = SorcerEnv.getHomeDir().getPath();
		setDefaultProperty(JavaSystemProperties.PROTOCOL_HANDLER_PKGS, "net.jini.url|sorcer.util.bdb|org.rioproject.url");
		setDefaultProperty(JavaSystemProperties.UTIL_LOGGING_CONFIG_FILE, sorcerHome + "/configs/sorcer.logging");
		setDefaultProperty(SorcerConstants.S_KEY_SORCER_ENV, sorcerHome + "/configs/sorcer.env");
	}

	private void setDefaultProperty(String key, String value) {
		String userValue = System.getProperty(key);
		if (userValue == null) {
			System.setProperty(key, value);
		}
	}

	/**
	 * Start services from the configs
	 *
	 * @param configs file path or URL of the services.config configuration
	 */
    public void start(Collection<String> configs) throws Exception {
        List<String> riverServices = new LinkedList<String>();
        List<File> cfgJars = new LinkedList<File>();
        List<File> opstrings = new LinkedList<File>();

        for (String path : configs) {
            File file = null;
            if (path.startsWith(":")) {
                file = findArtifact(path.substring(1));
            } else if (Artifact.isArtifact(path))
                file = new File(Resolver.resolveAbsolute(path));
            if (file == null) file = new File(path);

            IOUtils.ensureFile(file, IOUtils.FileCheck.readable);
            path = file.getPath();
            String ext = path.substring(path.lastIndexOf('.') + 1);

            if (file.isDirectory())
                riverServices.add(findConfigUrl(path).toExternalForm());
            else if (SUFFIX_RIVER.equals(ext))
                riverServices.add(path);
            else if ("oar".equals(ext) || "jar".equals(ext))
                cfgJars.add(file);
            else if ("opstring".equals(ext) || "groovy".equals(ext))
                opstrings.add(file);
            else
                throw new IllegalArgumentException("Unrecognized file " + path);
        }
        Map<Configuration, List<? extends ServiceDescriptor>> descs = new LinkedHashMap<Configuration, List<? extends ServiceDescriptor>>();
        descs.putAll(instantiateDescriptors(riverServices));

        List<OpstringServiceDescriptor> serviceDescriptors = createFromOpStrFiles(opstrings);
        serviceDescriptors.addAll(createFromOar(cfgJars));
        descs.put(EmptyConfiguration.INSTANCE, serviceDescriptors);

        sorcer.com.sun.jini.start.ServiceStarter.instantiateServices(descs, services);
    }

    private Map<Configuration, List<ServiceDescriptor>> instantiateDescriptors(List<String> riverServices) throws ConfigurationException {
        List<Configuration> configs = new ArrayList<Configuration>(riverServices.size());
        for (String s : riverServices) {
            configs.add(ConfigurationProvider.getInstance(new String[]{s}));
        }
        return sorcer.com.sun.jini.start.ServiceStarter.instantiateDescriptors(configs);
    }

    private File findArtifact(String artifactId) {
        Collection<File> files = FileUtils.listFiles(new File(System.getProperty("user.dir")), new ArtifactIdFileFilter(artifactId), DirectoryFileFilter.INSTANCE);
        if (files.size() == 0) {
            log.error("Artifact file {} not found", artifactId);
            return null;
        }
        if (files.size() > 1) {
            log.warn("Found {} files possibly matching artifactId, using the first", files.size());
        }
        return files.iterator().next();
    }

    protected List<OpstringServiceDescriptor> createFromOpStrFiles(Collection<File> files) throws Exception {
        List<OpstringServiceDescriptor> result = new LinkedList<OpstringServiceDescriptor>();
        String policyFile = System.getProperty(JavaSystemProperties.SECURITY_POLICY);
        URL policyFileUrl = new File(policyFile).toURI().toURL();
        OpStringLoader loader = new OpStringLoader();
        for (File opString : files) {
            OperationalString[] operationalStrings = loader.parseOperationalString(opString);
            result.addAll(createServiceDescriptors(operationalStrings, policyFileUrl));
        }
        return result;
    }

    private List<OpstringServiceDescriptor> createFromOar(Iterable<File> oarFiles) throws Exception {
        List<OpstringServiceDescriptor> result = new LinkedList<OpstringServiceDescriptor>();
        for (File oarFile : oarFiles) {
            SorcerOAR oar = new SorcerOAR(oarFile);
            OperationalString[] operationalStrings = oar.loadOperationalStrings();
            URL policyFile = oar.getPolicyFile();
            result.addAll(createServiceDescriptors(operationalStrings, policyFile));
        }
        return result;
    }

    private List<OpstringServiceDescriptor> createServiceDescriptors(OperationalString[] operationalStrings, URL policyFile) throws ConfigurationException {
        List<OpstringServiceDescriptor> descriptors = new LinkedList<OpstringServiceDescriptor>();
        for (OperationalString op : operationalStrings) {
            for (ServiceElement se : op.getServices()) {
                descriptors.add(new OpstringServiceDescriptor(se, policyFile));
            }

            descriptors.addAll(createServiceDescriptors(op.getNestedOperationalStrings(), policyFile));
        }
        return descriptors;
    }

    private URL findConfigUrl(String path) throws IOException {
        File configFile = new File(path);
        if (configFile.isDirectory()) {
            return new File(configFile, CONFIG_RIVER).toURI().toURL();
        } else if (path.endsWith(".jar")) {
            ZipEntry entry = new ZipFile(path).getEntry(CONFIG_RIVER);
            if (entry != null) {
                return new URL(String.format("jar:file:%1$s!/%2$s", path, CONFIG_RIVER));
            }
        }
        return new File(path).toURI().toURL();
    }

    private static class ArtifactIdFileFilter extends AbstractFileFilter {
        private String artifactId;

        public ArtifactIdFileFilter(String artifactId) {
            this.artifactId = artifactId;
        }

        @Override
        public boolean accept(File dir, String name) {
            String parent = dir.getName();
            String grandParent = dir.getParentFile().getName();
            return
                    name.startsWith(artifactId + "-") && name.endsWith(".jar") && (
                            //check development structure
                            "target".equals(parent)
                                    //check repository just in case
                                    || artifactId.equals(grandParent)
                    )
                            //check distribution structure
                            || "lib".equals(grandParent) && (artifactId + ".jar").equals(name)
                    ;
        }
    }
}
