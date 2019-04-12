package com.silverback.carman2.models;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

import androidx.annotation.NonNull;

public class Opinet  {

    public static class OilPrice implements Serializable {
        private String tradeDate;
        private String productCode;
        private String productName;
        private float price;
        private float diff;

        String getTradeDate() {
            return tradeDate;
        }
        void setTradeDate(String tradeDate) {
            this.tradeDate = tradeDate;
        }

        public String getProductCode() {
            return productCode;
        }
        void setProductCode(String productCode) {
            this.productCode = productCode;
        }

        String getProductName() {
            return productName;
        }
        void setProductName(String productName) {
            this.productName = productName;
        }

        public float getPrice() {
            return price;
        }
        public void setPrice(float price) {
            this.price = price;
        }

        public float getDiff() {
            return diff;
        }
        void setDiff(float diff) {
            this.diff = diff;
        }

        // Overrided method for showing results.
        @Override
        public String toString() {
            return " tradeDate: " + getTradeDate()
                    + "\n productCode=" + getProductCode()
                    + "\n productName: " + getProductName()
                    + "\n price: " + + getPrice() + " 원"
                    + "\n diff: " + getDiff() + " %";
        }

    }

    public static class SidoPrice implements Serializable {

        private String sidoCode;
        private String sidoName;
        private String productCd;
        private float price;
        private float diff;


        String getSidoCode() {
            return sidoCode;
        }
        void setSidoCode(String sidoCode) {
            this.sidoCode = sidoCode;
        }

        public String getSidoName() {
            return sidoName;
        }
        void setSidoName(String sidoName) {
            this.sidoName = sidoName;
        }

        public String getProductCd() {
            return productCd;
        }
        void setProductCd(String productCd) {
            this.productCd = productCd;
        }

        public float getPrice() {
            return price;
        }

        public void setPrice(float price) {
            this.price = price;
        }

        public float getDiff() {
            return diff;
        }
        void setDiff(float diff) {
            this.diff = diff;
        }

        @NonNull
        @Override
        public String toString(){
            return "시도코드: " + getSidoCode()
                    + "\n시도이름: " + getSidoName()
                    + "\n제품코드: " + getProductCd()
                    + "\n제품가격: " + getPrice()
                    + "\n변동률: " + getDiff();
        }
    }

    public static class SigunPrice implements Serializable {

        private String sigunCode;
        private String sigunName;
        private String productCd;
        private float price;
        private float diff;


        String getSigunCode() {
            return sigunCode;
        }
        void setSigunCode(String sigunCode) {
            this.sigunCode = sigunCode;
        }

        public String getSigunName() {
            return sigunName;
        }
        void setSigunName(String name) {
            String[] sigunName = name.split("\\s+", 2); //Excludes Sido name from Sigun name
            this.sigunName = sigunName[1];
        }

        public String getProductCd() {
            return productCd;
        }
        void setProductCd(String productCd) {
            this.productCd = productCd;
        }

        public float getPrice() {
            return price;
        }
        public void setPrice(float price) {
            this.price = price;
        }

        public float getDiff() {
            return diff;
        }
        void setDiff(float dist) {
            this.diff = dist;
        }

        @Override
        public String toString(){
            return "시군코드: " + getSigunCode()
                    + "\n시군이름: " + getSigunName()
                    + "\n제품코드: " + getProductCd()
                    + "\n제품가격: " + getPrice()
                    + "\n변동률: " + getDiff();
        }
    }



    public static class DistrictCode implements Serializable {

        private String districtCode;
        private String districtName;

        public String getDistrictCode() { return districtCode; }
        void setDistrictCode(String districtCode) {
            this.districtCode = districtCode;
        }
        public String getDistrictName() {
            return districtName;
        }
        void setDistrictName(String districtName) {
            this.districtName = districtName;
        }

        @Override
        public String toString(){
            return "지역코드: " + getDistrictCode()
                    + "\n지역이름: " + getDistrictName();
        }
    }

    /**
     * Retrieve data of a specific station with the station id given.
     */
    public static class GasStationInfo {

        private String stationCode;
        private String stationName;
        private String oldAddrs;
        private String newAddrs;
        private String telNo;
        private String sigunCode;
        private String isCarWash;
        private String isService;
        private String isCVS;
        private String oilPrice;
        private String xCoord;
        private String yCoord;

        // Setters and Getters by JavaBean format
        void setStationCode(String stationCode) { this.stationCode = stationCode; }
        public String getStationCode() { return stationCode; }


        public void setStationName(String stationName) { this.stationName = stationName; }
        public String getStationName() { return stationName; }

