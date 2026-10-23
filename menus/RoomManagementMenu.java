package menus;

import exceptions.InvalidInputException;
import models.Admission;
import models.Room;
import services.AdmissionService;
import util.Format;
import util.InputHelper;
import util.Table;
import util.Validator;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Admin: add, view, update and delete rooms.
 */
public class RoomManagementMenu extends Menu {

    public static final BigDecimal MAX_DAILY_RATE = new BigDecimal("100000");

    private final AdmissionService admissions = new AdmissionService();

    @Override
    protected String getTitle() {
        return "ADMIN MENU > ROOM MANAGEMENT";
    }

    @Override
    protected void printSummary() {
        System.out.println("Rooms: " + data.getRooms().size() + " | Available: " + admissions.getAvailableRooms().size());
        if (data.getRooms().isEmpty()) {
            Format.info("Add a room to see the other options.");
        }
        System.out.println();
    }

    @Override
    protected List<Option> getOptions() {
        boolean hasRooms = !data.getRooms().isEmpty();
        boolean hasAvailable = !admissions.getAvailableRooms().isEmpty();
        List<Option> options = new ArrayList<>();
        options.add(new Option("Add room", this::addRoom));
        addIf(options, hasRooms, "View all rooms", () -> printTable(data.getRooms()));
        addIf(options, hasAvailable, "View available rooms", () -> printTable(admissions.getAvailableRooms()));
        addIf(options, hasRooms, "Update room", this::updateRoom);
        addIf(options, !getDeletableRooms().isEmpty(), "Delete room", this::deleteRoom);
        return options;
    }

    private void addRoom() {
        Format.cancelHint();
        String number = InputHelper.read("Room number (e.g. 201 or ICU-1): ", s -> checkNewRoomNumber(Validator.validateRoomNumber(s)));
        Room.Type type = readType();
        BigDecimal rate = InputHelper.readAmount("Daily rate (" + Format.PESO + "): ", "Daily rate", MAX_DAILY_RATE);
        Room room = new Room(number, type, rate);
        data.getRooms().add(room);
        data.save();
        Format.success("Room " + number + " (" + type.getLabel() + ") added.");
    }

    /** Checked while typing, so a taken number is simply asked again. */
    private String checkNewRoomNumber(String number) throws InvalidInputException {
        for (Room r : data.getRooms()) {
            if (r.getRoomNumber().equalsIgnoreCase(number)) {
                throw new InvalidInputException("Room " + number + " already exists. Enter a different room number.");
            }
        }
        return number;
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

    private void updateRoom() {
        printTable(data.getRooms());
        Room room = Prompts.select("Enter room number: ", data::findRoom);
        System.out.println("Updating room " + room.getRoomNumber() + ". Type \"" + InputHelper.KEEP_WORD
                + "\" to keep the current value.");
        System.out.println("Current type: " + room.getType().getLabel());
        Room.Type type = InputHelper.readYesNo("Change room type?") ? readType() : null;
        BigDecimal rate = InputHelper.readOptional("Daily rate [" + Format.money(room.getDailyRate()) + "]: ",
                s -> Validator.validateAmount(s, "Daily rate", MAX_DAILY_RATE));

        Room.Status status = null;
        if (room.getStatus() == Room.Status.OCCUPIED) {
            Format.info("Status cannot be changed while the room is occupied. Discharge the patient first.");
        } else {
            System.out.println("Current status: " + room.getStatus().getLabel());
            System.out.println("  1. Available");
            System.out.println("  2. Under Maintenance");
            Integer s = InputHelper.readOptional("New status (type \"" + InputHelper.KEEP_WORD + "\" to keep): ",
                    in -> Validator.validateRange(Validator.parseInt(in, "Status"), 1, 2, "Status"));
            if (s != null) {
                status = s == 1 ? Room.Status.AVAILABLE : Room.Status.MAINTENANCE;
            }
        }

        if (type == null && rate == null && status == null) {
            Format.info("No changes made.");
            return;
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

    /**
     * Only available rooms that were never used can be deleted. A room with admission records is kept,
     * otherwise a new room with the same number would look like it was part of those old stays.
     * Returns null if the room can be deleted.
     */
    private String getDeleteBlocker(Room room) {
        if (!room.isAvailable()) {
            return "Room " + room.getRoomNumber() + " is " + room.getStatus().getLabel().toLowerCase()
                    + ". Only available rooms can be deleted.";
        }
        for (Admission a : data.getAdmissions()) {
            if (a.getRoomNumber().equals(room.getRoomNumber())) {
                return "Room " + room.getRoomNumber() + " has admission records and cannot be deleted. "
                        + "Set it to Under Maintenance instead if it should no longer be used.";
            }
        }
        return null;
    }

    private List<Room> getDeletableRooms() {
        List<Room> result = new ArrayList<>();
        for (Room r : data.getRooms()) {
            if (getDeleteBlocker(r) == null) {
                result.add(r);
            }
        }
        return result;
    }

    private void deleteRoom() {
        System.out.println("Only available rooms without admission records can be deleted:");
        printTable(getDeletableRooms());
        Room room = Prompts.select("Enter room number: ", number -> {
            Room found = data.findRoom(number);
            String blocker = getDeleteBlocker(found);
            if (blocker != null) {
                throw new InvalidInputException(blocker);
            }
            return found;
        });
        if (!InputHelper.confirm("Delete room " + room.getRoomNumber() + "?")) {
            Format.info("Delete cancelled.");
            return;
        }
        data.getRooms().remove(room);
        data.save();
        Format.success("Room deleted.");
    }

    public static void printTable(List<Room> rooms) {
        if (rooms.isEmpty()) {
            Format.info("No rooms found.");
            return;
        }
        Table table = new Table("Room", "Type", "Daily Rate", "Status").alignRight(2);
        for (Room r : rooms) {
            table.addRow(r.getRoomNumber(), r.getType().getLabel(), Format.money(r.getDailyRate()), r.getStatus().getLabel());
        }
        table.print();
    }
}
