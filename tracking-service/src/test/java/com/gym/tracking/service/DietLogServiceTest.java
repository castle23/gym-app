package com.gym.tracking.service;

import com.gym.tracking.dto.DietLogDTO;
import com.gym.tracking.dto.DietLogRequestDTO;
import com.gym.tracking.entity.DietLog;
import com.gym.tracking.repository.DietLogRepository;
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
class DietLogServiceTest {
    
    @Mock
    private DietLogRepository dietLogRepository;
    
    @InjectMocks
    private DietLogService dietLogService;
    
    private DietLog dietLog;
    
    @BeforeEach
    void setUp() {
        dietLog = DietLog.builder()
                .id(1L)
                .userId(1L)
                .logDate(LocalDate.now())
                .meal("Breakfast")
                .foodItems("Eggs, Toast, Coffee")
                .calories(500.0)
                .macros("20-10-70")
                .notes("Healthy start")
                .createdAt(LocalDateTime.now())
                .build();
    }
    
    @Test
    void testGetDietLogById_Success() {
        when(dietLogRepository.findById(1L))
                .thenReturn(Optional.of(dietLog));
        
        DietLogDTO result = dietLogService.getDietLogById(1L, 1L);
        
        assertNotNull(result);
        assertEquals("Breakfast", result.getMeal());
    }
    
    @Test
    void testGetDietLogById_NotFound() {
        when(dietLogRepository.findById(999L))
                .thenReturn(Optional.empty());
        
        assertThrows(IllegalArgumentException.class,
                () -> dietLogService.getDietLogById(999L, 1L));
    }
    
    @Test
    void testGetDietLogById_Unauthorized() {
        when(dietLogRepository.findById(1L))
                .thenReturn(Optional.of(dietLog));
        
        assertThrows(IllegalArgumentException.class,
                () -> dietLogService.getDietLogById(1L, 2L));
    }
    
    @Test
    void testGetUserDietLogs() {
        when(dietLogRepository.findByUserId(1L))
                .thenReturn(List.of(dietLog));
        
        List<DietLogDTO> result = dietLogService.getUserDietLogs(1L);
        
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Breakfast", result.get(0).getMeal());
    }
    
    @Test
    void testGetDietLogsByDate() {
        LocalDate date = LocalDate.now();
        when(dietLogRepository.findByUserIdAndLogDate(1L, date))
                .thenReturn(List.of(dietLog));
        
        List<DietLogDTO> result = dietLogService.getDietLogsByDate(1L, date);
        
        assertNotNull(result);
        assertEquals(1, result.size());
    }
    
    @Test
    void testGetDietLogsByDateRange() {
        LocalDate startDate = LocalDate.now().minusDays(7);
        LocalDate endDate = LocalDate.now();
        
        when(dietLogRepository.findByUserIdAndLogDateBetween(1L, startDate, endDate))
                .thenReturn(List.of(dietLog));
        
        List<DietLogDTO> result = dietLogService.getDietLogsByDateRange(1L, startDate, endDate);
        
        assertNotNull(result);
        assertEquals(1, result.size());
    }
    
    @Test
    void testCreateDietLog() {
        DietLogRequestDTO request = DietLogRequestDTO.builder()
                .logDate(LocalDate.now())
                .meal("Lunch")
                .foodItems("Chicken, Rice, Vegetables")
                .calories(600.0)
                .macros("40-30-30")
                .notes("Good lunch")
                .build();
        
        when(dietLogRepository.save(any(DietLog.class)))
                .thenReturn(dietLog);
        
        DietLogDTO result = dietLogService.createDietLog(1L, request);
        
        assertNotNull(result);
        assertEquals("Breakfast", result.getMeal());
    }
    
    @Test
    void testUpdateDietLog() {
        DietLogRequestDTO request = DietLogRequestDTO.builder()
                .logDate(LocalDate.now())
                .meal("Updated Breakfast")
                .foodItems("Updated items")
                .calories(550.0)
                .macros("25-15-60")
                .notes("Updated notes")
                .build();
        
        when(dietLogRepository.findById(1L))
                .thenReturn(Optional.of(dietLog));
        when(dietLogRepository.save(any(DietLog.class)))
                .thenReturn(dietLog);
        
        DietLogDTO result = dietLogService.updateDietLog(1L, 1L, request);
        
        assertNotNull(result);
        verify(dietLogRepository, times(1)).save(any(DietLog.class));
    }
    
    @Test
    void testDeleteDietLog() {
        when(dietLogRepository.findById(1L))
                .thenReturn(Optional.of(dietLog));
        doNothing().when(dietLogRepository).delete(any(DietLog.class));
        
        dietLogService.deleteDietLog(1L, 1L);
        
        verify(dietLogRepository, times(1)).delete(any(DietLog.class));
    }
    
    @Test
    void testDeleteDietLog_Unauthorized() {
        when(dietLogRepository.findById(1L))
                .thenReturn(Optional.of(dietLog));
        
        assertThrows(IllegalArgumentException.class,
                () -> dietLogService.deleteDietLog(1L, 2L));
    }
}
