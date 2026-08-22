package models;
import java.util.List;
public interface TradingMethod {
    /**
     * Calculates the current price of a specific option (a decimal value between 0 and 1).
     *
     * @param options      the list of all options available in the event
     * @param optionIndex  the zero-based index of the target option in the list
     * @return the current calculated price of the option
     */
    double calculatePrice(List<EventOption> options, int optionIndex);

    /**
     * Calculates the monetary cost (gross) for purchasing a specified quantity of shares
     * for a given option.
     *
     * @param options      the list of all options available in the event
     * @param optionIndex  the zero-based index of the target option
     * @param sharesToBuy  the number of shares the user wishes to purchase
     * @return the total cost of the purchase
     */
    double calculateCost(List<EventOption> options, int optionIndex, int sharesToBuy);

    /**
     * Executes the trade operation, updating the share quantities within the options list.
     *
     * @param options      the list of all options available in the event
     * @param optionIndex  the zero-based index of the target option
     * @param sharesToBuy  the number of shares to buy and update
     */
    void executeTrade(List<EventOption> options, int optionIndex, int sharesToBuy);

    /**
     * Calculates the initial subsidy required to open the market.
     *
     * @param numberOfOptions the total number of options in the event
     * @return the initial monetary subsidy required
     */
    double calculateInitialSubsidy(int numberOfOptions);
}
