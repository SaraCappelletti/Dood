package Dood;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;

import java.io.IOException;
import java.time.Duration;

public class App {
    public static void main(String[] args) throws IOException {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "error");

        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost("tcp://host.docker.internal:2375").build();

        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .maxConnections(100)
                .connectionTimeout(Duration.ofSeconds(30))
                .responseTimeout(Duration.ofSeconds(45))
                .build();

        DockerClient dockerClient = DockerClientImpl.getInstance(config, httpClient);
        String image = "saracappelletti/digital-twin-with-configuration-files";
        CreateContainerResponse container = dockerClient.createContainerCmd(image)
                .exec();

        // Avvio del container
        dockerClient.startContainerCmd(container.getId()).exec();

        // Stampa dell'ID del container appena creato
        System.out.println("Container ID: " + container.getId());

        // Chiudi il client Docker
        dockerClient.close();
        
    }

}
