package com.iot.thingmodel.config;

import com.iot.thingmodel.common.Constants;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "thingmodel")
public class ThingModelProperties {

    private final Tb tb = new Tb();
    private final Auth auth = new Auth();
    private final Security security = new Security();
    private final Database database = new Database();
    private final RuleChain ruleChain = new RuleChain();

    public Tb getTb() {
        return tb;
    }

    public Auth getAuth() {
        return auth;
    }

    public Security getSecurity() {
        return security;
    }

    public Database getDatabase() {
        return database;
    }

    public RuleChain getRuleChain() {
        return ruleChain;
    }

    public static class Tb {

        private String baseUrl = Constants.Defaults.TB_BASE_URL;
        private int clientMaxRetries = Constants.Defaults.TB_CLIENT_MAX_RETRIES;
        private long clientInitialRetryDelayMs = Constants.Defaults.TB_CLIENT_INITIAL_RETRY_DELAY_MS;
        private long clientMaxRetryDelayMs = Constants.Defaults.TB_CLIENT_MAX_RETRY_DELAY_MS;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public int getClientMaxRetries() {
            return clientMaxRetries;
        }

        public void setClientMaxRetries(int clientMaxRetries) {
            this.clientMaxRetries = clientMaxRetries;
        }

        public long getClientInitialRetryDelayMs() {
            return clientInitialRetryDelayMs;
        }

        public void setClientInitialRetryDelayMs(long clientInitialRetryDelayMs) {
            this.clientInitialRetryDelayMs = clientInitialRetryDelayMs;
        }

        public long getClientMaxRetryDelayMs() {
            return clientMaxRetryDelayMs;
        }

        public void setClientMaxRetryDelayMs(long clientMaxRetryDelayMs) {
            this.clientMaxRetryDelayMs = clientMaxRetryDelayMs;
        }
    }

    public static class Auth {

        private int cacheSeconds = Constants.Defaults.AUTH_CACHE_SECONDS;

        public int getCacheSeconds() {
            return cacheSeconds;
        }

        public void setCacheSeconds(int cacheSeconds) {
            this.cacheSeconds = cacheSeconds;
        }
    }

    public static class Security {

        private String secretKey = "";

        public String getSecretKey() {
            return secretKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }
    }

    public static class Database {

        private String expectedName = Constants.Defaults.DB_NAME;
        private String expectedSchema = Constants.Defaults.DB_SCHEMA;

        public String getExpectedName() {
            return expectedName;
        }

        public void setExpectedName(String expectedName) {
            this.expectedName = expectedName;
        }

        public String getExpectedSchema() {
            return expectedSchema;
        }

        public void setExpectedSchema(String expectedSchema) {
            this.expectedSchema = expectedSchema;
        }
    }

    public static class RuleChain {

        private boolean enabled = true;
        private String kafkaBootstrap = "";
        private String topicTelemetry = Constants.Defaults.KAFKA_TOPIC_TELEMETRY;
        private String topicEvent = Constants.Defaults.KAFKA_TOPIC_EVENT;
        private String topicInvalid = Constants.Defaults.KAFKA_TOPIC_INVALID;
        private String keyPattern = Constants.Defaults.KAFKA_KEY_PATTERN;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getKafkaBootstrap() {
            return kafkaBootstrap;
        }

        public void setKafkaBootstrap(String kafkaBootstrap) {
            this.kafkaBootstrap = kafkaBootstrap;
        }

        public String getTopicTelemetry() {
            return topicTelemetry;
        }

        public void setTopicTelemetry(String topicTelemetry) {
            this.topicTelemetry = topicTelemetry;
        }

        public String getTopicEvent() {
            return topicEvent;
        }

        public void setTopicEvent(String topicEvent) {
            this.topicEvent = topicEvent;
        }

        public String getTopicInvalid() {
            return topicInvalid;
        }

        public void setTopicInvalid(String topicInvalid) {
            this.topicInvalid = topicInvalid;
        }

        public String getKeyPattern() {
            return keyPattern;
        }

        public void setKeyPattern(String keyPattern) {
            this.keyPattern = keyPattern;
        }
    }
}
