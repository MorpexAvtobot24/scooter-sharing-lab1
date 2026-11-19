package com.course.app.scooter;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ScooterMapper {

    ScooterDto toDto(Scooter scooter);

    List<ScooterDto> toDtos(List<Scooter> scooters);

    @Mapping(target = "id", ignore = true)
    Scooter toEntity(ScooterDto scooterDto);

    @Mapping(target = "id", ignore = true)
    void updateEntity(ScooterDto scooterDto, @MappingTarget Scooter scooter);
}
