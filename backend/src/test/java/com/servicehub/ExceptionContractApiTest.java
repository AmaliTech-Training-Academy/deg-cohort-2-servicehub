package com.servicehub;

import com.servicehub.model.ServiceRequest;
import com.servicehub.model.User;
import com.servicehub.model.enums.Priority;
import com.servicehub.model.enums.RequestCategory;
import com.servicehub.model.enums.RequestStatus;
import com.servicehub.model.enums.Role;
import com.servicehub.repository.ServiceRequestRepository;
import com.servicehub.repository.UserRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests verifying the exception -> HTTP status contract from PR #37.
 *
 * Regression guards for downstream PRs:
 *   Kevine #43/#46: updateRequest must throw ForbiddenException (not RuntimeException).
 *                   validateStatusTransition must throw BadRequestException (not RuntimeException).
 *   David  #39:     duplicate email must produce 409 via EmailAlreadyExistsException.
 *
 * Run standalone: mvn test -Dtest=ExceptionContractApiTest -pl backend
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ExceptionContractApiTest {

    @LocalServerPort int port;

    @Autowired UserRepository userRepository;
    @Autowired ServiceRequestRepository requestRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private String employeeToken;
    private String otherToken;
    private String agentToken;
    private Long requestId;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        requestRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();

        User employee = userRepository.save(User.builder()
                .email("employee@test.com").fullName("Test Employee")
                .password(passwordEncoder.encode("pass123")).role(Role.EMPLOYEE).build());

        userRepository.save(User.builder()
                .email("other@test.com").fullName("Other Employee")
                .password(passwordEncoder.encode("pass123")).role(Role.EMPLOYEE).build());

        userRepository.save(User.builder()
                .email("agent@test.com").fullName("Test Agent")
                .password(passwordEncoder.encode("pass123")).role(Role.AGENT).build());

        ServiceRequest req = requestRepository.save(ServiceRequest.builder()
                .title("Fix printer").description("Printer not working")
                .category(RequestCategory.IT_SUPPORT).priority(Priority.HIGH)
                .status(RequestStatus.OPEN).requester(employee)
                .slaDeadline(LocalDateTime.now().plusHours(4))
                .responseDeadline(LocalDateTime.now().plusHours(1))
                .build());
        requestId = req.getId();

        employeeToken = loginToken("employee@test.com", "pass123");
        otherToken    = loginToken("other@test.com",    "pass123");
        agentToken    = loginToken("agent@test.com",    "pass123");
    }

    private String loginToken(String email, String password) {
        return given()
                .contentType(ContentType.JSON)
                .body(Map.of("email", email, "password", password))
                .post("/api/auth/login")
                .then().statusCode(200)
                .extract().path("token");
    }

    @Test @Order(1)
    void getById_nonExistentRequest_returns404() {
        given()
                .header("Authorization", "Bearer " + employeeToken)
                .get("/api/requests/99999")
                .then()
                .statusCode(404)
                .body("error", equalTo("Request not found"));
    }

    @Test @Order(2)
    void getMyRequests_deletedUser_returns404() {
        requestRepository.deleteAllInBatch();
        userRepository.findByEmail("employee@test.com").ifPresent(userRepository::delete);

        given()
                .header("Authorization", "Bearer " + employeeToken)
                .get("/api/requests/my-requests")
                .then()
                .statusCode(404)
                .body("error", equalTo("User not found"));
    }

    @Test @Order(3)
    void updateRequest_byNonOwner_returns403_notRuntimeException500() {
        // Regression guard: Kevine's PRs still throw RuntimeException("Not authorized")
        // in updateRequest. With PR #37's catch-all returning 500 that would break here.
        given()
                .header("Authorization", "Bearer " + otherToken)
                .contentType(ContentType.JSON)
                .body(Map.of("title", "Sneaky edit"))
                .put("/api/requests/" + requestId)
                .then()
                .statusCode(403)
                .body("error", containsString("Not authorized"));
    }

    @Test @Order(4)
    void updateStatus_openToInProgress_returns400_invalidTransition() {
        given()
                .header("Authorization", "Bearer " + agentToken)
                .contentType(ContentType.JSON)
                .body(Map.of("newStatus", "IN_PROGRESS"))
                .put("/api/requests/" + requestId + "/status")
                .then()
                .statusCode(400)
                .body("error", containsString("Invalid status transition"));
    }

    @Test @Order(5)
    void updateStatus_openToAssigned_returns200() {
        given()
                .header("Authorization", "Bearer " + agentToken)
                .contentType(ContentType.JSON)
                .body(Map.of("newStatus", "ASSIGNED"))
                .put("/api/requests/" + requestId + "/status")
                .then()
                .statusCode(200)
                .body("status", equalTo("ASSIGNED"));
    }

    @Test @Order(6)
    void register_duplicateEmail_returns409() {
        Map<String, String> reg = Map.of(
                "name", "New User", "email", "fresh@test.com",
                "password", "pass123456", "department", "IT");

        given().contentType(ContentType.JSON).body(reg)
                .post("/api/auth/register").then().statusCode(200);

        given().contentType(ContentType.JSON).body(reg)
                .post("/api/auth/register")
                .then()
                .statusCode(409)
                .body("error", equalTo("Email already in use"));
    }

    @Test @Order(7)
    void getRequests_withoutToken_returns401() {
        given().get("/api/requests").then().statusCode(401);
    }

    @Test @Order(8)
    void createRequest_blankTitle_returns400WithFieldError() {
        given()
                .header("Authorization", "Bearer " + employeeToken)
                .contentType(ContentType.JSON)
                .body(Map.of("title", "", "category", "IT_SUPPORT", "priority", "HIGH"))
                .post("/api/requests")
                .then()
                .statusCode(400)
                .body("title", notNullValue());
    }
}