package hms.menus;

import hms.billing.DiscountType;
import hms.exceptions.DuplicateRecordException;
import hms.exceptions.InvalidInputException;
import hms.exceptions.RecordNotFoundException;
import hms.models.Admission;
import hms.models.Appointment;
import hms.models.AppointmentStatus;
import hms.models.Bill;
import hms.models.Patient;
import hms.services.AdmissionService;
import hms.services.AppointmentService;
import hms.services.BillingService;
import hms.util.Format;
import hms.util.InputHelper;

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
        return "BILLING";
    }

    @Override
    protected String[] getOptions() {
        return new String[]{
                "Create bill for a consultation",
                "Create bill for an admission",
                "View all bills",
                "View unpaid bills",
                "View bill statement",
                "Change discount",
                "Mark bill as paid",
                "Delete unpaid bill"
        };
    }

    @Override
    protected void handle(int choice) throws InvalidInputException, RecordNotFoundException, DuplicateRecordException {
        switch (choice) {
            case 1:
                billConsultation();
                break;
            case 2:
                billAdmission();
                break;
            case 3:
                service.printTable(data.getBills());
                break;
            case 4:
                viewUnpaid();
                break;
            case 5:
                service.printStatement(selectBill());
                break;
            case 6:
                changeDiscount();
                break;
            case 7:
                markPaid();
                break;
            case 8:
                delete();
                break;
            default:
                break;
        }
    }

    private Bill selectBill() {
        return Prompts.select("Enter bill ID: ", data::findBill);
    }

    private void billConsultation() throws InvalidInputException, RecordNotFoundException, DuplicateRecordException {
        List<Appointment> unbilled = new ArrayList<>();
        for (Appointment a : data.getAppointments()) {
            if (a.getStatus() == AppointmentStatus.COMPLETED
                    && service.findExistingBill(Bill.Source.CONSULTATION, a.getId()) == null) {
                unbilled.add(a);
            }
        }
        if (unbilled.isEmpty()) {
            Format.info("There are no completed appointments waiting to be billed.");
            return;
        }
        System.out.println("Completed appointments not yet billed:");
        new AppointmentService().printTable(unbilled);
        Appointment appointment = Prompts.select("Enter appointment ID: ", data::findAppointment);
        service.checkCanBillAppointment(appointment);
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
        List<Admission> unbilled = new ArrayList<>();
        for (Admission a : data.getAdmissions()) {
            if (a.isDischarged() && service.findExistingBill(Bill.Source.ADMISSION, a.getId()) == null) {
                unbilled.add(a);
            }
        }
        if (unbilled.isEmpty()) {
            Format.info("There are no discharged admissions waiting to be billed.");
            return;
        }
        System.out.println("Discharged admissions not yet billed:");
        new AdmissionService().printTable(unbilled);
        Admission admission = Prompts.select("Enter admission ID: ", data::findAdmission);
        service.checkCanBillAdmission(admission);
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
        if (types.size() == 1) {
            Format.info("No Senior Citizen, PWD or PhilHealth ID on file, so only Regular billing is available.");
        }
        int choice = InputHelper.readInt("Choose discount: ", 1, types.size(), "Discount");
        return types.get(choice - 1);
    }

    private BigDecimal readDeduction(DiscountType type, BigDecimal gross) {
        if (type != DiscountType.PHILHEALTH) {
            return BigDecimal.ZERO;
        }
        return InputHelper.readAmount("PhilHealth deduction (PHP, max " + Format.money(gross) + "): ",
                "PhilHealth deduction", gross);
    }

    private void viewUnpaid() {
        List<Bill> unpaid = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (Bill b : data.getBills()) {
            if (!b.isPaid()) {
                unpaid.add(b);
                total = total.add(b.getTotalAmount());
            }
        }
        service.printTable(unpaid);
        if (!unpaid.isEmpty()) {
            System.out.println("Total receivables: " + Format.money(total));
        }
    }

    private void changeDiscount() throws InvalidInputException, RecordNotFoundException {
        Bill bill = selectBill();
        if (bill.isPaid()) {
            throw new InvalidInputException("Bill " + bill.getId() + " is already paid and can no longer be changed.");
        }
        Patient patient = data.findPatient(bill.getPatientId());
        System.out.println("Current discount: " + bill.getStrategy().getDescription());
        DiscountType type = readDiscountType(patient);
        BigDecimal deduction = readDeduction(type, bill.getGrossAmount());
        service.changeDiscount(bill, patient, type, deduction);
        Format.success("Discount updated.");
        service.printStatement(bill);
    }

    private void markPaid() throws InvalidInputException {
        Bill bill = selectBill();
        service.printStatement(bill);
        if (bill.isPaid()) {
            throw new InvalidInputException("This bill is already paid.");
        }
        if (!InputHelper.readYesNo("Record full payment of " + Format.money(bill.getTotalAmount()) + "?")) {
            Format.info("Payment not recorded.");
            return;
        }
        service.markPaid(bill);
        Format.success("Bill " + bill.getId() + " marked as paid.");
    }

    private void delete() throws InvalidInputException {
        Bill bill = selectBill();
        if (bill.isPaid()) {
            throw new InvalidInputException("Paid bills cannot be deleted.");
        }
        service.printStatement(bill);
        if (!InputHelper.confirm("Delete bill " + bill.getId() + "?")) {
            Format.info("Delete cancelled.");
            return;
        }
        service.delete(bill);
        Format.success("Bill deleted.");
    }
}
