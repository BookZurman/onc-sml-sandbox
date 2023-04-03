package ca.uhn.fhir.jpa.starter.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.cqframework.cql.cql2elm.CqlTranslatorOptions;
import org.cqframework.cql.cql2elm.CqlTranslatorOptionsMapper;
import org.cqframework.cql.cql2elm.LibrarySourceProvider;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.cqframework.fhir.utilities.IGContext;
import org.hl7.cql.model.NamespaceInfo;
import org.hl7.fhir.convertors.advisors.interfaces.BaseAdvisor;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4b.model.IntegerType;
import org.hl7.fhir.r5.context.IWorkerContext.ILoggingService;
import org.opencds.cqf.cql.engine.execution.EvaluationResult;
import org.opencds.cqf.cql.engine.execution.ExpressionResult;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.evaluator.CqlEvaluator;
import org.opencds.cqf.cql.evaluator.CqlOptions;
import org.opencds.cqf.cql.evaluator.builder.CqlEvaluatorBuilder;
import org.opencds.cqf.cql.evaluator.builder.DataProviderComponents;
import org.opencds.cqf.cql.evaluator.builder.DataProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.EndpointInfo;
import org.opencds.cqf.cql.evaluator.dagger.CqlEvaluatorComponent;
import org.opencds.cqf.cql.evaluator.dagger.DaggerCqlEvaluatorComponent;
import org.opencds.cqf.cql.evaluator.fhir.npm.NpmProcessor;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.jpa.starter.common.ApplyCQL.LibraryParameter.ContextParameter;
import ca.uhn.fhir.jpa.starter.common.ApplyCQL.LibraryParameter.ModelParameter;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.util.BundleBuilder;

public class ApplyCQL {

	static class NamespaceParameter {

		public String namespaceName;

		public String namespaceUri;
	}

	static class LibraryParameter {

		public String libraryUrl;

		public String libraryName;

		public String libraryVersion;

		public String terminologyUrl;

		public ModelParameter model;

		public List<ParameterParameter> parameters;

		public String[] expression;

		public ContextParameter context;

		static class ContextParameter {

			public String contextName;

			public String contextValue;
		}

		static class ModelParameter {

			public String modelName;

			public String modelUrl;
		}

		static class ParameterParameter {

			public String parameterName;

			public String parameterValue;
		}
	}

	public static class Logger implements ILoggingService {

		@Override
		public void logMessage(String s) {
			System.out.println(s);
		}

		@Override
		public void logDebugMessage(LogCategory logCategory, String s) {
			System.out.println(String.format("%s: %s", logCategory.toString(), s));
		}
	}

	public static String fhirVersion;
	public static String rootDir;
	public static String igPath;
	public static String optionsPath;
	public static List<LibraryParameter> libraries = new ArrayList<>();
	public static NamespaceParameter namespace;
	private static String testResourcePath = "src/test/resources";

	private static Map<String, LibrarySourceProvider> librarySourceProviderIndex = new HashMap<>();
	private static Map<String, TerminologyProvider> terminologyProviderIndex = new HashMap<>();

	private static String toVersionNumber(FhirVersionEnum fhirVersion) {
		switch (fhirVersion) {
		case R4:
			return "4.0.1";
		case R5:
			return "5.0.0-ballot";
		case DSTU3:
			return "3.0.2";
		default:
			throw new IllegalArgumentException(String.format("Unsupported FHIR version %s", fhirVersion));
		}
	}

	public static boolean isUri(String uri) {
		if (uri == null) {
			return false;
		}

		System.out.println(uri.startsWith("file"));
		System.out.println(uri.startsWith("\\w+?"));
		System.out.println(uri.startsWith("\\w+?://.*"));

		return uri.startsWith("file:/") || uri.matches("\\w+?://.*");
	}

	public static boolean isFileUri(String uri) {
		if (uri == null) {
			return false;
		}

		System.out.println(uri.startsWith("file"));
		System.out.println(uri.startsWith("\\w+?"));
		System.out.println(uri.startsWith("\\w+?://.*"));

		return uri.startsWith("file") || !uri.matches("\\w+?://.*");
	}

	RestfulServer fhirServer;

	public ApplyCQL(RestfulServer fhirServer) {
		this.fhirServer = fhirServer;
	}

	@Operation(name = "$applycql", idempotent = true)
	public IBaseBundle applyCQL(javax.servlet.http.HttpServletRequest theServletRequest,
			javax.servlet.http.HttpServletResponse theServletResponse,
			ca.uhn.fhir.rest.api.server.RequestDetails theRequestDetails) {
		BundleBuilder root = new BundleBuilder(this.fhirServer.getFhirContext());

		ca.uhn.fhir.jpa.rp.r4.PatientResourceProvider prp = null;

		for (IResourceProvider resourceProvider : fhirServer.getResourceProviders()) {
 			if (resourceProvider instanceof ca.uhn.fhir.jpa.rp.r4.PatientResourceProvider) {
				prp = (ca.uhn.fhir.jpa.rp.r4.PatientResourceProvider) resourceProvider;
			}

		}

		IBundleProvider patients = prp.search(theServletRequest, theServletResponse, theRequestDetails, null, null,
				null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
				null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
				null, null, null, null, null, null);

		for (IBaseResource p : patients.getAllResources()) {
			Patient theTargetPatient = (Patient) p;

//			root.addCollectionEntry(theTargetPatient);

			BundleBuilder patientBundle = new BundleBuilder(this.fhirServer.getFhirContext());

			patientBundle.addCollectionEntry(p);

//				PatientEverythingParameters asdfaa;
//				asdfaa.set

			IntegerType theCount = new IntegerType();
			theCount.setValue(1000);
			// IPrimitiveType<Integer> aaa;

//				prp.get
//			IBundleProvider everythings =	 prp.patientTypeEverything(theServletRequest, theCount, null, null, null, null, null, null, null, null, theRequestDetails);

			IBundleProvider everythings = prp.patientInstanceEverything(theServletRequest,
					theTargetPatient.getIdElement(), theCount, null, null, null, null, null, null, null,
					theRequestDetails);

			// theServletRequest, theTargetPatient.getIdElement(), theCount , null, null,
			// null, null, null, null, null, theRequestDetails);

//			System.err.println(everythings.getCurrentPageSize());

//everythings.get

			for (IBaseResource resource : everythings.getResources(0, 100)) {
//				IParser parser = this.fhirServer.getFhirContext().newJsonParser();
//				parser.setPrettyPrint(true);
//				String serialized = parser.encodeResourceToString(resource);
//				System.err.println(serialized);

				root.addCollectionEntry(resource);
			}

			apply(theTargetPatient,root.getBundle());
		}

		return root.getBundle();
	}

