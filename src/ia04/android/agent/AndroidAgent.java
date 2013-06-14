package ia04.android.agent;

import java.util.logging.Level;

import android.content.Context;
import android.content.Intent;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.util.Logger;

public class AndroidAgent extends Agent implements SendToTableInterface {
	private static final long serialVersionUID = 1594371294421614291L;
	private Logger logger = Logger.getJADELogger(this.getClass().getName());

	protected Context context = null;
	private ACLMessage openMsg;
	private static final String TABLE_ID = "__table__";


	protected void setup() {
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName( getAID() ); 
		ServiceDescription sd  = new ServiceDescription();
		sd.setType( "smartphone" );
		sd.setName( getLocalName() );
		dfd.addServices(sd);

		try { DFService.register(this, dfd ); }
		catch (FIPAException fe) { fe.printStackTrace(); }
		Object[] args = getArguments();
		if (args != null && args.length > 0) {
			if (args[0] instanceof Context) {
				context = (Context) args[0];
			}
		}
		// Initialize the message used to open a file on a table
		openMsg = new ACLMessage(ACLMessage.REQUEST);
		openMsg.setConversationId(TABLE_ID);

		// Activate the Agent to GUI communication
		registerO2AInterface(SendToTableInterface.class, this);
		
		addBehaviour(new ReloadListener(this));
	}

	@Override
	public void sendToTable(String filePath, String tableName) {
		addBehaviour(new TableCommander(this, filePath, tableName));
	}

	/**
	 * Inner class ReloadListener.
	 * This behaviour keeps the webpage up to date by reloading it
	 * when being informed of an update by the ManagerAgent.
	 */
	class ReloadListener extends CyclicBehaviour {
		private static final long serialVersionUID = 741233963737842521L;

		ReloadListener(Agent a) {
			super(a);
		}
		public void action() {
			ACLMessage msg = myAgent.receive();
			if (msg != null) {
				if (msg.getPerformative() == ACLMessage.INFORM
						&& msg.getContent().equals("update")) {
					Intent broadcast = new Intent();
					broadcast.setAction("RELOAD_PAGE");
					logger.log(Level.INFO, "Sending broadcast " + broadcast.getAction());
					context.sendBroadcast(broadcast);
				} else {
					handleUnexpected(msg);
				}
			} else {
				block();
			}
		}
	} // END of inner class ReloadListener
	
	
	/**
	 * Inner class TableCommander.
	 * This behaviour REQUEST a table to open a file.
	 */
	private class TableCommander extends OneShotBehaviour {
		private static final long serialVersionUID = -1426033904935339194L;
		private String tableName;
		private String filePath;

		private TableCommander(Agent a, String filePath, String tableName) {
			super(a);
			this.tableName = tableName;
			this.filePath = filePath;
		}

		public void action() {
			AID aid = new AID();
			aid.setLocalName(tableName);
			
			openMsg.clearAllReceiver();
			openMsg.setContent(filePath);
			openMsg.addReceiver(aid);
			
			send(openMsg);
		}
	} // END of inner class TableCommander
	
	private void handleUnexpected(ACLMessage msg) {
		if (logger.isLoggable(Logger.WARNING)) {
			logger.log(Logger.WARNING, "Unexpected message received from "
					+ msg.getSender().getName());
			logger.log(Logger.WARNING, "Content is: " + msg.getContent());
		}
	}
}
