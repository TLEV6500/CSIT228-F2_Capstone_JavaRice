package com.example.javarice_capstone.javarice_capstone.Strategies;

import com.example.javarice_capstone.javarice_capstone.Abstracts.AbstractCard;
import com.example.javarice_capstone.javarice_capstone.Abstracts.ComputerStrategy;
import com.example.javarice_capstone.javarice_capstone.enums.Colors;
import com.example.javarice_capstone.javarice_capstone.enums.Types;

import java.util.List;
import java.util.Random;

/**
 * Troll strategy: The computer tries to confuse and annoy the human player by acting unpredictably.
 * Sometimes hoards cards of its most common color, sometimes plays a random action,
 * and occasionally plays a weak card or a random valid card. The goal is to look like it's trolling the player!
 */
public class TrollStrat implements ComputerStrategy {
    private final Random random = new Random();

    @Override
    public int selectCardToPlay(List<AbstractCard> hand, AbstractCard topCard, Colors currentColor) {

        // Gather all valid card indices
        int[] validIndices = hand.stream().mapToInt(hand::indexOf).filter(i -> hand.get(i).canPlayOn(topCard) || hand.get(i).getColor() == currentColor).toArray();
        if (validIndices.length == 0) return -1; // No valid move

        int[] colorCounts = new int[Colors.values().length];
        for (AbstractCard card : hand) {
            if (card.getColor() != Colors.WILD) colorCounts[card.getColor().ordinal()]++;
        }

        // Find the most common color in hand
        int maxColorCount = 0;
        Colors mostCommon = Colors.RED;
        for (Colors c : Colors.values()) {
            if (c == Colors.WILD) continue;
            if (colorCounts[c.ordinal()] > maxColorCount) {
                maxColorCount = colorCounts[c.ordinal()];
                mostCommon = c;
            }
        }

        int action = random.nextInt(10);
        switch (action) {
            case 0:
                // 1 in 10: play a random valid card
                return validIndices[random.nextInt(validIndices.length)];
            case 1:
                // 1 in 10: play a random valid card
                return validIndices[random.nextInt(validIndices.length)];
            case 2:
                // 1 in 10: play an action card if possible (SKIP, REVERSE, DRAW_TWO, DRAW_FOUR, WILD)
                for (int idx : validIndices) {
                    AbstractCard card = hand.get(idx);
                    if (card.getType() != Types.NUMBER) return idx;
                }
                return validIndices[random.nextInt(validIndices.length)];
            case 3:
                // 1 in 10: play the weakest card (NUMBER) if possible
                for (int idx : validIndices) {
                    AbstractCard card = hand.get(idx);
                    if (card.getType() == Types.NUMBER) return idx;
                }
                return validIndices[random.nextInt(validIndices.length)];
            case 4:
                // 1 in 10: play the last valid card in hand
                return validIndices[validIndices.length - 1];
            case 5:
                // 1 in 10: HOARD - avoid playing most common color if possible
                for (int idx : validIndices) {
                    AbstractCard card = hand.get(idx);
                    if (card.getColor() != mostCommon) return idx;
                }
                return validIndices[random.nextInt(validIndices.length)];
            case 6:
                // 1 in 10: play the first valid card (default fallback)
                return validIndices[0];
            default:
                // 7-9 (3 in 10): play a random valid card
                return validIndices[random.nextInt(validIndices.length)];
        }
    }
}