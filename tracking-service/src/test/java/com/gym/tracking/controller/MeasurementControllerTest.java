package com.gym.tracking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gym.tracking.dto.MeasurementValueDTO;
import com.gym.tracking.dto.MeasurementValueRequestDTO;
import com.gym.tracking.dto.MeasurementTypeDTO;
import com.gym.tracking.dto.MeasurementTypeRequestDTO;
import com.gym.tracking.service.MeasurementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MeasurementController.class)
@ExtendWith(MockitoExtension.class)
class MeasurementControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private MeasurementService measurementService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private MeasurementValueDTO measurementDTO;
    private MeasurementValueRequestDTO measurementRequestDTO;
    private MeasurementTypeDTO typeDTO;
    private MeasurementTypeRequestDTO typeRequestDTO;
    
    @BeforeEach
    void setUp() {
        typeDTO = MeasurementTypeDTO.builder()
                .id(1L)
                .name("Weight")
                .unit("kg")
                .createdAt(LocalDateTime.now())
                .build();
        
        typeRequestDTO = MeasurementTypeRequestDTO.builder()
                .name("Weight")
                .unit("kg")
                .build();
        
        measurementDTO = MeasurementValueDTO.builder()
                .id(1L)
                .userId(1L)
                .measurementTypeId(1L)
                .value(75.5)
                .recordedDate(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
        
        measurementRequestDTO = MeasurementValueRequestDTO.builder()
                .measurementTypeId(1L)
                .value(75.5)
                .recordedDate(LocalDateTime.now())
                .build();
    }
    
    // GET /api/v1/measurements - Get user measurements
    @Test
    void testGetUserMeasurements_Success() throws Exception {
        when(measurementService.getUserMeasurements(1L))
                .thenReturn(List.of(measurementDTO));
        
        mockMvc.perform(get("/api/v1/measurements")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].value").value(75.5))
                .andExpect(jsonPath("$[0].userId").value(1))
                .andDo(print());
        
        verify(measurementService, times(1)).getUserMeasurements(1L);
    }
    
    // GET /api/v1/measurements/{id} - Get measurement by ID
    @Test
    void testGetMeasurementById_Success() throws Exception {
        when(measurementService.getMeasurementValueById(1L, 1L))
                .thenReturn(measurementDTO);
        
        mockMvc.perform(get("/api/v1/measurements/1")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.value").value(75.5))
                .andDo(print());
        
        verify(measurementService, times(1)).getMeasurementValueById(1L, 1L);
    }
    
    @Test
    void testGetMeasurementById_NotFound() throws Exception {
        when(measurementService.getMeasurementValueById(999L, 1L))
                .thenThrow(new IllegalArgumentException("Measurement not found: 999"));
        
        mockMvc.perform(get("/api/v1/measurements/999")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Measurement not found: 999"))
                .andDo(print());
    }
    
    @Test
    void testGetMeasurementById_Unauthorized() throws Exception {
        when(measurementService.getMeasurementValueById(1L, 2L))
                .thenThrow(new IllegalArgumentException("unauthorized: userId mismatch"));
        
        mockMvc.perform(get("/api/v1/measurements/1")
                .header("X-User-Id", "2")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("FORBIDDEN"))
                .andDo(print());
    }
    
    // GET /api/v1/measurement-types/{typeId} - Get measurement type
    @Test
    void testGetMeasurementType_Success() throws Exception {
        when(measurementService.getMeasurementTypeById(1L))
                .thenReturn(typeDTO);
        
        mockMvc.perform(get("/api/v1/measurements/types/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Weight"))
                .andExpect(jsonPath("$.unit").value("kg"))
                .andDo(print());
        
        verify(measurementService, times(1)).getMeasurementTypeById(1L);
    }
    
    @Test
    void testGetMeasurementType_NotFound() throws Exception {
        when(measurementService.getMeasurementTypeById(999L))
                .thenThrow(new IllegalArgumentException("Measurement type not found: 999"));
        
        mockMvc.perform(get("/api/v1/measurements/types/999")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"))
                .andDo(print());
    }
    
    // POST /api/v1/measurement-types - Create measurement type
    @Test
    void testCreateMeasurementType_Success() throws Exception {
        when(measurementService.createMeasurementType(any(MeasurementTypeRequestDTO.class)))
                .thenReturn(typeDTO);
        
        mockMvc.perform(post("/api/v1/measurements/types")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(typeRequestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Weight"))
                .andDo(print());
        
        verify(measurementService, times(1)).createMeasurementType(any(MeasurementTypeRequestDTO.class));
    }
    
    @Test
    void testCreateMeasurementType_BadRequest() throws Exception {
        when(measurementService.createMeasurementType(any(MeasurementTypeRequestDTO.class)))
                .thenThrow(new IllegalArgumentException("Invalid measurement type"));
        
        mockMvc.perform(post("/api/v1/measurements/types")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(typeRequestDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andDo(print());
    }
    
    // GET /api/v1/measurements/by-type/{typeId} - Get measurements by type
    @Test
    void testGetMeasurementsByType_Success() throws Exception {
        when(measurementService.getUserMeasurementsByType(1L, 1L))
                .thenReturn(List.of(measurementDTO));
        
        mockMvc.perform(get("/api/v1/measurements/by-type/1")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].measurementTypeId").value(1))
                .andDo(print());
        
        verify(measurementService, times(1)).getUserMeasurementsByType(1L, 1L);
    }
    
    @Test
    void testGetMeasurementsByType_BadRequest() throws Exception {
        when(measurementService.getUserMeasurementsByType(1L, 999L))
                .thenThrow(new IllegalArgumentException("Invalid measurement type"));
        
        mockMvc.perform(get("/api/v1/measurements/by-type/999")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andDo(print());
    }
    
    // POST /api/v1/measurements - Record measurement
    @Test
    void testRecordMeasurement_Success() throws Exception {
        when(measurementService.recordMeasurement(1L, measurementRequestDTO))
                .thenReturn(measurementDTO);
        
        mockMvc.perform(post("/api/v1/measurements")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(measurementRequestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.value").value(75.5))
                .andDo(print());
        
        verify(measurementService, times(1)).recordMeasurement(1L, measurementRequestDTO);
    }
    
    @Test
    void testRecordMeasurement_BadRequest() throws Exception {
        when(measurementService.recordMeasurement(1L, measurementRequestDTO))
                .thenThrow(new IllegalArgumentException("Invalid measurement value"));
        
        mockMvc.perform(post("/api/v1/measurements")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(measurementRequestDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andDo(print());
    }
    
    // PUT /api/v1/measurements/{id} - Update measurement
    @Test
    void testUpdateMeasurement_Success() throws Exception {
        when(measurementService.updateMeasurement(1L, 1L, measurementRequestDTO))
                .thenReturn(measurementDTO);
        
        mockMvc.perform(put("/api/v1/measurements/1")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(measurementRequestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andDo(print());
        
        verify(measurementService, times(1)).updateMeasurement(1L, 1L, measurementRequestDTO);
    }
    
    @Test
    void testUpdateMeasurement_Unauthorized() throws Exception {
        when(measurementService.updateMeasurement(1L, 2L, measurementRequestDTO))
                .thenThrow(new IllegalArgumentException("unauthorized: userId mismatch"));
        
        mockMvc.perform(put("/api/v1/measurements/1")
                .header("X-User-Id", "2")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(measurementRequestDTO)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("FORBIDDEN"))
                .andDo(print());
    }
    
    // DELETE /api/v1/measurements/{id} - Delete measurement
    @Test
    void testDeleteMeasurement_Success() throws Exception {
        doNothing().when(measurementService).deleteMeasurement(1L, 1L);
        
        mockMvc.perform(delete("/api/v1/measurements/1")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent())
                .andDo(print());
        
        verify(measurementService, times(1)).deleteMeasurement(1L, 1L);
    }
    
    @Test
    void testDeleteMeasurement_NotFound() throws Exception {
        doThrow(new IllegalArgumentException("Measurement not found: 999"))
                .when(measurementService).deleteMeasurement(999L, 1L);
        
        mockMvc.perform(delete("/api/v1/measurements/999")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"))
                .andDo(print());
    }
    
    @Test
    void testDeleteMeasurement_Unauthorized() throws Exception {
        doThrow(new IllegalArgumentException("unauthorized: userId mismatch"))
                .when(measurementService).deleteMeasurement(1L, 2L);
        
        mockMvc.perform(delete("/api/v1/measurements/1")
                .header("X-User-Id", "2")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("FORBIDDEN"))
                .andDo(print());
    }
}
