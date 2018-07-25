# WebSocket Load Tester
This is a load tester for WebSocket.
This can be used to test a WebSocket server with number of concurrent users with different payload sizes. This load tester does not wait for a message round trip in order to send the next message to the server. Instead it sends messages without delays to the server and expect the echo back of it with the same sequence.