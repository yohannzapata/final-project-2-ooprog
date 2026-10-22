package billing;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * PhilHealth deduction together with the Senior Citizen or PWD discount. As on hospital bills,
 * the PhilHealth deduction is taken first and the 20% discount applies to the remaining balance.
 */
public class PhilHealthWithDiscountBilling implements BillingStrategy {

    private final PhilHealthBilling philHealth;
    private final BillingStrategy discount;

    public PhilHealthWithDiscountBilling(BigDecimal deduction, BillingStrategy discount) {
        this.philHealth = new PhilHealthBilling(deduction);
        this.discount = discount;
    }

    @Override
    public BigDecimal computeTotal(BigDecimal grossAmount) {
        BigDecimal afterPhilHealth = philHealth.computeTotal(grossAmount);
        return discount.computeTotal(afterPhilHealth).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public String getDescription() {
        return philHealth.getDescription() + " + " + discount.getDescription();
    }

    public BigDecimal getDeduction() {
        return philHealth.getDeduction();
    }

    /** The 20% discount part, e.g. "Senior Citizen (20%)", shown on its own line of the statement. */
    public String getDiscountDescription() {
        return discount.getDescription();
    }
}
