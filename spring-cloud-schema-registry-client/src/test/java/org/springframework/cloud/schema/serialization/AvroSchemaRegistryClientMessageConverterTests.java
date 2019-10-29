/*
 * Copyright 2016-2019 the original author or authors.
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

package org.springframework.cloud.schema.serialization;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import example.avro.Command;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mockito;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.NoOpCache;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.cloud.schema.avro.User1;
import org.springframework.cloud.schema.avro.User2;
import org.springframework.cloud.schema.registry.EnableSchemaRegistryServer;
import org.springframework.cloud.schema.registry.avro.AvroSchemaRegistryClientMessageConverter;
import org.springframework.cloud.schema.registry.avro.AvroSchemaServiceManager;
import org.springframework.cloud.schema.registry.avro.AvroSchemaServiceManagerImpl;
import org.springframework.cloud.schema.registry.client.DefaultSchemaRegistryClient;
import org.springframework.cloud.schema.registry.client.EnableSchemaRegistryClient;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.cloud.stream.messaging.Source;
import org.springframework.cloud.stream.test.binder.MessageCollector;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.cloud.schema.serialization.AvroMessageConverterSerializationTests.notification;

/**
 * @author Marius Bogoevici
 * @author Oleg Zhurakousky
 * @author Sercan Karaoglu
 * @author James Gee
 * @author Christian Tzolov
 */
@RunWith(Parameterized.class)
public class AvroSchemaRegistryClientMessageConverterTests {

	private String propertyPrefix;

	private ConfigurableApplicationContext schemaRegistryServerContext;

	public AvroSchemaRegistryClientMessageConverterTests(String propertyPrefix) {
		this.propertyPrefix = propertyPrefix;
	}

	// Use parametrization to test the deprecated prefix (spring.cloud.stream) is handled as the new (spring.cloud)
	// prefix.
	@Parameterized.Parameters
	public static Collection primeNumbers() {
		return Arrays.asList("spring.cloud.stream", "spring.cloud");
	}

	@Before
	public void setup() {
		this.schemaRegistryServerContext = SpringApplication.run(
				ServerApplication.class,
				"--spring.main.allow-bean-definition-overriding=true");
	}

	@After
	public void tearDown() {
		this.schemaRegistryServerContext.close();
	}

