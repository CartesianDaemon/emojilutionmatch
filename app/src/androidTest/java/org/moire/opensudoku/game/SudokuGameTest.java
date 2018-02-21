package org.moire.opensudoku.game;

import junit.framework.TestCase;

import android.os.Bundle;

/**
 * Created by Jack on 22/06/2017.
 */
public class SudokuGameTest extends TestCase {
    public void testSerialiseUnserialise() throws Exception {
        Bundle bundle = new Bundle();
        SudokuGame game1 = SudokuGame.createEmptyGame();
        game1.saveState(bundle);
        SudokuGame game2 = SudokuGame.createEmptyGame();
        game2.restoreState(bundle);
    }

}