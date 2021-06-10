package com.silverback.carman.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class AutoDataMakerEntity {

    @PrimaryKey(autoGenerate = true)
    public int _id;

    @ColumnInfo(name = "auto_maker")
    public String autoMaker;

    @ColumnInfo(name = "auto_type")
    public String autoType;

}

