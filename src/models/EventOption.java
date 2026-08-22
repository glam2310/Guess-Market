package models;

public class EventOption {
    private final String optionTitle;
    private int shares;// q

    public EventOption(String optionTitle) {
        this.optionTitle = optionTitle;
        this.shares = 0;
    }

    public String getOptionTitle() {
        return optionTitle;
    }

    public int getShares() {
        return shares;
    }

    public void addShares(int additionalShares) {
        this.shares += additionalShares;
    }

    @Override
    public String toString() {
        return "EventOption{" +
                "optionTitle='" + optionTitle + '\'' +
                '}';
    }
}
