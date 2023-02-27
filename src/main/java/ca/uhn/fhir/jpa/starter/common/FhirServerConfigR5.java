package ca.uhn.fhir.jpa.starter.common;

import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ca.uhn.fhir.jpa.config.r5.JpaR5Config;
import ca.uhn.fhir.jpa.starter.annotations.OnR5Condition;

@Configuration
@Conditional(OnR5Condition.class)
@Import({
	StarterJpaConfig.class,
	JpaR5Config.class,
	ElasticsearchConfig.class
})
public class FhirServerConfigR5 {
}
