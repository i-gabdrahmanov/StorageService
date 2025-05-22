package com.storage.storageservice.service.impl;

import com.storage.storageservice.dto.ContractDto;
import com.storage.storageservice.model.Contract;
import com.storage.storageservice.repository.ContractRepository;
import com.storage.storageservice.service.ContractService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class ContractServiceImpl implements ContractService {

    private final ContractRepository contractRepository;

    @Override
    @Transactional
    public void addNewContract(ContractDto dto) {
        Contract contract = new Contract();
        contract.setContractText(dto.getContractText());
        contract.setName(dto.getName());
        contract.setSurname(dto.getSurname());
        contractRepository.save(contract);
    }

    @Override
    @Transactional(readOnly = true)
    public ContractDto findByName(String name) {
        Contract contract = contractRepository.findByName(name)
                .orElseThrow(() -> new RuntimeException("Контракт не найден"));
        return new ContractDto(
                contract.getName(),
                contract.getSurname(),
                contract.getContractText()
        );
    }

    @Override
    @Transactional
    public void generateContracts(int count) {
        List<Contract> resultList = IntStream.range(0, count)
                .parallel() // Добавлена параллельная обработка
                .mapToObj(i -> {
                    Contract contract = new Contract();
                    contract.setName(generateRandomString(40));
                    contract.setSurname(generateRandomString(40));
                    contract.setContractText(generateRandomString(100));
                    return contract;
                })
                .collect(Collectors.toList());

        contractRepository.saveAll(resultList);
    }

    private String generateRandomString(int length) {
        return ThreadLocalRandom.current()
                .ints(length, 'a', 'z' + 1)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }
}
