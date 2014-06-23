/*
 * Copyright 2014 Sorcersoft.com S.A.
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

package sorcer.core.service;

import net.jini.export.Exporter;
import sorcer.config.Component;
import sorcer.config.ConfigEntry;
import sorcer.container.jeri.AbstractExporterFactory;
import sorcer.core.SorcerEnv;

import static sorcer.container.jeri.ExporterFactories.EXPORTER;

/**
 * @author Rafał Krupiński
 */
@Component("sorcer.core.service.ServiceBuilder")
public class ServiceBuilderConfig {
    @ConfigEntry
    public Class type;

    @ConfigEntry
    public boolean export = true;

    @ConfigEntry
    public boolean register = true;

    @ConfigEntry
    public AbstractExporterFactory<? extends Exporter> exporterFactory = EXPORTER;

    @ConfigEntry
    public String[] lookupGroups = SorcerEnv.getLookupGroups();


}
