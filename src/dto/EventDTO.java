package dto;

import java.util.ArrayList;
import java.util.List;

public class EventDTO {
    private final int id;
    private final String title;
    private final String description;
    private final String status;
    private final String methodType;
    private final int commissionRate;
    private final String collectionType;
    private final double accountBalance;
    private final double totalCommissions;
    private final Integer winningOptionIndex;
    private final String winningOptionTitle;
    private final boolean allowMint;
    private final double maxLimitPrice;
    private final List<OptionMarketDTO> options;
    private final List<ParticipantDTO> participants;
    private final List<String> tradeHistoryLines;

    public EventDTO(int id, String title, String description, String status, String methodType,
                    int commissionRate, String collectionType, double accountBalance, double totalCommissions,
                    Integer winningOptionIndex, String winningOptionTitle, boolean allowMint, double maxLimitPrice,
                    List<OptionMarketDTO> options, List<ParticipantDTO> participants, List<String> tradeHistoryLines) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.status = status;
        this.methodType = methodType;
        this.commissionRate = commissionRate;
        this.collectionType = collectionType;
        this.accountBalance = accountBalance;
        this.totalCommissions = totalCommissions;
        this.winningOptionIndex = winningOptionIndex;
        this.winningOptionTitle = winningOptionTitle;
        this.allowMint = allowMint;
        this.maxLimitPrice = maxLimitPrice;
        this.options = options != null ? options : new ArrayList<>();
        this.participants = participants != null ? participants : new ArrayList<>();
        this.tradeHistoryLines = tradeHistoryLines != null ? tradeHistoryLines : new ArrayList<>();
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }
    public String getMethodType() { return methodType; }
    public int getCommissionRate() { return commissionRate; }
    public String getCollectionType() { return collectionType; }
    public double getAccountBalance() { return accountBalance; }
    public double getTotalCommissions() { return totalCommissions; }
    public Integer getWinningOptionIndex() { return winningOptionIndex; }
    public String getWinningOptionTitle() { return winningOptionTitle; }
    public boolean isAllowMint() { return allowMint; }
    public double getMaxLimitPrice() { return maxLimitPrice; }
    public List<OptionMarketDTO> getOptions() { return options; }
    public List<ParticipantDTO> getParticipants() { return participants; }
    public List<String> getTradeHistoryLines() { return tradeHistoryLines; }

    public boolean isOrderBook() { return "Order Book".equals(methodType); }
    public boolean isLmsr() { return "LMSR".equals(methodType); }
    public boolean isActive() { return "Active".equals(status); }
    public boolean isClosed() { return "Closed".equals(status); }
    public boolean isNotStarted() { return "Not Started".equals(status); }
}
