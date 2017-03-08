System Requirements:
-------------------------
- Maven 3.0.4 or higher

Running the project:
-------------------------
0. pre-step
mvn clean install

1. In root directory

mvn jetty:run

This will start the web server

2. In a different window

mvn exec:java -Dexec.mainClass=ChatClient

You must specify your first name you will use in the chat.  
When this comes up, enter your messages.  
You can also open up another window to have a conversation.
