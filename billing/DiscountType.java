package billing;

import java.math.BigDecimal;

/**
 * The discount options an admin can choose when creating a bill.
 */
public enum DiscountType {
    REGULAR("Regular", "Regular (no discount)"),
    SENIOR_CITIZEN("Senior Citizen", "Senior Citizen (20%)"),
    PWD("PWD", "PWD (20%)"),
    PHILHEALTH("PhilHealth", "PhilHealth (fixed deduction)"),
    // A patient with PhilHealth and a Senior Citizen or PWD ID gets both: the deduction first, then 20% of the rest.
    PHILHEALTH_SENIOR("PhilHealth + Senior", "PhilHealth deduction, then Senior Citizen 20% of the rest"),
    PHILHEALTH_PWD("PhilHealth + PWD", "PhilHealth deduction, then PWD 20% of the rest");

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

    public boolean usesPhilHealth() {
        return this == PHILHEALTH || this == PHILHEALTH_SENIOR || this == PHILHEALTH_PWD;
    }

    public boolean usesSeniorDiscount() {
        return this == SENIOR_CITIZEN || this == PHILHEALTH_SENIOR;
    }

    public boolean usesPwdDiscount() {
        return this == PWD || this == PHILHEALTH_PWD;
    }

    /** Creates the matching strategy. The deduction is only used for the PhilHealth types. */
    public BillingStrategy toStrategy(BigDecimal philHealthDeduction) {
        switch (this) {
            case SENIOR_CITIZEN:
                return new SeniorCitizenBilling();
            case PWD:
                return new PwdBilling();
            case PHILHEALTH:
                return new PhilHealthBilling(philHealthDeduction);
            case PHILHEALTH_SENIOR:
                return new PhilHealthWithDiscountBilling(philHealthDeduction, new SeniorCitizenBilling());
            case PHILHEALTH_PWD:
                return new PhilHealthWithDiscountBilling(philHealthDeduction, new PwdBilling());
            case REGULAR:
            default:
                return new RegularBilling();
        }
    }
}
