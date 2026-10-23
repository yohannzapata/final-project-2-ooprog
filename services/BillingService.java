package services;

import billing.BillingStrategy;
import billing.DiscountType;
import billing.PhilHealthWithDiscountBilling;
import data.HospitalData;
import exceptions.DuplicateRecordException;
import exceptions.InvalidInputException;
import exceptions.RecordNotFoundException;
import models.Admission;
import models.Appointment;
import models.AppointmentStatus;
import models.Bill;
import models.Patient;
import util.Format;
import util.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Creates and updates bills and checks who can get which discount.
 */
public class BillingService {

    private final HospitalData data = HospitalData.getInstance();

    public Bill findExistingBill(Bill.Source source, String sourceId) {
        for (Bill bill : data.getBills()) {
            if (bill.getSource() == source && bill.getSourceId().equals(sourceId)) {
                return bill;
            }
        }
        return null;
    }

    /** Completed appointments that do not have a bill yet. */
    public List<Appointment> getUnbilledAppointments() {
        List<Appointment> result = new ArrayList<>();
        for (Appointment a : data.getAppointments()) {
            if (a.getStatus() == AppointmentStatus.COMPLETED && findExistingBill(Bill.Source.CONSULTATION, a.getId()) == null) {
                result.add(a);
            }
        }
        return result;
    }

    /** Discharged admissions that do not have a bill yet. */
    public List<Admission> getUnbilledAdmissions() {
        List<Admission> result = new ArrayList<>();
        for (Admission a : data.getAdmissions()) {
            if (a.isDischarged() && findExistingBill(Bill.Source.ADMISSION, a.getId()) == null) {
                result.add(a);
            }
        }
        return result;
    }

    public List<Bill> getUnpaidBills() {
        List<Bill> result = new ArrayList<>();
        for (Bill b : data.getBills()) {
            if (!b.isPaid()) {
                result.add(b);
            }
        }
        return result;
    }

    public void checkCanBillAppointment(Appointment appointment) throws InvalidInputException, DuplicateRecordException {
        if (appointment.getStatus() != AppointmentStatus.COMPLETED) {
            throw new InvalidInputException("Only completed appointments can be billed. Appointment "
                    + appointment.getId() + " is " + appointment.getStatus().getLabel() + ".");
        }
        Bill existing = findExistingBill(Bill.Source.CONSULTATION, appointment.getId());
        if (existing != null) {
            throw new DuplicateRecordException("Appointment " + appointment.getId() + " is already billed ("
                    + existing.getId() + ").");
        }
    }

    public void checkCanBillAdmission(Admission admission) throws InvalidInputException, DuplicateRecordException {
        if (!admission.isDischarged()) {
            throw new InvalidInputException("The patient must be discharged before the admission can be billed.");
        }
        Bill existing = findExistingBill(Bill.Source.ADMISSION, admission.getId());
        if (existing != null) {
            throw new DuplicateRecordException("Admission " + admission.getId() + " is already billed ("
                    + existing.getId() + ").");
        }
    }

    public BigDecimal computeGross(Admission admission) {
        return admission.getDailyRate().multiply(BigDecimal.valueOf(admission.getDaysStayed()));
    }

    /** Business rules for each discount type; the combined types must meet both rules. */
    public void checkEligibility(Patient patient, DiscountType type, BigDecimal grossAmount, BigDecimal deduction)
            throws InvalidInputException {
        if (type.usesSeniorDiscount()) {
            if (!patient.isSeniorCitizen()) {
                throw new InvalidInputException("Senior Citizen discount is only for patients aged 60 and above. "
                        + patient.getFullName() + " is " + patient.getAge() + ".");
            }
            // Age alone is not proof: a patient can edit their own birthdate, so the ID is required too.
            if (!patient.hasSeniorCitizenId()) {
                throw new InvalidInputException("No Senior Citizen ID on file. Update the patient record first.");
            }
        }
        if (type.usesPwdDiscount() && !patient.hasPwdId()) {
            throw new InvalidInputException("No PWD ID on file. Update the patient record first.");
        }
        if (type.usesPhilHealth()) {
            if (deduction == null || deduction.signum() <= 0) {
                throw new InvalidInputException("PhilHealth deduction must be greater than 0.");
            }
            if (deduction.compareTo(grossAmount) > 0) {
                throw new InvalidInputException("PhilHealth deduction (" + Format.money(deduction)
                        + ") cannot be more than the bill (" + Format.money(grossAmount) + ").");
            }
        }
    }

