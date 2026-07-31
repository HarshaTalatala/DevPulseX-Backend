package com.devpulsex;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.HealthContributor;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("prod")
@TestPropertySource(properties = {
    "DATABASE_URL=jdbc:h2:mem:prodtest;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
    "DB_USERNAME=sa",
    "DB_PASSWORD=dummy_password",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "JWT_SECRET=devpulseXdevpulseXdevpulseXdevpulseXdevpulseXdevpulseX",
    "GITHUB_CLIENT_ID=test-github-id",
    "GITHUB_CLIENT_SECRET=test-github-secret",
    "GOOGLE_CLIENT_ID=test-google-id",
    "GOOGLE_CLIENT_SECRET=test-google-secret",
    "TRELLO_KEY=test-trello-key",
    "TRELLO_ENC_SECRET=test-trello-enc-secret-32-bytes-minimum"
})
class HealthEndpointProdTest {

    @Autowired
    private HealthEndpoint healthEndpoint;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void healthEndpointIsUpWithoutDbContributor() {
        // Verify Actuator health endpoint returns UP
        assertThat(healthEndpoint.health().getStatus()).isEqualTo(Status.UP);

        // Verify DB health contributor bean is NOT present in application context
        boolean hasDbHealthContributor = applicationContext.containsBean("dbHealthContributor") 
                || applicationContext.containsBean("dbHealthIndicator");
        assertThat(hasDbHealthContributor)
                .as("dbHealthContributor/Indicator should be disabled by management.health.db.enabled=false")
                .isFalse();
    }
}
