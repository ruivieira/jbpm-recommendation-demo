package org.jbpm.recommendation.demo;

import com.sun.org.apache.regexp.internal.RE;
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
    private static final int ITERATIONS = 50;
    private KieServicesConfiguration conf;
    private KieServicesClient kieServicesClient;
    private UserTaskServicesClient userTaskServicesClient;

    public RESTClient() {

        this.conf = KieServicesFactory.newRestConfiguration(URL, OWNER, PASSWORD);
        this.kieServicesClient = KieServicesFactory.newKieServicesClient(conf);
        this.conf.setMarshallingFormat(FORMAT);
        this.userTaskServicesClient = kieServicesClient.getServicesClient(UserTaskServicesClient.class);
    }

    public void addTask(String actor, int level, String item, Boolean approved) {
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("actor", actor);
        inputData.put("level", level);
        inputData.put("item", item);
        Map<String, Object> outputData = new HashMap<>();
        outputData.put("approved", approved);

        ProcessServicesClient processClient = kieServicesClient.getServicesClient(ProcessServicesClient.class);
        Long processId = processClient.startProcess(CONTAINER_ID, PROCESS_ID, inputData);

        System.out.println(String.format("Starting and completing task #%s (user=%s, level=%s, item=%s, approved=%s)",
                processId, actor, level, item, approved));

        userTaskServicesClient.releaseTask(CONTAINER_ID, processId, OWNER);
        userTaskServicesClient.claimTask(CONTAINER_ID, processId, OWNER);
        userTaskServicesClient.startTask(CONTAINER_ID, processId, OWNER);
        userTaskServicesClient.completeTask(CONTAINER_ID, processId, OWNER, outputData);
    }

    public static void main(String[] args) {

        final RESTClient client = new RESTClient();

        for (int i = 0 ; i < ITERATIONS ; i++) {
            client.addTask("John", 5, "Lenovo", true);
            client.addTask("John", 5, "Lenovo", false);
            client.addTask("John", 5, "Apple", true);
            client.addTask("John", 5, "Apple", false);
        }
    }

}