	public  void apply(Patient theTargetPatient, IBaseBundle theBundle) {

//		System.out.println("isFileUri " + isFileUri("http://localhost"));
//		System.out.println("isUri " + isUri("http://localhost"));

	 

		fhirVersion = "R4";
		FhirVersionEnum fhirVersionEnum = FhirVersionEnum.valueOf(fhirVersion);

		CqlEvaluatorComponent.Builder builder = DaggerCqlEvaluatorComponent.builder()
				.fhirContext(fhirVersionEnum.newContext());

		IGContext igContext = null;
		if (rootDir != null && igPath != null) {

			igContext = new IGContext(new Logger());
			igContext.initializeFromIg(rootDir, igPath, toVersionNumber(fhirVersionEnum));
		}

		CqlEvaluatorComponent cqlEvaluatorComponent = builder.build();

		CqlOptions cqlOptions = CqlOptions.defaultOptions();

		if (optionsPath != null) {
			CqlTranslatorOptions options = CqlTranslatorOptionsMapper.fromFile(optionsPath);
			cqlOptions.setCqlTranslatorOptions(options);
		}

		LibraryParameter lp = new LibraryParameter();
		lp.libraryUrl = testResourcePath + "/r4";
		lp.libraryName = "TestFHIR";
		ContextParameter context = new ContextParameter();
		context.contextName = "Patient";
		context.contextValue = "example";
		lp.context = context;
		ModelParameter model = new ModelParameter();
		model.modelName = "FHIR";
		model.modelUrl = testResourcePath + "/r4";
		lp.model = model;
		lp.terminologyUrl = testResourcePath + "/r4/vocabulary/ValueSet";

		libraries.add(lp);

		for (LibraryParameter library : libraries) {
			CqlEvaluatorBuilder cqlEvaluatorBuilder = cqlEvaluatorComponent.createBuilder().withCqlOptions(cqlOptions);

			if (namespace != null) {
				cqlEvaluatorBuilder
						.withNamespaceInfo(new NamespaceInfo(namespace.namespaceName, namespace.namespaceUri));
			}

			if (igContext != null) {
				cqlEvaluatorBuilder.withNpmProcessor(new NpmProcessor(igContext));
			}

			LibrarySourceProvider librarySourceProvider = librarySourceProviderIndex.get(library.libraryUrl);

			if (librarySourceProvider == null) {
				librarySourceProvider = cqlEvaluatorComponent.createLibrarySourceProviderFactory()
						.create(new EndpointInfo().setAddress(library.libraryUrl));
				librarySourceProviderIndex.put(library.libraryUrl, librarySourceProvider);
			}

			cqlEvaluatorBuilder.withLibrarySourceProvider(librarySourceProvider);

			if (library.terminologyUrl != null) {
				TerminologyProvider terminologyProvider = terminologyProviderIndex.get(library.terminologyUrl);
				if (terminologyProvider == null) {
					terminologyProvider = cqlEvaluatorComponent.createTerminologyProviderFactory()
							.create(new EndpointInfo().setAddress(library.terminologyUrl));
					terminologyProviderIndex.put(library.terminologyUrl, terminologyProvider);
				}

				cqlEvaluatorBuilder.withTerminologyProvider(terminologyProvider);
			}

			DataProviderComponents dataProvider = null;
			DataProviderFactory dataProviderFactory = cqlEvaluatorComponent.createDataProviderFactory();
			if (library.model != null) {
				dataProvider = dataProviderFactory.create(new EndpointInfo().setAddress(library.model.modelUrl));
			}
			LocalRetrieveProvider lpr = new LocalRetrieveProvider(FhirContext.forR4(), theBundle);
			cqlEvaluatorBuilder.withModelResolverAndRetrieveProvider(dataProvider.getModelUri(),
					dataProvider.getModelResolver(), lpr);

			CqlEvaluator evaluator = cqlEvaluatorBuilder.build();

			VersionedIdentifier identifier = new VersionedIdentifier().withId(library.libraryName);

			Pair<String, Object> contextParameter = null;

			contextParameter = Pair.of("Patient", theTargetPatient.getIdPart());

			EvaluationResult result = evaluator.evaluate(identifier, contextParameter);

			for (Map.Entry<String, ExpressionResult> libraryEntry : result.expressionResults.entrySet()) {
				System.out.println(libraryEntry.getKey() + "=" + tempConvert(libraryEntry.getValue().value()));
			}

			System.out.println();
		}

//	        return 0;

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
