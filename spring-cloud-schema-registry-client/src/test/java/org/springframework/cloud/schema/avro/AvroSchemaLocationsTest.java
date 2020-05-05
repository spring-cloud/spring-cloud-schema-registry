/*
 * Copyright 2020-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.schema.avro;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cloud.schema.registry.client.SchemaRegistryClient;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.cloud.stream.messaging.Source;
import org.springframework.cloud.stream.test.binder.MessageCollector;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Christian Tzolov
 */
public class AvroSchemaLocationsTest {

	@Test
	public void schemaLocationWithMultipleRecords() throws Exception {
		testMultipleRecordsSchemaLoading(
				"--spring.cloud.schema.avro.schema-locations=classpath:schemas/user1_multiple_records.schema");
	}

	@Test
	public void schemaImportWithMultipleRecords() throws Exception {
		testMultipleRecordsSchemaLoading(
				"--spring.cloud.schema.avro.schema-imports=classpath:schemas/user1_multiple_records.schema");
	}

	private void testMultipleRecordsSchemaLoading(String schemaLoadingProperty) throws Exception {
		User1 user1 = new User1();
		user1.setFavoriteColor("foo" + UUID.randomUUID().toString());
		user1.setName("foo" + UUID.randomUUID().toString());

		org.springframework.cloud.schema.avro.v2.User1 user2 = new org.springframework.cloud.schema.avro.v2.User1();
		user2.setFavoriteColor("foo" + UUID.randomUUID().toString());
		user2.setName("foo" + UUID.randomUUID().toString());
		user2.setFavoritePlace("Amsterdam");

		// Source
		ConfigurableApplicationContext sourceContext = SpringApplication.run(
				AvroSourceApplication.class, "--server.port=0", "--spring.jmx.enabled=false",
				"--spring.cloud.stream.bindings.output.contentType=application/*+avro",
				schemaLoadingProperty);

		Source source1 = sourceContext.getBean(Source.class);
		source1.output().send(MessageBuilder.withPayload(user1).build());

		MessageCollector sourceMessageCollector = sourceContext.getBean(MessageCollector.class);
		Message<?> message1 = sourceMessageCollector.forChannel(source1.output()).poll(1000, TimeUnit.MILLISECONDS);

		source1.output().send(MessageBuilder.withPayload(user2).build());

		MessageCollector barSourceMessageCollector = sourceContext.getBean(MessageCollector.class);
		Message<?> message2 = barSourceMessageCollector.forChannel(source1.output()).poll(1000, TimeUnit.MILLISECONDS);

		assertThat(message2).isNotNull();

		// Sink
		ConfigurableApplicationContext sinkContext =
				SpringApplication.run(AvroSinkApplication.class, "--server.port=0", "--spring.jmx.enabled=false");

		Sink sink = sinkContext.getBean(Sink.class);
		sink.input().send(message1);
		sink.input().send(message2);

		List<?> result = sinkContext.getBean(AvroSinkApplication.class).getReceivedPojos();

		Assertions.assertThat(result).hasSize(2);

		sourceContext.close();
		sinkContext.close();

		User1 resultUser1 = (User1) result.get(0);
		User1 resultUser2 = (User1) result.get(1);

		assertThat(resultUser1.getFavoriteColor()).isEqualTo(user1.getFavoriteColor());
		assertThat(resultUser1.getName()).isEqualTo(user1.getName());

		assertThat(resultUser2.getFavoriteColor()).isEqualTo(user2.getFavoriteColor());
		assertThat(resultUser2.getName()).isEqualTo(user2.getName());
	}

	static SchemaRegistryClient stubSchemaRegistryClient = new StubSchemaRegistryClient();

	@EnableBinding(Source.class)
	@EnableAutoConfiguration
	public static class AvroSourceApplication {

		@Bean
		public SchemaRegistryClient schemaRegistryClient() {
			return stubSchemaRegistryClient;
		}

	}

	@EnableBinding(Sink.class)
	@EnableAutoConfiguration
	public static class AvroSinkApplication {

		public List<User1> receivedPojos = new ArrayList<>();


		@StreamListener(Sink.INPUT)
		public void listen(User1 fooPojo) {
			this.receivedPojos.add(fooPojo);
		}

		@Bean
		public SchemaRegistryClient schemaRegistryClient() {
			return stubSchemaRegistryClient;
		}

		public List<User1> getReceivedPojos() {
			return this.receivedPojos;
		}
	}
}
