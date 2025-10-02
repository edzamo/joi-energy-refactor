package uk.tw.energy.domain;

import java.util.List;

public record PricePlan(String planName, String energySupplier, List<Tariff> tariffs) {}
