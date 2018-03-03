/* 
 * Copyright (C) 2009 Roman Masek
 * 
 * This file is part of OpenSudoku.
 * 
 * OpenSudoku is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * OpenSudoku is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with OpenSudoku.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package org.moire.opensudoku.game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

/**
 * Collection of sudoku cells. This class in fact represents one sudoku board (9x9).
 *
 * @author romario
 */
public class CellCollection {

	public static final int SUDOKU_SIZE = 9;

	private static final boolean enable_testing = false;

	/**
	 * String is expected to be in format "00002343243202...", where each number represents
	 * cell value, no other information can be set using this method.
	 */
	public static int DATA_VERSION_PLAIN = 0;

	/**
	 * See {@link #DATA_PATTERN_VERSION_1} and {@link #serialize()}.
	 */
	public static int DATA_VERSION_1 = 1;

	// TODO: An array of ints is a much better than an array of Integers, but this also generalizes to the fact that two parallel arrays of ints are also a lot more efficient than an array of (int,int) objects
	// Cell's data.
	private Cell[][] mCells;

	private int score = 0;
	public int mNUnlocked = 1; // TODO: Create accessors etc
	public int mCheatMode = 0; // TODO: Create accessors etc

	public static final int next_size = 3;
	public LinkedList<Integer> next_food = new LinkedList<Integer>(); // Or arraylist? // MYTODO: make private again, only used by savestate

	Random rand = new Random();

	// Helper arrays, contains references to the groups of cells, which should contain unique
	// numbers.
	private CellGroup[] mSectors;
	private CellGroup[] mRows;
	private CellGroup[] mColumns;

	private boolean mOnChangeEnabled = true;

	private final List<OnChangeListener> mChangeListeners = new ArrayList<OnChangeListener>();

	private Cell selectedCell; // Cell user is in, only able to place in this one. Unrelated to old notion of "selected" in other classes.

	public Cell getSelectedCell()
	{
		return selectedCell;
	}

	/**
	 * Creates empty sudoku.
	 *
	 * @return
	 */
	public static CellCollection createEmpty() {
		Cell[][] cells = new Cell[SUDOKU_SIZE][SUDOKU_SIZE];

		for (int r = 0; r < SUDOKU_SIZE; r++) {

			for (int c = 0; c < SUDOKU_SIZE; c++) {
				cells[r][c] = new Cell();
			}
		}

		CellCollection ret = new CellCollection(cells);

		ret.fillNext();

		return ret;
	}

	public void fillNext() // TODO: make private again
	{
		int[] candidates = getInitialCandidates();
		while (next_food.size() < next_size)
		{
			int idx = enable_testing ? candidates.length - 1 : rand.nextInt(candidates.length);
			next_food.push(candidates[idx]);
		}
	}

	public int popNext()
	{
		int ret = next_food.pop();
		fillNext();
		return ret;
	}

	/**
	 * Return true, if no value is entered in any of cells.
	 *
	 * @return
	 */
	public boolean isEmpty() {
		for (int r = 0; r < SUDOKU_SIZE; r++) {
			for (int c = 0; c < SUDOKU_SIZE; c++) {
				Cell cell = mCells[r][c];
				if (cell.getValue() != 0)
					return false;
			}
		}
		return true;
	}

	public void consumeMatchingLines(Cell hint_cell)
	{
		clearHighlights();

		mOnChangeEnabled = false;

		for (int more_to_do=1; more_to_do>0;--more_to_do) {
			int d_score = 0;

			int x0 = hint_cell.getRowIndex();
			int y0 = hint_cell.getColumnIndex();
			int xa, xb, ya, yb; // TODO: Can move these into for statements or not?
			for (xa = x0; xa >= 0 && isMatch(getCell(xa, y0).getValue(), hint_cell.getValue()); xa--)
				;
			for (xb = x0; xb < 9 && isMatch(getCell(xb, y0).getValue(), hint_cell.getValue()); xb++)
				;
			for (ya = y0; ya >= 0 && isMatch(getCell(x0, ya).getValue(), hint_cell.getValue()); ya--)
				;
			for (yb = y0; yb < 9 && isMatch(getCell(x0, yb).getValue(), hint_cell.getValue()); yb++)
				;

			int old_val =hint_cell.getValue();
			int evolved_value = evolveTo(old_val);

			// TODO: Move to separate function
			if ( old_val%3==0 && old_val/3==mNUnlocked && old_val < emoji_chars.length - 1)
			{
				++mNUnlocked; // Unlock new set of three
			}

			int full_set_size = enable_testing ? 2 : 3; // Change to 1 for testing

			if (xb - xa - 1 >= full_set_size) {
				for (int x = xa + 1; x < xb; x++) {
					Cell cell = getCell(x, y0);
					d_score += scoreForTile(cell.getValue());
					getCell(x, y0).setValue(0);
				}
			}
			if (yb - ya - 1 >= full_set_size) {
				for (int y = ya + 1; y < yb; y++) {
					d_score += scoreForTile(getCell(x0, y).getValue());
					getCell(x0, y).setValue(0);
				}
			}

			if (xb - xa - 1 >= full_set_size || yb - ya - 1 >= full_set_size)
			{
				hint_cell.setValue(evolved_value);
				more_to_do++;
			}

			score += d_score;
		}

		mOnChangeEnabled = true;
		onChange();
	}

