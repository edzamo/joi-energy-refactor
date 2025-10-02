package uk.tw.energy;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.tw.energy.domain.ElectricityReading;
import uk.tw.energy.domain.EnergyType;
import uk.tw.energy.domain.PricePlan;
import uk.tw.energy.domain.Tariff;
import uk.tw.energy.generator.ElectricityReadingsGenerator;

@Configuration
public class SeedingApplicationDataConfiguration {

    private static final String MOST_EVIL_PRICE_PLAN_ID = "price-plan-0";
    private static final String RENEWABLES_PRICE_PLAN_ID = "price-plan-1";
    private static final String STANDARD_PRICE_PLAN_ID = "price-plan-2";

    @Bean
    public List<PricePlan> pricePlans() {
        return List.of(
                // Plan solo de electricidad
                new PricePlan(
                        MOST_EVIL_PRICE_PLAN_ID,
                        "Dr Evil's Dark Energy",
                        List.of(new Tariff(EnergyType.ELECTRICITY, BigDecimal.TEN))),
                // Plan dual (electricidad y gas)
                new PricePlan(
                        RENEWABLES_PRICE_PLAN_ID,
                        "The Green Eco",
                        List.of(
                                new Tariff(EnergyType.ELECTRICITY, BigDecimal.valueOf(2)),
                                new Tariff(EnergyType.GAS, BigDecimal.valueOf(1.5)))),
                // Plan solo de electricidad
                new PricePlan(
                        STANDARD_PRICE_PLAN_ID,
                        "Power for Everyone",
                        List.of(new Tariff(EnergyType.ELECTRICITY, BigDecimal.ONE))));
    }

    @Bean
    public Map<String, List<ElectricityReading>> perMeterElectricityReadings() {
        final Map<String, List<ElectricityReading>> readings = new HashMap<>();
        final ElectricityReadingsGenerator electricityReadingsGenerator = new ElectricityReadingsGenerator();
        smartMeterToPricePlanAccounts()
                .keySet()
                .forEach(smartMeterId -> readings.put(smartMeterId, electricityReadingsGenerator.generate(20)));
        return readings;
    }

    @Bean
    public Map<String, String> smartMeterToPricePlanAccounts() {
        final Map<String, String> smartMeterToPricePlanAccounts = new HashMap<>();
        smartMeterToPricePlanAccounts.put("smart-meter-0", MOST_EVIL_PRICE_PLAN_ID);
        smartMeterToPricePlanAccounts.put("smart-meter-1", RENEWABLES_PRICE_PLAN_ID);
        smartMeterToPricePlanAccounts.put("smart-meter-2", MOST_EVIL_PRICE_PLAN_ID);
        smartMeterToPricePlanAccounts.put("smart-meter-3", STANDARD_PRICE_PLAN_ID);
        smartMeterToPricePlanAccounts.put("smart-meter-4", RENEWABLES_PRICE_PLAN_ID);
        return smartMeterToPricePlanAccounts;
    }
}
