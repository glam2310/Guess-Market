package models;

public enum CollectionType {
    ON_PURCHASE,
    ON_CLOSE;

    @Override
    public String toString() {
        switch (this) {
            case ON_PURCHASE:
                return "On Purchase";
            case ON_CLOSE:
                return "On Close";
            default:
                return super.toString();
        }
    }
}
