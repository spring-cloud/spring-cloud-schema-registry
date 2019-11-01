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

package org.springframework.cloud.schema.registry.config;

import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.util.CollectionUtils;

/**
 * Override deprecated properties for backward compatibility.
 * @author Christian Tzolov
 */
public abstract class AbstractBackwardCompatibilityEnvironmentPostProcessor implements EnvironmentPostProcessor {

	private final Log logger = LogFactory.getLog(AbstractBackwardCompatibilityEnvironmentPostProcessor.class);

	private final String propertyKeyName;


	public AbstractBackwardCompatibilityEnvironmentPostProcessor(String propertyKeyName) {
		this.propertyKeyName = propertyKeyName;
	}

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {

		Properties properties = new Properties();

		for (Map.Entry<String, String> e : doGetPropertyMappings().entrySet()) {
			if (environment.containsProperty(e.getKey())) {
				properties.setProperty(e.getValue(), environment.getProperty(e.getKey()));
			}
		}

		// This post-processor is called multiple times but sets the properties only once.
		if (!CollectionUtils.isEmpty(properties)) {
			logger.info(" 'spring.cloud.stream.schemaXXX' property prefix detected! " +
					"Use the 'spring.schemaXXX' prefix instead!");
			PropertiesPropertySource propertiesPropertySource =
					new PropertiesPropertySource(propertyKeyName, properties);
			environment.getPropertySources().addLast(propertiesPropertySource);
		}
	}

	protected abstract Map<String, String> doGetPropertyMappings();
}
