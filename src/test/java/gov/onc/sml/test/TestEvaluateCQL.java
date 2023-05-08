package gov.onc.sml.test;

import static java.lang.Thread.sleep;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Subscription;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.starter.Application;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.CacheControlDirective;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = Application.class, properties = {
		"spring.datasource.url=jdbc:h2:mem:dbr4",
		"hapi.fhir.enable_repository_validating_interceptor=true",
		"hapi.fhir.fhir_version=r4",
		"hapi.fhir.subscription.websocket_enabled=true",
		"hapi.fhir.mdm_enabled=true",
		"hapi.fhir.implementationguides.dk-core.name=hl7.fhir.dk.core",
		"hapi.fhir.implementationguides.dk-core.version=1.1.0",
		// Override is currently required when using MDM as the construction of the MDM
		// beans are ambiguous as they are constructed multiple places. This is evident
		// when running in a spring boot environment
		"spring.main.allow-bean-definition-overriding=true" })
class TestEvaluateCQL {
	private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(TestEvaluateCQL.class);
	private IGenericClient ourClient;
	private FhirContext ourCtx;

	@LocalServerPort
	private int port;

	@Test
	@Order(0)
	void testCreateAndRead() {
		String methodName = "testCreateAndRead";
		ourLog.info("Entering " + methodName + "()...");

		Patient pt = new Patient();
		pt.setActive(true);
		pt.getBirthDateElement().setValueAsString("2020-01-01");
		pt.addIdentifier().setSystem("http://foo").setValue("12345");
		pt.addName().setFamily(methodName);
		IIdType id = ourClient.create().resource(pt).execute().getId();

		Patient pt2 = ourClient.read().resource(Patient.class).withId(id).execute();
		assertEquals(methodName, pt2.getName().get(0).getFamily());

		// Wait until the MDM message has been processed
		await().atMost(1, TimeUnit.MINUTES).until(() -> getGoldenResourcePatient() != null);
		Patient goldenRecord = getGoldenResourcePatient();

		// Verify that a golden record Patient was created
		assertNotNull(
			goldenRecord.getMeta().getTag("http://hapifhir.io/fhir/NamingSystem/mdm-record-status", "GOLDEN_RECORD"));
	}
	
	Bundle get(String path) throws IOException {
		
		
		Path filePath = Path.of("/Users/seanmuir/git/synthea/output/fhir/"+path);

		String content = Files.readString(filePath);

		// Instantiate a new parser
		IParser parser = ourCtx.newJsonParser();

		// Parse it
		Bundle bundle = parser.parseResource(Bundle.class, content);
		
		return bundle;
		
	}
	@Test
	void testEvaluateCQL() throws IOException {
		
 	
		
		FhirContext ctx = FhirContext.forR4();
		Bundle resp = ourClient.transaction().withBundle(get("practitionerInformation1667837391816.json")).execute();
		 resp = ourClient.transaction().withBundle(get("hospitalInformation1667837391816.json")).execute();
		 resp = ourClient.transaction().withBundle(get("Donald774_Beier427_8e38ddff-dcc3-dcb8-a19e-f6f69a4bb9e8.json")).execute();
//		 resp = ourClient.transaction().withBundle(get("Kory651_Maggio310_256873e0-c611-ab57-8053-2f8936eec144.json")).execute();		
		String methodName = "testBundleOfBundles";
		ourLog.info("Entering " + methodName + "()...");		
		 applyCQL();
	}
	
	private  void applyCQL() {

		Parameters outParams = ourClient
			.operation().onServer()
			.named("$applycql").withNoParameters(Parameters.class)
			.useHttpGet()  
			.execute();		
		 Resource responseBundle =  outParams.getParameter().get(0).getResource();
		System.out.println(ourCtx.newXmlParser().setPrettyPrint(true).encodeResourceToString(responseBundle));
	}

	
	
	