    /** Discount types this patient can actually use, shown to the admin as choices. */
    public List<DiscountType> getEligibleTypes(Patient patient) {
        boolean senior = patient.isSeniorCitizen() && patient.hasSeniorCitizenId();
        boolean pwd = patient.hasPwdId();
        boolean philHealth = true; // PhilHealth coverage is a fixed amount entered by the admin
        List<DiscountType> types = new ArrayList<>();
        types.add(DiscountType.REGULAR);
        if (senior) {
            types.add(DiscountType.SENIOR_CITIZEN);
        }
        if (pwd) {
            types.add(DiscountType.PWD);
        }
        if (philHealth) {
            types.add(DiscountType.PHILHEALTH);
        }
        if (philHealth && senior) {
            types.add(DiscountType.PHILHEALTH_SENIOR);
        }
        if (philHealth && pwd) {
            types.add(DiscountType.PHILHEALTH_PWD);
        }
        return types;
    }

    public Bill createForAppointment(Appointment appointment, Patient patient, DiscountType type, BigDecimal deduction)
            throws InvalidInputException, DuplicateRecordException {
        checkCanBillAppointment(appointment);
        BigDecimal gross = appointment.getConsultationFee();
        checkEligibility(patient, type, gross, deduction);
        Bill bill = new Bill(data.nextBillId(), patient.getId(), Bill.Source.CONSULTATION, appointment.getId(),
                gross, type, deduction);
        data.getBills().add(bill);
        data.save();
        return bill;
    }

    public Bill createForAdmission(Admission admission, Patient patient, DiscountType type, BigDecimal deduction)
            throws InvalidInputException, DuplicateRecordException {
        checkCanBillAdmission(admission);
        BigDecimal gross = computeGross(admission);
        checkEligibility(patient, type, gross, deduction);
        Bill bill = new Bill(data.nextBillId(), patient.getId(), Bill.Source.ADMISSION, admission.getId(),
                gross, type, deduction);
        data.getBills().add(bill);
        data.save();
        return bill;
    }

    public void changeDiscount(Bill bill, Patient patient, DiscountType type, BigDecimal deduction)
            throws InvalidInputException {
        checkUnpaid(bill, "changed");
        checkEligibility(patient, type, bill.getGrossAmount(), deduction);
        bill.applyDiscount(type, deduction);
        data.save();
    }

    public void markPaid(Bill bill) throws InvalidInputException {
        checkUnpaid(bill, "paid again");
        bill.markPaid(LocalDate.now());
        data.save();
    }

    public void delete(Bill bill) throws InvalidInputException {
        checkUnpaid(bill, "deleted");
        data.getBills().remove(bill);
        data.save();
    }

    private void checkUnpaid(Bill bill, String action) throws InvalidInputException {
        if (bill.isPaid()) {
            throw new InvalidInputException("Bill " + bill.getId() + " was paid on " + Format.date(bill.getDatePaid())
                    + " and cannot be " + action + ".");
        }
    }

    public List<Bill> getByPatient(String patientId) {
        List<Bill> result = new ArrayList<>();
        for (Bill b : data.getBills()) {
            if (b.getPatientId().equals(patientId)) {
                result.add(b);
            }
        }
        return result;
    }

