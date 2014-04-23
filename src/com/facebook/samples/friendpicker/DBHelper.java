package com.facebook.samples.friendpicker;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by markus on 22.04.14.
 */
class DBHelper extends SQLiteOpenHelper {

    public DBHelper(Context context) {
      super(context, "myDB", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

      db.execSQL("create table mytable ("
              + "id integer primary key autoincrement,"
              + "name text,"
              + "email text" + ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
  }


