package com.bank.management.employee.controller;

import com.bank.management.auth.entity.Session;
import com.bank.management.auth.enums.SessionTerminationReason;
import com.bank.management.auth.repository.SessionRepository;
import com.bank.management.auth.enums.SessionStatus;
import com.bank.management.auth.service.JwtService;
import org.springframework.http.HttpHeaders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.bank.management.branch.entity.Branch;
import com.bank.management.branch.repository.BranchRepository;
import com.bank.management.common.security.EmployeeUserDetails;
import com.bank.management.employee.entity.Employee;
import com.bank.management.employee.enums.EmployeeRole;
import com.bank.management.employee.enums.EmployeeStatus;
import com.bank.management.employee.enums.Gender;
import com.bank.management.employee.repository.EmployeeRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EmployeeControllerResourceAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private JwtService jwtService;

    @Test
    void managerCanAccessTheirOwnStaff() throws Exception {
        TestEmployees fixture = createTestEmployees();

        mockMvc.perform(get("/api/employees/{id}", fixture.staffA().getId())
                        .with(user(new EmployeeUserDetails(fixture.managerA()))))
                .andExpect(status().isOk());
    }

    @Test
    void managerCannotAccessAnotherManagersStaffByChangingUrlId()
            throws Exception {
        TestEmployees fixture = createTestEmployees();

        mockMvc.perform(get("/api/employees/{id}", fixture.staffB().getId())
                        .with(user(new EmployeeUserDetails(fixture.managerA()))))
                .andExpect(status().isForbidden());
    }

    @Test
void managerCannotReassignStaffThroughAssignmentEndpoint()
        throws Exception {
    TestEmployees fixture = createTestEmployees();

    mockMvc.perform(patch(
                    "/api/employees/{id}/assignment",
                    fixture.staffA().getId())
                    .with(user(new EmployeeUserDetails(fixture.managerA())))
                    .contentType("application/json")
                    .content("""
                            {
                              "branchId": 1,
                              "reportingManagerId": 1
                            }
                            """))
            .andExpect(status().isForbidden());
}

@Test
void validJwtCanAccessOwnStaffThroughRealAuthenticationFilter()
        throws Exception {
    TestEmployees fixture = createTestEmployees();
    String token = createAccessToken(fixture.managerA());

    mockMvc.perform(get(
                    "/api/employees/{id}",
                    fixture.staffA().getId())
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());
}

@Test
void validJwtCannotAccessAnotherManagersStaffByChangingUrlId()
        throws Exception {
    TestEmployees fixture = createTestEmployees();
    String token = createAccessToken(fixture.managerA());

    mockMvc.perform(get(
                    "/api/employees/{id}",
                    fixture.staffB().getId())
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isForbidden());
}

@Test
void jwtWithRoleMismatchCannotAuthenticate()
        throws Exception {
    TestEmployees fixture = createTestEmployees();
    Employee manager = fixture.managerA();

    Session session = new Session(
            manager,
            LocalDateTime.now(),
            null,
            null
    );

    session = sessionRepository.saveAndFlush(session);

    String token = jwtService.generateAccessToken(
            manager.getId(),
            EmployeeRole.STAFF.name(),
            session.getSessionId()
    );

    mockMvc.perform(get("/api/employees")
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isUnauthorized());
}

@Test
void validJwtWithRevokedSessionCannotAuthenticate() throws Exception {
    TestEmployees fixture = createTestEmployees();
    Employee employee = fixture.staffA();

    Session session = new Session(
            employee,
            LocalDateTime.now(),
            "test-refresh-token-hash",
            LocalDateTime.now().plusDays(1)
    );

    sessionRepository.saveAndFlush(session);

    // Revoke the server-side session while the access token still exists.
    session.endSession(
            LocalDateTime.now(),
            SessionStatus.REVOKED,
            SessionTerminationReason.REVOKED
    );

    sessionRepository.saveAndFlush(session);

    String token = jwtService.generateAccessToken(
            employee.getId(),
            employee.getRole().name(),
            session.getSessionId()
    );

    mockMvc.perform(get("/api/employees/" + employee.getId())
                    .header(
                            HttpHeaders.AUTHORIZATION,
                            "Bearer " + token
                    ))
            .andExpect(status().isUnauthorized());
}

