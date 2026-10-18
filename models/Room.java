package models;

import util.RecordFormat;

import java.math.BigDecimal;

/**
 * A hospital room. One room holds one admitted patient at a time.
 */
public class Room {

    public enum Type {
        WARD("Ward"),
        SEMI_PRIVATE("Semi-Private"),
        PRIVATE("Private"),
        ICU("ICU");

        private final String label;

        Type(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    public enum Status {
        AVAILABLE("Available"),
        OCCUPIED("Occupied"),
        MAINTENANCE("Under Maintenance");

        private final String label;

        Status(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    private final String roomNumber;
    private Type type;
    private BigDecimal dailyRate;
    private Status status;

    public Room(String roomNumber, Type type, BigDecimal dailyRate) {
        this.roomNumber = roomNumber;
        this.type = type;
        this.dailyRate = dailyRate;
        this.status = Status.AVAILABLE;
    }

    public boolean isAvailable() {
        return status == Status.AVAILABLE;
    }

    public String toRecord() {
        return RecordFormat.join(roomNumber, type.name(), dailyRate.toPlainString(), status.name());
    }

    public static Room fromRecord(String[] f) {
        Room room = new Room(f[0], Type.valueOf(f[1]), new BigDecimal(f[2]));
        room.status = Status.valueOf(f[3]);
        return room;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public BigDecimal getDailyRate() {
        return dailyRate;
    }

    public void setDailyRate(BigDecimal dailyRate) {
        if (dailyRate == null || dailyRate.signum() <= 0) {
            throw new IllegalArgumentException("Daily rate must be greater than 0.");
        }
        this.dailyRate = dailyRate;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}
