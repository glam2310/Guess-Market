package engine;

import jaxb.generated.GuessMarket;
import jaxb.generated.GMEvent;
import jaxb.generated.GMLMSR;
import models.*;
import exception.*;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MarketParser {

    public static List<Event> loadMarketFromXml(String filePath) {
        if (filePath == null || !filePath.trim().toLowerCase().endsWith(".xml")) {
            throw new InvalidMarketFileException("Invalid file path: File must end with .xml extension.");
        }
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            throw new InvalidMarketFileException("File does not exist or is not a valid file: " + filePath);
        }
        try {
            JAXBContext context = JAXBContext.newInstance(GuessMarket.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            GuessMarket jaxbMarket = (GuessMarket) unmarshaller.unmarshal(new File(filePath));

            List<Event> coreEvents = new ArrayList<>();
            Set<Integer> existingIds = new HashSet<>();

            for (GMEvent jaxbEvent : jaxbMarket.getGMEvents().getGMEvent()) {
                int eventId = jaxbEvent.getId();

                // 1. Check for duplicate event ID
                if (existingIds.contains(eventId)) {
                    throw new InvalidEventException("Duplicate event ID: " + eventId);
                }
                existingIds.add(eventId);

                // 2. Check for out-of-range commission value
                int commissionValue = jaxbEvent.getComision().getValue();
                if (commissionValue < 0 || commissionValue > 90) {
                    throw new InvalidEventException("Commission for event " + eventId + " is out of range (0-90): " + commissionValue);
                }

                List<EventOption> options = new ArrayList<>();
                for (String optionTitle : jaxbEvent.getGMOptions().getGMOption()) {
                    options.add(new EventOption(optionTitle));
                }
                if (options.size() != 2) {
                    throw new InvalidEventException("Event " + eventId + " must have exactly 2 options, but found " + options.size());
                }

                GMLMSR jaxbLmsr = jaxbEvent.getGMMethod().getGMLMSR();
                TradingMethod tradingMethod = new LMSR(jaxbLmsr.getB());

                CollectionType collectionType = jaxbEvent.getComision().getType().equals("on-purchase")
                        ? CollectionType.ON_PURCHASE
                        : CollectionType.ON_CLOSE;

                Event event = new Event(
                        eventId,
                        String.join(" ", jaxbEvent.getName()),
                        jaxbEvent.getDescription(),
                        commissionValue,
                        collectionType,
                        options,
                        tradingMethod
                );

                event.initializeSubsidy();
                coreEvents.add(event);
            }

            return coreEvents;

            // Catch JAXB errors and throw a custom file exception
        } catch (JAXBException e) {
            String errorMsg = e.getMessage();
            if (e.getLinkedException() != null) {
                errorMsg = e.getLinkedException().getMessage();
            }
            throw new InvalidMarketFileException("XML Structure Error: " + (errorMsg != null ? errorMsg : "Corrupted XML file."));

            // Fallback catch-all for other reading errors (e.g., file not found)
        } catch (Exception e) {
            // אם זה כבר Exception משלנו (כמו InvalidEventException), נזרוק אותו הלאה כמו שהוא
            if (e instanceof InvalidEventException) {
                throw e;
            }
            throw new InvalidMarketFileException("Unexpected error reading file: " + e.getMessage());
        }
    }
}