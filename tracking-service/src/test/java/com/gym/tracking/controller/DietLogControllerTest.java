package com.gym.tracking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gym.tracking.dto.DietLogDTO;
import com.gym.tracking.dto.DietLogRequestDTO;
import com.gym.tracking.service.DietLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
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

@WebMvcTest(DietLogController.class)
@Import({GymTestSecurityAutoConfiguration.class, GymExceptionHandlerAutoConfiguration.class})
@ActiveProfiles("test")
class DietLogControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private DietLogService dietLogService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private DietLogDTO dietLogDTO;
    private DietLogRequestDTO dietLogRequestDTO;
    private LocalDate testDate;
    
    @BeforeEach
    void setUp() {
        testDate = LocalDate.of(2026, 3, 19);
        
        dietLogDTO = DietLogDTO.builder()
                .id(1L)
                .userId(1L)
                .logDate(testDate)
                .meal("Breakfast")
                .foodItems("Eggs, Toast, Orange Juice")
                .calories(450.0)
                .macros("Protein: 25g, Carbs: 50g, Fat: 15g")
                .notes("Good morning meal")
                .createdAt(LocalDateTime.now())
                .build();
        
        dietLogRequestDTO = DietLogRequestDTO.builder()
                .logDate(testDate)
                .meal("Breakfast")
                .foodItems("Eggs, Toast, Orange Juice")
                .calories(450.0)
                .macros("Protein: 25g, Carbs: 50g, Fat: 15g")
                .notes("Good morning meal")
                .build();
    }
    
    // Test 1: GET /api/v1/diet-logs/{id} - Success
    @Test
    void testGetDietLogById_Success() throws Exception {
        when(dietLogService.getDietLogById(1L, 1L))
                .thenReturn(dietLogDTO);
        
        mockMvc.perform(get("/api/v1/diet-logs/1")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.meal").value("Breakfast"))
                .andExpect(jsonPath("$.calories").value(450.0))
                .andDo(print());
        
        verify(dietLogService, times(1)).getDietLogById(1L, 1L);
    }
    
    // Test 2: GET /api/v1/diet-logs/{id} - Not Found
    @Test
    void testGetDietLogById_NotFound() throws Exception {
        when(dietLogService.getDietLogById(999L, 1L))
                .thenThrow(new IllegalArgumentException("Diet log not found: 999"));
        
        mockMvc.perform(get("/api/v1/diet-logs/999")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Diet log not found: 999"))
                .andDo(print());
        
        verify(dietLogService, times(1)).getDietLogById(999L, 1L);
    }
    
    // Test 3: GET /api/v1/diet-logs/{id} - Unauthorized
    @Test
    void testGetDietLogById_Unauthorized() throws Exception {
        when(dietLogService.getDietLogById(1L, 2L))
                .thenThrow(new IllegalArgumentException("User is not authorized to access this diet log"));
        
        mockMvc.perform(get("/api/v1/diet-logs/1")
                .header("X-User-Id", "2")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("FORBIDDEN"))
                .andDo(print());
        
        verify(dietLogService, times(1)).getDietLogById(1L, 2L);
    }
    
    // Test 4: GET /api/v1/diet-logs - Get all user diet logs
    @Test
    void testGetUserDietLogs_Success() throws Exception {
        DietLogDTO dietLog2 = DietLogDTO.builder()
                .id(2L)
                .userId(1L)
                .logDate(testDate)
                .meal("Lunch")
                .foodItems("Chicken, Rice, Vegetables")
                .calories(650.0)
                .macros("Protein: 40g, Carbs: 70g, Fat: 20g")
                .notes("Protein-rich lunch")
                .createdAt(LocalDateTime.now())
                .build();
        
        when(dietLogService.getUserDietLogs(1L))
                .thenReturn(List.of(dietLogDTO, dietLog2));
        
        mockMvc.perform(get("/api/v1/diet-logs")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].meal").value("Breakfast"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].meal").value("Lunch"))
                .andDo(print());
        
        verify(dietLogService, times(1)).getUserDietLogs(1L);
    }
    
    // Test 5: GET /api/v1/diet-logs/date/{date} - Success
    @Test
    void testGetByDate_Success() throws Exception {
        when(dietLogService.getDietLogsByDate(1L, testDate))
                .thenReturn(List.of(dietLogDTO));
        
        mockMvc.perform(get("/api/v1/diet-logs/date/2026-03-19")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].logDate").value("2026-03-19"))
                .andExpect(jsonPath("$[0].meal").value("Breakfast"))
                .andDo(print());
        
        verify(dietLogService, times(1)).getDietLogsByDate(1L, testDate);
    }
    
    // Test 6: GET /api/v1/diet-logs/date/{date} - Bad Request (invalid date format)
    @Test
    void testGetByDate_BadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/diet-logs/date/invalid-date")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Invalid date format. Please use YYYY-MM-DD"))
                .andDo(print());
    }
    
    // Test 7: POST /api/v1/diet-logs - Create - Success
    @Test
    void testCreateDietLog_Success() throws Exception {
        when(dietLogService.createDietLog(1L, dietLogRequestDTO))
                .thenReturn(dietLogDTO);
        
        mockMvc.perform(post("/api/v1/diet-logs")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dietLogRequestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.meal").value("Breakfast"))
                .andExpect(jsonPath("$.calories").value(450.0))
                .andDo(print());
        
        verify(dietLogService, times(1)).createDietLog(1L, dietLogRequestDTO);
    }
    
    // Test 8: POST /api/v1/diet-logs - Create - Bad Request (validation error)
    @Test
    void testCreateDietLog_BadRequest() throws Exception {
        DietLogRequestDTO invalidRequest = DietLogRequestDTO.builder()
                .logDate(testDate)
                .meal("") // Invalid: blank
                .foodItems("Eggs, Toast")
                .calories(450.0)
                .build();
        
        mockMvc.perform(post("/api/v1/diet-logs")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andDo(print());
    }
    
    // Test 9: PUT /api/v1/diet-logs/{id} - Update - Success
    @Test
    void testUpdateDietLog_Success() throws Exception {
        when(dietLogService.updateDietLog(1L, 1L, dietLogRequestDTO))
                .thenReturn(dietLogDTO);
        
        mockMvc.perform(put("/api/v1/diet-logs/1")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dietLogRequestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.meal").value("Breakfast"))
                .andExpect(jsonPath("$.calories").value(450.0))
                .andDo(print());
        
        verify(dietLogService, times(1)).updateDietLog(1L, 1L, dietLogRequestDTO);
    }
    
    // Test 10: DELETE /api/v1/diet-logs/{id} - Delete - Success
    @Test
    void testDeleteDietLog_Success() throws Exception {
        doNothing().when(dietLogService).deleteDietLog(1L, 1L);
        
        mockMvc.perform(delete("/api/v1/diet-logs/1")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent())
                .andDo(print());
        
        verify(dietLogService, times(1)).deleteDietLog(1L, 1L);
    }
}
