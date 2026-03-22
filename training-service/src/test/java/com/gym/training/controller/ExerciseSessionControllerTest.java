package com.gym.training.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gym.common.dto.PageResponse;
import com.gym.training.dto.ExerciseSessionDTO;
import com.gym.training.dto.ExerciseSessionRequestDTO;
import com.gym.training.service.ExerciseSessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import com.gym.common.config.GymExceptionHandlerAutoConfiguration;
import com.gym.common.config.GymTestSecurityAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ExerciseSessionController.class)
@Import({GymTestSecurityAutoConfiguration.class, GymExceptionHandlerAutoConfiguration.class})
@ActiveProfiles("test")
class ExerciseSessionControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private ExerciseSessionService exerciseSessionService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private ExerciseSessionDTO sessionDTO;
    private ExerciseSessionRequestDTO sessionRequestDTO;
    
    @BeforeEach
    void setUp() {
        sessionDTO = ExerciseSessionDTO.builder()
                .id(1L)
                .userRoutineId(1L)
                .exerciseId(1L)
                .exerciseName("Push Up")
                .sets(3)
                .reps(10)
                .weight(0.0)
                .duration(300)
                .notes("Felt strong")
                .sessionDate(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
        
        sessionRequestDTO = ExerciseSessionRequestDTO.builder()
                .userRoutineId(1L)
                .exerciseId(1L)
                .sets(3)
                .reps(10)
                .weight(0.0)
                .duration(300)
                .notes("Felt strong")
                .build();
    }
    
    // GET /api/v1/exercise-sessions/routine/{routineId} - Get sessions by routine with pagination
    @Test
    void testGetSessionsByRoutine_Success() throws Exception {
        Page<ExerciseSessionDTO> page = new PageImpl<>(List.of(sessionDTO), PageRequest.of(0, 20), 1);
        PageResponse<ExerciseSessionDTO> pageResponse = PageResponse.of(page);
        
        when(exerciseSessionService.getSessionsByRoutineId(eq(1L), any(Pageable.class)))
                .thenReturn(pageResponse);
        
        mockMvc.perform(get("/api/v1/exercise-sessions/routine/1")
                .header("X-User-Id", "1")
                .param("page", "0")
                .param("size", "20")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].exerciseName").value("Push Up"))
                .andExpect(jsonPath("$.data[0].sets").value(3))
                .andExpect(jsonPath("$.currentPage").value(0))
                .andDo(print());
        
        verify(exerciseSessionService, times(1)).getSessionsByRoutineId(eq(1L), any(Pageable.class));
    }
    
    // GET /api/v1/exercise-sessions/date/{date} - Get sessions by date with pagination
    @Test
    void testGetSessionsByDate_Success() throws Exception {
        Page<ExerciseSessionDTO> page = new PageImpl<>(List.of(sessionDTO), PageRequest.of(0, 20), 1);
        PageResponse<ExerciseSessionDTO> pageResponse = PageResponse.of(page);
        
        when(exerciseSessionService.getSessionsByUserIdAndDate(eq(1L), any(LocalDate.class), any(Pageable.class)))
                .thenReturn(pageResponse);
        
        mockMvc.perform(get("/api/v1/exercise-sessions/date/2024-03-18")
                .header("X-User-Id", "1")
                .param("page", "0")
                .param("size", "20")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].exerciseName").value("Push Up"))
                .andDo(print());
        
        verify(exerciseSessionService, times(1)).getSessionsByUserIdAndDate(eq(1L), any(LocalDate.class), any(Pageable.class));
    }
    
    @Test
    void testGetSessionsByDate_InvalidFormat() throws Exception {
        mockMvc.perform(get("/api/v1/exercise-sessions/date/invalid-date")
                .header("X-User-Id", "1")
                .param("page", "0")
                .param("size", "20")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andDo(print());
    }
    
    // GET /api/v1/exercise-sessions/{id} - Get single session
    @Test
    void testGetSessionById_Success() throws Exception {
        when(exerciseSessionService.getSessionById(1L, 1L))
                .thenReturn(sessionDTO);
        
        mockMvc.perform(get("/api/v1/exercise-sessions/1")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.exerciseName").value("Push Up"))
                .andDo(print());
        
        verify(exerciseSessionService, times(1)).getSessionById(1L, 1L);
    }
    
    @Test
    void testGetSessionById_NotFound() throws Exception {
        when(exerciseSessionService.getSessionById(999L, 1L))
                .thenThrow(new IllegalArgumentException("Session not found or unauthorized"));
        
        mockMvc.perform(get("/api/v1/exercise-sessions/999")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"))
                .andDo(print());
    }
    
    // POST /api/v1/exercise-sessions - Create session
    @Test
    void testCreateSession_Success() throws Exception {
        ExerciseSessionDTO createdSession = ExerciseSessionDTO.builder()
                .id(2L)
                .userRoutineId(1L)
                .exerciseId(2L)
                .exerciseName("Pull Up")
                .sets(4)
                .reps(8)
                .weight(10.0)
                .duration(400)
                .notes("Great workout")
                .sessionDate(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
        
        ExerciseSessionRequestDTO createRequest = ExerciseSessionRequestDTO.builder()
                .userRoutineId(1L)
                .exerciseId(2L)
                .sets(4)
                .reps(8)
                .weight(10.0)
                .duration(400)
                .notes("Great workout")
                .build();
        
        when(exerciseSessionService.createSession(any(ExerciseSessionRequestDTO.class), eq(1L)))
                .thenReturn(createdSession);
        
        mockMvc.perform(post("/api/v1/exercise-sessions")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.exerciseName").value("Pull Up"))
                .andDo(print());
        
        verify(exerciseSessionService, times(1)).createSession(any(ExerciseSessionRequestDTO.class), eq(1L));
    }
    
    @Test
    void testCreateSession_BadRequest() throws Exception {
        ExerciseSessionRequestDTO createRequest = ExerciseSessionRequestDTO.builder()
                .userRoutineId(999L)
                .exerciseId(999L)
                .sets(3)
                .reps(10)
                .weight(0.0)
                .duration(300)
                .build();
        
        when(exerciseSessionService.createSession(any(ExerciseSessionRequestDTO.class), eq(1L)))
                .thenThrow(new IllegalArgumentException("User routine not found: 999"));
        
        mockMvc.perform(post("/api/v1/exercise-sessions")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andDo(print());
    }
    
    @Test
    void testCreateSession_UserNotAuthorized() throws Exception {
        ExerciseSessionRequestDTO createRequest = ExerciseSessionRequestDTO.builder()
                .userRoutineId(1L)
                .exerciseId(1L)
                .sets(3)
                .reps(10)
                .weight(0.0)
                .duration(300)
                .build();
        
        when(exerciseSessionService.createSession(any(ExerciseSessionRequestDTO.class), eq(1L)))
                .thenThrow(new IllegalArgumentException("User routine does not belong to user"));
        
        mockMvc.perform(post("/api/v1/exercise-sessions")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andDo(print());
    }
    
    // PUT /api/v1/exercise-sessions/{id} - Update session
    @Test
    void testUpdateSession_Success() throws Exception {
        ExerciseSessionDTO updatedSession = ExerciseSessionDTO.builder()
                .id(1L)
                .userRoutineId(1L)
                .exerciseId(1L)
                .exerciseName("Push Up")
                .sets(4)
                .reps(12)
                .weight(5.0)
                .duration(350)
                .notes("Improved performance")
                .sessionDate(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
        
        ExerciseSessionRequestDTO updateRequest = ExerciseSessionRequestDTO.builder()
                .userRoutineId(1L)
                .exerciseId(1L)
                .sets(4)
                .reps(12)
                .weight(5.0)
                .duration(350)
                .notes("Improved performance")
                .build();
        
        when(exerciseSessionService.updateSession(eq(1L), any(ExerciseSessionRequestDTO.class), eq(1L)))
                .thenReturn(updatedSession);
        
        mockMvc.perform(put("/api/v1/exercise-sessions/1")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sets").value(4))
                .andExpect(jsonPath("$.reps").value(12))
                .andExpect(jsonPath("$.notes").value("Improved performance"))
                .andDo(print());
        
        verify(exerciseSessionService, times(1)).updateSession(eq(1L), any(ExerciseSessionRequestDTO.class), eq(1L));
    }
    
    @Test
    void testUpdateSession_NotFound() throws Exception {
        ExerciseSessionRequestDTO updateRequest = ExerciseSessionRequestDTO.builder()
                .userRoutineId(1L)
                .exerciseId(1L)
                .sets(3)
                .reps(10)
                .build();
        
        when(exerciseSessionService.updateSession(eq(999L), any(ExerciseSessionRequestDTO.class), eq(1L)))
                .thenThrow(new IllegalArgumentException("Session not found or unauthorized"));
        
        mockMvc.perform(put("/api/v1/exercise-sessions/999")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"))
                .andDo(print());
    }
    
    // DELETE /api/v1/exercise-sessions/{id} - Delete session
    @Test
    void testDeleteSession_Success() throws Exception {
        doNothing().when(exerciseSessionService).deleteSession(1L, 1L);
        
        mockMvc.perform(delete("/api/v1/exercise-sessions/1")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent())
                .andDo(print());
        
        verify(exerciseSessionService, times(1)).deleteSession(1L, 1L);
    }
    
    @Test
    void testDeleteSession_NotFound() throws Exception {
        doThrow(new IllegalArgumentException("Session not found or unauthorized"))
                .when(exerciseSessionService).deleteSession(999L, 1L);
        
        mockMvc.perform(delete("/api/v1/exercise-sessions/999")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"))
                .andDo(print());
    }
    
    // Test pagination with custom parameters
    @Test
    void testGetSessionsByRoutine_WithCustomPagination() throws Exception {
        Page<ExerciseSessionDTO> page = new PageImpl<>(List.of(sessionDTO), PageRequest.of(1, 50), 100);
        PageResponse<ExerciseSessionDTO> pageResponse = PageResponse.of(page);
        
        when(exerciseSessionService.getSessionsByRoutineId(eq(1L), any(Pageable.class)))
                .thenReturn(pageResponse);
        
        mockMvc.perform(get("/api/v1/exercise-sessions/routine/1")
                .header("X-User-Id", "1")
                .param("page", "1")
                .param("size", "50")
                .param("sort", "sessionDate,desc")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPage").value(1))
                .andExpect(jsonPath("$.pageSize").value(50))
                .andExpect(jsonPath("$.totalElements").value(100))
                .andDo(print());
    }
}