@Test
void managerCannotUpdateAnotherManagersStaffByChangingUrlId()
        throws Exception {

    TestEmployees fixture = createTestEmployees();
    Employee staffB = fixture.staffB();

    mockMvc.perform(put("/api/employees/{id}", staffB.getId())
                    .with(user(new EmployeeUserDetails(fixture.managerA())))
                    .contentType("application/json")
                    .content("""
                            {
                              "employeeId": "AUTH-TEST-UPDATE",
                              "email": "auth-test-update@example.com",
                              "fullName": "Unauthorized Name Change",
                              "dateOfBirth": "1995-01-15",
                              "identityProofNumber": "987654321012",
                              "phoneNumber": "+91 9876543210",
                              "gender": "MALE",
                              "dateOfJoining": "2020-01-01",
                              "monthlySalary": 50000.00
                            }
                            """))
            .andExpect(status().isForbidden());

    Employee unchangedStaff = employeeRepository.findById(staffB.getId())
            .orElseThrow();

    org.junit.jupiter.api.Assertions.assertNotEquals(
            "Unauthorized Name Change",
            unchangedStaff.getFullName()
    );
}

@Test
void managerCannotChangeStatusOfAnotherManagersStaff()
        throws Exception {

    TestEmployees fixture = createTestEmployees();
    Employee staffB = fixture.staffB();

    mockMvc.perform(patch(
                    "/api/employees/{id}/status",
                    staffB.getId())
                    .with(user(new EmployeeUserDetails(fixture.managerA())))
                    .contentType("application/json")
                    .content("""
                            {
                              "status": "INACTIVE"
                            }
                            """))
            .andExpect(status().isForbidden());

    Employee unchangedStaff = employeeRepository.findById(staffB.getId())
            .orElseThrow();

    org.junit.jupiter.api.Assertions.assertEquals(
            EmployeeStatus.ACTIVE,
            unchangedStaff.getStatus()
    );
}

private String createAccessToken(Employee employee) {
    Session session = new Session(
            employee,
            LocalDateTime.now(),
            null,
            null
    );

    session = sessionRepository.saveAndFlush(session);

    return jwtService.generateAccessToken(
            employee.getId(),
            employee.getRole().name(),
            session.getSessionId()
    );
}

    private TestEmployees createTestEmployees() {
        String suffix = Long.toString(System.nanoTime());

        Branch branchA = branchRepository.save(new Branch(
                "A" + suffix,
                "Test Branch A",
                "Test Location A",
                LocalDateTime.now()
        ));

        Branch branchB = branchRepository.save(new Branch(
                "B" + suffix,
                "Test Branch B",
                "Test Location B",
                LocalDateTime.now()
        ));

        Employee managerA = employeeRepository.save(new Employee(
                "MA" + suffix,
                null,
                "test-password-hash",
                "Test Manager A",
                LocalDate.of(1990, 1, 1),
                Long.toString(System.nanoTime()),
                "9876543210",
                Gender.MALE,
                LocalDate.of(2020, 1, 1),
                new BigDecimal("50000.00"),
                EmployeeRole.MANAGER,
                EmployeeStatus.ACTIVE,
                branchA,
                null
        ));

        Employee managerB = employeeRepository.save(new Employee(
                "MB" + suffix,
                null,
                "test-password-hash",
                "Test Manager B",
                LocalDate.of(1991, 1, 1),
                Long.toString(System.nanoTime()),
                "9876543211",
                Gender.MALE,
                LocalDate.of(2020, 1, 1),
                new BigDecimal("50000.00"),
                EmployeeRole.MANAGER,
                EmployeeStatus.ACTIVE,
                branchB,
                null
        ));

        Employee staffA = employeeRepository.save(new Employee(
                "SA" + suffix,
                null,
                "test-password-hash",
                "Test Staff A",
                LocalDate.of(1995, 1, 1),
                Long.toString(System.nanoTime()),
                "9876543212",
                Gender.MALE,
                LocalDate.of(2021, 1, 1),
                new BigDecimal("25000.00"),
                EmployeeRole.STAFF,
                EmployeeStatus.ACTIVE,
                branchA,
                managerA
        ));

        Employee staffB = employeeRepository.save(new Employee(
                "SB" + suffix,
                null,
                "test-password-hash",
                "Test Staff B",
                LocalDate.of(1996, 1, 1),
                Long.toString(System.nanoTime()),
                "9876543213",
                Gender.MALE,
                LocalDate.of(2021, 1, 1),
                new BigDecimal("25000.00"),
                EmployeeRole.STAFF,
                EmployeeStatus.ACTIVE,
                branchB,
                managerB
        ));

        return new TestEmployees(managerA, staffA, staffB);
    }

    private record TestEmployees(
            Employee managerA,
            Employee staffA,
            Employee staffB
    ) {
    }
}
