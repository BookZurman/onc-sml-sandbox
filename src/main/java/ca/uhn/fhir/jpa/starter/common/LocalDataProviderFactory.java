package ca.uhn.fhir.jpa.starter.common;

import java.util.Set;

import org.opencds.cqf.cql.evaluator.builder.ModelResolverFactory;
import org.opencds.cqf.cql.evaluator.builder.data.DataProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.data.TypedRetrieveProviderFactory;

import ca.uhn.fhir.context.FhirContext;

public class LocalDataProviderFactory extends DataProviderFactory {

	public LocalDataProviderFactory(FhirContext fhirContext, Set<ModelResolverFactory> modelResolverFactories,
			Set<TypedRetrieveProviderFactory> retrieveProviderFactories) {
		super(fhirContext, modelResolverFactories, retrieveProviderFactories);
		// TODO Auto-generated constructor stub
	}

}
