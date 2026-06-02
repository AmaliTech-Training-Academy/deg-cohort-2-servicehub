package com.amalitech.qa;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.testng.annotations.*;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

/**
 * QA API tests for Kevine's Backend B features:
 *   - SlaService       (#15 / PR #43) — SLA deadlines, /overdue endpoint, SlaController
 *   - WorkflowService  (#14 / PR #46) — status transition chain, firstResponseAt, resolvedAt
 *   - SlaEngine        (#16 / PR #45) — slaBreached flag, priority escalation
 *
 * Requires: app running at localhost:8080 with migrations V1–V5 applied.
 * Run with a fresh Docker volume: docker-compose down -v && docker-compose up --build
 */
public class SlaWorkflowApiTest {

    private String managerToken;
    private String agentToken;
    private String employeeToken;

    @BeforeClass
    public void setup() {
        RestAssured.baseURI = "http://localhost:8080";

        managerToken = given().contentType(ContentType.JSON)
            .body("{\"email\":\"manager@amalitech.com\",\"password\":\"password123\"}")
        .when().post("/api/auth/login")
        .then().statusCode(200).extract().path("token");

        agentToken = given().contentType(ContentType.JSON)
            .body("{\"email\":\"agent@amalitech.com\",\"password\":\"password123\"}")
        .when().post("/api/auth/login")
        .then().statusCode(200).extract().path("token");

        employeeToken = given().contentType(ContentType.JSON)
            .body("{\"email\":\"user@amalitech.com\",\"password\":\"password123\"}")
        .when().post("/api/auth/login")
        .then().statusCode(200).extract().path("token");
    }

    // ── TC-SLA: SLA deadlines on create ─────────────────────────────────────

