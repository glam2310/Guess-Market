package ui;

import dto.EventDTO;
import dto.OptionMarketDTO;
import dto.OrderDTO;
import dto.ParticipantDTO;
import dto.UserDTO;
import dto.UserTradeRecord;
import engine.IMarketEngine;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import models.Order;
import models.OrderType;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainController {

    private IMarketEngine engine;

    @FXML private Label statusLabel;
    @FXML private ProgressBar progressBar;
    @FXML private VBox centerContent;
    @FXML private ListView<String> usersListView;
    @FXML private ListView<String> eventsListView;

    private boolean isRefreshing = false;
    private String typeFilter = "All";
    private String statusFilter = "All";
    private String feeFilter = "All";
    private String lastCenterView = null;

    public void setEngine(IMarketEngine engine) {
        this.engine = engine;
    }

    @FXML
    public void initialize() {
        if (usersListView != null) {
            usersListView.setCellFactory(lv -> new javafx.scene.control.ListCell<String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    updateView(item, isSelected());
                }

                @Override
                public void updateSelected(boolean selected) {
                    super.updateSelected(selected);
                    updateView(getItem(), selected);
                }

                private void updateView(String item, boolean selected) {
                    if (item == null || emptyProperty().get()) {
                        setText(null);
                        setStyle("");
                    } else {
                        if (selected) {
                            setText("✔ " + item);
                            setStyle("-fx-font-weight: bold; -fx-text-fill: white; -fx-font-size: 14px;");
                        } else {
                            setText(item);
                            setStyle("-fx-text-fill: black; -fx-font-weight: normal;");
                        }
                    }
                }
            });

            usersListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                if (isRefreshing) return;
                showSelectedUser();
            });
            usersListView.setOnMouseClicked(e -> {
                if (isRefreshing) return;
                showSelectedUser();
            });
        }

        if (eventsListView != null) {
            installEventFilters();
            eventsListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                if (isRefreshing) return;
                showSelectedEvent();
            });
            eventsListView.setOnMouseClicked(e -> {
                if (isRefreshing) return;
                showSelectedEvent();
            });
        }
    }

    private void showSelectedUser() {
        if (engine == null || usersListView == null) return;
        String userName = extractUserName(usersListView.getSelectionModel().getSelectedItem());
        if (userName == null) return;
        try {
            lastCenterView = "user";
            displayUserDetails(engine.getUserDTO(userName));
        } catch (Exception ex) {
            statusLabel.setText("Error: " + ex.getMessage());
        }
    }

    private void showSelectedEvent() {
        if (engine == null || eventsListView == null) return;
        Integer eventId = extractEventId(eventsListView.getSelectionModel().getSelectedItem());
        if (eventId == null) return;
        try {
            lastCenterView = "event";
            displayEventDetails(engine.getEventDTO(eventId));
        } catch (Exception ex) {
            statusLabel.setText("Error: " + ex.getMessage());
        }
    }

    private void installEventFilters() {
        if (!(eventsListView.getParent() instanceof VBox parent)) return;
        int listIndex = parent.getChildren().indexOf(eventsListView);
        if (listIndex < 0) return;

        VBox filterBox = new VBox(4);
        filterBox.getChildren().add(buildFilterCombo("Type", new String[]{"All", "LMSR", "Order Book"}, value -> typeFilter = value));
        filterBox.getChildren().add(buildFilterCombo("Status", new String[]{"All", "Not Started", "Active", "Closed"}, value -> statusFilter = value));
        filterBox.getChildren().add(buildFilterCombo("Fee", new String[]{"All", "On Purchase", "On Close"}, value -> feeFilter = value));
        parent.getChildren().add(listIndex, filterBox);
    }

    private VBox buildFilterCombo(String title, String[] options, FilterConsumer onChange) {
        VBox box = new VBox(2);
        Label label = new Label(title);
        ComboBox<String> combo = new ComboBox<>();
        combo.getItems().addAll(options);
        combo.getSelectionModel().select("All");
        combo.setMaxWidth(Double.MAX_VALUE);
        combo.setOnAction(e -> {
            String selected = combo.getSelectionModel().getSelectedItem();
            if (selected != null) {
                onChange.accept(selected);
                populateLists();
            }
        });
        box.getChildren().addAll(label, combo);
        return box;
    }

    private interface FilterConsumer {
        void accept(String value);
    }

    @FXML
    private void handleLoadXml(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select XML File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML Files", "*.xml"));
        File selectedFile = fileChooser.showOpenDialog(statusLabel.getScene().getWindow());
        if (selectedFile != null) {
            runLoadingTask(selectedFile.getAbsolutePath());
        }
    }

    private void runLoadingTask(String filePath) {
        progressBar.setVisible(true);
        statusLabel.setText("Loading...");

        Task<Void> loadTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                updateProgress(10, 100);
                Thread.sleep(600);
                updateProgress(50, 100);
                engine.loadEventsFromXML(filePath);
                updateProgress(100, 100);
                return null;
            }
        };

        loadTask.setOnSucceeded(e -> {
            statusLabel.setText("File Loaded Successfully!");
            progressBar.setVisible(false);
            populateLists();
        });

        loadTask.setOnFailed(e -> {
            statusLabel.setText("Error: " + (loadTask.getException() != null ? loadTask.getException().getMessage() : "Unknown"));
            progressBar.setVisible(false);
        });

        progressBar.progressProperty().bind(loadTask.progressProperty());
        new Thread(loadTask).start();
    }

    private void populateLists() {
        isRefreshing = true;
        String activeUserName = extractUserName(usersListView != null ? usersListView.getSelectionModel().getSelectedItem() : null);
        Integer selectedEventId = extractEventId(eventsListView != null ? eventsListView.getSelectionModel().getSelectedItem() : null);

        if (usersListView != null) {
            usersListView.getItems().clear();
            for (UserDTO user : engine.getUsersDTO()) {
                usersListView.getItems().add(String.format("%s ($%.2f)", user.getName(), user.getBalance()));
            }
            if (activeUserName != null) {
                for (String item : usersListView.getItems()) {
                    if (extractUserName(item).equalsIgnoreCase(activeUserName)) {
                        usersListView.getSelectionModel().select(item);
                        break;
                    }
                }
            }
        }

        if (eventsListView != null) {
            eventsListView.getItems().clear();
            for (EventDTO ev : filteredEvents()) {
                eventsListView.getItems().add(formatEventListItem(ev));
            }
            if (selectedEventId != null) {
                for (String item : eventsListView.getItems()) {
                    if (selectedEventId.equals(extractEventId(item))) {
                        eventsListView.getSelectionModel().select(item);
                        break;
                    }
                }
            }
        }

        isRefreshing = false;
        if ("user".equals(lastCenterView)) {
            showSelectedUser();
        } else if ("event".equals(lastCenterView)) {
            showSelectedEvent();
        }
    }

    private List<EventDTO> filteredEvents() {
        List<EventDTO> result = new ArrayList<>();
        for (EventDTO ev : engine.getEventsDTO()) {
            if (!"All".equals(typeFilter) && !typeFilter.equals(ev.getMethodType())) continue;
            if (!"All".equals(statusFilter) && !statusFilter.equals(ev.getStatus())) continue;
            if (!"All".equals(feeFilter) && !feeFilter.equals(ev.getCollectionType())) continue;
            result.add(ev);
        }
        return result;
    }

    private String formatEventListItem(EventDTO ev) {
        return String.format("Event #%d: %s | %s | %s | %s %d%% | $%.2f",
                ev.getId(), ev.getTitle(), ev.getMethodType(), ev.getStatus(),
                ev.getCollectionType(), ev.getCommissionRate(), ev.getAccountBalance());
    }

    private String extractUserName(String item) {
        if (item == null) return null;
        return item.replace("✔ ", "").split(" \\(")[0];
    }

    private Integer extractEventId(String item) {
        if (item == null) return null;
        String clean = item.replace("✔ ", "");
        int hash = clean.indexOf('#');
        int colon = clean.indexOf(':');
        if (hash < 0 || colon < 0 || colon <= hash) return null;
        try {
            return Integer.parseInt(clean.substring(hash + 1, colon).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String selectedUserName() {
        return extractUserName(usersListView.getSelectionModel().getSelectedItem());
    }

    private void displayUserDetails(UserDTO user) {
        if (centerContent == null) return;
        centerContent.getChildren().clear();
        centerContent.setSpacing(15);
        centerContent.setStyle("-fx-padding: 20; -fx-background-color: #f9f9f9;");

        Label headerLabel = new Label("User Profile: " + user.getName());
        headerLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 20px; -fx-text-fill: #2c3e50;");

        VBox infoCard = new VBox(8);
        infoCard.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-border-color: #e0e0e0; -fx-border-radius: 5; -fx-background-radius: 5;");
        Label balanceLabel = new Label(String.format("Current Balance: $%.2f", user.getBalance()));
        balanceLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #27ae60;");
        String statusText = user.isBlocked() ? "Blocked (Negative Balance)" : "Active";
        String statusColor = user.isBlocked() ? "#c0392b" : "#2980b9";
        Label statusLabelUser = new Label("Account Status: " + statusText);
        statusLabelUser.setStyle("-fx-font-size: 14px; -fx-text-fill: " + statusColor + "; -fx-font-weight: bold;");
        infoCard.getChildren().addAll(balanceLabel, statusLabelUser);

        if (!user.getMarketMakerEventIds().isEmpty()) {
            infoCard.getChildren().add(new Label("Market Maker of events: " + user.getMarketMakerEventIds()));
        }

        Label participationHeader = new Label("Participating Events:");
        participationHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #34495e; -fx-padding: 10 0 0 0;");
        VBox participationBox = new VBox(8);
        if (user.getParticipatingEventIds().isEmpty()) {
            Label empty = new Label("This user has not submitted any orders yet.");
            empty.setStyle("-fx-text-fill: #7f8c8d; -fx-font-style: italic;");
            participationBox.getChildren().add(empty);
        } else {
            for (Integer eventId : user.getParticipatingEventIds()) {
                participationBox.getChildren().add(buildParticipationCard(user, eventId));
            }
        }

        centerContent.getChildren().addAll(headerLabel, infoCard, participationHeader, participationBox);
    }

    private VBox buildParticipationCard(UserDTO user, int eventId) {
        EventDTO event = engine.getEventsDTO().stream().filter(e -> e.getId() == eventId).findFirst().orElse(null);
        VBox card = new VBox(5);
        card.setStyle("-fx-background-color: white; -fx-padding: 10; -fx-border-color: #bdc3c7; -fx-border-radius: 4;");
        String title = event == null ? ("Event #" + eventId) : ("Event #" + eventId + ": " + event.getTitle());
        String method = event == null ? "" : event.getMethodType();
        card.getChildren().add(new Label(title + (method.isEmpty() ? "" : " (" + method + ")")));

        int[] shares = user.getPortfolio().getOrDefault(eventId, new int[]{0, 0});
        double[] paid = user.getAmountPaidPerOption().getOrDefault(eventId, new double[]{0.0, 0.0});
        double commission = user.getCommissionPaidPerEvent().getOrDefault(eventId, 0.0);

        if (event != null && event.isLmsr()) {
            List<UserTradeRecord> history = user.getTradeHistory().stream()
                    .filter(t -> t.getEventId() == eventId)
                    .toList();
            if (history.isEmpty()) {
                card.getChildren().add(new Label("No LMSR trades yet."));
            } else {
                for (UserTradeRecord rec : history) {
                    card.getChildren().add(new Label(String.format(
                            "%s | %d shares | Paid $%.2f | Commission $%.2f",
                            rec.getOptionTitle(), rec.getShares(), rec.getPricePaid(), rec.getCommissionPaid())));
                }
            }
            card.getChildren().add(new Label(String.format("Total commission paid: $%.2f", commission)));
            if (event.isClosed()) {
                for (int i = 0; i < event.getOptions().size(); i++) {
                    int count = i < shares.length ? shares[i] : 0;
                    card.getChildren().add(new Label("Total shares of option " + (i + 1) + " (" +
                            event.getOptions().get(i).getOptionTitle() + "): " + count));
                }
                card.getChildren().add(new Label("Winning option: " + event.getWinningOptionTitle()));
            }
        } else {
            int optionCount = event == null ? shares.length : event.getOptions().size();
            for (int i = 0; i < optionCount; i++) {
                String optionName = event != null && i < event.getOptions().size()
                        ? event.getOptions().get(i).getOptionTitle()
                        : ("Option " + (i + 1));
                int count = i < shares.length ? shares[i] : 0;
                double amount = i < paid.length ? paid[i] : 0;
                card.getChildren().add(new Label(String.format("Option %d (%s): %d shares | Paid $%.2f",
                        i + 1, optionName, count, amount)));
            }
            card.getChildren().add(new Label(String.format("Total commission paid: $%.2f", commission)));
            if (event != null && event.isClosed()) {
                Double pnl = user.getClosedProfitLoss().get(eventId);
                card.getChildren().add(new Label(String.format("Closed profit/loss: $%.2f", pnl == null ? 0.0 : pnl)));
            }
        }
        return card;
    }

    private void displayEventDetails(EventDTO event) {
        if (centerContent == null) return;
        centerContent.getChildren().clear();
        centerContent.setSpacing(15);
        centerContent.setStyle("-fx-padding: 20; -fx-background-color: #f9f9f9;");

        Label headerLabel = new Label("Event #" + event.getId() + ": " + event.getTitle());
        headerLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 20px; -fx-text-fill: #2c3e50;");

        VBox infoCard = new VBox(8);
        infoCard.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-border-color: #e0e0e0; -fx-border-radius: 5; -fx-background-radius: 5;");
        Label descLabel = new Label("Description: " + event.getDescription());
        descLabel.setWrapText(true);
        infoCard.getChildren().addAll(
                descLabel,
                new Label("Type: " + event.getMethodType()),
                new Label("Status: " + event.getStatus()),
                new Label("Commission: " + event.getCommissionRate() + "% (" + event.getCollectionType() + ")"),
                new Label(String.format("Event account (contract): $%.2f", event.getAccountBalance())),
                new Label(String.format("Total commissions collected: $%.2f", event.getTotalCommissions()))
        );
        if (event.isClosed() && event.getWinningOptionTitle() != null) {
            infoCard.getChildren().add(new Label("Winning option: " + event.getWinningOptionTitle()));
        }

        HBox managementBox = buildEventManagementBox(event);

        Label optionsHeader = new Label("Market Options & Trading:");
        optionsHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #34495e; -fx-padding: 10 0 0 0;");
        VBox optionsBox = new VBox(10);
        for (OptionMarketDTO opt : event.getOptions()) {
            optionsBox.getChildren().add(buildOptionCard(event, opt));
        }

        Label historyHeader = new Label("Event Trade History (newest first):");
        historyHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #34495e; -fx-padding: 10 0 0 0;");
        VBox historyBox = new VBox(4);
        if (event.getTradeHistoryLines().isEmpty()) {
            Label empty = new Label("No trades yet.");
            empty.setStyle("-fx-text-fill: #7f8c8d; -fx-font-style: italic;");
            historyBox.getChildren().add(empty);
        } else {
            for (String line : event.getTradeHistoryLines()) {
                historyBox.getChildren().add(new Label(line));
            }
        }

        Label participantsHeader = new Label("Event Participants:");
        participantsHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #34495e; -fx-padding: 10 0 0 0;");
        VBox participantsBox = new VBox(6);
        if (event.getParticipants().isEmpty()) {
            Label empty = new Label("No participants yet.");
            empty.setStyle("-fx-text-fill: #7f8c8d; -fx-font-style: italic;");
            participantsBox.getChildren().add(empty);
        } else {
            for (ParticipantDTO p : event.getParticipants()) {
                StringBuilder line = new StringBuilder(p.getName());
                for (int i = 0; i < p.getShares().length; i++) {
                    String optionName = i < event.getOptions().size() ? event.getOptions().get(i).getOptionTitle() : ("Option " + (i + 1));
                    line.append(String.format(" | Option %d (%s): %d shares ($%.2f)",
                            i + 1, optionName, p.getShares()[i], p.getHoldingValue()[i]));
                }
                participantsBox.getChildren().add(new Label(line.toString()));
            }
        }

        centerContent.getChildren().addAll(headerLabel, infoCard, managementBox, optionsHeader, optionsBox,
                historyHeader, historyBox, participantsHeader, participantsBox);
        if (statusLabel != null) {
            statusLabel.setText("Selected Event: " + event.getTitle());
        }
    }

    private HBox buildEventManagementBox(EventDTO event) {
        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER_LEFT);
        Button openButton = new Button("Open Event");
        Button close1 = new Button("Close: Option 1");
        Button close2 = new Button("Close: Option 2");

        openButton.setOnAction(e -> runMmAction(() -> engine.openEvent(event.getId(), requireSelectedUser()),
                "Event opened successfully."));
        close1.setOnAction(e -> runMmAction(() -> engine.closeEvent(event.getId(), requireSelectedUser(), 0),
                "Event closed. Option 1 is the winner."));
        close2.setOnAction(e -> runMmAction(() -> engine.closeEvent(event.getId(), requireSelectedUser(), 1),
                "Event closed. Option 2 is the winner."));

        boolean mmSelected = isSelectedUserMarketMaker(event.getId());
        openButton.setDisable(!mmSelected || !event.isNotStarted());
        close1.setDisable(!mmSelected || !event.isActive());
        close2.setDisable(!mmSelected || !event.isActive());
        box.getChildren().addAll(openButton, close1, close2);
        return box;
    }

    private boolean isSelectedUserMarketMaker(int eventId) {
        String name = selectedUserName();
        if (name == null) return false;
        try {
            return engine.getUserDTO(name).getMarketMakerEventIds().contains(eventId);
        } catch (Exception e) {
            return false;
        }
    }

    private String requireSelectedUser() throws Exception {
        String name = selectedUserName();
        if (name == null) {
            throw new Exception("Please select a user from the left list first!");
        }
        return name;
    }

    private void runMmAction(EngineAction action, String successMessage) {
        try {
            action.run();
            statusLabel.setText(successMessage);
            Integer eventId = extractEventId(eventsListView.getSelectionModel().getSelectedItem());
            populateLists();
            if (eventId != null) {
                displayEventDetails(engine.getEventDTO(eventId));
            }
        } catch (Exception ex) {
            statusLabel.setText("Event Error: " + ex.getMessage());
        }
    }

    private interface EngineAction {
        void run() throws Exception;
    }

    private VBox buildOptionCard(EventDTO event, OptionMarketDTO opt) {
        final int optIndex = opt.getOptionIndex();
        VBox optCard = new VBox(8);
        optCard.setStyle("-fx-background-color: white; -fx-padding: 12; -fx-border-color: #3498db; -fx-border-radius: 6; -fx-border-width: 1 1 1 4;");

        Label optTitle = new Label("Option " + (optIndex + 1) + ": " + opt.getOptionTitle());
        optTitle.setStyle("-fx-font-weight: bold;");
        optCard.getChildren().add(optTitle);
        optCard.getChildren().add(new Label("Current Shares: " + opt.getShares()));

        if (event.isOrderBook()) {
            optCard.getChildren().add(new Label(String.format(
                    "LAST: $%.2f | BID: $%.2f | ASK: $%.2f | MID: $%.2f | SPREAD: $%.2f",
                    opt.getLast(), opt.getBid(), opt.getAsk(), opt.getMid(), opt.getSpread())));
            optCard.getChildren().add(buildOrderQueueLabel("Buy orders", opt.getBuyOrders()));
            optCard.getChildren().add(buildOrderQueueLabel("Sell orders", opt.getSellOrders()));
        } else {
            Label optPrice = new Label(String.format("Live Price: $%.2f", opt.getLivePrice()));
            optPrice.setStyle("-fx-text-fill: #e67e22; -fx-font-weight: bold;");
            optCard.getChildren().add(optPrice);
        }

        HBox tradeBox = new HBox(10);
        tradeBox.setAlignment(Pos.CENTER_LEFT);
        TextField amountField = new TextField();
        amountField.setPromptText("Shares amount");
        amountField.setPrefWidth(110);
        TextField priceField = new TextField();
        priceField.setPromptText("Price ($)");
        priceField.setPrefWidth(70);
        if (event.isLmsr()) {
            priceField.setDisable(true);
            priceField.setPromptText("Auto");
        }

        Button buyButton = new Button("Buy");
        buyButton.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold;");
        Button sellButton = new Button("Sell");
        sellButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold;");
        boolean canTrade = event.isActive();
        buyButton.setDisable(!canTrade);
        sellButton.setDisable(!canTrade || event.isLmsr());
        amountField.setDisable(!canTrade);
        if (!event.isLmsr()) priceField.setDisable(!canTrade);

        buyButton.setOnAction(e -> handleBuy(event, optIndex, amountField, priceField));
        sellButton.setOnAction(e -> handleSell(event, optIndex, amountField, priceField));

        if (event.isLmsr()) {
            tradeBox.getChildren().addAll(amountField, priceField, buyButton);
        } else {
            tradeBox.getChildren().addAll(amountField, priceField, buyButton, sellButton);
        }
        optCard.getChildren().add(tradeBox);
        return optCard;
    }

    private Label buildOrderQueueLabel(String title, List<OrderDTO> orders) {
        if (orders == null || orders.isEmpty()) {
            Label empty = new Label(title + ": none");
            empty.setStyle("-fx-text-fill: #7f8c8d; -fx-font-style: italic;");
            return empty;
        }
        StringBuilder sb = new StringBuilder(title + ": ");
        for (int i = 0; i < orders.size(); i++) {
            OrderDTO o = orders.get(i);
            if (i > 0) sb.append(" ; ");
            sb.append(String.format("%s | %d shares @ $%.2f", o.getUserName(), o.getAmount(), o.getLimitPrice()));
        }
        Label label = new Label(sb.toString());
        label.setWrapText(true);
        return label;
    }

    private void handleBuy(EventDTO event, int optIndex, TextField amountField, TextField priceField) {
        try {
            int amount = Integer.parseInt(amountField.getText().trim());
            double limitPrice = 0.0;
            if (!priceField.isDisabled() && !priceField.getText().isEmpty()) {
                limitPrice = Double.parseDouble(priceField.getText().trim());
            }
            if (amount <= 0) {
                statusLabel.setText("Error: Amount must be greater than 0.");
                return;
            }
            String activeUser = requireSelectedUser();
            Order buyOrder = new Order(activeUser, OrderType.BUY, limitPrice, amount);
            engine.submitOrder(event.getId(), buyOrder, optIndex);
            UserDTO afterBuy = engine.getUserDTO(activeUser);
            if (afterBuy.isBlocked()) {
                statusLabel.setText(String.format(
                        "Order submitted for %s. Balance is now $%.2f. Account is permanently blocked from future transactions.",
                        activeUser, afterBuy.getBalance()));
            } else {
                statusLabel.setText("Order submitted for " + activeUser);
            }
            populateLists();
            displayEventDetails(engine.getEventDTO(event.getId()));
        } catch (NumberFormatException ex) {
            statusLabel.setText("Error: Please enter a valid number for amount and price.");
        } catch (Exception ex) {
            statusLabel.setText("Buy Error: " + ex.getMessage());
        }
    }

    private void handleSell(EventDTO event, int optIndex, TextField amountField, TextField priceField) {
        try {
            int amount = Integer.parseInt(amountField.getText().trim());
            double limitPrice = 0.0;
            if (!priceField.isDisabled() && !priceField.getText().isEmpty()) {
                limitPrice = Double.parseDouble(priceField.getText().trim());
            }
            if (amount <= 0) {
                statusLabel.setText("Error: Amount must be greater than 0.");
                return;
            }
            String activeUser = requireSelectedUser();
            Order sellOrder = new Order(activeUser, OrderType.SELL, limitPrice, amount);
            engine.submitSellOrder(event.getId(), sellOrder, optIndex);
            statusLabel.setText("Sell order submitted for " + activeUser);
            populateLists();
            displayEventDetails(engine.getEventDTO(event.getId()));
        } catch (NumberFormatException ex) {
            statusLabel.setText("Error: Please enter a valid number for amount and price.");
        } catch (Exception ex) {
            statusLabel.setText("Sell Error: " + ex.getMessage());
        }
    }
}
