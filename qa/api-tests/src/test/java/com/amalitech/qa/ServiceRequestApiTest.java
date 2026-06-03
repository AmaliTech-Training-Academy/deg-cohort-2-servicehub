package com.amalitech.qa;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.testng.annotations.*;
import java.util.UUID;
import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;
import static org.testng.Assert.assertNotNull;

public class ServiceRequestApiTest {

    private String managerToken;
    private String agentToken;
    private String employeeToken;

    @BeforeClass
    public void setup() {
        RestAssured.baseURI = "http://localhost:8080";

        // BUG FIX: was admin@amalitech.com which does not exist in seed data
        managerToken = given()
            .contentType(ContentType.JSON)
            .body("{\"email\":\"manager@amalitech.com\",\"password\":\"password123\"}")
        .when().post("/api/auth/login")
        .then().statusCode(200).extract().path("token");
        assertNotNull(managerToken, "manager login failed — check seed data");

        agentToken = given()
            .contentType(ContentType.JSON)
            .body("{\"email\":\"agent@amalitech.com\",\"password\":\"password123\"}")
        .when().post("/api/auth/login")
        .then().statusCode(200).extract().path("token");
        assertNotNull(agentToken, "agent login failed — check seed data");

        employeeToken = given()
            .contentType(ContentType.JSON)
            .body("{\"email\":\"user@amalitech.com\",\"password\":\"password123\"}")
        .when().post("/api/auth/login")
        .then().statusCode(200).extract().path("token");
        assertNotNull(employeeToken, "employee login failed — check seed data");
    }

    // ── TC-AUTH ──────────────────────────────────────────────────────────────

    @Test
    public void testLoginReturnsToken() {
        // TC-AUTH-04
        given().contentType(ContentType.JSON)
            .body("{\"email\":\"manager@amalitech.com\",\"password\":\"password123\"}")
        .when().post("/api/auth/login")
        .then().statusCode(200)
            .body("token", notNullValue())
            .body("role", equalTo("MANAGER"));
    }

    @Test
    public void testLoginWrongPassword() {
        // TC-AUTH-05
        given().contentType(ContentType.JSON)
            .body("{\"email\":\"manager@amalitech.com\",\"password\":\"wrongpassword\"}")
        .when().post("/api/auth/login")
        .then().statusCode(400);
    }

    @Test
    public void testRegisterCreatesEmployee() {
        // TC-AUTH-01 — unique email per run to avoid duplicate conflict
        String email = "newuser_" + UUID.randomUUID() + "@test.com";
        given().contentType(ContentType.JSON)
            .body("{\"name\":\"Test User\",\"email\":\"" + email + "\",\"password\":\"password123\",\"department\":\"IT\"}")
        .when().post("/api/auth/register")
        .then().statusCode(200)
            .body("token", notNullValue())
            .body("role", equalTo("EMPLOYEE"));
    }

    @Test
    public void testRegisterDuplicateEmail() {
        // TC-AUTH-02
        given().contentType(ContentType.JSON)
            .body("{\"name\":\"Manager User\",\"email\":\"manager@amalitech.com\",\"password\":\"password123\",\"department\":\"IT\"}")
        .when().post("/api/auth/register")
        .then().statusCode(400);
    }

    @Test
    public void testProtectedRouteWithoutToken() {
        // TC-AUTH-07
        given()
        .when().get("/api/requests")
        .then().statusCode(401);
    }

    @Test
    public void testProtectedRouteWithInvalidToken() {
        // TC-AUTH-08
        given().header("Authorization", "Bearer this.is.not.valid")
        .when().get("/api/requests")
        .then().statusCode(401);
    }

    // ── TC-REQ ──────────────────────────────────────────────────────────────

    @Test
    public void testGetAllRequestsAsManager() {
        // TC-REQ-07
        given().header("Authorization", "Bearer " + managerToken)
        .when().get("/api/requests")
        .then().statusCode(200)
            .body("content", notNullValue());
    }

    @Test
    public void testGetAllRequestsAsEmployeeForbidden() {
        // TC-REQ-08
        given().header("Authorization", "Bearer " + employeeToken)
        .when().get("/api/requests")
        .then().statusCode(403);
    }

    @Test
    public void testGetMyRequests() {
        // TC-REQ-09
        given().header("Authorization", "Bearer " + employeeToken)
        .when().get("/api/requests/my-requests")
        .then().statusCode(200)
            .body("content", notNullValue());
    }

