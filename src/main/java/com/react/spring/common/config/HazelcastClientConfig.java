package com.react.spring.common.config;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Connect to an external Hazelcast member (Docker container) instead of
 * spinning up the embedded instance the starter creates. Overrides the
 * starter's hazelcastInstance bean.
 *
 * Activate with: app.hazelcast.client.enabled=true.
 * Requires spring.main.allow-bean-definition-overriding=true.
 */
@Configuration
@ConditionalOnProperty(prefix = "app.hazelcast.client", name = "enabled", havingValue = "true")
public class HazelcastClientConfig {

    @Bean(name = "hazelcastInstance", destroyMethod = "shutdown")
    @Primary
    public HazelcastInstance hazelcastInstance(
        @Value("${app.hazelcast.client.cluster-name:dev}") String clusterName,
        @Value("${app.hazelcast.client.members:127.0.0.1:5701}") String members
    ) {
        ClientConfig cfg = new ClientConfig();
        cfg.setClusterName(clusterName);
        for (String addr : members.split(",")) {
            String trimmed = addr.trim();
            if (!trimmed.isEmpty()) {
                cfg.getNetworkConfig().addAddress(trimmed);
            }
        }
        return HazelcastClient.newHazelcastClient(cfg);
    }
}
