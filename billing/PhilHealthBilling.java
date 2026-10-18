package billing;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * PhilHealth coverage: a fixed amount (the case rate) is deducted from the bill.
 * The deduction can never be more than the bill itself.
 */
public class PhilHealthBilling implements BillingStrategy {

    private final BigDecimal deduction;

    public PhilHealthBilling(BigDecimal deduction) {
        if (deduction == null || deduction.signum() <= 0) {
            throw new IllegalArgumentException("PhilHealth deduction must be greater than 0.");
        }
        this.deduction = deduction.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal computeTotal(BigDecimal grossAmount) {
        if (deduction.compareTo(grossAmount) > 0) {
            throw new IllegalArgumentException("PhilHealth deduction cannot be more than the bill amount.");
        }
        return grossAmount.subtract(deduction).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public String getDescription() {
        return "PhilHealth deduction";
    }

    public BigDecimal getDeduction() {
        return deduction;
    }
}
