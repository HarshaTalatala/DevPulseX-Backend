package com.devpulsex.test;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class UserEndpointIntegrationTest extends BaseIntegrationTest {

    @Test
    void getUsers_withoutAuth_shouldReturn403() throws Exception {
        mockMvc.perform(get("/api/users")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    void getUsers_withAdminToken_shouldReturn200() throws Exception {
        String token = TestUtils.registerAndLoginAdmin(mockMvc, "admin-users@example.com");
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk());
    }
}
