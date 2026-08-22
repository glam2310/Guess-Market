package models;

import java.util.ArrayList;
import java.util.List;

public class TradingAccount {
    private double balance;          // Current balance of the market pool
    private double totalCommissions; // Cumulative commissions collected for the report
    private List<Transaction> transactions = new ArrayList<>(); // רשימה לשמירת היסטוריית המסחר עבור פקודה 3

    // מחלקה פנימית לתיעוד שורת מסחר
    public static class Transaction {
        private String optionName;
        private int sharesCount;
        private double totalPaid;

        public Transaction(String optionName, int sharesCount, double totalPaid) {
            this.optionName = optionName;
            this.sharesCount = sharesCount;
            this.totalPaid = totalPaid;
        }

        public String getOptionName() { return optionName; }
        public int getSharesCount() { return sharesCount; }
        public double getTotalPaid() { return totalPaid; }
    }

    /**
     * Constructs a new TradingAccount with an initial balance and commissions set to zero.
     */
    public TradingAccount() {
        this.balance = 0.0;
        this.totalCommissions = 0.0;
        this.transactions = new ArrayList<>();
    }

    public double getBalance() {
        return balance;
    }

    public double getTotalCommissions() {
        return totalCommissions;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    /**
     * Processes a trade transaction by adding the stock cost and commission
     * to the total balance, while keeping track of the cumulative commissions.
     *
     * @param stockCost  the calculated cost of the purchased shares
     * @param commission the commission fee added to the transaction
     */
    public void addTransaction(double stockCost, double commission) {
        double totalPaid = stockCost + commission;
        this.balance += totalPaid;
        this.totalCommissions += commission;
    }

    // מתודת עזר שגם מעדכנת את היתרות וגם מתעדת את העסקה להיסטוריה
    public void addTransaction(String optionName, int shares, double stockCost, double commission) {
        addTransaction(stockCost, commission);
        double totalPaid = stockCost + commission;
        transactions.add(new Transaction(optionName, shares, totalPaid));
    }

    /**
     * משלמת לזוכים ומעדכנת את חשבון האירוע בעת סגירתו
     */
    public void processEventClosure(double netPayout, double commissionOnClose) {
        // הכסף שמשולם לזוכים יוצא מיתרת החשבון
        this.balance -= netPayout;

        // עמלת סגירה (אם יש) מתווספת לסך העמלות שנאספו
        this.totalCommissions += commissionOnClose;
    }
}