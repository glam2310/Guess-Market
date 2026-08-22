package models;

public enum EventStatus {
    INACTIVE,
    ACTIVE,
    CLOSED;

    @Override
    public String toString() {
        switch (this) {
            case INACTIVE:
                return "Not Started";
            case ACTIVE:
                return "Active";
            case CLOSED:
                return "Closed";
            default:
                return super.toString();
        }
    }
}
