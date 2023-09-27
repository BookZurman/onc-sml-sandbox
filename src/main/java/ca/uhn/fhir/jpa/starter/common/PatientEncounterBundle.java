package ca.uhn.fhir.jpa.starter.common;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.IdType;
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

	private ReferenceAndListParam createParam(String paramValue) {
		ReferenceAndListParam param = new ReferenceAndListParam();
		ReferenceOrListParam param2 = new ReferenceOrListParam();
		ReferenceParam param3 = new ReferenceParam();
		;
		param3.setValue(paramValue);
		param2.add(param3);
		param.addValue(param2);
		return param;

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

		IIdType pid = new IdType();

		pid.setValue(patientCtr.getValue());

		Patient thePatienta = prp.getDao().read(pid, theRequestDetails);

		org.hl7.fhir.r4.model.Patient thePatient = (Patient) thePatienta;

		SearchParameterMap paramMap = new SearchParameterMap();

		paramMap.add("patient", createParam("Patient/" + thePatient.getIdElement().getValue()));
		IBundleProvider encounters = erp.getDao().search(paramMap, theRequestDetails, theServletResponse);

		// Assume less the 100 encounters currently
		for (IBaseResource baseResource : encounters.getResources(0, 100)) {
			Encounter encounter = (Encounter) baseResource;
			BundleBuilder encounterBundle = new BundleBuilder(this.fhirServer.getFhirContext());
			encounterBundle.addCollectionEntry(thePatient);
			encounterBundle.addCollectionEntry(baseResource);
			// Search on patient as encounter search is not working currently
			IBundleProvider observations = orp.getDao().search(paramMap, theRequestDetails, theServletResponse);

			boolean hasObservations = false;
			for (IBaseResource o : observations.getResources(0, 100)) {
				Observation observation = (Observation) o;

				// match to current encounter
				if (observation.getEncounter() != null) {

					if (encounter.getId().startsWith(observation.getEncounter().getReference() + "/")) {
						encounterBundle.addCollectionEntry(o);
						hasObservations = true;
					}
				}
			}

			if (hasObservations) {
				root.addCollectionEntry(encounterBundle.getBundle());
			}

		}

		return root.getBundle();

	}

}
