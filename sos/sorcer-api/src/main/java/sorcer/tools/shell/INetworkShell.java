package sorcer.tools.shell;
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


import net.jini.core.lookup.ServiceRegistrar;
import sorcer.util.WhitespaceTokenizer;

import java.io.File;
import java.io.PrintStream;
import java.util.StringTokenizer;

/**
 * @author Rafał Krupiński
 */
public interface INetworkShell {
    void addToCommandTable(String cmd, Class<? extends ShellCmd> inCls);

    void addAlias(String alias, String command);

    String getCmd();

    File getCurrentDir();

    String getHomeDir();

    String getNshWebsterUrl();

    void setCurrentDir(File file);

    ServiceRegistrar getSelectedRegistrar();

    PrintStream getOutputStream();

    WhitespaceTokenizer getTokenizer();

    boolean isDebug();

    boolean isRemoteLogging();
}
