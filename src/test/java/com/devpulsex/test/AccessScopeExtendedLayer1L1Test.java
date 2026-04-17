package com.devpulsex.test;

import com.devpulsex.model.DeploymentStatus;
import com.devpulsex.model.Role;
import com.devpulsex.repository.UserRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("Layer1")
public class AccessScopeExtendedLayer1L1Test extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    @SuppressWarnings("null")
    void shouldEnforceCrossTeamRestrictionsForCommitsAndDeployments() throws Exception {
        String suffix = String.valueOf(System.currentTimeMillis());
        String password = "Admin@123";

        String adminEmail = "admin-x-" + suffix + "@example.com";
        String managerAEmail = "manager-a-" + suffix + "@example.com";
        String devAEmail = "dev-a-x-" + suffix + "@example.com";
        String devBEmail = "dev-b-x-" + suffix + "@example.com";

        TestUtils.registerUser(mockMvc, adminEmail, password);
        TestUtils.registerUser(mockMvc, managerAEmail, password);
        TestUtils.registerUser(mockMvc, devAEmail, password);
        TestUtils.registerUser(mockMvc, devBEmail, password);

        TestUtils.promoteUser(userRepository, adminEmail, Role.ADMIN);
        TestUtils.promoteUser(userRepository, managerAEmail, Role.MANAGER);

        String adminToken = TestUtils.loginUser(mockMvc, adminEmail, password);
        String managerAToken = TestUtils.loginUser(mockMvc, managerAEmail, password);
        String devAToken = TestUtils.loginUser(mockMvc, devAEmail, password);

        Long adminId = userRepository.findByEmail(adminEmail).orElseThrow().getId();
        Long managerAId = userRepository.findByEmail(managerAEmail).orElseThrow().getId();
        Long devAId = userRepository.findByEmail(devAEmail).orElseThrow().getId();
        Long devBId = userRepository.findByEmail(devBEmail).orElseThrow().getId();

        long teamAId = createTeam(adminToken, "Team-A-X-" + suffix, adminId, managerAId, devAId);
        long teamBId = createTeam(adminToken, "Team-B-X-" + suffix, adminId, devBId);

        long projectAId = createProject(adminToken, "Project-A-X-" + suffix, teamAId);
        long projectBId = createProject(adminToken, "Project-B-X-" + suffix, teamBId);

        long commitAId = createCommit(adminToken, projectAId, devAId, "Commit in team A project");
        long commitBId = createCommit(adminToken, projectBId, devBId, "Commit in team B project");

        long deploymentAId = createDeployment(adminToken, projectAId, DeploymentStatus.PENDING.name());
        long deploymentBId = createDeployment(adminToken, projectBId, DeploymentStatus.PENDING.name());

        mockMvc.perform(get("/api/commits")
                        .header("Authorization", "Bearer " + devAToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id==" + commitAId + ")]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.id==" + commitBId + ")]").isEmpty());

        String blockedCommitJson = "{" +
                "\"projectId\":" + projectBId + "," +
                "\"userId\":" + devAId + "," +
                "\"message\":\"Cross-team commit attempt\"" +
                "}";

        mockMvc.perform(post("/api/commits")
                        .header("Authorization", "Bearer " + devAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(blockedCommitJson))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/deployments")
                        .header("Authorization", "Bearer " + managerAToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id==" + deploymentAId + ")]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.id==" + deploymentBId + ")]").isEmpty());

        String blockedDeploymentJson = "{" +
                "\"projectId\":" + projectBId + "," +
                "\"status\":\"PENDING\"" +
                "}";

        mockMvc.perform(post("/api/deployments")
                        .header("Authorization", "Bearer " + managerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(blockedDeploymentJson))
                .andExpect(status().isForbidden());
    }

    @SuppressWarnings("null")
    private long createTeam(String adminToken, String teamName, Long... memberIds) throws Exception {
        StringBuilder memberList = new StringBuilder();
        for (int i = 0; i < memberIds.length; i++) {
            if (i > 0) {
                memberList.append(',');
            }
            memberList.append(memberIds[i]);
        }

        String payload = "{" +
                "\"name\":\"" + teamName + "\"," +
                "\"memberIds\":[" + memberList + "]" +
                "}";

        String body = mockMvc.perform(post("/api/teams")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Number id = com.jayway.jsonpath.JsonPath.read(body, "$.id");
        return id.longValue();
    }

    @SuppressWarnings("null")
    private long createProject(String adminToken, String projectName, long teamId) throws Exception {
        String payload = "{" +
                "\"name\":\"" + projectName + "\"," +
                "\"teamId\":" + teamId +
                "}";

        String body = mockMvc.perform(post("/api/projects")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Number id = com.jayway.jsonpath.JsonPath.read(body, "$.id");
        return id.longValue();
    }

    @SuppressWarnings("null")
    private long createCommit(String token, long projectId, long userId, String message) throws Exception {
        String payload = "{" +
                "\"projectId\":" + projectId + "," +
                "\"userId\":" + userId + "," +
                "\"message\":\"" + message + "\"" +
                "}";

        String body = mockMvc.perform(post("/api/commits")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Number id = com.jayway.jsonpath.JsonPath.read(body, "$.id");
        return id.longValue();
    }

    @SuppressWarnings("null")
    private long createDeployment(String token, long projectId, String statusText) throws Exception {
        String payload = "{" +
                "\"projectId\":" + projectId + "," +
                "\"status\":\"" + statusText + "\"" +
                "}";

        String body = mockMvc.perform(post("/api/deployments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Number id = com.jayway.jsonpath.JsonPath.read(body, "$.id");
        return id.longValue();
    }
}
