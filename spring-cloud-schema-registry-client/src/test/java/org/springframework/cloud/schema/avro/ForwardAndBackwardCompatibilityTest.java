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
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import example.avro.v2.User;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.generic.GenericRecordBuilder;
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
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Christian Tzolov
 */
public class ForwardAndBackwardCompatibilityTest {

	static SchemaRegistryClient stubSchemaRegistryClient = new StubSchemaRegistryClient();

	public static final String NO_EXPLICIT_V1_SCHEMA = null;

	public static final String NO_EXPLICIT_V2_SCHEMA = null;

	public static final String NO_READER_SCHEMA = null;

	public static final boolean NO_DYNAMIC_SCHEMA_GENERATION = false;

	public static final boolean ENABLE_DYNAMIC_SCHEMA_GENERATION = true;

	@Test
	public void genericRecordBackwardCompatibility() throws Exception {

		Schema s1 = new Schema.Parser().parse(
				new DefaultResourceLoader().getResource("classpath:schemas/user.avsc").getInputStream());

		GenericRecord user1 = new GenericRecordBuilder(s1)
				.set("name", "foo" + UUID.randomUUID().toString())
				.set("favoriteColor", "foo" + UUID.randomUUID().toString())
				.set("favoriteNumber", 12)
				.build();

		Schema s2 = new Schema.Parser().parse(
				new DefaultResourceLoader().getResource("classpath:schemas/user_v2.avsc").getInputStream());

		GenericRecord user2 = new GenericRecordBuilder(s2)
				.set("name", "foo" + UUID.randomUUID().toString())
				.set("favoriteColor", "foo" + UUID.randomUUID().toString())
				.set("favoriteNumber", 13)
				.set("favoritePlace", "Amsterdam")
				.build();

		List<?> result = compatibilityTest(user1, user2,
				AvroSinkApplicationGenericRecord.class,
				NO_EXPLICIT_V1_SCHEMA,
				NO_EXPLICIT_V2_SCHEMA,
				"classpath:schemas/user_v2.avsc",
				NO_DYNAMIC_SCHEMA_GENERATION);

		GenericData.Record resultUser1 = (GenericData.Record) result.get(0);
		GenericData.Record resultUser2 = (GenericData.Record) result.get(1);

		assertThat(resultUser1.getSchema()).isEqualTo(s2);
		assertThat(resultUser2.getSchema()).isEqualTo(s2);

		assertThat(resultUser1.get("favoriteColor").toString()).isEqualTo(user1.get("favoriteColor").toString());
		assertThat(resultUser1.get("name").toString()).isEqualTo(user1.get("name").toString());
		assertThat(resultUser1.get("favoritePlace").toString()).isEqualTo("NYC");
		assertThat(resultUser1.get("favoriteNumber")).isEqualTo(user1.get("favoriteNumber"));

		assertThat(resultUser2.get("favoriteColor").toString()).isEqualTo(user2.get("favoriteColor").toString());
		assertThat(resultUser2.get("name").toString()).isEqualTo(user2.get("name").toString());
		assertThat(resultUser2.get("favoritePlace").toString()).isEqualTo(user2.get("favoritePlace").toString());
		assertThat(resultUser2.get("favoriteNumber")).isEqualTo(user2.get("favoriteNumber"));
	}

	@Test
	public void genericRecordForwardCompatibility() throws Exception {

		Schema s1 = new Schema.Parser().parse(
				new DefaultResourceLoader().getResource("classpath:schemas/user.avsc").getInputStream());

		GenericRecord user1 = new GenericRecordBuilder(s1)
				.set("name", "foo" + UUID.randomUUID().toString())
				.set("favoriteColor", "foo" + UUID.randomUUID().toString())
				.set("favoriteNumber", 12)
				.build();

		Schema s2 = new Schema.Parser().parse(
				new DefaultResourceLoader().getResource("classpath:schemas/user_v2.avsc").getInputStream());

		GenericRecord user2 = new GenericRecordBuilder(s2)
				.set("name", "foo" + UUID.randomUUID().toString())
				.set("favoriteColor", "foo" + UUID.randomUUID().toString())
				.set("favoriteNumber", 13)
				.set("favoritePlace", "Amsterdam")
				.build();

		List<?> result = compatibilityTest(user1, user2,
				AvroSinkApplicationGenericRecord.class,
				NO_EXPLICIT_V1_SCHEMA,
				NO_EXPLICIT_V2_SCHEMA,
				"classpath:schemas/user.avsc",
				NO_DYNAMIC_SCHEMA_GENERATION);

		GenericData.Record resultUser1 = (GenericData.Record) result.get(0);
		GenericData.Record resultUser2 = (GenericData.Record) result.get(1);

		assertThat(resultUser1.getSchema()).isEqualTo(s1);
		assertThat(resultUser2.getSchema()).isEqualTo(s1);

		assertThat(resultUser1.get("favoriteColor").toString()).isEqualTo(user1.get("favoriteColor").toString());
		assertThat(resultUser1.get("name").toString()).isEqualTo(user1.get("name").toString());
		assertThat(resultUser1.get("favoriteNumber")).isEqualTo(user1.get("favoriteNumber"));

		assertThat(resultUser2.get("favoriteColor").toString()).isEqualTo(user2.get("favoriteColor").toString());
		assertThat(resultUser2.get("name").toString()).isEqualTo(user2.get("name").toString());
		assertThat(resultUser2.get("favoriteNumber")).isEqualTo(user2.get("favoriteNumber"));
	}

