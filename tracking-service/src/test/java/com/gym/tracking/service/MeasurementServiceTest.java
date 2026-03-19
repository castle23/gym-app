package com.gym.tracking.service;

import com.gym.tracking.dto.MeasurementTypeDTO;
import com.gym.tracking.dto.MeasurementTypeRequestDTO;
import com.gym.tracking.dto.MeasurementValueDTO;
import com.gym.tracking.dto.MeasurementValueRequestDTO;
import com.gym.tracking.entity.MeasurementType;
import com.gym.tracking.entity.MeasurementValue;
import com.gym.tracking.repository.MeasurementTypeRepository;
import com.gym.tracking.repository.MeasurementValueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MeasurementServiceTest {
    
    @Mock
    private MeasurementTypeRepository measurementTypeRepository;
    
    @Mock
    private MeasurementValueRepository measurementValueRepository;
    
    @InjectMocks
    private MeasurementService measurementService;
    
    private MeasurementType weight;
    private MeasurementValue weightValue;
    
    @BeforeEach
    void setUp() {
        weight = MeasurementType.builder()
                .id(1L)
                .type("weight")
                .unit("kg")
                .isSystem(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        weightValue = MeasurementValue.builder()
                .id(1L)
                .userId(1L)
                .measurementType(weight)
                .value(75.5)
                .measurementDate(LocalDate.now())
                .notes("Morning weight")
                .createdAt(LocalDateTime.now())
                .build();
    }
    
    @Test
    void testGetAllMeasurementTypes() {
        when(measurementTypeRepository.findAll())
                .thenReturn(List.of(weight));
        
        List<MeasurementTypeDTO> result = measurementService.getAllMeasurementTypes();
        
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("weight", result.get(0).getType());
        verify(measurementTypeRepository, times(1)).findAll();
    }
    
    @Test
    void testGetMeasurementTypeById_Success() {
        when(measurementTypeRepository.findById(1L))
                .thenReturn(Optional.of(weight));
        
        MeasurementTypeDTO result = measurementService.getMeasurementTypeById(1L);
        
        assertNotNull(result);
        assertEquals("weight", result.getType());
    }
    
    @Test
    void testGetMeasurementTypeById_NotFound() {
        when(measurementTypeRepository.findById(999L))
                .thenReturn(Optional.empty());
        
        assertThrows(IllegalArgumentException.class, 
                () -> measurementService.getMeasurementTypeById(999L));
    }
    
    @Test
    void testCreateMeasurementType() {
        MeasurementTypeRequestDTO request = MeasurementTypeRequestDTO.builder()
                .type("height")
                .unit("cm")
                .isSystem(true)
                .build();
        
        MeasurementType saved = MeasurementType.builder()
                .id(2L)
                .type("height")
                .unit("cm")
                .isSystem(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        when(measurementTypeRepository.save(any(MeasurementType.class)))
                .thenReturn(saved);
        
        MeasurementTypeDTO result = measurementService.createMeasurementType(request);
        
        assertNotNull(result);
        assertEquals("height", result.getType());
        verify(measurementTypeRepository, times(1)).save(any(MeasurementType.class));
    }
    
    @Test
    void testGetUserMeasurements() {
        when(measurementValueRepository.findByUserId(1L))
                .thenReturn(List.of(weightValue));
        
        List<MeasurementValueDTO> result = measurementService.getUserMeasurements(1L);
        
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(75.5, result.get(0).getValue());
    }
    
    @Test
    void testGetUserMeasurementsByType() {
        when(measurementTypeRepository.existsById(1L))
                .thenReturn(true);
        when(measurementValueRepository.findByUserIdAndMeasurementTypeId(1L, 1L))
                .thenReturn(List.of(weightValue));
        
        List<MeasurementValueDTO> result = measurementService.getUserMeasurementsByType(1L, 1L);
        
        assertNotNull(result);
        assertEquals(1, result.size());
    }
    
    @Test
    void testRecordMeasurement() {
        MeasurementValueRequestDTO request = MeasurementValueRequestDTO.builder()
                .measurementTypeId(1L)
                .value(76.0)
                .measurementDate(LocalDate.now())
                .notes("Afternoon weight")
                .build();
        
        when(measurementTypeRepository.existsById(1L))
                .thenReturn(true);
        when(measurementTypeRepository.findById(1L))
                .thenReturn(Optional.of(weight));
        when(measurementValueRepository.save(any(MeasurementValue.class)))
                .thenReturn(weightValue);
        
        MeasurementValueDTO result = measurementService.recordMeasurement(1L, request);
        
        assertNotNull(result);
        assertEquals(75.5, result.getValue());
    }
    
    @Test
    void testDeleteMeasurement() {
        when(measurementValueRepository.findById(1L))
                .thenReturn(Optional.of(weightValue));
        doNothing().when(measurementValueRepository).delete(any(MeasurementValue.class));
        
        measurementService.deleteMeasurement(1L, 1L);
        
        verify(measurementValueRepository, times(1)).delete(any(MeasurementValue.class));
    }
    
    @Test
    void testDeleteMeasurement_Unauthorized() {
        when(measurementValueRepository.findById(1L))
                .thenReturn(Optional.of(weightValue));
        
        assertThrows(IllegalArgumentException.class, 
                () -> measurementService.deleteMeasurement(2L, 1L));
    }
    
    @Test
    void testGetUserMeasurementsByDateRange() {
        LocalDate startDate = LocalDate.now().minusDays(7);
        LocalDate endDate = LocalDate.now();
        
        when(measurementTypeRepository.existsById(1L))
                .thenReturn(true);
        when(measurementValueRepository.findByUserIdAndMeasurementTypeIdAndMeasurementDateBetween(
                1L, 1L, startDate, endDate))
                .thenReturn(List.of(weightValue));
        
        List<MeasurementValueDTO> result = measurementService.getUserMeasurementsByDateRange(
                1L, 1L, startDate, endDate);
        
        assertNotNull(result);
        assertEquals(1, result.size());
    }
}
