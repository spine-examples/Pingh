# Pingh

A [GitHub App](https://github.com/apps/pingh-tracker-of-github-mentions) that notifies users 
of new GitHub `@mention`s. It also runs as a system tray application on macOS.

- Displays a list of recent `@mention`s for a logged-in GitHub user.
- Marks mentions as “read” or snooze them for later.
- Notifies upon receiving new mentions or when the snooze time for a mention has expired.
- Supports Do Not Disturb mode.

The application stores the current settings and session data in the application folder 
within the user data directory. The app settings are device-specific, 
meaning they remain unchanged when the account is switched.

## Tech

Server stack:

- Spine Event Engine 1.9.0.
- Kotlin 1.9.20.
- JDK 11.
- Ktor 2.3.11.
- Google Datastore.
- Docker.

Builds with Gradle 6.9.x. Deploys onto Google Cloud.

Client stack:

- Kotlin 1.9.20.
- Compose Multiplatform 1.6.11.
- JDK 17.

Builds with Gradle 8.10.x. Runs locally.

## Project structure

- `github` defines value objects to represent the GitHub context, 
  along with descriptions of the JSON responses returned by the GitHub REST API.
- `sessions` implements the Sessions bounded context, 
  which includes user session management and authentication via GitHub.
- `mentions` implements the Mentions bounded context,
  which includes managing mention statuses and 
  the process of retrieving new user mentions from GitHub.
- `clock` emulates an external system by sending the current time to the server.
- `server` configures the server environment, sets up the server, and starts it.
  This module also enables interaction with [Google Cloud](#google-cloud-deployment).
- `client` manages process states and flows for the client application.
- `desktop` provides the user interface created with Compose Multiplatform.

There are several auxiliary modules designed for testing of the corresponding Gradle subprojects: 
`testutil-sessions`, `testutil-mentions`, and `testutil-client`.

For a detailed analysis of the processes within domain contexts, 
refer to the [#EventStorming documentation](./EventStorming.md).

## GitHub data access

The application tracks user's mentions in all public GitHub repositories.

The mentions in the private repositories are tracked if the user has access to the repository and 
the [GitHub app](https://github.com/apps/pingh-tracker-of-github-mentions) is installed 
on the repository or the organization that owns it.

## Authentication

The application can be used only by members of certain organizations.

Users who are members of any permitted organization can log in to the application; 
otherwise, they will encounter an exception.

To check a user’s membership within an organization, the app must be installed 
in that organization.

By installing the application in an organization on the permitted list, 
access to its private repositories for mention searches is granted, 
along with the ability to verify membership for authentication. Installing the application 
in an organization not on the permitted list enables mention detection in private repositories 
but does not affect authentication.

The names of permitted organizations are listed 
in the `sessions/src/main/resources/config/auth.properties` file, separated by commas. 
The names can be updated as needed. For instance, 
for organizations `spine-examples` and `SpineEventEngine`, enter:

```properties
permitted-organizations=spine-examples, SpineEventEngine
```

## Local run

Both client and server can be built and launched locally.

When running the application locally, consider the following:

- In-memory storage is used, so data will be lost between server restarts.
- Both GitHub App ID and GitHub App secret must be specified explicitly 
  by editing [server.properties](./server/src/main/resources/local/config/server.properties) file.

<img src="./img/interaction-diagrams/local.jpg" width="600px" alt="Local interaction diagram">

### Prerequisites

- JDK 11 and 17.
- Docker Desktop.

### Build

To run the application locally, download the project from GitHub and follow these steps:

1. Specify the GitHub App ID and secret in the configuration file. To do this, 
  enter the GitHub App ID and secret 
  in the `server/src/main/resources/local/config/server.properties` as follows:

```properties
github-app.client.id=client_id
github-app.client.secret=client_secret
```

Replace `client_id` and `client_secret` with the values obtained from GitHub.

2. Specify the names of permitted organizations. See: [Authentication](#authentication).
 
3. Start the Pingh server locally. The server always runs on port `50051`. 
  To launch it, run the following command in the root project directory:

```shell
./gradlew publishToMavenLocal run
```

This will start the server on `localhost:50051` and publish the required JAR files 
for the client application to the Maven Local repository.

4. Configure the client's connection to the server by modifying
  `desktop/src/main/resources/config/server.properties` as follows:

```properties
server.address=localhost
server.port=50051
```

5. Build and run the client application:

```shell
cd ./desktop
./gradlew runDistributable
```

This will generate a runnable distribution and start it automatically.

To create a distribution of the client application without launching it, 
use the following command:

```shell
./gradlew createDistributable
```

## Google Cloud deployment

The Pingh application runs in the cloud environment on the Google Cloud Platform.

<img src="./img/interaction-diagrams/google-cloud.jpg" width="650px" alt="Google Cloud interaction diagram">

To start the server in production mode on the cloud, 
the JVM argument named `GCP_PROJECT_ID` must be passed at server startup. 
This argument must specify the Google Cloud project ID.

### Compute Engine

The [Compute Engine](https://cloud.google.com/products/compute) offers the capability to create 
virtual machines. The Pingh server is deployed and running on a Compute Engine instance.

Hosting the application in Compute Engine also enables access 
to other Google Cloud services.

To allow external requests, 
a [firewall rule](https://cloud.google.com/firewall/docs/firewalls) must be configured, 
and ports `50051` (for the Pingh RPC server) and `8080` (for the HTTP server handling requests 
from the Google Cloud [Scheduler](#scheduler)) must be open.

### Datastore

The Google Cloud [Datastore](https://cloud.google.com/products/datastore?hl) is used 
for data storage as a highly scalable NoSQL database.

To allow a server running on a Compute Engine instance to read from and write to Datastore, 
the server's service account must be granted the `Cloud Datastore User` role, 
and the `datastore` OAuth scope must be enabled.

Datastore requires initial configuration, including setting up indexes 
for Spine's internal record types. The configuration file can be found 
in the server's resources directory at `datastore/config/index.yaml`.
For more information, see the Google Cloud Platform 
[documentation](https://cloud.google.com/datastore/docs/tools/indexconfig).

### Scheduler

The Google Cloud [Scheduler](https://cloud.google.com/scheduler/docs/overview) allows for
configuring multiple scheduled tasks that deliver messages to specific targets.

For the Pingh application, a `cron` task is set up to send a POST request with an empty body 
to the Pingh server every minute. This request includes an authentication token to ensure 
it will be accepted by the Pingh server.

### Secret Manager

The [Secret Manager](https://cloud.google.com/security/products/secret-manager) service 
is used to securely store and manage application secrets.

To allow a server running on a Compute Engine instance to access data from Secret Manager,
the server's service account must be granted the `Secret Manager Secret Accessor` role,
and the `cloud-platform` OAuth scope must be enabled.

The following secrets are configured for the Pingh app:

- `github_client_id`: The client ID of a GitHub App.
- `github_client_secret`: The client secret of a GitHub App.
- `auth_token`: The authentication token required for accessing the HTTP server running on the VM.

## Testing

This project includes several types of testing.

1. Unit tests focus on the behavior of entities within bounded contexts, using the Black Box 
  provided by the Spine server testing API.

2. End-to-end tests validate client-server interactions by starting the server and
  running various user scenarios, with a Datastore emulator used for data storage.

3. UI tests are created with the Testing Compose Multiplatform UI API 
  to verify the functionality of the client application, 
  with a test server also launched for these tests.

A Docker environment is required for end-to-end and UI testing, 
as the Datastore emulator is automatically started in a Docker container.

## Feedback

We accept the questions and suggestions via the corresponding 
[GitHub Discussions section](https://github.com/orgs/SpineEventEngine/discussions).
