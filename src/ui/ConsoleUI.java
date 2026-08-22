package ui;

import engine.MarketParser;
import models.Event;
import models.EventOption;
import models.TradingAccount;
import models.CollectionType;
import models.EventStatus;
import java.util.List;
import java.util.Scanner;

public class ConsoleUI {

    private List<Event> events;
    private final Scanner scanner;

    public ConsoleUI() {
        this.scanner = new Scanner(System.in);
    }

    public void run() {
        boolean running = true;

        while (running) {
            printMenu();
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    loadXmlFile();
                    break;
                case "2":
                    showAllEvents();
                    break;
                case "3":
                    showEventTradingState();
                    break;
                case "4":
                    participateInEvent();
                    break;
                case "5":
                    closeEvent();
                    break;
                case "6":
                    running = false;
                    System.out.println("Exiting application. Goodbye!");
                    break;
                default:
                    System.out.println("Invalid choice, please try again.");
            }
        }
    }

    private void printMenu() {
        System.out.println("\n=== Guess Market System ===");
        System.out.println("1. Load market from XML file");
        System.out.println("2. Show all events");
        System.out.println("3. Event trading state");
        System.out.println("4. Participate in event");
        System.out.println("5. Close event");
        System.out.println("6. Exit");
        System.out.print("Enter your choice: ");
    }

    private void showAllEvents() {
        if (events == null || events.isEmpty()) {
            System.out.println("Error: No market data loaded yet. Please load an XML file first.");
            return;
        }

        System.out.println("\n=== Loaded Market Events ===");
        for (Event event : events) {
            System.out.println("ID: " + event.getEventID());
            System.out.println("  Title: " + event.getEventTitle());
            System.out.println("  Description: " + event.getEventDescription());
            System.out.println("  Commission Rate: " + event.getCommissionRate() + "%");
            System.out.println("  Collection Type: " + event.getCollectionType());
            System.out.println("  Status: " + event.getEventStatus());
            System.out.println("  Options:");
            for (EventOption option : event.getOptions()) {
                System.out.println("    - " + option.getOptionTitle());
            }
            System.out.println("--------------------------------------------------");
        }
    }

    // פקודה 3: מצב המסחר באירוע (עכשיו נקייה ומשתמשת במתודת עזר!)
    private void showEventTradingState() {
        if (events == null || events.isEmpty()) {
            System.out.println("Error: No market data loaded yet. Please load an XML file first.");
            return;
        }

        showAllEvents();

        System.out.print("Enter event ID to view its trading state: ");
        try {
            int eventId = Integer.parseInt(scanner.nextLine().trim());
            Event selectedEvent = events.stream()
                    .filter(e -> e.getEventID() == eventId)
                    .findFirst()
                    .orElse(null);

            if (selectedEvent == null) {
                System.out.println("Event with ID " + eventId + " not found.");
                return;
            }

            // קריאה למתודת העזר במקום לשכפל קוד
            printEventTradingState(selectedEvent);

        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid numeric ID.");
        }
    }

    // פקודה 4: השתתפות באירוע (עכשיו UI טיפש לחלוטין שמקבל נתונים מוכנים!)
    private void participateInEvent() {
        if (events == null || events.isEmpty()) {
            System.out.println("Error: No market data loaded yet. Please load an XML file first.");
            return;
        }

        List<Event> activeEvents = events.stream()
                .filter(e -> e.getEventStatus() == EventStatus.ACTIVE)
                .toList();

        if (activeEvents.isEmpty()) {
            System.out.println("No active events available for participation.");
            return;
        }

        System.out.println("\n--- Active Events ---");
        for (Event event : activeEvents) {
            System.out.println("ID: " + event.getEventID() + " - " + event.getEventTitle());
        }

        System.out.print("Enter event ID to participate in: ");
        try {
            int eventId = Integer.parseInt(scanner.nextLine().trim());
            Event selectedEvent = activeEvents.stream()
                    .filter(e -> e.getEventID() == eventId)
                    .findFirst()
                    .orElse(null);

            if (selectedEvent == null) {
                System.out.println("Invalid active event ID.");
                return;
            }

            printEventTradingState(selectedEvent);

            System.out.print("Enter option index to buy: ");
            int optionIndex = Integer.parseInt(scanner.nextLine().trim())-1;

            System.out.print("Enter number of shares to buy: ");
            int sharesToBuy = Integer.parseInt(scanner.nextLine().trim());

            // --- התיקון: מקבלים קבלה מוכנה מהמנוע! ---
            Event.PurchaseReceipt receipt = selectedEvent.purchaseShares(optionIndex, sharesToBuy);

            System.out.println("\n=== Purchase Successful ===");
            System.out.printf("Shares Cost: %.2f\n", receipt.getStockCost());
            System.out.printf("Commission: %.2f\n", receipt.getCommission());
            System.out.printf("Total Paid: %.2f\n", receipt.getTotalPaid());

            printEventTradingState(selectedEvent);

        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid numeric value.");
        } catch (Exception e) {
            System.out.println("Error during purchase: " + e.getMessage());
        }
    }

    private void loadXmlFile() {
        System.out.print("Enter full path to XML file: ");
        String filePath = scanner.nextLine().trim();
        try {
            List<Event> loadedEvents = MarketParser.loadMarketFromXml(filePath);

            // סבסוד התחלתי - ירד מכאן כי עשית את זה כבר בצורה מושלמת בתוך MarketParser!

            events = loadedEvents;
            System.out.println("Success! Loaded " + events.size() + " events successfully.");
        } catch (Exception e) {
            System.out.println("Error loading file: " + e.getMessage());

            System.out.println("The system remains with the previous valid state (if any).");
        }
    }

    private void printEventTradingState(Event event) {
        System.out.println("\n=== Trading State for Event: " + event.getEventTitle() + " ===");

        System.out.println("Current Options State:");
        List<EventOption> options = event.getOptions();
        for (int i = 0; i < options.size(); i++) {
            EventOption opt = options.get(i);
            double currentPrice = event.getTradingMethod().calculatePrice(options, i);
            // הדפסה מ-1 ולא מ-0 (i + 1)
            System.out.printf("  Option [%d] %s: Current Value = %.2f, Shares Bought = %d\n",
                    (i + 1), opt.getOptionTitle(), currentPrice, opt.getShares());
        }

        TradingAccount account = event.getAccount();
        System.out.println("Account Balance: " + account.getBalance());
        System.out.println("Total Commissions Collected: " + account.getTotalCommissions());

        System.out.println("Trading History (Newest to Oldest):");
        List<TradingAccount.Transaction> history = account.getTransactions();
        if (history.isEmpty()) {
            System.out.println("  (No trades executed yet)");
        } else {
            for (int i = history.size() - 1; i >= 0; i--) {
                TradingAccount.Transaction tx = history.get(i);
                System.out.printf("  -> Option: %s | Shares: %d | Paid: %.2f\n",
                        tx.getOptionName(), tx.getSharesCount(), tx.getTotalPaid());
            }
        }

        if (event.getEventStatus() == EventStatus.CLOSED) {
            System.out.println("*** Event is CLOSED ***");
            if (event.getWinningOptionIndex() != null) {
                System.out.println("Winning Option: " + event.getOptions().get(event.getWinningOptionIndex()).getOptionTitle());
            }
        }
    }

    // פקודה 5: סגירת אירוע
    private void closeEvent() {
        if (events == null || events.isEmpty()) {
            System.out.println("Error: No market data loaded yet. Please load an XML file first.");
            return;
        }

        // סינון אירועים פעילים בלבד
        List<Event> activeEvents = events.stream()
                .filter(e -> e.getEventStatus() == EventStatus.ACTIVE)
                .toList();

        if (activeEvents.isEmpty()) {
            System.out.println("No active events available to close.");
            return;
        }

        System.out.println("\n--- Active Events ---");
        for (Event event : activeEvents) {
            System.out.println("ID: " + event.getEventID() + " - " + event.getEventTitle());
        }

        System.out.print("Enter event ID to close: ");
        try {
            int eventId = Integer.parseInt(scanner.nextLine().trim());
            Event selectedEvent = activeEvents.stream()
                    .filter(e -> e.getEventID() == eventId)
                    .findFirst()
                    .orElse(null);

            if (selectedEvent == null) {
                System.out.println("Invalid active event ID.");
                return;
            }

            // הצגת מצב המסחר הנוכחי של האירוע טרם הסגירה
            printEventTradingState(selectedEvent);

            System.out.print("Enter the index of the winning option: ");
            int winningOptionIndex = Integer.parseInt(scanner.nextLine().trim())-1;

            // --- הפעלת הלוגיקה במנוע! ---
            selectedEvent.closeEvent(winningOptionIndex);

            System.out.println("\n=== Event Closed Successfully ===");

            // הצגת מצב המסחר המעודכן (כולל היתרות החדשות וסימון שהאירוע סגור)
            printEventTradingState(selectedEvent);

        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid numeric value.");
        } catch (Exception e) {
            System.out.println("Error closing event: " + e.getMessage());
        }
    }
}