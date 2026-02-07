package com.taskvantage.backend.controller;

import com.taskvantage.backend.Security.AuthorizationUtil;
import com.taskvantage.backend.Security.JwtFilter;
import com.taskvantage.backend.Security.JwtUtil;
import com.taskvantage.backend.dto.RecommendationResponse;
import com.taskvantage.backend.model.Task;
import com.taskvantage.backend.service.CustomUserDetailsService;
import com.taskvantage.backend.service.RecommendationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = RecommendationController.class,
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {JwtFilter.class})
        })
@AutoConfigureMockMvc(addFilters = false)
public class RecommendationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RecommendationService recommendationService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private AuthorizationUtil authorizationUtil;

    @BeforeEach
    public void setup() {
        // Create mock tasks
        Task recommendedTask1 = new Task(1L, "Write documentation", "Complete the user documentation");
        Task recommendedTask2 = new Task(2L, "Create slides", "Prepare slides for presentation");
        List<Task> mockRecommendations = Arrays.asList(recommendedTask1, recommendedTask2);

        // Create response
        RecommendationResponse response = new RecommendationResponse();
        response.setRecommendations(mockRecommendations);
        response.setStatus("success");
        response.setMessage("Recommendations fetched successfully.");

        // Mock the service method
        when(recommendationService.getRecommendedTasks(anyLong(), anyLong(), anyInt()))
                .thenReturn(response);
    }

    @Test
    @WithMockUser
    public void testGetRecommendations() throws Exception {
        mockMvc.perform(get("/api/recommendations/user/1/task/1")
                        .header("Authorization", "Bearer mock-jwt-token")
                        .param("limit", "3")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.recommendations").isArray())
                .andExpect(jsonPath("$.recommendations[0].title").value("Write documentation"))
                .andExpect(jsonPath("$.recommendations[1].title").value("Create slides"))
                .andExpect(jsonPath("$.message").value("Recommendations fetched successfully."))
                .andExpect(jsonPath("$.status").value("success"));
    }
}