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
 * This configuration is used to configure Rio tools (UI & CLI)
 */

import org.rioproject.config.Component;
//import org.rioproject.tools.cli.CLI.OptionHandlerDesc;

@Component('net.jini.discovery.LookupDiscovery')
class ClientDiscoveryConfig {
    long multicastAnnouncementInterval=5000
}

/*@Component('org.rioproject.tools.cli')
class RioCliConfig {
    OptionHandlerDesc[] addOptionHandlers = [
            new OptionHandlerDesc("boot", "sorcer.rio.cli.StartOptionHandler"),
            new OptionHandlerDesc("props", "sorcer.rio.cli.PropertiesOptionHandler")
    ];
}

@Component('org.rioproject.start')
class PropsInit {
    String[] systemProperties = [ "java.protocol.handler.pkgs", "net.jini.url|org.rioproject.url" ];
}
*/