package com.gym.tracking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gym.tracking.dto.ObjectiveDTO;
import com.gym.tracking.dto.ObjectiveRequestDTO;
import com.gym.tracking.service.ObjectiveService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ObjectiveController.class)
class ObjectiveControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private ObjectiveService objectiveService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private ObjectiveDTO objectiveDTO;
    private ObjectiveRequestDTO objectiveRequestDTO;
    
    @BeforeEach
    void setUp() {
        objectiveDTO = ObjectiveDTO.builder()
                .id(1L)
                .userId(1L)
                .title("Run a 5K")
                .description("Complete a 5 kilometer run")
                .category("Cardio")
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        objectiveRequestDTO = ObjectiveRequestDTO.builder()
                .title("Run a 5K")
                .description("Complete a 5 kilometer run")
                .category("Cardio")
                .isActive(true)
                .build();
    }
    
    // GET /api/v1/objectives - Get user objectives
    @Test
    void testGetUserObjectives_Success() throws Exception {
        when(objectiveService.getUserObjectives(1L))
                .thenReturn(List.of(objectiveDTO));
        
        mockMvc.perform(get("/api/v1/objectives")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("Run a 5K"))
                .andExpect(jsonPath("$[0].userId").value(1))
                .andDo(print());
        
        verify(objectiveService, times(1)).getUserObjectives(1L);
    }
    
    // GET /api/v1/objectives/{id} - Get objective by ID
    @Test
    void testGetObjectiveById_Success() throws Exception {
        when(objectiveService.getObjectiveById(1L, 1L))
                .thenReturn(objectiveDTO);
        
        mockMvc.perform(get("/api/v1/objectives/1")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Run a 5K"))
                .andDo(print());
        
        verify(objectiveService, times(1)).getObjectiveById(1L, 1L);
    }
    
    @Test
    void testGetObjectiveById_NotFound() throws Exception {
        when(objectiveService.getObjectiveById(999L, 1L))
                .thenThrow(new IllegalArgumentException("Objective not found: 999"));
        
        mockMvc.perform(get("/api/v1/objectives/999")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Objective not found: 999"))
                .andDo(print());
    }
    
    @Test
    void testGetObjectiveById_Unauthorized() throws Exception {
        when(objectiveService.getObjectiveById(1L, 2L))
                .thenThrow(new IllegalArgumentException("unauthorized: userId mismatch"));
        
        mockMvc.perform(get("/api/v1/objectives/1")
                .header("X-User-Id", "2")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("FORBIDDEN"))
                .andDo(print());
    }
    
    // POST /api/v1/objectives - Create objective
    @Test
    void testCreateObjective_Success() throws Exception {
        when(objectiveService.createObjective(1L, objectiveRequestDTO))
                .thenReturn(objectiveDTO);
        
        mockMvc.perform(post("/api/v1/objectives")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(objectiveRequestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Run a 5K"))
                .andDo(print());
        
        verify(objectiveService, times(1)).createObjective(1L, objectiveRequestDTO);
    }
    
    @Test
    void testCreateObjective_BadRequest() throws Exception {
        when(objectiveService.createObjective(1L, objectiveRequestDTO))
                .thenThrow(new IllegalArgumentException("Invalid objective data"));
        
        mockMvc.perform(post("/api/v1/objectives")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(objectiveRequestDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andDo(print());
    }
    
    // PUT /api/v1/objectives/{id} - Update objective
    @Test
    void testUpdateObjective_Success() throws Exception {
        when(objectiveService.updateObjective(1L, 1L, objectiveRequestDTO))
                .thenReturn(objectiveDTO);
        
        mockMvc.perform(put("/api/v1/objectives/1")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(objectiveRequestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andDo(print());
        
        verify(objectiveService, times(1)).updateObjective(1L, 1L, objectiveRequestDTO);
    }
    
    @Test
    void testUpdateObjective_Unauthorized() throws Exception {
        when(objectiveService.updateObjective(1L, 2L, objectiveRequestDTO))
                .thenThrow(new IllegalArgumentException("unauthorized: userId mismatch"));
        
        mockMvc.perform(put("/api/v1/objectives/1")
                .header("X-User-Id", "2")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(objectiveRequestDTO)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("FORBIDDEN"))
                .andDo(print());
    }
    
    // DELETE /api/v1/objectives/{id} - Delete objective
    @Test
    void testDeleteObjective_Success() throws Exception {
        doNothing().when(objectiveService).deleteObjective(1L, 1L);
        
        mockMvc.perform(delete("/api/v1/objectives/1")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent())
                .andDo(print());
        
        verify(objectiveService, times(1)).deleteObjective(1L, 1L);
    }
    
    @Test
    void testDeleteObjective_NotFound() throws Exception {
        doThrow(new IllegalArgumentException("Objective not found: 999"))
                .when(objectiveService).deleteObjective(999L, 1L);
        
        mockMvc.perform(delete("/api/v1/objectives/999")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"))
                .andDo(print());
    }
}
