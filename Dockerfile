# Usa un'immagine di base di Java
FROM openjdk:11-jre

# Crea una directory /app all'interno del contenitore
RUN mkdir /app

# Copia il tuo file ZIP nell'immagine
COPY ./app/build/distributions/app.zip /app

# Sposta il file ZIP nella directory /app
WORKDIR /app
RUN unzip app.zip -d app

# Specifica il comando di avvio dell'applicazione
CMD ["sh", "./app/app/bin/app"]
