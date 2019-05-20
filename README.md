[![Codacy Badge](https://api.codacy.com/project/badge/Grade/3ba6416fd11d41fdaf281e7dab6042dc)](https://www.codacy.com/app/philwhiles/census-contact-centre-service?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=ONSdigital/census-contact-centre-service&amp;utm_campaign=Badge_Grade)
[![Build Status](https://travis-ci.com/ONSdigital/census-contact-centre-service.svg?branch=master)](https://travis-ci.com/ONSdigital/census-contact-centre-service)
[![codecov](https://codecov.io/gh/ONSdigital/census-contact-centre-service/branch/master/graph/badge.svg)](https://codecov.io/gh/ONSdigital/census-contact-centre-service)

# Contact Centre Data Service
This repository contains the Contact Centre Data service. This microservice is a RESTful web service implemented using [Spring Boot](http://projects.spring.io/spring-boot/). It manages contact centre data, where a Contact Centre Data object represents an expected response from the Contact Centre Data service, which provides all the data that is required by Contact Centre in order for it to verify the contact centre's UAC code and connect them to the relevant EQ questionnaire.

## Set Up

Do the following steps to set up the code to run locally:
* Install Java 11 locally
* Make sure that you have a suitable settings.xml file in your local .m2 directory
* Clone the census-contact-centre locally

## Running

There are two ways of running this service

* The first way is from the command line after moving into the same directory as the pom.xml:
    ```bash
    mvn clean install
    mvn spring-boot:run
    ```
* The second way requires that you first create a JAR file using the following mvn command (after moving into the same directory as the pom.xml):
    ```bash
    mvn clean package
    ```
This will create the JAR file in the Target directory. You can then right-click on the JAR file (in Intellij) and choose 'Run'.

## End Point

When running successfully version information can be obtained from the info endpoint
    
* localhost:8171/info
    
## Docker image build

Is switched off by default for clean deploy. Switch on with;

* mvn dockerfile:build -Dskip.dockerfile=false

    
## Copyright
Copyright (C) 2019 Crown Copyright (Office for National Statistics)