    @Test
    public void testCreateServiceRequest() {
        // TC-REQ-01 — BUG FIX: removed invalid departmentId field; department is auto-assigned
        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + employeeToken)
            .body("{\"title\":\"Laptop not working\",\"description\":\"Screen is blank\",\"category\":\"IT_SUPPORT\",\"priority\":\"HIGH\"}")
        .when().post("/api/requests")
        .then().statusCode(200)
            .body("title", equalTo("Laptop not working"))
            .body("status", equalTo("OPEN"))
            .body("departmentName", equalTo("IT Support"));
    }

    @Test
    public void testCreateRequestMissingTitle() {
        // TC-REQ-02
        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + employeeToken)
            .body("{\"title\":\"\",\"description\":\"Some issue\",\"category\":\"IT_SUPPORT\",\"priority\":\"HIGH\"}")
        .when().post("/api/requests")
        .then().statusCode(400);
    }

    @Test
    public void testCreateRequestInvalidCategory() {
        // TC-REQ-04
        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + employeeToken)
            .body("{\"title\":\"Test\",\"description\":\"Test\",\"category\":\"INVALID\",\"priority\":\"HIGH\"}")
        .when().post("/api/requests")
        .then().statusCode(400);
    }

    // ── TC-ROUTE ─────────────────────────────────────────────────────────────

    @Test
    public void testAutoRouteItSupport() {
        // TC-ROUTE-01
        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + employeeToken)
            .body("{\"title\":\"IT issue\",\"description\":\"Printer broken\",\"category\":\"IT_SUPPORT\",\"priority\":\"LOW\"}")
        .when().post("/api/requests")
        .then().statusCode(200)
            .body("departmentName", equalTo("IT Support"));
    }

    @Test
    public void testAutoRouteHrRequest() {
        // TC-ROUTE-02
        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + employeeToken)
            .body("{\"title\":\"Leave request\",\"description\":\"Need annual leave\",\"category\":\"HR_REQUEST\",\"priority\":\"LOW\"}")
        .when().post("/api/requests")
        .then().statusCode(200)
            .body("departmentName", equalTo("HR"));
    }

    @Test
    public void testAutoRouteFacilities() {
        // TC-ROUTE-03
        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + employeeToken)
            .body("{\"title\":\"AC broken\",\"description\":\"Office too hot\",\"category\":\"FACILITIES\",\"priority\":\"MEDIUM\"}")
        .when().post("/api/requests")
        .then().statusCode(200)
            .body("departmentName", equalTo("Facilities"));
    }

    // ── TC-WORKFLOW ──────────────────────────────────────────────────────────

    @Test
    public void testValidStatusTransitionOpenToAssigned() {
        // TC-WF-01
        Integer requestId = given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + employeeToken)
            .body("{\"title\":\"Workflow test\",\"description\":\"Testing transitions\",\"category\":\"IT_SUPPORT\",\"priority\":\"MEDIUM\"}")
        .when().post("/api/requests")
        .then().statusCode(200).extract().path("id");

        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + agentToken)
            .body("{\"newStatus\":\"ASSIGNED\"}")
        .when().put("/api/requests/" + requestId + "/status")
        .then().statusCode(200)
            .body("status", equalTo("ASSIGNED"));
    }

    @Test
    public void testInvalidStatusTransitionSkipStep() {
        // TC-WF-05 — OPEN directly to IN_PROGRESS must be rejected
        Integer requestId = given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + employeeToken)
            .body("{\"title\":\"Skip transition test\",\"description\":\"Should reject\",\"category\":\"FACILITIES\",\"priority\":\"LOW\"}")
        .when().post("/api/requests")
        .then().statusCode(200).extract().path("id");

        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + agentToken)
            .body("{\"newStatus\":\"IN_PROGRESS\"}")
        .when().put("/api/requests/" + requestId + "/status")
        .then().statusCode(400);
    }

    @Test
    public void testStatusUpdateByEmployeeForbidden() {
        // TC-WF-11
        Integer requestId = given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + employeeToken)
            .body("{\"title\":\"Auth test\",\"description\":\"Employee should not update status\",\"category\":\"IT_SUPPORT\",\"priority\":\"LOW\"}")
        .when().post("/api/requests")
        .then().statusCode(200).extract().path("id");

        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + employeeToken)
            .body("{\"newStatus\":\"ASSIGNED\"}")
        .when().put("/api/requests/" + requestId + "/status")
        .then().statusCode(403);
    }

    // ── TC-SLA ───────────────────────────────────────────────────────────────

    @Test
    public void testSlaDeadlineIsSetOnCreate() {
        // TC-SLA-01 — slaDeadline must never be null
        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + employeeToken)
            .body("{\"title\":\"SLA test\",\"description\":\"Check deadline\",\"category\":\"IT_SUPPORT\",\"priority\":\"CRITICAL\"}")
        .when().post("/api/requests")
        .then().statusCode(200)
            .body("slaDeadline", notNullValue());
    }

    @Test
    public void testIsOverdueFalseForFreshRequest() {
        // TC-SLA-05 — brand new request should never be overdue
        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + employeeToken)
            .body("{\"title\":\"Fresh request\",\"description\":\"Should not be overdue\",\"category\":\"IT_SUPPORT\",\"priority\":\"LOW\"}")
        .when().post("/api/requests")
        .then().statusCode(200)
            .body("isOverdue", not(equalTo(true)));
    }

    // ── TC-DEPT ──────────────────────────────────────────────────────────────

    @Test
    public void testGetDepartments() {
        // TC-DEPT-01
        given().header("Authorization", "Bearer " + managerToken)
        .when().get("/api/departments")
        .then().statusCode(200)
            .body("size()", greaterThanOrEqualTo(3));
    }

    @Test
    public void testCreateDepartmentAsEmployeeForbidden() {
        // TC-DEPT-03
        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + employeeToken)
            .body("{\"name\":\"New Dept\",\"category\":\"IT_SUPPORT\",\"contactEmail\":\"new@test.com\"}")
        .when().post("/api/departments")
        .then().statusCode(403);
    }
}
