package billing;

import java.math.BigDecimal;

/**
 * Strategy pattern: each discount type computes the amount due in its own way,
 * so Bill does not need a big if-else for every discount.
 */
public interface BillingStrategy {

    /** Returns the amount the patient has to pay after the discount/deduction. */
    BigDecimal computeTotal(BigDecimal grossAmount);

    /** Short description shown on the bill, e.g. "Senior Citizen (20%)". */
    String getDescription();
}
