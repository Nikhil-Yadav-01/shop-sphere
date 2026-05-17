package com.rudraksha.shopsphere.auth.chaos;

import com.rudraksha.shopsphere.auth.controller.WelcomeController;
import com.rudraksha.shopsphere.auth.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = WelcomeController.class)
@ContextConfiguration(classes = WelcomeController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
public class AuthChaosMonkeyResilienceTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testGlobalExceptionHandlerResilienceDuringChaosException() throws Exception {
        // We verify that if an unhandled Exception (like a Chaos Monkey assault) occurs,
        // the GlobalExceptionHandler catches it and returns a clean 500 error response instead of crashing.
        mockMvc.perform(get("/")
                        .param("throwErrorForChaosMonkeyTest", "true"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
    }
}
