package com.storage.storageservice.mapper;

import com.storage.storageservice.dto.ArtifactDto;
import com.storage.storageservice.model.Artifact;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ArtifactMapper {

    ArtifactDto toDto(Artifact artifact);

    Artifact toEntity(ArtifactDto artifactDto);
}
