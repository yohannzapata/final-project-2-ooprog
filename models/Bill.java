package models;

import billing.BillingStrategy;
import billing.DiscountType;
import util.RecordFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A bill for either a completed appointment (consultation fee) or a discharged
 * admission (daily rate x days). The amount due is computed by a BillingStrategy.
 */
public final class Bill {

    public enum Source {
        CONSULTATION("Consultation"),
        ADMISSION("Admission");

        private final String label;

        Source(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    private final String id;
    private final String patientId;
    private final Source source;
    private final String sourceId;
    private final BigDecimal grossAmount;
    private DiscountType discountType;
    private BigDecimal philHealthDeduction;
    private BigDecimal totalAmount;
    private final LocalDate dateIssued;
    private LocalDate datePaid;

    public Bill(String id, String patientId, Source source, String sourceId, BigDecimal grossAmount,
                DiscountType discountType, BigDecimal philHealthDeduction) {
        this.id = id;
        this.patientId = patientId;
        this.source = source;
        this.sourceId = sourceId;
        this.grossAmount = grossAmount;
        this.dateIssued = LocalDate.now();
        applyDiscount(discountType, philHealthDeduction);
    }

    /** Recomputes the total using the strategy of the chosen discount type. */
    public final void applyDiscount(DiscountType type, BigDecimal deduction) {
        if (isPaid()) {
            throw new IllegalStateException("A paid bill can no longer be changed.");
        }
        BigDecimal cleanDeduction = type == DiscountType.PHILHEALTH ? deduction : BigDecimal.ZERO;
        BillingStrategy strategy = type.toStrategy(cleanDeduction);
        this.totalAmount = strategy.computeTotal(grossAmount);
        this.discountType = type;
        this.philHealthDeduction = cleanDeduction;
    }

    public BillingStrategy getStrategy() {
        return discountType.toStrategy(philHealthDeduction);
    }

    public BigDecimal getDiscountAmount() {
        return grossAmount.subtract(totalAmount);
    }

    public boolean isPaid() {
        return datePaid != null;
    }

    public String getStatusLabel() {
        return isPaid() ? "Paid" : "Unpaid";
    }

    public void markPaid(LocalDate date) {
        if (isPaid()) {
            throw new IllegalStateException("Bill is already paid.");
        }
        this.datePaid = date;
    }

    public String toRecord() {
        return RecordFormat.join(id, patientId, source.name(), sourceId, grossAmount.toPlainString(),
                discountType.name(), philHealthDeduction.toPlainString(), totalAmount.toPlainString(),
                dateIssued.toString(), datePaid == null ? "" : datePaid.toString());
    }

    public static Bill fromRecord(String[] f) {
        Bill bill = new Bill(f[0], f[1], Source.valueOf(f[2]), f[3], new BigDecimal(f[4]),
                DiscountType.valueOf(f[5]), new BigDecimal(f[6]), LocalDate.parse(f[8]));
        bill.totalAmount = new BigDecimal(f[7]);
        if (!f[9].isEmpty()) {
            bill.datePaid = LocalDate.parse(f[9]);
        }
        return bill;
    }

    private Bill(String id, String patientId, Source source, String sourceId, BigDecimal grossAmount,
                 DiscountType discountType, BigDecimal philHealthDeduction, LocalDate dateIssued) {
        this.id = id;
        this.patientId = patientId;
        this.source = source;
        this.sourceId = sourceId;
        this.grossAmount = grossAmount;
        this.discountType = discountType;
        this.philHealthDeduction = philHealthDeduction;
        this.dateIssued = dateIssued;
    }

    public String getId() {
        return id;
    }

    public String getPatientId() {
        return patientId;
    }

    public Source getSource() {
        return source;
    }

    public String getSourceId() {
        return sourceId;
    }

    public BigDecimal getGrossAmount() {
        return grossAmount;
    }

    public DiscountType getDiscountType() {
        return discountType;
    }

    public BigDecimal getPhilHealthDeduction() {
        return philHealthDeduction;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public LocalDate getDateIssued() {
        return dateIssued;
    }

    public LocalDate getDatePaid() {
        return datePaid;
    }
}
