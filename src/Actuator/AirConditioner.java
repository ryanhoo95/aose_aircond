/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Actuator;

import Controller.Controller;
import Environment.Environment;
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
public class AirConditioner extends Agent {
    private static final String SERVICE_NAME = "air_conditioner";
    public static final String SERVICE_TYPE = "actuator";
    
    public static final String VERY_WARM = "Very Warm";
    public static final String WARM = "Warm";
    public static final String VERY_COOL = "Very Cool";
    public static final String COOL = "Cool";
    public static final String STAND_BY = "Stand By";
    
    public static final String UPDATE_TEMP = "update_temp";
    
    //default status is off
    private String status = STAND_BY;
    
    private AID environment;
    
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
            System.out.println("[AirConditioner] register error: " + ex.getMessage());
        }
        
        //search environment for every 1s
        searchEnvironment();
        addBehaviour(new TickerBehaviour(this, 1000) {
           protected void onTick() {
               searchEnvironment();
           } 
        });
        
        //cyclic behaviour to process inform (status) from controller
        addBehaviour(new CyclicBehaviour(this) {
           public void action() {
               ACLMessage msg = receive();
               
               if(msg != null && msg.getPerformative() == ACLMessage.INFORM
                  && msg.getConversationId().equals(Controller.UPDATE_STATUS)) {
                   status = msg.getContent();
               }
               block();
           } 
        });
        
        //cyclic behaviour to request environment update temp every 1s
        addBehaviour(new TickerBehaviour(this, 1000) {
           protected void onTick() {
               ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
               msg.setConversationId(UPDATE_TEMP);
               if(environment != null) {
                   msg.addReceiver(environment);
                   send(msg);
                   //System.out.println("[AirConditioner]: update temp");
               }
               
               //block(1000);
           } 
        });
    }
    
    private void searchEnvironment() {
        try {
            String serviceType = Environment.SERVICE_TYPE;
            
            //build the desc for search constraints
            DFAgentDescription agent = new DFAgentDescription();
            ServiceDescription service = new ServiceDescription();
            service.setType(serviceType);
            agent.addServices(service);
            
            SearchConstraints search = new SearchConstraints();
            DFAgentDescription[] results = DFService.search(this, agent, search);
            
            if(results.length > 0) {
                environment = results[0].getName();
            }
            else {
                System.out.println("[AirConditioner] environment not found.");
            }
        }
        catch(FIPAException ex) {
            System.out.println("[AirConditioner] search environment error: " + ex.getMessage());
        }
    }
}
