package com.storage.storageservice.controller;

import com.storage.storageservice.dto.ArtifactDto;
import com.storage.storageservice.service.ArtifactService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v2/artifact")
@RequiredArgsConstructor
public class ArtifactController {

    private final ArtifactService service;

    @PostMapping(value = "new")
    public void addNewArtifact(@RequestBody ArtifactDto request) {
        service.addNewArtifact(request);
    }
}
