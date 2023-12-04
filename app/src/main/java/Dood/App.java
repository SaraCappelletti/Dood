package Dood;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.core.exec.CreateContainerCmdExec;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import org.apache.commons.io.IOUtils;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Duration;

public class App {
    public static void main(String[] args) throws IOException {
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();

        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .maxConnections(100)
                .connectionTimeout(Duration.ofSeconds(30))
                .responseTimeout(Duration.ofSeconds(45))
                .build();

        DockerHttpClient.Request request = DockerHttpClient.Request.builder()
                .method(DockerHttpClient.Request.Method.GET)
                .path("/_ping")
                .build();

        System.out.println(request);

        //DockerClient dockerClient = DockerClientImpl.getInstance(config, httpClient);
        //dockerClient.pingCmd().exec();
        /*// Crea la configurazione del client Docker
        DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();

        // Crea il client Docker
        DockerClient dockerClient = DockerClientBuilder.getInstance(config).build();

        String command = "docker ps -a";

        try {
            // Crea un processo per eseguire il comando
            Process process = Runtime.getRuntime().exec(command);

            // Legge l'output del processo
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            // Attendi che il processo termini
            int exitCode = process.waitFor();
            System.out.println("Exit code: " + exitCode);

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        ------

        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();

        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .maxConnections(100)
                .connectionTimeout(Duration.ofSeconds(30))
                .responseTimeout(Duration.ofSeconds(45))
                .build();

        DockerHttpClient.Request request = DockerHttpClient.Request.builder()
                .method(DockerHttpClient.Request.Method.GET)
                .path("/_ping")
                .build();

        try (DockerHttpClient.Response response = httpClient.execute(request)) {
            assertThat(response.getStatusCode(), equalTo(200));
            assertThat(IOUtils.toString(response.getBody()), equalTo("OK"));
        }

        DockerClient dockerClient = DockerClientImpl.getInstance(config, httpClient);
        dockerClient.pingCmd().exec();
        dockerClient.startContainerCmd(dockerClient.createContainerCmd("")
                .withCmd("comando_iniziale")
                .exec().getId()).exec();

        -----

        DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .maxConnections(100)
                .connectionTimeout(Duration.ofSeconds(30))
                .responseTimeout(Duration.ofSeconds(45))
                .build();

        // Crea direttamente il client Docker senza DockerClientBuilder
        DockerClient dockerClient = new DockerClientImpl(httpClient, config);

        // ID dell'immagine Ubuntu
        String ubuntuImageId = "c6";

        // Configura il comando per creare un nuovo container
        CreateContainerCmdExec createContainerCmdExec = new CreateContainerCmdExec(
                dockerClient.createCreateContainerCmd(ubuntuImageId)
                        .withCmd("/bin/bash")
                        .withHostConfig(HostConfig.newHostConfig()
                                .withPortBindings(PortBinding.parse("8080:80")))
        );

        // Esegui il comando e ottieni l'ID del nuovo container
        String newContainerId = createContainerCmdExec.exec().getId();

        // Avvia il nuovo container
        dockerClient.startContainerCmd(newContainerId).exec();

        // Output dell'ID del nuovo container
        System.out.println("New Container ID: " + newContainerId);

        // Puoi eseguire ulteriori operazioni con il container se necessario

        // Chiudi la connessione al client Docker
        dockerClient.close();*/
    }

}
