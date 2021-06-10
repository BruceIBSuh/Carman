package com.silverback.carman.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;


@Entity(foreignKeys = @ForeignKey(
        entity = AutoDataMakerEntity.class, parentColumns = "_id", childColumns = "parent_id"),
        indices = @Index("parent_id"))


public class AutoDataModelEntity {

    @PrimaryKey(autoGenerate = true)
    public int model_id;

    @ColumnInfo(name = "parent_id")
    public int parentId;

    @ColumnInfo(name = "model_name")
    public String modelName;

    @ColumnInfo(name = "auto_type")
    public int autoType;

    @Ignore
    @ColumnInfo(name = "drive_type")
    public int driveType;

    @Ignore
    @ColumnInfo(name = "fuel_type")
    public int fuelType;
}
