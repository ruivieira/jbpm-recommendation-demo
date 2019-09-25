# jbpm-recommendation-demo

Demo project for the recommendation service API in jBPM.

First we will go through the necessary steps to setup the demo and lastly we will look at some implementation details on how the recommendation API works.
This will allow you to learn how to create your own machine learning (ML) based recommendation services and how to integrate them with jBPM.

# Setup

## jBPM

Download and install jBPM from [here](https://www.jbpm.org/download/download.html).

## Recommendation service

This repository contain an example recommendation service implementation as a Maven module and a REST client to populate the project with task to allow the predictive model training.
Start by downloading, or alternatively cloning, the repository:

```$shell
$ git clone git@github.com:ruivieira/jbpm-recommendation-demo.git
```

For this demo, a random forest based service (using the [SMILE](https://github.com/haifengl/smile) library) will be used.
This service, which is in the Maven module located in `services/jbpm-recommendation-smile-random-forest`,
can be built with:

```shell
$ cd services/jbpm-recommendation-smile-random-forest
$ mvn clean install -T1C -DskipTests -Dgwt.compiler.skip=true -Dfindbugs.skip=true -Drevapi.skip=true -Denforcer.skip=true
```

The resulting JAR file can then be included in the Workbench's `kie-server.war` located in `standalone/deployments` directory of your jBPM server installation.

jBPM will search for a recommendation service with an identifier specified by a Java property named `org.jbpm.task.prediction.service`. Since in our demo, the random forest service has the indentifier `SMILERandomForest`, we can set this value before starting the workbench, for instance as an environment variable:

```shell
$ export JAVA_OPTS="-Dorg.jbpm.task.prediction.service=SMILERandomForest"
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



The project consists of a single Human Task, which can be inspected using the WB. The task is generic and simple enough in order to demonstrate the working of the jBPM's recommendation API.

![human_task](docs/images/human_task.png)

For the purposes of the demonstration, this task will be used to model a simple purchasing task. Where the purchase of a laptop of a certain brand is requested and must be eventually manually approved. The tasks **inputs** are:

- `item` - a `String` with the brand's name
- `level` - an `Integer` representing a dummy variable to add an extra feature to the model
- `ActorId` - a `String` representing the user requesting the purchase

The task provides as outputs:

- `approved` - a `Boolean` specifying whether the purchased was approved or not

###  Batch creation of tasks

This repository contains a REST client (under `client`) which allows to add Human Tasks in batch in order to have sufficient data points to train the model, so that we can have meaningful recommendations.

***NOTE***: Before running the REST client, make sure that the Workbench is running and the demo project is deployed and also running.

The class  `org.jbpm.recommendation.demo.RESTClient` performs this task and can be executed from the `client` directory with:

```shell
$ mvn exec:java -Dexec.mainClass="org.jbpm.recommendation.demo.RESTClient"
```

# Description

## API

jBPM offers an API which allows for predictive models to be trained with Human Tasks (HT) data and for HT to incorporate the model's predictions as outputs ore even complete a HT.

This is achieved by connecting the HT handling to a *recommendation service*. A recommendation service is simply any third-party class wich implements the `org.kie.internal.task.api.prediction.PredictionService` interface.

![api_diagram](docs/images/api.png)

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

![sequence](docs/images/sequence.png)

## Example service implementation

When creating and completing a batch of tasks (as previously) we are simultaneously training the predictive model.

The service implementation is based on a random forest model a popular ensemble learning method.

You will notice that all the tasks created and completed by the `RESTClient` have the input data of `John` as `ActorId`, `Lenovo` as the `item` and `5` as the `level`. However, half the tasks are completed as having `true` as `approved` and the other half having `false`. This is to illustrate the scenario where the prediction confidence is lower than the threshold.

In this service, the confidence threshold is set as `0.7` and, intuitively, we can expect that a predicted outcome for `John`, `Lenovo` and `5` would be either `true`or `false` with a confidence close to `0.5` (50% probability).

In fact, after the training is performed, if we create a.new task instance and provided the above input data, we will see that the task form recommends `true` with a confidence of `0.5`

![form](docs/images/form.png)