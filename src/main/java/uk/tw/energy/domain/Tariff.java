package uk.tw.energy.domain;

import java.math.BigDecimal;

public record Tariff(EnergyType energyType, BigDecimal unitRate) {}