    public void printTable(List<Bill> list) {
        if (list.isEmpty()) {
            Format.info("No bills found.");
            return;
        }
        Table table = new Table("ID", "Patient", "For", "Discount", "Amount Due", "Status").alignRight(4);
        for (Bill b : list) {
            table.addRow(b.getId(), data.patientName(b.getPatientId()), b.getSource().getLabel(),
                    b.getDiscountType().getLabel(), Format.money(b.getTotalAmount()), b.getStatusLabel());
        }
        table.print();
    }

    /** Prints the bill like a simple statement of account. */
    public void printStatement(Bill b) {
        Table table = Table.details("STATEMENT OF ACCOUNT - BILL " + b.getId()).alignRight(1);
        table.addRow("Patient", data.patientName(b.getPatientId()) + " (" + b.getPatientId() + ")");
        table.addRow("Date issued", Format.date(b.getDateIssued()));
        table.addRow("For", b.getSource().getLabel() + " (" + b.getSourceId() + ")");
        addSourceDetails(table, b);
        table.addSeparator();
        table.addRow("Gross amount", Format.money(b.getGrossAmount()));
        addDiscountLines(table, b);
        table.addSeparator();
        table.addRow("AMOUNT DUE", Format.money(b.getTotalAmount()));
        table.addRow("Status", b.isPaid() ? "Paid on " + Format.date(b.getDatePaid()) : "Unpaid");
        table.print();
    }

    /** A combined discount is shown as two lines, in the order they are taken off. */
    private void addDiscountLines(Table table, Bill b) {
        BillingStrategy strategy = b.getStrategy();
        if (strategy instanceof PhilHealthWithDiscountBilling) {
            PhilHealthWithDiscountBilling combined = (PhilHealthWithDiscountBilling) strategy;
            BigDecimal percentPart = b.getDiscountAmount().subtract(combined.getDeduction());
            table.addRow("Less: PhilHealth deduction", "(" + Format.money(combined.getDeduction()) + ")");
            table.addRow("Less: " + combined.getDiscountDescription(), "(" + Format.money(percentPart) + ")");
        } else {
            table.addRow("Less: " + strategy.getDescription(), "(" + Format.money(b.getDiscountAmount()) + ")");
        }
    }

    /** Totals over a list of bills: what was billed, taken off, collected and still owed. */
    public void printTotals(List<Bill> bills) {
        BigDecimal gross = BigDecimal.ZERO;
        BigDecimal discounts = BigDecimal.ZERO;
        BigDecimal due = BigDecimal.ZERO;
        BigDecimal collected = BigDecimal.ZERO;
        int paidCount = 0;
        for (Bill b : bills) {
            gross = gross.add(b.getGrossAmount());
            discounts = discounts.add(b.getDiscountAmount());
            due = due.add(b.getTotalAmount());
            if (b.isPaid()) {
                collected = collected.add(b.getTotalAmount());
                paidCount++;
            }
        }
        Table.details("TOTALS FOR ALL BILLS").alignRight(1)
                .addRow("Bills", bills.size() + " (" + paidCount + " paid, " + (bills.size() - paidCount) + " unpaid)")
                .addRow("Gross amount", Format.money(gross))
                .addRow("Less: discounts and PhilHealth", "(" + Format.money(discounts) + ")")
                .addSeparator()
                .addRow("Amount due", Format.money(due))
                .addRow("Collected", Format.money(collected))
                .addRow("Still unpaid", Format.money(due.subtract(collected)))
                .print();
    }

    /** The doctor or room line is optional; it is left out if the appointment or admission was removed. */
    private void addSourceDetails(Table table, Bill b) {
        try {
            if (b.getSource() == Bill.Source.CONSULTATION) {
                Appointment a = data.findAppointment(b.getSourceId());
                table.addRow("Doctor", data.doctorName(a.getDoctorId()) + ", " + Format.date(a.getDate()));
            } else {
                Admission a = data.findAdmission(b.getSourceId());
                table.addRow("Room", a.getRoomNumber() + ", " + a.getDaysStayed() + " day(s) x "
                        + Format.money(a.getDailyRate()));
            }
        } catch (RecordNotFoundException e) {
            // nothing to add
        }
    }
}
