package Dood;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

public class App {
    public static void main(String[] args) throws IOException {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "error");

        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost("tcp://localhost:2375").build();

        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .maxConnections(100)
                .connectionTimeout(Duration.ofSeconds(30))
                .responseTimeout(Duration.ofSeconds(45))
                .build();

        /*DockerHttpClient.Request request = DockerHttpClient.Request.builder()
                .method(DockerHttpClient.Request.Method.GET)
                .path("/_ping")
                .build();

        System.out.println(request);
        dockerClient.pingCmd().exec();*/

        DockerClient dockerClient = DockerClientImpl.getInstance(config, httpClient);
        // Esegui il comando "docker ps -a"
        List<Container> containers = dockerClient.listContainersCmd().withShowSize(true).exec();

        // Stampa informazioni sui container
        System.out.println("Container List:");
        for (Container container : containers) {
            System.out.println("ID: " + container.getId());
            System.out.println("Image: " + container.getImage());
            System.out.println("Command: " + container.getCommand());
            // ... altre informazioni sui container
            System.out.println("----------");
        }

        // Chiudi la connessione al demone Docker
        dockerClient.close();
        
    }

}
