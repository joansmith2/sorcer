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

package sorcer.service;

import java.rmi.RemoteException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.security.auth.Subject;

import net.jini.core.lookup.ServiceID;
import net.jini.core.transaction.Transaction;
import net.jini.core.transaction.TransactionException;
import sorcer.co.tuple.Entry;
import sorcer.core.SorcerConstants;
import sorcer.core.context.ControlContext;
import sorcer.core.context.ControlContext.ThrowableTrace;
import sorcer.core.context.ServiceContext;
import sorcer.core.signature.NetSignature;
import sorcer.eo.operator;
import sorcer.security.util.Auth;
import sorcer.security.util.SorcerPrincipal;

/**
 * A job is a composite service-oriented message comprised of {@link Exertion}
 * instances with its own service {@link Context} and a collection of service
 * {@link Signature}s. The job's signature is usually referring to a
 * {@link sorcer.service.Jobber} and the job's dataContext describes the composition
 * of component exertions as defined by the Interpreter programming pattern.
 * 
 * @see sorcer.service.Exertion
 * @see sorcer.service.Task
 * 
 * @author Mike Sobolewski
 */
public abstract class Job extends ServiceExertion {

	private static final long serialVersionUID = -6161435179772214884L;

	/* our logger */
	protected final static Logger logger = Logger.getLogger(Job.class.getName());
	
	/**
	 * Component exertions of this job (the Composite Design pattern)
	 */
	protected List<Exertion> exertions = new ArrayList<Exertion>();

	public Integer state = new Integer(ExecState.INITIAL);

	/**
	 * Constructs a job and sets all default values to it.
	 * @throws SignatureException 
	 */
	public Job() {
		exertions = new ArrayList<Exertion>();
		// exertions = Collections.synchronizedList(new ArrayList<Exertion>());
		init();
	}

	/**
	 * Constructs a job and sets all default values to it.
	 * 
	 * @param name
	 *            The name of the job.
	 * @throws SignatureException 
	 */
	public Job(String name) {
		super(name);
	}

	/**
	 * Constructs a job and sets all default values to it.
	 * 
	 * @param name
	 *            The first Exertion of the job.
	 */
	public Job(Exertion exertion) {
		addExertion(exertion);
	}

	public Job(String name, String description) {
		this(name);
		this.description = description;
	}

	public Job(String name, String description, List<Signature> signatures) {
		this(name, description);
		this.signatures = signatures;
	}

	/**
	 * Initialize it with assigning it a new ControlContext and a defaultMethod
	 * with serviceType as "sorcer.core.provider.jobber.ServiceJobber" name as
	 * "service" and providerName "*"
	 * @throws SignatureException 
	 */
	private void init() {
		NetSignature s = new NetSignature("service", Jobber.class);
		// Needs to be RemoteJobber for Cataloger to find it
		// s.setServiceType(Jobber.class.getName());
		s.setProviderName(null);
		s.setType(Signature.Type.SRV);
		signatures.add(s); // Add the signature
	}

	public List<Signature> getSignatures() {
		if (signatures != null)
			for (int i = 0; i < signatures.size(); i++)
				signatures.get(i).setProviderName(controlContext.getRendezvousName());
		return signatures;
	}

	/** {@inheritDoc} */
	public boolean isJob() {
		// jobs with one exertion are not treated as job compositions
		// inner element is treated as standalone exertion and the outer
		// one as the dummy container for a technical reason 
		// to use the operator 'xrt' in EOL for both 'task' and 'job'.
		if (exertions.size() > 0)
			return true;
		else
			return false;
	}

	/* (non-Javadoc)
	 * @see sorcer.service.ServiceExertion#isTask()
	 */
	@Override
	public boolean isTask() {
		return !isJob();
	}
	
	public boolean hasChild(String childName) {
		for (Exertion ext : exertions) {
			if (ext.getName().equals(childName))
				return true;
		}
		return false;
	}

	public Exertion getChild(String childName) {
		for (Exertion ext : exertions) {
			if (ext.getName().equals(childName))
				return ext;
		}
		return null;
	}

	public long getLsbID() {
		return (lsbId == null) ? -1 : lsbId.longValue();
	}

