package com.gym.training.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gym.common.dto.PageResponse;
import com.gym.training.config.TestSecurityConfig;
import com.gym.training.dto.UserRoutineDTO;
import com.gym.training.dto.UserRoutineRequestDTO;
import com.gym.training.service.UserRoutineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserRoutineController.class)
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
class UserRoutineControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private UserRoutineService userRoutineService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private UserRoutineDTO routineDTO;
    private UserRoutineRequestDTO routineRequestDTO;
    
    @BeforeEach
    void setUp() {
        routineDTO = UserRoutineDTO.builder()
                .id(1L)
                .userId(1L)
                .routineTemplateId(1L)
                .routineTemplateName("Full Body Workout")
                .isActive(true)
                .startDate(LocalDateTime.now())
                .endDate(null)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        routineRequestDTO = UserRoutineRequestDTO.builder()
                .routineTemplateId(1L)
                .isActive(true)
                .build();
    }
    
    // GET /api/v1/user-routines/active - Get active routines with pagination
    @Test
    void testGetActiveRoutines_Success() throws Exception {
        Page<UserRoutineDTO> page = new PageImpl<>(List.of(routineDTO), PageRequest.of(0, 20), 1);
        PageResponse<UserRoutineDTO> pageResponse = PageResponse.of(page);
        
        when(userRoutineService.getUserActiveRoutines(eq(1L), any(Pageable.class)))
                .thenReturn(pageResponse);
        
        mockMvc.perform(get("/api/v1/user-routines/active")
                .header("X-User-Id", "1")
                .param("page", "0")
                .param("size", "20")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].routineTemplateName").value("Full Body Workout"))
                .andExpect(jsonPath("$.data[0].isActive").value(true))
                .andExpect(jsonPath("$.currentPage").value(0))
                .andDo(print());
        
        verify(userRoutineService, times(1)).getUserActiveRoutines(eq(1L), any(Pageable.class));
    }
    
    // GET /api/v1/user-routines - Get all routines with pagination
    @Test
    void testGetAllRoutines_Success() throws Exception {
        Page<UserRoutineDTO> page = new PageImpl<>(List.of(routineDTO), PageRequest.of(0, 20), 1);
        PageResponse<UserRoutineDTO> pageResponse = PageResponse.of(page);
        
        when(userRoutineService.getUserRoutines(eq(1L), any(Pageable.class)))
                .thenReturn(pageResponse);
        
        mockMvc.perform(get("/api/v1/user-routines")
                .header("X-User-Id", "1")
                .param("page", "0")
                .param("size", "20")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].routineTemplateName").value("Full Body Workout"))
                .andDo(print());
        
        verify(userRoutineService, times(1)).getUserRoutines(eq(1L), any(Pageable.class));
    }
    
    // GET /api/v1/user-routines/{id} - Get single routine
    @Test
    void testGetRoutineById_Success() throws Exception {
        when(userRoutineService.getRoutineById(1L, 1L))
                .thenReturn(routineDTO);
        
        mockMvc.perform(get("/api/v1/user-routines/1")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.routineTemplateName").value("Full Body Workout"))
                .andDo(print());
        
        verify(userRoutineService, times(1)).getRoutineById(1L, 1L);
    }
    
    @Test
    void testGetRoutineById_NotFound() throws Exception {
        when(userRoutineService.getRoutineById(999L, 1L))
                .thenThrow(new IllegalArgumentException("Routine not found or unauthorized"));
        
        mockMvc.perform(get("/api/v1/user-routines/999")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"))
                .andDo(print());
    }
    
    // POST /api/v1/user-routines/assign - Assign routine
    @Test
    void testAssignRoutine_Success() throws Exception {
        UserRoutineDTO assignedRoutine = UserRoutineDTO.builder()
                .id(2L)
                .userId(1L)
                .routineTemplateId(2L)
                .routineTemplateName("Upper Body")
                .isActive(true)
                .startDate(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        UserRoutineRequestDTO assignRequest = UserRoutineRequestDTO.builder()
                .routineTemplateId(2L)
                .isActive(true)
                .build();
        
        when(userRoutineService.assignRoutine(any(UserRoutineRequestDTO.class), eq(1L)))
                .thenReturn(assignedRoutine);
        
        mockMvc.perform(post("/api/v1/user-routines/assign")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(assignRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.routineTemplateName").value("Upper Body"))
                .andDo(print());
        
        verify(userRoutineService, times(1)).assignRoutine(any(UserRoutineRequestDTO.class), eq(1L));
    }
    
    @Test
    void testAssignRoutine_BadRequest() throws Exception {
        UserRoutineRequestDTO assignRequest = UserRoutineRequestDTO.builder()
                .routineTemplateId(999L)
                .isActive(true)
                .build();
        
        when(userRoutineService.assignRoutine(any(UserRoutineRequestDTO.class), eq(1L)))
                .thenThrow(new IllegalArgumentException("Routine template not found: 999"));
        
        mockMvc.perform(post("/api/v1/user-routines/assign")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(assignRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andDo(print());
    }
    
    // PUT /api/v1/user-routines/{id} - Update routine
    @Test
    void testUpdateRoutine_Success() throws Exception {
        UserRoutineDTO updatedRoutine = UserRoutineDTO.builder()
                .id(1L)
                .userId(1L)
                .routineTemplateId(1L)
                .routineTemplateName("Full Body Workout")
                .isActive(false)
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        UserRoutineRequestDTO updateRequest = UserRoutineRequestDTO.builder()
                .routineTemplateId(1L)
                .isActive(false)
                .build();
        
        when(userRoutineService.updateRoutine(eq(1L), any(UserRoutineRequestDTO.class), eq(1L)))
                .thenReturn(updatedRoutine);
        
        mockMvc.perform(put("/api/v1/user-routines/1")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(false))
                .andDo(print());
        
        verify(userRoutineService, times(1)).updateRoutine(eq(1L), any(UserRoutineRequestDTO.class), eq(1L));
    }
    
    @Test
    void testUpdateRoutine_NotFound() throws Exception {
        UserRoutineRequestDTO updateRequest = UserRoutineRequestDTO.builder()
                .routineTemplateId(1L)
                .isActive(false)
                .build();
        
        when(userRoutineService.updateRoutine(eq(999L), any(UserRoutineRequestDTO.class), eq(1L)))
                .thenThrow(new IllegalArgumentException("Routine not found or unauthorized"));
        
        mockMvc.perform(put("/api/v1/user-routines/999")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"))
                .andDo(print());
    }
    
    // PATCH /api/v1/user-routines/{id}/deactivate - Deactivate routine
    @Test
    void testDeactivateRoutine_Success() throws Exception {
        UserRoutineDTO deactivatedRoutine = UserRoutineDTO.builder()
                .id(1L)
                .userId(1L)
                .routineTemplateId(1L)
                .routineTemplateName("Full Body Workout")
                .isActive(false)
                .startDate(LocalDateTime.now().minusDays(7))
                .endDate(LocalDateTime.now())
                .createdAt(LocalDateTime.now().minusDays(7))
                .updatedAt(LocalDateTime.now())
                .build();
        
        when(userRoutineService.deactivateRoutine(1L, 1L))
                .thenReturn(deactivatedRoutine);
        
        mockMvc.perform(patch("/api/v1/user-routines/1/deactivate")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(false))
                .andExpect(jsonPath("$.endDate").exists())
                .andDo(print());
        
        verify(userRoutineService, times(1)).deactivateRoutine(1L, 1L);
    }
    
    @Test
    void testDeactivateRoutine_NotFound() throws Exception {
        when(userRoutineService.deactivateRoutine(999L, 1L))
                .thenThrow(new IllegalArgumentException("Routine not found or unauthorized"));
        
        mockMvc.perform(patch("/api/v1/user-routines/999/deactivate")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"))
                .andDo(print());
    }
    
    // DELETE /api/v1/user-routines/{id} - Delete routine
    @Test
    void testDeleteRoutine_Success() throws Exception {
        doNothing().when(userRoutineService).deleteRoutine(1L, 1L);
        
        mockMvc.perform(delete("/api/v1/user-routines/1")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent())
                .andDo(print());
        
        verify(userRoutineService, times(1)).deleteRoutine(1L, 1L);
    }
    
    @Test
    void testDeleteRoutine_NotFound() throws Exception {
        doThrow(new IllegalArgumentException("Routine not found or unauthorized"))
                .when(userRoutineService).deleteRoutine(999L, 1L);
        
        mockMvc.perform(delete("/api/v1/user-routines/999")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"))
                .andDo(print());
    }
    
    // Test pagination with custom parameters
    @Test
    void testGetActiveRoutines_WithCustomPagination() throws Exception {
        Page<UserRoutineDTO> page = new PageImpl<>(List.of(routineDTO), PageRequest.of(1, 50), 100);
        PageResponse<UserRoutineDTO> pageResponse = PageResponse.of(page);
        
        when(userRoutineService.getUserActiveRoutines(eq(1L), any(Pageable.class)))
                .thenReturn(pageResponse);
        
        mockMvc.perform(get("/api/v1/user-routines/active")
                .header("X-User-Id", "1")
                .param("page", "1")
                .param("size", "50")
                .param("sort", "startDate,desc")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPage").value(1))
                .andExpect(jsonPath("$.pageSize").value(50))
                .andExpect(jsonPath("$.totalElements").value(100))
                .andDo(print());
    }
}
