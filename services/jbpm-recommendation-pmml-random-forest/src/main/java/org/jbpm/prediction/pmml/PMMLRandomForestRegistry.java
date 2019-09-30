package org.jbpm.prediction.pmml;

import org.kie.internal.task.api.prediction.PredictionService;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;


public class PMMLRandomForestRegistry {
    private static final ServiceLoader<PredictionService> foundServices = ServiceLoader.load(PredictionService.class, PMMLRandomForestRegistry.class.getClassLoader());
    private String selectedService = System.getProperty("org.jbpm.prediction.pmml", PMMLRandomForest.IDENTIFIER);
    private Map<String, PredictionService> predictionServices = new HashMap<>();

    private PMMLRandomForestRegistry() {

        foundServices
                .forEach(strategy -> predictionServices.put(strategy.getIdentifier(), strategy));
    }

    public static PMMLRandomForestRegistry get() {
        return Holder.INSTANCE;
    }

    public PredictionService getService() {
        PredictionService predictionService = predictionServices.get(selectedService);
        if (predictionService == null) {
            throw new IllegalArgumentException("No prediction service was found with id " + selectedService);
        }

        return predictionService;
    }

    public synchronized void addStrategy(PMMLRandomForest predictionService) {
        this.predictionServices.put(predictionService.getIdentifier(), predictionService);

    }

    private static class Holder {
        static final PMMLRandomForestRegistry INSTANCE = new PMMLRandomForestRegistry();
    }
}
