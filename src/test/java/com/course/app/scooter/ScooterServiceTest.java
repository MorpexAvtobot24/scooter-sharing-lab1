package com.course.app.scooter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.data.domain.Sort;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScooterServiceTest {

    @Mock
    private ScooterRepository repository;

    @Mock
    private ScooterMapper mapper;

    @InjectMocks
    private ScooterService service;

    @Test
    void findAll_returnsMappedScootersAndSortsById() {
        List<Scooter> scooters = List.of(new Scooter());
        List<ScooterDto> expected = List.of(dto(1L, "SC-1", 85, ScooterStatus.AVAILABLE));
        when(repository.findAll(any(Sort.class))).thenReturn(scooters);
        when(mapper.toDtos(scooters)).thenReturn(expected);

        List<ScooterDto> actual = service.findAll();

        assertSame(expected, actual);
        ArgumentCaptor<Sort> sortCaptor = ArgumentCaptor.forClass(Sort.class);
        verify(repository).findAll(sortCaptor.capture());
        assertEquals("id: ASC", sortCaptor.getValue().toString());
        verify(mapper).toDtos(scooters);
    }

    @Test
    void findById_existingScooter_returnsMappedDto() {
        Scooter scooter = new Scooter();
        ScooterDto expected = dto(4L, "SC-4", 55, ScooterStatus.IN_USE);
        when(repository.findById(4L)).thenReturn(Optional.of(scooter));
        when(mapper.toDto(scooter)).thenReturn(expected);

        ScooterDto actual = service.findById(4L);

        assertSame(expected, actual);
        verify(repository).findById(4L);
        verify(mapper).toDto(scooter);
    }

    @Test
    void findById_missingScooter_throwsNotFound() {
        when(repository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ScooterNotFoundException.class, () -> service.findById(404L));

        verify(repository).findById(404L);
        verifyNoInteractions(mapper);
    }

    @Test
    void create_newSerial_savesMappedEntityAndReturnsDto() {
        ScooterDto request = dto(null, "SC-1", 80, ScooterStatus.AVAILABLE);
        Scooter scooter = new Scooter();
        Scooter savedScooter = new Scooter();
        ScooterDto expected = dto(1L, "SC-1", 80, ScooterStatus.AVAILABLE);
        when(repository.existsBySerialNumber("SC-1")).thenReturn(false);
        when(mapper.toEntity(request)).thenReturn(scooter);
        when(repository.save(scooter)).thenReturn(savedScooter);
        when(mapper.toDto(savedScooter)).thenReturn(expected);

        ScooterDto actual = service.create(request);

        assertSame(expected, actual);
        verify(repository).existsBySerialNumber("SC-1");
        verify(mapper).toEntity(request);
        verify(repository).save(scooter);
        verify(mapper).toDto(savedScooter);
    }

    @Test
    void create_duplicateSerial_throwsConflictException() {
        ScooterDto request = dto(null, "SC-1", 80, ScooterStatus.AVAILABLE);
        when(repository.existsBySerialNumber("SC-1")).thenReturn(true);

        assertThrows(ScooterConflictException.class, () -> service.create(request));

        verify(repository).existsBySerialNumber("SC-1");
        verifyNoInteractions(mapper);
        verify(repository, never()).save(any(Scooter.class));
    }

    @Test
    void update_existingScooterWithUniqueSerial_updatesAndSavesSameEntity() {
        Scooter scooter = new Scooter();
        ScooterDto request = dto(null, "SC-2", 65, ScooterStatus.IN_USE);
        Scooter savedScooter = new Scooter();
        ScooterDto expected = dto(2L, "SC-2", 65, ScooterStatus.IN_USE);
        when(repository.findById(2L)).thenReturn(Optional.of(scooter));
        when(repository.existsBySerialNumberAndIdNot("SC-2", 2L)).thenReturn(false);
        when(repository.save(scooter)).thenReturn(savedScooter);
        when(mapper.toDto(savedScooter)).thenReturn(expected);

        ScooterDto actual = service.update(2L, request);

        assertSame(expected, actual);
        verify(mapper).updateEntity(request, scooter);
        verify(repository).save(scooter);
        verify(mapper).toDto(savedScooter);
    }

    @Test
    void update_missingScooter_throwsNotFoundBeforeCheckingSerial() {
        ScooterDto request = dto(null, "SC-2", 65, ScooterStatus.IN_USE);
        when(repository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(ScooterNotFoundException.class, () -> service.update(2L, request));

        verify(repository, never()).existsBySerialNumberAndIdNot(any(), anyLong());
        verifyNoInteractions(mapper);
    }

    @Test
    void update_duplicateSerial_throwsConflictWithoutChangingScooter() {
        Scooter scooter = new Scooter();
        ScooterDto request = dto(null, "SC-2", 65, ScooterStatus.IN_USE);
        when(repository.findById(2L)).thenReturn(Optional.of(scooter));
        when(repository.existsBySerialNumberAndIdNot("SC-2", 2L)).thenReturn(true);

        assertThrows(ScooterConflictException.class, () -> service.update(2L, request));

        verify(mapper, never()).updateEntity(any(), any());
        verify(repository, never()).save(any());
    }

    @Test
    void delete_existingScooter_deletesLoadedEntity() {
        Scooter scooter = new Scooter();
        when(repository.findById(7L)).thenReturn(Optional.of(scooter));

        service.delete(7L);

        verify(repository).delete(scooter);
    }

    @Test
    void delete_missingScooter_throwsNotFoundWithoutDeleting() {
        when(repository.findById(7L)).thenReturn(Optional.empty());

        assertThrows(ScooterNotFoundException.class, () -> service.delete(7L));

        verify(repository, never()).delete(any());
    }

    @Test
    void isBatteryReadyForRental_returnsTrueAtBoundaryAndFalseBelowOrNull() {
        assertTrue(service.isBatteryReadyForRental(20));
        assertFalse(service.isBatteryReadyForRental(19));
        assertFalse(service.isBatteryReadyForRental(null));
    }

    @Test
    void logExecutionTimeAspect_runsForExternalCallButNotSelfInvocation() {
        AtomicInteger adviceCalls = new AtomicInteger();
        AspectJProxyFactory proxyFactory = new AspectJProxyFactory(service);
        proxyFactory.addAspect(new TestLogExecutionTimeAspect(adviceCalls));
        ScooterService proxy = proxyFactory.getProxy();

        assertTrue(proxy.isBatteryReadyForRental(25));
        assertEquals(1, adviceCalls.get());

        assertTrue(proxy.isBatteryReadyForRentalViaSelfInvocation(25));
        assertEquals(1, adviceCalls.get());
    }

    private static ScooterDto dto(Long id, String serial, Integer battery, ScooterStatus status) {
        return new ScooterDto(id, serial, "Model X", battery, new BigDecimal("0.25"), status);
    }

    @Aspect
    static class TestLogExecutionTimeAspect {
        private final AtomicInteger adviceCalls;

        TestLogExecutionTimeAspect(AtomicInteger adviceCalls) {
            this.adviceCalls = adviceCalls;
        }

        @Around("@annotation(com.course.app.scooter.LogExecutionTime)")
        public Object countInvocations(ProceedingJoinPoint joinPoint) throws Throwable {
            adviceCalls.incrementAndGet();
            return joinPoint.proceed();
        }
    }
}
