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
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r5.context.IWorkerContext;
import org.opencds.cqf.cql.engine.execution.EvaluationResult;
import org.opencds.cqf.cql.engine.execution.ExpressionResult;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.evaluator.CqlEvaluator;
import org.opencds.cqf.cql.evaluator.CqlOptions;
import org.opencds.cqf.cql.evaluator.builder.Constants;
import org.opencds.cqf.cql.evaluator.builder.CqlEvaluatorBuilder;
import org.opencds.cqf.cql.evaluator.builder.DataProviderComponents;
import org.opencds.cqf.cql.evaluator.builder.DataProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.EndpointInfo;
import org.opencds.cqf.cql.evaluator.dagger.CqlEvaluatorComponent;
import org.opencds.cqf.cql.evaluator.dagger.DaggerCqlEvaluatorComponent;
import org.opencds.cqf.cql.evaluator.fhir.npm.NpmProcessor;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.jpa.starter.common.TestEvaluate.LibraryParameter.ContextParameter;
import ca.uhn.fhir.jpa.starter.common.TestEvaluate.LibraryParameter.ModelParameter;
 

public class TestEvaluate {
	
	 
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

	
	 private static class Logger implements IWorkerContext.ILoggingService {

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
	  public static List<LibraryParameter> libraries= new ArrayList<>();
	  public static NamespaceParameter namespace;
	  private static String testResourcePath =  "src/test/resources";

	  
	  private static  Map<String, LibrarySourceProvider> librarySourceProviderIndex = new HashMap<>();
	    private  static Map<String, TerminologyProvider> terminologyProviderIndex = new HashMap<>();

	    private static String toVersionNumber(FhirVersionEnum fhirVersion) {
	        switch (fhirVersion) {
	            case R4: return "4.0.1";
	            case R5: return "5.0.0-ballot";
	            case DSTU3: return "3.0.2";
	            default: throw new IllegalArgumentException(String.format("Unsupported FHIR version %s", fhirVersion));
	        }
	    }
	  
	public static void main(String[] args) {
		/*
		 *    "cql",
                "-fv=R4",
                "-lu="+ testResourcePath + "/r4a",
                "-ln=TestFHIR",
                "-m=FHIR",
                "-mu=" + testResourcePath + "/r4",
                "-t=" + testResourcePath + "/r4/vocabulary/ValueSet",
                "-c=Patient",
                "-cv=example"
		 */

		fhirVersion = "R4";
		FhirVersionEnum fhirVersionEnum = FhirVersionEnum.valueOf(fhirVersion);

	        CqlEvaluatorComponent.Builder builder = DaggerCqlEvaluatorComponent.builder().fhirContext(fhirVersionEnum.newContext());

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
	        lp.libraryUrl =testResourcePath + "/r4";
	        lp.libraryName = "TestFHIR";
	        ContextParameter context = new ContextParameter();;
	        context.contextName = "Patient";
	        context.contextValue="example";		
			lp.context = context;
			ModelParameter model = new ModelParameter();
			model.modelName = "FHIR";
			model.modelUrl = testResourcePath + "/r4";
			lp.model = model ;
			lp.terminologyUrl = testResourcePath + "/r4/vocabulary/ValueSet";
			
	        
	        libraries.add(lp);
	        
	        for (LibraryParameter library : libraries) {
	            CqlEvaluatorBuilder cqlEvaluatorBuilder = cqlEvaluatorComponent.createBuilder().withCqlOptions(cqlOptions);

	            if (namespace != null) {
	                cqlEvaluatorBuilder.withNamespaceInfo(new NamespaceInfo(namespace.namespaceName, namespace.namespaceUri));
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
	            // default to FHIR
	            else {
	                dataProvider = dataProviderFactory.create(new EndpointInfo().setType(Constants.HL7_FHIR_FILES_CODE));
	            }
	            
//	            cqlEvaluatorBuilder.with

	            cqlEvaluatorBuilder.withModelResolverAndRetrieveProvider(dataProvider.getModelUri(), dataProvider.getModelResolver(),
	                    dataProvider.getRetrieveProvider());

	            CqlEvaluator evaluator = cqlEvaluatorBuilder.build();

	            VersionedIdentifier identifier = new VersionedIdentifier().withId(library.libraryName);

	            Pair<String, Object> contextParameter = null;

	            if (library.context != null) {
	                contextParameter = Pair.of(library.context.contextName, library.context.contextValue);
	            }

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
