package billing;

import java.math.BigDecimal;

/**
 * The discount options an admin can choose when creating a bill.
 */
public enum DiscountType {
    REGULAR("Regular", "Regular (no discount)"),
    SENIOR_CITIZEN("Senior Citizen", "Senior Citizen (20%)"),
    PWD("PWD", "PWD (20%)"),
    PHILHEALTH("PhilHealth", "PhilHealth (fixed deduction)");

    private final String label;
    private final String optionText;

    DiscountType(String label, String optionText) {
        this.label = label;
        this.optionText = optionText;
    }

    public String getOptionText() {
        return optionText;
    }

    public String getLabel() {
        return label;
    }

    /** Creates the matching strategy. The deduction is only used for PhilHealth. */
    public BillingStrategy toStrategy(BigDecimal philHealthDeduction) {
        switch (this) {
            case SENIOR_CITIZEN:
                return new SeniorCitizenBilling();
            case PWD:
                return new PwdBilling();
            case PHILHEALTH:
                return new PhilHealthBilling(philHealthDeduction);
            case REGULAR:
            default:
                return new RegularBilling();
        }
    }
}
