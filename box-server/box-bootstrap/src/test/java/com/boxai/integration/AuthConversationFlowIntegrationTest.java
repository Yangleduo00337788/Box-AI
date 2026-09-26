package com.boxai.integration;

import com.boxai.common.constant.HeaderNames;
import com.boxai.common.constant.TenantTypes;
import com.boxai.common.result.Result;
import com.boxai.user.api.AuthVO;
import com.boxai.user.api.RegisterRequest;
import com.boxai.user.api.SendVerificationCodeRequest;
import com.boxai.user.api.SendVerificationCodeResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration")
class AuthConversationFlowIntegrationTest {

    @DynamicPropertySource
    static void infrastructure(DynamicPropertyRegistry registry) {
        IntegrationTestInfrastructure.registerDataSources(registry);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registerLoginCreateAgentAndConversation() throws Exception {
        String email = "flow-it-" + UUID.randomUUID() + "@example.com";
        String password = "password1";

        String code = requestVerificationCode(email);
        AuthVO auth = register(email, password, code);
        assertNotNull(auth.token());
        assertNotNull(auth.currentWorkspaceId());

        String token = auth.token();
        Long workspaceId = auth.currentWorkspaceId();

        mockMvc.perform(get("/api/v1/auth/me")
                        .header(HeaderNames.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.user.email").value(email.toLowerCase()));

        Long platformModelId = firstPlatformModelId(token);
        Long agentId = createAgent(token, workspaceId, platformModelId);
        createConversation(token, workspaceId, agentId);
    }

    private String requestVerificationCode(String email) throws Exception {
        SendVerificationCodeRequest body = new SendVerificationCodeRequest(email, "REGISTER");
        MvcResult result = mockMvc.perform(post("/api/v1/auth/verification-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();

        Result<SendVerificationCodeResponse> wrapped = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<>() {});
        assertNotNull(wrapped.data());
        assertNotNull(wrapped.data().devCode());
        return wrapped.data().devCode();
    }

    private AuthVO register(String email, String password, String code) throws Exception {
        RegisterRequest body = new RegisterRequest(
                email, password, code, "Flow IT", TenantTypes.PERSONAL, null, null);
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();

        Result<AuthVO> wrapped = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<>() {});
        return wrapped.data();
    }

    private Long firstPlatformModelId(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/platform/models")
                        .header(HeaderNames.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].id").exists())
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        return data.get(0).get("id").asLong();
    }

    private Long createAgent(String token, Long workspaceId, Long platformModelId) throws Exception {
        Map<String, Object> body = Map.of(
                "name", "Flow IT Agent",
                "platformModelId", platformModelId);
        MvcResult result = mockMvc.perform(post("/api/v1/agents")
                        .header(HeaderNames.AUTHORIZATION, bearer(token))
                        .header(HeaderNames.WORKSPACE_ID, workspaceId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").exists())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("data").get("id").asLong();
    }

    private void createConversation(String token, Long workspaceId, Long agentId) throws Exception {
        Map<String, Object> body = Map.of(
                "agentId", agentId,
                "title", "Integration chat");
        mockMvc.perform(post("/api/v1/conversations")
                        .header(HeaderNames.AUTHORIZATION, bearer(token))
                        .header(HeaderNames.WORKSPACE_ID, workspaceId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.agentId").value(agentId));
    }

    private static String bearer(String token) {
        return HeaderNames.BEARER_PREFIX + token;
    }
}
