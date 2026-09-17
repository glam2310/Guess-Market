package dto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserDTO {
    private final String name;
    private final double balance;
    private final boolean isBlocked;
    private final List<Integer> marketMakerEventIds;
    private final List<Integer> participatingEventIds;
    private final Map<Integer, int[]> portfolio;
    private final Map<Integer, double[]> amountPaidPerOption;
    private final Map<Integer, Double> commissionPaidPerEvent;
    private final Map<Integer, Double> closedProfitLoss;
    private final List<UserTradeRecord> tradeHistory;

    public UserDTO(String name, double balance, boolean isBlocked,
                   List<Integer> marketMakerEventIds, List<Integer> participatingEventIds,
                   Map<Integer, int[]> portfolio, Map<Integer, double[]> amountPaidPerOption,
                   Map<Integer, Double> commissionPaidPerEvent, Map<Integer, Double> closedProfitLoss,
                   List<UserTradeRecord> tradeHistory) {
        this.name = name;
        this.balance = balance;
        this.isBlocked = isBlocked;
        this.marketMakerEventIds = marketMakerEventIds != null ? marketMakerEventIds : new ArrayList<>();
        this.participatingEventIds = participatingEventIds != null ? participatingEventIds : new ArrayList<>();
        this.portfolio = portfolio != null ? portfolio : new HashMap<>();
        this.amountPaidPerOption = amountPaidPerOption != null ? amountPaidPerOption : new HashMap<>();
        this.commissionPaidPerEvent = commissionPaidPerEvent != null ? commissionPaidPerEvent : new HashMap<>();
        this.closedProfitLoss = closedProfitLoss != null ? closedProfitLoss : new HashMap<>();
        this.tradeHistory = tradeHistory != null ? tradeHistory : new ArrayList<>();
    }

    public String getName() { return name; }
    public double getBalance() { return balance; }
    public boolean isBlocked() { return isBlocked; }
    public List<Integer> getMarketMakerEventIds() { return marketMakerEventIds; }
    public List<Integer> getParticipatingEventIds() { return participatingEventIds; }
    public Map<Integer, int[]> getPortfolio() { return portfolio; }
    public Map<Integer, double[]> getAmountPaidPerOption() { return amountPaidPerOption; }
    public Map<Integer, Double> getCommissionPaidPerEvent() { return commissionPaidPerEvent; }
    public Map<Integer, Double> getClosedProfitLoss() { return closedProfitLoss; }
    public List<UserTradeRecord> getTradeHistory() { return tradeHistory; }
}
