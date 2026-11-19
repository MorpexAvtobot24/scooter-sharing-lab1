package com.course.app.scooter;

import jakarta.validation.Valid;
import com.course.app.web.CurrentUser;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/scooters")
public class ScooterController {

    private final ScooterService service;

    public ScooterController(ScooterService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<ScooterDto>> findAll(@CurrentUser String actor) {
        return ResponseEntity.ok().header("X-Actor", actor).body(service.findAll());
    }

    @GetMapping("/{id}")
    public ScooterDto findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @AdminPostEndpoint
    public ResponseEntity<ScooterDto> create(@Valid @RequestBody ScooterDto scooterDto) {
        ScooterDto created = service.create(scooterDto);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ScooterDto update(@PathVariable Long id, @Valid @RequestBody ScooterDto scooterDto) {
        return service.update(id, scooterDto);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