	/**
	 * Returns the number of exertions in this Job.
	 * 
	 * @return the number of exertions in this Job.
	 */
	public int size() {
		return exertions.size();
	}

	public int indexOf(Exertion ex) {
		return exertions.indexOf(ex);
	}

	/**
	 * Sets the component at the specified <code>index</code> of this vector to
	 * be the specified object. The previous component at that position is
	 * discarded.
	 * <p>
	 */
	public void setExertionAt(Exertion ex, int i) {
		exertions.set(i, ex);
	}

	public Exertion getMasterExertion() {
		String contextName = null;
		try {
			contextName = (String) controlContext.getValue(ControlContext.MASTER_EXERTION);
		} catch (ContextException ex) {
			ex.printStackTrace();
		}
		if (contextName == null
				&& controlContext.getFlowType().equals(ControlContext.SEQUENTIAL)) {
			return (size() > 0) ? exertionAt(size() - 1) : null;
		} else {
			Exertion master = null;
			for (int i = 0; i < size(); i++) {
				if (((ServiceExertion) exertionAt(i)).getId().equals(
						contextName)) {
					master = exertionAt(i);
					break;
				}
			}
			return master;
		}
	}

	public void setRendezvousName(String jobberName) {
		controlContext.setRendezvousName(jobberName);
	}

	public Signature getProcessSignature() {
		Signature method = super.getProcessSignature();
		if (method != null)
			method.setProviderName(controlContext.getRendezvousName());
		return method;
	}
	
	public Job addExertion(Exertion ex) {
		exertions.add(ex);
		// default Jobber signature
//		if (exertions.size()==2)
//			signatures.add(new NetSignature("service", RemoteJobber.class));
		((ServiceExertion) ex).setIndex(exertions.indexOf(ex));
		controlContext.registerExertion(ex);
		((ServiceExertion) ex).setParentId(getId());
		return this;
	}

	public void addExertions(List<Exertion> exertions) {
		if (this.exertions != null)
			this.exertions.addAll(exertions);
		else {
			this.exertions = new ArrayList<Exertion>();
			this.exertions.addAll(exertions);
		}
	}

	public void setExertions(List<Exertion> exertions) {
		this.exertions = exertions;

	}

	public Job addExertion(Exertion exertion, int priority) {
		addExertion(exertion);
		controlContext.setPriority(exertion, priority);
		return this;
	}

	public Exertion removeExertion(Exertion exertion) {
		// int index = ((ExertionImpl)exertion).getIndex();
		exertions.remove(exertion);
		controlContext.deregisterExertion(this, exertion);
		return exertion;
	}

	public void removeExertionAt(int index) {
		removeExertion(exertionAt(index));
	}

	/**
	 * Returns the exertion at the specified index.
	 * <p>
	 * 
	 * @param index
	 *            an index into this vector.
	 * @return the Exertion at the specified index.
	 * @exception ArrayIndexOutOfBoundsException
	 *                if the <tt>index</tt> is negative or not less than the
	 *                current size of this <tt>Job</tt> object.
	 */
	public Exertion exertionAt(int i) {
		return (Exertion) exertions.get(i);
	}

	public abstract Job doJob(Transaction txn) throws ExertionException,
			SignatureException, RemoteException, TransactionException;

	public void undoJob() throws ExertionException, SignatureException,
			RemoteException {
		throw new ExertionException("Not implemneted by this Job: " + this);
	}
	
	public void setState(int state) {
		this.state = new Integer(state);
	}

	public int getState() {
		return state.intValue();
	}

	public String getPrincipalID() {
		Set principals = subject.getPrincipals();
		Iterator iterator = principals.iterator();
		while (iterator.hasNext()) {
			Principal p = (Principal) iterator.next();
			if (p instanceof SorcerPrincipal)
				return ((SorcerPrincipal) p).getId();
		}
		return null;
	}

	public void setPrincipalID(String id) {
		Set principals = subject.getPrincipals();
		Iterator iterator = principals.iterator();
		while (iterator.hasNext()) {
			Principal p = (Principal) iterator.next();
			if (p instanceof SorcerPrincipal)
				((SorcerPrincipal) p).setId(id);
		}
	}

