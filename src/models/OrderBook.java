package models;

import java.util.ArrayList;
import java.util.List;

public class OrderBook implements TradingMethod {
    private final int initial;
    private final double d;
    private final boolean allowMint;

    // רשימות הפקודות (אינדקס 0 = אפשרות ראשונה/YES, אינדקס 1 = אפשרות שנייה/NO)
    private final List<List<Order>> buyOrders;
    private final List<List<Order>> sellOrders;

    // שמירת מחיר העסקה האחרונה (LAST) עבור כל אפשרות
    private final double[] lastPrices;

    public OrderBook(int initial, int d, boolean allowMint) {
        this(initial, (double) d, allowMint);
    }

    public OrderBook(int initial, double d, boolean allowMint) {
        this.initial = initial;
        this.d = d;
        this.allowMint = allowMint;

        this.buyOrders = new ArrayList<>();
        this.sellOrders = new ArrayList<>();

        // תמיד יש בדיוק 2 אפשרויות ב-Guess Market (YES/NO)
        for (int i = 0; i < 2; i++) {
            this.buyOrders.add(new ArrayList<>());
            this.sellOrders.add(new ArrayList<>());
        }

        // אתחול מחיר אחרון ל-0
        this.lastPrices = new double[]{0.0, 0.0};
    }

    // === פונקציות הוספה וניהול תור הפקודות ===

    public void addOrder(Order order, int optionIndex) {
        if (order.getType() == OrderType.BUY) {
            buyOrders.get(optionIndex).add(order);
            // מיון קונים: מחיר מהגבוה לנמוך (עדיפות למרבה במחיר).
            // במקרה של שוויון: הישן קודם (FIFO)
            buyOrders.get(optionIndex).sort((o1, o2) -> {
                int priceCompare = Double.compare(o2.getLimitPrice(), o1.getLimitPrice());
                if (priceCompare != 0) return priceCompare;
                return o1.getTimestamp().compareTo(o2.getTimestamp());
            });
        } else {
            sellOrders.get(optionIndex).add(order);
            // מיון מוכרים: מחיר מהנמוך לגבוה (עדיפות למוכר הזול).
            // במקרה של שוויון: הישן קודם (FIFO)
            sellOrders.get(optionIndex).sort((o1, o2) -> {
                int priceCompare = Double.compare(o1.getLimitPrice(), o2.getLimitPrice());
                if (priceCompare != 0) return priceCompare;
                return o1.getTimestamp().compareTo(o2.getTimestamp());
            });
        }
    }

    // === סטטיסטיקות הנדרשות ל-UI עפ"י ההוראות ===

    // מחיר עסקה אחרונה (LAST)
    public double getLastPrice(int optionIndex) {
        return lastPrices[optionIndex];
    }

    public void updateLastPrice(int optionIndex, double price) {
        this.lastPrices[optionIndex] = price;
    }

    // הצעת הקנייה הגבוהה ביותר (BID)
    public double getHighestBid(int optionIndex) {
        List<Order> bids = buyOrders.get(optionIndex);
        if (bids.isEmpty()) return 0.0;
        return bids.get(0).getLimitPrice(); // התור ממוין, לכן הראשון הוא הגבוה ביותר
    }

    // הצעת המכירה הנמוכה ביותר (ASK)
    public double getLowestAsk(int optionIndex) {
        List<Order> asks = sellOrders.get(optionIndex);
        if (asks.isEmpty()) return 0.0;
        return asks.get(0).getLimitPrice(); // התור ממוין, לכן הראשון הוא הנמוך ביותר
    }

    // המרווח (SPREAD)
    public double getSpread(int optionIndex) {
        double bid = getHighestBid(optionIndex);
        double ask = getLowestAsk(optionIndex);
        if (bid == 0.0 || ask == 0.0) return 0.0; // אי אפשר לחשב מרווח אם חסר צד
        return ask - bid;
    }

    // מחיר אמצע (MID)
    public double getMidPrice(int optionIndex) {
        double bid = getHighestBid(optionIndex);
        double ask = getLowestAsk(optionIndex);
        if (bid == 0.0 && ask == 0.0) return 0.0;
        if (bid == 0.0) return ask;
        if (ask == 0.0) return bid;
        return (bid + ask) / 2.0;
    }

    // גטרים שחושפים את התורים במקרה שצריך לקרוא אותם מבחוץ
    public List<Order> getBuyOrders(int optionIndex) { return buyOrders.get(optionIndex); }
    public List<Order> getSellOrders(int optionIndex) { return sellOrders.get(optionIndex); }

    public int getInitial() { return initial; }
    public double getD() { return d; }
    public boolean isAllowMint() { return allowMint; }
    public double getMaxLimitPrice() { return Math.max(0.01, d - 0.01); }
    public int getInitialPairs() {
        if (d <= 0) return 0;
        return (int) Math.round(initial / d);
    }

    // === מימוש הממשק TradingMethod ===

