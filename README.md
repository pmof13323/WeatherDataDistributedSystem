Hi! thanks for taking a look at my distributed systems project

In this project, which has been my first introduction into Distributed Systems, I created a distributed weather data system to gain an understanding of what is required to build a client/server system, and to build a system that aggregates and distributes weather data in JSON format using a RESTful API.
_____________________________________________________
Summary of implemented functionalities:
_____________________________________________________
1. Text sending 
2. client, server and content server processes start up and communicate 
3. PUT operation works for one content server 
4. GET operation works for many read clients 
5. Aggregation server expunging expired data after 30 seconds 
6. Retry on errors (server not available etc)
7. Lamport clocks are implemented 
8. Critical error codes are implemented 
9. Content servers are replicated and fault tolerant
_____________________________________________________
Failure management:
_____________________________________________________
Failure management has been applied in many parts of the program, here are (just a few of the many) measures taken as a starting point:
- if incorrect id is inputted in a client it is handled safely and user is asked again in correct form
- if server is not available client and content servers retry
- if there is no data correct error codes are sent to relevant components indicating the issue safely
- if data storage files (between all components) are not found, the system creates them
- if user does not specify ID and the specific headers designed (to maintain lamport clocks) are not sent, default parameters are used such that they still receive correct data (browser connections for example) hence implementing a successful RESTful API
- in case of system failure, data is stored in an intermediate text file so data is not lost 
_____________________________________________________
Lamport Clock implementation:
_____________________________________________________
The aggregation server, content server and client all hold, and are capable of maintaining their own lamport clock. Lamport clocks are ticked upon each request sent, and each request received. The lamport clock request ordering system inside of my program works as follows ->
1. When a content server and/or a client are opened, they make a get request to the aggregation server, receiving the lamport clock value of the aggregation server and setting their lamport clocks accordingly
2. Upon sending a request (from a client or a content server) to the aggregation server, the aggregation server ticks its own lamport clock, if necessary, corrects incoming timestamp values to match the current correct clock/request order.
** note that timestamps are sent to the aggregation server from clients and content servers via request headers
3. The response sent back to the client or server sending a request includes a timestamp header, which the respective server or client uses to update its own lamport clock prior to the next request
4. These requests once arriving into the aggregation server are turned into a request abstract data type that holds the request as well as it's timestamp (timestamp of the client and/or content server)
5. These requests, with their timestamp are added to a priority queue which is fundamentally responsible for maintining a correct request order
6. A request processing thread is always running when an aggregation server is on, which (if the priority queue is not empty) polls the next request in order of lowest timestamp, and runs it accordingly, hence ordering system requests appropriately.
_____________________________________________________
Testing information:
_____________________________________________________
A variety of effective and versatile testing systems have been developed to ensure system performance.

Error catching:
Automated error catching can be found throughout the various files.

Integration testing:
Integration testing has been/is done through user interaction with the system.
To test system integration do as follows ->
1. Follow "How to naturally run:" instructions above
2. Input "IDS60901" into the client terminal to receive weather data
3. Content server successful operation can be viewed inside the terminal where the content server is being run in, informing use of status codes/messages after each request
4. Client successful operation can viewed in its terminal with status codes/messages after each request being printed as well as the weather data received in correct formatting
5. Aggregation server successful operation can be viewed inside it's terminal, which informs on the nature of each request being undertaken, as well as the ID's being requested

Regression testing: 
- after modifying methods/program operation all tests outlined in this section were re-run to ensure that the program operates consistently and successfully

Unit testing:
Unit tests were made for the three primary components and can be run following these instructions:

Aggregation server unit testing:
1. In a terminal run the unit test by inputting "make run_AggServerUnitTests"
***Note that there is some waiting (~ 1min) for this test to run due to the cleanup method being unit tested (involves 30 second waits to test data expunging)
***Allow 10-15 seconds after unit tests are run for program to terminate end opened threads

Content server unit testing:
1. start up an aggregation server by inputting in a terminal "make run_server"
2. in another terminal input "make run_CServerUnitTests"

Client unit testing:
1. start up an aggregation server by inputting in a terminal "make run_server"
2. in another terminal input "make run_ClientUnitTests"

Lamport Clock unit testing:
1. In a terminal run the unit test by inputting "make run_lClockUnitTests"

Testing Harness for Multiple Distributed Entities:
1. A lamport clock system (refer to lamport clock section) has been used in order to maintin order and synchronisation between the interactions of the distributed entities
2. Running the system naturally as previously described outlines the interactions of the distributed components
3. Multithreading is used throughout the program, successfully simulating the interactions between multiple clients and servers
4. Concurrent put and get requests were tested, and can be run with the following instructions:
    CONCURRENT GET REQUESTS ->
    1. startup an aggregation server (make run_server)
    2. startup a content server (make run_contentServer)
    3. for concurrent get requests, in its own terminal run "make run_3clients"
    4. as designed, information will be printed in the client terminal as well as saved in their respective text files
    CONCURRENT PUT REQUESTS ->
    1. startup an aggregation server (make run_server)
    2. for concurrent put requests, in its own terminal run "make run_3contentServers"
    3. as designed, information will be printed in the content server terminal as well as saved in dataEntry.txt as intended

    Due to the operational lamport clock implementation, these requests are run fairly and in accordance with the system's intended order

Synchronization and fault testing:
- A lamport clock system (refer to lamport clock section) has been used in order to maintain order and synchronisation between the interactions of the distributed entities
- Request timestamps are dealt with appropriately (refer to lamport clock section)
- Incorrect order request runs are caught by the system, and the user is informed in the terminal running the aggregation server that an incorrected order has been detected gracefully

Edge cases:
- Refer to Testing Harness for Multiple Distributed Entities's section on concurrent put and get requests to test multiple content servers and/or clients attempting to put and/or get simultaneously
- A variety of edge cases have been tested throughout the entire testing implementation and can be found throughout such as invalid id's, very old/very new content server data entries, concurrent gets and so on...
_____________________________________________________

Thanks! and here is a fun programming joke I found:
Why don't programmers like the outdoors?
Because there is too many bugs.