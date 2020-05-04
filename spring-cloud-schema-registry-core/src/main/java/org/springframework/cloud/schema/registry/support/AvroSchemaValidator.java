/*
 * Copyright 2016-2020 the original author or authors.
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

package org.springframework.cloud.schema.registry.support;

import java.util.List;

import org.apache.avro.Schema.Parser;
import org.apache.avro.SchemaParseException;

import org.springframework.cloud.schema.registry.model.Compatibility;
import org.springframework.cloud.schema.registry.model.Schema;

/**
 * @author Vinicius Carvalho
 * @author Christian Tzolov
 */
public class AvroSchemaValidator implements SchemaValidator {

	/**
	 * Unique Avro schema format identifier.
	 */
	public static final String AVRO_FORMAT = "avro";

	private Parser parseReferences(Parser parser, List<Schema> schemaReferences) {
		for (Schema schemaReference : schemaReferences) {
			if (!schemaReference.getReferences().isEmpty()) {
				parser = parseReferences(parser, schemaReference.getReferences());
			}
			parser.parse(schemaReference.getDefinition());
		}
		return parser;
	}

	@Override
	public boolean isValid(String definition, List<Schema> schemaReferences) {
		boolean result = true;
		try {
			Parser avroParser = new Parser();
			if (schemaReferences != null) {
				avroParser = parseReferences(avroParser, schemaReferences);
			}
			avroParser.parse(definition);
		}
		catch (SchemaParseException ex) {
			result = false;
		}
		return result;
	}

	@Override
	public void validate(String definition, List<Schema> schemaReferences) {
		try {
			Parser avroParser = new Parser();
			if (schemaReferences != null) {
				avroParser = parseReferences(avroParser, schemaReferences);
			}
			avroParser.parse(definition);
		}
		catch (SchemaParseException ex) {
			throw new InvalidSchemaException((ex.getMessage()));
		}
	}

	@Override
	public Compatibility compatibilityCheck(String source, String other) {
		return null;
	}

	@Override
	public Schema match(List<Schema> schemas, String definition, List<Schema> schemaReferences) {
		Schema result = null;
		Parser avroParser = new Parser();
		if (schemaReferences != null) {
			avroParser = parseReferences(avroParser, schemaReferences);
		}
		org.apache.avro.Schema source = avroParser.parse(definition);
		for (Schema s : schemas) {
			avroParser = new Parser();
			if (!s.getReferences().isEmpty()) {
				avroParser = parseReferences(avroParser, s.getReferences());
			}
			org.apache.avro.Schema target = avroParser.parse(s.getDefinition());
			if (target.equals(source)) {
				result = s;
				break;
			}
		}
		return result;
	}

	@Override
	public String getFormat() {
		return AVRO_FORMAT;
	}

}
