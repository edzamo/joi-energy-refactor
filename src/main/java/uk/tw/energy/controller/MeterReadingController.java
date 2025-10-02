package uk.tw.energy.controller;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.tw.energy.domain.ElectricityReading;
import uk.tw.energy.domain.MeterReadings;
import uk.tw.energy.service.MeterReadingService;
import uk.tw.energy.validator.MeterReadingValidator;

@RestController
@RequestMapping("/readings")
public class MeterReadingController {

    private final MeterReadingService meterReadingService;
    private final MeterReadingValidator meterReadingValidator;

    public MeterReadingController(
            MeterReadingService meterReadingService, MeterReadingValidator meterReadingValidator) {
        this.meterReadingService = meterReadingService;
        this.meterReadingValidator = meterReadingValidator;
    }

    @PostMapping("/store")
    public ResponseEntity<Void> storeReadings(@RequestBody MeterReadings meterReadings) {
        if (!meterReadingValidator.validate(meterReadings)) {
            return ResponseEntity.badRequest().build();
        }
        meterReadingService.storeReadings(meterReadings.smartMeterId(), meterReadings.electricityReadings());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/read/{smartMeterId}")
    public ResponseEntity<List<ElectricityReading>> readReadings(@PathVariable String smartMeterId) {
        return meterReadingService
                .getReadings(smartMeterId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
