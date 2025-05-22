package com.storage.storageservice.service;

import com.storage.storageservice.dto.ContractDto;

public interface ContractService {

    void addNewContract(ContractDto dto);

    ContractDto findByName(String name);

    void generateContracts(int counter);
}