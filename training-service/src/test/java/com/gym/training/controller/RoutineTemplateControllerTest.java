package com.gym.training.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gym.common.dto.PageResponse;
import com.gym.training.config.TestSecurityConfig;
import com.gym.training.dto.RoutineTemplateDTO;
import com.gym.training.dto.RoutineTemplateRequestDTO;
import com.gym.training.entity.RoutineTemplate;
import com.gym.training.entity.RoutineTemplate.TemplateType;
import com.gym.training.service.RoutineTemplateService;
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

@WebMvcTest(RoutineTemplateController.class)
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
class RoutineTemplateControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private RoutineTemplateService routineTemplateService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private RoutineTemplateDTO templateDTO;
    private RoutineTemplateRequestDTO templateRequestDTO;
    
    @BeforeEach
    void setUp() {
        templateDTO = RoutineTemplateDTO.builder()
                .id(1L)
                .name("Full Body Workout")
                .description("Complete full body routine")
                .type(TemplateType.SYSTEM)
                .createdBy(null)
                .exerciseIds(List.of(1L, 2L, 3L))
                .createdAt(LocalDateTime.now())
                .build();
        
        templateRequestDTO = RoutineTemplateRequestDTO.builder()
                .name("Full Body Workout")
                .description("Complete full body routine")
                .type(TemplateType.SYSTEM)
                .exerciseIds(List.of(1L, 2L, 3L))
                .build();
    }
    
    // GET /api/v1/routine-templates/system - Paginated system templates
    @Test
    void testGetAllSystemTemplates_Success() throws Exception {
        Page<RoutineTemplateDTO> page = new PageImpl<>(List.of(templateDTO), PageRequest.of(0, 20), 1);
        PageResponse<RoutineTemplateDTO> pageResponse = PageResponse.of(page);
        
        when(routineTemplateService.getAllSystemTemplates(any(Pageable.class)))
                .thenReturn(pageResponse);
        
        mockMvc.perform(get("/api/v1/routine-templates/system")
                .param("page", "0")
                .param("size", "20")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].name").value("Full Body Workout"))
                .andExpect(jsonPath("$.currentPage").value(0))
                .andDo(print());
        
        verify(routineTemplateService, times(1)).getAllSystemTemplates(any(Pageable.class));
    }
    
    // GET /api/v1/routine-templates/my-templates - User-specific paginated templates
    @Test
    void testGetUserTemplates_Success() throws Exception {
        Page<RoutineTemplateDTO> page = new PageImpl<>(List.of(templateDTO), PageRequest.of(0, 20), 1);
        PageResponse<RoutineTemplateDTO> pageResponse = PageResponse.of(page);
        
        when(routineTemplateService.getUserTemplates(eq(1L), any(Pageable.class)))
                .thenReturn(pageResponse);
        
        mockMvc.perform(get("/api/v1/routine-templates/my-templates")
                .header("X-User-Id", "1")
                .param("page", "0")
                .param("size", "20")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Full Body Workout"))
                .andDo(print());
        
        verify(routineTemplateService, times(1)).getUserTemplates(eq(1L), any(Pageable.class));
    }
    
    // GET /api/v1/routine-templates/{id} - Single template
    @Test
    void testGetTemplateById_Success() throws Exception {
        when(routineTemplateService.getTemplateById(1L))
                .thenReturn(templateDTO);
        
        mockMvc.perform(get("/api/v1/routine-templates/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Full Body Workout"))
                .andDo(print());
        
        verify(routineTemplateService, times(1)).getTemplateById(1L);
    }
    
    @Test
    void testGetTemplateById_NotFound() throws Exception {
        when(routineTemplateService.getTemplateById(999L))
                .thenThrow(new IllegalArgumentException("Template not found: 999"));
        
        mockMvc.perform(get("/api/v1/routine-templates/999")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"))
                .andDo(print());
    }
    
    // POST /api/v1/routine-templates - Create template
    @Test
    void testCreateTemplate_Success() throws Exception {
        RoutineTemplateDTO createdTemplate = RoutineTemplateDTO.builder()
                .id(2L)
                .name("Upper Body Workout")
                .description("Upper body focus routine")
                .type(TemplateType.USER)
                .createdBy(1L)
                .exerciseIds(List.of(1L, 2L))
                .createdAt(LocalDateTime.now())
                .build();
        
        RoutineTemplateRequestDTO createRequest = RoutineTemplateRequestDTO.builder()
                .name("Upper Body Workout")
                .description("Upper body focus routine")
                .type(TemplateType.USER)
                .exerciseIds(List.of(1L, 2L))
                .build();
        
        when(routineTemplateService.createTemplate(any(RoutineTemplateRequestDTO.class), eq(1L)))
                .thenReturn(createdTemplate);
        
        mockMvc.perform(post("/api/v1/routine-templates")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.name").value("Upper Body Workout"))
                .andDo(print());
        
        verify(routineTemplateService, times(1)).createTemplate(any(RoutineTemplateRequestDTO.class), eq(1L));
    }
    
    @Test
    void testCreateTemplate_BadRequest() throws Exception {
        RoutineTemplateRequestDTO createRequest = RoutineTemplateRequestDTO.builder()
                .name("Template")
                .description("Description")
                .type(TemplateType.USER)
                .exerciseIds(List.of(999L))
                .build();
        
        when(routineTemplateService.createTemplate(any(RoutineTemplateRequestDTO.class), eq(1L)))
                .thenThrow(new IllegalArgumentException("Exercise not found: 999"));
        
        mockMvc.perform(post("/api/v1/routine-templates")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andDo(print());
    }
    
    // PUT /api/v1/routine-templates/{id} - Update template
    @Test
    void testUpdateTemplate_Success() throws Exception {
        RoutineTemplateDTO updatedTemplate = RoutineTemplateDTO.builder()
                .id(1L)
                .name("Updated Template")
                .description("Updated description")
                .type(TemplateType.USER)
                .createdBy(1L)
                .exerciseIds(List.of(1L, 2L, 3L, 4L))
                .createdAt(LocalDateTime.now())
                .build();
        
        RoutineTemplateRequestDTO updateRequest = RoutineTemplateRequestDTO.builder()
                .name("Updated Template")
                .description("Updated description")
                .type(TemplateType.USER)
                .exerciseIds(List.of(1L, 2L, 3L, 4L))
                .build();
        
        when(routineTemplateService.updateTemplate(eq(1L), any(RoutineTemplateRequestDTO.class), eq(1L)))
                .thenReturn(updatedTemplate);
        
        mockMvc.perform(put("/api/v1/routine-templates/1")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Template"))
                .andDo(print());
        
        verify(routineTemplateService, times(1)).updateTemplate(eq(1L), any(RoutineTemplateRequestDTO.class), eq(1L));
    }
    
    @Test
    void testUpdateTemplate_Unauthorized() throws Exception {
        RoutineTemplateRequestDTO updateRequest = RoutineTemplateRequestDTO.builder()
                .name("Updated Template")
                .description("Updated description")
                .type(TemplateType.USER)
                .exerciseIds(List.of(1L, 2L))
                .build();
        
        when(routineTemplateService.updateTemplate(eq(1L), any(RoutineTemplateRequestDTO.class), eq(2L)))
                .thenThrow(new IllegalArgumentException("User is unauthorized to update this template"));
        
        mockMvc.perform(put("/api/v1/routine-templates/1")
                .header("X-User-Id", "2")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("FORBIDDEN"))
                .andDo(print());
    }
    
    // DELETE /api/v1/routine-templates/{id} - Delete template
    @Test
    void testDeleteTemplate_Success() throws Exception {
        doNothing().when(routineTemplateService).deleteTemplate(1L, 1L);
        
        mockMvc.perform(delete("/api/v1/routine-templates/1")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent())
                .andDo(print());
        
        verify(routineTemplateService, times(1)).deleteTemplate(1L, 1L);
    }
    
    @Test
    void testDeleteTemplate_NotFound() throws Exception {
        doThrow(new IllegalArgumentException("Template not found: 999"))
                .when(routineTemplateService).deleteTemplate(999L, 1L);
        
        mockMvc.perform(delete("/api/v1/routine-templates/999")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"))
                .andDo(print());
    }
    
    // Test pagination with custom parameters
    @Test
    void testGetAllSystemTemplates_WithCustomPagination() throws Exception {
        Page<RoutineTemplateDTO> page = new PageImpl<>(List.of(templateDTO), PageRequest.of(1, 50), 100);
        PageResponse<RoutineTemplateDTO> pageResponse = PageResponse.of(page);
        
        when(routineTemplateService.getAllSystemTemplates(any(Pageable.class)))
                .thenReturn(pageResponse);
        
        mockMvc.perform(get("/api/v1/routine-templates/system")
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
