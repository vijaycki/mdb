package com.example.demo;

import java.io.IOException;
import java.net.UnknownHostException;

import org.bson.Document;
import org.springframework.boot.autoconfigure.AbstractDependsOnBeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.mongodb.BasicDBList;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongoCmdOptions;
import de.flapdoodle.embed.mongo.config.MongodConfig;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.config.Storage;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;

@Configuration(proxyBeanMethods = false)
@AutoConfigureBefore({ MongoAutoConfiguration.class })
@ConditionalOnClass({ MongoClient.class, MongodStarter.class })
@Import({ EmbeddedMongoAutoConfiguration.class, EmbeddedMongoWithTransactionsConfig.DependenciesConfiguration.class })
public class EmbeddedMongoWithTransactionsConfig {

	// You may get a warning in the log upon shutdown like this:
	// "...Destroy method 'stop' on bean with name 'embeddedMongoServer' threw an
	// exception: java.lang.IllegalStateException: Couldn't kill mongod process!..."
	// That seems harmless as the MongoD process shuts down and frees up the port.
	// There are multiple related issues logged on GitHub:
	// https://github.com/flapdoodle-oss/de.flapdoodle.embed.mongo/issues?q=is%3Aissue+Couldn%27t+kill+mongod+process%21

	public static final int DFLT_PORT_NUMBER = 27017;
	public static final String DFLT_REPLICASET_NAME = "rs0";
	public static final int DFLT_STOP_TIMEOUT_MILLIS = 200;

	private Version.Main mFeatureAwareVersion = Version.Main.V5_0;
	private int mPortNumber = DFLT_PORT_NUMBER;
	private String mReplicaSetName = DFLT_REPLICASET_NAME;
	private long mStopTimeoutMillis = DFLT_STOP_TIMEOUT_MILLIS;
	MongodConfig a;

	@Bean
	public MongodConfig mongodConfig() throws UnknownHostException, IOException {
		final MongodConfig mongodConfig = MongodConfig.builder().version(mFeatureAwareVersion)
				.replication(new Storage("target/mongo/config/", "rs0", 10))

				.stopTimeoutInMillis(mStopTimeoutMillis)
				.cmdOptions(MongoCmdOptions.builder().useNoJournal(false).build())
				.net(new Net(mPortNumber, Network.localhostIsIPv6())).build();
		return mongodConfig;
	}
	
	class EmbeddedMongoReplicaSetInitialization {

        EmbeddedMongoReplicaSetInitialization() throws Exception {
            MongoClient mongoClient = null;
            try {
                final BasicDBList members = new BasicDBList();
                members.add(new Document("_id", 0).append("host", "localhost:" + mPortNumber));

                final Document replSetConfig = new Document("_id", mReplicaSetName);
                replSetConfig.put("members", members);

                mongoClient =
                		MongoClients.create("mongodb://localhost:27017");
                final MongoDatabase adminDatabase = mongoClient.getDatabase("admin");
                adminDatabase.runCommand(new Document("replSetInitiate", replSetConfig));
            }
            finally {
                if (mongoClient != null) {
                    mongoClient.close();
                }
            }
        }
        
	}
	
	@Bean
    EmbeddedMongoReplicaSetInitialization embeddedMongoReplicaSetInitialization() throws Exception {
        return new EmbeddedMongoReplicaSetInitialization();
    }
	@ConditionalOnClass({ MongoClient.class, MongodStarter.class })
    protected static class DependenciesConfiguration
        extends AbstractDependsOnBeanFactoryPostProcessor {

        DependenciesConfiguration() {
            super(EmbeddedMongoReplicaSetInitialization.class, null, MongodExecutable.class);
        }
    }

}
