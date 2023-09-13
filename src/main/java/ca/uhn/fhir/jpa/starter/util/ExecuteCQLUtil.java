package ca.uhn.fhir.jpa.starter.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.atlas.json.JSON;
import org.apache.jena.atlas.json.JsonObject;
import org.cqframework.cql.cql2elm.LibrarySourceProvider;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.cql.engine.execution.EvaluationResult;
import org.opencds.cqf.cql.engine.execution.ExpressionResult;
import org.opencds.cqf.cql.engine.fhir.model.R4FhirModelResolver;
import org.opencds.cqf.cql.engine.retrieve.RetrieveProvider;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.evaluator.CqlEvaluator;
import org.opencds.cqf.cql.evaluator.CqlOptions;
import org.opencds.cqf.cql.evaluator.builder.CqlEvaluatorBuilder;
import org.opencds.cqf.cql.evaluator.builder.EndpointInfo;
import org.opencds.cqf.cql.evaluator.dagger.CqlEvaluatorComponent;
import org.opencds.cqf.cql.evaluator.dagger.DaggerCqlEvaluatorComponent;
import org.opencds.cqf.cql.evaluator.engine.model.CachingModelResolverDecorator;
import org.opencds.cqf.cql.evaluator.engine.retrieve.BundleRetrieveProvider;
import org.opencds.cqf.cql.evaluator.fhir.DirectoryBundler;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;


public class ExecuteCQLUtil {

	private static String cqlLibraries;
	
	private static String cqlVocabulary;
	
	private static String cqlPlan;
	

	public static void setCqlPlan(String cqlPlan) {
		ExecuteCQLUtil.cqlPlan = cqlPlan;
	}

	public static void setCqlLibraries(String cqlLibraries) {
		ExecuteCQLUtil.cqlLibraries = cqlLibraries;
	}

	public static void setCqlVocabulary(String cqlValuesets) {
		ExecuteCQLUtil.cqlVocabulary = cqlValuesets;
	}
	
	public static List<JsonObject> getPlans() {
		
		List<JsonObject> plans = new ArrayList<JsonObject>();
        try {
        	
        	  Consumer<? super Path> loadConceptMap = new Consumer<Path>() {

				@Override
				public void accept(Path t) {
					try {
						 
						plans .add(JSON.parse(Files.readString(t)));
						
					
						
					} catch (IOException e) {
					}
					
				}
        		  
        	  };
            for (String mapsFolder : Stream.of(cqlPlan.split(",", -1)).collect(Collectors.toList())) {
                    try (Stream<Path> paths = Files.walk(Paths.get(mapsFolder))) {
                            paths.filter(Files::isRegularFile).forEach(loadConceptMap);
                    }
            }
    } catch (IOException e) {

            e.printStackTrace();
    }

        return plans;
	}


	public static HashMap<String,String> executeCQL(String library,String fhirVersion,RetrieveProvider retrieveProvider,Set<String> datapoints) {

	 
		 org.opencds.cqf.cql.evaluator.fhir.DirectoryBundler_Factory fff;
		 
		HashMap<String,String> results = new HashMap<String,String>();
		
		FhirVersionEnum fhirVersionEnum = FhirVersionEnum.valueOf(fhirVersion);

		VersionedIdentifier identifier = new VersionedIdentifier().withId(library);

		CqlEvaluatorComponent.Builder builder = DaggerCqlEvaluatorComponent.builder()
				.fhirContext(fhirVersionEnum.newContext());
		
//		DirectoryBundler_Factory foo = null;

		CqlEvaluatorComponent cqlEvaluatorComponent = builder.build();

		CqlOptions cqlOptions = CqlOptions.defaultOptions();

		CqlEvaluatorBuilder cqlEvaluatorBuilder = cqlEvaluatorComponent.createBuilder().withCqlOptions(cqlOptions);

		LibrarySourceProvider librarySourceProvider = cqlEvaluatorComponent.createLibrarySourceProviderFactory()
				.create(new EndpointInfo().setAddress(cqlLibraries ));
		
		cqlEvaluatorBuilder.withLibrarySourceProvider(librarySourceProvider);

		TerminologyProvider terminologyProvider = cqlEvaluatorComponent.createTerminologyProviderFactory()
				.create(new EndpointInfo().setAddress(cqlVocabulary));
		cqlEvaluatorBuilder.withTerminologyProvider(terminologyProvider);
		
		R4FhirModelResolver bar = new R4FhirModelResolver();
		CachingModelResolverDecorator cmrd = new CachingModelResolverDecorator(bar);
			
		cqlEvaluatorBuilder.withModelResolverAndRetrieveProvider("http://hl7.org/fhir",
				cmrd, retrieveProvider);

		CqlEvaluator evaluator = cqlEvaluatorBuilder.build();


		Pair<String, Object> contextParameter = null;
		// ???
		contextParameter = Pair.of("Patient", "example");

		EvaluationResult result = evaluator.evaluate(identifier, contextParameter);

		for (Map.Entry<String, ExpressionResult> libraryEntry : result.expressionResults.entrySet()) {
			if (datapoints.contains(libraryEntry.getKey())) {
				results.put(libraryEntry.getKey(), popStringValue(libraryEntry.getValue().value()));
			}
		}
		
		return results;

	}

	public static void main(String[] args) {
		
		DirectoryBundler directoryBundler = new DirectoryBundler(FhirContext.forR4Cached());
		
		IBaseBundle bundle = directoryBundler.bundle("src/test/resources/testCQL");
		
		BundleRetrieveProvider bundleProvider = new BundleRetrieveProvider(FhirContext.forR4Cached(), bundle);
		
		HashSet<String> datapoints = new HashSet<>();
		datapoints.add("Had mammogram in past two years");
		
		HashMap<String, String> results = ExecuteCQLUtil.executeCQL("BreastCancerScreening","R4",bundleProvider,datapoints);
		
		
		for (String key : results.keySet()) {
			System.out.println(key + " : " + results.get(key));
		}
		
	}



	private static String popStringValue(Object value) {
		if (value == null) {
			return "null";
		}

		String result = "";
		if (value instanceof Iterable) {
			result += "[";
			Iterable<?> values = (Iterable<?>) value;
			for (Object o : values) {

				result += (popStringValue(o) + ", ");
			}

			if (result.length() > 1) {
				result = result.substring(0, result.length() - 2);
			}

			result += "]";
		} else if (value instanceof IBaseResource) {
			IBaseResource resource = (IBaseResource) value;
			result = resource.fhirType() + (resource.getIdElement() != null && resource.getIdElement().hasIdPart()
					? "(id=" + resource.getIdElement().getIdPart() + ")"
					: "");
		} else if (value instanceof IBase) {
			result = ((IBase) value).fhirType();
		} else if (value instanceof IBaseDatatype) {
			result = ((IBaseDatatype) value).fhirType();
		} else {
			result = value.toString();
		}

		return result;
	}

}
