package uk.tw.energy.controller;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.tw.energy.domain.ElectricityReading;
import uk.tw.energy.domain.PricePlan;
import uk.tw.energy.service.AccountService;
import uk.tw.energy.service.MeterReadingService;
import uk.tw.energy.service.PricePlanService;

class PricePlanComparatorControllerTest {
    private static final String WORST_PLAN_ID = "worst-supplier";
    private static final String BEST_PLAN_ID = "best-supplier";
    private static final String SECOND_BEST_PLAN_ID = "second-best-supplier";
    private static final String SMART_METER_ID = "smart-meter-id";
    private PricePlanComparatorController controller;
    private MeterReadingService meterReadingService;
    private AccountService accountService;

    @BeforeEach
    void setUp() {
        meterReadingService = new MeterReadingService(new HashMap<>());

        Map<DayOfWeek, BigDecimal> worstSupplierPeak = Map.of(
                DayOfWeek.SATURDAY, new BigDecimal("1.5"),
                DayOfWeek.SUNDAY, new BigDecimal("1.5"));
        Map<DayOfWeek, BigDecimal> secondBestSupplierPeak = Map.of(
                DayOfWeek.MONDAY, new BigDecimal("1.2"),
                DayOfWeek.TUESDAY, new BigDecimal("1.2"),
                DayOfWeek.WEDNESDAY, new BigDecimal("1.2"),
                DayOfWeek.THURSDAY, new BigDecimal("1.2"),
                DayOfWeek.FRIDAY, new BigDecimal("1.2"));
        PricePlan pricePlan1 = new PricePlan(WORST_PLAN_ID, "Worst Supplier", BigDecimal.TEN, worstSupplierPeak);
        PricePlan pricePlan2 = new PricePlan(BEST_PLAN_ID, "Best Supplier", BigDecimal.ONE, emptyMap());
        PricePlan pricePlan3 = new PricePlan(
                SECOND_BEST_PLAN_ID, "Second Best Supplier", BigDecimal.valueOf(2), secondBestSupplierPeak);
        List<PricePlan> pricePlans = List.of(pricePlan1, pricePlan2, pricePlan3);
        PricePlanService pricePlanService = new PricePlanService(pricePlans, meterReadingService);

        accountService = new AccountService(Map.of(SMART_METER_ID, WORST_PLAN_ID));

        controller = new PricePlanComparatorController(pricePlanService, accountService);
    }

    @Test
    void calculatedCostForEachPricePlan_happyPath() {
        // Arrange
        var readingTime = Instant.now().minusSeconds(3600);
        var electricityReading = new ElectricityReading(readingTime, BigDecimal.valueOf(15.0));
        var otherReading = new ElectricityReading(Instant.now(), BigDecimal.valueOf(5.0));
        meterReadingService.storeReadings(SMART_METER_ID, List.of(electricityReading, otherReading));

        // Act
        ResponseEntity<Map<String, Object>> response = controller.calculatedCostForEachPricePlan(SMART_METER_ID);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Calculate expected costs dynamically to make the test robust against day-of-week changes
        DayOfWeek day = readingTime.atZone(ZoneId.systemDefault()).getDayOfWeek();
        BigDecimal consumption = BigDecimal.valueOf(10.0); // (15+5)/2 * 1 hour

        BigDecimal worstPlanCost = day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY
                ? new BigDecimal("150.00")
                : new BigDecimal("100.00");
        BigDecimal bestPlanCost = new BigDecimal("10.00");
        BigDecimal secondBestPlanCost = day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY
                ? new BigDecimal("20.00")
                : new BigDecimal("24.00");

        Map<String, Object> expected = Map.of(
                PricePlanComparatorController.PRICE_PLAN_ID_KEY,
                WORST_PLAN_ID,
                PricePlanComparatorController.PRICE_PLAN_COMPARISONS_KEY,
                Map.of(
                        WORST_PLAN_ID,
                        worstPlanCost,
                        BEST_PLAN_ID,
                        bestPlanCost,
                        SECOND_BEST_PLAN_ID,
                        secondBestPlanCost));
        assertThat(response.getBody()).isEqualTo(expected);
    }

    @Test
    public void calculatedCostForEachPricePlan_noReadings() {
        ResponseEntity<Map<String, Object>> response = controller.calculatedCostForEachPricePlan("not-found");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void recommendCheapestPricePlans_noLimit() {
        var electricityReading = new ElectricityReading(Instant.now().minusSeconds(1800), BigDecimal.valueOf(35.0));
        var otherReading = new ElectricityReading(Instant.now(), BigDecimal.valueOf(3.0));
        meterReadingService.storeReadings(SMART_METER_ID, List.of(electricityReading, otherReading));

        ResponseEntity<List<Map.Entry<String, BigDecimal>>> response =
                controller.recommendCheapestPricePlans(SMART_METER_ID, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        var expectedPricePlanToCost = List.of(
                new AbstractMap.SimpleEntry<>(BEST_PLAN_ID, new BigDecimal("9.50")),
                new AbstractMap.SimpleEntry<>(SECOND_BEST_PLAN_ID, new BigDecimal("22.80")),
                new AbstractMap.SimpleEntry<>(WORST_PLAN_ID, new BigDecimal("95.00")));
        assertThat(response.getBody()).isEqualTo(expectedPricePlanToCost);
    }

    @Test
    void recommendCheapestPricePlans_withLimit() {
        var electricityReading = new ElectricityReading(Instant.now().minusSeconds(2700), BigDecimal.valueOf(5.0));
        var otherReading = new ElectricityReading(Instant.now(), BigDecimal.valueOf(20.0));
        meterReadingService.storeReadings(SMART_METER_ID, List.of(electricityReading, otherReading));

        ResponseEntity<List<Map.Entry<String, BigDecimal>>> response =
                controller.recommendCheapestPricePlans(SMART_METER_ID, 2);

        var expectedPricePlanToCost = List.of(
                new AbstractMap.SimpleEntry<>(BEST_PLAN_ID, new BigDecimal("9.38")),
                new AbstractMap.SimpleEntry<>(SECOND_BEST_PLAN_ID, new BigDecimal("22.50")));
        assertThat(response.getBody()).isEqualTo(expectedPricePlanToCost);
    }

    @Test
    void recommendCheapestPricePlans_limitHigherThanNumberOfEntries() {
        var reading0 = new ElectricityReading(Instant.now().minusSeconds(3600), BigDecimal.valueOf(25.0));
        var reading1 = new ElectricityReading(Instant.now(), BigDecimal.valueOf(3.0));
        meterReadingService.storeReadings(SMART_METER_ID, List.of(reading0, reading1));

        ResponseEntity<List<Map.Entry<String, BigDecimal>>> response =
                controller.recommendCheapestPricePlans(SMART_METER_ID, 5);

        var expectedPricePlanToCost = List.of(
                new AbstractMap.SimpleEntry<>(BEST_PLAN_ID, new BigDecimal("14.00")),
                new AbstractMap.SimpleEntry<>(SECOND_BEST_PLAN_ID, new BigDecimal("33.60")),
                new AbstractMap.SimpleEntry<>(WORST_PLAN_ID, new BigDecimal("140.00")));
        assertThat(response.getBody()).isEqualTo(expectedPricePlanToCost);
    }
}
