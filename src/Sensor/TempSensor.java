/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Sensor;

import Controller.Controller;
import Environment.Environment;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;

/**
 *
 * @author hb_ke
 */
public class TempSensor extends Agent{
    public static final String SERVICE_NAME = "temp_sensor";
    public static final String SERVICE_TYPE = "sensor";
    
    //default temp
    private int temp = 20;
    
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
            System.out.println("[TempSensor] register error: " + ex.getMessage());
        }
        
        //cyclic behaviour to process request (inform or request temp)
        addBehaviour(new CyclicBehaviour(this) {
           public void action() {
               ACLMessage msg = receive();
               
               if(msg != null) {
                   //inform temp from environment
                   if(msg.getPerformative() == ACLMessage.INFORM && msg.getConversationId().equals(Environment.INFORM_TEMP)) {
                       temp = Integer.parseInt(msg.getContent());
                   }
                   //request from controller (to get temp)
                   else if(msg.getPerformative() == ACLMessage.REQUEST && msg.getConversationId().equals(Controller.REQUEST_TEMP)) {
                       ACLMessage reply = msg.createReply();
                       reply.setPerformative(ACLMessage.INFORM);
                       reply.setContent(Integer.toString(temp));
                       send(reply);
                   }
               }
           } 
        });
    }
}
