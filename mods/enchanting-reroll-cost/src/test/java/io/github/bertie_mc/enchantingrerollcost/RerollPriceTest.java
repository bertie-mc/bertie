package io.github.bertie_mc.enchantingrerollcost;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RerollPriceTest {
    @ParameterizedTest
    @CsvSource({"0,1,6", "1.5,1,6", "10,2,8", "30,6,16", "60,12,28", "100,20,56", "12.49,2,8", "12.5,3,10"})
    void chargesTheMinimumFirstOffer(float power, int minimumLevel, int experience) {
        assertEquals(minimumLevel, RerollPrice.minimumOfferLevel(power));
        assertEquals(experience, RerollPrice.experienceCost(power));
    }
}
