package models;

import java.time.LocalDateTime;

public class TradeExecution {
    public static final String MINT_PARTY = "MINT";

    private final String buyerName;
    private final String sellerName; // יישמר כ-"MINT" אם המניות נוצרו מהבשלה
    private final int optionIndex;
    private final int shares;
    private final double pricePerShare;
    private final LocalDateTime executionTime;

    public TradeExecution(String buyerName, String sellerName, int optionIndex, int shares, double pricePerShare) {
        this.buyerName = buyerName;
        this.sellerName = sellerName;
        this.optionIndex = optionIndex;
        this.shares = shares;
        this.pricePerShare = pricePerShare;
        this.executionTime = LocalDateTime.now();
    }

    public String getBuyerName() { return buyerName; }
    public String getSellerName() { return sellerName; }
    public int getOptionIndex() { return optionIndex; }
    public int getShares() { return shares; }
    public double getPricePerShare() { return pricePerShare; }
    public LocalDateTime getExecutionTime() { return executionTime; }

    // חישוב עזר לסך כל העסקה
    public double getTotalCost() {
        return shares * pricePerShare;
    }

    public boolean isMint() {
        return MINT_PARTY.equals(sellerName) || MINT_PARTY.equals(buyerName);
    }
}