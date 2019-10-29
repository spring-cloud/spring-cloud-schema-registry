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

package org.springframework.cloud.schema.registry.avro;

import java.util.HashMap;
import java.util.Map;

import org.springframework.cloud.schema.registry.AbstractBackwardCompatibilityEnvironmentPostProcessor;

/**
 * Processor overrides deprecated AvroMessageConverterProperties for backward compatibility.
 *
 * @author Christian Tzolov
 */
public class AvroMessageConverterPropertiesBackwardCompatibilityEnvironmentPostProcessor extends AbstractBackwardCompatibilityEnvironmentPostProcessor {

	public AvroMessageConverterPropertiesBackwardCompatibilityEnvironmentPostProcessor() {
		super(AvroMessageConverterPropertiesBackwardCompatibilityEnvironmentPostProcessor.class.getName());
	}

	@Override
	protected Map<String, String> doGetPropertyMappings() {
		Map<String, String> propertyMapping = new HashMap<>();
		propertyMapping.put("spring.cloud.stream.schema.avro.dynamicSchemaGenerationEnabled", "spring.cloud.schema.avro.dynamicSchemaGenerationEnabled");
		propertyMapping.put("spring.cloud.stream.schema.avro.dynamic-schema-generation-enabled", "spring.cloud.schema.avro.dynamic-schema-generation-enabled");
		propertyMapping.put("spring.cloud.stream.schema.avro.readerSchema", "spring.cloud.schema.avro.readerSchema");
		propertyMapping.put("spring.cloud.stream.schema.avro.reader-schema", "spring.cloud.schema.avro.reader-schema");
		propertyMapping.put("spring.cloud.stream.schema.avro.schemaLocations", "spring.cloud.schema.avro.schemaLocations");
		propertyMapping.put("spring.cloud.stream.schema.avro.schema-locations", "spring.cloud.schema.avro.schema-locations");
		propertyMapping.put("spring.cloud.stream.schema.avro.schemaImports", "spring.cloud.schema.avro.schemaImports");
		propertyMapping.put("spring.cloud.stream.schema.avro.schema-imports", "spring.cloud.schema.avro.schema-imports");
		propertyMapping.put("spring.cloud.stream.schema.avro.prefix", "spring.cloud.schema.avro.prefix");
		propertyMapping.put("spring.cloud.stream.schema.avro.subjectNamingStrategy", "spring.cloud.schema.avro.subjectNamingStrategy");
		propertyMapping.put("spring.cloud.stream.schema.avro.subject-naming-strategy", "spring.cloud.schema.avro.subject-naming-strategy");

		return propertyMapping;
	}
}
