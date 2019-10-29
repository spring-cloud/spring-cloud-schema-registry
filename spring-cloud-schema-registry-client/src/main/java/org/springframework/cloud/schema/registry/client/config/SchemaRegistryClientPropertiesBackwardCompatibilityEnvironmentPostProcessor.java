/*
 * Copyright 2019-2019 the original author or authors.
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

package org.springframework.cloud.schema.registry.client.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.cloud.schema.registry.AbstractBackwardCompatibilityEnvironmentPostProcessor;

/**
 * Processor overrides deprecated SchemaRegistryClientProperties for backward compatibility.
 *
 * @author Christian Tzolov
 */
public class SchemaRegistryClientPropertiesBackwardCompatibilityEnvironmentPostProcessor extends AbstractBackwardCompatibilityEnvironmentPostProcessor {

	public SchemaRegistryClientPropertiesBackwardCompatibilityEnvironmentPostProcessor() {
		super(SchemaRegistryClientPropertiesBackwardCompatibilityEnvironmentPostProcessor.class.getName());
	}

	@Override
	protected Map<String, String> doGetPropertyMappings() {
		Map<String, String> propertyMapping = new HashMap<>();
		propertyMapping.put("spring.cloud.stream.schema-registry-client.endpoint", "spring.cloud.schema-registry-client.endpoint");
		propertyMapping.put("spring.cloud.stream.schemaRegistryClient.endpoint", "spring.cloud.schemaRegistryClient.endpoint");
		propertyMapping.put("spring.cloud.stream.schema-registry-client.cached", "spring.cloud.schema-registry-client.cached");
		propertyMapping.put("spring.cloud.stream.schemaRegistryClient.cached", "spring.cloud.schemaRegistryClient.cached");
		return propertyMapping;
	}
}
