package ca.uhn.fhir.jpa.starter.common;

import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ca.uhn.fhir.jpa.config.dstu3.JpaDstu3Config;
import ca.uhn.fhir.jpa.starter.annotations.OnDSTU3Condition;

@Configuration
@Conditional(OnDSTU3Condition.class)
@Import({
	JpaDstu3Config.class,
	StarterJpaConfig.class,
	 
	ElasticsearchConfig.class})
public class FhirServerConfigDstu3 {
}
