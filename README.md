# microservice-ecommerce-customers

## Introduction
This is a Spring Boot microservice project for the **eCommerce Payments Backend Service**.\
The service offers RESTful endpoints for incoming CRUD operations from the outside.\
It "talks" internally with other eCommerce services via event-driven async communication
using a Kafka broker.

Since this is an educational / portfolio project, it doesn't implement any kind of authentication.\
The project is meant to run exclusively on a local environment.

The customers database tables are being filled with customer data (name, address, payment infos) by a SQL
script. The script runs on application startup.\
The [H2 Console Web-App](http://localhost:9010/h2-console) can be opened in your internet browser.

For further information, please go to the documentation in the
[microservice-ecommerce-orders](https://github.com/wagner-wutzke/microservice-ecommerce-orders/README.md)
project.

## Running with Docker
In order to create a docker image and run it locally, execute the following commands:
```
mvn package -DskipTests
```
```
docker build -t payments-service:local .
```
```
docker network create ecommerce-net
```
```
docker run --rm --name payments-service \
  --network ecommerce-net \
  -e KAFKA_BOOTSTRAP_SERVERS=kafka:9092 \
  -p 9030:9030 \
  payments-service:local
```
Please be aware of the port being used in the docker command (9030) and the one configured in the
[application.yml](src/main/resources/application.yml) file within the source code. They must be the same.


## Database
The project is using the pattern **one-database-per-service**.\
It uses a file based **H2** database for simplicity matters, since this is only a portfolio project.\
Tables are created and updated on application start up.
