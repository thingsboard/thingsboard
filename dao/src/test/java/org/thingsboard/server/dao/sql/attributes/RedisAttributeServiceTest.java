package org.thingsboard.server.dao.sql.attributes;

import lombok.extern.slf4j.Slf4j;
import org.junit.ClassRule;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.testcontainers.containers.GenericContainer;

@TestPropertySource(properties = {
        "cache.type=redis", "redis.connection.type=standalone"
})
@ContextConfiguration(initializers = RedisAttributeServiceTest.class)
@Slf4j
public class RedisAttributeServiceTest extends AttributeServiceTest implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @ClassRule
    public static GenericContainer redis = new GenericContainer("redis:4.0").withExposedPorts(6379);

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
                applicationContext, "redis.standalone.host=localhost");
        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
                applicationContext, "redis.standalone.port=" + redis.getMappedPort(6379));
    }

}