	@Test
	public void testSendMessage() throws Exception {

		ConfigurableApplicationContext sourceContext = SpringApplication.run(
				AvroSourceApplication.class, "--server.port=0",
				"--spring.jmx.enabled=false",
				"--spring.cloud.stream.bindings.output.contentType=application/*+avro",
				"--" + propertyPrefix + ".schema.avro.dynamicSchemaGenerationEnabled=true");
		Source source = sourceContext.getBean(Source.class);
		User1 firstOutboundFoo = new User1();
		firstOutboundFoo.setFavoriteColor("foo" + UUID.randomUUID().toString());
		firstOutboundFoo.setName("foo" + UUID.randomUUID().toString());
		source.output().send(MessageBuilder.withPayload(firstOutboundFoo).build());
		MessageCollector sourceMessageCollector = sourceContext
				.getBean(MessageCollector.class);
		Message<?> outboundMessage = sourceMessageCollector.forChannel(source.output())
				.poll(1000, TimeUnit.MILLISECONDS);

		ConfigurableApplicationContext barSourceContext = SpringApplication.run(
				AvroSourceApplication.class, "--server.port=0",
				"--spring.jmx.enabled=false",
				"--spring.cloud.stream.bindings.output.contentType=application/vnd.user1.v1+avro",
				"--" + propertyPrefix + ".schema.avro.dynamicSchemaGenerationEnabled=true");
		Source barSource = barSourceContext.getBean(Source.class);
		User2 firstOutboundUser2 = new User2();
		firstOutboundUser2.setFavoriteColor("foo" + UUID.randomUUID().toString());
		firstOutboundUser2.setName("foo" + UUID.randomUUID().toString());
		barSource.output().send(MessageBuilder.withPayload(firstOutboundUser2).build());
		MessageCollector barSourceMessageCollector = barSourceContext
				.getBean(MessageCollector.class);
		Message<?> barOutboundMessage = barSourceMessageCollector
				.forChannel(barSource.output()).poll(1000, TimeUnit.MILLISECONDS);

		assertThat(barOutboundMessage).isNotNull();

		User2 secondBarOutboundPojo = new User2();
		secondBarOutboundPojo.setFavoriteColor("foo" + UUID.randomUUID().toString());
		secondBarOutboundPojo.setName("foo" + UUID.randomUUID().toString());
		source.output().send(MessageBuilder.withPayload(secondBarOutboundPojo).build());
		Message<?> secondBarOutboundMessage = sourceMessageCollector
				.forChannel(source.output()).poll(1000, TimeUnit.MILLISECONDS);

		ConfigurableApplicationContext sinkContext = SpringApplication.run(
				AvroSinkApplication.class, "--server.port=0",
				"--spring.jmx.enabled=false");
		Sink sink = sinkContext.getBean(Sink.class);
		sink.input().send(outboundMessage);
		sink.input().send(barOutboundMessage);
		sink.input().send(secondBarOutboundMessage);
		List<User2> receivedPojos = sinkContext
				.getBean(AvroSinkApplication.class).receivedPojos;
		assertThat(receivedPojos).hasSize(3);
		assertThat(receivedPojos.get(0)).isNotSameAs(firstOutboundFoo);
		assertThat(receivedPojos.get(0).getFavoriteColor())
				.isEqualTo(firstOutboundFoo.getFavoriteColor());
		assertThat(receivedPojos.get(0).getName()).isEqualTo(firstOutboundFoo.getName());
		assertThat(receivedPojos.get(0).getFavoritePlace()).isEqualTo("NYC");

		assertThat(receivedPojos.get(1)).isNotSameAs(firstOutboundUser2);
		assertThat(receivedPojos.get(1).getFavoriteColor())
				.isEqualTo(firstOutboundUser2.getFavoriteColor());
		assertThat(receivedPojos.get(1).getName())
				.isEqualTo(firstOutboundUser2.getName());
		assertThat(receivedPojos.get(1).getFavoritePlace()).isEqualTo("Boston");

		assertThat(receivedPojos.get(2)).isNotSameAs(secondBarOutboundPojo);
		assertThat(receivedPojos.get(2).getFavoriteColor())
				.isEqualTo(secondBarOutboundPojo.getFavoriteColor());
		assertThat(receivedPojos.get(2).getName())
				.isEqualTo(secondBarOutboundPojo.getName());
		assertThat(receivedPojos.get(2).getFavoritePlace())
				.isEqualTo(secondBarOutboundPojo.getFavoritePlace());

		sinkContext.close();
		barSourceContext.close();
		sourceContext.close();
		this.schemaRegistryServerContext.close();
	}