        void setOldAddrs(String oldAddrs) { this.oldAddrs = oldAddrs; }
        public String getOldAddrs() { return oldAddrs; }

        public String getNewAddrs() { return newAddrs; }
        void setNewAddrs(String newAddrs) { this.newAddrs = newAddrs; }

        void setTelNo(String telNo) { this.telNo = telNo; }
        public String getTelNo() { return telNo; }

        void setSigunCode(String sigunCode) { this.sigunCode = sigunCode; }
        public String getSigunCode() { return sigunCode; }

        void setIsCarWash(String isCarWash) { this.isCarWash = isCarWash; }
        public String getIsCarWash() { return isCarWash; }

        void setIsService(String isMaint) {
            this.isService = isMaint;
        }
        public String getIsService() {
            return isService;
        }

        void setIsCVS(String isCVS){
            this.isCVS = isCVS;
        }
        public String getIsCVS() {
            return isCVS;
        }

        void setOilPrice(String oilPrice) {
            this.oilPrice = oilPrice;
        }
        public String getOilPrice() {
            return oilPrice;
        }

        void setxCoord(String xCoord) { this.xCoord = xCoord; }
        public String getxCoord() { return xCoord; }

        void setyCoord(String yCoord) { this.yCoord = yCoord; }
        public String getyCoord() { return yCoord; }
    }



    public static class GasStnParcelable implements Parcelable, Serializable {

        private String stnId;
        private String stnCode;
        private String stnName;
        private float stnPrice;
        private float distance;
        private float xCoord;
        private float yCoord;


        GasStnParcelable() {
            // default
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel out, int i) {
            out.writeString(stnId);
            out.writeString(stnCode);
            out.writeString(stnName);
            out.writeFloat(stnPrice);
            out.writeFloat(distance);
            out.writeFloat(xCoord);
            out.writeFloat(yCoord);

        }

        static final Parcelable.Creator<GasStnParcelable> CREATOR = new Parcelable.Creator<GasStnParcelable>() {

            @Override
            public GasStnParcelable createFromParcel(Parcel parcel) {
                return new GasStnParcelable(parcel);
            }

            @Override
            public GasStnParcelable[] newArray(int size) {
                return new GasStnParcelable[size];
            }
        };

        private GasStnParcelable(Parcel in) {
            stnId = in.readString();
            stnCode = in.readString();
            stnName = in.readString();
            stnPrice = in.readFloat();
            distance = in.readFloat();
            xCoord = in.readFloat();
            yCoord = in.readFloat();
        }

        public String getStnId() { return stnId; }
        void setStnId(String stnId) {
            this.stnId = stnId;
        }

        public String getStnCode() {
            return stnCode;
        }
        void setStnCode(String stnCode) {
            this.stnCode = stnCode;
        }

        public String getStnName() {
            return stnName;
        }
        void setStnName(String stnName) {
            this.stnName = stnName;
        }

        public float getStnPrice() {
            return stnPrice;
        }
        void setStnPrice(float price) {
            this.stnPrice = price;
        }

        public float getDist() {
            return distance;
        }
        void setDist(float dist) {
            this.distance = dist;
        }

        public float getLongitude() {
            return xCoord;
        }
        void setLongitude(float x) {
            this.xCoord = x;
        }

        public float getLatitude() {
            return yCoord;
        }
        void setLatitude(float y) {
            this.yCoord = y;
        }
    }

    // Makes List<TypedList> parcelable such that it is passed from DownloadStationService to Local
    // Broadcast Receiver defined in OpinetStationListFragment.
    /*
    public static class GasStnListParcelable implements Parcelable {
        private List<GasStnParcelable> gasStnList;
        // Constructor
        public GasStnListParcelable(List<GasStnParcelable> list) {
            gasStnList = list;
        }
        @Override
        public int describeContents() {
            return 0;
        }
        @Override
        public void writeToParcel(Parcel out, int flag) {
            out.writeTypedList(gasStnList);
        }
        public static final Parcelable.Creator<GasStnListParcelable> CREATOR
                = new Parcelable.Creator<GasStnListParcelable>() {
            @Override
            public GasStnListParcelable createFromParcel(Parcel in) {
                return new GasStnListParcelable(in);
            }
            @Override
            public GasStnListParcelable[] newArray(int size) {
                return new GasStnListParcelable[size];
            }
        };
        private GasStnListParcelable(Parcel in) {
            this.gasStnList = new ArrayList<>();
            in.readTypedList(gasStnList, GasStnParcelable.CREATOR);
        }
        public List<GasStnParcelable> getGasStnListParcelable(){
            return gasStnList;
        }
    }
    */
}
