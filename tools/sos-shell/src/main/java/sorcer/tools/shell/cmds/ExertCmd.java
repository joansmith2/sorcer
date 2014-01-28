/*
 * Copyright 2011 the original author or authors.
 * Copyright 2011 SorcerSoft.org.
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

package sorcer.tools.shell.cmds;

import java.io.*;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import sorcer.core.context.Contexts;
import sorcer.core.context.ThrowableTrace;
import sorcer.core.context.node.ContextNode;
import sorcer.netlet.ScriptExerter;
import sorcer.service.*;
import sorcer.tools.shell.*;

public class ExertCmd extends ShellCmd {

	{
		COMMAND_NAME = "exert";

		NOT_LOADED_MSG = "***command not loaded due to conflict";

		COMMAND_USAGE = "exert [-cc] [[-s | --s | --m] <output filename>] <input filename>";

		COMMAND_HELP = "Manage and execute the federation of services specified by the <input filename>;"
				+ "\n  -cc   print the executed exertion with control dataContext"
				+ "\n  -s   save the command output in a file"
				+ "\n  --s   serialize the command output in a file"
				+ "\n  --m   marshal the the command output in a file";
	}

	private final static Logger logger = Logger.getLogger(ExertCmd.class
			.getName());

    private ScriptExerter scriptExerter;

	private String input;

	private PrintStream out;

	private File outputFile;

	private File scriptFile;

	private String script;

    private INetworkShell shell;

	public ExertCmd() {
	}

	public void execute() throws Throwable {
        out = NetworkShell.getShellOutputStream();
        shell = NetworkShell.getInstance();
        scriptExerter = new ScriptExerter(out, ShellStarter.getLoader(), NetworkShell.getWebsterUrl(), shell.isDebug());
        // TODO - an ugly workaround for problems with noninteractive loading of scripts that contain artifact codebase
        if (!NetworkShell.isInteractive()) Thread.sleep(1000);
        //
        scriptExerter.setConfig(config);
        input = shell.getCmd();
		if (out == null)
			throw new NullPointerException("Must have an output PrintStream");

		File d = NetworkShell.getInstance().getCurrentDir();
		String scriptFilename = null;
		boolean outPersisted = false;
		boolean outputControlContext = false;
		boolean marshalled = false;
		boolean commandLine = NetworkShell.isInteractive();

        Pattern p = Pattern.compile("(\"[^\"]*\"|[^\"^\\s]+)(\\s+|$)", Pattern.MULTILINE);
        Matcher m = p.matcher(input);
        if (m.groupCount() == 0) {
            out.println(COMMAND_USAGE);
            return;
        }
        while (m.find()) {
            String nextToken = m.group(1);
            if (nextToken.startsWith("\""))
                nextToken = nextToken.substring(1, nextToken.length() - 1);
            if (nextToken.equals("-s")) {
					outPersisted = true;
					outputFile = new File("" + d + File.separator + nextToken);
				} else if (nextToken.equals("-controlContext"))
					outputControlContext = true;
				else if (nextToken.equals("-m"))
					marshalled = true;
				// evaluate text
				else if (nextToken.equals("-t")) {
					if (script == null || script.length() == 0) {
						throw new NullPointerException("Must have not empty script");
					}
				}
				// evaluate file script
				else if (nextToken.equals("-f"))
					scriptFilename = nextToken;
				else
					scriptFilename = nextToken;
			}
        if (script != null) {
            scriptExerter.readScriptWithHeaders(script);
		} else if (scriptFilename != null) {
			if ((new File(scriptFilename)).isAbsolute()) {
				scriptFile = NetworkShell.huntForTheScriptFile(scriptFilename);
			} else {
				scriptFile = NetworkShell.huntForTheScriptFile("" + d
						+ File.separator + scriptFilename);
			}
			try {
                scriptExerter.readFile(scriptFile);
			} catch (IOException e) {
				out.append("File: " + scriptFile.getAbsolutePath() + " could not be found or read: " + e.getMessage());
			}
		} else {
			out.println("Missing exertion input filename!");
			return;
		}
        Object target = scriptExerter.parse();
        Object result = scriptExerter.execute();
        // System.out.println(">>>>>>>>>>> result: " + result);
		if (result != null) {
			if (!(result instanceof Exertion)) {
				out.println("\n---> EVALUATION RESULT --->");
				out.println(result);
				return;
			}
			Exertion xrt = (Exertion) result;
			if (xrt.getExceptions().size() > 0) {
				if (commandLine) {
					out.println("Exceptions: ");
					out.println(xrt.getExceptions());
				} else {
					List<ThrowableTrace> ets = xrt.getExceptions();
                    out.println("Exceptions: ");
					for (ThrowableTrace t : ets) {
                        out.println(t.message);
						out.println(t.describe());
					}
				}
			}
			out.println("\n---> OUTPUT EXERTION --->");
			out.println(((ServiceExertion) xrt).describe());
			out.println("\n---> OUTPUT DATA CONTEXT --->");
			if (xrt.isJob()) {
				out.println(((Job)xrt).getJobContext());
            } else {
				out.println(xrt.getDataContext());
            }
            saveFilesFromContext(xrt, out);
			if (outputControlContext) {
				out.println("\n---> OUTPUT CONTROL CONTEXT --->");
				out.println(xrt.getControlContext());
			}
		} else {
			if (target != null) {
				out.println("\n--- Failed to excute exertion ---");
				out.println(((ServiceExertion) target).describe());
				out.println(((ServiceExertion) target).getDataContext());
				if (!commandLine) {
					out.println("Script failed: " + scriptFilename);
					out.println(script);
				}
			}
			// System.out.println(">>> executing script: \n" + sb.toString());
		}
	}

	public String getScript() {
		return script;
	}

	public void setScript(String script) {
		this.script = script;
	}

	public static String readFile(File file) throws IOException {
		// String lineSep = System.getProperty("line.separator");
		String lineSep = "\n";
		BufferedReader br = new BufferedReader(new FileReader(file));
		String nextLine = "";
		StringBuffer sb = new StringBuffer();
		nextLine = br.readLine();
		// skip shebang line
		if (nextLine.indexOf("#!") < 0) {
			sb.append(nextLine);
			sb.append(lineSep);
    }
		while ((nextLine = br.readLine()) != null) {
			sb.append(nextLine);
			sb.append(lineSep);
		}
		return sb.toString();
	}

	private StringBuilder readTextFromJar(String filename) {
		InputStream is = null;
		BufferedReader br = null;
		String line;
		StringBuilder sb = new StringBuilder();

		try {
			is = getClass().getResourceAsStream(filename);
			if (is != null) {
				br = new BufferedReader(new InputStreamReader(is));
				while (null != (line = br.readLine())) {
					sb.append(line);
					sb.append("\n");
    			}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null)
					br.close();
				if (is != null)
					is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return sb;
	}


    private void saveFilesFromContext(Exertion xrt, PrintStream out) {
        try {
            ContextNode[] cns = (xrt.isJob() ? Contexts.getTaskContextNodes((ComplexExertion)xrt)
                    : Contexts.getTaskContextNodes(xrt));
            for (ContextNode cn : cns) {

                if (cn.isOut() && cn.getData()!=null && cn.getData() instanceof byte[]) {
                    File f = new File(cn.getName());
                    FileUtils.writeByteArrayToFile(f, (byte[])cn.getData());
                    out.println("A file was extracted and saved from context to: " + f.getAbsolutePath());
                }
            }
        } catch (ContextException e) {
            out.println(e.getMessage());
        } catch (IOException e) {
            out.println(e.getMessage());
        }
    }
}
