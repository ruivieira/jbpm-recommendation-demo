package org.jbpm.prediction.pmml;

import org.kie.api.task.model.Task;
import org.kie.internal.task.api.prediction.PredictionOutcome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.util.*;

public class PMMLRandomForest extends AbstractPMMLBackend {

    public static final String IDENTIFIER = "PMMLRandomForest";

    private static final Logger logger = LoggerFactory.getLogger(PMMLRandomForest.class);

    /**
     * Reads the PMML model configuration from a properties files.
     * "inputs.properties" should contain the input attribute names as keys and (optional) attribute types as values
     * "output.properties" should contain the output attribute name and the confidence threshold
     * "model.properties" should contain the location of the PMML model
     * @return A map of input attributes with the attribute name as key and attribute type as value
     */
    private static PMMLRandomForestConfiguration readConfigurationFromFile() {

        final PMMLRandomForestConfiguration configuration = new PMMLRandomForestConfiguration();

        final List<String> inputFeatures = new ArrayList<>();
        inputFeatures.add("ActorId");
        inputFeatures.add("price");
        inputFeatures.add("item");

        configuration.setInputFeatures(inputFeatures);

        configuration.setOutcomeName("approved");
        configuration.setConfidenceThreshold(1.0);

        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            InputStream input = classLoader.getResourceAsStream("/META-INF/models/random_forest.pmml");

            configuration.setModelFile(input);
        } catch (Exception e) {
        logger.error("Exception: " + e);
    }

        System.out.println("Loaded model successfully.");
        return configuration;
    }

    public PMMLRandomForest() {
        this(readConfigurationFromFile());
    }

    public PMMLRandomForest(PMMLRandomForestConfiguration configuration) {
        this(configuration.getInputFeatures(), configuration.getOutcomeName(), configuration.getConfidenceThreshold(), configuration.getModelFile());
    }

    public PMMLRandomForest(List<String> inputFeatures,
                            String outputFeatureName,
                            double confidenceThreshold,
                            InputStream pmmlFile) {
        super(inputFeatures, outputFeatureName, confidenceThreshold, pmmlFile);
    }

    /**
     * Returns the processed data (e.g. perform categorisation, etc). If no processing is needed, simply return
     * the original data.
     *
     * @param data A map containing the input data, with attribute names as key and values as values.
     * @return data A map containing the processed data, with attribute names as key and values as values.
     */
    @Override
    protected Map<String, Object> preProcess(Map<String, Object> data) {
        return data;
    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }

    /**
     * Returns a model prediction given the input data
     *
     * @param task Human task data
     * @param data A map containing the input attribute names as keys and the attribute values as values.
     * @return A {@link PredictionOutcome} containing the model's prediction for the input data.
     */
    @Override
    public PredictionOutcome predict(Task task, Map<String, Object> data) {
        Map<String, ?> result = evaluate(data);
        System.out.println(result);
        Map<String, Object> outcomes = new HashMap<>();

        String prediction = (String) result.get(outcomeFeatureName);
        double confidence = (Double) result.get(String.format("probability(%s)", prediction));

        outcomes.put("approved", Boolean.valueOf(prediction));
        outcomes.put("confidence", confidence);

        System.out.println(data + ", prediction = " + prediction + ", confidence = " + confidence);

        return new PredictionOutcome(0.0, this.confidenceThreshold, outcomes);
    }
}
