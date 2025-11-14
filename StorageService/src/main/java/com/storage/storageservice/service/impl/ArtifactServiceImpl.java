package com.storage.storageservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.storage.storageservice.dto.ArtifactDto;
import com.storage.storageservice.dto.CustomArtifactRequest;
import com.storage.storageservice.mapper.ArtifactMapper;
import com.storage.storageservice.model.Artifact;
import com.storage.storageservice.repository.ArtifactRepository;
import com.storage.storageservice.service.ArtifactBatchService;
import com.storage.storageservice.service.ArtifactCacheService;
import com.storage.storageservice.service.ArtifactService;
import com.storage.storageservice.utils.DynamicDtoMapper;
import jakarta.persistence.Tuple;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Slf4j
@RequiredArgsConstructor
public class ArtifactServiceImpl implements ArtifactService {

    private final ArtifactRepository artifactRepository;
    private final DynamicDtoMapper mapper;
    private final ObjectMapper objectMapper;
    private final ArtifactCacheService artifactCacheService;
    private final ArtifactMapper artifactMapper;
    private final ArtifactBatchService artifactBatchService;

    @Override
    @Transactional
    public void addNewArtifact(ArtifactDto dto) {
        addNewArtifactRecursive(dto, null);
    }

    @Override
    @Transactional
    public void generateSomeArtifacts(int count) {
        final int BATCH_SIZE = 50;
        List<Artifact> batch = new ArrayList<>(BATCH_SIZE);

        for (int i = 0; i < count; i++) {
            Artifact artifact = getRandomArtifact();

            batch.add(artifact);
            if (batch.size() == BATCH_SIZE) {
                artifactBatchService.saveBatch(new ArrayList<>(batch));
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            artifactBatchService.saveBatch(batch);
        }
    }

    private Artifact getRandomArtifact() {
        Artifact parent = Artifact.builder()
                .name(generateRandomString(20))
                .surname(generateRandomString(20))
                .payload(Map.of(
                        "series", generateRandomString(20),
                        "department", generateRandomString(20),
                        "price", ThreadLocalRandom.current().nextInt()
                ))
                .build();

        parent.addChild(Artifact.builder()
                .name("Random")
                .surname("Random")
                .payload(Map.of(
                        "series", generateRandomString(20),
                        "department", generateRandomString(20),
                        "price", ThreadLocalRandom.current().nextInt()
                ))
                .build());

        parent.addChild(Artifact.builder()
                .name("Random")
                .surname("Random")
                .payload(Map.of(
                        "series", generateRandomString(20),
                        "department", generateRandomString(20),
                        "price", ThreadLocalRandom.current().nextInt()
                ))
                .build());

        return parent;
    }



    @Override
    @Transactional(readOnly = true)
    public ArtifactDto getArtByJsonField(String key, String value) {
        Artifact art = artifactRepository.findByJsonField(key, value);
        // TODO to mapper
        return ArtifactDto.builder()
                .name(art.getName())
                .surname(art.getSurname())
                .payload(art.getPayload())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ArtifactDto getArtByNativeJsonFields(Map<String, Object> request) {
        String json;
        try {
            json = objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        Artifact art = artifactRepository.findByJson(json);
        return ArtifactDto.builder()
                .name(art.getName())
                .surname(art.getSurname())
                .payload(art.getPayload())
                .build();
    }

    @Override
    @Transactional
    public ArtifactDto getCustomById(CustomArtifactRequest request) {
        Tuple tuple = artifactRepository.findProjectedById(request.getId(), request.getRequiredResponseFields());
        return mapper.mapToDto(tuple, request.getRequiredResponseFields(), ArtifactDto.class);
    }

    @Transactional
    @Override
    public ArtifactDto getById(UUID id) {
        ArtifactDto artifactDto = artifactCacheService.get(id.toString());
        if (artifactDto != null) {
            log.info("From redis cache");
            return artifactDto;
        }
        Artifact artifact = artifactRepository.findById(id).orElseThrow();
        return artifactMapper.toDto(artifact);
    }

    private void addNewArtifactRecursive(ArtifactDto dto, Artifact parent) {
        Artifact artifact = new Artifact();
        artifact.setName(dto.getName());
        artifact.setSurname(dto.getSurname());
        artifact.setPayload(dto.getPayload());
        if (parent != null) {
            artifact.setParent(parent);
        }
        if (dto.getChildren() != null) {
            dto.getChildren().forEach(c -> addNewArtifactRecursive(c, artifact));
        }
        artifactRepository.save(artifact);
    }

    private String generateRandomString(int length) {
        return ThreadLocalRandom.current()
                .ints(length, 'a', 'z' + 1)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }
}
