package models;

import java.time.LocalDateTime;

public class Order {
    private final String userName;
    private final OrderType type;
    private final double limitPrice;
    private int amount; // לא final כי הכמות יכולה לרדת אם הפקודה מתבצעת חלקית
    private final LocalDateTime timestamp;

    public Order(String userName, OrderType type, double limitPrice, int amount) {
        this.userName = userName;
        this.type = type;
        this.limitPrice = limitPrice;
        this.amount = amount;
        this.timestamp = LocalDateTime.now(); // מתעד את רגע יצירת הפקודה לטובת סדר קדימויות
    }

    public String getUserName() { return userName; }
    public OrderType getType() { return type; }
    public double getLimitPrice() { return limitPrice; }
    public int getAmount() { return amount; }
    public LocalDateTime getTimestamp() { return timestamp; }

    // פונקציה שמפחיתה מהכמות כשהפקודה מתממשת (חלקית או מלאה)
    public void decreaseAmount(int amountToDecrease) {
        if (amountToDecrease > this.amount) {
            throw new IllegalArgumentException("Cannot decrease more than the existing amount.");
        }
        this.amount -= amountToDecrease;
    }

    // בודק אם הפקודה מומשה במלואה
    public boolean isFulfilled() {
        return this.amount == 0;
    }
}