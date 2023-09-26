package ca.uhn.fhir.jpa.starter.common;

import java.util.List;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.StringType;

import ca.uhn.fhir.jpa.rp.r4.EncounterResourceProvider;
import ca.uhn.fhir.jpa.rp.r4.ObservationResourceProvider;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.util.BundleBuilder;

public class PatientEncounterBundle {

	RestfulServer fhirServer;

	public PatientEncounterBundle(RestfulServer fhirServer) {
		this.fhirServer = fhirServer;
	}

	@Operation(name = "$bundleofbundles", idempotent = true)
	public IBaseBundle bundleofbundles(javax.servlet.http.HttpServletRequest theServletRequest,
			javax.servlet.http.HttpServletResponse theServletResponse,
			ca.uhn.fhir.rest.api.server.RequestDetails theRequestDetails,
			@OperationParam(name = "patient") StringType patientCtr) {

		BundleBuilder root = new BundleBuilder(this.fhirServer.getFhirContext());

		ca.uhn.fhir.jpa.rp.r4.PatientResourceProvider prp = null;
		EncounterResourceProvider erp = null;
		ObservationResourceProvider orp = null;

		for (IResourceProvider resourceProvider : fhirServer.getResourceProviders()) {
			if (resourceProvider instanceof ca.uhn.fhir.jpa.rp.r4.PatientResourceProvider) {
				prp = (ca.uhn.fhir.jpa.rp.r4.PatientResourceProvider) resourceProvider;
			}

			if (resourceProvider instanceof ca.uhn.fhir.jpa.rp.r4.EncounterResourceProvider) {
				erp = (ca.uhn.fhir.jpa.rp.r4.EncounterResourceProvider) resourceProvider;
			}

			if (resourceProvider instanceof ca.uhn.fhir.jpa.rp.r4.ObservationResourceProvider) {
				orp = (ca.uhn.fhir.jpa.rp.r4.ObservationResourceProvider) resourceProvider;
			}

		}

		IBundleProvider patients = prp.search(theServletRequest, theServletResponse, theRequestDetails, null, null,
				null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
				null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
				null, null, null, null, null, null);
		
//		System.err.println(patients.size());
//		System.err.println(patients.size());
//		System.err.println(patients.size());
//		System.err.println(patients.getCurrentPageSize());
//		System.err.println(patients.getCurrentPageSize());
//		System.err.println(patients.getCurrentPageSize());
//		System.err.println(patients.getCurrentPageOffset());
//		System.err.println(patients.getCurrentPageOffset());
//		System.err.println(patients.getCurrentPageOffset());
//		System.err.println(patients.getAllResourceIds());
//		System.err.println(patients.getAllResourceIds());
//		System.err.println(patients.getAllResourceIds());
//		System.err.println(patients.getAllResourceIds());
		
//	 Bundle boo =	 (Bundle) root.getBundle();
	 
//	 boo.setTotal(patients.size());
	 
//	 for (int ctr = 0; ctr < patients.size(); ctr++) {
		
		
		/*
		 * 
		 * 	SearchParameterMap paramMap = new SearchParameterMap();
			paramMap.add(ca.uhn.fhir.rest.api.Constants.PARAM_FILTER, theFtFilter);
			paramMap.add(ca.uhn.fhir.rest.api.Constants.PARAM_CONTENT, theFtContent);
			paramMap.add(ca.uhn.fhir.rest.api.Constants.PARAM_TEXT, theFtText);
			paramMap.add(ca.uhn.fhir.rest.api.Constants.PARAM_TAG, theSearchForTag);
			paramMap.add(ca.uhn.fhir.rest.api.Constants.PARAM_SECURITY, theSearchForSecurity);
			paramMap.add(ca.uhn.fhir.rest.api.Constants.PARAM_PROFILE, theSearchForProfile);
			paramMap.add(ca.uhn.fhir.rest.api.Constants.PARAM_SOURCE, theSearchForSource);
			paramMap.add("_has", theHas);
			paramMap.add("_id", the_id);
			paramMap.add("account", theAccount);
			paramMap.add("appointment", theAppointment);
			paramMap.add("based-on", theBased_on);
			paramMap.add("class", theClass);
			paramMap.add("date", theDate);
			paramMap.add("diagnosis", theDiagnosis);
			paramMap.add("episode-of-care", theEpisode_of_care);
			paramMap.add("identifier", theIdentifier);
			paramMap.add("length", theLength);
			paramMap.add("location", theLocation);
			paramMap.add("location-period", theLocation_period);
			paramMap.add("part-of", thePart_of);
			paramMap.add("participant", theParticipant);
			paramMap.add("participant-type", theParticipant_type);
			paramMap.add("patient", thePatient);
			paramMap.add("practitioner", thePractitioner);
			paramMap.add("reason-code", theReason_code);
			paramMap.add("reason-reference", theReason_reference);
			paramMap.add("service-provider", theService_provider);
			paramMap.add("special-arrangement", theSpecial_arrangement);
			paramMap.add("status", theStatus);
			paramMap.add("subject", theSubject);
			paramMap.add("type", theType);
			paramMap.setRevIncludes(theRevIncludes);
			paramMap.setLastUpdated(theLastUpdated);
			paramMap.setIncludes(theIncludes);
			paramMap.setSort(theSort);
			paramMap.setCount(theCount);
			paramMap.setOffset(theOffset);
			paramMap.setSummaryMode(theSummaryMode);
			paramMap.setSearchTotalMode(theSearchTotalMode);
			paramMap.setSearchContainedMode(theSearchContainedMode);

			getDao().translateRawParameters(theAdditionalRawParams, paramMap);

			ca.uhn.fhir.rest.api.server.IBundleProvider retVal = getDao().search(paramMap, theRequestDetails, theServletResponse);
		 */
		 
		  
		int which = Integer.valueOf(patientCtr.getValue()).intValue();

		for (IBaseResource thePatient : patients.getResources(which, which+1)) {
			org.hl7.fhir.r4.model.Patient x = (Patient) thePatient;
//			BundleBuilder patientBundle = new BundleBuilder(this.fhirServer.getFhirContext());
//			patientBundle.addCollectionEntry(thePatient);
			
			SearchParameterMap paramMap = new SearchParameterMap();
			ReferenceAndListParam xxx = new ReferenceAndListParam();
			
			
			ReferenceOrListParam foo = new ReferenceOrListParam();
			ReferenceParam rparm = new ReferenceParam();;
			rparm.setValue("Patient/"+thePatient.getIdElement().getValue());
			foo.add(rparm );
			xxx.addValue(foo );
			//				getDao().translateRawParameters(theAdditionalRawParams, paramMap);
			paramMap.add("patient", xxx);
			IBundleProvider boo = erp.getDao().search(paramMap, theRequestDetails, theServletResponse);
			
			System.err.println(  boo.size());
			System.err.println(  boo.size());
			System.err.println(  boo.size());
			System.err.println(  boo.size());
			System.err.println(  boo.size());
			System.err.println(  boo.size());
			
			System.err.println( boo.isEmpty());
			System.err.println( boo.isEmpty());
			System.err.println( boo.isEmpty());
			System.err.println( boo.isEmpty());
			
			System.err.println(boo.getCurrentPageSize());
			System.err.println(boo.getCurrentPageSize());
			System.err.println(boo.getCurrentPageSize());
			System.err.println(boo.getCurrentPageSize());
			System.err.println(boo.getCurrentPageSize());
			
			
			
			
			
			
//			List<String> bar = boo.getAllResourceIds();
//			
//			
//			IBundleProvider encounters = erp.search(theServletRequest, theServletResponse, theRequestDetails, null,
//					null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
//					null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
//					null, null, null, null, null, null, null, null, null);

			for (IBaseResource e : boo.getResources(0, 100)) {
				Encounter encounter = (Encounter) e;
				BundleBuilder encounterBundle = new BundleBuilder(this.fhirServer.getFhirContext());
				encounterBundle.addCollectionEntry(thePatient);
				encounterBundle.addCollectionEntry(e);
//				IBundleProvider observations = orp.search(theServletRequest, theServletResponse, theRequestDetails,
//						null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
//						null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
//						null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
//						null, null, null, null, null, null, null, null, null);
//				
//			
//		
//				
////				boo.
				
				
//				SearchParameterMap paramMape = new SearchParameterMap();
//				ReferenceAndListParam xxxe = new ReferenceAndListParam();
//				ReferenceOrListParam fooe = new ReferenceOrListParam();
//				ReferenceParam rparme = new ReferenceParam();;
//				rparme.setValue("Encounter/"+encounter.getIdElement().getValue());
//				fooe.add(rparme );
//				xxxe.addValue(fooe );
//				//				getDao().translateRawParameters(theAdditionalRawParams, paramMap);
//				paramMape.add("encounter", xxx);
				IBundleProvider booe = orp.getDao().search(paramMap, theRequestDetails, theServletResponse);

				
				
				boolean hasObservations = false;
				for (IBaseResource o : booe.getResources(0, 100)) {
					Observation observation = (Observation) o;
					
					
					if (observation.getEncounter() != null) {
						
					
						
						if (encounter.getId().startsWith(observation.getEncounter().getReference() + "/")) {
							
							System.err.println("aaaa");
							System.err.println(observation.getSubject().getReference());
							System.err.println(observation.getEncounter().getReference());
							
							System.err.println(observation.getEncounter().getReference() + "/");
							System.err.println(encounter.getId());
							System.err.println("bbb");
 
							encounterBundle.addCollectionEntry(o);
							 hasObservations = true;
						}
					}
				}

				if (hasObservations) {
					root.addCollectionEntry(encounterBundle.getBundle());
				}
				
			}
			
		}
		
//	 }

		return root.getBundle();

	}

}
