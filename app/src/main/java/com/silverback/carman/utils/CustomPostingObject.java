package com.silverback.carman.utils;

import com.google.firebase.firestore.PropertyName;
import java.util.List;

public class CustomPostingObject  {
    @PropertyName("isAutoclub")
    private boolean isAutoclub;
    @PropertyName("isGeneral")
    private boolean isGeneral;
    @PropertyName("auto_filter")
    List<String> autofilter;

    public CustomPostingObject() {}
    public CustomPostingObject(boolean isAutoclub, boolean isGeneral, List<String> autofilter){
        this.isAutoclub = isAutoclub;
        this.isGeneral = isGeneral;
        this.autofilter= autofilter;
    }

    @PropertyName("isAutoclub")
    public boolean isPostAutoclub() {
        return isAutoclub;
    }

    @PropertyName("isGeneral")
    public boolean isGeneral() {
        return isGeneral;
    }

    @PropertyName("auto_filter")
    public List<String> getAutofilter() {
        return autofilter;
    }
}
