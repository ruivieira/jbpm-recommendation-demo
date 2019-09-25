package org.jbpm.prediction.randomforest;

import org.jbpm.services.api.model.DeploymentUnit;
import org.jbpm.test.services.AbstractKieServicesTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.kie.api.task.model.TaskSummary;
import org.kie.internal.query.QueryFilter;

import java.util.*;

import static org.junit.Assert.*;

public class RandomForestPredictionServiceProcessTest extends AbstractKieServicesTest {

    private List<Long> instances = new ArrayList<>();

    private static void assertBetween(double value, double min, double max) {
        assertTrue(value > min && value < max);
    }

    @BeforeClass
    public static void setupOnce() {
        System.setProperty("org.jbpm.task.prediction.service", SmileRandomForest.IDENTIFIER);
    }

    @AfterClass
    public static void cleanOnce() {
        System.clearProperty("org.jbpm.task.prediction.service");
    }

    @Override
    protected List<String> getProcessDefinitionFiles() {
        List<String> processes = new ArrayList<>();
        processes.add("BPMN2-UserTask.bpmn2");
        return processes;
    }

    @Override
    public DeploymentUnit prepareDeploymentUnit() throws Exception {
        // specify GROUP_ID, ARTIFACT_ID, VERSION of your kjar
        return createAndDeployUnit("org.jbpm.test.prediction", "random-forest-test", "1.0.0");
    }


    /**
     * For this test insert a quantity of true training samples
     * to verify the random forest class/process is functional.
     * Expect confidence > 90.0 and "approved" to be true.
     */
    @Test
    public void testRepeatedRandomForestPredictionService() {
        Map<String, Object> outputs;
        outputs = startAndReturnTaskOutputData("test item", "john", 5, false);
        for (int i = 0; i < 20; i++) {
            outputs = startAndReturnTaskOutputData("test item", "john", 5, true);
        }
        assertTrue((double) outputs.get("confidence") > 0.9);
        assertEquals("true", outputs.get("approved"));
    }

    /**
     * Insert an equal number of true and false samples, making
     * sure the total number of samples is larger than the dataset
     * size threshold. Since the dataset size
     * threshold will have been met and the probability of true
     * and false will be nearly equal, we expect confidence to be
     * lower than 0.55 (55%).
     */
    @Test
    public void testEqualProbabilityRandomForestPredictionService() {
        Map<String, Object> outputs = new HashMap<>();

        for (int i = 0; i < 100; i++) {
            startAndReturnTaskOutputData("test item", "john", 5, false);
            outputs = startAndReturnTaskOutputData("test item", "john", 5, true);
        }

        final double confidence = (double) outputs.get("confidence");
        assertTrue(confidence < 0.55 && confidence > 0.45);
    }

    /**
     * Insert a disproportionate partitioning of true and false samples
     * of a sample set larger than the dataset size threshold. In this
     * case true will have higher probability and as such we expect
     * confidence to be high.
     */
    @Test
    public void testUnequalProbabilityRandomForestPredictionService() {
        Map<String, Object> outputs;

        outputs = startAndReturnTaskOutputData("test item", "john", 5, true);
        for (int i = 0; i < 10; i++) {
            outputs = startAndReturnTaskOutputData("test item", "john", 5, false);
        }
        for (int i = 0; i < 90; i++) {
            outputs = startAndReturnTaskOutputData("test item", "john", 5, true);
        }

        assertEquals("true", outputs.get("approved"));
    }

    /**
     * This test shows how after testing with a mixed set (3x false, 17x true) how prediction (and probability)
     * evolve when you then keep sending as "false" value to the outcome.
     */
    @Test
    public void test1() {
        Map<String, Object> outputs = new HashMap<>();

        for (int i = 0; i < 17; i++) {
            outputs = startAndReturnTaskOutputData("test item", "john", 5, true);
        }
        for (int i = 0; i < 40; i++) {
            outputs = startAndReturnTaskOutputData("test item", "john", 5, false);
        }
        assertTrue((double) outputs.get("confidence") > 0.5);
        assertEquals("false", outputs.get("approved"));

    }

