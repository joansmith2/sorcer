package sorcer.core.dispatch;

import net.jini.config.ConfigurationException;
import sorcer.core.Dispatcher;
import sorcer.core.provider.Provider;
import sorcer.service.Block;
import sorcer.service.Exec;

import java.rmi.RemoteException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BlockThread extends Thread {
	private final static Logger logger = Logger.getLogger(BlockThread.class
			.getName());

	private static final int SLEEP_TIME = 250;
	// doBlock method calls internally
	private Block block;

	private Block result;

	Provider provider;

	public BlockThread(Block block, Provider provider) {
        super("[" + Thread.currentThread().getName() + "] Block-" + block.toString());
		this.block = block;
		this.provider = provider;
	}

	public void run() {
		logger.finer("*** Exertion dispatcher started with control context ***\n"
				+ block.getControlContext());
		try {
            String exertionDeploymentConfig = null;
			if (block.isProvisionable()) {
				try {
					exertionDeploymentConfig = 
							(String) provider.getProviderConfiguration().getEntry("sorcer.core.provider.ServiceProvider",
									"exertionDeploymentConfig", 
									String.class, 
									null);
				} catch (ConfigurationException e1) {
					logger.log(Level.WARNING, "Unable to read property from configuration", e1);
                } catch (RemoteException e) {
             				// ignore it, locall call
				}
			}
            Dispatcher dispatcher = ExertDispatcherFactory.getFactory().createDispatcher(block, provider, exertionDeploymentConfig);

			try {
				block.getControlContext().appendTrace(provider.getProviderName() +
						" dispatcher: " + dispatcher.getClass().getName());
			} catch (RemoteException e) {
				// ignore it, locall call
			}
			 int COUNT = 1000;
			 int count = COUNT;
			while (dispatcher.getState() != Exec.DONE
					&& dispatcher.getState() != Exec.FAILED
					&& dispatcher.getState() != Exec.SUSPENDED) {
				 count--;
				 if (count < 0) {
				 logger.finer("*** Concatenator's Exertion Dispatcher waiting in state: "
				 + dispatcher.getState());
				 count = COUNT;
				 }
				Thread.sleep(SLEEP_TIME);
			}
			logger.finer("*** Dispatcher exit state = " + dispatcher.getClass().getName()  + " state: " + dispatcher.getState()
					+ " for block***\n" + block.getControlContext());
            result = (Block) dispatcher.getExertion();
		} catch (DispatcherException de) {
			de.printStackTrace();
		} catch (InterruptedException ie) {
			ie.printStackTrace();
		}
	}

	public Block getBlock() {
		return block;
	}

	public Block getResult() {
		return result;
	}
}
