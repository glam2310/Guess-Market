package models;

import dto.UserTradeRecord;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class User {
    private String name;
    private double balance;
    private boolean isBlocked;
    private boolean permanentlyBlocked;
    private List<Integer> marketMakerEventIds;
    private Map<Integer, int[]> portfolio;
    private final Set<Integer> participatingEventIds;
    private final Map<Integer, double[]> amountPaidPerOption;
    private final Map<Integer, Double> commissionPaidPerEvent;
    private final Map<Integer, Double> closedProfitLoss;
    private final List<UserTradeRecord> tradeHistory;
    private double reservedCash;
    private final Map<Integer, int[]> reservedShares;

    public User() {
        this.marketMakerEventIds = new ArrayList<>();
        this.isBlocked = false;
        this.permanentlyBlocked = false;
        this.portfolio = new HashMap<>();
        this.participatingEventIds = new HashSet<>();
        this.amountPaidPerOption = new HashMap<>();
        this.commissionPaidPerEvent = new HashMap<>();
        this.closedProfitLoss = new HashMap<>();
        this.tradeHistory = new ArrayList<>();
        this.reservedShares = new HashMap<>();
    }

    public User(String name, double initialCash) {
        this();
        this.name = name;
        this.balance = initialCash;
        this.isBlocked = initialCash < 0;
        this.permanentlyBlocked = initialCash < 0;
    }

    public String getName() { return name; }
    public double getBalance() { return balance; }
    public boolean isBlocked() { return isBlocked || permanentlyBlocked; }
    public List<Integer> getMarketMakerEventIds() { return marketMakerEventIds; }
    public Map<Integer, int[]> getPortfolio() { return portfolio; }
    public List<Integer> getParticipatingEventIds() { return new ArrayList<>(participatingEventIds); }
    public Map<Integer, double[]> getAmountPaidPerOption() { return amountPaidPerOption; }
    public Map<Integer, Double> getCommissionPaidPerEvent() { return commissionPaidPerEvent; }
    public Map<Integer, Double> getClosedProfitLoss() { return closedProfitLoss; }
    public List<UserTradeRecord> getTradeHistory() { return tradeHistory; }
    public double getAvailableBalance() { return balance - reservedCash; }

    public void addMarketMakerEventId(int eventId) {
        this.marketMakerEventIds.add(eventId);
    }

    public void markParticipation(int eventId) {
        participatingEventIds.add(eventId);
    }

    public void addBalance(double amount) {
        this.balance += amount;
        if (!permanentlyBlocked && this.balance >= 0) {
            this.isBlocked = false;
        }
    }

    public void deductBalance(double amount) {
        this.balance -= amount;
        if (this.balance < 0) {
            this.isBlocked = true;
            this.permanentlyBlocked = true;
        }
    }

    public void updateBalance(double amount) {
        if (amount >= 0) {
            addBalance(amount);
        } else {
            deductBalance(Math.abs(amount));
        }
    }

    public void clearReservations() {
        this.reservedCash = 0;
        this.reservedShares.clear();
    }

    public void reserveCash(double amount) {
        this.reservedCash += amount;
    }

    public void releaseCash(double amount) {
        this.reservedCash = Math.max(0, this.reservedCash - amount);
    }

    public void reserveShares(int eventId, int optionIndex, int shares) {
        reservedShares.putIfAbsent(eventId, new int[]{0, 0});
        reservedShares.get(eventId)[optionIndex] += shares;
    }

    public void releaseShares(int eventId, int optionIndex, int shares) {
        if (!reservedShares.containsKey(eventId)) return;
        reservedShares.get(eventId)[optionIndex] = Math.max(0, reservedShares.get(eventId)[optionIndex] - shares);
    }

    public int getAvailableShares(int eventId, int optionIndex) {
        int owned = portfolio.getOrDefault(eventId, new int[]{0, 0})[optionIndex];
        int reserved = reservedShares.getOrDefault(eventId, new int[]{0, 0})[optionIndex];
        return owned - reserved;
    }

    public void addShares(int eventId, int optionIndex, int shares) {
        portfolio.putIfAbsent(eventId, new int[]{0, 0});
        portfolio.get(eventId)[optionIndex] += shares;
        markParticipation(eventId);
    }

    public void removeShares(int eventId, int optionIndex, int shares) {
        if (portfolio.containsKey(eventId)) {
            portfolio.get(eventId)[optionIndex] -= shares;
        }
    }

    public void recordPurchase(int eventId, int optionIndex, double amountPaid, double commission) {
        amountPaidPerOption.putIfAbsent(eventId, new double[]{0.0, 0.0});
        amountPaidPerOption.get(eventId)[optionIndex] += amountPaid;
        commissionPaidPerEvent.merge(eventId, commission, Double::sum);
    }

    public void recordTrade(UserTradeRecord record) {
        tradeHistory.add(0, record);
        markParticipation(record.getEventId());
    }

    public void recordClosedProfitLoss(int eventId, double pnl) {
        closedProfitLoss.put(eventId, pnl);
    }
}
