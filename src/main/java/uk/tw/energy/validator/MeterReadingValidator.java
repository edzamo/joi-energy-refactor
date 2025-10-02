package uk.tw.energy.validator;

import org.springframework.stereotype.Component;
import uk.tw.energy.domain.MeterReadings;

@Component
public class MeterReadingValidator {

    public boolean validate(MeterReadings meterReadings) {
        return meterReadings != null
                && meterReadings.smartMeterId() != null
                && !meterReadings.smartMeterId().isEmpty()
                && meterReadings.electricityReadings() != null
                && !meterReadings.electricityReadings().isEmpty();
    }
}
