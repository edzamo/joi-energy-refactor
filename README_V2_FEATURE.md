# Resumen de la Funcionalidad v2: Precios Peak/Off-Peak

Este documento describe la implementación de la historia de usuario para la **versión 2 (v2)** del producto, cuyo objetivo era añadir precios diferenciados para horas punta (peak) y horas valle (off-peak).

## 1. ¿Qué se ha implementado?

El sistema ahora es capaz de calcular el coste del consumo eléctrico aplicando diferentes tarifas según el día de la semana. Esto permite a los proveedores de energía, como "Dr. Evil's Dark Energy", establecer precios más altos durante los fines de semana u otros periodos de alta demanda.

## 2. ¿Por qué es importante este cambio?

- **Realismo del Mercado**: Simula un escenario de mercado energético más realista, donde los precios fluctúan.
- **Flexibilidad del Modelo**: Permite a JOI Energy ofrecer comparaciones de tarifas más sofisticadas y precisas.
- **Cumplimiento de Requisitos**: Satisface el objetivo principal de la v2 del roadmap del producto.

## 3. ¿Cómo se ha implementado?

La implementación se centró en tres áreas clave de la aplicación: el **modelo de dominio**, la **lógica de servicio** y las **pruebas**.

### a. Modelo de Dominio y Configuración (`PricePlan` y `SeedingApplicationDataConfiguration`)

El `record` `PricePlan` ya había sido refactorizado para incluir un `Map<DayOfWeek, BigDecimal> peakTimeMultipliers`.

Para implementar la v2, hemos actualizado la configuración inicial de datos (`SeedingApplicationDataConfiguration`) para que el plan "Dr Evil's Dark Energy" tenga un multiplicador de `x2` los sábados y domingos.

```java
// En SeedingApplicationDataConfiguration.java
new PricePlan(
    "price-plan-0",
    "Dr Evil's Dark Energy",
    BigDecimal.TEN,
    Map.of(
            DayOfWeek.SATURDAY, BigDecimal.valueOf(2),
            DayOfWeek.SUNDAY, BigDecimal.valueOf(2)
    )
),
```

### b. Lógica de Servicio (`PricePlanService`)

Esta fue la parte central del cambio. La lógica de cálculo de costes se ha hecho más inteligente y se ha extraído a un método privado para mayor claridad.

El método `calculateCostForInterval` ahora realiza los siguientes pasos:

1.  **Obtiene el día de la semana** a partir de la fecha de la lectura.
2.  **Busca un multiplicador** en el mapa `peakTimeMultipliers` del plan de precios.
3.  **Usa `getOrDefault(dayOfWeek, BigDecimal.ONE)`**: Si no se encuentra un multiplicador para ese día (es decir, es una hora valle u "off-peak"), se utiliza `1` como valor por defecto, manteniendo la tarifa base.
4.  **Calcula el precio efectivo**: Multiplica la tarifa base por el multiplicador.
5.  **Calcula el coste del intervalo**: Usa este precio efectivo para determinar el coste del consumo en ese pequeño lapso de tiempo.

```java
// En PricePlanService.java
private BigDecimal calculateCostForInterval(
        ElectricityReading currentReading, ElectricityReading nextReading, PricePlan pricePlan) {
    // 1. Calcula la diferencia de tiempo en segundos entre dos lecturas.
    long timeDifferenceInSeconds =
            Duration.between(currentReading.time(), nextReading.time()).getSeconds();
    if (timeDifferenceInSeconds <= 0) {
        return BigDecimal.ZERO;
    }
    // 2. Convierte la diferencia de tiempo a horas.
    BigDecimal timeDifferenceInHours = BigDecimal.valueOf(timeDifferenceInSeconds)
            .divide(BigDecimal.valueOf(3600), 10, RoundingMode.HALF_UP);

    // 3. Obtiene el día de la semana para verificar si aplican precios de horas punta (peak).
    DayOfWeek dayOfWeek =
            currentReading.time().atZone(ZoneId.systemDefault()).getDayOfWeek();
    // 4. Obtiene el multiplicador del plan; si no hay uno para ese día, usa 1 (sin sobrecoste).
    BigDecimal multiplier = pricePlan.peakTimeMultipliers().getOrDefault(dayOfWeek, BigDecimal.ONE);
    // 5. Calcula el precio efectivo aplicando el multiplicador a la tarifa base.
    BigDecimal effectivePrice = pricePlan.unitRate().multiply(multiplier);

    // ... (cálculo del consumo y coste final)
}
```

### c. Pruebas (`EndpointTest` y `PricePlanComparatorControllerTest`)

Se ha añadido un nuevo test de integración (`shouldCalculateHigherPriceForPeakTime`) que verifica específicamente que el coste se duplica en un día de fin de semana para el plan de Dr. Evil. Además, los tests unitarios se han hecho dinámicos para que calculen los costes esperados en función del día en que se ejecutan, haciéndolos más robustos.

## 4. Impacto

El sistema ahora calcula los costes de forma más precisa y realista, sentando una base sólida para futuras funcionalidades como la mensajería al usuario sobre ahorros en horas punta/valle.

