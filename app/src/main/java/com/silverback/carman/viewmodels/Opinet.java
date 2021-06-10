package com.silverback.carman.viewmodels;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;

public class Opinet  {

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

        @NonNull
        @Override
        public String toString(){
            return "districtCode: " + getDistrictCode() + "\ndistrictName: " + getDistrictName();
        }
    }

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
        @NonNull
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
            return "sidoCode: " + getSidoCode()
                    + "\nsidoName: " + getSidoName()
                    + "\nproductCd: " + getProductCd()
                    + "\nprice: " + getPrice()
                    + "\ndiff: " + getDiff();
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

        @NonNull
        @Override
        public String toString(){
            return "sigunCode: " + getSigunCode()
                    + "\nsigunName: " + getSigunName()
                    + "\nproductCd: " + getProductCd()
                    + "\nprice: " + getPrice()
                    + "\ndiff: " + getDiff();
        }
    }


    public static class StationPrice implements Serializable {
        private String stnId;
        private String stnCompany;
        private String stnName;
        private String productCd;
        private Map<String, Float> stnPrice;
        private Map<String, Float> priceDiff;

        StationPrice() {
            stnPrice = new HashMap<>();
        }

        public String getStnId() {
            return stnId;
        }
        public void setStnId(String stnId) {
            this.stnId = stnId;
        }

        String getStnCompany() {
            return stnCompany;
        }
        void setStnCompany(String stnCompany) {
            this.stnCompany = stnCompany;
        }

        public String getStnName() {
            return stnName;
        }
        public void setStnName(String stnName) {
            this.stnName = stnName;
        }

        String getProductCd() {
            return productCd;
        }
        void setProductCd(String productCd) {
            this.productCd = productCd;
        }

        public Map<String, Float> getStnPrice() {
            return stnPrice;
        }
        void setStnPrice(String prodCd, float price) {
            stnPrice.put(prodCd, price);
        }

        public Map<String, Float> getPriceDiff() {
            return priceDiff;
        }
        public void setPriceDiff(Map<String, Float> diff) {
            priceDiff = diff;
        }

        @NonNull
        @Override
        public String toString(){
            return "stnId: " + getStnId()
                    + "\nstnCompany: " + getStnCompany()
                    + "\nstnName: " + getStnName()
                    + "\nproductCd: " + getProductCd()
                    + "\nprice: " + getStnPrice()
                    + "\ndiff: " + getPriceDiff();
        }
    }





    /**
     * Retrieve data of a specific station with the station id given.
     */
    public static class GasStationInfo implements Serializable {
        private static final long serialVersionUID = 1L;
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

        void setIsCarWash(String isCarWash) {
            this.isCarWash = isCarWash;
        }
        public String getIsCarWash() { return isCarWash; }

        void setIsService(String isService) {
            this.isService = isService;
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
        public String getXcoord() { return xCoord; }

        void setyCoord(String yCoord) { this.yCoord = yCoord; }
        public String getYcoord() { return yCoord; }

        /*
        @NonNull
        @Override
        public String toString() {
            return "stationCode: " + getStationCode()
                    + "\nstationName: " + getStationName()
                    + "\noldAddrs: " + getOldAddrs()
                    + "\nnewAddrs: " + getNewAddrs()
                    + "\ntelNo: " + getTelNo()
                    + "\nisCarWash: " + getIsCarWash()
                    + "\nisService: " + getIsService()
                    + "\nisCVS: " + getIsCarWash()
                    + "\noilPrice: " + getOilPrice()
                    + "\nxCoord: " + getXcoord()
                    + "\nyCorrd: " + getYcoord();
        }
        */
    }



    public static class GasStnParcelable implements Parcelable, Serializable {
        private String stnId;
        private String stnCode;
        private String stnName;
        private int stnPrice;
        private float distance;
        private float xCoord;
        private float yCoord;
        private boolean isWash;

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
            out.writeInt(stnPrice);
            out.writeFloat(distance);
            out.writeFloat(xCoord);
            out.writeFloat(yCoord);
            out.writeByte((byte)(isWash? 1 : 0));
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
            stnPrice = in.readInt();
            distance = in.readFloat();
            xCoord = in.readFloat();
            yCoord = in.readFloat();

            // Handle boolean with byte
            isWash = in.readByte() != 0;
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

        public int getStnPrice() {
            return stnPrice;
        }
        void setStnPrice(int price) {
            this.stnPrice = price;
        }

        public float getStnDistance() {
            return distance;
        }
        void setStnDistance(float dist) { this.distance = dist; }

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

        public boolean getIsWash() {
            return isWash;
        }
        public void setIsWash(boolean isWash) {
            this.isWash = isWash;
        }

        // TEST CODING FOR WHETHER A STATION HAS BEEN VISITED.
        //public boolean getHasVisited() { return hasVisited; }
        //public void setHasVisited(boolean hasVisited) { this.hasVisited = hasVisited; }
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
