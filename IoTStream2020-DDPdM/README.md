# Code for the Data Driven Predictive Maintenance Workshop of IoT Stream 2020 with the title "AutoML for Predictive Maintenance: One Tool to RUL them all"

This repository holds the code for the Data Driven Predictive Maintenance Workshop submission of IoT Stream 2020 with the title 
"AutoML for Predictive Maintenance: One Tool to RUL them all" by Tanja Tornede, Alexander Tornede, Marcel Wever, Felix Mohr and Eyke Hüllermeier. 

## Installation
* Clone this repository
* Check out locally [commit](https://github.com/tornede/AILibs/commit/e0f62ec9a31d417c5435a650c06c93467b9315c6) of tornede/AILibs
* Setup a singularity container with the given [recipe](https://github.com/tornede/IoTStreamPdM2020Workshop/blob/master/IoTStream2020-DDPdM/conf/cluster/mlplan_pdm_singularity.recipe)

## Reproduction of Results
In order to reproduce the results, we assume you to have a MySQL database server running. We also assume the database settings to be written in a file in __conf/experiments/db.conf__, which is of the following form:

```
db.driver = mysql
db.host = {URL}
db.username = {USERNAME}
db.password = {PASSWORD}
db.database = {DATABASE_NAME}
db.table = {TABLE_NAME}
db.ssl = {TRUE/FALSE}
```

First you have to execute __DatabaseTableSetup.java__, to setup the table in the database with the experiments. After this was done, make sure that the columns __finalpipeline__ and __exception__ are of the type LONGTEXT. Then you can start the experiments using __Experimenter.java__ with three parameters: 

1. subtaskId: String. This is used for parallel execution on the cluster. If you don't run it in parallel, a simple string can be given here. It has no further meaning.
2. totalTimeout: Long. This timeout (in minutes) is used to decide how many different experiments should be executed. We started the script for each single experiment. This timeout should always be a bit higher than the search timeout, as the final pipeline has to be evaluated. We suggest to add an additional hour (300 minutes).
3. searchTimeout: Long. This timeout (in minutes) is used for ML-Plan-RUL or RS to terminate. In this paper we used 4h (240 minutes). 

Doing so, will run all experiments and store the associated results in a table described in the configuration file. The structure of the generated table should be self-explanatory. In order to see an aggregated form of the results, we recommend grouping by __search_algorithms__, __dataset__ and __internal_performance_measure__ while averaging over __performance_asymmetric_loss__ or __performance_mean_absolute_percentage_error__.
