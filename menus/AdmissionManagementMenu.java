package menus;

import exceptions.InvalidInputException;
import exceptions.RoomNotAvailableException;
import models.Admission;
import models.Bill;
import models.Patient;
import models.Room;
import services.AdmissionService;
import services.AppointmentService;
import services.BillingService;
import util.Format;
import util.InputHelper;
import util.Validator;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Admin: admit patients to available rooms and discharge them.
 */
public class AdmissionManagementMenu extends Menu {

    private final AdmissionService service = new AdmissionService();
    private final BillingService billing = new BillingService();

    @Override
    protected String getTitle() {
        return "ADMIN MENU > ADMISSION MANAGEMENT";
    }

    @Override
    protected void printSummary() {
        System.out.println("Currently admitted: " + service.getActiveAdmissions().size()
                + " | Available rooms: " + service.getAvailableRooms().size());
        if (service.getAvailableRooms().isEmpty()) {
            Format.info("No room is available, so patients cannot be admitted right now.");
        }
        System.out.println();
    }

    @Override
    protected List<Option> getOptions() {
        boolean canAdmit = !service.getAvailableRooms().isEmpty() && !service.getAdmittablePatients().isEmpty();
        boolean hasAdmissions = !data.getAdmissions().isEmpty();
        boolean hasActive = !service.getActiveAdmissions().isEmpty();
        List<Option> options = new ArrayList<>();
        addIf(options, canAdmit, "Admit patient", this::admit);
        addIf(options, hasActive, "View current admissions", () -> service.printTable(service.getActiveAdmissions()));
        addIf(options, hasAdmissions, "View all admissions", () -> service.printTable(data.getAdmissions()));
        addIf(options, hasAdmissions, "View admission details", this::viewDetails);
        addIf(options, hasActive, "Discharge patient", this::discharge);
        addIf(options, !getDeletableAdmissions().isEmpty(), "Delete admission (entered by mistake)", this::delete);
        return options;
    }

    private void admit() throws InvalidInputException, RoomNotAvailableException {
        System.out.println("Patients who can be admitted:");
        PatientManagementMenu.printTable(service.getAdmittablePatients());
        Patient patient = Prompts.select("Enter patient ID: ", id -> {
            Patient found = data.findPatient(id);
            service.checkCanAdmit(found);
            return found;
        });

        System.out.println("Available rooms:");
        RoomManagementMenu.printTable(service.getAvailableRooms());
        Room room = Prompts.select("Enter room number: ", number -> {
            Room found = data.findRoom(number);
            if (!found.isAvailable()) {
                throw new InvalidInputException("Room " + found.getRoomNumber() + " is "
                        + found.getStatus().getLabel().toLowerCase() + ". Please choose an available room.");
            }
            return found;
        });

        LocalDate date = InputHelper.read("Admission date (yyyy-MM-dd, or \"today\"): ", s -> {
            LocalDate d = Validator.parseDateOrToday(s, "Admission date");
            service.validateAdmissionDate(patient, room, d);
            return d;
        });
        String reason = InputHelper.readText("Reason for admission: ", "Reason", 150, true);

        Admission admission = service.admit(patient, room, date, reason);
        Format.success(patient.getFullName() + " admitted to room " + room.getRoomNumber() + " (" + admission.getId() + ").");
        int upcoming = new AppointmentService().getUpcoming(new AppointmentService().getByPatient(patient.getId())).size();
        if (upcoming > 0) {
            Format.info(patient.getFullName() + " still has " + upcoming + " upcoming appointment(s). Cancel them under "
                    + "Manage appointments if the patient cannot attend.");
        }
    }

    private void viewDetails() {
        service.printTable(data.getAdmissions());
        service.printDetails(Prompts.select("Enter admission ID: ", data::findAdmission));
    }

    private void discharge() throws InvalidInputException {
        service.printTable(service.getActiveAdmissions());
        Admission admission = Prompts.select("Enter admission ID: ", id -> {
            Admission found = data.findAdmission(id);
            if (found.isDischarged()) {
                throw new InvalidInputException("This patient was already discharged on "
                        + Format.date(found.getDischargeDate()) + ".");
            }
            return found;
        });
        service.printDetails(admission);
        LocalDate date = InputHelper.read("Discharge date (yyyy-MM-dd, or \"today\"): ", s -> {
            LocalDate d = Validator.parseDateOrToday(s, "Discharge date");
            service.validateDischargeDate(admission, d);
            return d;
        });
        if (!InputHelper.confirm("Discharge " + data.patientName(admission.getPatientId()) + " on " + Format.date(date) + "?")) {
            Format.info("Discharge cancelled.");
            return;
        }
        service.discharge(admission, date);
        Format.success("Patient discharged. Room " + admission.getRoomNumber() + " is now available.");
        Format.info("Days to bill: " + admission.getDaysStayed() + ". Create the bill under Manage bills.");
    }

    /** Admissions that have not been billed yet; a billed admission is part of the records and is kept. */
    private List<Admission> getDeletableAdmissions() {
        List<Admission> result = new ArrayList<>();
        for (Admission a : data.getAdmissions()) {
            if (billing.findExistingBill(Bill.Source.ADMISSION, a.getId()) == null) {
                result.add(a);
            }
        }
        return result;
    }

    private void delete() throws InvalidInputException {
        System.out.println("Only admissions that have not been billed can be deleted:");
        service.printTable(getDeletableAdmissions());
        Admission admission = Prompts.select("Enter admission ID: ", id -> {
            Admission found = data.findAdmission(id);
            Bill bill = billing.findExistingBill(Bill.Source.ADMISSION, found.getId());
            if (bill != null) {
                throw new InvalidInputException("Admission " + found.getId() + " already has bill " + bill.getId()
                        + " and cannot be deleted.");
            }
            return found;
        });
        service.printDetails(admission);
        if (!InputHelper.confirm("Delete this admission record? Use this only if it was entered by mistake.")) {
            Format.info("Delete cancelled.");
            return;
        }
        service.delete(admission);
        Format.success("Admission deleted.");
    }
}
