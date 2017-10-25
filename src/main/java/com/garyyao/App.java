package com.garyyao;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.Properties;

/**
 * Instructions in README.md
 */
public class App {

	public static void main(String[] args) throws InterruptedException {

		final Properties properties = new Properties();
		properties.put("bootstrap.servers", "localhost:9092");
		properties.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		properties.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		properties.put("transactional.id", "test-producer-" + System.currentTimeMillis());

		// set transaction timeout to low value
		properties.put("transaction.timeout.ms", "100");

		properties.put("enable.idempotence", "true");

		final KafkaProducer<String, String> kafkaProducer = new KafkaProducer<String, String>(properties);
		kafkaProducer.initTransactions();
		kafkaProducer.beginTransaction();

		final String topic = "producer-test";

		kafkaProducer.send(new ProducerRecord<String, String>(topic, 0, null, "message 1 " + System.currentTimeMillis()));

		// Wait for > 60 s which is way longer than the configured transaction.timeout.ms
		// This is necessary due to kafka.coordinator.transaction.TransactionStateManager.DefaultAbortTimedOutTransactionsIntervalMs = 60 s
		Thread.sleep(70000);

		int message2Partition = 0;
		// uncomment line below to publish message 2 to partition 1 and experience silent data loss
		message2Partition = 1;

		kafkaProducer.send(new ProducerRecord<String, String>(topic, message2Partition, null, "message 2 " + System.currentTimeMillis()));

		try {
			// Should throw org.apache.kafka.common.errors.InvalidTxnStateException because
			// transaction.timeout.ms is set to 100. However, if message 2 is published to
			// partition 1, commitTransaction() will not throw an exception but only message 2 is
			// published to a consumer with read_commited isolation level.
			kafkaProducer.commitTransaction();
		} finally {
			kafkaProducer.close();
		}
	}
}
