package org.jbpm.prediction.pmml;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates the model's output information.
 */
public class PMMLRandomForestConfiguration {

    private String outcomeName;
    private double confidenceThreshold;
    private List<String> inputFeatures = new ArrayList<>();
    private InputStream modelFile;

    public InputStream getModelFile() {
        return modelFile;
    }

    public void setModelFile(InputStream modelFile) {
        this.modelFile = modelFile;
    }

    /**
     * Returns the name of the output attribute
     *
     * @return The name of the output attribute
     */
    public String getOutcomeName() {
        return outcomeName;
    }

    public void setOutcomeName(String outcomeName) {
        this.outcomeName = outcomeName;
    }

    /**
     * Returns the confidence threshold to use for automatic task completion
     *
     * @return The confidence threshold, between 0.0 and 1.0
     */
    public double getConfidenceThreshold() {
        return confidenceThreshold;
    }

    public void setConfidenceThreshold(double confidenceThreshold) {
        this.confidenceThreshold = confidenceThreshold;
    }

    public List<String> getInputFeatures() {
        return inputFeatures;
    }

    public void setInputFeatures(List<String> inputFeatures) {
        this.inputFeatures = inputFeatures;
    }
}
