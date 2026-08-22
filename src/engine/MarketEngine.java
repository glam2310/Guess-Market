package engine;

import models.Event;
import models.GuessMarketRoot;
import exception.InvalidEventException;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MarketEngine {

    private List<Event> activeEvents;

    public MarketEngine() {
        this.activeEvents = new ArrayList<>();
    }

    public List<Event> getActiveEvents() {
        return activeEvents;
    }

    public void loadEventsFromXML(String filePath) throws InvalidEventException {
        File file = new File(filePath.trim());
        if (!file.exists() || !file.getName().toLowerCase().endsWith(".xml")) {
            throw new InvalidEventException("File error: The file does not exist or is not an XML file.");
        }

        try {
            JAXBContext context = JAXBContext.newInstance(GuessMarketRoot.class, Event.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            GuessMarketRoot root = (GuessMarketRoot) unmarshaller.unmarshal(file);

            List<Event> loadedEvents = root.getEvents();
            if (loadedEvents == null || loadedEvents.isEmpty()) {
                throw new InvalidEventException("Validation error: The XML file contains no events.");
            }

            validateLoadedEvents(loadedEvents);

            this.activeEvents = loadedEvents;

        } catch (InvalidEventException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidEventException("XML structure error: Failed to parse the file. " + e.getMessage());
        }
    }

    /**
     * Helper method to validate all business rules on the loaded events.
     */
    private void validateLoadedEvents(List<Event> loadedEvents) throws exception.InvalidEventException {
        Set<Integer> uniqueIds = new HashSet<>();

        for (Event event : loadedEvents) {
            if (!uniqueIds.add(event.getEventID())) {
                throw new exception.InvalidEventException("Validation error: Duplicate Event ID found (" + event.getEventID() + ").");
            }

            if (event.getCommissionRate() < 0 || event.getCommissionRate() > 90) {
                throw new exception.InvalidEventException("Validation error: Commission rate for event '" + event.getEventTitle() + "' must be between 0 and 90.");
            }

            if (event.getOptions() == null || event.getOptions().size() < 2) {
                throw new exception.InvalidEventException("Validation error: Event '" + event.getEventTitle() + "' must have at least 2 options.");
            }
        }
    }
}