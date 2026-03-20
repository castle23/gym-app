package com.gym.tracking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gym.tracking.dto.RecommendationDTO;
import com.gym.tracking.dto.RecommendationRequestDTO;
import com.gym.tracking.service.RecommendationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RecommendationController.class)
class RecommendationControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private RecommendationService recommendationService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private RecommendationDTO recommendationDTO;
    private RecommendationRequestDTO recommendationRequestDTO;
    
    @BeforeEach
    void setUp() {
        recommendationDTO = RecommendationDTO.builder()
                .id(1L)
                .trainingComponentId(1L)
                .dietComponentId(null)
                .title("Strength Training Tips")
                .description("Focus on compound movements")
                .professionalName("Coach John")
                .createdAt(LocalDateTime.now())
                .build();
        
        recommendationRequestDTO = RecommendationRequestDTO.builder()
                .trainingComponentId(1L)
                .dietComponentId(null)
                .title("Strength Training Tips")
                .description("Focus on compound movements")
                .professionalName("Coach John")
                .build();
    }
    
    // GET /api/v1/recommendations/{id} - Get by ID
    @Test
    void testGetRecommendationById_Success() throws Exception {
        when(recommendationService.getRecommendationById(1L))
                .thenReturn(recommendationDTO);
        
        mockMvc.perform(get("/api/v1/recommendations/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.trainingComponentId").value(1))
                .andExpect(jsonPath("$.title").value("Strength Training Tips"))
                .andExpect(jsonPath("$.description").value("Focus on compound movements"))
                .andExpect(jsonPath("$.professionalName").value("Coach John"))
                .andDo(print());
        
        verify(recommendationService, times(1)).getRecommendationById(1L);
    }
    
    @Test
    void testGetRecommendationById_NotFound() throws Exception {
        when(recommendationService.getRecommendationById(999L))
                .thenThrow(new IllegalArgumentException("Recommendation not found: 999"));
        
        mockMvc.perform(get("/api/v1/recommendations/999")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Recommendation not found: 999"))
                .andDo(print());
        
        verify(recommendationService, times(1)).getRecommendationById(999L);
    }
    
    // GET /api/v1/training-components/{trainingComponentId}/recommendations
    @Test
    void testGetByTrainingComponentId_Success() throws Exception {
        List<RecommendationDTO> recommendations = Arrays.asList(recommendationDTO);
        
        when(recommendationService.getRecommendationsByTrainingComponent(1L))
                .thenReturn(recommendations);
        
        mockMvc.perform(get("/api/v1/recommendations/training-component/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].trainingComponentId").value(1))
                .andExpect(jsonPath("$[0].title").value("Strength Training Tips"))
                .andDo(print());
        
        verify(recommendationService, times(1)).getRecommendationsByTrainingComponent(1L);
    }
    
    // GET /api/v1/diet-components/{dietComponentId}/recommendations
    @Test
    void testGetByDietComponentId_Success() throws Exception {
        RecommendationDTO dietRecommendation = RecommendationDTO.builder()
                .id(2L)
                .trainingComponentId(null)
                .dietComponentId(1L)
                .title("Nutrition Tips")
                .description("Increase protein intake")
                .professionalName("Dietitian Jane")
                .createdAt(LocalDateTime.now())
                .build();
        
        List<RecommendationDTO> recommendations = Arrays.asList(dietRecommendation);
        
        when(recommendationService.getRecommendationsByDietComponent(1L))
                .thenReturn(recommendations);
        
        mockMvc.perform(get("/api/v1/recommendations/diet-component/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].dietComponentId").value(1))
                .andExpect(jsonPath("$[0].title").value("Nutrition Tips"))
                .andDo(print());
        
        verify(recommendationService, times(1)).getRecommendationsByDietComponent(1L);
    }
    
    // POST /api/v1/recommendations - Create
    @Test
    void testCreateRecommendation_Success() throws Exception {
        when(recommendationService.createRecommendation(eq(1L), any(RecommendationRequestDTO.class)))
                .thenReturn(recommendationDTO);
        
        mockMvc.perform(post("/api/v1/recommendations")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(recommendationRequestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Strength Training Tips"))
                .andExpect(jsonPath("$.professionalName").value("Coach John"))
                .andDo(print());
        
        verify(recommendationService, times(1)).createRecommendation(eq(1L), any(RecommendationRequestDTO.class));
    }
    
    @Test
    void testCreateRecommendation_BadRequest() throws Exception {
        RecommendationRequestDTO invalidRequest = RecommendationRequestDTO.builder()
                .trainingComponentId(null)
                .dietComponentId(null)
                .title("Incomplete")
                .description("Missing component")
                .professionalName("Coach John")
                .build();
        
        when(recommendationService.createRecommendation(eq(1L), any(RecommendationRequestDTO.class)))
                .thenThrow(new IllegalArgumentException("At least one component (training or diet) is required"));
        
        mockMvc.perform(post("/api/v1/recommendations")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andDo(print());
        
        verify(recommendationService, times(1)).createRecommendation(eq(1L), any(RecommendationRequestDTO.class));
    }
    
    // PUT /api/v1/recommendations/{id} - Update
    @Test
    void testUpdateRecommendation_Success() throws Exception {
        RecommendationDTO updatedDTO = RecommendationDTO.builder()
                .id(1L)
                .trainingComponentId(1L)
                .dietComponentId(null)
                .title("Updated Tips")
                .description("Updated description")
                .professionalName("Coach John")
                .createdAt(LocalDateTime.now())
                .build();
        
        when(recommendationService.updateRecommendation(eq(1L), any(RecommendationRequestDTO.class)))
                .thenReturn(updatedDTO);
        
        mockMvc.perform(put("/api/v1/recommendations/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(recommendationRequestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Updated Tips"))
                .andDo(print());
        
        verify(recommendationService, times(1)).updateRecommendation(eq(1L), any(RecommendationRequestDTO.class));
    }
    
    // DELETE /api/v1/recommendations/{id} - Delete
    @Test
    void testDeleteRecommendation_Success() throws Exception {
        doNothing().when(recommendationService).deleteRecommendation(1L);
        
        mockMvc.perform(delete("/api/v1/recommendations/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent())
                .andDo(print());
        
        verify(recommendationService, times(1)).deleteRecommendation(1L);
    }
}
