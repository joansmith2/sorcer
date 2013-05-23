/**
 *
 * Copyright 2013 Rafał Krupiński.
 * Copyright 2013 Sorcersoft.com S.A.
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
package sorcer.maven.plugin;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.jini.core.lookup.ServiceRegistrar;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.InstantiationStrategy;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.resolution.DependencyResolutionException;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.sonatype.aether.util.artifact.JavaScopes;

import sorcer.core.SorcerConstants;
import sorcer.maven.util.ArtifactUtil;
import sorcer.maven.util.EnvFileHelper;
import sorcer.maven.util.JavaProcessBuilder;
import sorcer.maven.util.TestCycleHelper;
import sorcer.tools.webster.Webster;
import sorcer.util.JavaSystemProperties;
import sorcer.util.ServiceAccessor;

/**
 * Boot sorcer provider
 */
@Execute(phase = LifecyclePhase.COMPILE)
@Mojo(name = "provider", defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, instantiationStrategy = InstantiationStrategy.SINGLETON)
public class BootMojo extends AbstractSorcerMojo {
	/**
	 * Location of the services file.
	 */
	@Parameter(defaultValue = "${project.build.outputDirectory}/META-INF/sorcer/services.config")
	private File servicesConfig;

	@Parameter(required = true, defaultValue = "sorcer.boot.ServiceStarter")
	protected String mainClass;

	@Parameter(property = "project.build.directory", readonly = true)
	protected File projectOutputDir;

	@Parameter(defaultValue = "${v.sorcer}")
	protected String sorcerVersion;

	@Parameter(defaultValue = "org.sorcersoft.sorcer:sos-boot")
	protected String booter;

	@Parameter(defaultValue = "${providerName}-prv")
	protected String provider;

	@Parameter(property = "sorcer.provider.debug")
	protected boolean debug;

	/**
	 * Log file to redirect standard and error output to. This only works for
	 * java 1.7+
	 */
	@Parameter(defaultValue = "${project.build.directory}/provider.log")
	protected File logFile;

	public void execute() throws MojoExecutionException, MojoFailureException {
		getLog().debug("servicesConfig: " + servicesConfig);

		// prepare sorcer.env with updated group
		String sorcerEnv = EnvFileHelper.prepareEnvFile(projectOutputDir.getPath());
		List<Artifact> artifacts;

		try {
			artifacts = resolveDependencies(new DefaultArtifact(booter + ":" + sorcerVersion), JavaScopes.RUNTIME);
		} catch (DependencyResolutionException e) {
			throw new MojoExecutionException("Error while resolving booter dependencies", e);
		}

		Map<String, String> properties = new HashMap<String, String>();
		String sorcerHome = System.getenv("SORCER_HOME");
		properties.put(JavaSystemProperties.JAVA_NET_PREFER_IPV4_STACK, "true");
		properties.put(JavaSystemProperties.JAVA_RMI_SERVER_USE_CODEBASE_ONLY, "false");
		properties.put(JavaSystemProperties.JAVA_PROTOCOL_HANDLER_PKGS, "net.jini.url|sorcer.util.bdb.sos");
		properties.put(JavaSystemProperties.JAVA_SECURITY_POLICY, new File(testOutputDir, "sorcer.policy").getPath());
		properties.put(SorcerConstants.SORCER_HOME, sorcerHome);
		properties.put(SorcerConstants.RIO_HOME, System.getenv("RIO_HOME"));
		properties.put(SorcerConstants.WEBSTER_TMP_DIR, new File(sorcerHome, "data").getPath());
		properties.put(SorcerConstants.S_KEY_SORCER_ENV, sorcerEnv);
		properties.put(SorcerConstants.P_WEBSTER_PORT, "" + reservePort());

		JavaProcessBuilder builder = new JavaProcessBuilder();
		builder.setMainClass(mainClass);
		builder.setProperties(properties);
		builder.setParameters(Arrays.asList(servicesConfig.getPath()));
		builder.setClassPath(ArtifactUtil.toString(artifacts));
		builder.setDebugger(debug);
		builder.setOutput(logFile);


		getLog().info("starting sorcer");
		putProcess(builder.startProcess());

		ServiceAccessor.getService(null, ServiceRegistrar.class);
	}

	private int reservePort() {
		int port = Webster.getAvailablePort();
		TestCycleHelper.getInstance().setWebsterPort(port);
		return port;
	}
}
