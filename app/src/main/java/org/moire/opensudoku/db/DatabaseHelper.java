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

package org.moire.opensudoku.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import org.moire.opensudoku.R;
import org.moire.opensudoku.game.SudokuGame;

/**
 * This class helps open, create, and upgrade the database file.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

	private static final String TAG = "DatabaseHelper";

	public static final int DATABASE_VERSION = 8;

	private Context mContext;

	DatabaseHelper(Context context) {
		super(context, SudokuDatabase.DATABASE_NAME, null, DATABASE_VERSION);
		this.mContext = context;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE " + SudokuDatabase.SUDOKU_TABLE_NAME + " ("
				+ SudokuColumns._ID + " INTEGER PRIMARY KEY,"
				+ SudokuColumns.FOLDER_ID + " INTEGER,"
				+ SudokuColumns.CREATED + " INTEGER,"
				+ SudokuColumns.STATE + " INTEGER,"
				+ SudokuColumns.TIME + " INTEGER,"
				+ SudokuColumns.LAST_PLAYED + " INTEGER,"
				+ SudokuColumns.DATA + " Text,"
				+ SudokuColumns.PUZZLE_NOTE + " Text"
				+ ");");

		db.execSQL("CREATE TABLE " + SudokuDatabase.FOLDER_TABLE_NAME + " ("
				+ FolderColumns._ID + " INTEGER PRIMARY KEY,"
				+ SudokuColumns.CREATED + " INTEGER,"
				+ FolderColumns.NAME + " TEXT"
				+ ");");

		insertFolder(db, 1, mContext.getString(R.string.difficulty_easy));
		insertSudoku(db, 1, 1, "Easy1", "000000000000000000000000000000000000000000000000000000000000000000000000000000000");

		createIndexes(db);
	}

	private void insertFolder(SQLiteDatabase db, long folderID, String folderName) {
		long now = System.currentTimeMillis();
		db.execSQL("INSERT INTO " + SudokuDatabase.FOLDER_TABLE_NAME + " VALUES (" + folderID + ", " + now + ", '" + folderName + "');");
	}

	// TODO: sudokuName is not used
	private void insertSudoku(SQLiteDatabase db, long folderID, long sudokuID, String sudokuName, String data) {
		String sql = "INSERT INTO " + SudokuDatabase.SUDOKU_TABLE_NAME + " VALUES (" + sudokuID + ", " + folderID + ", 0, " + SudokuGame.GAME_STATE_NOT_STARTED + ", 0, null, '" + data + "', null);";
		db.execSQL(sql);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.i(TAG, "Upgrading database from version " + oldVersion + " to "
				+ newVersion + "");

		createIndexes(db);
	}

	private void createIndexes(SQLiteDatabase db) {
		db.execSQL("create index " + SudokuDatabase.SUDOKU_TABLE_NAME +
				"_idx1 on " +
				SudokuDatabase.SUDOKU_TABLE_NAME + " (" + SudokuColumns.FOLDER_ID + ");");
	}
}
