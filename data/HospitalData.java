package data;

import exceptions.RecordNotFoundException;
import models.Admin;
import models.Admission;
import models.Appointment;
import models.Bill;
import models.Doctor;
import models.MedicalRecord;
import models.Patient;
import models.Room;
import models.User;
import util.RecordFormat;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Singleton that holds every list of the system and saves/loads them to text files.
 * All menus use this one instance, so a change made in one place is seen everywhere.
 */
public final class HospitalData {

    public static final String DEFAULT_ADMIN_USERNAME = "admin";
    public static final String DEFAULT_ADMIN_PASSWORD = "Admin1234";

    private static final Path DATA_DIR = Paths.get("hospital_data");
    private static final String ADMINS_FILE = "admins.txt";
    private static final String DOCTORS_FILE = "doctors.txt";
    private static final String PATIENTS_FILE = "patients.txt";
    private static final String APPOINTMENTS_FILE = "appointments.txt";
    private static final String RECORDS_FILE = "medical_records.txt";
    private static final String ROOMS_FILE = "rooms.txt";
    private static final String ADMISSIONS_FILE = "admissions.txt";
    private static final String BILLS_FILE = "bills.txt";
    private static final String SEQUENCES_FILE = "id_sequences.txt";

    private static HospitalData instance;

    private final List<Admin> admins = new ArrayList<>();
    private final List<Doctor> doctors = new ArrayList<>();
    private final List<Patient> patients = new ArrayList<>();
    private final List<Appointment> appointments = new ArrayList<>();
    private final List<MedicalRecord> medicalRecords = new ArrayList<>();
    private final List<Room> rooms = new ArrayList<>();
    private final List<Admission> admissions = new ArrayList<>();
    private final List<Bill> bills = new ArrayList<>();
    // Last number used per ID prefix, so IDs of deleted records are never given out again.
    private final Map<String, Integer> lastIds = new HashMap<>();

    private HospitalData() {
    }

    public static HospitalData getInstance() {
        if (instance == null) {
            instance = new HospitalData();
        }
        return instance;
    }

    // ---------------------------------------------------------------- loading and saving

    public void load() {
        // User files gained a middle name field; files saved before that have one field less.
        loadFile(ADMINS_FILE, 7, 8, Admin::fromRecord, admins);
        loadFile(DOCTORS_FILE, 14, 15, Doctor::fromRecord, doctors);
        loadFile(PATIENTS_FILE, 15, 16, Patient::fromRecord, patients);
        loadFile(APPOINTMENTS_FILE, 8, Appointment::fromRecord, appointments);
        loadFile(RECORDS_FILE, 9, MedicalRecord::fromRecord, medicalRecords);
        loadFile(ROOMS_FILE, 4, Room::fromRecord, rooms);
        loadFile(ADMISSIONS_FILE, 7, Admission::fromRecord, admissions);
        loadFile(BILLS_FILE, 10, Bill::fromRecord, bills);
        loadSequences();

        if (admins.isEmpty()) {
            Admin admin = new Admin(nextAdminId(), "System", "Administrator",
                    DEFAULT_ADMIN_USERNAME, DEFAULT_ADMIN_PASSWORD);
            admin.setMustChangePassword(true);
            admins.add(admin);
            save();
            System.out.println("[i] First run: default admin account created (username: "
                    + DEFAULT_ADMIN_USERNAME + ", password: " + DEFAULT_ADMIN_PASSWORD + ").");
            System.out.println("[i] You will be asked to change this password after logging in.");
        }
    }

    private <T> void loadFile(String fileName, int fieldCount, Function<String[], T> parser, List<T> target) {
        loadFile(fileName, fieldCount, fieldCount, parser, target);
    }

