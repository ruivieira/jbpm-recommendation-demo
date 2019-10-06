package org.jbpm.recommendation.demo;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.kie.server.api.marshalling.MarshallingFormat;
import org.kie.server.client.*;

import java.util.HashMap;
import java.util.Map;

public class RESTClient {
    private static final String URL = "http://localhost:8080/kie-server/services/rest/server";
    private static final MarshallingFormat FORMAT = MarshallingFormat.JSON;
    private static final String OWNER = "wbadmin";
    private static final String PASSWORD = "wbadmin";
    private static final String CONTAINER_ID = "recommendation-demo_1.0.0-SNAPSHOT";
    private static final String PROCESS_ID = "UserTask";
    private static final int ITERATIONS = 200;
    private KieServicesConfiguration conf;
    private KieServicesClient kieServicesClient;
    private UserTaskServicesClient userTaskServicesClient;

    public RESTClient() {

        this.conf = KieServicesFactory.newRestConfiguration(URL, OWNER, PASSWORD);
        this.kieServicesClient = KieServicesFactory.newKieServicesClient(conf);
        this.conf.setMarshallingFormat(FORMAT);
        this.userTaskServicesClient = kieServicesClient.getServicesClient(UserTaskServicesClient.class);
    }

    public void addTask(String actor, double price, String item, Boolean approved) {
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("actor", actor);
        inputData.put("price", price);
        inputData.put("item", item);
        Map<String, Object> outputData = new HashMap<>();
        outputData.put("approved", approved);

        ProcessServicesClient processClient = kieServicesClient.getServicesClient(ProcessServicesClient.class);
        Long processId = processClient.startProcess(CONTAINER_ID, PROCESS_ID, inputData);

        System.out.println(String.format("Starting and completing task #%s (user=%s, price=%s, item=%s, approved=%s)",
                processId, actor, price, item, approved));

        userTaskServicesClient.releaseTask(CONTAINER_ID, processId, OWNER);
        userTaskServicesClient.claimTask(CONTAINER_ID, processId, OWNER);
        userTaskServicesClient.startTask(CONTAINER_ID, processId, OWNER);
        userTaskServicesClient.completeTask(CONTAINER_ID, processId, OWNER, outputData);
    }

    public static void main(String[] args) {

        final RESTClient client = new RESTClient();

        // Purchases for Lenovos by John, using a price distribution of price ~ N(1500, 40)
        final NormalDistribution lenovoPrice = new NormalDistribution(1500.0, 40.0);
        final NormalDistribution applePrice = new NormalDistribution(2500.0, 40.0);
        for (int i = 0 ; i < ITERATIONS ; i++) {
                client.addTask("John", lenovoPrice.sample(), "Lenovo", true);
                client.addTask("Mary", applePrice.sample(), "Apple", true);
                client.addTask("Mary", lenovoPrice.sample(), "Lenovo", true);
                client.addTask("John", applePrice.sample(), "Apple", true);
        }
        for (int i = 0 ; i < ITERATIONS ; i++) {
            client.addTask("John", applePrice.sample(), "Lenovo", false);
            client.addTask("Mary", applePrice.sample(), "Lenovo", false);
        }

    }

}
