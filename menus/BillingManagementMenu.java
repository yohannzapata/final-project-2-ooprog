package menus;

import billing.DiscountType;
import exceptions.DuplicateRecordException;
import exceptions.InvalidInputException;
import exceptions.RecordNotFoundException;
import models.Admission;
import models.Appointment;
import models.Bill;
import models.Patient;
import services.AdmissionService;
import services.AppointmentService;
import services.BillingService;
import util.Format;
import util.InputHelper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Admin: create bills, change discounts, record payments and delete unpaid bills.
 */
public class BillingManagementMenu extends Menu {

    private final BillingService service = new BillingService();

    @Override
    protected String getTitle() {
        return "ADMIN MENU > BILLING";
    }

    @Override
    protected void printSummary() {
        BigDecimal receivables = BigDecimal.ZERO;
        for (Bill b : service.getUnpaidBills()) {
            receivables = receivables.add(b.getTotalAmount());
        }
        System.out.println("Bills: " + data.getBills().size() + " | Unpaid: " + service.getUnpaidBills().size()
                + " (" + Format.money(receivables) + ")");
        System.out.println("Waiting to be billed: " + service.getUnbilledAppointments().size() + " consultation(s), "
                + service.getUnbilledAdmissions().size() + " admission(s)");
        System.out.println();
    }

    @Override
    protected List<Option> getOptions() {
        boolean hasBills = !data.getBills().isEmpty();
        boolean hasUnpaid = !service.getUnpaidBills().isEmpty();
        List<Option> options = new ArrayList<>();
        addIf(options, !service.getUnbilledAppointments().isEmpty(), "Create bill for a consultation", this::billConsultation);
        addIf(options, !service.getUnbilledAdmissions().isEmpty(), "Create bill for an admission", this::billAdmission);
        addIf(options, hasBills, "View all bills", () -> service.printTable(data.getBills()));
        addIf(options, hasUnpaid, "View unpaid bills", this::viewUnpaid);
        addIf(options, hasBills, "View bill statement", this::viewStatement);
        addIf(options, hasBills, "View all bill statements", this::viewAllStatements);
        addIf(options, hasUnpaid, "Change discount", this::changeDiscount);
        addIf(options, hasUnpaid, "Mark bill as paid", this::markPaid);
        addIf(options, hasUnpaid, "Delete unpaid bill", this::delete);
        return options;
    }

    /** Only unpaid bills can be changed, paid or deleted; a paid bill is asked again with the reason. */
    private Bill selectUnpaidBill() {
        service.printTable(service.getUnpaidBills());
        return Prompts.select("Enter bill ID: ", id -> {
            Bill found = data.findBill(id);
            if (found.isPaid()) {
                throw new InvalidInputException("Bill " + found.getId() + " was paid on "
                        + Format.date(found.getDatePaid()) + " and can no longer be changed.");
            }
            return found;
        });
    }

    /** Every bill's full statement, one after another, then the totals of all bills. */
    private void viewAllStatements() {
        List<Bill> bills = data.getBills();
        System.out.println("Statements of all " + bills.size() + " bill(s):");
        for (Bill b : bills) {
            System.out.println();
            service.printStatement(b);
        }
        System.out.println();
        service.printTotals(bills);
    }

    private void viewStatement() {
        service.printTable(data.getBills());
        service.printStatement(Prompts.select("Enter bill ID: ", data::findBill));
    }

    private void billConsultation() throws InvalidInputException, RecordNotFoundException, DuplicateRecordException {
        System.out.println("Completed appointments not yet billed:");
        new AppointmentService().printTable(service.getUnbilledAppointments());
        Appointment appointment = Prompts.select("Enter appointment ID: ", id -> {
            Appointment found = data.findAppointment(id);
            try {
                service.checkCanBillAppointment(found);
            } catch (DuplicateRecordException e) {
                throw new InvalidInputException(e.getMessage());
            }
            return found;
        });
        Patient patient = data.findPatient(appointment.getPatientId());
        BigDecimal gross = appointment.getConsultationFee();
        System.out.println("Consultation fee: " + Format.money(gross));

        DiscountType type = readDiscountType(patient);
        BigDecimal deduction = readDeduction(type, gross);
        Bill bill = service.createForAppointment(appointment, patient, type, deduction);
        Format.success("Bill " + bill.getId() + " created.");
        service.printStatement(bill);
    }

