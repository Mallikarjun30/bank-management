package com.bank.management.employee.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EmployeeControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void unauthenticatedUserCannotListEmployees() throws Exception {
        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void staffCannotReassignEmployee() throws Exception {
        mockMvc.perform(patch("/api/employees/100/assignment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "branchId": 1,
                                  "reportingManagerId": 2
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void managerCannotReassignEmployee() throws Exception {
        mockMvc.perform(patch("/api/employees/100/assignment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "branchId": 1,
                                  "reportingManagerId": 2
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidBearerTokenCannotAccessEmployees() throws Exception {
        mockMvc.perform(get("/api/employees")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void staffCannotCreateManager() throws Exception {
        mockMvc.perform(post("/api/employees/managers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "employeeId": "MGR-TEST-001",
                                  "email": "manager.test@example.com",
                                  "initialPassword": "TestPassword123",
                                  "fullName": "Test Manager",
                                  "dateOfBirth": "1995-05-15",
                                  "identityProofNumber": "123456789012",
                                  "phoneNumber": "9876543210",
                                  "gender": "MALE",
                                  "dateOfJoining": "2026-01-01",
                                  "monthlySalary": 45000.00,
                                  "branchId": 1
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void staffCannotUpdateEmployee() throws Exception {
        mockMvc.perform(
                org.springframework.test.web.servlet.request
                        .MockMvcRequestBuilders.put("/api/employees/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "employeeId": "EMP-100",
                                  "email": "test@example.com",
                                  "fullName": "Unauthorized Update",
                                  "dateOfBirth": "1995-05-15",
                                  "identityProofNumber": "123456789012",
                                  "phoneNumber": "9876543210",
                                  "gender": "MALE",
                                  "dateOfJoining": "2025-01-01",
                                  "monthlySalary": 25000.00
                                }
                                """))
                .andExpect(status().isForbidden());
    }
}
