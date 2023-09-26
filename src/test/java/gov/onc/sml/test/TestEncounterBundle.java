package gov.onc.sml.test;

import static java.lang.Thread.sleep;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Subscription;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.starter.Application;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.CacheControlDirective;
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

	List<Identifier> postSyntheaBundles() throws IOException {
		
		List<Identifier> patientIdentifiers = new ArrayList<Identifier>();
		
		Consumer<? super Path> loadSyntheaBundle = new Consumer<Path>() {
			@Override
			public void accept(Path bundlePath) {
				try {
					System.err.println(bundlePath);
					Bundle bundle= (Bundle) ourCtx.newJsonParser().parseResource(Files.readString(bundlePath));;
					Bundle resp = ourClient.transaction().withBundle(bundle).execute();					
					List<Patient> patients = BundleUtil.toListOfResourcesOfType(ourCtx, bundle, Patient.class);
					for (Patient patient :patients) {
						patientIdentifiers.add(patient.getIdentifierFirstRep());						
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
		
		return patientIdentifiers;

	}

	@Test
	void testBundleOfBundles() throws Exception {
		List<Identifier> patientIdentifiers = postSyntheaBundles();
		String methodName = "testBundleOfBundles";
		ourLog.info("Entering " + methodName + "()...");
		
		for (Identifier patientIdentifier : patientIdentifiers) {
			bundleOfBundlesOperation(patientIdentifier);
		}
		
	}

	private void bundleOfBundlesOperation(Identifier i) throws Exception {	
		Parameters inParams = new Parameters();
		inParams.addParameter().setName("patient").setValue(new StringType("0"));
		Parameters outParams = ourClient.operation().onServer().named("$bundleofbundles").withParameters(inParams)
				.useHttpGet() 
				.execute();
		Resource responseBundle = outParams.getParameter().get(0).getResource();
		
		Path testPath = Paths.get("target/test-output/bundleofbundles/" + i.getValue());
		if (!Files.exists(testPath)) {
			Files.createDirectories(testPath);
		}

		Path path = Paths.get("target/test-output/bundleofbundles/" + i.getValue() + "/Patient" + i.getValue() + ".xml");
		
		
		 

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
