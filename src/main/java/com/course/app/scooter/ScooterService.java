package com.course.app.scooter;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ScooterService {

    private final ScooterRepository repository;
    private final ScooterMapper mapper;

    public ScooterService(ScooterRepository repository, ScooterMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<ScooterDto> findAll() {
        return mapper.toDtos(repository.findAll(Sort.by("id")));
    }

    @Transactional(readOnly = true)
    public ScooterDto findById(Long id) {
        return mapper.toDto(getScooter(id));
    }

    public ScooterDto create(ScooterDto scooterDto) {
        if (repository.existsBySerialNumber(scooterDto.serialNumber())) {
            throw new ScooterConflictException(scooterDto.serialNumber());
        }
        return mapper.toDto(repository.save(mapper.toEntity(scooterDto)));
    }

    public ScooterDto update(Long id, ScooterDto scooterDto) {
        Scooter scooter = getScooter(id);
        if (repository.existsBySerialNumberAndIdNot(scooterDto.serialNumber(), id)) {
            throw new ScooterConflictException(scooterDto.serialNumber());
        }
        mapper.updateEntity(scooterDto, scooter);
        return mapper.toDto(repository.save(scooter));
    }

    public void delete(Long id) {
        repository.delete(getScooter(id));
    }

    private Scooter getScooter(Long id) {
        return repository.findById(id).orElseThrow(() -> new ScooterNotFoundException(id));
    }
}
