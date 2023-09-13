package ca.uhn.fhir.jpa.starter.common;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.StringType;

import ca.uhn.fhir.jpa.rp.r4.EncounterResourceProvider;
import ca.uhn.fhir.jpa.rp.r4.ObservationResourceProvider;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.util.BundleBuilder;

public class PatientEncounterBundle {
	
 
	RestfulServer fhirServer;
	
	public PatientEncounterBundle(RestfulServer fhirServer) {
		this.fhirServer = fhirServer;
	}
	
 

	@Operation(name="$bundleofbundles", idempotent=true)
	public IBaseBundle bundleofbundles( javax.servlet.http.HttpServletRequest theServletRequest,
			javax.servlet.http.HttpServletResponse theServletResponse,

			ca.uhn.fhir.rest.api.server.RequestDetails theRequestDetails,@OperationParam(name="patient") StringType patient) {
		
		 
		
		ca.uhn.fhir.jpa.rp.r4.PatientResourceProvider prp =null;
		EncounterResourceProvider erp = null;
		ObservationResourceProvider orp = null;
		
		for (IResourceProvider resourceProvider : fhirServer.getResourceProviders()) {

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
			
 
			BundleBuilder root = new BundleBuilder(this.fhirServer.getFhirContext());
 			
			for (IBaseResource p : patients.getAllResources()) {
				
				BundleBuilder patientBundle = new BundleBuilder(this.fhirServer.getFhirContext());
			
				patientBundle.addCollectionEntry(p);
				
				
 
				IBundleProvider encounters = erp.search(theServletRequest, theServletResponse, theRequestDetails, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
				
				
				 
			
 
				for (IBaseResource e : encounters.getResources(0, 1)) {
					
					
					
					
					IBundleProvider fff = erp.EncounterInstanceEverything(theServletRequest, e.getIdElement(), null, null, null, null);
					
					
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
				
 
				root.addCollectionEntry(patientBundle.getBundle());
				
 
			}
			
			
			return root.getBundle();
 
	}

}
