package com.bank.management.branch.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BranchControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String VALID_BRANCH_JSON = """
            {
              "branchId": "BR-SEC-001",
              "branchName": "Security Test Branch",
              "location": "Bengaluru"
            }
            """;

    @Test
    void unauthenticatedUserCannotListBranches() throws Exception {
        mockMvc.perform(get("/api/branches"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticatedUserCannotCreateBranch() throws Exception {
        mockMvc.perform(post("/api/branches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BRANCH_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void managerCannotListBranches() throws Exception {
        mockMvc.perform(get("/api/branches"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void staffCannotListBranches() throws Exception {
        mockMvc.perform(get("/api/branches"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void managerCannotCreateBranch() throws Exception {
        mockMvc.perform(post("/api/branches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BRANCH_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void staffCannotCreateBranch() throws Exception {
        mockMvc.perform(post("/api/branches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BRANCH_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void managerCannotViewBranchById() throws Exception {
        mockMvc.perform(get("/api/branches/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void staffCannotViewBranchById() throws Exception {
        mockMvc.perform(get("/api/branches/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void managerCannotUpdateBranch() throws Exception {
        mockMvc.perform(put("/api/branches/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BRANCH_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void staffCannotUpdateBranch() throws Exception {
        mockMvc.perform(put("/api/branches/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BRANCH_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void managerCannotDeleteBranch() throws Exception {
        mockMvc.perform(delete("/api/branches/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void staffCannotDeleteBranch() throws Exception {
        mockMvc.perform(delete("/api/branches/1"))
                .andExpect(status().isForbidden());
    }
}
