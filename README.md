# Pingh

An app notifying of new GitHub `@mention`s.

## Status

Currently, this is a work-in-progress example.

## Planned Features

- [ ] Display the list of recent `@mention`s to a logged in GitHub user via a Chrome extension.
- [ ] Mark the mentions as "read", or snooze them until later.
- [ ] Enter DND mode.

## Tech

* JDK 11 for server and JDK 17 for client.
* Spine Event Engine.
* Kotlin.
* Google Cloud.

## Run locally

### Prerequisites

* [Docker Desktop](https://docs.docker.com/desktop/) installed;
* JDK 11 and 17 installed.

### Steps

1. Start Docker Desktop.
2. Ensure the terminal is opened in the project's root directory.

3. Launch the web server on the `localhost:50051`:
```shell
./gradlew build run
```

4. Build and run the client application distribution:
```shell
cd desktop
./gradlew build runDistributable
```
