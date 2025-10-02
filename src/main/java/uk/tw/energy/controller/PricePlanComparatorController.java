package uk.tw.energy.controller;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.tw.energy.service.AccountService;
import uk.tw.energy.service.PricePlanService;

@RestController
@RequestMapping("/price-plans")
public class PricePlanComparatorController {

    public static final String PRICE_PLAN_ID_KEY = "pricePlanId";
    public static final String PRICE_PLAN_COMPARISONS_KEY = "pricePlanComparisons";
    private final PricePlanService pricePlanService;
    private final AccountService accountService;

    public PricePlanComparatorController(PricePlanService pricePlanService, AccountService accountService) {
        this.pricePlanService = pricePlanService;
        this.accountService = accountService;
    }

    @GetMapping("/compare-all/{smartMeterId}")
    public ResponseEntity<Map<String, Object>> calculatedCostForEachPricePlan(@PathVariable String smartMeterId) {
        Optional<Map<String, BigDecimal>> consumptionsForPricePlans =
                pricePlanService.getConsumptionCostOfElectricityReadingsForEachPricePlan(smartMeterId);

        return consumptionsForPricePlans
                .map(consumptions -> {
                    Map<String, Object> pricePlanComparisons = new HashMap<>();
                    String pricePlanId = accountService.getPricePlanIdForSmartMeterId(smartMeterId);
                    pricePlanComparisons.put(PRICE_PLAN_ID_KEY, pricePlanId);
                    pricePlanComparisons.put(PRICE_PLAN_COMPARISONS_KEY, consumptions);
                    return ResponseEntity.ok(pricePlanComparisons);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/recommend/{smartMeterId}")
    public ResponseEntity<List<Map.Entry<String, BigDecimal>>> recommendCheapestPricePlans(
            @PathVariable String smartMeterId, @RequestParam(value = "limit", required = false) Integer limit) {
        Optional<Map<String, BigDecimal>> consumptionsForPricePlans =
                pricePlanService.getConsumptionCostOfElectricityReadingsForEachPricePlan(smartMeterId);

        return consumptionsForPricePlans
                .map(consumptions -> {
                    Stream<Map.Entry<String, BigDecimal>> recommendations =
                            consumptions.entrySet().stream().sorted(Map.Entry.comparingByValue());

                    if (limit != null) {
                        recommendations = recommendations.limit(limit);
                    }

                    return ResponseEntity.ok(recommendations.collect(Collectors.toList()));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
