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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class App {
    private static final ExecutorService executor = Executors.newFixedThreadPool(10);
    private static String dockerHost = System.getenv("DOCKER_HOST");

    public static void main(String[] args) throws Exception {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "error");

        if (dockerHost == null || dockerHost.isEmpty()) {
            dockerHost = "tcp://localhost:2375";
        }

        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(dockerHost).build(); //"tcp://host.docker.internal:2375"

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
                            String requestState = "";
                            try {
                                String requestBody = message;

                                // Converte la stringa JSON in un oggetto Java
                                ObjectMapper objectMapper = new ObjectMapper();
                                JsonNode jsonNode = objectMapper.readTree(requestBody);
                                requestState = "ACCEPTED";

                                // Estrai i valori da jsonNode
                                String dtImage = jsonNode.get("digitalTwinImage").asText();
                                String dtId = jsonNode.get("digitalTwinId").asText();
                                String ownerId = jsonNode.get("ownerId").asText();
                                String tags = jsonNode.get("tags").asText();
                                String dtPort = jsonNode.get("digitalTwinPort").asText();
                                System.out.println("Image name: " + dtImage);

                                CreateContainerResponse container = dockerClient.createContainerCmd(dtImage)
                                        .exec();

                                // Avvio del container
                                dockerClient.startContainerCmd(container.getId()).exec();
                                requestState = "FULLFILLED";

                                // Stampa dell'ID del container appena creato
                                /*exchange.getResponseSender().send("Container ID: " + container.getId());*/
                                System.out.println("Container ID: " + container.getId());

                                Map<String, Object> deploymentDescriptor = new LinkedHashMap<>();
                                deploymentDescriptor.put("digitalTwinImage", dtImage);
                                deploymentDescriptor.put("digitalTwinId", dtId);
                                deploymentDescriptor.put("ownerId", ownerId);
                                deploymentDescriptor.put("tags", tags);
                                deploymentDescriptor.put("digitalTwinPort", dtPort);

                                Map<String, Object> jsonResponse = new HashMap<>();
                                jsonResponse.put("containerId", container.getId());
                                jsonResponse.put("deploymentDescriptor", deploymentDescriptor);
                                jsonResponse.put("requestState", requestState);
                                jsonResponse.put("digitalTwinURI", "http://localhost:" + dtPort + "/" + dtId);

                                String jsonString = objectMapper.writeValueAsString(jsonResponse);

                                exchange.getResponseSender().send(jsonString);
                            } catch (Exception e) {
                                requestState = "REFUSED";
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

        System.out.println("Server Undertow in esecuzione. Endpoint: http://localhost:8081create-container");
    }
}

/*{
    "containerId":"f31913d0be44f5d3442c1845ec9327c76740a4f9c2adb812c8485270e5a4a854",
    "deploymentDescriptor":{
        "digitalTwinImage":"saracappelletti/digital-twin-with-configuration-files",
        "digitalTwinId":"termometro",
        "ownerId":"Sara Cappelletti",
        "tags":[
            "device"
        ],
        "digitalTwinPort":3000
    },
    "requestState":"FULLFILLED",
    "digitalTwinURI" : "http://localhost:3000/termometro"
}*/

