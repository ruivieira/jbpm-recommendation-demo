# jbpm-recommendation-demo

Demo project for the recommendation service API in jBPM.

First we will go through the necessary steps to setup the demo and lastly
we will look at some implementation details on how the recommendation API works.
This will allow you to learn how to create your own machine learning (ML) based
recommendation services and how to integrate them with jBPM.

# Setup

## jBPM

Download and install jBPM from [here](https://www.jbpm.org/download/download.html).

## Recommendation service

Download the example prediction service backends from here.
Alternatively, clone the repository:
```$shell
git clone git@github.com:ruivieira/kie-jpmml-integration.git
```

For this demo, the SMILE-based random forest service will be used.
The service, which is in the Maven module `jbpm-recommendation-smile-random-forest`,
can be built with:

```shell
cd jbpm-recommendation-smile-random-forest
mvn clean install -T1C -DskipTests -Dgwt.compiler.skip=true -Dfindbugs.skip=true -Drevapi.skip=true -Denforcer.skip=true
```

## Installing the project

From the Workbench (WB) select "_Import project_" and use the project git URL:

```shell
https://github.com/ruivieira/jbpm-recommendation-demo-project.git
```

## Running the project

Start the WB by running

```
./bin/standalone.sh
```

Add the HT in bulk by running the REST client in `RESTClient`.



# Description

## API

jBPM offers an API which allows for predictive models to be trained with Human Tasks (HT) data and for HT to incorporate the model's predictions as outputs ore even complete a HT.

This is achieved by connecting the HT handling to a *recommendation service*. A recommendation service is simply any third-party class wich implements the `org.kie.internal.task.api.prediction.PredictionService` interface.

This interface consists of three methods:

- `getIdentifier()` - this methods simply returns a unique (`String`) identifier for your prediction service
- `predict(Task task, Map<String, Object> inputData)` - this method takes task information and the task's inputs from which we will derive the model's inputs, as a map. The method returns a `PredictionOutcome` instance, which we will look in closer detail later on
- `train(Task task, Map<String, Object> inputData, Map<String, Object> outputData)` - this method, similarly to `predict`, takes task info and the task's inputs, but now we also need to provide the task's outputs, as a map, for training

It is important to note that the prediction service makes no assumptions about which features will be used for model training and prediction. The API exposes the task information, inputs and outputs, but it is up to the developer/data scientist to select which inputs and outputs will be used for training, or if pre-processing is necessary, for instance. 

The `PredictionOutcome` is a class which encapsulates the model's prediction for a certain `Map<String, Object> inputData`.

This class will contain:

- A `Map<String, Object> outcome` containing the prediction outputs, each entry represents a output attribute name and value. This map can be empty, which corresponds to the model not providing any prediction.
- A `confidence` value. The meaning of this field is left to the developer. As an example, it could represent a probability between `0.0` and `1.0`. It's relevance is related to the `confidenceThreshold` below.
- A `confidenceThreshold` - this value represents the `confidence` cutoff after which an action can be taken by the HT item handler.

As example, let's assume our `confidence` represents a prediction probability between `0.0` and `1.0`. If the `confidenceThreshold` is `0.7`, that would mean that for ` confidence > 0.7` the HT outputs would be set to the `outcome` and the task automatically closed. If the `confidence < 0.7`, then the HT would set the prediction `outcome` as suggested values, but the task would not be closed and still need human interaction. If the `outcome` is empty, then the HT lifecycle would proceed as if no prediction was made.