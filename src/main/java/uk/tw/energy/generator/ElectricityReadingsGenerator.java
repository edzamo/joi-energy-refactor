package uk.tw.energy.generator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import uk.tw.energy.domain.ElectricityReading;

public class ElectricityReadingsGenerator {

    public List<ElectricityReading> generate(int number) {
        Instant now = Instant.now();
        Random readingRandomiser = new Random();

        return IntStream.range(0, number)
                .mapToObj(i -> {
                    long secondsToSubtract = (long) (number - 1 - i) * 10L;
                    Instant timestamp = now.minusSeconds(secondsToSubtract);
                    double positiveRandomValue = Math.abs(readingRandomiser.nextGaussian());
                    BigDecimal randomReading =
                            BigDecimal.valueOf(positiveRandomValue).setScale(4, RoundingMode.CEILING);
                    return new ElectricityReading(timestamp, randomReading);
                })
                .collect(Collectors.toList());
    }
}