	/**
	 * Generates debug game.
	 *
	 * @return
	 */
	public static CellCollection createDebugGame() {
		CellCollection debugGame = new CellCollection(new Cell[][]{
				{new Cell(), new Cell(), new Cell(), new Cell(4), new Cell(5), new Cell(6), new Cell(7), new Cell(8), new Cell(9),},
				{new Cell(), new Cell(), new Cell(), new Cell(7), new Cell(8), new Cell(9), new Cell(1), new Cell(2), new Cell(3),},
				{new Cell(), new Cell(), new Cell(), new Cell(1), new Cell(2), new Cell(3), new Cell(4), new Cell(5), new Cell(6),},
				{new Cell(2), new Cell(3), new Cell(4), new Cell(), new Cell(), new Cell(), new Cell(8), new Cell(9), new Cell(1),},
				{new Cell(5), new Cell(6), new Cell(7), new Cell(), new Cell(), new Cell(), new Cell(2), new Cell(3), new Cell(4),},
				{new Cell(8), new Cell(9), new Cell(1), new Cell(), new Cell(), new Cell(), new Cell(5), new Cell(6), new Cell(7),},
				{new Cell(3), new Cell(4), new Cell(5), new Cell(6), new Cell(7), new Cell(8), new Cell(9), new Cell(1), new Cell(2),},
				{new Cell(6), new Cell(7), new Cell(8), new Cell(9), new Cell(1), new Cell(2), new Cell(3), new Cell(4), new Cell(5),},
				{new Cell(9), new Cell(1), new Cell(2), new Cell(3), new Cell(4), new Cell(5), new Cell(6), new Cell(7), new Cell(8),},
		});
		debugGame.markFilledCellsAsNotEditable();
		return debugGame;
	}

	public Cell[][] getCells() {
		return mCells;
	}

	/**
	 * Wraps given array in this object.
	 *
	 * @param cells
	 */
	private CellCollection(Cell[][] cells) {

		mCells = cells;
		initCollection();
	}

	/**
	 * Gets cell at given position.
	 *
	 * @param rowIndex
	 * @param colIndex
	 * @return
	 */
	public Cell getCell(int rowIndex, int colIndex) {
		return mCells[rowIndex][colIndex];
	}

	public void clearHighlights() {
		mOnChangeEnabled = false;
		for (int r = 0; r < SUDOKU_SIZE; r++) {
			for (int c = 0; c < SUDOKU_SIZE; c++) {
				mCells[r][c].mHighlight = false;
			}
		}
		mOnChangeEnabled = true;
		onChange();
	}

	public int getScore()
	{
		return score;
	}

