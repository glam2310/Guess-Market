package engine;

import dto.EventDTO;
import dto.OptionMarketDTO;
import dto.OrderDTO;
import dto.ParticipantDTO;
import dto.UserDTO;
import dto.UserTradeRecord;
import exception.InvalidEventException;
import exception.InvalidMarketFileException;
import models.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class MarketEngine implements IMarketEngine {

    private List<Event> events;
    private List<User> users;

    public MarketEngine() {
        this.events = new ArrayList<>();
        this.users = new ArrayList<>();
    }

    @Override
    public void loadEventsFromXML(String filePath) throws InvalidEventException {
        try {
            MarketParser.MarketParseResult result = MarketParser.loadMarketFromXml(filePath);
            List<Event> loadedEvents = result.getEvents();
            validateLoadedEvents(loadedEvents);
            this.events = loadedEvents;
            this.users = result.getUsers();
        } catch (InvalidEventException | InvalidMarketFileException e) {
            throw new InvalidEventException(e.getMessage());
        } catch (Exception e) {
            throw new InvalidEventException("Failed to load XML file: " + e.getMessage());
        }
    }

    private void validateLoadedEvents(List<Event> loadedEvents) throws InvalidEventException {
        if (loadedEvents == null || loadedEvents.isEmpty()) {
            throw new InvalidEventException("Validation error: The XML file contains no events.");
        }
    }

    @Override
    public List<Event> getActiveEvents() {
        return this.events;
    }

    @Override
    public List<User> getUsers() {
        return this.users;
    }

    @Override
    public List<EventDTO> getActiveEventsDTO() {
        return getEventsDTO();
    }

    @Override
    public List<EventDTO> getEventsDTO() {
        if (this.events == null) return new ArrayList<>();
        return this.events.stream().map(this::toEventDTO).collect(Collectors.toList());
    }

    @Override
    public EventDTO getEventDTO(int eventId) throws Exception {
        return toEventDTO(getEventById(eventId));
    }

    @Override
    public List<UserDTO> getUsersDTO() {
        if (this.users == null) return new ArrayList<>();
        return this.users.stream().map(this::toUserDTO).collect(Collectors.toList());
    }

    @Override
    public UserDTO getUserDTO(String name) throws Exception {
        return toUserDTO(getUserByName(name));
    }

    public User getUserByName(String name) throws Exception {
        return users.stream()
                .filter(u -> u.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(() -> new Exception("User not found: " + name));
    }

    public Event getEventById(int id) throws Exception {
        return events.stream()
                .filter(e -> e.getEventID() == id)
                .findFirst()
                .orElseThrow(() -> new Exception("Event not found: " + id));
    }

    private User getMarketMakerForEvent(int eventId) {
        return users.stream()
                .filter(u -> u.getMarketMakerEventIds().contains(eventId))
                .findFirst()
                .orElse(null);
    }

    private void requireActiveEvent(Event event) throws Exception {
        if (event.getEventStatus() != EventStatus.ACTIVE) {
            throw new Exception("Trading is allowed only while the event is Active. Current status: " + event.getEventStatus());
        }
    }

    private void validateOrderBookLimitPrice(OrderBook orderBook, Order order) throws Exception {
        double maxPrice = orderBook.getMaxLimitPrice();
        if (order.getLimitPrice() <= 0 || order.getLimitPrice() > maxPrice + 1e-9) {
            throw new Exception(String.format(
                    "Invalid limit price $%.2f. Must be greater than 0 and at most $%.2f (d - 0.01).",
                    order.getLimitPrice(), maxPrice));
        }
    }

    @Override
    public void openEvent(int eventId, String actingUserName) throws Exception {
        User actor = getUserByName(actingUserName);
        if (actor.isBlocked()) {
            throw new Exception("Cannot perform action: User " + actor.getName() + " is blocked due to negative balance.");
        }
        Event event = getEventById(eventId);
        User mm = getMarketMakerForEvent(eventId);
        if (mm == null || !mm.getName().equalsIgnoreCase(actor.getName())) {
            throw new Exception("Only the Market Maker of this event can open it.");
        }
        if (event.getEventStatus() != EventStatus.INACTIVE) {
            throw new Exception("Event can be opened only from Not Started status.");
        }

        TradingMethod method = event.getTradingMethod();
        double openingCost = method.calculateInitialSubsidy(event.getOptions().size());
        if (actor.getAvailableBalance() + 1e-9 < openingCost) {
            throw new Exception(String.format(
                    "Market Maker does not have enough funds to open this event. Required: $%.2f, Available: $%.2f",
                    openingCost, actor.getAvailableBalance()));
        }

        actor.deductBalance(openingCost);
        actor.markParticipation(eventId);
        event.getAccount().addTransaction(openingCost, 0.0);

        if (method instanceof OrderBook orderBook) {
            int pairs = orderBook.getInitialPairs();
            if (pairs > 0) {
                actor.addShares(eventId, 0, pairs);
                actor.addShares(eventId, 1, pairs);
                event.getOptions().get(0).addShares(pairs);
                event.getOptions().get(1).addShares(pairs);
                actor.recordPurchase(eventId, 0, openingCost / 2.0, 0.0);
                actor.recordPurchase(eventId, 1, openingCost / 2.0, 0.0);
            }
        }

        event.activate();
    }

    @Override
    public void closeEvent(int eventId, String actingUserName, int winningOptionIndex) throws Exception {
        User actor = getUserByName(actingUserName);
        if (actor.isBlocked()) {
            throw new Exception("Cannot perform action: User " + actor.getName() + " is blocked due to negative balance.");
        }
        Event event = getEventById(eventId);
        User mm = getMarketMakerForEvent(eventId);
        if (mm == null || !mm.getName().equalsIgnoreCase(actor.getName())) {
            throw new Exception("Only the Market Maker of this event can close it.");
        }
        if (event.getEventStatus() != EventStatus.ACTIVE) {
            throw new Exception("Only an Active event can be closed.");
        }

        event.markClosed(winningOptionIndex);

        double payoutPerShare = 1.0;
        if (event.getTradingMethod() instanceof OrderBook orderBook) {
            payoutPerShare = orderBook.getD();
        }

        for (User user : users) {
            int[] shares = user.getPortfolio().getOrDefault(eventId, new int[]{0, 0});
            int winningShares = winningOptionIndex < shares.length ? shares[winningOptionIndex] : 0;
            double gross = winningShares * payoutPerShare;
            double commission = 0.0;
            if (event.getCollectionType() == CollectionType.ON_CLOSE && gross > 0) {
                commission = gross * (event.getCommissionRate() / 100.0);
            }
            double net = gross - commission;
            if (net != 0) {
                user.addBalance(net);
            }
            if (commission > 0) {
                mm.addBalance(commission);
                event.getAccount().addTransaction(0.0, commission);
            }
            if (gross > 0) {
                event.getAccount().withdraw(gross);
            }

            double amountPaid = 0.0;
            double[] paid = user.getAmountPaidPerOption().get(eventId);
            if (paid != null) {
                for (double p : paid) amountPaid += p;
            }
            amountPaid += user.getCommissionPaidPerEvent().getOrDefault(eventId, 0.0);
            user.recordClosedProfitLoss(eventId, net - amountPaid);
        }

        if (event.getTradingMethod() instanceof LMSR) {
            double leftover = event.getAccount().getBalance();
            if (leftover > 0) {
                mm.addBalance(leftover);
            }
        }
        event.getAccount().drainBalance();
    }

    private void refreshOrderBookReservations() {
        for (User user : users) {
            user.clearReservations();
        }
        for (Event event : events) {
            if (!(event.getTradingMethod() instanceof OrderBook orderBook)) continue;
            double feeMultiplier = event.getCollectionType() == CollectionType.ON_PURCHASE
                    ? (1.0 + event.getCommissionRate() / 100.0)
                    : 1.0;
            for (int i = 0; i < 2; i++) {
                for (Order buy : orderBook.getBuyOrders(i)) {
                    try {
                        User buyer = getUserByName(buy.getUserName());
                        buyer.reserveCash(buy.getLimitPrice() * buy.getAmount() * feeMultiplier);
                    } catch (Exception ignored) {
                    }
                }
                for (Order sell : orderBook.getSellOrders(i)) {
                    try {
                        User seller = getUserByName(sell.getUserName());
                        seller.reserveShares(event.getEventID(), i, sell.getAmount());
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }

    private void settleOrderBookTrades(Event event, List<TradeExecution> receipts) throws Exception {
        if (receipts == null || receipts.isEmpty()) {
            return;
        }

        User mm = getMarketMakerForEvent(event.getEventID());

        for (TradeExecution receipt : receipts) {
            User buyer = getUserByName(receipt.getBuyerName());
            double rawCost = receipt.getTotalCost();
            double commission = 0.0;
            if (event.getCollectionType() == CollectionType.ON_PURCHASE) {
                commission = rawCost * (event.getCommissionRate() / 100.0);
            }

            buyer.deductBalance(rawCost + commission);
            buyer.addShares(event.getEventID(), receipt.getOptionIndex(), receipt.getShares());
            buyer.recordPurchase(event.getEventID(), receipt.getOptionIndex(), rawCost, commission);
            buyer.recordTrade(new UserTradeRecord(
                    event.getEventID(),
                    event.getEventTitle(),
                    event.getOptions().get(receipt.getOptionIndex()).getOptionTitle(),
                    receipt.getShares(),
                    rawCost,
                    commission,
                    "Order Book"
            ));

            if (mm != null && commission > 0) {
                mm.addBalance(commission);
            }

            String optionTitle = event.getOptions().get(receipt.getOptionIndex()).getOptionTitle();
            if (receipt.isMint()) {
                event.getAccount().addTransaction(optionTitle, receipt.getShares(), rawCost, 0.0);
                event.getOptions().get(receipt.getOptionIndex()).addShares(receipt.getShares());
            } else {
                User seller = getUserByName(receipt.getSellerName());
                seller.addBalance(rawCost);
                seller.removeShares(event.getEventID(), receipt.getOptionIndex(), receipt.getShares());
                event.getAccount().addTransaction(optionTitle, receipt.getShares(), rawCost, commission);
            }
        }
    }

    @Override
    public void submitOrder(int eventId, Order newOrder, int optionIndex) throws Exception {
        User actionUser = getUserByName(newOrder.getUserName());
        if (actionUser.isBlocked()) {
            throw new Exception("Cannot perform action: User " + actionUser.getName() + " is blocked due to negative balance.");
        }

        Event event = getEventById(eventId);
        requireActiveEvent(event);
        actionUser.markParticipation(eventId);
        TradingMethod method = event.getTradingMethod();

        if (method instanceof OrderBook orderBook) {
            validateOrderBookLimitPrice(orderBook, newOrder);

            List<TradeExecution> tradeReceipts = orderBook.processOrder(newOrder, optionIndex);
            settleOrderBookTrades(event, tradeReceipts);
            refreshOrderBookReservations();
        } else if (method instanceof LMSR lmsr) {
            int sharesToBuy = newOrder.getAmount();
            double cost = lmsr.calculateCost(event.getOptions(), optionIndex, sharesToBuy);
            double commission = 0.0;
            if (event.getCollectionType() == CollectionType.ON_PURCHASE) {
                commission = cost * (event.getCommissionRate() / 100.0);
            }
            double totalCost = cost + commission;

            actionUser.deductBalance(totalCost);
            actionUser.addShares(eventId, optionIndex, sharesToBuy);
            actionUser.recordPurchase(eventId, optionIndex, cost, commission);
            actionUser.recordTrade(new UserTradeRecord(
                    eventId,
                    event.getEventTitle(),
                    event.getOptions().get(optionIndex).getOptionTitle(),
                    sharesToBuy,
                    cost,
                    commission,
                    "LMSR"
            ));

            event.getOptions().get(optionIndex).addShares(sharesToBuy);

            User mm = getMarketMakerForEvent(eventId);
            if (mm != null && commission > 0) {
                mm.addBalance(commission);
            }

            event.getAccount().addTransaction(
                    event.getOptions().get(optionIndex).getOptionTitle(),
                    sharesToBuy,
                    cost,
                    commission
            );
        } else {
            throw new Exception("Event has no valid trading method configured.");
        }
    }

    @Override
    public void submitSellOrder(int eventId, Order sellOrder, int optionIndex) throws Exception {
        User actionUser = getUserByName(sellOrder.getUserName());
        if (actionUser.isBlocked()) {
            throw new Exception("Cannot perform action: User " + actionUser.getName() + " is blocked.");
        }

        Event event = getEventById(eventId);
        requireActiveEvent(event);
        actionUser.markParticipation(eventId);
        TradingMethod method = event.getTradingMethod();

        if (method instanceof OrderBook orderBook) {
            validateOrderBookLimitPrice(orderBook, sellOrder);
            int currentShares = actionUser.getAvailableShares(eventId, optionIndex);
            if (currentShares < sellOrder.getAmount()) {
                throw new Exception("User does not have enough shares to sell. Available: " + currentShares + ", Requested: " + sellOrder.getAmount());
            }

            actionUser.reserveShares(eventId, optionIndex, sellOrder.getAmount());
            List<TradeExecution> tradeReceipts = orderBook.processOrder(sellOrder, optionIndex);
            settleOrderBookTrades(event, tradeReceipts);
            refreshOrderBookReservations();
        } else {
            throw new Exception("Selling is only supported for Order Book events.");
        }
    }

    @Override
    public double calculateOptionPrice(int eventId, int optionIndex) throws Exception {
        Event event = getEventById(eventId);
        TradingMethod method = event.getTradingMethod();
        if (method instanceof LMSR lmsr) {
            return lmsr.calculatePrice(event.getOptions(), optionIndex);
        } else if (method instanceof OrderBook orderBook) {
            double last = orderBook.getLastPrice(optionIndex);
            if (last > 0.0) return last;
            return orderBook.getMidPrice(optionIndex);
        }
        return 0.0;
    }

    private UserDTO toUserDTO(User user) {
        return new UserDTO(
                user.getName(),
                user.getBalance(),
                user.isBlocked(),
                new ArrayList<>(user.getMarketMakerEventIds()),
                user.getParticipatingEventIds(),
                copyPortfolio(user.getPortfolio()),
                copyPaid(user.getAmountPaidPerOption()),
                new HashMap<>(user.getCommissionPaidPerEvent()),
                new HashMap<>(user.getClosedProfitLoss()),
                new ArrayList<>(user.getTradeHistory())
        );
    }

    private Map<Integer, int[]> copyPortfolio(Map<Integer, int[]> source) {
        Map<Integer, int[]> copy = new HashMap<>();
        for (Map.Entry<Integer, int[]> entry : source.entrySet()) {
            copy.put(entry.getKey(), entry.getValue().clone());
        }
        return copy;
    }

    private Map<Integer, double[]> copyPaid(Map<Integer, double[]> source) {
        Map<Integer, double[]> copy = new HashMap<>();
        for (Map.Entry<Integer, double[]> entry : source.entrySet()) {
            copy.put(entry.getKey(), entry.getValue().clone());
        }
        return copy;
    }

    private EventDTO toEventDTO(Event event) {
        TradingMethod method = event.getTradingMethod();
        boolean isOb = method instanceof OrderBook;
        OrderBook orderBook = isOb ? (OrderBook) method : null;
        String methodType = isOb ? "Order Book" : "LMSR";

        List<OptionMarketDTO> optionDtos = new ArrayList<>();
        for (int i = 0; i < event.getOptions().size(); i++) {
            EventOption opt = event.getOptions().get(i);
            double live = 0.0;
            try {
                live = calculateOptionPrice(event.getEventID(), i);
            } catch (Exception ignored) {
            }
            double last = 0, bid = 0, ask = 0, mid = 0, spread = 0;
            List<OrderDTO> buys = new ArrayList<>();
            List<OrderDTO> sells = new ArrayList<>();
            if (orderBook != null) {
                last = orderBook.getLastPrice(i);
                bid = orderBook.getHighestBid(i);
                ask = orderBook.getLowestAsk(i);
                mid = orderBook.getMidPrice(i);
                spread = orderBook.getSpread(i);
                for (Order o : orderBook.getBuyOrders(i)) {
                    buys.add(new OrderDTO(o.getUserName(), "BUY", o.getAmount(), o.getLimitPrice()));
                }
                for (Order o : orderBook.getSellOrders(i)) {
                    sells.add(new OrderDTO(o.getUserName(), "SELL", o.getAmount(), o.getLimitPrice()));
                }
            }
            optionDtos.add(new OptionMarketDTO(i, opt.getOptionTitle(), opt.getShares(),
                    last, bid, ask, mid, spread, live, buys, sells));
        }

        Set<String> participantNames = new LinkedHashSet<>();
        for (User user : users) {
            if (user.getParticipatingEventIds().contains(event.getEventID())
                    || user.getMarketMakerEventIds().contains(event.getEventID())
                    || user.getPortfolio().containsKey(event.getEventID())) {
                participantNames.add(user.getName());
            }
        }
        if (orderBook != null) {
            for (int i = 0; i < 2; i++) {
                for (Order o : orderBook.getBuyOrders(i)) participantNames.add(o.getUserName());
                for (Order o : orderBook.getSellOrders(i)) participantNames.add(o.getUserName());
            }
        }

        List<ParticipantDTO> participants = new ArrayList<>();
        for (String name : participantNames) {
            try {
                User user = getUserByName(name);
                int[] shares = user.getPortfolio().getOrDefault(event.getEventID(), new int[]{0, 0});
                double[] values = new double[shares.length];
                for (int i = 0; i < shares.length; i++) {
                    double px = optionDtos.size() > i ? optionDtos.get(i).getLivePrice() : 0.0;
                    values[i] = shares[i] * px;
                }
                participants.add(new ParticipantDTO(user.getName(), shares.clone(), values));
            } catch (Exception ignored) {
            }
        }

        List<String> history = new ArrayList<>();
        List<TradingAccount.Transaction> txs = event.getAccount().getTransactions();
        for (int i = txs.size() - 1; i >= 0; i--) {
            TradingAccount.Transaction tx = txs.get(i);
            history.add(String.format("%s | %d shares | Paid $%.2f | Commission $%.2f",
                    tx.getOptionName(), tx.getSharesCount(), tx.getTotalPaid(), tx.getCommission()));
        }

        Integer winningIdx = event.getWinningOptionIndex();
        String winningTitle = null;
        if (winningIdx != null && winningIdx >= 0 && winningIdx < event.getOptions().size()) {
            winningTitle = event.getOptions().get(winningIdx).getOptionTitle();
        }

        return new EventDTO(
                event.getEventID(),
                event.getEventTitle(),
                event.getEventDescription(),
                event.getEventStatus().toString(),
                methodType,
                event.getCommissionRate(),
                event.getCollectionType().toString(),
                event.getAccount().getBalance(),
                event.getAccount().getTotalCommissions(),
                winningIdx,
                winningTitle,
                orderBook != null && orderBook.isAllowMint(),
                orderBook != null ? orderBook.getMaxLimitPrice() : 0.0,
                optionDtos,
                participants,
                history
        );
    }
}
