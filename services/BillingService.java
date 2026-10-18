package services;

import billing.DiscountType;
import data.HospitalData;
import exceptions.DuplicateRecordException;
import exceptions.InvalidInputException;
import models.Admission;
import models.Appointment;
import models.AppointmentStatus;
import models.Bill;
import models.Patient;
import util.Format;

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

    /** Business rules for each discount type. */
    public void checkEligibility(Patient patient, DiscountType type, BigDecimal grossAmount, BigDecimal deduction)
            throws InvalidInputException {
        switch (type) {
            case SENIOR_CITIZEN:
                if (!patient.isSeniorCitizen()) {
                    throw new InvalidInputException("Senior Citizen discount is only for patients aged 60 and above. "
                            + patient.getFullName() + " is " + patient.getAge() + ".");
                }
                if (!patient.hasSeniorCitizenId()) {
                    throw new InvalidInputException("No Senior Citizen ID on file. Update the patient record first.");
                }
                break;
            case PWD:
                if (!patient.hasPwdId()) {
                    throw new InvalidInputException("No PWD ID on file. Update the patient record first.");
                }
                break;
            case PHILHEALTH:
                if (!patient.hasPhilHealth()) {
                    throw new InvalidInputException("No PhilHealth number on file. Update the patient record first.");
                }
                if (deduction == null || deduction.signum() <= 0) {
                    throw new InvalidInputException("PhilHealth deduction must be greater than 0.");
                }
                if (deduction.compareTo(grossAmount) > 0) {
                    throw new InvalidInputException("PhilHealth deduction (" + Format.money(deduction)
                            + ") cannot be more than the bill (" + Format.money(grossAmount) + ").");
                }
                break;
            case REGULAR:
            default:
                break;
        }
    }

    /** Discount types this patient can actually use, shown to the admin as choices. */
    public List<DiscountType> getEligibleTypes(Patient patient) {
        List<DiscountType> types = new ArrayList<>();
        types.add(DiscountType.REGULAR);
        if (patient.isSeniorCitizen() && patient.hasSeniorCitizenId()) {
            types.add(DiscountType.SENIOR_CITIZEN);
        }
        if (patient.hasPwdId()) {
            types.add(DiscountType.PWD);
        }
        if (patient.hasPhilHealth()) {
            types.add(DiscountType.PHILHEALTH);
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
        String fmt = "%-7s %-22s %-13s %-16s %-16s %-7s%n";
        System.out.printf(fmt, "ID", "Patient", "For", "Discount", "Amount Due", "Status");
        Format.line(86);
        for (Bill b : list) {
            System.out.printf(fmt, b.getId(), Format.fit(data.patientName(b.getPatientId()), 22),
                    b.getSource().getLabel(), b.getDiscountType().getLabel(), Format.money(b.getTotalAmount()),
                    b.getStatusLabel());
        }
    }

    /** Prints the bill like a simple statement of account. */
    public void printStatement(Bill b) {
        Format.line(50);
        System.out.println("STATEMENT OF ACCOUNT            Bill No. " + b.getId());
        Format.line(50);
        System.out.println("Patient     : " + data.patientName(b.getPatientId()) + " (" + b.getPatientId() + ")");
        System.out.println("Date Issued : " + Format.date(b.getDateIssued()));
        if (b.getSource() == Bill.Source.CONSULTATION) {
            System.out.println("For         : Consultation (" + b.getSourceId() + ")");
            try {
                Appointment a = data.findAppointment(b.getSourceId());
                System.out.println("              " + data.doctorName(a.getDoctorId()) + ", " + Format.date(a.getDate()));
            } catch (Exception ignored) {
                // appointment details are optional on the statement
            }
        } else {
            System.out.println("For         : Admission (" + b.getSourceId() + ")");
            try {
                Admission a = data.findAdmission(b.getSourceId());
                System.out.println("              Room " + a.getRoomNumber() + ", " + a.getDaysStayed() + " day(s) x "
                        + Format.money(a.getDailyRate()));
            } catch (Exception ignored) {
                // admission details are optional on the statement
            }
        }
        Format.line(50);
        System.out.printf("%-28s %20s%n", "Gross Amount", Format.money(b.getGrossAmount()));
        System.out.printf("%-28s %20s%n", "Less: " + b.getStrategy().getDescription(),
                "(" + Format.money(b.getDiscountAmount()) + ")");
        Format.line(50);
        System.out.printf("%-28s %20s%n", "AMOUNT DUE", Format.money(b.getTotalAmount()));
        System.out.println("Status      : " + b.getStatusLabel()
                + (b.isPaid() ? " on " + Format.date(b.getDatePaid()) : ""));
        Format.line(50);
    }
}
