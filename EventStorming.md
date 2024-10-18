# #EventStorming

The [#EventStorming](https://www.eventstorming.com/) sessions resulted 
in the identification of two bounded contexts.

- **Sessions** manages user sessions and handles authentication via GitHub. 
  It is also responsible for automatically refreshing the session if it has expired.

<img src="./img/event-storming/sessions-bc.jpg" width="700px" alt="Sessions bounded context">

- **Mentions** manages user mentions and retrieves new mentions from GitHub. 
  Additionally, it oversees mention states and is responsible for deactivating snooze mode.

<img src="./img/event-storming/mentions-bc-1.jpg" width="700px" alt="Mentions bounded context 1">

<img src="./img/event-storming/mentions-bc-2.jpg" width="500px" alt="Mentions bounded context 2">
