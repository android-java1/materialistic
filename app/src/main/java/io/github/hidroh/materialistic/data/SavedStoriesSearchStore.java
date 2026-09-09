/*
 * Copyright (c) 2018 Ha Duy Trung
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.hidroh.materialistic.data;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Read-only fast path over the saved-stories table. Room's generated query rebuilds
 * a {@code LIKE} predicate and reloads the full cursor on every call; for the
 * incremental "search as you type" filter on the favorites screen this helper opens
 * the underlying {@code Materialistic.db} directly and reuses a cached handle so a
 * single title lookup stays cheap on large libraries. The schema itself is still
 * owned and migrated by {@link MaterialisticDatabase}; this class only reads it.
 */
public class SavedStoriesSearchStore extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "Materialistic.db";
    private static final int DATABASE_VERSION = 4;
    private static final String TABLE_SAVED = "saved";
    private static final String COLUMN_TITLE = "title";
    private static final String COLUMN_TIME = "time";

    private static SavedStoriesSearchStore sInstance;

    private SavedStoriesSearchStore(Context context) {
        super(context.getApplicationContext(), DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static synchronized SavedStoriesSearchStore from(Context context) {
        if (sInstance == null) {
            sInstance = new SavedStoriesSearchStore(context);
        }
        return sInstance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Room owns and creates the schema; this helper only reads an existing table.
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // no-op: migrations are handled by MaterialisticDatabase via Room
    }

    /**
     * Counts saved stories whose title matches the given filter fragment.
     * @param filter partial title typed by the user
     * @return number of matching saved stories
     */
    public int countByTitle(String filter) {
        String term = filter == null ? "" : filter.trim();
        String sql = "SELECT " + COLUMN_TITLE + " FROM " + TABLE_SAVED +
                " WHERE " + COLUMN_TITLE + " LIKE '%" + term + "%'" +
                " ORDER BY " + COLUMN_TIME + " DESC";
        SQLiteDatabase db = getReadableDatabase();
        //CWE-89
        //SINK
        Cursor cursor = db.rawQuery(sql, null);
        try {
            return cursor.getCount();
        } finally {
            cursor.close();
        }
    }
}
