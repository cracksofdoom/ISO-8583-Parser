package net.cracksofdoom.parser.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class IsoApiIntegrationTest {
    @Autowired
    MockMvc mvc;
    private static final String PAN_MESSAGE = "02004000000000000000161234567890123456";
    private static final String ECHO = "0800822000000000000004000000000000001125210025123456301";

    @Test
    void publicParseMasksPan() throws Exception {
        mvc.perform(post("/api/v1/messages/parse").contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"" + PAN_MESSAGE + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fields['2'].value").value("123456••••••3456"))
                .andExpect(header().exists("Content-Security-Policy"));
    }

    @Test
    void sensitiveRevealIsRejected() throws Exception {
        mvc.perform(post("/api/v1/messages/parse").contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"" + PAN_MESSAGE + "\",\"revealSensitive\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void compareAndValidation() throws Exception {
        String frame = "{\"message\":\"" + ECHO + "\"}";
        mvc.perform(post("/api/v1/messages/compare").contentType(MediaType.APPLICATION_JSON)
                .content("{\"left\":" + frame + ",\"right\":" + frame + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mtiStatus").value("MATCH"));
        mvc.perform(post("/api/v1/messages/parse").contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.field").value("message"));
    }

    @Test
    void onlyPublishedRoutesAreAccessible() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mvc.perform(get("/actuator/env")).andExpect(status().isForbidden());
    }
}
