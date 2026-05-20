package com.devpulsex.test;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class AuthIntegrationTest extends BaseIntegrationTest {

    @Test
    @SuppressWarnings("null")
    void registerAndLogin_shouldReturnJwtAndUser() throws Exception {
        String email = "admin@example.com";
        String password = "Admin@123";

        // Register
        String registerJson = "{" +
                "\"name\":\"Admin User\"," +
                "\"email\":\"" + email + "\"," +
                "\"password\":\"" + password + "\"" +
                "}";

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value(email))
                .andExpect(jsonPath("$.user.role").value("DEVELOPER"));

        // Login
        String loginJson = "{" +
                "\"email\":\"" + email + "\"," +
                "\"password\":\"" + password + "\"" +
                "}";

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value(email));
    }

    @Test
    @SuppressWarnings("null")
    void prepareOAuthState_shouldSetSecureCookie_whenForwardedProtoIsHttps() throws Exception {
        String state = "123456789012345678901234";
        MvcResult result = mockMvc.perform(post("/api/auth/oauth/state/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Forwarded-Proto", "https")
                        .content("{\"state\":\"" + state + "\"}"))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        String setCookie = result.getResponse().getHeader("Set-Cookie");
        assertThat(setCookie, containsString("oauth_state_google=" + state));
        assertThat(setCookie, containsString("SameSite=None"));
        assertThat(setCookie, containsString("Secure"));
    }

    @Test
    @SuppressWarnings("null")
    void googleLogin_stateMismatch_shouldClearStateCookieWithSecure_whenForwardedProtoIsHttps() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Forwarded-Proto", "https")
                        .content("{\"code\":\"code-value\",\"state\":\"state-value\"}"))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andReturn();

        String setCookie = result.getResponse().getHeader("Set-Cookie");
        assertThat(setCookie, containsString("oauth_state_google="));
        assertThat(setCookie, containsString("Max-Age=0"));
        assertThat(setCookie, containsString("SameSite=None"));
        assertThat(setCookie, containsString("Secure"));
    }

    @Test
    @SuppressWarnings("null")
    void githubLogin_stateMismatch_shouldClearStateCookieWithSecure_whenForwardedProtoIsHttps() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/github")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Forwarded-Proto", "https")
                        .content("{\"code\":\"code-value\",\"state\":\"state-value\"}"))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andReturn();

        String setCookie = result.getResponse().getHeader("Set-Cookie");
        assertThat(setCookie, containsString("oauth_state_github="));
        assertThat(setCookie, containsString("Max-Age=0"));
        assertThat(setCookie, containsString("SameSite=None"));
        assertThat(setCookie, containsString("Secure"));
    }
}