	@Test
	public void genericRecordNoReaderSchema() throws Exception {

		Schema s1 = new Schema.Parser().parse(
				new DefaultResourceLoader().getResource("classpath:schemas/user.avsc").getInputStream());

		GenericRecord user1 = new GenericRecordBuilder(s1)
				.set("name", "foo" + UUID.randomUUID().toString())
				.set("favoriteColor", "foo" + UUID.randomUUID().toString())
				.set("favoriteNumber", 12)
				.build();

		Schema s2 = new Schema.Parser().parse(
				new DefaultResourceLoader().getResource("classpath:schemas/user_v2.avsc").getInputStream());

		GenericRecord user2 = new GenericRecordBuilder(s2)
				.set("name", "foo" + UUID.randomUUID().toString())
				.set("favoriteColor", "foo" + UUID.randomUUID().toString())
				.set("favoriteNumber", 13)
				.set("favoritePlace", "Amsterdam")
				.build();

		List<?> result = compatibilityTest(user1, user2,
				AvroSinkApplicationGenericRecord.class,
				NO_EXPLICIT_V1_SCHEMA,
				NO_EXPLICIT_V2_SCHEMA,
				NO_READER_SCHEMA,
				NO_DYNAMIC_SCHEMA_GENERATION);

		GenericData.Record resultUser1 = (GenericData.Record) result.get(0);
		GenericData.Record resultUser2 = (GenericData.Record) result.get(1);

		assertThat(resultUser1.getSchema()).isEqualTo(s1);
		assertThat(resultUser2.getSchema()).isEqualTo(s2);

		assertThat(resultUser1.get("favoriteColor").toString()).isEqualTo(user1.get("favoriteColor").toString());
		assertThat(resultUser1.get("name").toString()).isEqualTo(user1.get("name").toString());
		assertThat(resultUser1.get("favoriteNumber")).isEqualTo(user1.get("favoriteNumber"));

		assertThat(resultUser2.get("favoriteColor").toString()).isEqualTo(user2.get("favoriteColor").toString());
		assertThat(resultUser2.get("name").toString()).isEqualTo(user2.get("name").toString());
		assertThat(resultUser2.get("favoriteNumber")).isEqualTo(user2.get("favoriteNumber"));
		assertThat(resultUser2.get("favoritePlace").toString()).isEqualTo(user2.get("favoritePlace").toString());
	}

	@Test
	public void specificRecordBackwardCompatibility() throws Exception {

		example.avro.User user1 = new example.avro.User();
		user1.setFavoriteColor("foo" + UUID.randomUUID().toString());
		user1.setName("foo" + UUID.randomUUID().toString());

		example.avro.v2.User user2 = new example.avro.v2.User();
		user2.setFavoriteColor("foo" + UUID.randomUUID().toString());
		user2.setName("foo" + UUID.randomUUID().toString());
		user2.setFavoritePlace("Amsterdam");

		List<?> result = compatibilityTest(user1, user2,
				AvroSinkApplicationSpecificRecord.class,
				NO_EXPLICIT_V1_SCHEMA,
				NO_EXPLICIT_V2_SCHEMA,
				"classpath:schemas/user_v2.avsc",
				NO_DYNAMIC_SCHEMA_GENERATION);

		example.avro.v2.User resultUser1 = (User) result.get(0);
		example.avro.v2.User resultUser2 = (User) result.get(1);

		assertThat(resultUser1.getFavoriteColor().toString()).isEqualTo(user1.getFavoriteColor().toString());
		assertThat(resultUser1.getName().toString()).isEqualTo(user1.getName().toString());
		assertThat(resultUser1.getFavoritePlace().toString()).isEqualTo("NYC");

		assertThat(resultUser2.getFavoriteColor().toString()).isEqualTo(user2.getFavoriteColor().toString());
		assertThat(resultUser2.getName().toString()).isEqualTo(user2.getName().toString());
		assertThat(resultUser2.getFavoritePlace().toString()).isEqualTo("Amsterdam");
	}

