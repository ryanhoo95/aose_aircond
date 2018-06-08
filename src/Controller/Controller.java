/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Controller;

import Actuator.AirConditioner;
import Environment.Environment;
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

/**
 *
 * @author hb_ke
 */
public class Controller extends Agent{
    private ControllerGUI controllerGUI;
    private AID tempSensor;
    private AID airConditioner;
    
    private static final String SERVICE_NAME = "aircond_controller";
    private static final String SERVICE_TYPE = "controller";
    public static final String REQUEST_TEMP = "requestTemp";
    public static final String UPDATE_STATUS = "updateStatus";
    
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
        
        //setup controller GUI
        controllerGUI = new ControllerGUI(this);
        controllerGUI.display();
        
        //search for sensor and aircond every 1s
        searchTempSensor();
        searchAirConditioner();
        addBehaviour(new TickerBehaviour(this, 1000) {
           protected void onTick() {
               searchTempSensor();
               searchAirConditioner();
           } 
        });
        
        //cyclic behaviour to request temp from temp sensor every 1s
        //after get temp from temp sensor, update the status of airconditioner
        //then change the GUI based on status
        addBehaviour(new CyclicBehaviour(this) {
           public void action() {
               ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
               msg.setConversationId(REQUEST_TEMP);
               
               if(tempSensor != null) {
                   msg.addReceiver(tempSensor);
                   send(msg);
               }
               
               ACLMessage reply = receive();
               
               if(reply != null) {
                   int temp = Integer.parseInt(reply.getContent());
                   String status = AirConditioner.STAND_BY;
                   
                   //temp is between 0 - 9 (aircond need to blow every warm)
                   if(temp >= 0 && temp <= 9) {
                       status = AirConditioner.VERY_WARM;
                   }
                   //temp is between 10 - 19 (aircond need to blow warm)
                   else if(temp >= 10 && temp <= 19) {
                       status = AirConditioner.WARM;
                   }
                   //temp is between 20 - 25 (optiomal temp, aircond just standby)
                   else if(temp >= 20 && temp <= 25) {
                       status = AirConditioner.STAND_BY;
                   }
                   //temp is between 26 - 35 (aircond need to blow cool)
                   else if(temp >= 26 && temp <= 35) {
                       status = AirConditioner.COOL;
                   }
                   else if(temp >= 36 && temp <=45) {
                       status = AirConditioner.VERY_COOL;
                   }
                   
                   //send message to airconditioner to inform the status
                   ACLMessage statusMsg = new ACLMessage(ACLMessage.INFORM);
                   statusMsg.setConversationId(UPDATE_STATUS);
                   
                   if(airConditioner != null) {
                       statusMsg.addReceiver(airConditioner);
                       send(statusMsg);
                   }
                   
                   //update the GUI
                   controllerGUI.setTemp(temp);
                   controllerGUI.setStatus(status);
                   controllerGUI.setLight();
               }
               block(2000);
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
                System.out.println("[Controller] temp sensor not found.");
            }
        }
        catch(FIPAException ex) {
            System.out.println("[Controller] search sensor error: " + ex.getMessage());
        }
        
    }

    private void searchAirConditioner() {
        try {
            String serviceType = AirConditioner.SERVICE_TYPE;
            
            //build the desc for search constraints
            DFAgentDescription agent = new DFAgentDescription();
            ServiceDescription service = new ServiceDescription();
            service.setType(serviceType);
            agent.addServices(service);

            SearchConstraints search = new SearchConstraints();
            DFAgentDescription[] results = DFService.search(this, agent, search);

            if(results.length > 0) {
                airConditioner = results[0].getName();
            }
            else {
                System.out.println("[Controller] air conditioner not found.");
            }
        }
        catch(FIPAException ex) {
            System.out.println("[Controller] search sensor error: " + ex.getMessage());
        }
    }
}
