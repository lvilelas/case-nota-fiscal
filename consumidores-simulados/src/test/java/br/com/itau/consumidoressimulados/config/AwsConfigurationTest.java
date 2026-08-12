package br.com.itau.consumidoressimulados.config;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

class AwsConfigurationTest {

    @Test
    void shouldCreateLocalClientWithEndpointAndStaticCredentials() {
        var properties = new AwsProperties(
                "us-east-1", "http://localhost:4566", "test", "test");

        try (var client = new AwsConfiguration().sqsClient(properties)) {
            assertThat(client.serviceClientConfiguration().endpointOverride())
                    .contains(URI.create("http://localhost:4566"));
        }
    }

    @Test
    void shouldCreateAwsClientUsingTheDefaultCredentialChain() {
        var properties = new AwsProperties("us-east-1", null, null, "unused");

        try (var client = new AwsConfiguration().sqsClient(properties)) {
            assertThat(client.serviceClientConfiguration().endpointOverride()).isEmpty();
        }
    }

    @Test
    void shouldIgnoreBlankEndpointAndIncompleteStaticCredentials() {
        var properties = new AwsProperties("us-east-1", " ", "access", " ");

        try (var client = new AwsConfiguration().sqsClient(properties)) {
            assertThat(client.serviceClientConfiguration().endpointOverride()).isEmpty();
        }
    }
}
