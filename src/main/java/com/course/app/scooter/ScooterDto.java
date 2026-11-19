package com.course.app.scooter;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@ValidScooterState
public record ScooterDto(
        Long id,
        @NotBlank @Size(max = 64) String serialNumber,
        @NotBlank @Size(max = 120) String model,
        @NotNull @Min(0) @Max(100) Integer batteryLevel,
        @NotNull @DecimalMin("0.01") @Digits(integer = 6, fraction = 2) BigDecimal pricePerMinute,
        @NotNull ScooterStatus status
) {
}
