package uk.tw.energy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import uk.tw.energy.builders.MeterReadingsBuilder;
import uk.tw.energy.domain.ElectricityReading;
import uk.tw.energy.domain.MeterReadings;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = App.class)
public class EndpointTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private static HttpEntity<MeterReadings> toHttpEntity(MeterReadings meterReadings) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(meterReadings, headers);
    }

    @Test
    public void shouldStoreReadings() {
        MeterReadings meterReadings =
                new MeterReadingsBuilder().generateElectricityReadings().build();
        HttpEntity<MeterReadings> entity = toHttpEntity(meterReadings);

        ResponseEntity<String> response = restTemplate.postForEntity("/readings/store", entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @SuppressWarnings("DataFlowIssue")
    @Test
    public void givenMeterIdShouldReturnAMeterReadingAssociatedWithMeterId() {
        String smartMeterId = "alice";
        List<ElectricityReading> data = List.of(
                new ElectricityReading(Instant.parse("2024-04-26T00:00:10.00Z"), new BigDecimal(10)),
                new ElectricityReading(Instant.parse("2024-04-26T00:00:20.00Z"), new BigDecimal(20)),
                new ElectricityReading(Instant.parse("2024-04-26T00:00:30.00Z"), new BigDecimal(30)));
        populateReadingsForMeter(smartMeterId, data);

        ResponseEntity<ElectricityReading[]> response =
                restTemplate.getForEntity("/readings/read/" + smartMeterId, ElectricityReading[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(Arrays.asList(response.getBody())).isEqualTo(data);
    }

    @Test
    void shouldCalculateAllPrices() {
        String smartMeterId = "bob";
        List<ElectricityReading> data = List.of(
                new ElectricityReading(Instant.parse("2024-04-26T00:00:10.00Z"), new BigDecimal(10)),
                new ElectricityReading(Instant.parse("2024-04-26T00:00:20.00Z"), new BigDecimal(20)),
                new ElectricityReading(Instant.parse("2024-04-26T00:00:30.00Z"), new BigDecimal(30)));
        populateReadingsForMeter(smartMeterId, data);

        ResponseEntity<CompareAllResponse> response =
                restTemplate.getForEntity("/price-plans/compare-all/" + smartMeterId, CompareAllResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        CompareAllResponse responseBody = response.getBody();
        assertThat(responseBody.pricePlanId()).isNull();
        assertThat(responseBody.pricePlanComparisons())
                .hasSize(3)
                .containsEntry("price-plan-0", 1.11)
                .containsEntry("price-plan-1", 0.22)
                .containsEntry("price-plan-2", 0.11);
    }

    @SuppressWarnings("rawtypes")
    @Test
    void givenMeterIdAndLimitShouldReturnRecommendedCheapestPricePlans() {
        String smartMeterId = "jane";
        List<ElectricityReading> data = List.of(
                new ElectricityReading(Instant.parse("2024-04-26T00:00:10.00Z"), new BigDecimal(10)),
                new ElectricityReading(Instant.parse("2024-04-26T00:00:20.00Z"), new BigDecimal(20)),
                new ElectricityReading(Instant.parse("2024-04-26T00:00:30.00Z"), new BigDecimal(30)));
        populateReadingsForMeter(smartMeterId, data);

        ResponseEntity<Map[]> response =
                restTemplate.getForEntity("/price-plans/recommend/" + smartMeterId + "?limit=2", Map[].class);

        assertThat(response.getBody()).containsExactly(Map.of("price-plan-2", 0.11), Map.of("price-plan-1", 0.22));
    }

    @Test
    void shouldCalculateHigherPriceForPeakTime() {
        String smartMeterId = "peak-user";
        // Saturday, April 27, 2024 - A day with peak time pricing for price-plan-0
        List<ElectricityReading> data = List.of(
                new ElectricityReading(Instant.parse("2024-04-27T10:00:00.00Z"), BigDecimal.valueOf(0.5)),
                new ElectricityReading(Instant.parse("2024-04-27T10:30:00.00Z"), BigDecimal.valueOf(0.5)));

        // Store the readings via the API
        MeterReadings readings = new MeterReadings(smartMeterId, data);
        HttpEntity<MeterReadings> entity = toHttpEntity(readings);
        restTemplate.postForEntity("/readings/store", entity, String.class);

        ResponseEntity<CompareAllResponse> response =
                restTemplate.getForEntity("/price-plans/compare-all/" + smartMeterId, CompareAllResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        CompareAllResponse responseBody = response.getBody();
        // price-plan-0 (Dr Evil) has a unit rate of 10 and a 2x multiplier on Saturday
        // Average power: (0.5 + 0.5) / 2 = 0.5 kW
        // Time difference: 0.5 hours
        // Consumption: 0.5 kW * 0.5 h = 0.25 kWh
        // Cost: 0.25 kWh * (10 * 2) = 5.00
        assertThat(responseBody.pricePlanComparisons().get("price-plan-0")).isCloseTo(5.00, within(0.01));
    }

    private void populateReadingsForMeter(String smartMeterId, List<ElectricityReading> data) {
        MeterReadings readings = new MeterReadings(smartMeterId, data);

        HttpEntity<MeterReadings> entity = toHttpEntity(readings);
        restTemplate.postForEntity("/readings/store", entity, String.class);
    }

    record CompareAllResponse(Map<String, Double> pricePlanComparisons, String pricePlanId) {}
}
