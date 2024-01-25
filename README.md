# Dood
Docker outside of docker

You can download the image with "docker pull saracappelletti/dood"

Create a new container using "docker run -it -p 8081:8081 --name containername saracappelletti/dood"


This program waits for a POST HTTP request to http://localhost:8081/create-container with this body
{
    "digitalTwinImage": "saracappelletti/digital-twin-with-configuration-files",
    "digitalTwinId": "termometro",
    "ownerId": "Sara Cappelletti",
    "tags": ["device"],
    "digitalTwinPort": 3000
}


If you want to restart the container, once you have already created it, you can do it with "docker start -ai containerid"
