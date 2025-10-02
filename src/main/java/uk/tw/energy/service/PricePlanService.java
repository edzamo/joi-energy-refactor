package uk.tw.energy.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import uk.tw.energy.domain.ElectricityReading;
import uk.tw.energy.domain.PricePlan;

@Service
public class PricePlanService {

    private final List<PricePlan> pricePlans;
    private final MeterReadingService meterReadingService;

    public PricePlanService(List<PricePlan> pricePlans, MeterReadingService meterReadingService) {
        this.pricePlans = pricePlans;
        this.meterReadingService = meterReadingService;
    }

    public Optional<Map<String, BigDecimal>> getConsumptionCostOfElectricityReadingsForEachPricePlan(
            String smartMeterId) {
        Optional<List<ElectricityReading>> electricityReadings = meterReadingService.getReadings(smartMeterId);

        return electricityReadings.filter(readings -> !readings.isEmpty()).map(readings -> pricePlans.stream()
                .collect(Collectors.toMap(PricePlan::planName, t -> calculateCost(readings, t))));
    }

    private BigDecimal calculateCost(List<ElectricityReading> electricityReadings, PricePlan pricePlan) {
        final BigDecimal averageReadingInKw = calculateAverageReading(electricityReadings);
        final BigDecimal usageTimeInHours = calculateUsageTimeInHours(electricityReadings);
        final BigDecimal energyConsumedInKwH = averageReadingInKw.multiply(usageTimeInHours);
        final BigDecimal cost = energyConsumedInKwH.multiply(pricePlan.unitRate());
        return cost.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateAverageReading(List<ElectricityReading> electricityReadings) {
        BigDecimal summedReadings =
                electricityReadings.stream().map(ElectricityReading::reading).reduce(BigDecimal.ZERO, BigDecimal::add);

        return summedReadings.divide(BigDecimal.valueOf(electricityReadings.size()), RoundingMode.HALF_UP);
    }

    private BigDecimal calculateUsageTimeInHours(List<ElectricityReading> electricityReadings) {
        ElectricityReading first = electricityReadings.stream()
                .min(Comparator.comparing(ElectricityReading::time))
                .get();

        ElectricityReading last = electricityReadings.stream()
                .max(Comparator.comparing(ElectricityReading::time))
                .get();

        return BigDecimal.valueOf(Duration.between(first.time(), last.time()).getSeconds() / 3600.0);
    }
}
