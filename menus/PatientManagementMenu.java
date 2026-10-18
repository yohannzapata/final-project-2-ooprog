package menus;

import exceptions.DuplicateRecordException;
import exceptions.InvalidInputException;
import models.Admission;
import models.Appointment;
import models.Bill;
import models.MedicalRecord;
import models.Patient;
import util.Format;
import util.InputHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Admin: add, view, search, update and delete patient records.
 */
public class PatientManagementMenu extends Menu {

    @Override
    protected String getTitle() {
        return "PATIENT MANAGEMENT";
    }

    @Override
    protected String[] getOptions() {
        return new String[]{
                "Add patient",
                "View all patients",
                "Search patients",
                "View patient details",
                "Update patient",
                "Delete patient"
        };
    }

    @Override
    protected void handle(int choice) throws InvalidInputException, DuplicateRecordException {
        switch (choice) {
            case 1:
                addPatient();
                break;
            case 2:
                printTable(data.getPatients());
                break;
            case 3:
                search();
                break;
            case 4:
                PatientForm.printProfile(Prompts.selectPatient());
                break;
            case 5:
                updatePatient();
                break;
            case 6:
                deletePatient();
                break;
            default:
                break;
        }
    }

    private void addPatient() throws DuplicateRecordException {
        Patient patient = PatientForm.create(true);
        Format.success("Patient " + patient.getFullName() + " added with ID " + patient.getId() + ".");
        Format.info("Give the username and temporary password to the patient. They must change it on first login.");
    }

    private void search() {
        String keyword = InputHelper.readRequired("Enter name or patient ID: ", "Search keyword").toLowerCase(Locale.ROOT);
        List<Patient> results = new ArrayList<>();
        for (Patient p : data.getPatients()) {
            if (p.getId().toLowerCase(Locale.ROOT).contains(keyword)
                    || p.getFullName().toLowerCase(Locale.ROOT).contains(keyword)
                    || p.getContactNumber().contains(keyword)) {
                results.add(p);
            }
        }
        printTable(results);
    }

    private void updatePatient() throws DuplicateRecordException {
        Patient patient = Prompts.selectPatient();
        System.out.println("Updating " + patient.getFullName() + " (" + patient.getId() + ")");
        PatientForm.update(patient, true);
        Format.success("Patient record updated.");
    }

    /** Patients with history are kept for record purposes and cannot be deleted. */
    private void deletePatient() throws InvalidInputException {
        Patient patient = Prompts.selectPatient();
        for (Appointment a : data.getAppointments()) {
            if (a.getPatientId().equals(patient.getId())) {
                throw new InvalidInputException(patient.getFullName() + " has appointment records and cannot be deleted.");
            }
        }
        for (Bill b : data.getBills()) {
            if (b.getPatientId().equals(patient.getId())) {
                throw new InvalidInputException(patient.getFullName() + " has bills and cannot be deleted.");
            }
        }
        for (Admission a : data.getAdmissions()) {
            if (a.getPatientId().equals(patient.getId())) {
                throw new InvalidInputException(patient.getFullName() + " has admission records and cannot be deleted.");
            }
        }
        for (MedicalRecord r : data.getMedicalRecords()) {
            if (r.getPatientId().equals(patient.getId())) {
                throw new InvalidInputException(patient.getFullName() + " has medical records and cannot be deleted.");
            }
        }
        if (!InputHelper.confirm("Delete patient " + patient.getFullName() + " (" + patient.getId() + ")?")) {
            Format.info("Delete cancelled.");
            return;
        }
        data.getPatients().remove(patient);
        data.save();
        Format.success("Patient deleted.");
    }

    public static void printTable(List<Patient> patients) {
        if (patients.isEmpty()) {
            Format.info("No patients found.");
            return;
        }
        String fmt = "%-6s %-26s %-4s %-7s %-12s %-16s%n";
        System.out.printf(fmt, "ID", "Name", "Age", "Sex", "Mobile", "Discount IDs");
        Format.line(76);
        for (Patient p : patients) {
            List<String> ids = new ArrayList<>();
            if (p.hasSeniorCitizenId()) {
                ids.add("Senior");
            }
            if (p.hasPwdId()) {
                ids.add("PWD");
            }
            if (p.hasPhilHealth()) {
                ids.add("PhilHealth");
            }
            System.out.printf(fmt, p.getId(), Format.fit(p.getFullName(), 26), p.getAge(), p.getSex(),
                    p.getContactNumber(), ids.isEmpty() ? "-" : String.join(", ", ids));
        }
    }
}
