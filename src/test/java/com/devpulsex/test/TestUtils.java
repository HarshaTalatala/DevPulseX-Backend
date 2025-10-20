package com.devpulsex.test;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class TestUtils {

    public static String registerAndLoginAdmin(MockMvc mockMvc, String email) throws Exception {
        String password = "Admin@123";
        // Register
        String registerJson = "{" +
                "\"name\":\"Admin User\"," +
                "\"email\":\"" + email + "\"," +
                "\"password\":\"" + password + "\"," +
                "\"role\":\"ADMIN\"" +
                "}";
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isOk());

        // Login
        String loginJson = "{" +
                "\"email\":\"" + email + "\"," +
                "\"password\":\"" + password + "\"" +
                "}";
        ResultActions loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson))
                .andExpect(status().isOk());

        String response = loginResult.andReturn().getResponse().getContentAsString();
        // naive parse token from JSON: {"token":"...",
        int tokenIdx = response.indexOf("\"token\":");
        if (tokenIdx < 0) throw new IllegalStateException("Token not found in response: " + response);
        int colon = response.indexOf(':', tokenIdx);
        int startQuote = response.indexOf('"', colon + 1);
        int endQuote = response.indexOf('"', startQuote + 1);
        return response.substring(startQuote + 1, endQuote);
    }
}
