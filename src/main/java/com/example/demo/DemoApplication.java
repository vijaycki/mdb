package com.example.demo;

import org.bson.Document;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.TransactionOptions;
import com.mongodb.WriteConcern;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

@SpringBootApplication
public class DemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);

		System.out.println("done launching sp");

		MongoClient mongoClient = MongoClients.create("mongodb://localhost:27017");
		ClientSession session = mongoClient.startSession();
		TransactionOptions txnOptions = TransactionOptions
				.builder()
				.readPreference(ReadPreference.primary())
				.readConcern(ReadConcern.MAJORITY)
				.writeConcern(WriteConcern.MAJORITY)
				.build();
		session.startTransaction(txnOptions);
		
		MongoDatabase db = mongoClient.getDatabase("test");
		MongoCollection<Document> collection = db.getCollection("test1Collection");
		Document doc = Document.parse("{ \"name\": \"vijay\"}");
		collection.insertOne(session, doc);
		System.out.println ( collection.countDocuments());
		session.commitTransaction();
		System.out.println ( collection.countDocuments());
		
		System.out.println("done");

	}

}