	private String[] emoji_chars = {
			"", // empty
			"\uD83D\uDC23", "\uD83D\uDC24", "\uD83D\uDC14", // egg, chick, hen
			"\uD83D\uDC29", "\uD83D\uDC15", "\uD83D\uDC3A", // poodle, dog, wolf face
			"\uD83D\uDC08", "\uD83D\uDC05", "\uD83E\uDD81", // cat, tiger, lion face
			"\uD83C\uDFA0", "\uD83D\uDC0E", "\uD83E\uDD84", // carousel horse, horse, unicorn face
			"\uD83D\uDEA3", "⛵", "\uD83D\uDEA2", // rowboat, sailboat, ship
			"\uD83C\uDF30", "\uD83C\uDF31", "\uD83C\uDF32", // chestnut seedling evergreen tree
			"\uD83D\uDC1C", "\uD83D\uDD77", "\uD83E\uDD82", // ant, spider, scorpion
			"\uD83D\uDC1A", "\uD83E\uDD90", "\uD83D\uDC19", // shell, shrimp, octopus
			"\uD83D\uDC0D", "\uD83D\uDC0A", "\uD83D\uDC09", // lizard, snake, dragon
			"\uD83D\uDE81", "\uD83D\uDEE9️", "\uD83D\uDE80",  // helicopter, airplane, rocket
			"\uD83D\uDC7D", "\uD83D\uDEF8", "\uD83D\uDC7E", // alien, flying saucer, alien monster
			"\uD83C\uDF1F", "\uD83C\uDF20", "✨", // star, shooting star, sparkles
			"\uD83D\uDC80", "\uD83D\uDC7B", "\uD83D\uDE08", // skull, ghost, devil
			"\uD83C\uDFB5", "\uD83C\uDFB6", "\uD83C\uDFBC" // music note, notes, score
	};

	private int[] getInitialCandidates()
	{
		int[] ret = new int[mNUnlocked];
		for(int i=0;i<mNUnlocked;++i) ret[i] = 3*i+1; // TODO: Proper syntax for this
		return ret;
	}

	private Boolean isMatch(int aa, int bb)
	{
		if (aa==0 || bb==0) return false;
		// if (enable_testing) return true;
		return aa==bb;
	}

	private int scoreForTile(int n)
	{
		//if (n==0) return 0;
		//Integer[] scores = {10,30,90,500};
		//return scores[(n-1)%4];
		if (n<=30) {
			return 10 * n;
		}
		else {
			int set = (n-31)/3; // 0 = first set past rocket, 1 = second set
			int elem = n-31-set*3; // 0 = first element of set, 1 = second etc
			return 1000 * (int)Math.pow(10,set) * (1+elem);
			// Scores look like:
			// 1000 -> 2000 -> 3000
			// 10,000 -> 20,000 -> 30,000
			// etc
		}

	}

	private int scoreForNFoods(int n)
	{
		return n*10;
	}

	public String IdxToEmoji(int value)
	{
		return emoji_chars[value];
	}

	private int evolveTo(int value)
	{
		if (value==0) return 0;
		else if (value%3==0) return 0;
		else return value+1;
	}

	public String getNextEmojisString()
	{
		// Currently returns first only, consider if more would be more interesting
		return IdxToEmoji(next_food.getFirst());
	}

	public String getUnlockedEmojisString()
	{
		StringBuilder str = new StringBuilder();
		for (int i = 0; i < mNUnlocked ; i++) {
			str.append(IdxToEmoji(1+i*3));
		}
		return str.toString();
	}

	// TODO: Remove
	public void markAllCellsAsValid() {
		mOnChangeEnabled = false;
		for (int r = 0; r < SUDOKU_SIZE; r++) {
			for (int c = 0; c < SUDOKU_SIZE; c++) {
				mCells[r][c].setValid(true);
			}
		}
		mOnChangeEnabled = true;
		onChange();
	}

	public void setLocation(double lat, double lon)
	{
		int cell_width_m = 10;
		int y = SUDOKU_SIZE - 1 - ((int)(lat*111111/cell_width_m))%9;
		int x = ((int)(lon*60000/cell_width_m))%9;
		selectedCell = mCells[y][x];
		onChange();
	}

	public void toggleCheatMode()
	{
		mCheatMode = mCheatMode>0 ? 0 : 1;
		onChange();
	}

	public void reset()
	{
		// TODO: Call this function from constructor instead of setting score manually
		mNUnlocked = 1;
		score = 0;
	}

	/**
	 * Validates numbers in collection according to the sudoku rules. Cells with invalid
	 * values are marked - you can use getInvalid method of cell to find out whether cell
	 * contains valid value.
	 *
	 * @return True if validation is successful.
	 */
	public boolean validate() {

		boolean valid = true;

		// first set all cells as valid
		markAllCellsAsValid();

		mOnChangeEnabled = false;
		// run validation in groups
		for (CellGroup row : mRows) {
			if (!row.validate()) {
				valid = false;
			}
		}
		for (CellGroup column : mColumns) {
			if (!column.validate()) {
				valid = false;
			}
		}
		for (CellGroup sector : mSectors) {
			if (!sector.validate()) {
				valid = false;
			}
		}

		mOnChangeEnabled = true;
		onChange();

		return valid;
	}

