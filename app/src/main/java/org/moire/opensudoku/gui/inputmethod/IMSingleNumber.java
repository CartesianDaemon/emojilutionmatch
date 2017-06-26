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

package org.moire.opensudoku.gui.inputmethod;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.PorterDuff;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import org.moire.opensudoku.R;
import org.moire.opensudoku.game.Cell;
import org.moire.opensudoku.game.CellCollection;
import org.moire.opensudoku.game.CellNote;
import org.moire.opensudoku.game.SudokuGame;
import org.moire.opensudoku.game.CellCollection.OnChangeListener;
import org.moire.opensudoku.gui.HintsQueue;
import org.moire.opensudoku.gui.SudokuBoardView;
import org.moire.opensudoku.gui.inputmethod.IMControlPanelStatePersister.StateBundle;

/**
 * This class represents following type of number input workflow: Number buttons are displayed
 * in the sidebar, user selects one number and then fill values by tapping the cells.
 *
 * @author romario
 */
public class IMSingleNumber extends InputMethod {

	private static final int MODE_EDIT_VALUE = 0;
	private static final int MODE_EDIT_NOTE = 1;

	private boolean mHighlightCompletedValues = true;
	private boolean mShowNumberTotals = false;

	private int mSelectedNumber = 1;
	private int mEditMode = MODE_EDIT_VALUE;

	private TextView mTxtScore;
	private TextView mTxtNextFood;

	private Handler mGuiHandler;
	//private Map<Integer, Button> mNumberButtons;

	public IMSingleNumber() {
		super();

		mGuiHandler = new Handler();
	}

	public boolean getHighlightCompletedValues() {
		return mHighlightCompletedValues;
	}

	/**
	 * If set to true, buttons for numbers, which occur in {@link CellCollection}
	 * more than {@link CellCollection#SUDOKU_SIZE}-times, will be highlighted.
	 *
	 * @param highlightCompletedValues
	 */
	public void setHighlightCompletedValues(boolean highlightCompletedValues) {
		mHighlightCompletedValues = highlightCompletedValues;
	}

	public boolean getShowNumberTotals() {
		return mShowNumberTotals;
	}

	public void setShowNumberTotals(boolean showNumberTotals) {
		mShowNumberTotals = showNumberTotals;
	}

	@Override
	protected void initialize(Context context, IMControlPanel controlPanel,
							  SudokuGame game, SudokuBoardView board, HintsQueue hintsQueue) {
		super.initialize(context, controlPanel, game, board, hintsQueue);

		game.getCells().addOnChangeListener(mOnCellsChangeListener);
	}

	@Override
	public int getNameResID() {
		return R.string.single_number;
	}

	@Override
	public int getHelpResID() {
		return R.string.im_single_number_hint;
	}

	@Override
	public String getAbbrName() {
		return mContext.getString(R.string.single_number_abbr);
	}

	@Override
	protected View createControlPanelView() {
		LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View controlPanel = inflater.inflate(R.layout.im_single_number, null);

		mTxtScore = (TextView) controlPanel.findViewById(R.id.score);
		mTxtScore.setText("Score: WIP");

		return controlPanel;
	}

    private View.OnTouchListener mNumberButtonTouched = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            mSelectedNumber = (Integer) view.getTag();
            update();
            return true;
        }
    };

	private OnClickListener mNumberButtonClicked = new OnClickListener() {

		@Override
		public void onClick(View v) {
			mSelectedNumber = (Integer) v.getTag();

			update();
		}
	};

	private OnChangeListener mOnCellsChangeListener = new OnChangeListener() {

		@Override
		public void onChange() {
			if (mActive) {
				update();
			}
		}
	};

	private void update() {
		switch (mEditMode) {
			//case MODE_EDIT_NOTE:
				//mSwitchNumNoteButton.setImageResource(R.drawable.ic_edit_white);
				//break;
			//case MODE_EDIT_VALUE:
				//mSwitchNumNoteButton.setImageResource(R.drawable.ic_edit_grey);
				//break;
		}

		// TODO: sometimes I change background too early and button stays in pressed state
		// this is just ugly workaround
		mGuiHandler.postDelayed(new Runnable() {
			@Override
			public void run() {

				Map<Integer, Integer> valuesUseCount = null;
				if (mHighlightCompletedValues || mShowNumberTotals)
					valuesUseCount = mGame.getCells().getValuesUseCount();

				if (mHighlightCompletedValues) {
					//int completedTextColor = mContext.getResources().getColor(R.color.im_number_button_completed_text);
					for (Map.Entry<Integer, Integer> entry : valuesUseCount.entrySet()) {
						boolean highlightValue = entry.getValue() >= CellCollection.SUDOKU_SIZE;
						if (highlightValue) {
							//Button b = mNumberButtons.get(entry.getKey());
							/*if (b.getTag().equals(mSelectedNumber)) {
								b.setTextColor(completedTextColor);
							} else {
                                b.getBackground().setColorFilter(0xFF008800, PorterDuff.Mode.MULTIPLY);
							}*/
                            // Only set background color
                            //b.getBackground().setColorFilter(0xFF1B5E20, PorterDuff.Mode.MULTIPLY);
							//b.setTextColor(Color.WHITE);
						}
					}
				}

			}
		}, 100);
	}

	@Override
	protected void onActivated() {
		update();
	}

	@Override
	protected void onCellTapped(Cell cell) {
		int selNumber = mSelectedNumber;

		switch (mEditMode) {
			case MODE_EDIT_NOTE:
				if (selNumber == 0) {
					mGame.setCellNote(cell, CellNote.EMPTY);
				} else if (selNumber > 0 && selNumber <= 9) {
					mGame.setCellNote(cell, cell.getNote().toggleNumber(selNumber));
				}
				break;
			case MODE_EDIT_VALUE:
				if (selNumber >= 0 && selNumber <= 9) {
				}
				break;
		}

	}

	@Override
	protected void onSaveState(StateBundle outState) {
		outState.putInt("selectedNumber", mSelectedNumber);
		outState.putInt("editMode", mEditMode);
	}

	@Override
	protected void onRestoreState(StateBundle savedState) {
		mSelectedNumber = savedState.getInt("selectedNumber", 1);
		mEditMode = savedState.getInt("editMode", MODE_EDIT_VALUE);
		if (isInputMethodViewCreated()) {
			update();
		}
	}

}