    /**
     * shows how after passing min count of 2 input, accuracy goes to > 0.95 (95%) very quickly.
     */
    @Test
    public void test2() {
        Map<String, Object> outputJohn = new HashMap<>();
        Map<String, Object> outputMary = new HashMap<>();

        for (int i = 0; i < 30; i++) {
            outputJohn = startAndReturnTaskOutputData("test item", "john", 5, false);
            outputMary = startAndReturnTaskOutputData("test item", "mary", 5, true);
        }

        assertTrue((double) outputJohn.get("confidence") > 0.5);
        assertEquals("false", outputJohn.get("approved"));
        assertTrue((double) outputMary.get("confidence") < 0.5);
        assertEquals("false", outputMary.get("approved"));
    }

    /**
     * This test shows how after passing min count of 2 input with 1 irrelevant param switching between 5 possible
     * values, it takes a while longer to get to high accuracy
     */
    @Test
    public void test3() {
        Map<String, Object> outputs = new HashMap<>();

        for (int i = 0; i < 50; i++) {
            outputs = startAndReturnTaskOutputData("test item", "john", i % 5, false);
            startAndReturnTaskOutputData("test item", "mary", i % 5, true);
        }
        assertBetween((double) outputs.get("confidence"), 0.5, 1.0);
        assertEquals("false", outputs.get("approved"));
    }

    /**
     * This test shows how after passing min count of 2 input with 1 irrelevant param switching between 5 possible
     * values, accuracy of completely new input is extremely high.
     */
    @Test
    public void test5() {
        Map<String, Object> outputs;

        for (int i = 0; i < 50; i++) {
            startAndReturnTaskOutputData("test item", "john", i % 5, false);
            startAndReturnTaskOutputData("test item", "mary", i % 5, true);
        }
        startAndReturnTaskOutputData("test item2", "krisv", 10, true);
        startAndReturnTaskOutputData("test item2", "krisv", 11, false);
        startAndReturnTaskOutputData("test item2", "krisv", 11, false);
        startAndReturnTaskOutputData("test item2", "krisv", 10, true);
        startAndReturnTaskOutputData("test item2", "krisv", 10, false);
        startAndReturnTaskOutputData("test item", "john", 5, false);
        outputs = startAndReturnTaskOutputData("test item2", "krisv", 10, true);

        assertBetween((double) outputs.get("confidence"), 0.4, 0.6);
        assertEquals("false", outputs.get("approved"));
    }

    @Test
    public void test6() {
        Map<String, Object> outputs;

        for (int i = 0; i < 100; i++) {
            startAndReturnTaskOutputData("test item", "john", i % 5, !(new Random().nextDouble() < 0.9));
            startAndReturnTaskOutputData("test item", "mary", i % 5, new Random().nextDouble() < 0.9);
            startAndReturnTaskOutputData("test item", "mary", i % 3, new Random().nextDouble() < 0.9);
        }
        startAndReturnTaskOutputData("test item2", "krisv", 10, true);
        startAndReturnTaskOutputData("test item2", "krisv", 11, false);
        startAndReturnTaskOutputData("test item2", "krisv", 11, false);
        startAndReturnTaskOutputData("test item2", "krisv", 10, true);
        startAndReturnTaskOutputData("test item2", "krisv", 10, false);
        startAndReturnTaskOutputData("test item", "john", 5, false);
        outputs = startAndReturnTaskOutputData("test item2", "krisv", 10, true);
        assertBetween((double) outputs.get("confidence"), 0.4, 0.6);
    }

    protected Map<String, Object> startAndReturnTaskOutputData(String item, String userId, Integer level, Boolean approved) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("item", item);
        parameters.put("level", level);
        parameters.put("actor", userId);
        long processInstanceId = processService.startProcess(deploymentUnit.getIdentifier(), "UserTask", parameters);
        instances.add(processInstanceId);

        List<TaskSummary> tasks = runtimeDataService.getTasksByStatusByProcessInstanceId(processInstanceId, null, new QueryFilter());
        assertNotNull(tasks);

        if (!tasks.isEmpty()) {

            Long taskId = tasks.get(0).getId();

            Map<String, Object> outputs = userTaskService.getTaskOutputContentByTaskId(taskId);
            assertNotNull(outputs);

            userTaskService.completeAutoProgress(taskId, userId, Collections.singletonMap("approved", approved));

            return outputs;
        }

        return null;
    }
}
