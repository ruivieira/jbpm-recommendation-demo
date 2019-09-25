package org.jbpm.prediction.randomforest;

import org.kie.api.task.model.Task;
import org.kie.internal.task.api.prediction.PredictionOutcome;
import org.kie.internal.task.api.prediction.PredictionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import smile.classification.RandomForest;
import smile.data.*;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.*;

public class SmileRandomForest extends AbstractPredictionEngine implements PredictionService {

    public static final String IDENTIFIER = "SMILERandomForest";
    private static final Logger logger = LoggerFactory.getLogger(SmileRandomForest.class);
    private final AttributeDataset dataset;
    private final Map<String, Attribute> smileAttributes;
    private final Attribute outcomeAttribute;
    private final int numAttributes;
    private final int numberTrees;
    protected List<String> attributeNames = new ArrayList<>();
    private RandomForest model = null;
    private Set<String> outcomeSet = new HashSet<>();

    private void println(String format, Object... args) {
        System.out.println("[SMILE RF]" + String.format(format, args));
    }

    public SmileRandomForest() {
        this(readConfigurationFromFile());
    }

    public SmileRandomForest(RandomForestConfiguration configuration) {
        this(configuration.getInputFeatures(),
                configuration.getOutcomeName(),
                configuration.getOutcomeType(),
                configuration.getConfidenceThreshold(),
                configuration.getNumTrees());
    }

    public SmileRandomForest(Map<String, AttributeType> inputFeatures,
                             String outputFeatureName,
                             AttributeType outputFeatureType,
                             double confidenceThreshold,
                             int numberTrees) {
        super(inputFeatures, outputFeatureName, outputFeatureType, confidenceThreshold);
        this.numberTrees = numberTrees;
        smileAttributes = new HashMap<>();
        for (Map.Entry<String, AttributeType> inputFeature : inputFeatures.entrySet()) {
            final String name = inputFeature.getKey();
            final AttributeType type = inputFeature.getValue();
            smileAttributes.put(name, createAttribute(name, type));
            attributeNames.add(name);
        }
        numAttributes = smileAttributes.size();
        outcomeAttribute = createAttribute(outputFeatureName, outputFeatureType);
        println("Stating the service with %s and %s attributes", numberTrees, numAttributes);
        dataset = new AttributeDataset("dataset", smileAttributes.values().toArray(new Attribute[numAttributes]), outcomeAttribute);
    }

    /**
     * Reads the random forest configuration from properties files.
     * "inputs.properties" should contain the input attribute names as keys and attribute types as values.
     *
     * @return A map of input attributes with the attribute name as key and attribute type as value.
     */
    private static RandomForestConfiguration readConfigurationFromFile() {

        final RandomForestConfiguration configuration = new RandomForestConfiguration();

        final Map<String, AttributeType> inputFeatures = new HashMap<>();
        inputFeatures.put("ActorId", AttributeType.NOMINAL);
        inputFeatures.put("level", AttributeType.NOMINAL);
        inputFeatures.put("item", AttributeType.NOMINAL);
        configuration.setInputFeatures(inputFeatures);

        configuration.setOutcomeName("approved");
        configuration.setOutcomeType(AttributeType.NOMINAL);
        configuration.setConfidenceThreshold(0.7);
        configuration.setNumTrees(100);

        return configuration;
    }

    private static Attribute createAttribute(String name, AttributeType type) {
        if (type == AttributeType.NOMINAL) {
            return new NominalAttribute(name);
        } else if (type == AttributeType.NUMERIC) {
            return new NumericAttribute(name);
        } else {
            return new StringAttribute(name);
        }
    }

    /**
     * Add the data provided as a map to a Smile {@link smile.data.Dataset}.
     *
     * @param data    A map containing the input attribute names as keys and the attribute values as values.
     * @param outcome The value of the outcome (output data).
     */
    public void addData(Map<String, Object> data, Object outcome) {
        final double[] features = new double[numAttributes];
        int i = 0;
        for (String attrName : smileAttributes.keySet()) {
            try {
                features[i] = smileAttributes.get(attrName).valueOf(data.get(attrName).toString());
            } catch (ParseException e) {
                e.printStackTrace();
            }
            i++;
        }
        try {
            final String outcomeStr = outcome.toString();
            outcomeSet.add(outcomeStr);
            dataset.add(features, outcomeAttribute.valueOf(outcomeStr));
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    /**
     * Build a set of features compatible with Smile's datasets from the map of input data
     *
     * @param data A map containing the input attribute names as keys and the attribute values as values.
     * @return A feature vector as a array of doubles.
     */
    protected double[] buildFeatures(Map<String, Object> data) {
        final double[] features = new double[numAttributes];
        for (int i = 0; i < numAttributes; i++) {
            final String attrName = attributeNames.get(i);
            try {
                features[i] = smileAttributes.get(attrName).valueOf(data.get(attrName).toString());
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return features;
    }

    /**
     * Returns the service's identifier
     *
     * @return The service identifier
     */
    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }

    /**
     * Returns a model prediction given the input data
     *
     * @param task      Human task data
     * @param inputData A map containing the input attribute names as keys and the attribute values as values.
     * @return A {@link PredictionOutcome} containing the model's prediction for the input data.
     */
    @Override
    public PredictionOutcome predict(Task task, Map<String, Object> inputData) {
        println("Predicting with input data: %s", inputData.toString());
        Map<String, Object> outcomes = new HashMap<>();
        if (outcomeSet.size() >= 2) {
            model = new RandomForest(dataset, this.numberTrees);
            final double[] features = buildFeatures(inputData);
            final double[] posteriori = new double[outcomeSet.size()];
            double prediction = model.predict(features, posteriori);

            String predictionStr = dataset.responseAttribute().toString(prediction);
            outcomes.put(outcomeAttribute.getName(), Boolean.valueOf(predictionStr));
            final double confidence = posteriori[(int) prediction];
            outcomes.put("confidence", confidence);

            println("prediction = %s, confidence = %f", predictionStr, confidence);
            logger.debug(inputData + ", prediction = " + predictionStr + ", confidence = " + confidence);

            return new PredictionOutcome(confidence, this.confidenceThreshold, outcomes);
        } else {
            outcomes.put("confidence", 0.0);
            return new PredictionOutcome(0.0, this.confidenceThreshold, outcomes);
        }
    }

    /**
     * Train the random forest model using data from the human task.
     *
     * @param task       Human task data
     * @param inputData  A map containing the input attribute names as keys and the attribute values as values.
     * @param outputData A map containing the output attribute names as keys and the attribute values as values.
     */
    @Override
    public void train(Task task, Map<String, Object> inputData, Map<String, Object> outputData) {
        println("Training with input data: %s", inputData.toString());
        println("Training with output data: %s", outputData.toString());

        addData(inputData, outputData.get(outcomeAttribute.getName()));
    }
}