	public boolean isCompleted() {
		for (int r = 0; r < SUDOKU_SIZE; r++) {
			for (int c = 0; c < SUDOKU_SIZE; c++) {
				Cell cell = mCells[r][c];
				if (cell.getValue() == 0 || !cell.isValid()) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Marks all cells as editable.
	 */
	public void markAllCellsAsEditable() {
		for (int r = 0; r < SUDOKU_SIZE; r++) {
			for (int c = 0; c < SUDOKU_SIZE; c++) {
				Cell cell = mCells[r][c];
				cell.setEditable(true);
			}
		}
	}

	/**
	 * Marks all filled cells (cells with value other than 0) as not editable.
	 */
	public void markFilledCellsAsNotEditable() {
		for (int r = 0; r < SUDOKU_SIZE; r++) {
			for (int c = 0; c < SUDOKU_SIZE; c++) {
				Cell cell = mCells[r][c];
				cell.setEditable(cell.getValue() == 0);
			}
		}
	}


	/**
	 * Returns how many times each value is used in <code>CellCollection</code>.
	 * Returns map with entry for each value.
	 *
	 * @return
	 */
	public Map<Integer, Integer> getValuesUseCount() {
		Map<Integer, Integer> valuesUseCount = new HashMap<Integer, Integer>();
		for (int value = 1; value <= CellCollection.SUDOKU_SIZE; value++) {
			valuesUseCount.put(value, 0);
		}

		for (int r = 0; r < CellCollection.SUDOKU_SIZE; r++) {
			for (int c = 0; c < CellCollection.SUDOKU_SIZE; c++) {
				int value = getCell(r, c).getValue();
				if (value != 0) {
					// valuesUseCount.put(value, valuesUseCount.get(value) + 1);
				}
			}
		}

		return valuesUseCount;
	}

	/**
	 * Initializes collection, initialization has two steps:
	 * 1) Groups of cells which must contain unique numbers are created.
	 * 2) Row and column index for each cell is set.
	 */
	private void initCollection() {
		mRows = new CellGroup[SUDOKU_SIZE];
		mColumns = new CellGroup[SUDOKU_SIZE];
		mSectors = new CellGroup[SUDOKU_SIZE];

		for (int i = 0; i < SUDOKU_SIZE; i++) {
			mRows[i] = new CellGroup();
			mColumns[i] = new CellGroup();
			mSectors[i] = new CellGroup();
		}

		for (int r = 0; r < SUDOKU_SIZE; r++) {
			for (int c = 0; c < SUDOKU_SIZE; c++) {
				Cell cell = mCells[r][c];

				cell.initCollection(this, r, c,
						mSectors[((c / 3) * 3) + (r / 3)],
						mRows[c],
						mColumns[r]
				);
			}
		}
	}

	/**
	 * Creates instance from given <code>StringTokenizer</code>.
	 *
	 * @param data
	 * @return
	 */
	public static CellCollection deserialize(StringTokenizer data) {
		Cell[][] cells = new Cell[SUDOKU_SIZE][SUDOKU_SIZE];

		int r = 0, c = 0;
		while (data.hasMoreTokens() && r < 9) {
			cells[r][c] = Cell.deserialize(data);
			c++;

			if (c == 9) {
				r++;
				c = 0;
			}
		}
		CellCollection cellCollection = new CellCollection(cells);
		cellCollection.mNUnlocked = Integer.parseInt(data.nextToken());
		cellCollection.mCheatMode = Integer.parseInt(data.nextToken());
		return cellCollection;
	}

	/**
	 * Creates instance from given string (string which has been
	 * created by {@link #serialize(StringBuilder)} or {@link #serialize()} method).
	 * earlier.
	 *
	 * @param note
	 */
	public static CellCollection deserialize(String data) {
		// TODO: use DATA_PATTERN_VERSION_1 to validate and extract puzzle data
		String[] lines = data.split("\n");
		if (lines.length == 0) {
			throw new IllegalArgumentException("Cannot deserialize Sudoku, data corrupted.");
		}

		if (lines[0].equals("version: 1")) {
			StringTokenizer st = new StringTokenizer(lines[1], "|");
			return deserialize(st);
		} else {
			return fromString(data);
		}
	}

	/**
	 * Creates collection instance from given string. String is expected
	 * to be in format "00002343243202...", where each number represents
	 * cell value, no other information can be set using this method.
	 *
	 * @param data
	 * @return
	 */
	public static CellCollection fromString(String data) {
		// TODO: validate

		Cell[][] cells = new Cell[SUDOKU_SIZE][SUDOKU_SIZE];

		int pos = 0;
		for (int r = 0; r < CellCollection.SUDOKU_SIZE; r++) {
			for (int c = 0; c < CellCollection.SUDOKU_SIZE; c++) {
				int value = 0;
				while (pos < data.length()) {
					pos++;
					if (data.charAt(pos - 1) >= '0'
							&& data.charAt(pos - 1) <= '9') {
						// value=Integer.parseInt(data.substring(pos-1, pos));
						value = data.charAt(pos - 1) - '0';
						break;
					}
				}
				Cell cell = new Cell();
				cell.setValue(value);
				cell.setEditable(value == 0);
				cells[r][c] = cell;
			}
		}

		return new CellCollection(cells);
	}

	public String serialize() {
		StringBuilder sb = new StringBuilder();
		serialize(sb);
		return sb.toString();
	}

	/**
	 * Writes collection to given StringBuilder. You can later recreate the object instance
	 * by calling {@link #deserialize(String)} method.
	 *
	 * @return
	 */
	public void serialize(StringBuilder data) {
		data.append("version: 1\n");

		for (int r = 0; r < SUDOKU_SIZE; r++) {
			for (int c = 0; c < SUDOKU_SIZE; c++) {
				Cell cell = mCells[r][c];
				cell.serialize(data);
			}
		}
		data.append(mNUnlocked).append("|");
		data.append(mCheatMode).append("|");
	}

	private static Pattern DATA_PATTERN_VERSION_PLAIN = Pattern.compile("^\\d{81}$");
	private static Pattern DATA_PATTERN_VERSION_1 = Pattern.compile("^version: 1\\n((?#value)\\d\\|(?#note)((\\d,)+|-)\\|(?#editable)[01]\\|){0,81}$");

	/**
	 * Returns true, if given <code>data</code> conform to format of given data version.
	 *
	 * @param data
	 * @param dataVersion
	 * @return
	 */
	public static boolean isValid(String data, int dataVersion) {
		if (dataVersion == DATA_VERSION_PLAIN) {
			return DATA_PATTERN_VERSION_PLAIN.matcher(data).matches();
		} else if (dataVersion == DATA_VERSION_1) {
			return DATA_PATTERN_VERSION_1.matcher(data).matches();
		} else {
			throw new IllegalArgumentException("Unknown version: " + dataVersion);
		}
	}

	public void addOnChangeListener(OnChangeListener listener) {
		if (listener == null) {
			throw new IllegalArgumentException("The listener is null.");
		}
		synchronized (mChangeListeners) {
			if (mChangeListeners.contains(listener)) {
				throw new IllegalStateException("Listener " + listener + "is already registered.");
			}
			mChangeListeners.add(listener);
		}
	}

	public void removeOnChangeListener(OnChangeListener listener) {
		if (listener == null) {
			throw new IllegalArgumentException("The listener is null.");
		}
		synchronized (mChangeListeners) {
			if (!mChangeListeners.contains(listener)) {
				throw new IllegalStateException("Listener " + listener + " was not registered.");
			}
			mChangeListeners.remove(listener);
		}
	}

	/**
	 * Returns whether change notification is enabled.
	 *
	 * If true, change notifications are distributed to the listeners
	 * registered by {@link #addOnChangeListener(OnChangeListener)}.
	 *
	 * @return
	 */
//	public boolean isOnChangeEnabled() {
//		return mOnChangeEnabled;
//	}
//	
//	/**
//	 * Enables or disables change notifications, that are distributed to the listeners
//	 * registered by {@link #addOnChangeListener(OnChangeListener)}.
//	 * 
//	 * @param onChangeEnabled
//	 */
//	public void setOnChangeEnabled(boolean onChangeEnabled) {
//		mOnChangeEnabled = onChangeEnabled;
//	}

	/**
	 * Notify all registered listeners that something has changed.
	 */
	protected void onChange() {
		if (mOnChangeEnabled) {
			synchronized (mChangeListeners) {
				for (OnChangeListener l : mChangeListeners) {
					l.onChange();
				}
			}
		}
	}

	public interface OnChangeListener {
		/**
		 * Called when anything in the collection changes (cell's value, note, etc.)
		 */
		void onChange();
	}
}
