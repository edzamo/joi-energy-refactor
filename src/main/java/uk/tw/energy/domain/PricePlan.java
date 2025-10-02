package uk.tw.energy.domain;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.Map;

public record PricePlan(
        String planName,
        String energySupplier,
        BigDecimal unitRate, // unit price per kWh
        Map<DayOfWeek, BigDecimal> peakTimeMultipliers) {
    public BigDecimal getPrice(LocalDateTime dateTime) {
        BigDecimal multiplier = peakTimeMultipliers.getOrDefault(dateTime.getDayOfWeek(), BigDecimal.ONE);
        return unitRate.multiply(multiplier);
    }
}