	@Test
	public void specificRecordForwardCompatibility() throws Exception {

		example.avro.User user1 = new example.avro.User();
		user1.setFavoriteColor("foo" + UUID.randomUUID().toString());
		user1.setName("foo" + UUID.randomUUID().toString());

		example.avro.v2.User user2 = new example.avro.v2.User();
		user2.setFavoriteColor("foo" + UUID.randomUUID().toString());
		user2.setName("foo" + UUID.randomUUID().toString());
		user2.setFavoritePlace("Amsterdam");

		List<?> result = compatibilityTest(user1, user2,
				AvroSinkApplicationSpecificRecord.class,
				NO_EXPLICIT_V1_SCHEMA,
				NO_EXPLICIT_V2_SCHEMA,
				"classpath:schemas/user.avsc",
				NO_DYNAMIC_SCHEMA_GENERATION);

		example.avro.User resultUser1 = (example.avro.User) result.get(0);
		example.avro.User resultUser2 = (example.avro.User) result.get(1);

		assertThat(resultUser1.getFavoriteColor().toString()).isEqualTo(user1.getFavoriteColor().toString());
		assertThat(resultUser1.getName().toString()).isEqualTo(user1.getName().toString());

		assertThat(resultUser2.getFavoriteColor().toString()).isEqualTo(user2.getFavoriteColor().toString());
		assertThat(resultUser2.getName().toString()).isEqualTo(user2.getName().toString());
	}

	@Test(expected = MessagingException.class)
	public void specificRecordNoReaderSchema() throws Exception {

		example.avro.User user1 = new example.avro.User();
		user1.setFavoriteColor("foo" + UUID.randomUUID().toString());
		user1.setName("foo" + UUID.randomUUID().toString());

		example.avro.v2.User user2 = new example.avro.v2.User();
		user2.setFavoriteColor("foo" + UUID.randomUUID().toString());
		user2.setName("foo" + UUID.randomUUID().toString());
		user2.setFavoritePlace("Amsterdam");

		compatibilityTest(user1, user2, AvroSinkApplicationSpecificRecord.class,
				NO_EXPLICIT_V1_SCHEMA,
				NO_EXPLICIT_V2_SCHEMA,
				NO_READER_SCHEMA,
				NO_DYNAMIC_SCHEMA_GENERATION);
	}

	@Test
	public void javaTypeBackwardCompatibility() throws Exception {
		org.springframework.cloud.schema.avro.User1 user1 = new org.springframework.cloud.schema.avro.User1();
		user1.setFavoriteColor("foo" + UUID.randomUUID().toString());
		user1.setName("foo" + UUID.randomUUID().toString());

		org.springframework.cloud.schema.avro.v2.User1 user2 = new org.springframework.cloud.schema.avro.v2.User1();
		user2.setFavoriteColor("foo" + UUID.randomUUID().toString());
		user2.setName("foo" + UUID.randomUUID().toString());
		user2.setFavoritePlace("Amsterdam");

		List<?> result = compatibilityTest(user1, user2,
				AvroSinkApplicationUser1V2.class, // Source with User1 v1 payload type
				"classpath:schemas/user1_v1.schema",
				"classpath:schemas/user1_v2.schema",
				NO_READER_SCHEMA, // the readerSchema is IGNORED for java type pojos
				NO_DYNAMIC_SCHEMA_GENERATION);

		org.springframework.cloud.schema.avro.v2.User1 resultUser1 = (org.springframework.cloud.schema.avro.v2.User1) result.get(0);
		org.springframework.cloud.schema.avro.v2.User1 resultUser2 = (org.springframework.cloud.schema.avro.v2.User1) result.get(1);

		assertThat(resultUser1.getFavoriteColor()).isEqualTo(user1.getFavoriteColor());
		assertThat(resultUser1.getName()).isEqualTo(user1.getName());
		assertThat(resultUser1.getFavoritePlace()).isEqualTo("NYC");

		assertThat(resultUser2.getFavoriteColor()).isEqualTo(user2.getFavoriteColor());
		assertThat(resultUser2.getName()).isEqualTo(user2.getName());
		assertThat(resultUser2.getFavoritePlace()).isEqualTo("Amsterdam");
	}

