package gov.onc.sml.test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.starter.Application;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.util.BundleUtil;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = Application.class, properties = {
		"spring.datasource.url=jdbc:h2:mem:dbr4", "hapi.fhir.enable_repository_validating_interceptor=true",
		"hapi.fhir.fhir_version=r4", "hapi.fhir.subscription.websocket_enabled=true", "hapi.fhir.mdm_enabled=true",
		"hapi.fhir.implementationguides.dk-core.name=hl7.fhir.dk.core",
		"hapi.fhir.implementationguides.dk-core.version=1.1.0", "spring.main.allow-bean-definition-overriding=true" })
class TestEncounterBundle {

	@BeforeAll
	public static void setEnvironment() {
		System.setProperty("synthea.bundles", "/Users/seanmuir/git/test/MNISTDocker/temp/fhir");
	}

	private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(TestEncounterBundle.class);
	private IGenericClient ourClient;
	private FhirContext ourCtx;

	@LocalServerPort
	private int port;

	private static Predicate<Path> bundleStartsWith(String prefix) {
		return path -> path.getFileName().startsWith(prefix);
	}

	private static Predicate<Path> bundleIsPatient() {
		return path -> (!path.getFileName().startsWith("practitionerInformation")
				&& !path.getFileName().startsWith("hospitalInformation"));
	}

	void postSyntheaBundles() throws IOException {
		
	 
		
		Consumer<? super Path> loadSyntheaBundle = new Consumer<Path>() {
			@Override
			public void accept(Path bundlePath) {
				try {
					System.err.println(bundlePath);
					Bundle bundle= (Bundle) ourCtx.newJsonParser().parseResource(Files.readString(bundlePath));;
					Bundle resp = ourClient.transaction().withBundle(bundle).execute();					
					List<Patient> patients = BundleUtil.toListOfResourcesOfType(ourCtx, resp, Patient.class);
					
System.err.println(					ourCtx.newJsonParser().encodeResourceToString(resp));
					
					for (Patient patient :patients) {
						System.out.println(patient.getId());
						System.out.println(patient.getIdentifierFirstRep().getValue()  );
//						patientIdentifiers.add(patient.getIdentifierFirstRep());						
					}				
				} catch (IOException e) {

				}
			}
		};
		try {
			
			
			try (Stream<Path> paths = Files.walk(Paths.get(System.getProperty("synthea.bundles")))) {

				paths.filter(Files::isRegularFile).filter(bundleStartsWith("practitionerInformation"))
						.forEach(loadSyntheaBundle);

			}
			try (Stream<Path> paths = Files.walk(Paths.get(System.getProperty("synthea.bundles")))) {

				paths.filter(Files::isRegularFile).filter(bundleStartsWith("hospitalInformation"))
						.forEach(loadSyntheaBundle);

			}
			try (Stream<Path> paths = Files.walk(Paths.get(System.getProperty("synthea.bundles")))) {

				paths.filter(Files::isRegularFile).filter(bundleIsPatient()).forEach(loadSyntheaBundle);

			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
 

	}

	@Test
	void testBundleOfBundles() throws Exception {
		postSyntheaBundles();
		String methodName = "testBundleOfBundles";
		ourLog.info("Entering " + methodName + "()...");
		
		Bundle thePatients = (Bundle) ourClient.search().forResource("Patient").execute();
		
		List<IBaseResource> patients = new ArrayList<>();
		
		patients.addAll(BundleUtil.toListOfResources(ourCtx, thePatients));
		
		for (IBaseResource resource: patients) {
			Patient patient = (Patient) resource;			
			System.err.println(  ourCtx.newJsonParser().encodeResourceToString(thePatients));
			System.err.println(patient.getIdElement().getIdPart());
			bundleOfBundlesOperation(patient.getIdElement().getIdPart());			
		}
		
//		thePatients.getEntry().
		
		System.err.println(thePatients);
		
//		for (Identifier patientIdentifier : patientIdentifiers) {
//			bundleOfBundlesOperation("patientIdentifier");
//		}
		
	}

	private void bundleOfBundlesOperation(String thePatientId) throws Exception {	
		Parameters inParams = new Parameters();
		inParams.addParameter().setName("patient").setValue(new StringType(thePatientId));
		Parameters outParams = ourClient.operation().onServer().named("$bundleofbundles").withParameters(inParams)
				.useHttpGet() 
				.execute();
		Resource responseBundle = outParams.getParameter().get(0).getResource();
		
		Path testPath = Paths.get("target/test-output/bundleofbundles/" + thePatientId);
		if (!Files.exists(testPath)) {
			Files.createDirectories(testPath);
		}

		Path path = Paths.get("target/test-output/bundleofbundles/" + thePatientId + "/Patient" + thePatientId + ".xml");
		
		
		 

		Files.write(path, ourCtx.newXmlParser().setPrettyPrint(true).encodeResourceToString(responseBundle).getBytes() );
		
	}

	@BeforeEach
	void beforeEach() {
		ourCtx = FhirContext.forR4();
		ourCtx.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
		ourCtx.getRestfulClientFactory().setSocketTimeout(1200 * 1000);
		String ourServerBase = "http://localhost:" + port + "/fhir/";
		ourClient = ourCtx.newRestfulGenericClient(ourServerBase);
	}
}
