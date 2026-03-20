package com.gym.tracking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gym.tracking.dto.PlanDTO;
import com.gym.tracking.dto.PlanRequestDTO;
import com.gym.tracking.entity.Plan;
import com.gym.tracking.service.PlanService;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PlanController.class)
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
class PlanControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private PlanService planService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private PlanDTO planDTO;
    private PlanRequestDTO planRequestDTO;
    
    @BeforeEach
    void setUp() {
        planDTO = PlanDTO.builder()
                .id(1L)
                .userId(1L)
                .name("Summer Fitness Plan")
                .description("Get fit for summer")
                .status(Plan.PlanStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();
        
        planRequestDTO = PlanRequestDTO.builder()
                .name("Summer Fitness Plan")
                .description("Get fit for summer")
                .objectiveId(1L)
                .status("ACTIVE")
                .startDate(LocalDateTime.now())
                .build();
    }
    
    // GET /api/v1/plans - Get user plans
    @Test
    void testGetUserPlans_Success() throws Exception {
        when(planService.getUserPlans(1L))
                .thenReturn(List.of(planDTO));
        
        mockMvc.perform(get("/api/v1/plans")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Summer Fitness Plan"))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andDo(print());
        
        verify(planService, times(1)).getUserPlans(1L);
    }
    
    // GET /api/v1/plans/{id} - Get plan by ID
    @Test
    void testGetPlanById_Success() throws Exception {
        when(planService.getPlanById(1L, 1L))
                .thenReturn(planDTO);
        
        mockMvc.perform(get("/api/v1/plans/1")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Summer Fitness Plan"))
                .andDo(print());
        
        verify(planService, times(1)).getPlanById(1L, 1L);
    }
    
    @Test
    void testGetPlanById_NotFound() throws Exception {
        when(planService.getPlanById(999L, 1L))
                .thenThrow(new IllegalArgumentException("Plan not found: 999"));
        
        mockMvc.perform(get("/api/v1/plans/999")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Plan not found: 999"))
                .andDo(print());
    }
    
    @Test
    void testGetPlanById_Unauthorized() throws Exception {
        when(planService.getPlanById(1L, 2L))
                .thenThrow(new IllegalArgumentException("unauthorized: userId mismatch"));
        
        mockMvc.perform(get("/api/v1/plans/1")
                .header("X-User-Id", "2")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("FORBIDDEN"))
                .andDo(print());
    }
    
    // POST /api/v1/plans - Create plan
    @Test
    void testCreatePlan_Success() throws Exception {
        when(planService.createPlan(1L, planRequestDTO))
                .thenReturn(planDTO);
        
        mockMvc.perform(post("/api/v1/plans")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(planRequestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Summer Fitness Plan"))
                .andDo(print());
        
        verify(planService, times(1)).createPlan(1L, planRequestDTO);
    }
    
    @Test
    void testCreatePlan_BadRequest() throws Exception {
        when(planService.createPlan(1L, planRequestDTO))
                .thenThrow(new IllegalArgumentException("Invalid plan data"));
        
        mockMvc.perform(post("/api/v1/plans")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(planRequestDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andDo(print());
    }
    
    // PUT /api/v1/plans/{id} - Update plan
    @Test
    void testUpdatePlan_Success() throws Exception {
        when(planService.updatePlan(1L, 1L, planRequestDTO))
                .thenReturn(planDTO);
        
        mockMvc.perform(put("/api/v1/plans/1")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(planRequestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andDo(print());
        
        verify(planService, times(1)).updatePlan(1L, 1L, planRequestDTO);
    }
    
    @Test
    void testUpdatePlan_Unauthorized() throws Exception {
        when(planService.updatePlan(1L, 2L, planRequestDTO))
                .thenThrow(new IllegalArgumentException("unauthorized: userId mismatch"));
        
        mockMvc.perform(put("/api/v1/plans/1")
                .header("X-User-Id", "2")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(planRequestDTO)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("FORBIDDEN"))
                .andDo(print());
    }
    
    // DELETE /api/v1/plans/{id} - Delete plan
    @Test
    void testDeletePlan_Success() throws Exception {
        doNothing().when(planService).deletePlan(1L, 1L);
        
        mockMvc.perform(delete("/api/v1/plans/1")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent())
                .andDo(print());
        
        verify(planService, times(1)).deletePlan(1L, 1L);
    }
    
    @Test
    void testDeletePlan_NotFound() throws Exception {
        doThrow(new IllegalArgumentException("Plan not found: 999"))
                .when(planService).deletePlan(999L, 1L);
        
        mockMvc.perform(delete("/api/v1/plans/999")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"))
                .andDo(print());
    }
}
