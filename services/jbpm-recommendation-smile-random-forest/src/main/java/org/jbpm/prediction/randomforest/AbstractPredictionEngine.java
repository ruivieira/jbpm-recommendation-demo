package org.jbpm.prediction.randomforest;

import java.util.Map;

abstract public class AbstractPredictionEngine {

    protected Map<String, AttributeType> inputFeatures;
    protected String outcomeFeatureName;
    protected AttributeType outcomeFeatureType;
    protected double confidenceThreshold;

    public AbstractPredictionEngine(Map<String, AttributeType> inputFeatures, String outputFeatureName, AttributeType outputFeatureType, double confidenceThreshold) {
        this.inputFeatures = inputFeatures;
        this.outcomeFeatureName = outputFeatureName;
        this.outcomeFeatureType = outputFeatureType;
        this.confidenceThreshold = confidenceThreshold;
    }

}
