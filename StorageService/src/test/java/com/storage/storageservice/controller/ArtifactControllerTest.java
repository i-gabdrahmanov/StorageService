package com.storage.storageservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.storage.storageservice.dto.ArtifactDto;
import com.storage.storageservice.dto.CustomArtifactRequest;
import com.storage.storageservice.dto.EmployeeDto;
import com.storage.storageservice.service.ArtifactService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ArtifactController.class)
class ArtifactControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ArtifactService artifactService;

    @Autowired
    private ObjectMapper objectMapper;

    private ArtifactDto artifactDto;
    private CustomArtifactRequest customRequest;

    @BeforeEach
    void setUp() {
        // Создаем тестовые объекты с правильными данными
        EmployeeDto employee = EmployeeDto.builder()
                .name("Test Employee")
                .build();

        artifactDto = ArtifactDto.builder()
                .name("Test Artifact")
                .surname("Test Surname")
                .payload(new HashMap<>())
                .children(List.of())
                .employee(employee)
                .build();

        customRequest = CustomArtifactRequest.builder()
                .id(UUID.randomUUID())
                .requiredResponseFields(Set.of("field1", "field2"))
                .build();
    }

    @Test
    void addNewArtifact_ShouldReturnNoContent() throws Exception {
        // Given
        ArtifactDto request = ArtifactDto.builder()
                .name("New Artifact")
                .surname("New Surname")
                .payload(new HashMap<>())
                .children(List.of())
                .employee(EmployeeDto.builder().name("name").build())
                .build();

        // When & Then
        mockMvc.perform(post("/api/v2/artifact/new")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(artifactService).addNewArtifact(any(ArtifactDto.class));
    }

    @Test
    void generateSomeArtifacts_ShouldReturnNoContent() throws Exception {
        // Given
        int count = 5;

        // When & Then
        mockMvc.perform(post("/api/v2/artifact/{count}/generate", count)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(artifactService).generateSomeArtifacts(count);
    }

    @Test
    void getByJsonField_ShouldReturnArtifactDto() throws Exception {
        // Given
        String key = "name";
        String value = "Test Artifact";

        when(artifactService.getArtByJsonField(key, value)).thenReturn(artifactDto);

        // When & Then
        mockMvc.perform(get("/api/v2/artifact")
                        .param("key", key)
                        .param("value", value)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Artifact"))
                .andExpect(jsonPath("$.surname").value("Test Surname"));

        verify(artifactService).getArtByJsonField(key, value);
    }

    @Test
    void getByNativeJsonFields_ShouldReturnArtifactDto() throws Exception {
        // Given
        Map<String, Object> request = new HashMap<>();
        request.put("field1", "value1");
        request.put("field2", "value2");

        when(artifactService.getArtByNativeJsonFields(any(Map.class))).thenReturn(artifactDto);

        // When & Then
        mockMvc.perform(post("/api/v2/artifact/json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Artifact"))
                .andExpect(jsonPath("$.surname").value("Test Surname"));

        verify(artifactService).getArtByNativeJsonFields(any(Map.class));
    }

    @Test
    void getCustomRequestById_ShouldReturnArtifactDto() throws Exception {
        // Given
        CustomArtifactRequest request = CustomArtifactRequest.builder()
                .id(UUID.randomUUID())
                .build();

        when(artifactService.getCustomById(any(CustomArtifactRequest.class))).thenReturn(artifactDto);

        // When & Then
        mockMvc.perform(post("/api/v2/artifact/customFields")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Artifact"))
                .andExpect(jsonPath("$.surname").value("Test Surname"));

        verify(artifactService).getCustomById(any(CustomArtifactRequest.class));
    }
}
