# Pingh

[![license][license-badge]][apache-license]

A [GitHub App][github-app] that notifies users of new GitHub `@mention`s.
It also runs as a system tray application on macOS.

- Displays a list of recent `@mention`s for the logged-in GitHub user
  and the [teams][github-team] they hold membership in.
- Marks mentions as “read” or snooze them for later.
- Notifies upon receiving new mentions or when the snooze time for a mention has expired.
- Supports Do Not Disturb mode.
- Ignores mentions from the specified repositories and organizations.

The application stores the current settings and session data in the application folder 
within the user data directory. The app settings are linked to the user who configured them, 
and they update accordingly when the account changes.

The application does not track mentions created by the user themselves.

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
- Compose Multiplatform 1.7.0.
- JDK 17.

Builds with Gradle 8.10.x. Runs locally.

The Amazon Corretto 21 JDK is recommended to build client application.
Other versions and implementations of the JDK may cause errors 
when running the client distribution on some versions of macOS.

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
the [GitHub app][github-app] is installed on the repository or the organization that owns it.

The application also tracks mentions of teams the user belongs to, 
including both visible and secret teams. To display these mentions, 
the organization owning the team must have the [GitHub app][github-app] installed.

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

3. Specify the directory to store the application logs if needed. To do this, 
  enter custom log directory in the `server/src/main/resources/log4j2.properties` as follows:

```properties
property.log.dir=/path/to/your/custom/directory
```

This step can be skipped. In that case, the server will try to write logs 
to the `/var/log/pingh` directory. If this directory does not exist or is inaccessible, 
the logs will not be saved, but this will not affect the server's performance.

4. Start the Pingh server locally. The server always runs on port `50051`. 
  To launch it, run the following command in the root project directory:

```shell
./gradlew publishToMavenLocal run
```

This will start the server on `localhost:50051` and publish the required JAR files 
for the client application to the Maven Local repository.

5. Configure the client's connection to the server by modifying
  `desktop/src/main/resources/config/server.properties` as follows:

```properties
server.address=localhost
server.port=50051
```

6. Build and run the client application:

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

The [Compute Engine][gcloud-compute-engine] offers the capability to create virtual machines.
The Pingh server is deployed and running on a Compute Engine instance.

Hosting the application in Compute Engine also enables access 
to other Google Cloud services.

To allow external requests, a [firewall rule][gcloud-firewall] must be configured, 
and ports `50051` (for the Pingh RPC server) and `8080` (for the HTTP server handling requests 
from the Google Cloud [Scheduler](#scheduler)) must be open.

### Datastore

The Google Cloud [Datastore][gcloud-datastore] is used for data storage
as a highly scalable NoSQL database.

To allow a server running on a Compute Engine instance to read from and write to Datastore, 
the server's service account must be granted the `Cloud Datastore User` role, 
and the `datastore` OAuth scope must be enabled.

Datastore requires initial configuration, including setting up indexes 
for Spine's internal record types. The configuration file can be found 
in the server's resources directory at `datastore/config/index.yaml`.
For more information, see the Google Cloud Platform [documentation][gcloud-datastore-indexconfig].

### Scheduler

The Google Cloud [Scheduler][gcloud-scheduler] allows for configuring multiple scheduled tasks
that deliver messages to specific targets.

For the Pingh application, a `cron` task is set up to send a POST request with an empty body 
to the Pingh server every minute. This request includes an authentication token to ensure 
it will be accepted by the Pingh server.

### Secret Manager

The [Secret Manager][gcloud-secret-manager] service is used to securely store
and manage application secrets.

To allow a server running on a Compute Engine instance to access data from Secret Manager,
the server's service account must be granted the `Secret Manager Secret Accessor` role,
and the `cloud-platform` OAuth scope must be enabled.

The following secrets are configured for the Pingh app:

- `github_client_id`: The client ID of a GitHub App.
- `github_client_secret`: The client secret of a GitHub App.
- `auth_token`: The authentication token required for accessing the HTTP server running on the VM.

### Google Logging

The Cloud [Logging][gcloud-logging] is a fully managed, real-time log management service 
that provides storage, search, analysis, and alerting capabilities.

The application server runs on a [Compute Engine instance](#compute-engine) 
and writes logs to local files on the virtual machine. The [Ops Agent][gcloud-ops-agent] is used 
to send these logs to Cloud Logging.

To configure the agent, follow these steps:

1. [Install][gcloud-ops-agent-installation] the agent on the virtual machine.

2. Grant the `Logs Writer` and `Monitoring Metric Writer` roles 
  to the Compute Engine instance's service account, 
  and ensure that the `cloud-platform` OAuth scope is enabled.
  This is required to enable the Ops agent to write data to Cloud Logging.

3. [Configure][gcloud-ops-agent-configuration] the agent. The configuration can be found 
  in `server/src/main/resources/logging/config.yaml` file.

The Ops agent also allows sending virtual machine metrics to Cloud Monitoring, 
but this feature is disabled in the configuration file.

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

## License

This project is licensed under the [Apache License 2.0][apache-license].

## Third-party resources

The project includes icons from [Google Material Symbols and Icons][google-icons],
available under the Apache License 2.0.

## Feedback

We accept the questions and suggestions via the corresponding 
[GitHub Discussions section][feedback].

[license-badge]: https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat
[apache-license]: https://www.apache.org/licenses/LICENSE-2.0
[github-app]: https://github.com/apps/pingh-tracker-of-github-mentions
[github-team]: https://docs.github.com/en/organizations/organizing-members-into-teams/about-teams
[gcloud-compute-engine]: https://cloud.google.com/products/compute
[gcloud-firewall]: https://cloud.google.com/firewall/docs/firewalls
[gcloud-datastore]: https://cloud.google.com/products/datastore
[gcloud-datastore-indexconfig]: https://cloud.google.com/datastore/docs/tools/indexconfig
[gcloud-scheduler]: https://cloud.google.com/scheduler/docs/overview
[gcloud-secret-manager]: https://cloud.google.com/security/products/secret-manager
[gcloud-logging]: https://cloud.google.com/logging
[gcloud-ops-agent]: https://cloud.google.com/stackdriver/docs/solutions/agents/ops-agent
[gcloud-ops-agent-installation]: https://cloud.google.com/stackdriver/docs/solutions/agents/ops-agent/install-index
[gcloud-ops-agent-configuration]: https://cloud.google.com/logging/docs/agent/ops-agent/configuration
[google-icons]: https://fonts.google.com/icons
[feedback]: https://github.com/orgs/SpineEventEngine/discussions
