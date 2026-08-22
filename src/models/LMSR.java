package models;
import java.util.List;
import java.util.ArrayList;

public class LMSR implements TradingMethod{

    private double b; // Liquidity parameter (positive number)

    /**
     * Constructs an LMSR trading method with a specified liquidity parameter b.
     *
     * @param b the liquidity parameter (must be > 0)
     */
    public LMSR(double b) {
        if (b <= 0) {
            throw new IllegalArgumentException("Liquidity parameter b must be a positive number.");
        }
        this.b = b;
    }

    @Override
    public double calculateInitialSubsidy(int numberOfOptions) {
        // C(0,0,...) = b * ln(n)
        return b * Math.log(numberOfOptions);
    }

    public double getB() {
        return b;
    }

    @Override
    public double calculatePrice(List<EventOption> options, int optionIndex) {
        double denominator = 0.0;

        // Sum all e^(q_j / b)
        for (EventOption option : options) {
            denominator += Math.exp(option.getShares() / b);
        }

        // Numerator is e^(q_i / b) for the target option
        double numerator = Math.exp(options.get(optionIndex).getShares() / b);

        return numerator / denominator;
    }

    @Override
    public double calculateCost(List<EventOption> options, int optionIndex, int sharesToBuy) {
        // Cost is C(q_after) - C(q_before)
        double costBefore = calculateCostFunction(options);

        double sumAfter = 0.0;
        for (int i = 0; i < options.size(); i++) {
            double q = options.get(i).getShares();
            if (i == optionIndex) {
                q += sharesToBuy;
            }
            sumAfter += Math.exp(q / b);
        }

        double costAfter = b * Math.log(sumAfter);

        return costAfter - costBefore;
    }

    /**
     * Helper method to calculate the baseline cost function C(q) = b * ln(sum(e^(q_j / b)))
     */
    private double calculateCostFunction(List<EventOption> options) {
        double sum = 0.0;
        for (EventOption option : options) {
            sum += Math.exp(option.getShares() / b);
        }
        return b * Math.log(sum);
    }

    @Override
    public void executeTrade(List<EventOption> options, int optionIndex, int sharesToBuy) {
        // Update the shares of the target option
        options.get(optionIndex).addShares(sharesToBuy);
    }
}