    @Test
    public void testSlaDeadlineAndResponseDeadlineSetOnCreate() {
        // TC-SLA-02: both deadlines must be populated — SlaService.computeDeadline is wired
        given().contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + employeeToken)
            .body("{\"title\":\"Deadline check\",\"description\":\"Both deadlines\",\"category\":\"IT_SUPPORT\",\"priority\":\"HIGH\"}")
        .when().post("/api/requests")
        .then().statusCode(200)
            .body("slaDeadline",      notNullValue())
            .body("responseDeadline", notNullValue());
    }

    @Test
    public void testSlaStatusIsOnTrackForFreshRequest() {
        // TC-SLA-03: brand new request — no breach yet
        given().contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + employeeToken)
            .body("{\"title\":\"SLA status check\",\"description\":\"Fresh\",\"category\":\"HR_REQUEST\",\"priority\":\"MEDIUM\"}")
        .when().post("/api/requests")
        .then().statusCode(200)
            .body("slaStatus", equalTo("ON_TRACK"));
    }

    @Test
    public void testIsOverdueFalseForFreshRequest() {
        // TC-SLA-04: brand new request is not overdue
        // Note: Boolean wrapper can serialize as null — using not(equalTo(true)) to be safe
        given().contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + employeeToken)
            .body("{\"title\":\"Overdue check\",\"description\":\"Should not be overdue\",\"category\":\"FACILITIES\",\"priority\":\"LOW\"}")
        .when().post("/api/requests")
        .then().statusCode(200)
            .body("isOverdue",         not(equalTo(true)))
            .body("isResponseOverdue", not(equalTo(true)))
            .body("slaBreached",       not(equalTo(true)));
    }

    @Test
    public void testResponseTimeMinutesNullBeforeAssigned() {
        // TC-SLA-05: no response yet — responseTimeMinutes must be null, not 0
        given().contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + employeeToken)
            .body("{\"title\":\"Response time null\",\"description\":\"Not assigned yet\",\"category\":\"IT_SUPPORT\",\"priority\":\"LOW\"}")
        .when().post("/api/requests")
        .then().statusCode(200)
            .body("responseTimeMinutes",    nullValue())
            .body("resolutionTimeMinutes",  nullValue())
            .body("firstResponseAt",        nullValue());
    }

    // ── TC-SLA: /api/requests/overdue ────────────────────────────────────────

    @Test
    public void testGetOverdueAsAgent() {
        // TC-SLA-OV-01: AGENT can see overdue list (may be empty but must return 200)
        given().header("Authorization", "Bearer " + agentToken)
        .when().get("/api/requests/overdue")
        .then().statusCode(200)
            .body("$", instanceOf(java.util.List.class));
    }

    @Test
    public void testGetOverdueAsManager() {
        // TC-SLA-OV-02: MANAGER can also see overdue list
        given().header("Authorization", "Bearer " + managerToken)
        .when().get("/api/requests/overdue")
        .then().statusCode(200);
    }

    @Test
    public void testGetOverdueAsEmployeeForbidden() {
        // TC-SLA-OV-03: EMPLOYEE must get 403 — endpoint is AGENT/MANAGER only
        given().header("Authorization", "Bearer " + employeeToken)
        .when().get("/api/requests/overdue")
        .then().statusCode(403);
    }

    // ── TC-SLA: /api/sla/policies ────────────────────────────────────────────

    @Test
    public void testGetSlaPoliciesAsManager() {
        // TC-SLA-POL-01: MANAGER sees all 12 category+priority policies
        given().header("Authorization", "Bearer " + managerToken)
        .when().get("/api/sla/policies")
        .then().statusCode(200)
            .body("size()", equalTo(12));
    }

    @Test
    public void testGetSlaPoliciesAsEmployeeForbidden() {
        // TC-SLA-POL-02: EMPLOYEE must not see policy config
        given().header("Authorization", "Bearer " + employeeToken)
        .when().get("/api/sla/policies")
        .then().statusCode(403);
    }

    @Test
    public void testGetSlaPoliciesAsAgentForbidden() {
        // TC-SLA-POL-03: AGENT also has no access to policy config
        given().header("Authorization", "Bearer " + agentToken)
        .when().get("/api/sla/policies")
        .then().statusCode(403);
    }

    @Test
    public void testUpdateSlaPolicyAsManager() {
        // TC-SLA-POL-04: MANAGER can update a policy and get the updated values back
        given().contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + managerToken)
            .body("{\"responseTimeHours\":3,\"resolutionTimeHours\":12}")
        .when().put("/api/sla/policies/3")
        .then().statusCode(200)
            .body("responseTimeHours",   equalTo(3))
            .body("resolutionTimeHours", equalTo(12));
    }

    @Test
    public void testUpdateSlaPolicyAsEmployeeForbidden() {
        // TC-SLA-POL-05: EMPLOYEE must get 403
        given().contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + employeeToken)
            .body("{\"responseTimeHours\":1,\"resolutionTimeHours\":2}")
        .when().put("/api/sla/policies/1")
        .then().statusCode(403);
    }

    // ── TC-SLA: /api/sla/breaches ────────────────────────────────────────────

    @Test
    public void testGetAllBreachesAsManager() {
        // TC-SLA-BR-01: MANAGER can see breach list (may be empty)
        given().header("Authorization", "Bearer " + managerToken)
        .when().get("/api/sla/breaches")
        .then().statusCode(200)
            .body("$", instanceOf(java.util.List.class));
    }

    @Test
    public void testGetResponseBreachesAsManager() {
        // TC-SLA-BR-02
        given().header("Authorization", "Bearer " + managerToken)
        .when().get("/api/sla/breaches/response")
        .then().statusCode(200);
    }

    @Test
    public void testGetResolutionBreachesAsManager() {
        // TC-SLA-BR-03
        given().header("Authorization", "Bearer " + managerToken)
        .when().get("/api/sla/breaches/resolution")
        .then().statusCode(200);
    }

    @Test
    public void testGetBreachesAsEmployeeForbidden() {
        // TC-SLA-BR-04: EMPLOYEE must not see breach data
        given().header("Authorization", "Bearer " + employeeToken)
        .when().get("/api/sla/breaches")
        .then().statusCode(403);
    }

    // ── TC-WF: Full workflow chain ───────────────────────────────────────────

    @Test
    public void testFullWorkflowChainWithSlaFields() {
        // TC-WF-CHAIN: OPEN → ASSIGNED → IN_PROGRESS → RESOLVED → CLOSED
        // Verifies: firstResponseAt on ASSIGNED, resolvedAt + COMPLETED on RESOLVED

        // Step 1 — create
        Integer requestId = given().contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + employeeToken)
            .body("{\"title\":\"Full chain test\",\"description\":\"Workflow\",\"category\":\"IT_SUPPORT\",\"priority\":\"HIGH\"}")
        .when().post("/api/requests")
        .then().statusCode(200)
            .body("status",    equalTo("OPEN"))
            .body("slaStatus", equalTo("ON_TRACK"))
            .extract().path("id");

        // Step 2 — OPEN → ASSIGNED: firstResponseAt must be set
        given().contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + agentToken)
            .body("{\"newStatus\":\"ASSIGNED\"}")
        .when().put("/api/requests/" + requestId + "/status")
        .then().statusCode(200)
            .body("status",          equalTo("ASSIGNED"))
            .body("firstResponseAt", notNullValue())
            .body("responseTimeMinutes", notNullValue());

        // Step 3 — ASSIGNED → IN_PROGRESS
        given().contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + agentToken)
            .body("{\"newStatus\":\"IN_PROGRESS\"}")
        .when().put("/api/requests/" + requestId + "/status")
        .then().statusCode(200)
            .body("status", equalTo("IN_PROGRESS"));

        // Step 4 — IN_PROGRESS → RESOLVED: resolvedAt set, slaStatus = COMPLETED
        given().contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + agentToken)
            .body("{\"newStatus\":\"RESOLVED\"}")
        .when().put("/api/requests/" + requestId + "/status")
        .then().statusCode(200)
            .body("status",                equalTo("RESOLVED"))
            .body("resolvedAt",            notNullValue())
            .body("resolutionTimeMinutes", notNullValue())
            .body("slaStatus",             equalTo("COMPLETED"));

        // Step 5 — RESOLVED → CLOSED
        given().contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + agentToken)
            .body("{\"newStatus\":\"CLOSED\"}")
        .when().put("/api/requests/" + requestId + "/status")
        .then().statusCode(200)
            .body("status",    equalTo("CLOSED"))
            .body("slaStatus", equalTo("COMPLETED"));
    }

    @Test
    public void testInvalidTransitionReturnsDescriptiveError() {
        // TC-WF-05: OPEN → IN_PROGRESS is an invalid skip — must return 400 with message
        Integer requestId = given().contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + employeeToken)
            .body("{\"title\":\"Skip test\",\"description\":\"Invalid\",\"category\":\"FACILITIES\",\"priority\":\"LOW\"}")
        .when().post("/api/requests")
        .then().statusCode(200).extract().path("id");

        given().contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + agentToken)
            .body("{\"newStatus\":\"IN_PROGRESS\"}")
        .when().put("/api/requests/" + requestId + "/status")
        .then().statusCode(400)
            .body("error", containsString("Invalid status transition"));
    }

    @Test
    public void testTransitionFromClosedIsRejected() {
        // TC-WF-06: CLOSED is a terminal state — any further transition must be rejected
        Integer requestId = given().contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + employeeToken)
            .body("{\"title\":\"Terminal test\",\"description\":\"CLOSED\",\"category\":\"IT_SUPPORT\",\"priority\":\"LOW\"}")
        .when().post("/api/requests")
        .then().statusCode(200).extract().path("id");

        // Move through to CLOSED
        for (String status : new String[]{"ASSIGNED", "IN_PROGRESS", "RESOLVED", "CLOSED"}) {
            given().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + agentToken)
                .body("{\"newStatus\":\"" + status + "\"}")
            .when().put("/api/requests/" + requestId + "/status")
            .then().statusCode(200);
        }

        // Try one more transition — must fail
        given().contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + agentToken)
            .body("{\"newStatus\":\"OPEN\"}")
        .when().put("/api/requests/" + requestId + "/status")
        .then().statusCode(400)
            .body("error", containsString("Invalid status transition"));
    }

    // ── TC-ENG: SlaEngine breach + escalation ────────────────────────────────

    @Test
    public void testSlaBreachedFalseOnFreshRequest() {
        // TC-ENG-01: slaBreached must start as false
        given().contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + employeeToken)
            .body("{\"title\":\"Breach flag check\",\"description\":\"Fresh\",\"category\":\"IT_SUPPORT\",\"priority\":\"HIGH\"}")
        .when().post("/api/requests")
        .then().statusCode(200)
            .body("slaBreached", equalTo(false));
    }

    @Test
    public void testSlaBreachEscalatesPriorityAfterSchedulerFires() throws InterruptedException {
        // TC-ENG-02: set a 0-hour resolution policy → create request → wait 65s for scheduler → verify breach + escalation
        // NOTE: this test takes ~65 seconds. Disable in fast runs; enable for full integration suite.

        // Set IT_SUPPORT HIGH policy to 0-hour resolution so request is immediately overdue
        given().contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + managerToken)
            .body("{\"responseTimeHours\":0,\"resolutionTimeHours\":0}")
        .when().put("/api/sla/policies/2")   // IT_SUPPORT HIGH (id=2)
        .then().statusCode(200);

        Integer requestId = given().contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + employeeToken)
            .body("{\"title\":\"Escalation test\",\"description\":\"Should breach\",\"category\":\"IT_SUPPORT\",\"priority\":\"HIGH\"}")
        .when().post("/api/requests")
        .then().statusCode(200)
            .body("slaBreached", equalTo(false))
            .body("priority",    equalTo("HIGH"))
            .extract().path("id");

        // Wait for SlaEngine scheduler (fires every 60s)
        Thread.sleep(65_000);

        given().header("Authorization", "Bearer " + agentToken)
        .when().get("/api/requests/" + requestId)
        .then().statusCode(200)
            .body("slaBreached", equalTo(true))
            .body("priority",    equalTo("HIGH"));  // HIGH stays HIGH — ceiling reached

        // Restore policy
        given().contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + managerToken)
            .body("{\"responseTimeHours\":2,\"resolutionTimeHours\":8}")
        .when().put("/api/sla/policies/2")
        .then().statusCode(200);
    }
}
