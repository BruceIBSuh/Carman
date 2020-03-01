package com.silverback.carman2.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity
public class AutoDataEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "auto_maker")
    public String autoMaker;

    @ColumnInfo(name = "auto_model")
    public String autoModel;

    @ColumnInfo(name = "auto_type")
    public int autoType;

    @Ignore
    @ColumnInfo(name = "drive_type")
    public int driveType;

    @Ignore
    @ColumnInfo(name = "fuel_type")
    public int fuelType;
}

