# WebSocket Test Bench
This is a test bench for WebSocket.
This can be used to test a WebSocket server with number of concurrent users with different payload sizes and different time periods. This load tester does not wait for a message round trips in order to send the next message to the server. Instead it sends messages without delays to the server and expect the echo back of it with the same sequence.

## Building the project
### Prerequisites
* Java 8
* Maven

### Steps to build
* Clone the project
* Run `mvn package` in the project derectory
* You can find the ` websocket-test-bench-jar-with-dependencies.jar` jar file on target directory

## Running the test bench

#### `java -jar websocket-test-bench-jar-with-dependencies.jar <options>`

### Options

|option|description|default value|
|------|-----------|-------------|
|-u , --url|URL of the server (Mandotory)|Non|
|-n , --connections|No of connections to be made to the server|1|
|-m , --messages|No of messages to be sent to the server per connection (This is not valid with -t option)|100|
|-p , --payload|Payload size of a message in bytes|100|
|-t , --time|Time period which test should run in minutes (If this option is enabled then messages will be sent for a given period of time ignoring the -m option)|0|

eg: ` java -jar websocket-test-bench-jar-with-dependencies.jar -u ws://localhost:15500/websocket  -n 10 -m 100`
