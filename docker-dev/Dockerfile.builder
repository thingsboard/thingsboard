FROM maven:3.9.11-eclipse-temurin-25

# inotify-tools for the file watcher, rsync/jq for small helper tasks
RUN apt-get update \
    && apt-get install -y --no-install-recommends inotify-tools rsync jq \
    && rm -rf /var/lib/apt/lists/*

COPY watch-build.sh /usr/local/bin/watch-build.sh
RUN chmod +x /usr/local/bin/watch-build.sh

WORKDIR /src
VOLUME ["/root/.m2", "/build"]

ENTRYPOINT ["/usr/local/bin/watch-build.sh"]
CMD ["watch"]
