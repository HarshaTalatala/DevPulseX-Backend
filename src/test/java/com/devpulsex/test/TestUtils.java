package com.devpulsex.test;

import com.devpulsex.model.Role;
import com.devpulsex.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Objects;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class TestUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static void registerUser(MockMvc mockMvc, String email, String password) throws Exception {
        String registerJson = "{" +
                "\"name\":\"Test User\"," +
                "\"email\":\"" + email + "\"," +
                "\"password\":\"" + password + "\"" +
                "}";

        mockMvc.perform(post("/api/auth/register")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content(registerJson))
                .andExpect(status().isOk());
    }

    public static String loginUser(MockMvc mockMvc, String email, String password) throws Exception {
        String loginJson = "{" +
                "\"email\":\"" + email + "\"," +
                "\"password\":\"" + password + "\"" +
                "}";

        ResultActions loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content(loginJson))
                .andExpect(status().isOk());

        String response = loginResult.andReturn().getResponse().getContentAsString();
        JsonNode root = OBJECT_MAPPER.readTree(response);
        return root.path("token").asText();
    }

    public static void promoteUser(UserRepository userRepository, String email, Role role) {
        userRepository.findByEmail(email).ifPresent(user -> {
            user.setRole(role);
            userRepository.save(user);
        });
    }

    public static String registerAndLoginAdmin(MockMvc mockMvc, UserRepository userRepository, String email) throws Exception {
        String password = "Admin@123";

        registerUser(mockMvc, email, password);
        promoteUser(userRepository, email, Role.ADMIN);
        return loginUser(mockMvc, email, password);
    }
}