    private <T> void loadFile(String fileName, int minFields, int maxFields, Function<String[], T> parser,
                              List<T> target) {
        Path file = DATA_DIR.resolve(fileName);
        target.clear();
        if (!Files.exists(file)) {
            return;
        }
        int lineNumber = 0;
        int skipped = 0;
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) {
                    continue;
                }
                String[] fields = RecordFormat.split(line);
                if (fields.length < minFields || fields.length > maxFields) {
                    skipped++;
                    continue;
                }
                try {
                    target.add(parser.apply(fields));
                } catch (RuntimeException e) {
                    // A corrupted line (bad date, number, etc.) should not stop the whole program.
                    skipped++;
                }
            }
        } catch (IOException e) {
            System.out.println("[!] Could not read " + fileName + ": " + e.getMessage());
        }
        if (skipped > 0) {
            System.out.println("[!] " + skipped + " invalid line(s) in " + fileName + " were skipped (" + lineNumber
                    + " line(s) read).");
        }
    }

    /** Saves every list. Called after each change so no data is lost if the program is closed. */
    public boolean save() {
        try {
            Files.createDirectories(DATA_DIR);
            writeFile(ADMINS_FILE, admins, Admin::toRecord);
            writeFile(DOCTORS_FILE, doctors, Doctor::toRecord);
            writeFile(PATIENTS_FILE, patients, Patient::toRecord);
            writeFile(APPOINTMENTS_FILE, appointments, Appointment::toRecord);
            writeFile(RECORDS_FILE, medicalRecords, MedicalRecord::toRecord);
            writeFile(ROOMS_FILE, rooms, Room::toRecord);
            writeFile(ADMISSIONS_FILE, admissions, Admission::toRecord);
            writeFile(BILLS_FILE, bills, Bill::toRecord);
            writeFile(SEQUENCES_FILE, new ArrayList<>(lastIds.entrySet()),
                    e -> RecordFormat.join(e.getKey(), String.valueOf(e.getValue())));
            return true;
        } catch (IOException e) {
            System.out.println("[!] Could not save data: " + e.getMessage());
            return false;
        }
    }

    /** Writes to a temporary file first, then replaces the real file, so a crash cannot leave half a file. */
    private <T> void writeFile(String fileName, List<T> items, Function<T, String> formatter) throws IOException {
        Path file = DATA_DIR.resolve(fileName);
        Path temp = DATA_DIR.resolve(fileName + ".tmp");
        try (BufferedWriter writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
            for (T item : items) {
                writer.write(formatter.apply(item));
                writer.newLine();
            }
        }
        Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
    }

    // ---------------------------------------------------------------- IDs

    private void loadSequences() {
        lastIds.clear();
        List<String[]> rows = new ArrayList<>();
        loadFile(SEQUENCES_FILE, 2, f -> f, rows);
        for (String[] row : rows) {
            try {
                lastIds.put(row[0], Integer.parseInt(row[1]));
            } catch (NumberFormatException ignored) {
                // the highest existing ID will be used instead
            }
        }
    }

    private String nextId(String prefix, int digits, List<String> existingIds) {
        int max = lastIds.getOrDefault(prefix, 0);
        for (String id : existingIds) {
            if (id.startsWith(prefix)) {
                try {
                    max = Math.max(max, Integer.parseInt(id.substring(prefix.length())));
                } catch (NumberFormatException ignored) {
                    // not one of our generated IDs
                }
            }
        }
        lastIds.put(prefix, max + 1);
        return prefix + String.format("%0" + digits + "d", max + 1);
    }

    private static <T> List<String> ids(List<T> items, Function<T, String> idGetter) {
        List<String> result = new ArrayList<>();
        for (T item : items) {
            result.add(idGetter.apply(item));
        }
        return result;
    }

    public String nextAdminId() {
        return nextId("AD", 3, ids(admins, User::getId));
    }

    public String nextDoctorId() {
        return nextId("D", 4, ids(doctors, User::getId));
    }

    public String nextPatientId() {
        return nextId("P", 4, ids(patients, User::getId));
    }

    public String nextAppointmentId() {
        return nextId("APT", 4, ids(appointments, Appointment::getId));
    }

    public String nextMedicalRecordId() {
        return nextId("MR", 4, ids(medicalRecords, MedicalRecord::getId));
    }

    public String nextAdmissionId() {
        return nextId("ADM", 4, ids(admissions, Admission::getId));
    }

    public String nextBillId() {
        return nextId("B", 4, ids(bills, Bill::getId));
    }

    // ---------------------------------------------------------------- users

    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>(admins);
        users.addAll(doctors);
        users.addAll(patients);
        return users;
    }

    public Optional<User> findUserByUsername(String username) {
        for (User user : getAllUsers()) {
            if (user.getUsername().equalsIgnoreCase(username)) {
                return Optional.of(user);
            }
        }
        return Optional.empty();
    }

    public boolean isUsernameTaken(String username) {
        return findUserByUsername(username).isPresent();
    }

    public Patient findPatient(String id) throws RecordNotFoundException {
        for (Patient p : patients) {
            if (p.getId().equalsIgnoreCase(id)) {
                return p;
            }
        }
        throw new RecordNotFoundException("No patient found with ID " + id + ".");
    }

    public Doctor findDoctor(String id) throws RecordNotFoundException {
        for (Doctor d : doctors) {
            if (d.getId().equalsIgnoreCase(id)) {
                return d;
            }
        }
        throw new RecordNotFoundException("No doctor found with ID " + id + ".");
    }

    public Appointment findAppointment(String id) throws RecordNotFoundException {
        for (Appointment a : appointments) {
            if (a.getId().equalsIgnoreCase(id)) {
                return a;
            }
        }
        throw new RecordNotFoundException("No appointment found with ID " + id + ".");
    }

    public MedicalRecord findMedicalRecord(String id) throws RecordNotFoundException {
        for (MedicalRecord r : medicalRecords) {
            if (r.getId().equalsIgnoreCase(id)) {
                return r;
            }
        }
        throw new RecordNotFoundException("No medical record found with ID " + id + ".");
    }

    public Room findRoom(String roomNumber) throws RecordNotFoundException {
        for (Room r : rooms) {
            if (r.getRoomNumber().equalsIgnoreCase(roomNumber)) {
                return r;
            }
        }
        throw new RecordNotFoundException("No room found with number " + roomNumber + ".");
    }

    public Admission findAdmission(String id) throws RecordNotFoundException {
        for (Admission a : admissions) {
            if (a.getId().equalsIgnoreCase(id)) {
                return a;
            }
        }
        throw new RecordNotFoundException("No admission found with ID " + id + ".");
    }

    public Bill findBill(String id) throws RecordNotFoundException {
        for (Bill b : bills) {
            if (b.getId().equalsIgnoreCase(id)) {
                return b;
            }
        }
        throw new RecordNotFoundException("No bill found with ID " + id + ".");
    }

    /** Name for display even if the patient was already deleted. */
    public String patientName(String id) {
        try {
            return findPatient(id).getFullName();
        } catch (RecordNotFoundException e) {
            return "(removed patient " + id + ")";
        }
    }

    public String doctorName(String id) {
        try {
            return findDoctor(id).getDisplayName();
        } catch (RecordNotFoundException e) {
            return "(removed doctor " + id + ")";
        }
    }

    // ---------------------------------------------------------------- lists

    public List<Admin> getAdmins() {
        return admins;
    }

    public List<Doctor> getDoctors() {
        return doctors;
    }

    public List<Patient> getPatients() {
        return patients;
    }

    public List<Appointment> getAppointments() {
        return appointments;
    }

    public List<MedicalRecord> getMedicalRecords() {
        return medicalRecords;
    }

    public List<Room> getRooms() {
        return rooms;
    }

    public List<Admission> getAdmissions() {
        return admissions;
    }

    public List<Bill> getBills() {
        return bills;
    }
}
