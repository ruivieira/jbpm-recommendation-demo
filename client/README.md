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