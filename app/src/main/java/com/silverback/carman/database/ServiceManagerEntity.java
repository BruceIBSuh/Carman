package com.silverback.carman.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

// Use the class name as the db table name by default. Otherwise, use @Entity(tableName = "xxxxx")
// Likewise, field names are used as column names by default and use @ColumnInfo(name = "xxxx")
// to change names.

// parentColumns: column name in the parent entity
// childColumn: column name in this entity.
@Entity(foreignKeys = @ForeignKey(
        entity = ExpenseBaseEntity.class, parentColumns = "_id", childColumns = "basic_id"),
        indices = @Index("basic_id"))

public class ServiceManagerEntity {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "service_id")
    public int serviceId;

    @ColumnInfo(name = "basic_id")
    public int basicId;

    @ColumnInfo(name = "service_center")
    public String serviceCenter;

    @ColumnInfo(name = "service_addrs")
    public String serviceAddrs;

}

