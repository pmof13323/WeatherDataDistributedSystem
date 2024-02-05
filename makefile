JAVAC = javac
JAVA = java
JFLAGS = -cp .:./json-20230618.jar

SRC_FILES = RequestHandler.java AggregationServer.java GETClient.java LamportClock.java ContentServer.java LClockGetter1.java ClientUnitTests.java AggServerUnitTests.java GetClientTest1.java GetClientTest2.java GetClientTest3.java ContentServerUnitTester.java LClockGetter1.java ContentServerTest1.java ContentServerTest2.java ContentServerTest3.java lamportClockUnitTester.java RequestWithTimestamp.java

SERVER = AggregationServer
CONSERV = ContentServer
CLIENT = GETClient

all: $(SERVER) $(CONSERV) $(CLIENT)

$(SERVER): $(SRC_FILES)
	@$(JAVAC) $(JFLAGS) $(SRC_FILES) -d .

$(CONSERV): ContentServer.java
	@$(JAVAC) $(JFLAGS) ContentServer.java

$(CLIENT): GETClient.java
	@$(JAVAC) $(JFLAGS) GETClient.java

run_server:
	@$(JAVA) $(JFLAGS) $(SERVER) 5678

run_contentServer:
	@$(JAVA) $(JFLAGS) $(CONSERV) http://localhost:5678 ConServData.txt

run_getClient:
	@$(JAVA) $(JFLAGS) $(CLIENT) http://localhost:5678

run_3contentServers:
	@$(JAVA) $(JFLAGS) ContentServerTest1 http://localhost:5678 ConServData.txt	& $(JAVA) $(JFLAGS) ContentServerTest2 http://localhost:5678 ConServData2.txt & $(JAVA) $(JFLAGS) ContentServerTest3 http://localhost:5678 ConServData3.txt

run_3clients:
	@$(JAVA) $(JFLAGS) GetClientTest1 http://localhost:5678 & $(JAVA) $(JFLAGS) GetClientTest2 http://localhost:5678 & $(JAVA) $(JFLAGS) GetClientTest3 http://localhost:5678

run_CServerUnitTests:
	@$(JAVA) $(JFLAGS) ContentServerUnitTester

run_AggServerUnitTests:
	@$(JAVA) $(JFLAGS) AggServerUnitTests

run_ClientUnitTests:
	@$(JAVA) $(JFLAGS) ClientUnitTests

run_lClockUnitTests:
	@$(JAVA) $(JFLAGS) lamportClockUnitTester

clean:
	rm -f *.class

.PHONY: all run_server run_contentServer run_getClient run_3getClients clean