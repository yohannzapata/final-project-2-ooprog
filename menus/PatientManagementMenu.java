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
        return "ADMIN MENU > PATIENT MANAGEMENT";
    }

    @Override
    protected void printSummary() {
        System.out.println("Patients on file: " + data.getPatients().size());
        if (data.getPatients().isEmpty()) {
            Format.info("Add a patient to see the other options.");
        }
        System.out.println();
    }

    @Override
    protected List<Option> getOptions() {
        boolean hasPatients = !data.getPatients().isEmpty();
        List<Option> options = new ArrayList<>();
        options.add(new Option("Add patient", this::addPatient));
        addIf(options, hasPatients, "View all patients", () -> printTable(data.getPatients()));
        addIf(options, hasPatients, "Search patients", this::search);
        addIf(options, hasPatients, "View patient details", this::viewDetails);
        addIf(options, hasPatients, "Update patient", this::updatePatient);
        addIf(options, !getDeletablePatients().isEmpty(), "Delete patient", this::deletePatient);
        return options;
    }

    private void addPatient() throws DuplicateRecordException, InvalidInputException {
        Patient patient = PatientForm.create(true);
        Format.success("Patient " + patient.getFullName() + " added with ID " + patient.getId() + ".");
        Format.info("Give the username and temporary password to the patient. They must change it on first login.");
    }

    /** Searches until something is found or the user stops. */
    private void search() {
        boolean isSearching = true;
        while (isSearching) {
            String keyword = InputHelper.readRequired("Enter name, patient ID or mobile number: ", "Search keyword")
                    .toLowerCase(Locale.ROOT);
            List<Patient> results = findPatients(keyword);
            printTable(results);
            isSearching = results.isEmpty() && InputHelper.readYesNo("Search again?");
        }
    }

    private List<Patient> findPatients(String keyword) {
        List<Patient> results = new ArrayList<>();
        for (Patient p : data.getPatients()) {
            if (p.getId().toLowerCase(Locale.ROOT).contains(keyword)
                    || p.getFullName().toLowerCase(Locale.ROOT).contains(keyword)
                    || p.getContactNumber().contains(keyword)) {
                results.add(p);
            }
        }
        return results;
    }

    private void viewDetails() {
        printTable(data.getPatients());
        PatientForm.printProfile(Prompts.selectPatient());
    }

    private void updatePatient() {
        printTable(data.getPatients());
        Patient patient = Prompts.selectPatient();
        System.out.println("Updating " + patient.getFullName() + " (" + patient.getId() + ")");
        PatientForm.update(patient, true);
        Format.success("Patient record updated.");
    }

    /** Patients with history are kept for record purposes and cannot be deleted. Returns null if there is none. */
    private String getHistoryReason(Patient patient) {
        for (Appointment a : data.getAppointments()) {
            if (a.getPatientId().equals(patient.getId())) {
                return patient.getFullName() + " has appointment records and cannot be deleted.";
            }
        }
        for (Bill b : data.getBills()) {
            if (b.getPatientId().equals(patient.getId())) {
                return patient.getFullName() + " has bills and cannot be deleted.";
            }
        }
        for (Admission a : data.getAdmissions()) {
            if (a.getPatientId().equals(patient.getId())) {
                return patient.getFullName() + " has admission records and cannot be deleted.";
            }
        }
        for (MedicalRecord r : data.getMedicalRecords()) {
            if (r.getPatientId().equals(patient.getId())) {
                return patient.getFullName() + " has medical records and cannot be deleted.";
            }
        }
        return null;
    }

    private List<Patient> getDeletablePatients() {
        List<Patient> result = new ArrayList<>();
        for (Patient p : data.getPatients()) {
            if (getHistoryReason(p) == null) {
                result.add(p);
            }
        }
        return result;
    }

    private void deletePatient() {
        System.out.println("Only patients without appointments, admissions, bills or records can be deleted:");
        printTable(getDeletablePatients());
        Patient patient = Prompts.select("Enter patient ID: ", id -> {
            Patient found = data.findPatient(id);
            String reason = getHistoryReason(found);
            if (reason != null) {
                throw new InvalidInputException(reason);
            }
            return found;
        });
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
