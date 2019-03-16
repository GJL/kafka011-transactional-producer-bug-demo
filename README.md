## Possible Silent Data Loss in Kafka011 Transactional Producer

This project demonstrates a possible bug in the transactional producer introduced in Kafka 0.11.0.0.
The example application publishes two messages to different partitions of a topic with a 
transactional `KafkaProducer`. Although both messages are in the same transaction, only the second message is visible
from a consumer with `read_committed` isolation level. This violates Kafka's guarantees on transactional delivery:

> Transactional delivery allows producers to send data to multiple partitions such that either all messages are successfully delivered, or none of them are.

The result is achieved by configuring a low `transaction.timeout.ms` in the producer and waiting long
enough between the two messages.

To run the demo, follow these steps:

Download the binary Kafka `0.11.0.1` distribution from https://www.apache.org/dyn/closer.cgi?path=/kafka/0.11.0.1/kafka_2.11-0.11.0.1.tgz

1. Start a new instance of ZooKeeper (default configuration):
    
        ./bin/zookeeper-server-start.sh  config/zookeeper.properties
    
1. Start a new Kafka broker (default configuration):

        ./bin/kafka-server-start.sh config/server.properties

1. Create the topic `producer-test` in Kafka with 2 partitions:

        ./bin/kafka-topics.sh --create --topic producer-test  --zookeeper localhost:2181 --partitions=2 --replication-factor=1

1. Start a consumer with `read_committed` isolation level:

        ./bin/kafka-console-consumer.sh  --topic producer-test  --bootstrap-server localhost:9092  --isolation-level read_committed 
        
1. Start a consumer with default isolation level (`read_uncommitted`):

        ./bin/kafka-console-consumer.sh  --topic producer-test  --bootstrap-server localhost:9092 
    
1. Run the main method in `App.java` (e.g., from an IDE). The main method takes at least 70 seconds to finish.
If you did not change the code, no exceptions should be thrown. 
From the perspective of a consumer with default isolation level, both messages should be visible. 
However, only the second message should be visible from a consumer with `read_committed` isolation level.
Because no exception was thrown, the loss of the first message is *silent*.

1. Comment the line containing `message2Partition = 1` to publish both messages to the same partition of the topic.  
This time, the application should throw an exception `org.apache.kafka.common.errors.InvalidTxnStateException` on `commitTransaction()`.
