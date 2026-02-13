package com.storage.storageservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.storage.storageservice.dto.ContractDto;
import com.storage.storageservice.service.ContractService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class ContractControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private ContractService contractService;

    @InjectMocks
    private ContractController contractController;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(contractController).build();
    }

    @Test
    void addNewContract_ShouldReturn200() throws Exception {
        ContractDto contractDto = new ContractDto();
        contractDto.setName("test-contract");
        contractDto.setContractText("Test contract text");

        mockMvc.perform(post("/api/v2/contract/new")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(contractDto)))
                .andExpect(status().isOk());

        verify(contractService, times(1)).addNewContract(contractDto);
    }

    @Test
    void getByName_ShouldReturnContractDto() throws Exception {
        ContractDto contractDto = new ContractDto();
        contractDto.setName("test-contract");
        contractDto.setContractText("Test contract text");

        when(contractService.findByName("test-contract")).thenReturn(contractDto);

        mockMvc.perform(get("/api/v2/contract")
                .param("name", "test-contract"))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(contractDto)));

        verify(contractService, times(1)).findByName("test-contract");
    }

    @Test
    void generateContracts_ShouldReturn200() throws Exception {
        int contractCount = 5;
        
        mockMvc.perform(post("/api/v2/contract/" + contractCount + "/generate"))
                .andExpect(status().isOk());

        verify(contractService, times(1)).generateContracts(contractCount);
    }
}