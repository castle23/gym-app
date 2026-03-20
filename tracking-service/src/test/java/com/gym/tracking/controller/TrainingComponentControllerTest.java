package com.gym.tracking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gym.tracking.dto.TrainingComponentDTO;
import com.gym.tracking.dto.TrainingComponentRequestDTO;
import com.gym.tracking.service.TrainingComponentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import com.gym.tracking.config.TestSecurityConfig;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TrainingComponentController.class)
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
class TrainingComponentControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private TrainingComponentService trainingComponentService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private TrainingComponentDTO trainingComponentDTO;
    private TrainingComponentRequestDTO trainingComponentRequestDTO;
    
    @BeforeEach
    void setUp() {
        trainingComponentDTO = TrainingComponentDTO.builder()
                .id(1L)
                .planId(1L)
                .focus("Strength")
                .intensity("High")
                .frequencyPerWeek(4)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        trainingComponentRequestDTO = TrainingComponentRequestDTO.builder()
                .planId(1L)
                .focus("Strength")
                .intensity("High")
                .frequencyPerWeek(4)
                .build();
    }
    
    // GET /api/v1/training-components/{id} - Get by ID
    @Test
    void testGetTrainingComponentById_Success() throws Exception {
        when(trainingComponentService.getTrainingComponentById(1L, 1L))
                .thenReturn(trainingComponentDTO);
        
        mockMvc.perform(get("/api/v1/training-components/1")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.planId").value(1))
                .andExpect(jsonPath("$.focus").value("Strength"))
                .andExpect(jsonPath("$.intensity").value("High"))
                .andDo(print());
        
        verify(trainingComponentService, times(1)).getTrainingComponentById(1L, 1L);
    }
    
    @Test
    void testGetTrainingComponentById_NotFound() throws Exception {
        when(trainingComponentService.getTrainingComponentById(999L, 1L))
                .thenThrow(new IllegalArgumentException("Training component not found: 999"));
        
        mockMvc.perform(get("/api/v1/training-components/999")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Training component not found: 999"))
                .andDo(print());
    }
    
    @Test
    void testGetTrainingComponentById_Unauthorized() throws Exception {
        when(trainingComponentService.getTrainingComponentById(1L, 2L))
                .thenThrow(new IllegalArgumentException("User is not authorized to access this resource"));
        
        mockMvc.perform(get("/api/v1/training-components/1")
                .header("X-User-Id", "2")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("FORBIDDEN"))
                .andDo(print());
    }
    
    // GET /api/v1/plans/{planId}/training-component - Get by plan ID
    @Test
    void testGetByPlanId_Success() throws Exception {
        when(trainingComponentService.getTrainingComponentByPlanId(1L, 1L))
                .thenReturn(trainingComponentDTO);
        
        mockMvc.perform(get("/api/v1/plans/1/training-component")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.planId").value(1))
                .andExpect(jsonPath("$.focus").value("Strength"))
                .andDo(print());
        
        verify(trainingComponentService, times(1)).getTrainingComponentByPlanId(1L, 1L);
    }
    
    // POST /api/v1/training-components - Create
    @Test
    void testCreateTrainingComponent_Success() throws Exception {
        when(trainingComponentService.createTrainingComponent(1L, trainingComponentRequestDTO))
                .thenReturn(trainingComponentDTO);
        
        mockMvc.perform(post("/api/v1/training-components")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(trainingComponentRequestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.planId").value(1))
                .andExpect(jsonPath("$.focus").value("Strength"))
                .andDo(print());
        
        verify(trainingComponentService, times(1)).createTrainingComponent(1L, trainingComponentRequestDTO);
    }
    
    @Test
    void testCreateTrainingComponent_BadRequest() throws Exception {
        when(trainingComponentService.createTrainingComponent(1L, trainingComponentRequestDTO))
                .thenThrow(new IllegalArgumentException("Plan not found or unauthorized"));
        
        mockMvc.perform(post("/api/v1/training-components")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(trainingComponentRequestDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andDo(print());
    }
    
    // PUT /api/v1/training-components/{id} - Update
    @Test
    void testUpdateTrainingComponent_Success() throws Exception {
        when(trainingComponentService.updateTrainingComponent(1L, 1L, trainingComponentRequestDTO))
                .thenReturn(trainingComponentDTO);
        
        mockMvc.perform(put("/api/v1/training-components/1")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(trainingComponentRequestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andDo(print());
        
        verify(trainingComponentService, times(1)).updateTrainingComponent(1L, 1L, trainingComponentRequestDTO);
    }
    
    @Test
    void testUpdateTrainingComponent_Unauthorized() throws Exception {
        when(trainingComponentService.updateTrainingComponent(1L, 2L, trainingComponentRequestDTO))
                .thenThrow(new IllegalArgumentException("User is not authorized to access this resource"));
        
        mockMvc.perform(put("/api/v1/training-components/1")
                .header("X-User-Id", "2")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(trainingComponentRequestDTO)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("FORBIDDEN"))
                .andDo(print());
    }
    
    // DELETE /api/v1/training-components/{id} - Delete
    @Test
    void testDeleteTrainingComponent_Success() throws Exception {
        doNothing().when(trainingComponentService).deleteTrainingComponent(1L, 1L);
        
        mockMvc.perform(delete("/api/v1/training-components/1")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent())
                .andDo(print());
        
        verify(trainingComponentService, times(1)).deleteTrainingComponent(1L, 1L);
    }
}
