/**
 * Deployment configuration for dbp-prv
 *
 * @author Pawel Rubach
 */
import sorcer.core.SorcerEnv;

String[] getInitialMemberGroups() {
    def groups = SorcerEnv.getLookupGroups();
    return groups as String[]
}

def getSorcerVersion() {
    return sorcerVersion = SorcerEnv.getSorcerVersion();
}

def String getCodebase() {
    return SorcerEnv.getWebsterUrl();
}


deployment(name: 'mahalo-provider') {
    groups getInitialMemberGroups();

    codebase getCodebase()

    artifact id: 'mahalo-dl', 'org.apache.river:mahalo-dl:2.2.2'
    artifact id: 'mahalo-cfg', "org.sorcersoft.sorcer:mahalo-cfg:" + getSorcerVersion()

    service(name: 'Mahalo') {
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
}
