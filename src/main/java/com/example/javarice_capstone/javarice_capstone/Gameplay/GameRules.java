package com.example.javarice_capstone.javarice_capstone.Gameplay;

/**
 * Encapsulates the state of all UNO game rules selected in the setup UI.
 */
public class GameRules {
    // Classic UNO
    private final boolean classicAllowJumpIn;
    private final boolean classicStackDrawCards;

    // UNO No Mercy
    private final boolean noMercyChainAllCards;
    private final boolean noMercyJumpInWilds;
    private final boolean noMercyReverseStack;
    private final boolean noMercyDoubleAttackDraws;

    // UNO 7-0
    private final boolean sevenZeroSwapAnyPlayer;
    private final boolean sevenZeroRotateHands;

    public GameRules(
            boolean classicAllowJumpIn,
            boolean classicStackDrawCards,
            boolean noMercyChainAllCards,
            boolean noMercyJumpInWilds,
            boolean noMercyReverseStack,
            boolean noMercyDoubleAttackDraws,
            boolean sevenZeroSwapAnyPlayer,
            boolean sevenZeroRotateHands
    ) {
        this.classicAllowJumpIn = classicAllowJumpIn;
        this.classicStackDrawCards = classicStackDrawCards;
        this.noMercyChainAllCards = noMercyChainAllCards;
        this.noMercyJumpInWilds = noMercyJumpInWilds;
        this.noMercyReverseStack = noMercyReverseStack;
        this.noMercyDoubleAttackDraws = noMercyDoubleAttackDraws;
        this.sevenZeroSwapAnyPlayer = sevenZeroSwapAnyPlayer;
        this.sevenZeroRotateHands = sevenZeroRotateHands;
    }

    // --- Getters ---
    public boolean isClassicAllowJumpIn() {
        return classicAllowJumpIn;
    }

    public boolean isClassicStackDrawCards() {
        return classicStackDrawCards;
    }

    public boolean isNoMercyChainAllCards() {
        return noMercyChainAllCards;
    }

    public boolean isNoMercyJumpInWilds() {
        return noMercyJumpInWilds;
    }

    public boolean isNoMercyReverseStack() {
        return noMercyReverseStack;
    }

    public boolean isNoMercyDoubleAttackDraws() {
        return noMercyDoubleAttackDraws;
    }

    public boolean isSevenZeroSwapAnyPlayer() {
        return sevenZeroSwapAnyPlayer;
    }

    public boolean isSevenZeroRotateHands() {
        return sevenZeroRotateHands;
    }

    @Override
    public String toString() {
        return "GameRules{" +
                "classicAllowJumpIn=" + classicAllowJumpIn +
                ", classicStackDrawCards=" + classicStackDrawCards +
                ", noMercyChainAllCards=" + noMercyChainAllCards +
                ", noMercyJumpInWilds=" + noMercyJumpInWilds +
                ", noMercyReverseStack=" + noMercyReverseStack +
                ", noMercyDoubleAttackDraws=" + noMercyDoubleAttackDraws +
                ", sevenZeroSwapAnyPlayer=" + sevenZeroSwapAnyPlayer +
                ", sevenZeroRotateHands=" + sevenZeroRotateHands +
                '}';
    }
}