	public void setSubject(Subject subject) {
		this.subject = subject;
		for (int i = 0; i < size(); i++) {
			((ServiceExertion) exertionAt(i)).setSubject(subject);
		}
	}

	public void setPrincipal(SorcerPrincipal principal) {
		setSubject(Auth.createSubject(principal));
		this.principal = principal;
	}

	public Subject getSubject() {
		return subject;
	}
	
	public ServiceID getServiceID() {
		if (lsbId == null || msbId == null)
			return null;
		else
			return new ServiceID(msbId.longValue(), lsbId.longValue());
	}
	
	/**
	 * Returns a string representation of Contexts of this Job, containing the
	 * String representation of each dataContext in it's exertion.
	 */
	public String jobContextToString() {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < exertions.size(); i++) {
			if (((ServiceExertion) exertionAt(i)).isJob())
				sb.append(((Job) exertionAt(i)).jobContextToString());
			else
				sb.append(((ServiceExertion) exertionAt(i)).contextToString());
		}
		return sb.toString();
	}

	public void setMasterExertion(Exertion exertion) {
		controlContext.setMasterExertion(exertion);
	}

	public void setOwnerId(String id) {
		ownerId = id;
		if (controlContext != null)
			controlContext.setOwnerID(id);
		for (int i = 0; i < exertions.size(); i++)
			(((ServiceExertion) exertionAt(i))).setOwnerId(id);
	}

	public String getContextName() {
		return Context.JOB_ + name + "[" + index + "]" + Context.ID;
	}

	public String toString() {
		StringBuffer desc = new StringBuffer(super.toString());
		desc.append("\n=== START PRINTING JOB ===\n");	
		desc
				.append("\n=============================\nListing Component Exertions\n=============================\n");
		for (int i = 0; i < size(); i++) {
			desc.append("\n===========\n Exertion ").append(i).append(
					"\n===========\n").append(
					((ServiceExertion) exertionAt(i)).describe());
		}
		desc.append("\n=== DONE PRINTING JOB ===\n");
		return desc.toString();
	}

	/**
	 * Returns all component <code>Exertion</code>s of this composite exertion.
	 * 
	 * @return all component exertions
	 */
	public List<Exertion> getExertions() {
		return exertions;
	}

	public List<Exertion> getExertions(List<Exertion> exs) {
		for (Exertion e : exertions)
			((ServiceExertion) e).getExertions(exs);
		exs.add(this);
		return exs;
	}
	
    @Override
    public List<ThrowableTrace> getExceptions() {
        List<ThrowableTrace> exceptions = new ArrayList<ThrowableTrace>();
        for (Exertion ext : exertions) {
            exceptions.addAll(ext.getExceptions());
        }
        return exceptions;
    }



    /**
	 * Return true if this composite <code>Job</code> is a tree.
	 * 
	 * @param visited
	 *            a set of visited exertions
	 * @return true if this <code>Job</code> composite is a tree
	 * @see Exertion#isTree()
	 */
	public boolean isTree(Set visited) {
		visited.add(this);
		Iterator i = exertions.iterator();
		while (i.hasNext()) {
			ServiceExertion e = (ServiceExertion) i.next();
			if (visited.contains(e) || !e.isTree(visited)) {
				return false;
			}
		}
		return true;
	}

	public Exertion getExertion(int index) {
		return exertions.get(index);
	}
	
	public Context getJobContext() {
		ServiceContext cxt = new ServiceContext("job/" + name);
		cxt.setSubject("job/data/context", name);
		return linkContext(cxt, "job[" + getName() + "]");
	}

    public Context getControlInfo() {
        ServiceContext cxt = new ServiceContext("control/" + name);
        //ServiceContext cxt = new ServiceContext(name);
        cxt.setSubject("job/control/context", name);

        return linkControlContext(cxt, "job[" + getName() + "]");
    }


    @Override
	public Context linkContext(Context context, String path) {
		Exertion ext;
		for (int i = 0; i < size(); i++) {
			ext = exertions.get(i);
			try {
				((ServiceExertion) ext).linkContext(context, path + SorcerConstants.CPS
						+ (ext instanceof Job ? "job[" : "task[")
						+ ext.getName() + "]");
			} catch (ContextException e) {
				e.printStackTrace();
			}
		}
		return context;
	}
	
	@Override
	public Context linkControlContext(Context context, String path) {
		Exertion ext;
		for (int i = 0; i < size(); i++) {
			ext = exertions.get(i);
			try {
				((ServiceExertion) ext).linkControlContext(context, path + SorcerConstants.CPS
						+ (ext instanceof Job ? "job" : "task[")
						+ ext.getName() + "]");
			} catch (ContextException e) {
				e.printStackTrace();
			}
		}
		return context;
	}

	public Object getJobValue(String path) throws ContextException {
		String[] attributes = operator.pathToArray(path);
		// remove the leading attribute of the current exertion
		if (attributes[0].equals(getName())) {
			String[] attributes1 = new String[attributes.length - 1];
			System.arraycopy(attributes, 1, attributes1, 0,
					attributes.length - 1);
			attributes = attributes1;
		}
		String last = attributes[0];
		Exertion exti = this;
		for (String attribute : attributes) {
			if (((ServiceExertion) exti).hasChild(attribute)) {
				exti = ((Job) exti).getChild(attribute);
				if (exti instanceof Task) {
					last = attribute;
					break;
				}
			} else {
				break;
			}
		}
		int index = path.indexOf(last);
		String contextPath = path.substring(index + last.length() + 1);

		return exti.getDataContext().getValue(contextPath);
	}

	public Object getValue(String path) throws ContextException {
		if (path.indexOf(name) >= 0)
			return getJobValue(path);
		else
			return super.getValue(path);
	}
	
	public Object putValue(String path, Object value) throws ContextException {
		if (path.indexOf(name) >= 0)
			putJobValue(path, value);
		else
			super.putValue(path, value);
		return value;
	}
	
	public Object putJobValue(String path, Object value) throws ContextException {
		String[] attributes = operator.pathToArray(path);
		// remove the leading attribute of the current exertion
		if (attributes[0].equals(getName())) {
			String[] attributes1 = new String[attributes.length - 1];
			System.arraycopy(attributes, 1, attributes1, 0,
					attributes.length - 1);
			attributes = attributes1;
		}
		String last = attributes[0];
		Exertion exti = this;
		for (String attribute : attributes) {
			if (((ServiceExertion) exti).hasChild(attribute)) {
				exti = ((Job) exti).getChild(attribute);
				if (exti instanceof Task) {
					last = attribute;
					break;
				}
			} else {
				break;
			}
		}
		int index = path.indexOf(last);
		String contextPath = path.substring(index + last.length() + 1);
		exti.getDataContext().putValue(contextPath, value);
		return value;
	}
	
	
	public Context getComponentContext(String path) {
		Exertion xrt = getComponentExertion(path);
		return xrt.getDataContext();
	}
	
	public Context getComponentControlContext(String path) {
		Exertion xrt = getComponentExertion(path);
		return xrt.getControlContext();
	}
	
	public Exertion getComponentExertion(String path) {
		String[] attributes = operator.pathToArray(path);
		// remove the leading attribute of the current exertion
		if (attributes[0].equals(getName())) {
			String[] attributes1 = new String[attributes.length - 1];
			System.arraycopy(attributes, 1, attributes1, 0,
					attributes.length - 1);
			attributes = attributes1;
		}
		Exertion exti = this;
		for (String attribute : attributes) {
			if (((ServiceExertion) exti).hasChild(attribute)) {
				exti = ((Job) exti).getChild(attribute);
				if (exti instanceof Task) {
					break;
				}
			} else {
				break;
			}
		}
		return exti;
	}
	
	public void substitute(Entry... entries) throws EvaluationException {
		try {
			for (Entry e : entries) {
				if (e.path().indexOf(name) >= 0)
					putJobValue(e.path(), e.value());

				else
					super.putValue(e.path(), e.value());
			}
		} catch (ContextException ex) {
			ex.printStackTrace();
			throw new EvaluationException(ex);
		}
	}
	
}
