package com.mamikos.kostapi.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base for every full-stack test: a real HTTP server, a real PostgreSQL, and a real Redis,
 * all in containers. The concurrency and idempotency behaviour this API depends on — row
 * locking, transaction boundaries, bulk updates — do not reproduce faithfully against H2 or
 * a mock, which is exactly why {@link com.mamikos.kostapi.credit.CreditRechargeServiceIT} and
 * {@link com.mamikos.kostapi.inquiry.ConcurrentInquiryIT} need this real a database.
 *
 * <p>The containers are started once, in a static initializer, and deliberately have no
 * {@code @Container}/{@code @Testcontainers} annotation on them: that JUnit extension stops
 * a static container again after every test <em>class</em> that carries it finishes — fine
 * for a container used by one class, but it means the second IT class to run would find this
 * shared container already killed out from under it. Leaving it to Ryuk to reap when the JVM
 * exits, instead of an explicit {@code @AfterAll}, is what lets it survive the whole suite.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));

    @ServiceConnection(name = "redis")
    static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

    static {
        POSTGRES.start();
        REDIS.start();
    }

    @Autowired
    protected TestRestTemplate restTemplate;
}
