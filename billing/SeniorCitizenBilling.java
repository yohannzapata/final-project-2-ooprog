package billing;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 20% discount for senior citizens (RA 9994).
 */
public class SeniorCitizenBilling implements BillingStrategy {

    public static final BigDecimal DISCOUNT_RATE = new BigDecimal("0.20");

    @Override
    public BigDecimal computeTotal(BigDecimal grossAmount) {
        BigDecimal discount = grossAmount.multiply(DISCOUNT_RATE).setScale(2, RoundingMode.HALF_UP);
        return grossAmount.subtract(discount).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public String getDescription() {
        return "Senior Citizen (20%)";
    }
}