	@Test
	public void javaTypeForwardCompatibility() throws Exception {
		org.springframework.cloud.schema.avro.User1 user1 = new org.springframework.cloud.schema.avro.User1();
		user1.setFavoriteColor("foo" + UUID.randomUUID().toString());
		user1.setName("foo" + UUID.randomUUID().toString());

		org.springframework.cloud.schema.avro.v2.User1 user2 = new org.springframework.cloud.schema.avro.v2.User1();
		user2.setFavoriteColor("foo" + UUID.randomUUID().toString());
		user2.setName("foo" + UUID.randomUUID().toString());
		user2.setFavoritePlace("Amsterdam");

		List<?> result = compatibilityTest(user1, user2,
				AvroSinkApplicationUser1V1.class, // Source with User1 v1 payload type
				"classpath:schemas/user1_v1.schema",
				"classpath:schemas/user1_v2.schema",
				NO_READER_SCHEMA, // the readerSchema is IGNORED for java type pojos
				NO_DYNAMIC_SCHEMA_GENERATION);

		org.springframework.cloud.schema.avro.User1 resultUser1 = (org.springframework.cloud.schema.avro.User1) result.get(0);
		org.springframework.cloud.schema.avro.User1 resultUser2 = (org.springframework.cloud.schema.avro.User1) result.get(1);

		assertThat(resultUser1.getFavoriteColor()).isEqualTo(user1.getFavoriteColor());
		assertThat(resultUser1.getName()).isEqualTo(user1.getName());

		assertThat(resultUser2.getFavoriteColor()).isEqualTo(user2.getFavoriteColor());
		assertThat(resultUser2.getName()).isEqualTo(user2.getName());
	}

	@Test(expected = MessageDeliveryException.class)
	public void javaTypeWithoutSourceSchemas() throws Exception {
		org.springframework.cloud.schema.avro.User1 user1 = new org.springframework.cloud.schema.avro.User1();
		user1.setFavoriteColor("foo" + UUID.randomUUID().toString());
		user1.setName("foo" + UUID.randomUUID().toString());

		org.springframework.cloud.schema.avro.v2.User1 user2 = new org.springframework.cloud.schema.avro.v2.User1();
		user2.setFavoriteColor("foo" + UUID.randomUUID().toString());
		user2.setName("foo" + UUID.randomUUID().toString());
		user2.setFavoritePlace("Amsterdam");

		compatibilityTest(user1, user2,
				AvroSinkApplicationUser1V1.class, // Source with User1 v1 payload type
				NO_EXPLICIT_V1_SCHEMA,
				NO_EXPLICIT_V2_SCHEMA,
				NO_READER_SCHEMA, // the readerSchema is IGNORED for java type pojos
				NO_DYNAMIC_SCHEMA_GENERATION);
	}

	@Test
	public void javaTypeWithDynamicSchemaGeneration() throws Exception {
		org.springframework.cloud.schema.avro.User1 user1 = new org.springframework.cloud.schema.avro.User1();
		user1.setFavoriteColor("foo" + UUID.randomUUID().toString());
		user1.setName("foo" + UUID.randomUUID().toString());

		org.springframework.cloud.schema.avro.v2.User1 user2 = new org.springframework.cloud.schema.avro.v2.User1();
		user2.setFavoriteColor("foo" + UUID.randomUUID().toString());
		user2.setName("foo" + UUID.randomUUID().toString());
		user2.setFavoritePlace("Amsterdam");

		List<?> result = compatibilityTest(user1, user2,
				AvroSinkApplicationUser1V1.class, // Source with User1 v1 payload type
				NO_EXPLICIT_V1_SCHEMA,
				NO_EXPLICIT_V2_SCHEMA,
				NO_READER_SCHEMA, // the readerSchema is IGNORED for java type pojos
				ENABLE_DYNAMIC_SCHEMA_GENERATION);

		org.springframework.cloud.schema.avro.User1 resultUser1 = (org.springframework.cloud.schema.avro.User1) result.get(0);
		org.springframework.cloud.schema.avro.User1 resultUser2 = (org.springframework.cloud.schema.avro.User1) result.get(1);

		assertThat(resultUser1.getFavoriteColor()).isEqualTo(user1.getFavoriteColor());
		assertThat(resultUser1.getName()).isEqualTo(user1.getName());

		assertThat(resultUser2.getFavoriteColor()).isEqualTo(user2.getFavoriteColor());
		assertThat(resultUser2.getName()).isEqualTo(user2.getName());
	}

