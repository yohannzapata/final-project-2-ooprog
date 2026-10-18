package services;

import data.HospitalData;
import exceptions.InvalidInputException;
import exceptions.RecordNotFoundException;
import exceptions.RoomNotAvailableException;
import models.Admission;
import models.Bill;
import models.Patient;
import models.Room;
import util.Format;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Admitting and discharging patients. Keeps the room status in sync with admissions.
 */
public class AdmissionService {

    /** Late encoding of an admission is allowed, but only within the last 30 days. */
    public static final int MAX_BACKDATE_DAYS = 30;

    private final HospitalData data = HospitalData.getInstance();

    public Admission getActiveAdmission(String patientId) {
        for (Admission a : data.getAdmissions()) {
            if (a.getPatientId().equals(patientId) && !a.isDischarged()) {
                return a;
            }
        }
        return null;
    }

    public void checkCanAdmit(Patient patient) throws InvalidInputException {
        Admission active = getActiveAdmission(patient.getId());
        if (active != null) {
            throw new InvalidInputException(patient.getFullName() + " is already admitted in room "
                    + active.getRoomNumber() + " (" + active.getId() + ").");
        }
    }

    public void checkRoomAvailable(Room room) throws RoomNotAvailableException {
        if (!room.isAvailable()) {
            throw new RoomNotAvailableException("Room " + room.getRoomNumber() + " is "
                    + room.getStatus().getLabel().toLowerCase() + ". Please choose an available room.");
        }
    }

    public void validateAdmissionDate(Patient patient, LocalDate date) throws InvalidInputException {
        LocalDate today = LocalDate.now();
        if (date.isAfter(today)) {
            throw new InvalidInputException("Admission date cannot be in the future.");
        }
        if (date.isBefore(today.minusDays(MAX_BACKDATE_DAYS))) {
            throw new InvalidInputException("Admission date can only be up to " + MAX_BACKDATE_DAYS + " days ago.");
        }
        if (date.isBefore(patient.getBirthdate())) {
            throw new InvalidInputException("Admission date cannot be before the patient's birthdate.");
        }
    }

    public Admission admit(Patient patient, Room room, LocalDate date, String reason)
            throws InvalidInputException, RoomNotAvailableException {
        checkCanAdmit(patient);
        checkRoomAvailable(room);
        validateAdmissionDate(patient, date);
        Admission admission = new Admission(data.nextAdmissionId(), patient.getId(), room.getRoomNumber(),
                date, reason, room.getDailyRate());
        room.setStatus(Room.Status.OCCUPIED);
        data.getAdmissions().add(admission);
        data.save();
        return admission;
    }

    public void validateDischargeDate(Admission admission, LocalDate date) throws InvalidInputException {
        if (date.isBefore(admission.getAdmissionDate())) {
            throw new InvalidInputException("Discharge date cannot be before the admission date ("
                    + Format.date(admission.getAdmissionDate()) + ").");
        }
        if (date.isAfter(LocalDate.now())) {
            throw new InvalidInputException("Discharge date cannot be in the future.");
        }
    }

    public void discharge(Admission admission, LocalDate date) throws InvalidInputException {
        if (admission.isDischarged()) {
            throw new InvalidInputException("This patient was already discharged on "
                    + Format.date(admission.getDischargeDate()) + ".");
        }
        validateDischargeDate(admission, date);
        admission.discharge(date);
        freeRoom(admission.getRoomNumber());
        data.save();
    }

    /** Removes an admission that was entered by mistake. Not allowed once it has been billed. */
    public void delete(Admission admission) throws InvalidInputException {
        for (Bill bill : data.getBills()) {
            if (bill.getSource() == Bill.Source.ADMISSION && bill.getSourceId().equals(admission.getId())) {
                throw new InvalidInputException("Admission " + admission.getId() + " already has bill "
                        + bill.getId() + " and cannot be deleted.");
            }
        }
        if (!admission.isDischarged()) {
            freeRoom(admission.getRoomNumber());
        }
        data.getAdmissions().remove(admission);
        data.save();
    }

    private void freeRoom(String roomNumber) {
        try {
            Room room = data.findRoom(roomNumber);
            if (room.getStatus() == Room.Status.OCCUPIED) {
                room.setStatus(Room.Status.AVAILABLE);
            }
        } catch (RecordNotFoundException e) {
            // room was removed; nothing to free
        }
    }

    public List<Admission> getByPatient(String patientId) {
        List<Admission> result = new ArrayList<>();
        for (Admission a : data.getAdmissions()) {
            if (a.getPatientId().equals(patientId)) {
                result.add(a);
            }
        }
        return result;
    }

    public void printTable(List<Admission> list) {
        if (list.isEmpty()) {
            Format.info("No admissions found.");
            return;
        }
        String fmt = "%-8s %-22s %-7s %-13s %-13s %-5s %-10s%n";
        System.out.printf(fmt, "ID", "Patient", "Room", "Admitted", "Discharged", "Days", "Status");
        Format.line(84);
        for (Admission a : list) {
            System.out.printf(fmt, a.getId(), Format.fit(data.patientName(a.getPatientId()), 22), a.getRoomNumber(),
                    Format.date(a.getAdmissionDate()), Format.date(a.getDischargeDate()), a.getDaysStayed(),
                    a.getStatusLabel());
        }
    }

    public void printDetails(Admission a) {
        System.out.println("Admission ID   : " + a.getId());
        System.out.println("Patient        : " + data.patientName(a.getPatientId()) + " (" + a.getPatientId() + ")");
        System.out.println("Room           : " + a.getRoomNumber() + " @ " + Format.money(a.getDailyRate()) + "/day");
        System.out.println("Reason         : " + a.getReason());
        System.out.println("Admitted       : " + Format.date(a.getAdmissionDate()));
        System.out.println("Discharged     : " + Format.date(a.getDischargeDate()));
        System.out.println("Days           : " + a.getDaysStayed() + (a.isDischarged() ? "" : " (so far)"));
        System.out.println("Status         : " + a.getStatusLabel());
    }
}
