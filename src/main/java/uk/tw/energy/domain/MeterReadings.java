package uk.tw.energy.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record MeterReadings(@NotBlank String smartMeterId, @NotEmpty List<ElectricityReading> electricityReadings) {}
