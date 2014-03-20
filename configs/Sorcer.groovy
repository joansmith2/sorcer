/**
 * Deployment configuration for the minimum Sorcer
 *
 * @author Pawel Rubach based on Dennis Reedy's example
 */
import sorcer.core.SorcerEnv;
import static sorcer.core.SorcerConstants.SORCER_VERSION;


String[] getInitialMemberGroups() {
    def groups = SorcerEnv.getLookupGroups();
    return groups as String[]
}

def getSorcerHome() {
    return SorcerEnv.getHomeDir().path;
}

def fs() {
    return File.separator;
}


def getSorcerVersion() {
    return sorcerVersion = SorcerEnv.getSorcerVersion();
}


def String getCodebase() {
    return SorcerEnv.getWebsterUrl();
}

deployment(name: 'Sorcer') {
    groups getInitialMemberGroups();

    codebase getCodebase()

    def sorcer = [
            api: "org.sorcersoft.sorcer:sorcer-api:" + getSorcerVersion(),
            platform:"org.sorcersoft.sorcer:sos-platform:" + getSorcerVersion()
    ]

    artifact id: 'mahalo-cfg', "org.sorcersoft.sorcer:mahalo-cfg:" + getSorcerVersion()
    artifact id: 'mahalo-dl', 'org.apache.river:mahalo-dl:2.2.2'
    artifact id: 'fiddler-cfg', "org.sorcersoft.sorcer:fiddler-cfg:" + getSorcerVersion()
    artifact id: 'fiddler-dl', 'org.apache.river:fiddler-dl:2.2.2'
    artifact id: 'norm-cfg', "org.sorcersoft.sorcer:norm-cfg:" + getSorcerVersion()
    artifact id: 'norm-dl', 'org.apache.river:norm-dl:2.2.2'
    artifact id: 'mercury-cfg', "org.sorcersoft.sorcer:mercury-cfg:" + getSorcerVersion()
    artifact id: 'mercury-dl', 'org.apache.river:mercury-dl:2.2.2'
    artifact id: 'sos-exertlet-sui', "org.sorcersoft.sorcer:sos-exertlet-sui:" + getSorcerVersion()

    def cataloger = [
            impl: 'org.sorcersoft.sorcer:cataloger-cfg:' + SORCER_VERSION,
            api : 'org.sorcersoft.sorcer:cataloger-api:' + SORCER_VERSION
    ]

    artifact id: 'jobber-cfg', "org.sorcersoft.sorcer:jobber-cfg:" + getSorcerVersion()
    artifact id: 'concatenator-cfg', "org.sorcersoft.sorcer:concatenator-cfg:" + getSorcerVersion()
    artifact id: 'spacer-cfg', "org.sorcersoft.sorcer:spacer-cfg:" + getSorcerVersion()
    artifact id: 'logger-cfg', "org.sorcersoft.sorcer:logger-cfg:" + getSorcerVersion()
    artifact id: 'logger-sui', "org.sorcersoft.sorcer:logger-sui:" + getSorcerVersion()
    artifact id: 'dbp-api', "org.sorcersoft.sorcer:dbp-api:" + getSorcerVersion()
    artifact id: 'dbp-cfg', "org.sorcersoft.sorcer:dbp-cfg:" + getSorcerVersion()
    artifact id: 'exertmonitor-cfg', "org.sorcersoft.sorcer:exertmonitor-cfg:" + getSorcerVersion()
    artifact id: 'exerter-cfg', "org.sorcersoft.sorcer:exerter-cfg:" + getSorcerVersion()

    def blitz = [
        impl:"org.sorcersoft.sorcer:blitz-cfg:" + getSorcerVersion(),
        api: 'org.sorcersoft.blitz:blitz-proxy:2.3'
    ]


    service(name: 'Mahalo') { //fork:'yes'
        interfaces {
            classes 'com.sun.jini.mahalo.TxnManager'
            artifact ref: 'mahalo-dl'
        }
        implementation(class: 'com.sun.jini.mahalo.TransientMahaloImpl') {
            artifact ref: 'mahalo-cfg'
        }
        configuration file: 'classpath:mahalo.config'
        maintain 1
    }
/*
    service(name: 'Fiddler') {
        interfaces {
            classes 'com.sun.jini.fiddler.Fiddler'
            artifact ref: 'fiddler-dl'
        }
        implementation(class: 'com.sun.jini.fiddler.TransientFiddlerImpl') {
            artifact ref: 'fiddler-cfg'
        }
        configuration file: 'classpath:fiddler.config'
        maintain 1
    }
    service(name: 'Norm') {
        interfaces {
            classes 'com.sun.jini.norm.NormServer'
            artifact ref: 'norm-dl'
        }
        implementation(class: 'com.sun.jini.norm.TransientNormServerImpl') {
            artifact ref: 'norm-cfg'
        }
        configuration file: 'classpath:norm.config'
        maintain 1
    }

    service(name: 'Mercury') {
        interfaces {
            classes 'com.sun.jini.mercury.MailboxBackEnd'
            artifact ref: 'mercury-dl'
        }
        implementation(class: 'com.sun.jini.mercury.TransientMercuryImpl') {
            artifact ref: 'mercury-cfg'
        }
        configuration file: 'classpath:mercury.config'
        maintain 1
    }
*/

    service(name: "BlitzSpace") { fork:'yes'
        interfaces {
            classes 'net.jini.space.JavaSpace05'
            artifact blitz.api
        }
        implementation(class: 'org.dancres.blitz.remote.BlitzServiceImpl') {
            artifact blitz.impl
        }
        configuration file: 'classpath:blitz.config'
        maintain 1
    }

    service(name: "Jobber") { //fork:'yes'
        interfaces {
            classes 'sorcer.core.provider.Jobber'
            artifact sorcer.platform
        }
        implementation(class: 'sorcer.core.provider.jobber.ServiceJobber') {
            artifact ref: 'jobber-cfg'
        }
        configuration file: 'classpath:jobber.config'
        maintain 1
    }

    service(name: "Spacer") { //fork:'yes'
        interfaces {
            classes 'sorcer.core.provider.Spacer'
            artifact ref: 'sos-exertlet-sui'
        }
        implementation(class: 'sorcer.core.provider.jobber.ServiceSpacer') {
            artifact ref: 'spacer-cfg'
        }
        configuration file: 'classpath:spacer.config'
        maintain 1
    }

    service(name: "Concatenator") { //fork:'yes'
        interfaces {
            classes 'sorcer.core.provider.Concatenator'
            artifact ref: 'sos-exertlet-sui'
        }
        implementation(class: 'sorcer.core.provider.jobber.ServiceConcatenator') {
            artifact ref: 'concatenator-cfg'
        }
        configuration file: 'classpath:concatenator.config'
        maintain 1
    }

    service(name: "Cataloger") { //fork:'yes'
        interfaces {
            classes 'sorcer.core.provider.Cataloger'
            artifact cataloger.api
        }
        implementation(class: 'sorcer.core.provider.cataloger.ServiceCataloger') {
            artifact cataloger.impl
        }
        configuration file: 'classpath:cataloger.config'
        maintain 1
    }

    service(name: "Logger") { //fork:'yes'
        interfaces {
            classes 'sorcer.core.RemoteLogger'
            artifact ref: 'logger-sui'
        }
        implementation(class: 'sorcer.core.provider.logger.RemoteLoggerManager') {
            artifact ref: 'logger-cfg'
        }
        configuration file: 'classpath:logger.config'
        maintain 1
    }

    service(name: "ExertMonitor") { fork:'yes'
        interfaces {
            classes 'sorcer.core.monitor.MonitoringManagement'
            artifact ref: 'sos-exertlet-sui'
        }
        implementation(class: 'sorcer.core.provider.exertmonitor.ExertMonitor') {
            artifact ref: 'exertmonitor-cfg'
        }
        configuration file: 'classpath:exertmonitor.config'
        maintain 1
    }

    service(name: "DatabaseStorer") { fork:'yes'
        interfaces {
            classes 'sorcer.service.DatabaseStorer'
            artifact ref: 'dbp-api'
        }
        implementation(class: 'sorcer.core.provider.ServiceProvider') {
            artifact ref: 'dbp-cfg'
        }
        configuration file: 'classpath:dbp.config'
        maintain 1
    }

    service(name: "Exerter") { //fork:'yes'
        interfaces {
            classes 'sorcer.core.provider.ServiceTasker'
            artifact ref: 'sos-exertlet-sui'
        }
        implementation(class: 'sorcer.core.provider.ServiceTasker') {
            artifact ref: 'exerter-cfg'
        }
        configuration file: 'classpath:exerter.config'
        maintain 1
    }
}
