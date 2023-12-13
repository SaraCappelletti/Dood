# Dood
Docker outside of docker
You can ran it inside this container
docker run -ti --rm -v /var/run/docker.sock:/var/run/docker.sock docker /bin/ash

The program waits for you to inser an image name and than creates a new container for it, and wait for the next image.