	public <U1, U2, S extends TestSinkApplication> List<?> compatibilityTest(
			U1 user1,
			U2 user2,
			Class<S> sinkApplicationClass,
			String user1Schema,
			String user2Schema,
			String readerSchema,
			boolean dynamicSchemaGenerationEnabled) throws Exception {

		List<String> commonSourceArguments = Arrays.asList("--server.port=0", "--spring.jmx.enabled=false",
				"--spring.cloud.stream.bindings.output.contentType=application/*+avro");

		// Source 1
		List<String> source1Args = new ArrayList<>(commonSourceArguments);
		if (user1Schema != null) {
			source1Args.add("--spring.cloud.schema.avro.schema-locations=" + user1Schema);
		}
		source1Args.add("--spring.cloud.schema.avro.dynamicSchemaGenerationEnabled=" + dynamicSchemaGenerationEnabled);

		ConfigurableApplicationContext sourceContext1 = SpringApplication.run(
				AvroSourceApplication.class, source1Args.toArray(new String[source1Args.size()]));

		Source source1 = sourceContext1.getBean(Source.class);
		source1.output().send(MessageBuilder.withPayload(user1).build());

		MessageCollector sourceMessageCollector = sourceContext1.getBean(MessageCollector.class);
		Message<?> outboundMessage = sourceMessageCollector.forChannel(source1.output()).poll(1000, TimeUnit.MILLISECONDS);

		// Source2 2
		List<String> source2Args = new ArrayList<>(commonSourceArguments);
		if (user2Schema != null) {
			source2Args.add("--spring.cloud.schema.avro.schema-locations=" + user2Schema);
		}
		source2Args.add("--spring.cloud.schema.avro.dynamicSchemaGenerationEnabled=" + dynamicSchemaGenerationEnabled);

		ConfigurableApplicationContext sourceContext2 = SpringApplication.run(
				AvroSourceApplication.class, source2Args.toArray(new String[source2Args.size()]));
		Source source2 = sourceContext2.getBean(Source.class);
		source2.output().send(MessageBuilder.withPayload(user2).build());

		MessageCollector barSourceMessageCollector = sourceContext2.getBean(MessageCollector.class);
		Message<?> barOutboundMessage = barSourceMessageCollector.forChannel(source2.output()).poll(1000, TimeUnit.MILLISECONDS);

		assertThat(barOutboundMessage).isNotNull();

		// Sink 1
		List<String> sinkArgs = new ArrayList<>(Arrays.asList("--server.port=0", "--spring.jmx.enabled=false"));
		if (StringUtils.hasText(readerSchema)) {
			sinkArgs.add("--spring.cloud.schema.avro.reader-schema=" + readerSchema);
		}
		ConfigurableApplicationContext sinkContext =
				SpringApplication.run(sinkApplicationClass, sinkArgs.toArray(new String[sinkArgs.size()]));

		Sink sink = sinkContext.getBean(Sink.class);
		sink.input().send(outboundMessage);
		sink.input().send(barOutboundMessage);

		List<?> receivedPojos = sinkContext.getBean(sinkApplicationClass).getReceivedPojos();

		Assertions.assertThat(receivedPojos).hasSize(2);

		sourceContext1.close();
		sourceContext2.close();
		sinkContext.close();

		return receivedPojos;
	}

	interface TestSinkApplication {
		List<?> getReceivedPojos();
	}

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
	public static class AvroSinkApplication2<T> implements TestSinkApplication {

		public List<T> receivedPojos = new ArrayList<>();


		@StreamListener(Sink.INPUT)
		public void listen(T fooPojo) {
			this.receivedPojos.add(fooPojo);
		}

		@Bean
		public SchemaRegistryClient schemaRegistryClient() {
			return stubSchemaRegistryClient;
		}

		@Override
		public List<?> getReceivedPojos() {
			return this.receivedPojos;
		}
	}

	public static class AvroSinkApplicationGenericRecord extends AvroSinkApplication2<GenericRecord> {

	}

	public static class AvroSinkApplicationSpecificRecord extends AvroSinkApplication2<org.apache.avro.specific.SpecificRecord> {

	}

	public static class AvroSinkApplicationUser1V1 extends AvroSinkApplication2<org.springframework.cloud.schema.avro.User1> {

	}

	public static class AvroSinkApplicationUser1V2 extends AvroSinkApplication2<org.springframework.cloud.schema.avro.v2.User1> {

	}
}
