package engine;

import jaxb.generated.GuessMarket;
import jaxb.generated.GMEvent;
import jaxb.generated.GMLMSR;
import jaxb.generated.GMOrderBook;
import jaxb.generated.GMUser;
import models.*;
import exception.*;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MarketParser {

    public static MarketParseResult loadMarketFromXml(String filePath) {
        if (filePath == null || !filePath.trim().toLowerCase().endsWith(".xml")) {
            throw new InvalidMarketFileException("Invalid file path: File must end with .xml extension.");
        }
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            throw new InvalidMarketFileException("File does not exist or is not a valid file: " + filePath);
        }
        try {
            rejectIfWrongSchema(file);

            JAXBContext context = JAXBContext.newInstance(GuessMarket.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            GuessMarket jaxbMarket = (GuessMarket) unmarshaller.unmarshal(file);
            requireExercise2Structure(jaxbMarket);

            List<Event> coreEvents = new ArrayList<>();
            Set<Integer> existingIds = new HashSet<>();

            // 1. קריאת האירועים
            if (jaxbMarket.getGMEvents() != null && jaxbMarket.getGMEvents().getGMEvent() != null) {
                for (GMEvent jaxbEvent : jaxbMarket.getGMEvents().getGMEvent()) {
                    int eventId = jaxbEvent.getId();

                    if (existingIds.contains(eventId)) {
                        throw new InvalidEventException("Duplicate event ID: " + eventId);
                    }
                    existingIds.add(eventId);

                    if (jaxbEvent.getCommission() == null) {
                        throw new InvalidEventException(
                                "Invalid XML format for event ID " + eventId + ": missing <commission> element. "
                                        + "This application accepts Exercise 2 files only. "
                                        + "Exercise 1 files that use <comision> cannot be loaded.");
                    }
                    int commissionValue = jaxbEvent.getCommission().getValue();
                    if (commissionValue < 0 || commissionValue > 90) {
                        throw new InvalidEventException("Commission for event " + eventId + " is out of range (0-90): " + commissionValue);
                    }

                    String commissionType = jaxbEvent.getCommission().getType() == null
                            ? ""
                            : jaxbEvent.getCommission().getType().trim().toLowerCase();

                    List<EventOption> options = new ArrayList<>();
                    if (jaxbEvent.getGMOptions() == null || jaxbEvent.getGMOptions().getGMOption() == null) {
                        throw new InvalidEventException("Event " + eventId + " is missing <GM-options>.");
                    }
                    for (String optionTitle : jaxbEvent.getGMOptions().getGMOption()) {
                        options.add(new EventOption(optionTitle == null ? "" : optionTitle.trim()));
                    }
                    if (options.size() != 2) {
                        throw new InvalidEventException("Event " + eventId + " must have exactly 2 options, but found " + options.size());
                    }

                    if (jaxbEvent.getGMMethod() == null) {
                        throw new InvalidEventException("Event " + eventId + " is missing <GM-method>.");
                    }

                    TradingMethod tradingMethod;
                    if (jaxbEvent.getGMMethod().getGMLMSR() != null) {
                        GMLMSR jaxbLmsr = jaxbEvent.getGMMethod().getGMLMSR();
                        if (jaxbLmsr.getB() <= 0) {
                            throw new InvalidEventException("LMSR parameter b for event " + eventId + " must be greater than 0.");
                        }
                        tradingMethod = new LMSR(jaxbLmsr.getB());
                    } else if (jaxbEvent.getGMMethod().getGMOrderBook() != null) {
                        GMOrderBook jaxbOb = jaxbEvent.getGMMethod().getGMOrderBook();
                        if (jaxbOb.getD() <= 0) {
                            throw new InvalidEventException("Order Book parameter d for event " + eventId + " must be greater than 0.");
                        }
                        boolean allowMint = Boolean.parseBoolean(jaxbOb.getAllowMint());
                        tradingMethod = new OrderBook(jaxbOb.getInitial(), jaxbOb.getD(), allowMint);
                    } else {
                        throw new InvalidEventException("Event " + eventId + " has no valid trading method.");
                    }

                    CollectionType collectionType = "on-purchase".equals(commissionType)
                            ? CollectionType.ON_PURCHASE
                            : CollectionType.ON_CLOSE;

                    String eventName = jaxbEvent.getName() == null ? "" : jaxbEvent.getName().trim();
                    String description = jaxbEvent.getDescription() == null ? "" : jaxbEvent.getDescription().trim();

                    Event event = new Event(
                            eventId,
                            eventName,
                            description,
                            commissionValue,
                            collectionType,
                            options,
                            tradingMethod
                    );

                    coreEvents.add(event);
                }
            }

            // 2. קריאת משתמשים
            List<User> loadedUsers = new ArrayList<>();
            Set<String> existingUsernames = new HashSet<>();
            Map<Integer, Boolean> eventAssignedToMM = new HashMap<>();

            for (Event ev : coreEvents) {
                eventAssignedToMM.put(ev.getEventID(), false);
            }

            if (jaxbMarket.getGMUsers() != null && jaxbMarket.getGMUsers().getGMUser() != null) {
                for (GMUser jaxbUser : jaxbMarket.getGMUsers().getGMUser()) {
                    String userName = jaxbUser.getName() == null ? "" : jaxbUser.getName().trim();
                    if (userName.isEmpty()) {
                        throw new InvalidEventException("Validation error: Username cannot be empty.");
                    }

                    String nameKey = userName.toLowerCase();
                    if (!existingUsernames.add(nameKey)) {
                        throw new InvalidEventException("Validation error: Duplicate username found (" + userName + ").");
                    }

                    int initialCash = jaxbUser.getInitialCash();
                    if (initialCash <= 0) {
                        throw new InvalidEventException("Validation error: Initial cash for user '" + userName + "' must be greater than 0.");
                    }

                    User user = new User(userName, initialCash);

                    if (jaxbUser.getGMMarketMaker() != null && jaxbUser.getGMMarketMaker().getEvent() != null) {
                        for (var eventRef : jaxbUser.getGMMarketMaker().getEvent()) {
                            int eventId = eventRef.getId();

                            boolean eventExists = coreEvents.stream().anyMatch(e -> e.getEventID() == eventId);
                            if (!eventExists) {
                                throw new InvalidEventException("Validation error: User '" + userName + "' is assigned as MM to a non-existent event ID (" + eventId + ").");
                            }

                            if (eventAssignedToMM.get(eventId)) {
                                throw new InvalidEventException("Validation error: Event ID " + eventId + " has more than one Market Maker.");
                            }

                            user.addMarketMakerEventId(eventId);
                            eventAssignedToMM.put(eventId, true);
                        }
                    }

                    loadedUsers.add(user);
                }
            }

            for (Map.Entry<Integer, Boolean> entry : eventAssignedToMM.entrySet()) {
                if (!entry.getValue()) {
                    throw new InvalidEventException("Validation error: Event ID " + entry.getKey() + " does not have an assigned Market Maker.");
                }
            }

            return new MarketParseResult(coreEvents, loadedUsers);

        } catch (JAXBException e) {
            String errorMsg = e.getMessage();
            if (e.getLinkedException() != null) {
                errorMsg = e.getLinkedException().getMessage();
            }
            throw new InvalidMarketFileException("XML Structure Error: this file does not match the Exercise 2 schema. "
                    + (errorMsg != null ? errorMsg : "The XML is corrupted or uses an unsupported format."));
        } catch (Exception e) {
            if (e instanceof InvalidEventException || e instanceof InvalidMarketFileException) {
                throw (RuntimeException) e;
            }
            throw new InvalidMarketFileException(
                    "Invalid XML format: this file does not match the Exercise 2 schema. "
                            + "Required Exercise 2 elements include <GM-users> and <commission> "
                            + "(Exercise 1 files that use <comision> cannot be loaded).");
        }
    }

    private static void rejectIfWrongSchema(File file) {
        try {
            String raw = Files.readString(file.toPath(), StandardCharsets.UTF_8).toLowerCase();
            if (raw.contains("gm-ex1") || raw.contains("<comision")) {
                throw new InvalidMarketFileException(
                        "Invalid XML format: this file uses the Exercise 1 schema. "
                                + "Please load an Exercise 2 XML file (must include <GM-users> and <commission>, not <comision>).");
            }
        } catch (InvalidMarketFileException e) {
            throw e;
        } catch (Exception ignored) {
            // If the file cannot be previewed, JAXB + structure checks still run.
        }
    }

    private static void requireExercise2Structure(GuessMarket jaxbMarket) {
        if (jaxbMarket == null || jaxbMarket.getGMEvents() == null
                || jaxbMarket.getGMEvents().getGMEvent() == null
                || jaxbMarket.getGMEvents().getGMEvent().isEmpty()) {
            throw new InvalidMarketFileException("Invalid XML format: missing <GM-events>. This application accepts Exercise 2 files only.");
        }
        if (jaxbMarket.getGMUsers() == null || jaxbMarket.getGMUsers().getGMUser() == null
                || jaxbMarket.getGMUsers().getGMUser().isEmpty()) {
            throw new InvalidMarketFileException(
                    "Invalid XML format: missing <GM-users>. This application accepts Exercise 2 files only "
                            + "(Exercise 1 schema is not supported).");
        }
    }

    public static class MarketParseResult {
        private final List<Event> events;
        private final List<User> users;

        public MarketParseResult(List<Event> events, List<User> users) {
            this.events = events;
            this.users = users;
        }

        public List<Event> getEvents() { return events; }
        public List<User> getUsers() { return users; }
    }
}