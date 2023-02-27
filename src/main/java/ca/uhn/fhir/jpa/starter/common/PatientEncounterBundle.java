package ca.uhn.fhir.jpa.starter.common;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.StringType;

import ca.uhn.fhir.jpa.rp.r4.EncounterResourceProvider;
import ca.uhn.fhir.jpa.rp.r4.ObservationResourceProvider;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.util.BundleBuilder;

public class PatientEncounterBundle {
	
//	@Autowired
//	protected IDataProviderFactory myCqlDataProviderFactory;
	
	RestfulServer fhirServer;
	
	public PatientEncounterBundle(RestfulServer fhirServer) {
		this.fhirServer = fhirServer;
	}
	
	/*
	 * 		try {
			SearchParameterMap paramMap = new SearchParameterMap();
			paramMap.add(ca.uhn.fhir.rest.api.Constants.PARAM_FILTER, theFtFilter);
			paramMap.add(ca.uhn.fhir.rest.api.Constants.PARAM_CONTENT, theFtContent);
			paramMap.add(ca.uhn.fhir.rest.api.Constants.PARAM_TEXT, theFtText);
			paramMap.add(ca.uhn.fhir.rest.api.Constants.PARAM_TAG, theSearchForTag);
			paramMap.add(ca.uhn.fhir.rest.api.Constants.PARAM_SECURITY, theSearchForSecurity);
			paramMap.add(ca.uhn.fhir.rest.api.Constants.PARAM_PROFILE, theSearchForProfile);
			paramMap.add(ca.uhn.fhir.rest.api.Constants.PARAM_SOURCE, theSearchForSource);
			paramMap.add("_has", theHas);
			paramMap.add("_id", the_id);
			paramMap.add("active", theActive);
			paramMap.add("address", theAddress);
			paramMap.add("address-city", theAddress_city);
			paramMap.add("address-country", theAddress_country);
			paramMap.add("address-postalcode", theAddress_postalcode);
			paramMap.add("address-state", theAddress_state);
			paramMap.add("address-use", theAddress_use);
			paramMap.add("birthdate", theBirthdate);
			paramMap.add("death-date", theDeath_date);
			paramMap.add("deceased", theDeceased);
			paramMap.add("email", theEmail);
			paramMap.add("family", theFamily);
			paramMap.add("gender", theGender);
			paramMap.add("general-practitioner", theGeneral_practitioner);
			paramMap.add("given", theGiven);
			paramMap.add("identifier", theIdentifier);
			paramMap.add("language", theLanguage);
			paramMap.add("link", theLink);
			paramMap.add("name", theName);
			paramMap.add("organization", theOrganization);
			paramMap.add("phone", thePhone);
			paramMap.add("phonetic", thePhonetic);
			paramMap.add("telecom", theTelecom);
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
			return retVal;
		} finally {
			endRequest(theServletRequest);
		}
	 */

