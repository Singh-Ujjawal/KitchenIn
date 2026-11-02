package com.example.kitechin.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

@Configuration
public class MongoConnectionConfig {

    private static final Logger log = LoggerFactory.getLogger(MongoConnectionConfig.class);

    @Bean(destroyMethod = "close")
    public MongoRuntime mongoRuntime(MongoProperties properties,
                                     MongoDatabaseMode mode,
                                     @Value("${app.mongodb.probe-timeout-ms:5000}") long probeTimeoutMs) {
        String uri = properties.getUri();
        if (reachable(uri, probeTimeoutMs)) {
            mode.useAtlas();
            log.info("MongoDB URI is reachable. Using that database.");
            MongoClient client = MongoClients.create(clientSettings(uri, Math.max(probeTimeoutMs, 15_000)));
            return new MongoRuntime(client, null);
        }

        MongoServer server = new MongoServer(new MemoryBackend());
        InetSocketAddress address = server.bind();
        mode.useMemory();
        String host = address.getAddress() == null ? "127.0.0.1" : address.getAddress().getHostAddress();
        if (host.contains(":")) {
            host = "127.0.0.1";
        }
        log.warn("MongoDB URI is missing or not reachable. Using a temporary in-memory database on {}:{}. Data lasts only until this process stops.",
                host, address.getPort());
        MongoClient client = MongoClients.create("mongodb://" + host + ":" + address.getPort() + "/kitechin");
        return new MongoRuntime(client, server);
    }

    @Bean(destroyMethod = "")
    public MongoClient mongoClient(MongoRuntime runtime) {
        return runtime.client();
    }

    private boolean reachable(String uri, long timeoutMs) {
        if (uri == null || uri.isBlank()) {
            log.warn("No MongoDB URI is configured.");
            return false;
        }
        try (MongoClient probe = MongoClients.create(clientSettings(uri, timeoutMs))) {
            probe.getDatabase("admin").runCommand(new Document("ping", 1));
            return true;
        } catch (RuntimeException ex) {
            log.warn("MongoDB connection check failed: {}", safe(ex));
            return false;
        }
    }

    private static MongoClientSettings clientSettings(String uri, long timeoutMs) {
        return MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(uri))
                .applyToClusterSettings(builder -> builder.serverSelectionTimeout(timeoutMs, TimeUnit.MILLISECONDS))
                .applyToSocketSettings(builder -> builder.connectTimeout(timeoutMs, TimeUnit.MILLISECONDS))
                .build();
    }

    private static String safe(Throwable error) {
        String message = error.getMessage();
        if (message == null || message.isBlank()) {
            return error.getClass().getSimpleName();
        }
        return message.replaceAll("://[^/@\\s]+:[^/@\\s]+@", "://***:***@");
    }

    static final class MongoRuntime implements AutoCloseable {
        private final MongoClient client;
        private final MongoServer memoryServer;

        private MongoRuntime(MongoClient client, MongoServer memoryServer) {
            this.client = client;
            this.memoryServer = memoryServer;
        }

        private MongoClient client() {
            return client;
        }

        @Override
        public void close() {
            client.close();
            if (memoryServer != null) {
                memoryServer.shutdown();
            }
        }
    }
}