	private Patient getGoldenResourcePatient() {
		Bundle bundle = ourClient.search().forResource(Patient.class)
			.withTag("http://hapifhir.io/fhir/NamingSystem/mdm-record-status", "GOLDEN_RECORD")
			.cacheControl(new CacheControlDirective().setNoCache(true)).returnBundle(Bundle.class).execute();
		if (bundle.getEntryFirstRep() != null) {
			return (Patient) bundle.getEntryFirstRep().getResource();
		} else {
			return null;
		}
	}

	@Test
	public void testBatchPutWithIdenticalTags() {
		String batchPuts = "{\n" +
			"\t\"resourceType\": \"Bundle\",\n" +
			"\t\"id\": \"patients\",\n" +
			"\t\"type\": \"batch\",\n" +
			"\t\"entry\": [\n" +
			"\t\t{\n" +
			"\t\t\t\"request\": {\n" +
			"\t\t\t\t\"method\": \"PUT\",\n" +
			"\t\t\t\t\"url\": \"Patient/pat-1\"\n" +
			"\t\t\t},\n" +
			"\t\t\t\"resource\": {\n" +
			"\t\t\t\t\"resourceType\": \"Patient\",\n" +
			"\t\t\t\t\"id\": \"pat-1\",\n" +
			"\t\t\t\t\"meta\": {\n" +
			"\t\t\t\t\t\"tag\": [\n" +
			"\t\t\t\t\t\t{\n" +
			"\t\t\t\t\t\t\t\"system\": \"http://mysystem.org\",\n" +
			"\t\t\t\t\t\t\t\"code\": \"value2\"\n" +
			"\t\t\t\t\t\t}\n" +
			"\t\t\t\t\t]\n" +
			"\t\t\t\t}\n" +
			"\t\t\t},\n" +
			"\t\t\t\"fullUrl\": \"/Patient/pat-1\"\n" +
			"\t\t},\n" +
			"\t\t{\n" +
			"\t\t\t\"request\": {\n" +
			"\t\t\t\t\"method\": \"PUT\",\n" +
			"\t\t\t\t\"url\": \"Patient/pat-2\"\n" +
			"\t\t\t},\n" +
			"\t\t\t\"resource\": {\n" +
			"\t\t\t\t\"resourceType\": \"Patient\",\n" +
			"\t\t\t\t\"id\": \"pat-2\",\n" +
			"\t\t\t\t\"meta\": {\n" +
			"\t\t\t\t\t\"tag\": [\n" +
			"\t\t\t\t\t\t{\n" +
			"\t\t\t\t\t\t\t\"system\": \"http://mysystem.org\",\n" +
			"\t\t\t\t\t\t\t\"code\": \"value2\"\n" +
			"\t\t\t\t\t\t}\n" +
			"\t\t\t\t\t]\n" +
			"\t\t\t\t}\n" +
			"\t\t\t},\n" +
			"\t\t\t\"fullUrl\": \"/Patient/pat-2\"\n" +
			"\t\t}\n" +
			"\t]\n" +
			"}";
		Bundle bundle = FhirContext.forR4().newJsonParser().parseResource(Bundle.class, batchPuts);
		ourClient.transaction().withBundle(bundle).execute();
	}

 

	private int activeSubscriptionCount() {
		return ourClient.search().forResource(Subscription.class).where(Subscription.STATUS.exactly().code("active"))
			.cacheControl(new CacheControlDirective().setNoCache(true)).returnBundle(Bundle.class).execute().getEntry()
			.size();
	}

	@BeforeEach
	void beforeEach() {

		ourCtx = FhirContext.forR4();
		ourCtx.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
		ourCtx.getRestfulClientFactory().setSocketTimeout(1200 * 1000);
		String ourServerBase = "http://localhost:" + port + "/fhir/";
		ourClient = ourCtx.newRestfulGenericClient(ourServerBase);

		await().atMost(2, TimeUnit.MINUTES).until(() -> {
			sleep(1000); // execute below function every 1 second
			return activeSubscriptionCount() == 2; // 2 subscription based on mdm-rules.json
		});
	}
}
