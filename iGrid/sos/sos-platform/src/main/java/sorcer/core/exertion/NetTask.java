/*
 * Copyright 2010 the original author or authors.
 * Copyright 2010 SorcerSoft.org.
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

package sorcer.core.exertion;

import java.rmi.RemoteException;
import java.util.Arrays;

import net.jini.core.transaction.Transaction;
import net.jini.core.transaction.TransactionException;
import net.jini.id.Uuid;
import sorcer.core.signature.NetSignature;
import sorcer.service.Context;
import sorcer.service.ExertionException;
import sorcer.service.Servicer;
import sorcer.service.Signature;
import sorcer.service.SignatureException;
import sorcer.service.Task;
import sorcer.util.ExertionShell;

/**
 * The SORCER service task extending the abstract task {@link Task}.
 * 
 * @author Mike Sobolewski
 */
public class NetTask extends ObjectTask {

	private static final long serialVersionUID = -6741189881780105534L;

	
	public NetTask() {
		// do nothing
	}

	public NetTask(String name) {
		super(name);
	}

	public NetTask(Uuid jobId, int jobState) {
		setParentId(jobId);
		status = new Integer(jobState);
	}

	public NetTask(String name, String description) {
		this(name);
		this.description = description;
	}

	public NetTask(String name, NetSignature signature) {
		this(name, "");
		signatures.add(signature);
	}

	public NetTask(String name, String description, NetSignature signature) {
		this(name, description);
		signatures.add(signature);
	}

	public NetTask(String name, NetSignature signature, Context context) {
		this(name);
		signatures.add(signature);
		setContext(context);
	}
	
	public NetTask(String name, String description, NetSignature signature, Context context) {
		this(name, description);
		signatures.add(signature);
		setContext(context);
	}

	public NetTask(String name, Signature[] signatures,  Context context)
			throws SignatureException {
		this(name);
		setContext(context);

		try {
			for (Signature s : signatures)
				((NetSignature) s).setExertion(this);
		} catch (ExertionException e) {
			e.printStackTrace();
		}
		this.signatures.addAll(Arrays.asList(signatures));
	}
	
	public void setServicer(Servicer provider) {
		((NetSignature) getProcessSignature()).setServicer(provider);
	}

	public Servicer getServicer() {
		return ((NetSignature) getProcessSignature()).getServicer();
	}
	
	public Task doTask(Transaction txn) throws ExertionException, SignatureException, RemoteException {	
		ExertionShell esh = new ExertionShell(this);
		try {
			return (Task)esh.exert();
		} catch (TransactionException e) {
			throw new ExertionException(e);
		}
	}
	
	public static NetTask getTemplate() {
		NetTask temp = new NetTask();
		temp.status = new Integer(INITIAL);
		temp.priority = null;
		temp.index = null;
		temp.signatures = null;
		return temp;
	}

	public static NetTask getTemplate(String provider) {
		NetTask temp = getTemplate();
		temp.getProcessSignature().setProviderName(provider);
		return temp;
	}

}
