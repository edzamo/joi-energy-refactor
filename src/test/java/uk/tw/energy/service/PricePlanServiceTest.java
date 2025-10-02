package uk.tw.energy.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import uk.tw.energy.domain.ElectricityReading;
import uk.tw.energy.domain.EnergyType;
import uk.tw.energy.domain.PricePlan;
import uk.tw.energy.domain.Tariff;

 class PricePlanServiceTest {

    private MeterReadingService meterReadingService;

    // El servicio que vamos a probar
    private PricePlanService pricePlanService;

    // Constantes para los datos de prueba
    private static final String SMART_METER_ID = "smart-meter-1";
    private static final String PLAN_A_ID = "plan-a";
    private static final String PLAN_B_ID = "plan-b";
    private static final String PLAN_C_GAS_ONLY_ID = "plan-c-gas-only";

    @BeforeEach
     void setUp() {
        // 1. Instanciar una dependencia real en lugar de un mock
        meterReadingService = new MeterReadingService(new HashMap<>());

        // 2. Crear los planes de precios que usaremos en los tests
        PricePlan planA = new PricePlan(PLAN_A_ID, "Supplier A",
                List.of(new Tariff(EnergyType.ELECTRICITY, BigDecimal.TEN))); // Tarifa: 10

        PricePlan planB = new PricePlan(PLAN_B_ID, "Supplier B",
                List.of(new Tariff(EnergyType.ELECTRICITY, BigDecimal.ONE))); // Tarifa: 1

        PricePlan planCGasOnly = new PricePlan(PLAN_C_GAS_ONLY_ID, "Supplier C",
                List.of(new Tariff(EnergyType.GAS, BigDecimal.valueOf(5)))); // Sin tarifa de electricidad

        List<PricePlan> pricePlans = List.of(planA, planB, planCGasOnly);

        // 3. Inicializar el servicio bajo prueba con sus dependencias
        pricePlanService = new PricePlanService(pricePlans, meterReadingService);
    }

    @Test
     void givenReadingsExistShouldCalculateCostForEachPlan() {
        // Arrange: Preparamos el escenario
        // Simulamos un consumo de 10 kWh durante 1 hora
        var reading1 = new ElectricityReading(Instant.now().minusSeconds(3600), BigDecimal.valueOf(15.0));
        var reading2 = new ElectricityReading(Instant.now(), BigDecimal.valueOf(5.0));
        List<ElectricityReading> readings = List.of(reading1, reading2);

        // Usamos el servicio real para almacenar las lecturas
        meterReadingService.storeReadings(SMART_METER_ID, readings);

        // Act: Ejecutamos el método que queremos probar
        Optional<Map<String, BigDecimal>> costs =
                pricePlanService.getConsumptionCostOfElectricityReadingsForEachPricePlan(SMART_METER_ID);

        // Assert: Verificamos que el resultado es el esperado
        assertThat(costs).isPresent();
        Map<String, BigDecimal> costMap = costs.get();

        // Consumo: (15+5)/2 * 1h = 10 kWh
        // Coste Plan A: 10 kWh * 10 = 100.00
        // Coste Plan B: 10 kWh * 1 = 10.00
        // Coste Plan C: 0.00 (no tiene tarifa de electricidad)
        assertThat(costMap.get(PLAN_A_ID)).isEqualByComparingTo("100.00");
        assertThat(costMap.get(PLAN_B_ID)).isEqualByComparingTo("10.00");
        assertThat(costMap.get(PLAN_C_GAS_ONLY_ID)).isEqualByComparingTo("0.00");
    }

    @Test
     void givenNoReadingsExistShouldReturnEmptyOptional() {
        // Arrange: No almacenamos ninguna lectura para el SMART_METER_ID,
        // por lo que el servicio real devolverá un Optional vacío.

        // Act: Ejecutamos el método
        Optional<Map<String, BigDecimal>> costs =
                pricePlanService.getConsumptionCostOfElectricityReadingsForEachPricePlan(SMART_METER_ID);

        // Assert: Verificamos que el resultado es un Optional vacío
        assertThat(costs).isEmpty();
    }

    @Test
     void givenInsufficientReadingsShouldCalculateZeroCost() {
        // Arrange: Devolvemos una lista con una sola lectura (insuficiente para calcular consumo)
        var reading1 = new ElectricityReading(Instant.now(), BigDecimal.valueOf(10.0));
        meterReadingService.storeReadings(SMART_METER_ID, List.of(reading1));

        // Act: Ejecutamos el método
        Optional<Map<String, BigDecimal>> costs =
                pricePlanService.getConsumptionCostOfElectricityReadingsForEachPricePlan(SMART_METER_ID);

        // Assert: Verificamos que el coste es cero para todos los planes
        assertThat(costs).isPresent();
        Map<String, BigDecimal> costMap = costs.get();
        assertThat(costMap.get(PLAN_A_ID)).isEqualByComparingTo("0.00");
        assertThat(costMap.get(PLAN_B_ID)).isEqualByComparingTo("0.00");
    }
}