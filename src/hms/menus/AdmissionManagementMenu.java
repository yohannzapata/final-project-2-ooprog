package hms.menus;

import hms.exceptions.InvalidInputException;
import hms.exceptions.RoomNotAvailableException;
import hms.models.Admission;
import hms.models.Patient;
import hms.models.Room;
import hms.services.AdmissionService;
import hms.util.Format;
import hms.util.InputHelper;
import hms.util.Validator;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Admin: admit patients to available rooms and discharge them.
 */
public class AdmissionManagementMenu extends Menu {

    private final AdmissionService service = new AdmissionService();

    @Override
    protected String getTitle() {
        return "ADMISSION MANAGEMENT";
    }

    @Override
    protected String[] getOptions() {
        return new String[]{
                "Admit patient",
                "View current admissions",
                "View all admissions",
                "View admission details",
                "Discharge patient",
                "Delete admission (entered by mistake)"
        };
    }

    @Override
    protected void handle(int choice) throws InvalidInputException, RoomNotAvailableException {
        switch (choice) {
            case 1:
                admit();
                break;
            case 2:
                viewCurrent();
                break;
            case 3:
                service.printTable(data.getAdmissions());
                break;
            case 4:
                service.printDetails(Prompts.select("Enter admission ID: ", data::findAdmission));
                break;
            case 5:
                discharge();
                break;
            case 6:
                delete();
                break;
            default:
                break;
        }
    }

    private void admit() throws InvalidInputException, RoomNotAvailableException {
        List<Room> available = new ArrayList<>();
        for (Room r : data.getRooms()) {
            if (r.isAvailable()) {
                available.add(r);
            }
        }
        if (available.isEmpty()) {
            throw new RoomNotAvailableException("There are no available rooms right now.");
        }
        Patient patient = Prompts.selectPatient();
        service.checkCanAdmit(patient);

        System.out.println("Available rooms:");
        RoomManagementMenu.printTable(available);
        Room room = Prompts.select("Enter room number: ", data::findRoom);
        service.checkRoomAvailable(room);

        LocalDate date = InputHelper.readOptional("Admission date (yyyy-MM-dd, press Enter for today): ", s -> {
            LocalDate d = Validator.parseDate(s, "Admission date");
            service.validateAdmissionDate(patient, d);
            return d;
        });
        if (date == null) {
            date = LocalDate.now();
        }
        String reason = InputHelper.readText("Reason for admission: ", "Reason", 150, true);

        Admission admission = service.admit(patient, room, date, reason);
        Format.success(patient.getFullName() + " admitted to room " + room.getRoomNumber() + " (" + admission.getId() + ").");
    }

    private void viewCurrent() {
        List<Admission> current = new ArrayList<>();
        for (Admission a : data.getAdmissions()) {
            if (!a.isDischarged()) {
                current.add(a);
            }
        }
        service.printTable(current);
    }

    private void discharge() throws InvalidInputException {
        Admission admission = Prompts.select("Enter admission ID: ", data::findAdmission);
        if (admission.isDischarged()) {
            throw new InvalidInputException("This patient was already discharged on "
                    + Format.date(admission.getDischargeDate()) + ".");
        }
        service.printDetails(admission);
        LocalDate date = InputHelper.readOptional("Discharge date (yyyy-MM-dd, press Enter for today): ", s -> {
            LocalDate d = Validator.parseDate(s, "Discharge date");
            service.validateDischargeDate(admission, d);
            return d;
        });
        if (date == null) {
            date = LocalDate.now();
        }
        if (!InputHelper.confirm("Discharge " + data.patientName(admission.getPatientId()) + " on " + Format.date(date) + "?")) {
            Format.info("Discharge cancelled.");
            return;
        }
        service.discharge(admission, date);
        Format.success("Patient discharged. Room " + admission.getRoomNumber() + " is now available.");
        Format.info("Days to bill: " + admission.getDaysStayed() + ". Create the bill under Manage Bills.");
    }

    private void delete() throws InvalidInputException {
        Admission admission = Prompts.select("Enter admission ID: ", data::findAdmission);
        service.printDetails(admission);
        if (!InputHelper.confirm("Delete this admission record? Use this only if it was entered by mistake.")) {
            Format.info("Delete cancelled.");
            return;
        }
        service.delete(admission);
        Format.success("Admission deleted.");
    }
}