	@Operation(name="$bundleofbundles", idempotent=true)
	public IBaseBundle bundleofbundles( javax.servlet.http.HttpServletRequest theServletRequest,
			javax.servlet.http.HttpServletResponse theServletResponse,

			ca.uhn.fhir.rest.api.server.RequestDetails theRequestDetails,@OperationParam(name="patient") StringType patient) {
		
		 
		
		ca.uhn.fhir.jpa.rp.r4.PatientResourceProvider prp =null;
		EncounterResourceProvider erp = null;
		ObservationResourceProvider orp = null;
		
		for (IResourceProvider resourceProvider : fhirServer.getResourceProviders()) {
//			resourceProvider.get
			System.err.println(resourceProvider.getResourceType().getName());
			System.err.println(resourceProvider.getClass().getCanonicalName());
			if (resourceProvider instanceof ca.uhn.fhir.jpa.rp.r4.PatientResourceProvider) {
			prp = (ca.uhn.fhir.jpa.rp.r4.PatientResourceProvider)resourceProvider;
			}
			
			if (resourceProvider instanceof ca.uhn.fhir.jpa.rp.r4.EncounterResourceProvider) {
			erp = (ca.uhn.fhir.jpa.rp.r4.EncounterResourceProvider)resourceProvider;
			}
			
			if (resourceProvider instanceof ca.uhn.fhir.jpa.rp.r4.ObservationResourceProvider) {
			orp = (ca.uhn.fhir.jpa.rp.r4.ObservationResourceProvider)resourceProvider;
			}

		}
				 
			IBundleProvider patients =	 prp.search(theServletRequest, theServletResponse, theRequestDetails, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
//			Bundle b2 = new Bundle();
		
			BundleBuilder root = new BundleBuilder(this.fhirServer.getFhirContext());
//			BundleBuilder patients = new BundleBuilder(this.fhirServer.getFhirContext());
//			BundleBuilder encount = new BundleBuilder(this.fhirServer.getFhirContext());
			
//			builder.addCollectionEntry(b2);
			
			
			for (IBaseResource p : patients.getAllResources()) {
				
				BundleBuilder patientBundle = new BundleBuilder(this.fhirServer.getFhirContext());
			
				patientBundle.addCollectionEntry(p);
				
				
//ReferenceAndListParam xxxx = new ReferenceAndListParam();
//ReferenceParam referenceParam = new ReferenceParam();
//referenceParam.setValue("asdf");
//ArrayList<ReferenceAndListParam> list = new ArrayList<ReferenceAndListParam>(); 
//xxxx.addValue(list);

//ReferenceParam[value=asdf]]

//xxxx.a
//xxxx.addValue(asdf);

				IBundleProvider encounters = erp.search(theServletRequest, theServletResponse, theRequestDetails, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
//				encounters.getResources(0, 0)
//				encounters.
				for (IBaseResource e : encounters.getResources(0, 1)) {
					
					BundleBuilder encounterBundle = new BundleBuilder(this.fhirServer.getFhirContext());
				
					encounterBundle.addCollectionEntry(e);
					
					
					IBundleProvider observations = orp.search(theServletRequest, theServletResponse, theRequestDetails, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

					BundleBuilder observatrionsBundle = new BundleBuilder(this.fhirServer.getFhirContext());
					
					for (IBaseResource o : observations.getResources(0, 1)) {
						observatrionsBundle.addCollectionEntry(o);	
					}
					
					encounterBundle.addCollectionEntry(observatrionsBundle.getBundle());
					
					
					patientBundle.addCollectionEntry(encounterBundle.getBundle());
				}
				
//				for (int ii =0; ii < 2; ii++) {
//					BundleBuilder encounterBundle = new BundleBuilder(this.fhirServer.getFhirContext());
//					
//					Encounter encounter = new Encounter();
//					
//					EncounterParticipantComponent participant = encounter.addParticipant();
//					
//					Date date = new Date();  
//					participant.getPeriod().setStart(date);
//					encounter.addReasonCode().setText("Reason code " + ii);
//					
//					encounterBundle.addCollectionEntry(encounter);
//					
//					Observation observation = new Observation();
//					
//					observation.addCategory().setText("category");
//					
//					encounterBundle.addCollectionEntry(observation);
//					
//					patientBundle.addCollectionEntry(encounterBundle.getBundle());
//				}
				
				root.addCollectionEntry(patientBundle.getBundle());
				
				
//				Bundle b3 = new Bundle();
//				b2.addEntry().setResource(b3);
//				Base v =b3.addChild("asdf");
//				
//				System.out.println(p);
//				b2.addEntry().
			}
			
			
			return root.getBundle();
//				 prp.
				 
//				 SearchParameterMap paramMap = new SearchParameterMap();
//				ca.uhn.fhir.rest.api.server.IBundleProvider retVal = prp.getDao().search(paramMap, theRequestDetails, theServletResponse);
//				System.err.println(retVal);

//				 paramMap.add(ca.uhn.fhir.rest.api.Constants.PARAM_FILTER, "theFtFilter");
				 
//				 prp.get
				 
//			}
//		}
		
	   
//		ca.uhn.fhir.rest.api.server.IBundleProvider  retVal = null;
	   // Populate bundle with matching resources
//	   return null;
	}

}