    @Override
    public double calculateInitialSubsidy(int numberOfOptions) {
        return initial; // ב-OB הסובסידיה היא בעצם סכום ההשקעה הראשוני של ה-MM
    }

    @Override
    public double calculatePrice(List<EventOption> options, int optionIndex) {
        return getMidPrice(optionIndex); // קירוב השווי ב-OB הוא מחיר האמצע
    }

    @Override
    public double calculateCost(List<EventOption> options, int optionIndex, int sharesToBuy) {
        // ב-Order Book, עלות מדויקת תלויה בפקודות שממתינות בספר, לכן כרגע מחזירים 0
        return 0.0;
    }

    @Override
    public void executeTrade(List<EventOption> options, int optionIndex, int sharesToBuy) {
        // הפונקציה הזו הגיעה מ-LMSR.
        // עבור Order Book אנחנו ניצור פונקציה חכמה יותר שתקבל אובייקט Order ותבצע Matching.
    }

    // === מנגנון ה-Matching (הפגשת פקודות) - מעודכן להחזרת עסקאות ===

    public List<TradeExecution> processOrder(Order newOrder, int optionIndex) {
        addOrder(newOrder, optionIndex);

        List<TradeExecution> executions = new ArrayList<>();
        boolean matched = true;

        while (matched) {
            matched = false;

            if (!buyOrders.get(optionIndex).isEmpty() && !sellOrders.get(optionIndex).isEmpty()) {
                Order highestBid = buyOrders.get(optionIndex).get(0);
                Order lowestAsk = sellOrders.get(optionIndex).get(0);

                if (highestBid.getLimitPrice() + 1e-9 >= lowestAsk.getLimitPrice()) {
                    executions.add(executeStandardMatch(highestBid, lowestAsk, optionIndex));
                    matched = true;
                    continue;
                }
            }

            if (canMint()) {
                Order bidYes = buyOrders.get(0).get(0);
                Order bidNo = buyOrders.get(1).get(0);
                executions.addAll(executeMintMatch(bidYes, bidNo));
                matched = true;
            }
        }
        return executions;
    }

    private boolean canMint() {
        if (!allowMint || buyOrders.get(0).isEmpty() || buyOrders.get(1).isEmpty()) {
            return false;
        }
        long yesCents = Math.round(buyOrders.get(0).get(0).getLimitPrice() * 100.0);
        long noCents = Math.round(buyOrders.get(1).get(0).getLimitPrice() * 100.0);
        long dCents = Math.round(d * 100.0);
        return yesCents + noCents >= dCents;
    }

    private TradeExecution executeStandardMatch(Order highestBid, Order lowestAsk, int optionIndex) {
        int tradeVolume = Math.min(highestBid.getAmount(), lowestAsk.getAmount());

        double executionPrice = highestBid.getTimestamp().isBefore(lowestAsk.getTimestamp()) ?
                highestBid.getLimitPrice() : lowestAsk.getLimitPrice();

        highestBid.decreaseAmount(tradeVolume);
        lowestAsk.decreaseAmount(tradeVolume);
        updateLastPrice(optionIndex, executionPrice);

        if (highestBid.isFulfilled()) buyOrders.get(optionIndex).remove(highestBid);
        if (lowestAsk.isFulfilled()) sellOrders.get(optionIndex).remove(lowestAsk);

        // מחזירים את אובייקט הקבלה!
        return new TradeExecution(
                highestBid.getUserName(),
                lowestAsk.getUserName(),
                optionIndex,
                tradeVolume,
                executionPrice
        );
    }

    private List<TradeExecution> executeMintMatch(Order bidYes, Order bidNo) {
        int tradeVolume = Math.min(bidYes.getAmount(), bidNo.getAmount());
        double executionPriceYes, executionPriceNo;

        if (bidYes.getTimestamp().isBefore(bidNo.getTimestamp())) {
            executionPriceYes = bidYes.getLimitPrice();
            executionPriceNo = d - executionPriceYes;
        } else {
            executionPriceNo = bidNo.getLimitPrice();
            executionPriceYes = d - executionPriceNo;
        }

        bidYes.decreaseAmount(tradeVolume);
        bidNo.decreaseAmount(tradeVolume);
        updateLastPrice(0, executionPriceYes);
        updateLastPrice(1, executionPriceNo);

        if (bidYes.isFulfilled()) buyOrders.get(0).remove(bidYes);
        if (bidNo.isFulfilled()) buyOrders.get(1).remove(bidNo);

        // ב-Mint אין "מוכר" אמיתי, לכן נרשום "MINT" בתור המוכר כדי שהמנוע יידע
        List<TradeExecution> mintExecutions = new ArrayList<>();
        mintExecutions.add(new TradeExecution(bidYes.getUserName(), TradeExecution.MINT_PARTY, 0, tradeVolume, executionPriceYes));
        mintExecutions.add(new TradeExecution(bidNo.getUserName(), TradeExecution.MINT_PARTY, 1, tradeVolume, executionPriceNo));

        return mintExecutions;
    }
}