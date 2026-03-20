package com.gym.training.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gym.common.dto.PageResponse;
import com.gym.training.config.TestSecurityConfig;
import com.gym.training.dto.ExerciseDTO;
import com.gym.training.dto.ExerciseRequestDTO;
import com.gym.training.entity.Exercise;
import com.gym.training.service.ExerciseService;
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

@WebMvcTest(ExerciseController.class)
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
class ExerciseControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private ExerciseService exerciseService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private ExerciseDTO exerciseDTO;
    private ExerciseRequestDTO exerciseRequestDTO;
    
    @BeforeEach
    void setUp() {
        exerciseDTO = ExerciseDTO.builder()
                .id(1L)
                .name("Push Up")
                .description("Upper body exercise")
                .type(Exercise.ExerciseType.SYSTEM)
                .disciplineId(1L)
                .disciplineName("Strength")
                .createdAt(LocalDateTime.now())
                .build();
        
        exerciseRequestDTO = ExerciseRequestDTO.builder()
                .name("Push Up")
                .description("Upper body exercise")
                .type(Exercise.ExerciseType.USER)
                .disciplineId(1L)
                .build();
    }
    
    // GET /api/v1/exercises/system - Paginated list
    @Test
    void testGetAllSystemExercises_Success() throws Exception {
        Page<ExerciseDTO> page = new PageImpl<>(List.of(exerciseDTO), PageRequest.of(0, 20), 1);
        PageResponse<ExerciseDTO> pageResponse = PageResponse.of(page);
        
        when(exerciseService.getAllSystemExercises(any(Pageable.class)))
                .thenReturn(pageResponse);
        
        mockMvc.perform(get("/api/v1/exercises/system")
                .param("page", "0")
                .param("size", "20")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].name").value("Push Up"))
                .andExpect(jsonPath("$.currentPage").value(0))
                .andExpect(jsonPath("$.pageSize").value(20))
                .andDo(print());
        
        verify(exerciseService, times(1)).getAllSystemExercises(any(Pageable.class));
    }
    
    // GET /api/v1/exercises/discipline/{id} - Paginated list with error handling
    @Test
    void testGetExercisesByDiscipline_Success() throws Exception {
        Page<ExerciseDTO> page = new PageImpl<>(List.of(exerciseDTO), PageRequest.of(0, 20), 1);
        PageResponse<ExerciseDTO> pageResponse = PageResponse.of(page);
        
        when(exerciseService.getExercisesByDiscipline(eq(1L), any(Pageable.class)))
                .thenReturn(pageResponse);
        
        mockMvc.perform(get("/api/v1/exercises/discipline/1")
                .param("page", "0")
                .param("size", "20")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Push Up"))
                .andDo(print());
        
        verify(exerciseService, times(1)).getExercisesByDiscipline(eq(1L), any(Pageable.class));
    }
    
    @Test
    void testGetExercisesByDiscipline_NotFound() throws Exception {
        when(exerciseService.getExercisesByDiscipline(eq(999L), any(Pageable.class)))
                .thenThrow(new IllegalArgumentException("Discipline not found: 999"));
        
        mockMvc.perform(get("/api/v1/exercises/discipline/999")
                .param("page", "0")
                .param("size", "20")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Discipline not found: 999"))
                .andDo(print());
    }
    
    // GET /api/v1/exercises/my-exercises - User-specific paginated list
    @Test
    void testGetUserExercises_Success() throws Exception {
        Page<ExerciseDTO> page = new PageImpl<>(List.of(exerciseDTO), PageRequest.of(0, 20), 1);
        PageResponse<ExerciseDTO> pageResponse = PageResponse.of(page);
        
        when(exerciseService.getUserExercises(eq(1L), any(Pageable.class)))
                .thenReturn(pageResponse);
        
        mockMvc.perform(get("/api/v1/exercises/my-exercises")
                .header("X-User-Id", "1")
                .param("page", "0")
                .param("size", "20")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Push Up"))
                .andDo(print());
        
        verify(exerciseService, times(1)).getUserExercises(eq(1L), any(Pageable.class));
    }
    
    // GET /api/v1/exercises/{id} - Single resource
    @Test
    void testGetExerciseById_Success() throws Exception {
        when(exerciseService.getExerciseById(1L))
                .thenReturn(exerciseDTO);
        
        mockMvc.perform(get("/api/v1/exercises/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Push Up"))
                .andDo(print());
        
        verify(exerciseService, times(1)).getExerciseById(1L);
    }
    
    @Test
    void testGetExerciseById_NotFound() throws Exception {
        when(exerciseService.getExerciseById(999L))
                .thenThrow(new IllegalArgumentException("Exercise not found: 999"));
        
        mockMvc.perform(get("/api/v1/exercises/999")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"))
                .andDo(print());
    }
    
    // POST /api/v1/exercises - Create exercise
    @Test
    void testCreateExercise_Success() throws Exception {
        ExerciseDTO createdExercise = ExerciseDTO.builder()
                .id(2L)
                .name("Custom Push Up")
                .description("Upper body exercise")
                .type(Exercise.ExerciseType.USER)
                .disciplineId(1L)
                .createdAt(LocalDateTime.now())
                .build();
        
        when(exerciseService.createExercise(any(ExerciseRequestDTO.class), eq(1L)))
                .thenReturn(createdExercise);
        
        mockMvc.perform(post("/api/v1/exercises")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(exerciseRequestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.name").value("Custom Push Up"))
                .andDo(print());
        
        verify(exerciseService, times(1)).createExercise(any(ExerciseRequestDTO.class), eq(1L));
    }
    
    @Test
    void testCreateExercise_BadRequest() throws Exception {
        when(exerciseService.createExercise(any(ExerciseRequestDTO.class), eq(1L)))
                .thenThrow(new IllegalArgumentException("Discipline not found"));
        
        mockMvc.perform(post("/api/v1/exercises")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(exerciseRequestDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andDo(print());
    }
    
    // PUT /api/v1/exercises/{id} - Update exercise
    @Test
    void testUpdateExercise_Success() throws Exception {
        ExerciseDTO updatedExercise = ExerciseDTO.builder()
                .id(1L)
                .name("Updated Push Up")
                .description("Updated description")
                .type(Exercise.ExerciseType.USER)
                .disciplineId(1L)
                .createdAt(LocalDateTime.now())
                .build();
        
        when(exerciseService.updateExercise(eq(1L), any(ExerciseRequestDTO.class), eq(1L)))
                .thenReturn(updatedExercise);
        
        mockMvc.perform(put("/api/v1/exercises/1")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(exerciseRequestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Push Up"))
                .andDo(print());
        
        verify(exerciseService, times(1)).updateExercise(eq(1L), any(ExerciseRequestDTO.class), eq(1L));
    }
    
    @Test
    void testUpdateExercise_Unauthorized() throws Exception {
        when(exerciseService.updateExercise(eq(1L), any(ExerciseRequestDTO.class), eq(1L)))
                .thenThrow(new IllegalArgumentException("User is unauthorized to update this exercise"));
        
        mockMvc.perform(put("/api/v1/exercises/1")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(exerciseRequestDTO)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("FORBIDDEN"))
                .andDo(print());
    }
    
    // DELETE /api/v1/exercises/{id} - Delete exercise
    @Test
    void testDeleteExercise_Success() throws Exception {
        doNothing().when(exerciseService).deleteExercise(1L, 1L);
        
        mockMvc.perform(delete("/api/v1/exercises/1")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent())
                .andDo(print());
        
        verify(exerciseService, times(1)).deleteExercise(1L, 1L);
    }
    
    @Test
    void testDeleteExercise_NotFound() throws Exception {
        doThrow(new IllegalArgumentException("Exercise not found: 999"))
                .when(exerciseService).deleteExercise(999L, 1L);
        
        mockMvc.perform(delete("/api/v1/exercises/999")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"))
                .andDo(print());
    }
    
    // Test pagination with custom parameters
    @Test
    void testGetAllSystemExercises_WithCustomPagination() throws Exception {
        Page<ExerciseDTO> page = new PageImpl<>(List.of(exerciseDTO), PageRequest.of(1, 50), 100);
        PageResponse<ExerciseDTO> pageResponse = PageResponse.of(page);
        
        when(exerciseService.getAllSystemExercises(any(Pageable.class)))
                .thenReturn(pageResponse);
        
        mockMvc.perform(get("/api/v1/exercises/system")
                .param("page", "1")
                .param("size", "50")
                .param("sort", "name,asc")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPage").value(1))
                .andExpect(jsonPath("$.pageSize").value(50))
                .andExpect(jsonPath("$.totalElements").value(100))
                .andDo(print());
    }
}
