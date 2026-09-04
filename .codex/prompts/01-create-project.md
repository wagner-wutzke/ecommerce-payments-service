---
name: create-kafka-rest-microservice
description: Create or complete a Java Maven Spring Boot microservice with REST CRUD endpoints,
  JPA/H2 persistence, Kafka producer and consumer, externalized configuration,
  DLQ handling, and isolated tests. Use for event-driven REST services whose domain objects are
  provided in the maven dependency `net.wowdev.ecommerce:domain`.
---

# Create a Kafka + REST microservice

Create a production-shaped Java Maven service in the current workspace. Inspect before editing,
preserve unrelated changes, and follow repository conventions.

## 1. Resolve inputs before coding

Read, in order:
1. Repository-local instructions (`AGENTS.md`, `.codex/AGENTS.md`, and equivalent files).
2. `.codex/config.toml`, when present; use it for project defaults.
3. `pom.xml`, Java/source layout, tests, resources, and existing configuration.

Request values override repository configuration.
If a required value remains ambiguous, state the chosen value before editing. Use these variables:
- `project_name`: Maven artifact and service name.
- `package_name`: valid Java root package.
- `domain_object`: singular Java entity name, such as `Product`; derive `domain_object_lower` for
  paths.
- `app_port`: positive HTTP port; default `9000`.
- `kafka_bootstrap_servers`: broker list; default `localhost:9092`.
- `kafka_pub_topic`: publication topic; default `${domain_object_lower}s-topic`.
- `kafka_sub_topic`: subscription topic; default `${domain_object_lower}s-topic`.

## 2. Build and project structure

Use Maven and the repository's Java/Spring Boot versions. For a new service, use this layered
structure:

```text
${package_name}
├── ${domain_object}sServiceApplication.java
├── config/                 # Kafka, Jackson, persistence, auditing, and properties
├── controller/             # REST endpoints and HTTP error mapping
├── messaging/              # producer, consumer
├── repository/
└── service/
```

Maven Properties to be used:

- java.version: 21
- jacoco.version: 0.8.13
- jackson.jsr310.version: 2.20.1
- spring-boot-starter-parent: 4.1.0

Maven project identification

- groupId: `net.wowdev.ecommerce`
- artifactId: `${domain_object}s-service`
- version: 1.0.0-SNAPSHOT
- name `${domain_object}s-service`
- description: `${domain_object}s-service` microservice.

Preserve compatible dependencies and annotation-processor configuration already present. For a new
project, include Spring Web, Data JPA, Validation, H2, H2 console, Spring Kafka, Lombok and
test dependencies.

Do not add versions or dependencies without checking the current `pom.xml`; use stable versions
compatible with the project rather than blindly selecting “latest”.

Add a maven dependency to `net.wowdev.ecommerce:domain:1.0.0-SNAPSHOT` in the `pom.xml`.
It contains all domain class definitions: Entities, DTOs, Mappers and events definitions.

## 3. REST API

Create `${package_name}.controller.${domain_object}Controller` at `/api/v1/${domain_object_lower}s`
with:

- `GET /{id}` lookup;
- `GET /` pagination using `page` default `0` and `pageSize` default `20`, sorted by `createdAt`
  descending;
- `POST` returning `201 Created` and a `Location` header;
- `PUT /{id}` returning the updated representation;
- `DELETE /{id}` returning `204 No Content`.

Use `@Valid`, explicit constraints, consistent 400 responses, and `ProblemDetail` or the
repository's established
equivalent for 404 and other errors. Validate page parameters and test valid, invalid, missing, and
update paths.

## 4. Kafka and configuration

- Keep broker addresses, topics, consumer groups, database settings, and credentials in
  `application.yml`. Never hardcode them in annotations or business logic.
- Configure a Producer class for the change events on domain objects, using the entity UUID as the stable
  key and
  `enable.idempotence=true`, `acks=all`, and bounded retries `3`. It shall produce events on topic
  configured at `app.kafka.${domain-object}s-topic` variable in `application.yml`.
- Do not publish before the database commit. Prefer an application event handled by
  `@TransactionalEventListener(phase = AFTER_COMMIT)` or use an explicitly documented transactional
  outbox.
- Configure a Consumer class for receiving events on the configured
  `app.kafka.${domain-object}s-topic` topic. Leave its
  implementation open. The class shall be annotated with `@Slf4j`, `@Component`,
  `@RequiredArgsConstructor` and the annotation below:

```java
@KafkaListener(
    groupId ="${spring.kafka.consumer.group-id}",
    topics = {},
    containerFactory ="kafkaListenerContainerFactory")
```

- The Consumer implementation must contain this method:
```java
@KafkaHandler(isDefault = true)
public void handleUnknown(Object event) {
  log.debug(">> Received an unmapped event of type {}", event.getClass().getSimpleName());
}
```

- Add producer and consumer unit tests in separate classes that do not require a live broker.
- Use embedded Kafka/Testcontainers only for an explicitly requested integration test.
- Add the `@EnableKafka` to the `KafkaConfig` file in order to activate the consumers.
- Use the following class as Sprig Configuration for Kafka:

