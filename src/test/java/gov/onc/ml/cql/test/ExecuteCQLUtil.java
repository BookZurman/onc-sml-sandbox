package gov.onc.ml.cql.test;

import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
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

	

	public static void executeCQL(String library,String fhirVersion,RetrieveProvider retrieveProvider) {

	 
		FhirVersionEnum fhirVersionEnum = FhirVersionEnum.valueOf(fhirVersion);

		VersionedIdentifier identifier = new VersionedIdentifier().withId(library);

		CqlEvaluatorComponent.Builder builder = DaggerCqlEvaluatorComponent.builder()
				.fhirContext(fhirVersionEnum.newContext());

		CqlEvaluatorComponent cqlEvaluatorComponent = builder.build();

		CqlOptions cqlOptions = CqlOptions.defaultOptions();

		CqlEvaluatorBuilder cqlEvaluatorBuilder = cqlEvaluatorComponent.createBuilder().withCqlOptions(cqlOptions);

		LibrarySourceProvider librarySourceProvider = cqlEvaluatorComponent.createLibrarySourceProviderFactory()
				.create(new EndpointInfo().setAddress("src/test/resources/testCQL"));

		cqlEvaluatorBuilder.withLibrarySourceProvider(librarySourceProvider);

		TerminologyProvider terminologyProvider = cqlEvaluatorComponent.createTerminologyProviderFactory()
				.create(new EndpointInfo().setAddress("src/test/resources/testCQL/vocabulary/ValueSet"));
		cqlEvaluatorBuilder.withTerminologyProvider(terminologyProvider);
		
		R4FhirModelResolver bar = new R4FhirModelResolver();
		CachingModelResolverDecorator cmrd = new CachingModelResolverDecorator(bar);
			
		cqlEvaluatorBuilder.withModelResolverAndRetrieveProvider("http://hl7.org/fhir",
				cmrd, retrieveProvider);

		CqlEvaluator evaluator = cqlEvaluatorBuilder.build();


		Pair<String, Object> contextParameter = null;
		contextParameter = Pair.of("Patient", "example");

		EvaluationResult result = evaluator.evaluate(identifier, contextParameter);

		for (Map.Entry<String, ExpressionResult> libraryEntry : result.expressionResults.entrySet()) {
			System.out.println(libraryEntry.getKey() + "=" + tempConvert(libraryEntry.getValue().value()));		 
		}

	}

	public static void main(String[] args) {
		
		DirectoryBundler directoryBundler = new DirectoryBundler(FhirContext.forR4Cached());
		
		IBaseBundle bundle = directoryBundler.bundle("src/test/resources/testCQL");
		
		BundleRetrieveProvider foobar = new BundleRetrieveProvider(FhirContext.forR4Cached(), bundle);
		
		ExecuteCQLUtil.executeCQL("BreastCancerScreening","R4",foobar);
		
		ExecuteCQLUtil.executeCQL("ColonCancerScreening","R4",foobar);
		
	}



	private static String tempConvert(Object value) {
		if (value == null) {
			return "null";
		}

		String result = "";
		if (value instanceof Iterable) {
			result += "[";
			Iterable<?> values = (Iterable<?>) value;
			for (Object o : values) {

				result += (tempConvert(o) + ", ");
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