	@Test
	public void testSchemaImportConfiguration() throws Exception {
		final String[] args = { "--server.port=0", "--spring.jmx.enabled=false",
				"--" + propertyPrefix + ".schema.avro.dynamicSchemaGenerationEnabled=true",
				"--spring.cloud.stream.bindings.output.contentType=application/*+avro",
				"--spring.cloud.stream.bindings.output.destination=test",
				"--spring.cloud.stream.bindings.schema-registry-client.endpoint=http://localhost:8990",
				"--" + propertyPrefix + ".schema.avro.schema-locations=classpath:schemas/Command.avsc",
				"--" + propertyPrefix + ".schema.avro.schema-imports=classpath:schemas/imports/Sms.avsc,"
						+ " classpath:schemas/imports/Email.avsc, classpath:schemas/imports/PushNotification.avsc" };

		final ConfigurableApplicationContext sourceContext = SpringApplication
				.run(AvroSourceApplication.class, args);
		final ConfigurableApplicationContext sinkContext = SpringApplication
				.run(CommandSinkApplication.class, args);
		final Source barSource = sourceContext.getBean(Source.class);
		final Command notification = notification();
		barSource.output().send(MessageBuilder.withPayload(notification).build());
		final MessageCollector barSourceMessageCollector = sourceContext
				.getBean(MessageCollector.class);
		final Message<?> outboundMessage = barSourceMessageCollector
				.forChannel(barSource.output()).poll(1000, TimeUnit.MILLISECONDS);
		assertThat(outboundMessage).isNotNull();
		Sink sink = sinkContext.getBean(Sink.class);
		sink.input().send(outboundMessage);
		List<Command> receivedPojos = sinkContext
				.getBean(CommandSinkApplication.class).receivedPojos;

		assertThat(receivedPojos).hasSize(1);
		assertThat(receivedPojos.get(0)).isEqualTo(notification);

	}

	@Test
	public void testNoCacheConfiguration() {
		if (propertyPrefix.equalsIgnoreCase("spring.cloud.stream")) {
			ConfigurableApplicationContext sourceContext = SpringApplication
					.run(NoCacheConfiguration.class, "--spring.main.web-environment=false");
			AvroSchemaRegistryClientMessageConverter converter = sourceContext
					.getBean(AvroSchemaRegistryClientMessageConverter.class);
			DirectFieldAccessor accessor = new DirectFieldAccessor(converter);
			assertThat(accessor.getPropertyValue("cacheManager"))
					.isInstanceOf(NoOpCacheManager.class);
		}
	}

	@Test
	public void testNamedCacheIsRequested() {
		CacheManager mockCache = Mockito.mock(CacheManager.class);
		when(mockCache.getCache(any())).thenReturn(new NoOpCache(""));
		AvroSchemaServiceManager manager = new AvroSchemaServiceManagerImpl();
		AvroSchemaRegistryClientMessageConverter converter = new AvroSchemaRegistryClientMessageConverter(new DefaultSchemaRegistryClient(), mockCache, manager);
		ReflectionTestUtils.invokeMethod(converter, "getCache", "TEST_CACHE");
		verify(mockCache).getCache("TEST_CACHE");
	}

	@EnableBinding(Source.class)
	@EnableAutoConfiguration
	@EnableSchemaRegistryClient
	public static class AvroSourceApplication {

	}

	@EnableBinding(Sink.class)
	@EnableAutoConfiguration
	@EnableSchemaRegistryClient
	public static class AvroSinkApplication {

		public List<User2> receivedPojos = new ArrayList<>();

		@StreamListener(Sink.INPUT)
		public void listen(User2 fooPojo) {
			this.receivedPojos.add(fooPojo);
		}

	}

	@EnableBinding(Sink.class)
	@EnableAutoConfiguration
	@EnableSchemaRegistryClient
	public static class CommandSinkApplication {

		public List<Command> receivedPojos = new ArrayList<>();

		@StreamListener(Sink.INPUT)
		public void listen(Command fooPojo) {
			this.receivedPojos.add(fooPojo);
		}

	}

	@Configuration
	public static class NoCacheConfiguration {

		@Bean
		AvroSchemaRegistryClientMessageConverter avroSchemaRegistryClientMessageConverter() {
			AvroSchemaServiceManager manager = new AvroSchemaServiceManagerImpl();
			return new AvroSchemaRegistryClientMessageConverter(
					new DefaultSchemaRegistryClient(), new NoOpCacheManager(), manager);
		}

		@Bean
		ServletWebServerFactory servletWebServerFactory() {
			return new TomcatServletWebServerFactory();
		}

	}

	@SpringBootApplication
	@EnableSchemaRegistryServer
	public static class ServerApplication {
		public static void main(String[] args) {
			SpringApplication.run(AvroMessageConverterSerializationTests.ServerApplication.class, args);
		}
	}

}
