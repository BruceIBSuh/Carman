package com.silverback.carman2.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity
public class AutoDataMakerEntity {

    @PrimaryKey(autoGenerate = true)
    public int _id;

    @ColumnInfo(name = "auto_maker")
    public String autoMaker;
}

