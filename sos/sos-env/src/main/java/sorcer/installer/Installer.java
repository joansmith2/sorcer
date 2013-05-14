package sorcer.installer;

import org.apache.commons.io.FileUtils;
import sorcer.resolver.Resolver;

import java.io.*;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * User: prubach
 * Date: 14.05.13
 * Time: 10:18
 */
public class Installer {
    protected Map<String, String> groupDirMap = new HashMap<String, String>();
    protected Map<String, String> versionsMap = new HashMap<String, String>();

    private static String repoDir;

    protected static final Logger logger = Logger.getLogger(Installer.class.getName());

    {
        repoDir = Resolver.getRepoDir();
        if (repoDir == null || (!new File(repoDir).exists()))
            logger.severe("Problem installing jars to local maven repository - repository directory does not exist!");

        String resourceName = "META-INF/maven/versions.properties";
        URL resourceVersions = Thread.currentThread().getContextClassLoader().getResource(resourceName);
        if (resourceVersions == null) {
            throw new RuntimeException("Could not find versions.properties");
        }
        resourceName = "META-INF/maven/repolayout.properties";
        URL resourceRepo = Thread.currentThread().getContextClassLoader().getResource(resourceName);
        if (resourceRepo == null) {
            throw new RuntimeException("Could not find repolayout.properties");
        }
        Properties propertiesRepo = new Properties();
        Properties propertiesVer = new Properties();
        InputStream inputStream = null;
        try {
            inputStream = resourceRepo.openStream();
            propertiesRepo.load(inputStream);
            // properties is a Map<Object, Object> but it contains only Strings
            @SuppressWarnings("unchecked")
            Map<String, String> propertyMap = (Map) propertiesRepo;
            groupDirMap.putAll(propertyMap);

            inputStream = resourceVersions.openStream();
            propertiesVer.load(inputStream);
            // properties is a Map<Object, Object> but it contains only Strings
            @SuppressWarnings("unchecked")
            Map<String, String> propertyMapVer = (Map) propertiesVer;
            versionsMap.putAll(propertyMapVer);
        } catch (IOException e) {
            throw new RuntimeException("Could not load repolayout.properties", e);
        } finally {
            close(inputStream);
        }
    }

    public void install() {
        for (String group : groupDirMap.keySet()) {
            // Ignore Sigar
            if (group.equals("org.sorcersoft.sigar")) continue;

            String dir = groupDirMap.get(group);
            String version = versionsMap.get(group);

            if (dir == null || version == null || !new File(Resolver.getRootDir() + "/" + dir).exists()) {
                logger.severe("Problem installing jars for groupId: " + group + " directory or version not specified: " + dir + " " + version);
                continue;
            }
            File[] jars = new File(Resolver.getRootDir() + "/" + dir).listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    if (pathname.getName().endsWith("jar"))
                        return true;
                    return false;  //To change body of implemented methods use File | Settings | File Templates.
                }
            });

            for (File jar : jars) {
                String fileNoExt = jar.getName().replace(".jar", "");
                String artifactDir = Resolver.getRepoDir() + "/" + group.replace(".", "/") + "/" + fileNoExt + "/" + version;
                try {
                    FileUtils.forceMkdir(new File(artifactDir));
                    FileUtils.copyFile(jar, new File(artifactDir, fileNoExt + "-" + version + ".jar"));
                } catch (IOException io) {
                    logger.severe("Problem installing jar: " + fileNoExt + " to: " + artifactDir);
                }
            }
        }
    }

    public static void main(String[] args) {
        new Installer().install();
    }

    protected void close(Closeable inputStream) {
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException e) {
                // igonre
            }
        }
    }
}
