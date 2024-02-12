package Dood;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.command.CreateServiceResponse;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class App {
    private static final ExecutorService executor = Executors.newFixedThreadPool(10);
    private static String dockerHost = System.getenv("DOCKER_HOST");

    private static Map<String, DigitalTwinInfo> requestedDigitalTwins = new HashMap<>();

    public static void main(String[] args) {

        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(dockerHost != null && !dockerHost.isEmpty() ? dockerHost : "tcp://localhost:2375")
                .build();

        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .maxConnections(100)
                .connectionTimeout(java.time.Duration.ofSeconds(30))
                .responseTimeout(java.time.Duration.ofSeconds(45))
                .build();

        DockerClient dockerClient = DockerClientBuilder.getInstance(config)
                .withDockerHttpClient(httpClient)
                .build();

        HttpHandler httpHandler = exchange -> {
            try {
                exchange.dispatch(executor, () -> exchange.getRequestReceiver()
                        .receiveFullString((exchange1, message) -> {
                            String requestState = "";
                            try {
                                String requestBody = message;
                                ObjectMapper objectMapper = new ObjectMapper();
                                JsonNode jsonNode = objectMapper.readTree(requestBody);
                                requestState = "ACCEPTED";
                                Map<String, Object> jsonResponse = new HashMap<>();

                                if (jsonNode.has("digitalTwinImage")) {
                                    //Extracting the dt's details from the JSON request
                                    String dtImage = jsonNode.get("digitalTwinImage").asText();
                                    String dtId = jsonNode.get("digitalTwinId").asText();
                                    String ownerId = jsonNode.get("ownerId").asText();
                                    List<String> tags = new ArrayList<>();
                                    for (JsonNode tagNode : jsonNode.get("tags")) {
                                        tags.add(tagNode.asText());
                                    }
                                    String dtPort = jsonNode.get("digitalTwinPort").asText();

                                    dockerClient.pullImageCmd(dtImage)
                                            .exec(new PullImageResultCallback())
                                            .awaitCompletion();

                                    int availablePort = findAvailablePort(Integer.parseInt(dtPort), dockerClient);
                                    jsonResponse.put("assignedPort", availablePort);

                                    ServiceSpec serviceSpec = new ServiceSpec()
                                            .withName(dtId)
                                            .withTaskTemplate(new TaskSpec()
                                                    .withContainerSpec(new ContainerSpec()
                                                            .withImage(dtImage)))
                                            .withEndpointSpec(new EndpointSpec()
                                                    .withPorts(Arrays.asList(new PortConfig().withPublishedPort(availablePort).withTargetPort(80))));

                                    CreateServiceResponse serviceResponse = dockerClient.createServiceCmd(serviceSpec)
                                            .exec();
                                    String serviceId = serviceResponse.getId();

                                    requestedDigitalTwins.put(dtId, new DigitalTwinInfo(dtImage, dtId, ownerId, tags, String.valueOf(availablePort)));

                                    jsonResponse.put("serviceId", serviceId);
                                    jsonResponse.put("requestState", requestState);
                                    jsonResponse.put("digitalTwinURI", "http://localhost:" + availablePort + "/" + dtId);
                                    /*//Getting the real mapped port
                                    Service service = dockerClient.inspectServiceCmd(serviceId).exec();
                                    if (service.getSpec().getEndpointSpec() != null) {
                                        List<PortConfig> portConfigs = service.getSpec().getEndpointSpec().getPorts();
                                        if (portConfigs != null && !portConfigs.isEmpty()) {
                                            int mappedPort = portConfigs.get(0).getPublishedPort();
                                            jsonResponse.put("mappedPort", mappedPort);
                                        } else {
                                            exchange.getResponseSender().send("No porte configurate");
                                        }
                                    } else {
                                        exchange.getResponseSender().send("No endpoint");
                                    }*/
                                }
                                else {
                                    //I am looking at the already created dts
                                    String group = jsonNode.get("group").asText();
                                    List<Map<String, Object>> deploymentDescriptors = new ArrayList<>();
                                    for (DigitalTwinInfo dt : requestedDigitalTwins.values()) {
                                        //jsonResponse.put("deploymentDescriptor", dt.getTags());
                                        if (dt.getTags().contains(group)) {
                                            Map<String, Object> deploymentDescriptor = new LinkedHashMap<>();
                                            deploymentDescriptor.put("digitalTwinImage", dt.getDtImage());
                                            deploymentDescriptor.put("digitalTwinId", dt.getDtId());
                                            deploymentDescriptor.put("ownerId", dt.getOwnerId());
                                            deploymentDescriptor.put("tags", dt.getTags());
                                            deploymentDescriptor.put("digitalTwinPort", dt.getDtPort());
                                            deploymentDescriptors.add(deploymentDescriptor);
                                        }
                                    }
                                    jsonResponse.put("deploymentDescriptors", deploymentDescriptors);
                                }
                                //Sending the answer
                                exchange.getResponseSender().send(objectMapper.writeValueAsString(jsonResponse));
                            } catch (Exception e) {
                                requestState = "REFUSED";
                                e.printStackTrace();
                                exchange.setStatusCode(500);
                                exchange.getResponseSender().send("Errore durante l'elaborazione della richiesta");
                            }
                        }));
            } catch (Exception e) {
                e.printStackTrace();
                exchange.setStatusCode(500);
                exchange.getResponseSender().send("Errore durante l'elaborazione della richiesta");
            }
        };

        Undertow server = Undertow.builder()
                .addHttpListener(8081, "0.0.0.0")
                .setHandler(Handlers.path().addExactPath("/create-service", httpHandler))
                .build();

        server.start();

        System.out.println("Server Undertow in esecuzione. Endpoint: http://localhost:8081/create-service");
    }

    private static int findAvailablePort(int desiredPort, DockerClient dockerClient) {
        int port = desiredPort;
        while (!isPortAvailable(port, dockerClient)) {
            port++;
        }
        return port;
    }

    private static boolean isPortAvailable(int port, DockerClient dockerClient) {
        try {
            List<Service> services = dockerClient.listServicesCmd().exec();
            for (Service service : services) {
                EndpointSpec endpointSpec = service.getSpec().getEndpointSpec();
                if (endpointSpec != null) {
                    List<PortConfig> portConfigs = endpointSpec.getPorts();
                    for (PortConfig portConfig : portConfigs) {
                        if (portConfig.getPublishedPort() == port) {
                            return false; // La porta è già in uso da un servizio Docker
                        }
                    }
                }
            }
            return true; // Nessun servizio Docker utilizza la porta
        } catch (Exception e) {
            // Gestire eventuali eccezioni
            e.printStackTrace();
            return false;
        }
    }
}
