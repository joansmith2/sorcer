/*
 * Copyright to the original author or authors.
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

/*
 * This configuration is used to start a Lookup Service, and an embedded Webster
 */

import org.rioproject.config.Component

//import sorcer.provider.boot.RioServiceDescriptorUtil;
import org.rioproject.util.ServiceDescriptorUtil;
import com.sun.jini.start.ServiceDescriptor;

@Component('org.rioproject.start')
class StartReggieConfig {

    String rioHome = System.getProperty('RIO_HOME')
    String platformDir = rioHome+'/config/platform/'

    ServiceDescriptor[] getServiceDescriptors() {
        ServiceDescriptorUtil.checkForLoopback()
        def websterRoots = [rioHome+'/lib-dl', ';', rioHome+'/lib']

        String policyFile = rioHome+'/../../configs/rio/rio.policy'
        def reggieConfig = [rioHome+'/../../configs/rio/common.groovy', rioHome+'/../../configs/rio/reggie.groovy']

        def serviceDescriptors = [
                ServiceDescriptorUtil.getWebster(policyFile, '10000', websterRoots as String[]),
                ServiceDescriptorUtil.getLookup(policyFile, reggieConfig as String[])
        ]

        return (ServiceDescriptor[])serviceDescriptors
    }

}


/*
        def serviceDescriptors = [
            RioServiceDescriptorUtil.getWebster(policyFile, '10000', websterRoots as String[]),
            RioServiceDescriptorUtil.getLookup(policyFile, reggieConfig as String[])
        ]
*/