```java
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties.AckMode;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@EnableKafka
public class KafkaConfig {

  @Value("${spring.kafka.consumer.group-id}")
  private String consumerGroup;

  @Value("${spring.kafka.bootstrap-servers}")
  private String bootstrapServers;

  @Value("${spring.kafka.producer.acks:all}")
  private String acks;

  @Value("${spring.kafka.producer.properties.delivery.timeout.ms:30000}")
  private String deliveryTimeout;

  @Value("${spring.kafka.producer.properties.linger.ms:0}")
  private String linger;

  @Value("${spring.kafka.producer.properties.request.timeout.ms:10000}")
  private String requestTimeout;

  @Value("${spring.kafka.producer.properties.enable.idempotence:true}")
  private boolean idempotence;

  @Value("${spring.kafka.producer.retries:3}")
  private Integer retries;

  @Value("${spring.kafka.producer.properties.max.in.flight.requests.per.connection:5}")
  private Integer maxRequestsInFlight;

  @Value("${spring.kafka.consumer.properties.spring.json.trusted.packages}")
  private String trustedPackages;

  @Bean
  public KafkaTemplate<String, Object> kafkaTemplate(
      final ProducerFactory<String, Object> factory) {
    return new KafkaTemplate<>(factory);
  }

  @Bean
  public ProducerFactory<String, Object> producerFactory() {
    final Map<String, Object> config = new HashMap<>();

    // Mandatory companion of enable.idempotence — Kafka refuses to start the producer
    // Exactly-once-per-partition semantics: the broker deduplicates retried batches
    // using the producer id + sequence number.
    config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, idempotence);
    config.put(ProducerConfig.RETRIES_CONFIG, retries);
    config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
    config.put(ProducerConfig.ACKS_CONFIG, acks);
    config.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, maxRequestsInFlight);
    config.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, deliveryTimeout);
    config.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, requestTimeout);
    config.put(ProducerConfig.LINGER_MS_CONFIG, linger);
    return new DefaultKafkaProducerFactory<>(config);
  }

  @Bean
  public ConsumerFactory<String, Object> consumerFactory() {
    final Map<String, Object> props = new HashMap<>();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    props.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroup);
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JacksonJsonDeserializer.class);
    props.put(JacksonJsonDeserializer.TRUSTED_PACKAGES, trustedPackages);
    return new DefaultKafkaConsumerFactory<>(props);
  }

  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
      final ConsumerFactory<String, Object> consumerFactory,
      final KafkaTemplate<String, Object> kafkaTemplate) {
    final var factory = new ConcurrentKafkaListenerContainerFactory<String, Object>();
    factory.setConsumerFactory(consumerFactory);
    factory.setConcurrency(3);
    factory.getContainerProperties().setAckMode(AckMode.RECORD);
    DefaultErrorHandler defaultErrorHandler =
        new DefaultErrorHandler(
            new DeadLetterPublishingRecoverer(kafkaTemplate), new FixedBackOff(2000L, retries));
    factory.setCommonErrorHandler(defaultErrorHandler);
    return factory;
  }
}
```
- Define the used variables in the `application.yml` file.
- Do not use the `ApplicationEventPublisher` class for publishing events. Use the generated Producer
  class instead.

## 5. JPA Persitence Configuration

- Add this `PersistenceConfig` class to the `config` directory.
```java
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
@EntityScan(basePackages = "net.wowdev.ecommerce.domain.entity")
public class PersistenceConfig {}
```

## 6. H2 Database

Configure local H2 as file-based (for example `jdbc:h2:file:./data/${domain_object_lower}s-db`),
expose `/h2-console` only as appropriate for local development, disable SQL logging by default,
and set `server.port` with an environment override. Keep secrets out of source control.

## 7. Code formatting requirements

- Follow the coding style from "Google Java Style" for the whole project.

## 8. Code style and safety

- Use UTF-8, four-space indentation, descriptive names, and the existing project formatter.
- Prefer immutable DTOs, constructor injection, early returns, constants over magic values, and
  explicit types over `var`.
- Make method parameters `final` only when that matches the repository's established style; do not
  introduce noisy style changes into unrelated files.
- Use Lombok logging (@Sl4j) only when Lombok is already enabled or explicitly selected.
- Log useful non-sensitive context with parameterized messages; never log credentials, tokens, or
  full sensitive payloads.

## 9. Tests and acceptance checks

Add focused JUnit 5 tests for mapper, validation, service, controller, repository, producer,
consumer, and error paths.
Prefer `@WebMvcTest` for MVC behavior, `@DataJpaTest` for repository behavior, and mocks for Kafka.
For Spring Boot 4, use the dedicated MVC test starter and current Spring test bean-override APIs
when required by the project.

- Add new unit test classes and in order to test every implemented production class.
- When suitable, implement unit tests for validating the business logic in the production classes.
- When suitable, each production class should have its own test class.
- The total test coverage is to be above 95% of code lines.

Configure JaCoCo to enforce at least 95% line coverage during `verify`. Exclude only generated
sources and a trivial bootstrap class when justified; never exclude business code to make the
threshold pass.

Before reporting completion:

1. Validate the reference schema and generated sources.
2. Run focused tests, then `mvn verify` or the repository-prescribed Maven command.
3. Inspect `target/site/jacoco/jacoco.csv` or the equivalent report and report actual coverage.
4. Review `git diff` and `git status`; mention pre-existing or unrelated changes.
5. Format all production and test code with "Google Java Style".

Do not report success if the schema is missing, tests were skipped, or verification passed only by
ignoring failures. If dependencies cannot be downloaded, report the exact command and environmental
blocker.
