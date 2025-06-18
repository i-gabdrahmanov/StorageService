package com.storage.storageservice.controller;

import com.storage.storageservice.dto.ArtifactDto;
import com.storage.storageservice.service.ArtifactService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v2/artifact")
@RequiredArgsConstructor
public class ArtifactController {

    private final ArtifactService service;

    @PostMapping(value = "new")
    public void addNewArtifact(@RequestBody ArtifactDto request) {
        service.addNewArtifact(request);
    }

    @PostMapping(value = "{count}/generate")
    public void generateSomeArtifacts(@PathVariable int count) {
        service.generateSomeArtifacts(count);
    }

    @GetMapping
    public ResponseEntity<ArtifactDto> getByJsonDepartment(
            @RequestParam String key,
            @RequestParam String value
    ) {
        ArtifactDto artByJsonName = service.getArtByJsonField(key, value);
        return ResponseEntity.ok(artByJsonName);
    }
}
