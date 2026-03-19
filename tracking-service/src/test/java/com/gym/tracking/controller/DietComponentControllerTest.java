package com.gym.tracking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gym.tracking.dto.DietComponentDTO;
import com.gym.tracking.dto.DietComponentRequestDTO;
import com.gym.tracking.service.DietComponentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DietComponentController.class)
@ExtendWith(MockitoExtension.class)
class DietComponentControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private DietComponentService dietComponentService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private DietComponentDTO dietComponentDTO;
    private DietComponentRequestDTO dietComponentRequestDTO;
    
    @BeforeEach
    void setUp() {
        dietComponentDTO = DietComponentDTO.builder()
                .id(1L)
                .planId(1L)
                .dietType("Balanced")
                .dailyCalories(2000)
                .macroDistribution("50-30-20")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        dietComponentRequestDTO = DietComponentRequestDTO.builder()
                .planId(1L)
                .dietType("Balanced")
                .dailyCalories(2000)
                .macroDistribution("50-30-20")
                .build();
    }
    
    // GET /api/v1/diet-components/{id} - Get by ID
    @Test
    void testGetDietComponentById_Success() throws Exception {
        when(dietComponentService.getDietComponentById(1L, 1L))
                .thenReturn(dietComponentDTO);
        
        mockMvc.perform(get("/api/v1/diet-components/1")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.planId").value(1))
                .andExpect(jsonPath("$.dietType").value("Balanced"))
                .andExpect(jsonPath("$.dailyCalories").value(2000))
                .andDo(print());
        
        verify(dietComponentService, times(1)).getDietComponentById(1L, 1L);
    }
    
    @Test
    void testGetDietComponentById_NotFound() throws Exception {
        when(dietComponentService.getDietComponentById(999L, 1L))
                .thenThrow(new IllegalArgumentException("Diet component not found: 999"));
        
        mockMvc.perform(get("/api/v1/diet-components/999")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Diet component not found: 999"))
                .andDo(print());
    }
    
    @Test
    void testGetDietComponentById_Unauthorized() throws Exception {
        when(dietComponentService.getDietComponentById(1L, 2L))
                .thenThrow(new IllegalArgumentException("User is not authorized to access this resource"));
        
        mockMvc.perform(get("/api/v1/diet-components/1")
                .header("X-User-Id", "2")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("FORBIDDEN"))
                .andDo(print());
    }
    
    // GET /api/v1/plans/{planId}/diet-component - Get by plan ID
    @Test
    void testGetByPlanId_Success() throws Exception {
        when(dietComponentService.getDietComponentByPlanId(1L, 1L))
                .thenReturn(dietComponentDTO);
        
        mockMvc.perform(get("/api/v1/plans/1/diet-component")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.planId").value(1))
                .andExpect(jsonPath("$.dietType").value("Balanced"))
                .andDo(print());
        
        verify(dietComponentService, times(1)).getDietComponentByPlanId(1L, 1L);
    }
    
    // POST /api/v1/diet-components - Create
    @Test
    void testCreateDietComponent_Success() throws Exception {
        when(dietComponentService.createDietComponent(1L, dietComponentRequestDTO))
                .thenReturn(dietComponentDTO);
        
        mockMvc.perform(post("/api/v1/diet-components")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dietComponentRequestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.planId").value(1))
                .andExpect(jsonPath("$.dietType").value("Balanced"))
                .andDo(print());
        
        verify(dietComponentService, times(1)).createDietComponent(1L, dietComponentRequestDTO);
    }
    
    @Test
    void testCreateDietComponent_BadRequest() throws Exception {
        when(dietComponentService.createDietComponent(1L, dietComponentRequestDTO))
                .thenThrow(new IllegalArgumentException("Plan not found or unauthorized"));
        
        mockMvc.perform(post("/api/v1/diet-components")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dietComponentRequestDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andDo(print());
    }
    
    // PUT /api/v1/diet-components/{id} - Update
    @Test
    void testUpdateDietComponent_Success() throws Exception {
        when(dietComponentService.updateDietComponent(1L, 1L, dietComponentRequestDTO))
                .thenReturn(dietComponentDTO);
        
        mockMvc.perform(put("/api/v1/diet-components/1")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dietComponentRequestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andDo(print());
        
        verify(dietComponentService, times(1)).updateDietComponent(1L, 1L, dietComponentRequestDTO);
    }
    
    @Test
    void testUpdateDietComponent_Unauthorized() throws Exception {
        when(dietComponentService.updateDietComponent(1L, 2L, dietComponentRequestDTO))
                .thenThrow(new IllegalArgumentException("User is not authorized to access this resource"));
        
        mockMvc.perform(put("/api/v1/diet-components/1")
                .header("X-User-Id", "2")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dietComponentRequestDTO)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("FORBIDDEN"))
                .andDo(print());
    }
    
    // DELETE /api/v1/diet-components/{id} - Delete
    @Test
    void testDeleteDietComponent_Success() throws Exception {
        doNothing().when(dietComponentService).deleteDietComponent(1L, 1L);
        
        mockMvc.perform(delete("/api/v1/diet-components/1")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent())
                .andDo(print());
        
        verify(dietComponentService, times(1)).deleteDietComponent(1L, 1L);
    }
}
