package org.jbpm.prediction.randomforest;

import org.kie.internal.task.api.prediction.PredictionService;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;


public class RandomForestRegistry {
    private static final ServiceLoader<PredictionService> foundServices = ServiceLoader.load(PredictionService.class, RandomForestRegistry.class.getClassLoader());
    private String selectedService = System.getProperty("org.jbpm.prediction.randomforest", SmileRandomForest.IDENTIFIER);
    private Map<String, PredictionService> predictionServices = new HashMap<>();

    private RandomForestRegistry() {

        foundServices
                .forEach(strategy -> predictionServices.put(strategy.getIdentifier(), strategy));
    }

    public static RandomForestRegistry get() {
        return Holder.INSTANCE;
    }

    public PredictionService getService() {
        PredictionService predictionService = predictionServices.get(selectedService);
        if (predictionService == null) {
            throw new IllegalArgumentException("No prediction service was found with id " + selectedService);
        }

        return predictionService;
    }

    public synchronized void addStrategy(SmileRandomForest predictionService) {
        this.predictionServices.put(predictionService.getIdentifier(), predictionService);

    }

    private static class Holder {
        static final RandomForestRegistry INSTANCE = new RandomForestRegistry();
    }
}
