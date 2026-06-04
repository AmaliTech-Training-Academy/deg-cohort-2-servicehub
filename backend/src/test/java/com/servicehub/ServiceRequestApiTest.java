package com.servicehub;

import com.servicehub.model.User;
import com.servicehub.model.enums.Role;
import com.servicehub.repository.ServiceRequestRepository;
import com.servicehub.repository.UserRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ServiceRequestApiTest {

    @LocalServerPort int port;

    @Autowired UserRepository userRepository;
    @Autowired ServiceRequestRepository requestRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private String managerToken;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        requestRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();

        userRepository.save(User.builder()
                .email("manager@amalitech.com").fullName("System Manager")
                .password(passwordEncoder.encode("password123")).role(Role.MANAGER).build());
        userRepository.save(User.builder()
                .email("agent@amalitech.com").fullName("Support Agent")
                .password(passwordEncoder.encode("password123")).role(Role.AGENT).build());
        userRepository.save(User.builder()
                .email("user@amalitech.com").fullName("Test User")
                .password(passwordEncoder.encode("password123")).role(Role.EMPLOYEE).build());

        managerToken = loginToken("manager@amalitech.com", "password123");
    }

    // ── Auth — login contracts ────────────────────────────────────────────────

    @Test
    void managerLogin_returnsTokenWithManagerRole() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("email", "manager@amalitech.com", "password", "password123"))
                .post("/api/auth/login")
                .then()
                .statusCode(200)
                .body("token", notNullValue())
                .body("role", equalTo("MANAGER"))
                .body("email", equalTo("manager@amalitech.com"));
    }

    @Test
    void agentLogin_returnsTokenWithAgentRole() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("email", "agent@amalitech.com", "password", "password123"))
                .post("/api/auth/login")
                .then()
                .statusCode(200)
                .body("token", notNullValue())
                .body("role", equalTo("AGENT"));
    }

    @Test
    void employeeLogin_returnsTokenWithEmployeeRole() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("email", "user@amalitech.com", "password", "password123"))
                .post("/api/auth/login")
                .then()
                .statusCode(200)
                .body("token", notNullValue())
                .body("role", equalTo("EMPLOYEE"));
    }

    // ── Auth — register contracts ─────────────────────────────────────────────

    @Test
    void register_newEmployee_returnsTokenAndEmployeeRole() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("name", "New Employee", "email", "new@amalitech.com",
                        "password", "password123", "department", "IT"))
                .post("/api/auth/register")
                .then()
                .statusCode(200)
                .body("token", notNullValue())
                .body("role", equalTo("EMPLOYEE"))
                .body("email", equalTo("new@amalitech.com"));
    }

    @Test
    void register_duplicateEmail_returns409() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("name", "Dup", "email", "manager@amalitech.com",
                        "password", "password123", "department", "IT"))
                .post("/api/auth/register")
                .then()
                .statusCode(409)
                .body("error", equalTo("Email already in use"));
    }

    // ── Auth guard — service requests ─────────────────────────────────────────

    @Test
    void getRequests_withValidManagerToken_returns200() {
        given()
                .header("Authorization", "Bearer " + managerToken)
                .get("/api/requests")
                .then()
                .statusCode(200);
    }

    @Test
    void getRequests_withoutToken_returns401() {
        given()
                .get("/api/requests")
                .then()
                .statusCode(401);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private String loginToken(String email, String password) {
        return given()
                .contentType(ContentType.JSON)
                .body(Map.of("email", email, "password", password))
                .post("/api/auth/login")
                .then().statusCode(200)
                .extract().path("token");
    }
}
