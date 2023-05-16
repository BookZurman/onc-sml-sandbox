package ca.uhn.fhir.jpa.starter.common;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.atlas.json.JsonObject;
import org.apache.jena.atlas.json.JsonValue;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StringType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDaoPatient;
import ca.uhn.fhir.jpa.api.dao.PatientEverythingParameters;
import ca.uhn.fhir.jpa.api.model.DaoMethodOutcome;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.jpa.starter.util.ExecuteCQLUtil;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.TokenOrListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import ca.uhn.fhir.util.BundleBuilder;

public class ApplyCQL {

	RestfulServer fhirServer;
	
	


	public ApplyCQL(RestfulServer fhirServer) {
		this.fhirServer = fhirServer;

	}
	
	private TokenOrListParam toFlattenedPatientIdTokenParamList(List<IIdType> theId) {
		TokenOrListParam retVal = new TokenOrListParam();
		if (theId != null) {
			for (IIdType next : theId) {
				if (StringUtils.isNotBlank(next.getValue())) {
					String[] split = next.getValueAsString().split(",");
					Arrays.stream(split).map(IdDt::new).forEach(id -> {
						retVal.addOr(new TokenParam(id.getIdPart()));
					});
				}
			}
		}

		return retVal.getValuesAsQueryTokens().isEmpty() ? null: retVal;
	}

	@Operation(name = "$applycql", idempotent = true)
	public IBaseBundle applyCQL(javax.servlet.http.HttpServletRequest theServletRequest,
			javax.servlet.http.HttpServletResponse theServletResponse,
			ca.uhn.fhir.rest.api.server.RequestDetails theRequestDetails) {
		BundleBuilder root = new BundleBuilder(this.fhirServer.getFhirContext());

		ca.uhn.fhir.jpa.rp.r4.PatientResourceProvider prp = null;
		ca.uhn.fhir.jpa.rp.r4.ObservationResourceProvider orp = null;

		for (IResourceProvider resourceProvider : fhirServer.getResourceProviders()) {
			if (resourceProvider instanceof ca.uhn.fhir.jpa.rp.r4.PatientResourceProvider) {
				prp = (ca.uhn.fhir.jpa.rp.r4.PatientResourceProvider) resourceProvider;
			}
			
			if (resourceProvider instanceof ca.uhn.fhir.jpa.rp.r4.ObservationResourceProvider) {
				orp = (ca.uhn.fhir.jpa.rp.r4.ObservationResourceProvider) resourceProvider;
			}

		}

		
		SearchParameterMap searchMap = new SearchParameterMap();;
		searchMap.setCount(10000);
		IBundleProvider pb = prp.getDao().search(searchMap,theRequestDetails);
		
		BundleBuilder observationsBundle = new BundleBuilder(this.fhirServer.getFhirContext());
		
		for (IBaseResource p : pb.getAllResources()) {
			Patient theTargetPatient = (Patient) p;
			BundleBuilder patientBundle = new BundleBuilder(this.fhirServer.getFhirContext());

			patientBundle.addCollectionEntry(p);
			IntegerType theCount = new IntegerType();
			theCount.setValue(1000);
			
			PatientEverythingParameters everythingParams = new PatientEverythingParameters();
			everythingParams.setCount(theCount);
//			everythingParams.setOffset(theOffset);
//			everythingParams.setLastUpdated(theLastUpdated);
//			everythingParams.setSort(theSortSpec);
//			everythingParams.setContent(toStringAndList(theContent));
//			everythingParams.setNarrative(toStringAndList(theNarrative));
//			everythingParams.setFilter(toStringAndList(theFilter));
//			everythingParams.setTypes(toStringAndList(theTypes));

			TokenOrListParam retVal = new TokenOrListParam();
			retVal.addOr(new TokenParam(theTargetPatient.getIdElement().getIdPart()));
			IBundleProvider everythings2 =	((IFhirResourceDaoPatient<?>) prp.getDao()).patientTypeEverything(theServletRequest, theRequestDetails, everythingParams,retVal);

			
			
			for (IBaseResource resource : everythings2.getResources(0, 1000)) {
				root.addCollectionEntry(resource);
			}

			int ctr = 1;
			LocalRetrieveProvider lpr = new LocalRetrieveProvider(FhirContext.forR4(), root.getBundle());

			for (JsonObject plans : ExecuteCQLUtil.getPlans()) {
//				System.err.println("JsonObject plans : ExecuteCQLUtil.getPlans() loop");
				
				List<JsonValue> foo = plans.getArray("plan").collect(Collectors.toList());
				
//				System.out.println(foo.size());
				for (JsonValue plan : plans.getArray("plan").collect(Collectors.toList())) {
					
//					System.err.println("plans.getArray(\"plan\").collect(Collectors.toList()loop");
					
					Function<JsonValue, DataPoint> mymapper = new Function<JsonValue, DataPoint>() {
						@Override
						public DataPoint apply(JsonValue t) {
							DataPoint dp = new DataPoint();
							dp.key =t.getAsObject().getString("key");
							dp.code =t.getAsObject().getString("code");
							dp.system =t.getAsObject().getString("system");
							return dp;
						}
					};
					
					HashSet<DataPoint> datapoints = new HashSet<>();

					datapoints.addAll(
							plan.getAsObject().getArray("datapoints").map(mymapper).collect(Collectors.toSet()));

					Set<String> datapoints2 = new HashSet<>();
					datapoints2.addAll(
							datapoints.stream().map(DataPoint::getKey).collect(Collectors.toSet()));
					
					HashMap<String, String> results = ExecuteCQLUtil.executeCQL(plan.getAsObject().getString("library"),
							"R4", lpr, datapoints2);

					for (DataPoint dp : datapoints) {
						
//						System.err.println("for (DataPoint dp : datapoints) loop");
						
						IFhirResourceDao<Observation> odao = orp.getDao();
						Observation observation = new Observation();
						RequestDetails request = new ServletRequestDetails();;
						
						observation.setSubject(new Reference(theTargetPatient.getId()));
						
						CodeableConcept cc = new 	CodeableConcept();
						
						cc.setText(dp.key);
						
						cc.addCoding().setCode(dp.code).setDisplay(dp.key).setSystem(dp.system);
						
						observation.setCode(cc);
						
						StringType value = new StringType();
						
						value.setValue(results.get(dp.key));
						
						observation.setValue(value);
						
						 
						 
						DaoMethodOutcome result = odao.create(observation,request);
						
						IParser jp = this.fhirServer.getFhirContext().newJsonParser();
						jp.setPrettyPrint(true);
						
						System.err.println("datapoints.size() " + datapoints.size());
						System.err.println("Adding Observation " + ctr++);
						
						observationsBundle.addCollectionEntry(result.getResource());
						
						
//						System.err.println(jp.encodeResourceToString(result.getResource()));
						
//						System.out.println(dp.key + " : " + results.get(dp.key));
					}
				}
			}
		}

		return observationsBundle.getBundle();
	}
	
	static class DataPoint {
		public String key;
		public String getKey() {
			return key;
		}
		public String getCode() {
			return code;
		}
		public String getSystem() {
			return system;
		}
		String code;
		String system;
	}

}
