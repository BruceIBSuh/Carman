package com.silverback.carman.utils;


import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.firestore.PropertyName;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CustomPostingObject implements Parcelable {
    @PropertyName("auto_filter")
    private List<String> autofilter;
    @PropertyName("cnt_comment")
    private int cntComment;
    @PropertyName("cnt_compathy")
    private int cntCompathy;
    @PropertyName("cnt_view")
    private int cntView;
    @PropertyName("isAutoclub")
    private boolean isAutoclub;
    @PropertyName("isGeneral")
    private boolean isGeneral;
    @PropertyName("post_content")
    private String postContent;
    @PropertyName("post_images")
    private List<String> postImages;
    @PropertyName("post_title")
    private String postTitle;
    @PropertyName("timestamp")
    private Date timestamp;
    @PropertyName("user_id")
    private String userId;
    @PropertyName("user_name")
    private String userName;
    @PropertyName("user_pic")
    private String userPic;

    public CustomPostingObject() {}
    public CustomPostingObject(List<String> autofilter, int cntComment, int cntCompathy, int cntView,
            boolean isAutoclub, boolean isGeneral, String postContent, List<String> postImages, String postTitle,
            Date timestamp, String userId, String userName, String userPic)
    {
        this.autofilter= autofilter;
        this.cntComment = cntComment;
        this.cntCompathy = cntCompathy;
        this.cntView = cntView;
        this.isAutoclub = isAutoclub;
        this.isGeneral = isGeneral;
        this.postContent = postContent;
        this.postImages = postImages;
        this.postTitle = postTitle;
        this.timestamp = timestamp;
        this.userId= userId;
        this.userName = userName;
        this.userPic = userPic;
    }

    @PropertyName("auto_filter")
    public List<String> getAutofilter() { return autofilter; }
    @PropertyName("cnt_comment")
    public int getCntComment() { return cntComment; }
    @PropertyName("cnt_compathy")
    public int getCntCompahty() { return cntCompathy;}
    @PropertyName("cnt_view")
    public int getCntView() { return cntView; }
    @PropertyName("isAutoclub")
    public boolean isAutoclub() { return isAutoclub; }
    @PropertyName("isGeneral")
    public boolean isGeneral() { return isGeneral; }
    @PropertyName("post_content")
    public String getPostContent() { return postContent; }
    @PropertyName("post_images")
    public List<String> getPostImages() { return postImages; }
    @PropertyName("post_title")
    public String getPostTitle() { return postTitle; }
    @PropertyName("timestamp")
    public Date getTimestamp() { return timestamp; }
    @PropertyName("user_id")
    public String getUserId() { return userId; }
    @PropertyName("user_name")
    public String getUserName() { return userName; }
    @PropertyName("user_pic")
    public String getUserPic() { return userPic; }

    // Parcelize the object
    protected CustomPostingObject(Parcel in) {
        autofilter = in.createStringArrayList();
        cntComment = in.readInt();
        cntCompathy = in.readInt();
        cntView = in.readInt();
        isAutoclub = in.readByte() != 0;
        isGeneral = in.readByte() != 0;
        postContent = in.readString();
        postImages = in.createStringArrayList();
        postTitle = in.readString();
        timestamp = new Date(in.readLong());
        userId = in.readString();
        userName = in.readString();
        userPic = in.readString();
    }

    public static final Creator<CustomPostingObject> CREATOR = new Creator<CustomPostingObject>() {
        @Override
        public CustomPostingObject createFromParcel(Parcel in) {
            return new CustomPostingObject(in);
        }

        @Override
        public CustomPostingObject[] newArray(int size) {
            return new CustomPostingObject[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeStringList(autofilter);
        parcel.writeInt(cntComment);
        parcel.writeInt(cntCompathy);
        parcel.writeInt(cntView);
        parcel.writeByte((byte) (isAutoclub ? 1 : 0));
        parcel.writeByte((byte) (isGeneral ? 1 : 0));
        parcel.writeString(postContent);
        parcel.writeStringList(postImages);
        parcel.writeString(postTitle);
        parcel.writeLong(timestamp.getTime());
        parcel.writeString(userId);
        parcel.writeString(userName);
        parcel.writeString(userPic);
    }
}
