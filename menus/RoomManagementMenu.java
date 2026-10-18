package menus;

import exceptions.DuplicateRecordException;
import exceptions.InvalidInputException;
import models.Admission;
import models.Room;
import util.Format;
import util.InputHelper;
import util.Validator;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Admin: add, view, update and delete rooms.
 */
public class RoomManagementMenu extends Menu {

    public static final BigDecimal MAX_DAILY_RATE = new BigDecimal("100000");

    @Override
    protected String getTitle() {
        return "ROOM MANAGEMENT";
    }

    @Override
    protected String[] getOptions() {
        return new String[]{
                "Add room",
                "View all rooms",
                "View available rooms",
                "Update room",
                "Delete room"
        };
    }

    @Override
    protected void handle(int choice) throws InvalidInputException, DuplicateRecordException {
        switch (choice) {
            case 1:
                addRoom();
                break;
            case 2:
                printTable(data.getRooms());
                break;
            case 3:
                printTable(getAvailableRooms());
                break;
            case 4:
                updateRoom();
                break;
            case 5:
                deleteRoom();
                break;
            default:
                break;
        }
    }

    private void addRoom() throws DuplicateRecordException {
        String number = InputHelper.read("Room number (e.g. 201 or ICU-01): ", Validator::validateRoomNumber);
        for (Room r : data.getRooms()) {
            if (r.getRoomNumber().equalsIgnoreCase(number)) {
                throw new DuplicateRecordException("Room " + number + " already exists.");
            }
        }
        Room.Type type = readType();
        BigDecimal rate = InputHelper.readAmount("Daily rate (PHP): ", "Daily rate", MAX_DAILY_RATE);
        Room room = new Room(number, type, rate);
        data.getRooms().add(room);
        data.save();
        Format.success("Room " + number + " (" + type.getLabel() + ") added.");
    }

    private Room.Type readType() {
        Room.Type[] types = Room.Type.values();
        System.out.println("Room types:");
        for (int i = 0; i < types.length; i++) {
            System.out.println("  " + (i + 1) + ". " + types[i].getLabel());
        }
        int choice = InputHelper.readInt("Choose room type: ", 1, types.length, "Room type");
        return types[choice - 1];
    }

    private void updateRoom() throws InvalidInputException {
        Room room = Prompts.select("Enter room number: ", data::findRoom);
        System.out.println("Updating room " + room.getRoomNumber() + ". Press Enter to keep the current value.");
        System.out.println("Current type: " + room.getType().getLabel());
        Room.Type type = InputHelper.readYesNo("Change room type?") ? readType() : null;
        BigDecimal rate = InputHelper.readOptional("Daily rate [" + room.getDailyRate() + "]: ",
                s -> Validator.validateAmount(s, "Daily rate", MAX_DAILY_RATE));

        Room.Status status = null;
        if (room.getStatus() == Room.Status.OCCUPIED) {
            Format.info("Status cannot be changed while the room is occupied. Discharge the patient first.");
        } else {
            System.out.println("Current status: " + room.getStatus().getLabel());
            System.out.println("  1. Available");
            System.out.println("  2. Under Maintenance");
            Integer s = InputHelper.readOptional("New status (press Enter to keep): ",
                    in -> Validator.validateRange(Validator.parseInt(in, "Status"), 1, 2, "Status"));
            if (s != null) {
                status = s == 1 ? Room.Status.AVAILABLE : Room.Status.MAINTENANCE;
            }
        }

        if (type != null) {
            room.setType(type);
        }
        if (rate != null) {
            room.setDailyRate(rate);
        }
        if (status != null) {
            room.setStatus(status);
        }
        data.save();
        Format.success("Room " + room.getRoomNumber() + " updated.");
        if (rate != null && room.getStatus() == Room.Status.OCCUPIED) {
            Format.info("The current patient keeps the rate from their admission. The new rate applies to new admissions.");
        }
    }

    private void deleteRoom() throws InvalidInputException {
        Room room = Prompts.select("Enter room number: ", data::findRoom);
        if (!room.isAvailable()) {
            throw new InvalidInputException("Room " + room.getRoomNumber() + " is "
                    + room.getStatus().getLabel().toLowerCase() + ". Only available rooms can be deleted.");
        }
        for (Admission a : data.getAdmissions()) {
            if (a.getRoomNumber().equals(room.getRoomNumber()) && !a.isDischarged()) {
                throw new InvalidInputException("Room " + room.getRoomNumber() + " has an active admission.");
            }
        }
        if (!InputHelper.confirm("Delete room " + room.getRoomNumber() + "?")) {
            Format.info("Delete cancelled.");
            return;
        }
        data.getRooms().remove(room);
        data.save();
        Format.success("Room deleted.");
    }

    public List<Room> getAvailableRooms() {
        List<Room> result = new ArrayList<>();
        for (Room r : data.getRooms()) {
            if (r.isAvailable()) {
                result.add(r);
            }
        }
        return result;
    }

    public static void printTable(List<Room> rooms) {
        if (rooms.isEmpty()) {
            Format.info("No rooms found.");
            return;
        }
        String fmt = "%-8s %-14s %-16s %-18s%n";
        System.out.printf(fmt, "Room", "Type", "Daily Rate", "Status");
        Format.line(58);
        for (Room r : rooms) {
            System.out.printf(fmt, r.getRoomNumber(), r.getType().getLabel(), Format.money(r.getDailyRate()),
                    r.getStatus().getLabel());
        }
    }
}
