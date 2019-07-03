package com.silverback.carman2.database;

import androidx.room.ColumnInfo;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

// Use the class name as the db table name by default. Otherwise, use @Entity(tableName = "xxxxx")
// Likewise, field names are used as column names by default and use @ColumnInfo(name = "xxxx")
// to change names.

// parentColumns: column name in the parent entity
// childColumn: column name in this entity.
@Entity(foreignKeys = @ForeignKey(entity = BasicManagerEntity.class, parentColumns = "_id", childColumns = "basic_id"),
        indices = @Index("basic_id"))

public class ServiceManagerEntity {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "service_id")
    public int _id;

    @ColumnInfo(name = "basic_id")
    public int basicId;

    @ColumnInfo(name = "service_name")
    public String serviceCenter;

    @ColumnInfo(name = "service_addrs")
    public String serviceAddrs;



    // Annotate the nested entity of ServiceItemEmbedded, which may be updated adding more items than
    // the current 5 items by setting SharedPreferences.
    @Embedded(prefix = "item1_")
    public ServiceItemEmbedded serviceItem1;

    @Embedded(prefix = "item2_")
    public ServiceItemEmbedded serviceItem2;

    @Embedded(prefix = "item3_")
    public ServiceItemEmbedded serviceItem3;

    @Embedded(prefix = "item4_")
    public ServiceItemEmbedded serviceItem4;

    @Embedded(prefix = "item5_")
    public ServiceItemEmbedded serviceItem5;

}

