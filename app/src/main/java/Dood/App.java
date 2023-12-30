package Dood;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class App {
    private static final ExecutorService executor = Executors.newFixedThreadPool(10);

    public static void main(String[] args) throws Exception {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "error");

        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost("tcp://host.docker.internal:2375").build();

        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .maxConnections(100)
                .connectionTimeout(java.time.Duration.ofSeconds(30))
                .responseTimeout(java.time.Duration.ofSeconds(45))
                .build();

        DockerClient dockerClient = DockerClientImpl.getInstance(config, httpClient);

        HttpHandler httpHandler = new HttpHandler() {
            @Override
            public void handleRequest(HttpServerExchange exchange) throws Exception {
                try {
                    // Usa AsyncContext per gestire la lettura asincrona
                    exchange.dispatch(executor, () -> {
                        exchange.getRequestReceiver().receiveFullString((exchange1, message) -> {
                            try {
                                String requestBody = message;

                                // Converte la stringa JSON in un oggetto Java
                                ObjectMapper objectMapper = new ObjectMapper();
                                JsonNode jsonNode = objectMapper.readTree(requestBody);

                                // Estrai i valori da jsonNode
                                String dtImage = jsonNode.get("dt-image").asText();
                                String owner = jsonNode.get("owner").asText();
                                System.out.println("Image name: " + dtImage);

                                CreateContainerResponse container = dockerClient.createContainerCmd(dtImage)
                                        .exec();

                                // Avvio del container
                                dockerClient.startContainerCmd(container.getId()).exec();

                                // Stampa dell'ID del container appena creato
                                exchange.getResponseSender().send("Container ID: " + container.getId());
                                System.out.println("Container ID: " + container.getId());
                            } catch (Exception e) {
                                e.printStackTrace();
                                exchange.setStatusCode(500); // Internal Server Error
                                exchange.getResponseSender().send("Errore durante l'elaborazione della richiesta");
                            }
                        });
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    exchange.setStatusCode(500); // Internal Server Error
                    exchange.getResponseSender().send("Errore durante l'elaborazione della richiesta");
                }
            }
        };

        // Aggiungi l'handler per la gestione della richiesta HTTP
        Undertow server = Undertow.builder()
                .addHttpListener(8081, "0.0.0.0")
                .setHandler(Handlers.path().addExactPath("/create-container", httpHandler))
                .build();

        // Avvia il server Undertow
        server.start();

        System.out.println("Server Undertow in esecuzione. Endpoint: http://localhost:80881create-container");
    }
}
