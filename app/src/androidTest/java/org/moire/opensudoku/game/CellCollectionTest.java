package org.moire.opensudoku.game;

import junit.framework.TestCase;

/**
 * Created by Jack on 22/06/2017.
 */
public class CellCollectionTest extends TestCase {
    public void testConsumeMatchingLines1() throws Exception {

        // Set up
        CellCollection cells = CellCollection.createEmpty();

        cells.getCell(1,1).setValue(1);
        cells.getCell(2,1).setValue(1);
        cells.getCell(3,1).setValue(1);

        cells.consumeMatchingLines(cells.getCell(1,1));

        assertEquals(cells.getCell(0,1).getValue(),0);
        assertEquals(cells.getCell(0,2).getValue(),0);
        assertEquals(cells.getCell(0,3).getValue(),0);

        assertEquals(cells.getCell(1,0).getValue(),0);
        assertEquals(cells.getCell(2,0).getValue(),0);
        assertEquals(cells.getCell(3,0).getValue(),0);

        assertEquals(cells.getCell(1,1).getValue(),9);
        assertEquals(cells.getCell(2,1).getValue(),9);
        assertEquals(cells.getCell(3,1).getValue(),9);

    }

    public void testConsumeMatchingLines2() throws Exception {

        // Set up
        CellCollection cells = CellCollection.createEmpty();

        cells.getCell(0,0).setValue(1);

        cells.consumeMatchingLines(cells.getCell(0,0));

        assertEquals(cells.getCell(0,0).getValue(),1);

        assertEquals(cells.getCell(1,0).getValue(),0);
        assertEquals(cells.getCell(0,1).getValue(),0);

    }

    public void testConsumeMatchingLines3() throws Exception {

        // Set up
        CellCollection cells = CellCollection.createEmpty();

        cells.getCell(0,0).setValue(1);
        cells.getCell(1,0).setValue(1);
        cells.getCell(2,0).setValue(1);

        cells.consumeMatchingLines(cells.getCell(2,0));

        assertEquals(cells.getCell(0,0).getValue(),9);
        assertEquals(cells.getCell(1,0).getValue(),9);
        assertEquals(cells.getCell(2,0).getValue(),9);

        assertEquals(cells.getCell(3,0).getValue(),0);

    }
}