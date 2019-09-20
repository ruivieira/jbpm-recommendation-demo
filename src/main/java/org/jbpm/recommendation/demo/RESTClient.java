package org.jbpm.recommendation.demo;

import org.kie.server.api.marshalling.MarshallingFormat;
import org.kie.server.client.*;

import java.util.HashMap;
import java.util.Map;

public class RESTClient {
    private static final String URL = "http://localhost:8080/kie-server/services/rest/server";
    private static final MarshallingFormat FORMAT = MarshallingFormat.JSON;
    private static final String OWNER = "wbadmin";
    private static final String PASSWORD = "wbadmin";
    private static final String CONTAINER_ID = "recommendation6_1.0.0-SNAPSHOT";
    private static final String PROCESS_ID = "UserTask";
    private static final int ITERATIONS = 50;

    public RESTClient() {

    }

    public static void main(String[] args) {
        KieServicesConfiguration conf = KieServicesFactory.newRestConfiguration(URL, OWNER, PASSWORD);
        KieServicesClient kieServicesClient = KieServicesFactory.newKieServicesClient(conf);
        conf.setMarshallingFormat(FORMAT);
        UserTaskServicesClient userTaskServicesClient = kieServicesClient.getServicesClient(UserTaskServicesClient.class);

        for (int i = 0 ; i < ITERATIONS ; i++) {

            Map<String, Object> inputData = new HashMap<>();
            inputData.put("actor", "John");
            inputData.put("level", 5);
            inputData.put("item", "Lenovo");
            Map<String, Object> outputData = new HashMap<>();

            double random = Math.random();

            if (random < 0.9) {
                outputData.put("approved", true);
            } else {
                outputData.put("approved", false);
            }


            ProcessServicesClient processClient = kieServicesClient.getServicesClient(ProcessServicesClient.class);
            Long processId = processClient.startProcess(CONTAINER_ID, PROCESS_ID, inputData);

            userTaskServicesClient.releaseTask(CONTAINER_ID, processId, OWNER);
            userTaskServicesClient.claimTask(CONTAINER_ID, processId, OWNER);
            userTaskServicesClient.startTask(CONTAINER_ID, processId, OWNER);
            userTaskServicesClient.completeTask(CONTAINER_ID, processId, OWNER, outputData);
        }

        for (int i = 0 ; i < ITERATIONS ; i++) {

            Map<String, Object> inputData = new HashMap<>();
            inputData.put("actor", "John");
            inputData.put("level", 5);
            inputData.put("item", "Apple");
            Map<String, Object> outputData = new HashMap<>();

            double random = Math.random();

            if (random < 0.9) {
                outputData.put("approved", false);
            } else {
                outputData.put("approved", true);
            }


            ProcessServicesClient processClient = kieServicesClient.getServicesClient(ProcessServicesClient.class);
            Long processId = processClient.startProcess(CONTAINER_ID, PROCESS_ID, inputData);

            userTaskServicesClient.releaseTask(CONTAINER_ID, processId, OWNER);
            userTaskServicesClient.claimTask(CONTAINER_ID, processId, OWNER);
            userTaskServicesClient.startTask(CONTAINER_ID, processId, OWNER);
            userTaskServicesClient.completeTask(CONTAINER_ID, processId, OWNER, outputData);
        }

    }

}
