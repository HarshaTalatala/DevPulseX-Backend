package com.devpulsex.test;

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
public class AccessScopeLayer1L1Test extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    @SuppressWarnings("null")
    void shouldRestrictDeveloperAccessToOnlyOwnTeamProjects() throws Exception {
        String suffix = String.valueOf(System.currentTimeMillis());

        String adminEmail = "admin-" + suffix + "@example.com";
        String devAEmail = "dev-a-" + suffix + "@example.com";
        String devBEmail = "dev-b-" + suffix + "@example.com";
        String password = "Admin@123";

        TestUtils.registerUser(mockMvc, adminEmail, password);
        TestUtils.registerUser(mockMvc, devAEmail, password);
        TestUtils.registerUser(mockMvc, devBEmail, password);

        TestUtils.promoteUser(userRepository, adminEmail, Role.ADMIN);

        String adminToken = TestUtils.loginUser(mockMvc, adminEmail, password);
        String devAToken = TestUtils.loginUser(mockMvc, devAEmail, password);

        Long adminId = userRepository.findByEmail(adminEmail).orElseThrow().getId();
        Long devAId = userRepository.findByEmail(devAEmail).orElseThrow().getId();
        Long devBId = userRepository.findByEmail(devBEmail).orElseThrow().getId();

        long teamAId = createTeam(adminToken, "Team-A-" + suffix, adminId, devAId);
        long teamBId = createTeam(adminToken, "Team-B-" + suffix, adminId, devBId);

        long projectAId = createProject(adminToken, "Project-A-" + suffix, teamAId);
        long projectBId = createProject(adminToken, "Project-B-" + suffix, teamBId);

        long taskInProjectB = createTask(adminToken, projectBId, "Task-B-" + suffix);

        mockMvc.perform(get("/api/projects")
                        .header("Authorization", "Bearer " + devAToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id==" + projectAId + ")]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.id==" + projectBId + ")]").isEmpty());

        mockMvc.perform(get("/api/projects/" + projectBId)
                        .header("Authorization", "Bearer " + devAToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        String blockedIssueJson = "{" +
                "\"projectId\":" + projectBId + "," +
                "\"userId\":" + devAId + "," +
                "\"description\":\"Cross-team issue attempt\"," +
                "\"status\":\"OPEN\"" +
                "}";

        mockMvc.perform(post("/api/issues")
                        .header("Authorization", "Bearer " + devAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(blockedIssueJson))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/tasks/" + taskInProjectB + "/status")
                        .header("Authorization", "Bearer " + devAToken)
                        .param("status", "IN_PROGRESS")
                        .contentType(MediaType.APPLICATION_JSON))
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
    private long createTask(String adminToken, long projectId, String title) throws Exception {
        String payload = "{" +
                "\"title\":\"" + title + "\"," +
                "\"description\":\"Scope validation task\"," +
                "\"projectId\":" + projectId + "," +
                "\"status\":\"TODO\"" +
                "}";

        String body = mockMvc.perform(post("/api/tasks")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

                Number id = com.jayway.jsonpath.JsonPath.read(body, "$.id");
                return id.longValue();
    }
}
