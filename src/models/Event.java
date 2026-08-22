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
     * Calculates and deposits the initial subsidy for the event pool,
     * independently of the specific trading method type.
     */
    public void initializeSubsidy() {
        // Event simply requests the cost from the trading method without knowing its internal calculation logic.
        double initialSubsidy = tradingMethod.calculateInitialSubsidy(options.size());

        if (initialSubsidy > 0) {
            // Deposit subsidy funds into the account (without commission)
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
        this.winningOptionIndex = winningOptionIndex;
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

        // 1. Calculate the shares cost via the trading method
        double stockCost = tradingMethod.calculateCost(options, optionIndex, sharesToBuy);

        // 2. Event calculates the commission based on business rules
        double commission = 0.0;
        if (this.collectionType == CollectionType.ON_PURCHASE) {
            commission = stockCost * (commissionRate / 100.0);
        }

        double totalCost = stockCost + commission;

        // 3. Execute the trade
        tradingMethod.executeTrade(options, optionIndex, sharesToBuy);

        // 4. Record in the event account and history
        EventOption chosenOption = options.get(optionIndex);
        account.addTransaction(chosenOption.getOptionTitle(), sharesToBuy, stockCost, commission);

        // 5. Return the receipt to the UI
        return new PurchaseReceipt(stockCost, commission, totalCost);
    }
}