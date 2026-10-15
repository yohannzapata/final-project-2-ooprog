package hms.billing;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * No discount: the patient pays the full amount.
 */
public class RegularBilling implements BillingStrategy {

    @Override
    public BigDecimal computeTotal(BigDecimal grossAmount) {
        return grossAmount.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public String getDescription() {
        return "Regular (no discount)";
    }
}
