package ca.uhn.fhir.jpa.starter.common;

import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ca.uhn.fhir.jpa.config.r4b.JpaR4BConfig;
import ca.uhn.fhir.jpa.starter.annotations.OnR4BCondition;

@Configuration
@Conditional(OnR4BCondition.class)
@Import({
	JpaR4BConfig.class,
	StarterJpaConfig.class,
	ElasticsearchConfig.class
})
public class FhirServerConfigR4B {
}
