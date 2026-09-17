package engine;

import dto.EventDTO;
import dto.UserDTO;
import exception.InvalidEventException;
import models.Event;
import models.User;
import models.Order;

import java.util.List;

public interface IMarketEngine {

    void loadEventsFromXML(String filePath) throws InvalidEventException;

    List<Event> getActiveEvents();
    List<User> getUsers();

    List<EventDTO> getActiveEventsDTO();
    List<EventDTO> getEventsDTO();
    EventDTO getEventDTO(int eventId) throws Exception;
    List<UserDTO> getUsersDTO();
    UserDTO getUserDTO(String name) throws Exception;
    double calculateOptionPrice(int eventId, int optionIndex) throws Exception;

    void submitOrder(int eventId, Order newOrder, int optionIndex) throws Exception;
    void submitSellOrder(int eventId, Order sellOrder, int optionIndex) throws Exception;
    void openEvent(int eventId, String actingUserName) throws Exception;
    void closeEvent(int eventId, String actingUserName, int winningOptionIndex) throws Exception;
}
