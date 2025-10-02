package uk.tw.energy.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.ZoneId;
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

    /**
     * Constructor para inyectar las dependencias necesarias.
     * @param pricePlans La lista de todos los planes de precios disponibles en el sistema.
     * @param meterReadingService El servicio para acceder a las lecturas de los medidores.
     */
    public PricePlanService(List<PricePlan> pricePlans, MeterReadingService meterReadingService) {
        this.pricePlans = pricePlans;
        this.meterReadingService = meterReadingService;
    }

    /**
     * Calcula el coste del consumo eléctrico para un medidor inteligente específico
     * en comparación con todos los planes de precios disponibles.
     *
     * @param smartMeterId El ID del medidor inteligente.
     * @return Un Optional que contiene un mapa con el ID de cada plan y su coste calculado.
     *         El Optional estará vacío si no se encuentran lecturas para el medidor.
     */
    public Optional<Map<String, BigDecimal>> getConsumptionCostOfElectricityReadingsForEachPricePlan(
            String smartMeterId) {
        // 1. Obtener las lecturas de electricidad para el medidor.
        Optional<List<ElectricityReading>> electricityReadings = meterReadingService.getReadings(smartMeterId);

        // 2. Si hay lecturas, calcular el coste para cada plan de precios.
        return electricityReadings.filter(readings -> !readings.isEmpty()).map(readings -> pricePlans.stream()
                .collect(Collectors.toMap(PricePlan::planName, t -> calculateCost(readings, t))));
    }

    private BigDecimal calculateCost(List<ElectricityReading> electricityReadings, PricePlan pricePlan) {
        // Inicializa el coste total en cero.
        BigDecimal totalCost = BigDecimal.ZERO;
        if (electricityReadings == null || electricityReadings.size() < 2) {
            return totalCost;
        }

        // Ordena las lecturas por tiempo para asegurar un cálculo cronológico.
        List<ElectricityReading> sortedReadings = electricityReadings.stream()
                .sorted(Comparator.comparing(ElectricityReading::time))
                .toList();

        // Itera sobre cada par de lecturas consecutivas para calcular el coste por intervalo.
        for (int i = 0; i < sortedReadings.size() - 1; i++) {
            ElectricityReading currentReading = sortedReadings.get(i);
            ElectricityReading nextReading = sortedReadings.get(i + 1);

            // Calcula el coste para el intervalo y lo suma al total.
            BigDecimal costForInterval = calculateCostForInterval(currentReading, nextReading, pricePlan);
            totalCost = totalCost.add(costForInterval);
        }

        // Redondea el resultado final a 2 decimales, como es estándar para valores monetarios.
        return totalCost.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateCostForInterval(
            ElectricityReading currentReading, ElectricityReading nextReading, PricePlan pricePlan) {
        // 1. Calcula la diferencia de tiempo en segundos entre dos lecturas.
        long timeDifferenceInSeconds =
                Duration.between(currentReading.time(), nextReading.time()).getSeconds();
        if (timeDifferenceInSeconds <= 0) {
            return BigDecimal.ZERO;
        }
        // 2. Convierte la diferencia de tiempo a horas.
        BigDecimal timeDifferenceInHours =
                BigDecimal.valueOf(timeDifferenceInSeconds).divide(BigDecimal.valueOf(3600), 10, RoundingMode.HALF_UP);

        // 3. Obtiene el día de la semana para verificar si aplican precios de horas punta (peak).
        DayOfWeek dayOfWeek =
                currentReading.time().atZone(ZoneId.systemDefault()).getDayOfWeek();
        // 4. Obtiene el multiplicador del plan; si no hay uno para ese día, usa 1 (sin sobrecoste).
        BigDecimal multiplier = pricePlan.peakTimeMultipliers().getOrDefault(dayOfWeek, BigDecimal.ONE);
        // 5. Calcula el precio efectivo aplicando el multiplicador a la tarifa base.
        BigDecimal effectivePrice = pricePlan.unitRate().multiply(multiplier);

        // 6. Calcula la potencia promedio en el intervalo (promedio de las dos lecturas).
        BigDecimal averagePower = currentReading
                .reading()
                .add(nextReading.reading())
                .divide(BigDecimal.valueOf(2), 10, RoundingMode.HALF_UP);
        // 7. Calcula la energía consumida en kWh (Potencia en kW * Tiempo en h).
        BigDecimal consumption = averagePower.multiply(timeDifferenceInHours);
        // 8. Devuelve el coste final para este intervalo (consumo * precio efectivo).
        return consumption.multiply(effectivePrice);
    }
}