    private void billAdmission() throws InvalidInputException, RecordNotFoundException, DuplicateRecordException {
        System.out.println("Discharged admissions not yet billed:");
        new AdmissionService().printTable(service.getUnbilledAdmissions());
        Admission admission = Prompts.select("Enter admission ID: ", id -> {
            Admission found = data.findAdmission(id);
            try {
                service.checkCanBillAdmission(found);
            } catch (DuplicateRecordException e) {
                throw new InvalidInputException(e.getMessage());
            }
            return found;
        });
        Patient patient = data.findPatient(admission.getPatientId());
        BigDecimal gross = service.computeGross(admission);
        System.out.println(admission.getDaysStayed() + " day(s) x " + Format.money(admission.getDailyRate())
                + " = " + Format.money(gross));

        DiscountType type = readDiscountType(patient);
        BigDecimal deduction = readDeduction(type, gross);
        Bill bill = service.createForAdmission(admission, patient, type, deduction);
        Format.success("Bill " + bill.getId() + " created.");
        service.printStatement(bill);
    }

    /** Only shows the discounts the patient is eligible for, based on age and IDs on file. */
    private DiscountType readDiscountType(Patient patient) {
        List<DiscountType> types = service.getEligibleTypes(patient);
        System.out.println("Discount options for " + patient.getFullName() + " (age " + patient.getAge() + "):");
        for (int i = 0; i < types.size(); i++) {
            System.out.println("  " + (i + 1) + ". " + types.get(i).getOptionText());
        }
        int choice = InputHelper.readInt("Choose discount: ", 1, types.size(), "Discount");
        return types.get(choice - 1);
    }

    private BigDecimal readDeduction(DiscountType type, BigDecimal gross) {
        if (!type.usesPhilHealth()) {
            return BigDecimal.ZERO;
        }
        return InputHelper.readAmount("PhilHealth deduction (max " + Format.money(gross) + "): ",
                "PhilHealth deduction", gross);
    }

    private void viewUnpaid() {
        List<Bill> unpaid = service.getUnpaidBills();
        BigDecimal total = BigDecimal.ZERO;
        for (Bill b : unpaid) {
            total = total.add(b.getTotalAmount());
        }
        service.printTable(unpaid);
        System.out.println("Total receivables: " + Format.money(total));
    }

    private void changeDiscount() throws InvalidInputException, RecordNotFoundException {
        Bill bill = selectUnpaidBill();
        Patient patient = data.findPatient(bill.getPatientId());
        System.out.println("Current discount: " + bill.getStrategy().getDescription());
        DiscountType type = readDiscountType(patient);
        BigDecimal deduction = readDeduction(type, bill.getGrossAmount());
        service.changeDiscount(bill, patient, type, deduction);
        Format.success("Discount updated.");
        service.printStatement(bill);
    }

    private void markPaid() throws InvalidInputException {
        Bill bill = selectUnpaidBill();
        service.printStatement(bill);
        if (!InputHelper.readYesNo("Record full payment of " + Format.money(bill.getTotalAmount()) + "?")) {
            Format.info("Payment not recorded.");
            return;
        }
        service.markPaid(bill);
        Format.success("Bill " + bill.getId() + " marked as paid.");
    }

    private void delete() throws InvalidInputException {
        Bill bill = selectUnpaidBill();
        service.printStatement(bill);
        if (!InputHelper.confirm("Delete bill " + bill.getId() + "?")) {
            Format.info("Delete cancelled.");
            return;
        }
        service.delete(bill);
        Format.success("Bill deleted.");
    }
}
