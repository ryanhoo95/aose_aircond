/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Environment;

import Actuator.AirConditioner;
import Sensor.TempSensor;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.util.Stack;

/**
 *
 * @author hb_ke
 */
public class Environment extends Agent{
    private EnvironmentGUI environmentGUI;
    private AID tempSensor;
    public static final String SERVICE_NAME = "environment";
    public static final String SERVICE_TYPE = "env";
    public static final String INFORM_TEMP = "inform_temp";

    
    protected void setup() {
        try {
            DFAgentDescription agent = new DFAgentDescription();
            agent.setName(getAID());
            ServiceDescription service = new ServiceDescription();
            service.setName(SERVICE_NAME);
            service.setType(SERVICE_TYPE);
            agent.addServices(service);
            
            DFService.register(this, agent);
        }
        catch(FIPAException ex) {
            System.out.println("[Environment] register error: " + ex.getMessage());
        }
        
        //setup environment GUI
        environmentGUI = new EnvironmentGUI(this);
        environmentGUI.display();
        
        //search temperature sensor for every 1s
        searchTempSensor();
        addBehaviour(new TickerBehaviour(this, 1000) {
           protected void onTick() {
               searchTempSensor();
           } 
        });
        
        //cyclic behaviour to inform temp sensor about current temp every 500ms
        addBehaviour(new CyclicBehaviour(this) {
           public void action() {
               ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
               msg.setConversationId(INFORM_TEMP);
               msg.setContent(Integer.toString(environmentGUI.getTemp()));
               
               if(tempSensor != null) {
                   msg.addReceiver(tempSensor);
                   send(msg); 
               }
               
               block(1000);
           } 
        });
        
        //cyclic behaviour to process request (update temp) from airconditioner
        addBehaviour(new CyclicBehaviour(this) {
           public void action() {
               ACLMessage msg = receive();
               if(msg != null) {
                   if(msg.getPerformative() == ACLMessage.REQUEST && msg.getConversationId().equals(AirConditioner.UPDATE_TEMP)) {
                       if(environmentGUI.getActionType().equals(EnvironmentGUI.INC_TEMP)) {
                           if(environmentGUI.getTemp() >= 45) {
                               //change the action to decrease temp
                               environmentGUI.setActionType(EnvironmentGUI.DEC_TEMP);
                           }
                           else {
                               //increment the temp by one
                               environmentGUI.increaseTempByOne();
                           }
                       }
                       else if(environmentGUI.getActionType().equals(EnvironmentGUI.DEC_TEMP)) {
                           if(environmentGUI.getTemp() <= 0) {
                               //change the action to increase temp
                               environmentGUI.setActionType(EnvironmentGUI.INC_TEMP);
                           }
                           else {
                               //decrement the temp by one
                               environmentGUI.decreaseTempByOne();
                           }
                       }
                   }
               }
               block();
           } 
        });
    }
    
    private void searchTempSensor() {
        try {
            String serviceType = TempSensor.SERVICE_TYPE;
            
            //build the desc for search constraints
            DFAgentDescription agent = new DFAgentDescription();
            ServiceDescription service = new ServiceDescription();
            service.setType(serviceType);
            agent.addServices(service);
            
            SearchConstraints search = new SearchConstraints();
            DFAgentDescription[] results = DFService.search(this, agent, search);
            
            if(results.length > 0) {
                tempSensor = results[0].getName();
            }
            else {
                System.out.println("[Environment] temp sensor not found.");
            }
        }
        catch(FIPAException ex) {
            System.out.println("[Environment] search temp sensor error: " + ex.getMessage());
        }
    }
    
}
