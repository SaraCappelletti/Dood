# Dood
Docker outside of docker

You can download the image with "docker pull saracappelletti/dood"

Create a new container using "docker run -e DOCKER_HOST=demon_docker_address -p 8081:8081 --name containername saracappelletti/dood"

demon_docker_address needs to be changed with:

If you are using Docker for Windows, for Mac or Docker usually is tcp://localhost:2375 so you can use the default value, or you can use tcp://host.docker.internal:2375

If you are using Linux, usually Docker is available at unix:///var/run/docker.sock.

This program waits for a POST HTTP request to http://localhost:8081/create-container with this body
{
    "digitalTwinImage": "saracappelletti/digital-twin-with-configuration-files",
    "digitalTwinId": "termometro",
    "ownerId": "Sara Cappelletti",
    "tags": ["device"],
    "digitalTwinPort": 3000
}


If you want to restart the container, once you have already created it, you can do it with "docker start -ai containerid"
