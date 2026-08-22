package models;

import java.util.List;
import java.util.ArrayList;

public class Event {

    private int eventID;
    private String eventTitle;
    private String eventDescription;
    private int commissionRate;
    private CollectionType collectionType;
    private List<EventOption> options;
    private TradingMethod tradingMethod;
    private EventStatus eventStatus; // enum
    private TradingAccount account;
    private Integer winningOptionIndex = null;

    public static class PurchaseReceipt {
        private final double stockCost;
        private final double commission;
        private final double totalPaid;

        public PurchaseReceipt(double stockCost, double commission, double totalPaid) {
            this.stockCost = stockCost;
            this.commission = commission;
            this.totalPaid = totalPaid;
        }

        public double getStockCost() { return stockCost; }
        public double getCommission() { return commission; }
        public double getTotalPaid() { return totalPaid; }
    }

    public Event(int eventID, String eventTitle, String eventDescription,
                 int commissionRate, CollectionType collectionType,
                 List<EventOption> options, TradingMethod tradingMethod) {

        this.eventID = eventID;
        this.eventTitle = eventTitle;
        this.eventDescription = eventDescription;
        this.commissionRate = commissionRate;
        this.collectionType = collectionType;
        this.options = options != null ? options : new ArrayList<>();
        this.tradingMethod = tradingMethod;
        this.account = new TradingAccount();
        this.eventStatus = EventStatus.ACTIVE;
    }

    /**
     * מחשבת ומפקידה את הסבסוד ההתחלתי לקופת האירוע,
     * ללא תלות בסוג שיטת המסחר הספציפית.
     */
    public void initializeSubsidy() {
        // ה-Event פשוט מבקש את הסכום מהשיטה, מבלי לדעת איך היא מחשבת אותו
        double initialSubsidy = tradingMethod.calculateInitialSubsidy(options.size());

        if (initialSubsidy > 0) {
            // הכנסת כסף הסבסוד לקופה (ללא עמלה)
            this.account.addTransaction(initialSubsidy, 0.0);
        }
    }

    // =========================================
    // Getters & Business Logic
    // =========================================

    public int getEventID() {
        return eventID;
    }

    public TradingAccount getAccount() {
        return account;
    }

    public CollectionType getCollectionType() {
        return collectionType;
    }

    public int getCommissionRate() {
        return commissionRate;
    }

    public String getEventDescription() {
        return eventDescription;
    }

    public EventStatus getEventStatus() {
        return eventStatus;
    }

    public String getEventTitle() {
        return eventTitle;
    }

    public TradingMethod getTradingMethod() {
        return tradingMethod;
    }

    public List<EventOption> getOptions() {
        return options;
    }

    public Integer getWinningOptionIndex() {
        return winningOptionIndex;
    }

    public void closeEvent(int winningOptionIndex) {
        if (this.eventStatus == EventStatus.CLOSED) {
            throw new IllegalStateException("Event is already closed.");
        }
        if (winningOptionIndex < 0 || winningOptionIndex >= options.size()) {
            throw new IllegalArgumentException("Invalid winning option index.");
        }
        this.eventStatus = EventStatus.CLOSED;
        this.winningOptionIndex = winningOptionIndex; // <--- הוספנו את השמירה!
        EventOption winningOption = options.get(winningOptionIndex);
        double totalPayout = winningOption.getShares();
        double commission = 0.0;
        if (this.collectionType == CollectionType.ON_CLOSE) {
            commission = totalPayout * (commissionRate / 100.0);
        }
        double netPayout = totalPayout - commission;
        this.account.processEventClosure(netPayout, commission);
    }

    public double calculateTotalCost(int optionIndex, int sharesToBuy) {
        double grossCost = tradingMethod.calculateCost(options, optionIndex, sharesToBuy);
        double commissionAmount = grossCost * (commissionRate / 100.0);
        return grossCost + commissionAmount;
    }

    public PurchaseReceipt purchaseShares(int optionIndex, int sharesToBuy) {
        if (this.eventStatus != EventStatus.ACTIVE) {
            throw new IllegalStateException("Purchase failed: The event is closed.");
        }

        if (sharesToBuy <= 0) {
            throw new IllegalArgumentException("Purchase failed: Shares to buy must be positive.");
        }

        if (optionIndex < 0 || optionIndex >= options.size()) {
            throw new IllegalArgumentException("Purchase failed: Invalid option index.");
        }

        // 1. חישוב עלות המניות דרך שיטת המסחר
        double stockCost = tradingMethod.calculateCost(options, optionIndex, sharesToBuy);

        // 2. ה-Event מחשב את העמלה לפי חוקי העסק
        double commission = 0.0;
        if (this.collectionType == CollectionType.ON_PURCHASE) {
            commission = stockCost * (commissionRate / 100.0);
        }

        double totalCost = stockCost + commission;

        // 3. ביצוע המסחר בפועל
        tradingMethod.executeTrade(options, optionIndex, sharesToBuy);

        // 4. רישום בחשבון האירוע ובהיסטוריה
        EventOption chosenOption = options.get(optionIndex);
        account.addTransaction(chosenOption.getOptionTitle(), sharesToBuy, stockCost, commission);

        // 5. מחזירים קבלה ל-UI
        return new PurchaseReceipt(stockCost, commission, totalCost);
    }
}