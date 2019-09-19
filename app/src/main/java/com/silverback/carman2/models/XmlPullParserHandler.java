package com.silverback.carman2.models;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class XmlPullParserHandler {

    // Logging
    private static LoggingHelper log = LoggingHelperFactory.create(XmlPullParserHandler.class);

    private List<Opinet.OilPrice> oilPriceList;
    private Opinet.OilPrice oilPrice;

    private List<Opinet.SidoPrice> sidoPriceList;
    private Opinet.SidoPrice sidoPrice;

    private List<Opinet.SigunPrice> sigunPriceList;
    private Opinet.SigunPrice sigunPrice;

    private List<Opinet.DistrictCode> distCodeList;
    private Opinet.DistrictCode districtCode;

    // Declare ArrayList type to transfer this instance via Bundle
    private List<Opinet.GasStnParcelable> gasStnParcelableList;
    private Opinet.GasStnParcelable gasStnParcelable;

    private List<Opinet.GasStationInfo> gasStationInfoList;
    private Opinet.GasStationInfo gasStationInfo;

    private String text;

    //Refactor: make switch with List<?> as param in order to instantiate each object as necessary.
    //check if this is thread-safe or not.
    public XmlPullParserHandler(){

        oilPriceList = new ArrayList<>();
        sidoPriceList = new ArrayList<>();
        sigunPriceList = new ArrayList<>();
        distCodeList = new ArrayList<>();
        gasStnParcelableList = new ArrayList<>();
        gasStationInfoList = new ArrayList<>();
    }

    public List<Opinet.DistrictCode> parseDistrictCode(InputStream is) {

        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(is, "utf-8");
            int eventType = parser.getEventType();

            while(eventType != XmlPullParser.END_DOCUMENT) {

                String tagName = parser.getName();
                switch(eventType) {

                    case XmlPullParser.START_TAG:
                        if (tagName.equalsIgnoreCase("OIL")) {
                            districtCode = new Opinet.DistrictCode();
                        }
                        break;

                    case XmlPullParser.TEXT:
                        text = parser.getText();
                        break;

                    case XmlPullParser.END_TAG:
                        if (tagName.equalsIgnoreCase("OIL")) {
                            distCodeList.add(districtCode);
                        } else if (tagName.equalsIgnoreCase("AREA_CD")) {
                            districtCode.setDistrictCode(text);
                        } else if (tagName.equalsIgnoreCase("AREA_NM")) {
                            districtCode.setDistrictName(text);
                        }
                        break;

                    default:
                        break;
                }

                eventType = parser.next();
            }

        } catch (XmlPullParserException e) {
            log.w("XmlPullParserException: %s", e);
        } catch (IOException e) {
            log.w("IOException: %s", e);
        }

        return distCodeList;
    }

    // Get nation-wide average fuel price
    public List<Opinet.OilPrice> parseOilPrice(InputStream is) {

        try {

            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(is, "utf-8");
            int eventType = parser.getEventType();

            while(eventType != XmlPullParser.END_DOCUMENT) {

                String tagName = parser.getName();
                switch(eventType) {

                    case XmlPullParser.START_TAG:
                        if (tagName.equalsIgnoreCase("OIL")) {
                            oilPrice = new Opinet.OilPrice();
                        }
                        break;

                    case XmlPullParser.TEXT:
                        text = parser.getText();
                        break;

                    case XmlPullParser.END_TAG:
                        if (tagName.equalsIgnoreCase("OIL")) {
                            oilPriceList.add(oilPrice);
                        } else if (tagName.equalsIgnoreCase("TRADE_DT")) {
                            oilPrice.setTradeDate(text);
                        } else if (tagName.equalsIgnoreCase("PRODCD")) {
                            oilPrice.setProductCode(text);
                        } else if (tagName.equalsIgnoreCase("PRODNM")) {
                            oilPrice.setProductName(text);
                        } else if (tagName.equalsIgnoreCase("PRICE")) {
                            oilPrice.setPrice(Float.parseFloat(text));
                        } else if (tagName.equalsIgnoreCase("DIFF")) {
                            oilPrice.setDiff(Float.parseFloat(text));
                        }
                        break;

                    default:
                        break;
                }

                eventType = parser.next();
            }

        } catch (XmlPullParserException e) {
            //Log.w(TAG, e.getMessage());
        } catch (IOException e) {
            //Log.w(TAG, e.getMessage());
        }

        return oilPriceList;
    }

    public List<Opinet.SidoPrice> parseSidoPrice(InputStream is) {

        try {

            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = factory.newPullParser();

            parser.setInput(is, "utf-8");
            int eventType = parser.getEventType();

            while(eventType != XmlPullParser.END_DOCUMENT) {

                String tagName = parser.getName();

                switch(eventType) {
                    case XmlPullParser.START_TAG:
                        if (tagName.equalsIgnoreCase("OIL")) {
                            sidoPrice = new Opinet.SidoPrice();
                        }
                        break;

                    case XmlPullParser.TEXT:
                        text = parser.getText();
                        break;

                    case XmlPullParser.END_TAG:
                        if (tagName.equalsIgnoreCase("OIL")) {
                            sidoPriceList.add(sidoPrice);
                        } else if (tagName.equalsIgnoreCase("SIDOCD")) {
                            sidoPrice.setSidoCode(text);
                        } else if (tagName.equalsIgnoreCase("SIDONM")) {
                            sidoPrice.setSidoName(text);
                        } else if (tagName.equalsIgnoreCase("PRODCD")) {
                            sidoPrice.setProductCd(text);
                        } else if (tagName.equalsIgnoreCase("PRICE")) {
                            sidoPrice.setPrice(Float.parseFloat(text));
                        } else if (tagName.equalsIgnoreCase("DIFF")) {
                            sidoPrice.setDiff(Float.parseFloat(text));
                        }

                        break;

                    default:
                        break;
                }

                eventType = parser.next();
            }

        } catch (XmlPullParserException e) {
            log.e("XMLPullParserException: %s", e.getMessage());
        } catch (IOException e) {
            log.e("IOException: %s", e.getMessage());
        }

        // Rearrange the original order(regular-premium-kerotine-diesel) to a new order(preminum-
        // regular-diesel-kerotine)
        /*
        if(sidoPriceList.size() >= 4) {
            //Collections.swap(sigunPriceList, 0, 1);
            //Collections.swap(sigunPriceList, 2, 3);
        }
        */

        return sidoPriceList;
    }


    // Get Sigun Price
    public List<Opinet.SigunPrice> parseSigunPrice(InputStream is) {

        try {

            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = factory.newPullParser();

            parser.setInput(is, "utf-8");
            int eventType = parser.getEventType();

            while(eventType != XmlPullParser.END_DOCUMENT) {

                String tagName = parser.getName();
                switch(eventType) {

                    case XmlPullParser.START_TAG:
                        if (tagName.equalsIgnoreCase("OIL")) {
                            sigunPrice = new Opinet.SigunPrice();
                        }
                        break;

                    case XmlPullParser.TEXT:
                        text = parser.getText();
                        break;

                    case XmlPullParser.END_TAG:
                        if (tagName.equalsIgnoreCase("OIL")) {
                            sigunPriceList.add(sigunPrice);
                        } else if (tagName.equalsIgnoreCase("SIGUNCD")) {
                            sigunPrice.setSigunCode(text);
                        } else if (tagName.equalsIgnoreCase("SIGUNNM")) {
                            sigunPrice.setSigunName(text);
                        } else if (tagName.equalsIgnoreCase("PRODCD")) {
                            sigunPrice.setProductCd(text);
                        } else if (tagName.equalsIgnoreCase("PRICE")) {
                            sigunPrice.setPrice(Float.parseFloat(text));
                        } else if (tagName.equalsIgnoreCase("DIFF")) {
                            sigunPrice.setDiff(Float.parseFloat(text));
                        }

                        break;

                    default:
                        break;
                }

                eventType = parser.next();
            }

        } catch (XmlPullParserException e) {
            log.e("XMLPullParserException: %s", e.getMessage());
        } catch (IOException e) {
            log.e("IOException: %s", e.getMessage());
        }

        // Rearrange the original order(regular-premium-kerotine-diesel) to a new order(preminum-
        // regular-diesel-kerotine)
        /*
        if(sigunPriceList.size() >= 4) {
            //Collections.swap(sigunPriceList, 0, 1);
            //Collections.swap(sigunPriceList, 2, 3);
        }
        */

        return sigunPriceList;
    }


    // GasStation made parcelable
    public List<Opinet.GasStnParcelable> parseStationListParcelable(InputStream is) {

        try {
            log.i("parseStationListParcelable");
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(is, "utf-8");
            int eventType = parser.getEventType();

            while(eventType != XmlPullParser.END_DOCUMENT) {

                String tagName = parser.getName();
                switch(eventType) {
                    case XmlPullParser.START_TAG:
                        if (tagName.equalsIgnoreCase("OIL"))
                            gasStnParcelable = new Opinet.GasStnParcelable();
                        break;

                    case XmlPullParser.TEXT:
                        text = parser.getText();
                        break;

                    case XmlPullParser.END_TAG:
                        if (tagName.equalsIgnoreCase("OIL")) {
                            gasStnParcelableList.add(gasStnParcelable);
                        } else if (tagName.equalsIgnoreCase("UNI_ID")) {
                            gasStnParcelable.setStnId(text);
                        } else if (tagName.equalsIgnoreCase("POLL_DIV_CO")) {
                            gasStnParcelable.setStnCode(text);
                        } else if (tagName.equalsIgnoreCase("OS_NM")) {
                            gasStnParcelable.setStnName(text);
                        } else if (tagName.equalsIgnoreCase("PRICE")) {
                            gasStnParcelable.setStnPrice(Integer.parseInt(text));
                        } else if (tagName.equalsIgnoreCase("DISTANCE")) {
                            gasStnParcelable.setStnDistance(Float.parseFloat(text));
                        } else if(tagName.equalsIgnoreCase("GIS_X_COOR")) {
                            gasStnParcelable.setLongitude(Float.parseFloat(text));
                        } else if(tagName.equalsIgnoreCase("GIS_Y_COOR")) {
                            gasStnParcelable.setLatitude(Float.parseFloat(text));
                        }

                        break;

                    default:
                        break;
                }

                eventType = parser.next();
            }

        } catch (XmlPullParserException e) {
            log.e("XMLPullParserException: %s", e.getMessage());
        } catch (IOException e) {
            log.e("IOException: %s", e.getMessage());
        }

        return gasStnParcelableList;
    }

    // Information on a gas station fetched by its Station ID
    public Opinet.GasStationInfo parseGasStationInfo(InputStream is) {

        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(is, "utf-8");
            int eventType = parser.getEventType();

            while(eventType != XmlPullParser.END_DOCUMENT) {
                String tagName = parser.getName();
                switch(eventType){
                    case XmlPullParser.START_TAG:
                        if (tagName.equalsIgnoreCase("OIL")) {
                            gasStationInfo = new Opinet.GasStationInfo();
                        }
                        break;

                    case XmlPullParser.TEXT:
                        text = parser.getText();
                        break;

                    case XmlPullParser.END_TAG:
                        if(tagName.equalsIgnoreCase("POLL_DIV_CO")){
                            gasStationInfo.setStationCode(text);
                        }else if(tagName.equalsIgnoreCase("OS_NAME")){
                            gasStationInfo.setStationName(text);
                        }else if(tagName.equalsIgnoreCase("VAN_ADR")){
                            gasStationInfo.setOldAddrs(text);
                        }else if(tagName.equalsIgnoreCase("NEW_ADR")){
                            gasStationInfo.setNewAddrs(text);
                        }else if(tagName.equalsIgnoreCase("TEL")){
                            gasStationInfo.setTelNo(text);
                        }else if(tagName.equalsIgnoreCase("CAR_WASH_YN")){
                            gasStationInfo.setIsCarWash(text);
                        }else if(tagName.equalsIgnoreCase("MAINT_YN")){
                            gasStationInfo.setIsService(text);
                        }else if(tagName.equalsIgnoreCase("CVS_YN")) {
                            gasStationInfo.setIsCVS(text);
                        }else if(tagName.equalsIgnoreCase("GIS_X_COOR")){
                            gasStationInfo.setxCoord(text);
                        }else if(tagName.equalsIgnoreCase("GIS_Y_COOR")){
                            gasStationInfo.setyCoord(text);
                        }else if(tagName.equalsIgnoreCase("OIL_PRICE")) {
                            // Read nested tags in <OIL_PRICE>
                            // Looks like that Informaion of all fuels is not provided.
                            parser.next();
                            int nestedEventType = parser.getEventType();

                            while (nestedEventType != XmlPullParser.END_DOCUMENT) {
                                String nestedTag = parser.getName();
                                switch (nestedEventType) {
                                    case XmlPullParser.START_TAG:
                                        break;
                                    case XmlPullParser.TEXT:
                                        text = parser.getText();
                                        break;
                                    case XmlPullParser.END_TAG:
                                        if(nestedTag.equalsIgnoreCase("PRICE")) {
                                            log.i("PRICE: %s", text);
                                        } else if (nestedTag.equalsIgnoreCase("PRODCD")){
                                            log.i("PRODCD: %s", text);
                                        }
                                        break;
                                }

                                nestedEventType = parser.next();
                            }

                        }

                        break;

                    default:
                        break;

                }

                eventType = parser.next();
            }


        } catch (XmlPullParserException e) {
            //Log.w(TAG, "XmlPullParserExceptin: " + e.getMessage());
        } catch (IOException e) {
            //Log.w(TAG, "IOException: " + e.getMessage());
        } finally {
            try {
                is.close();
            } catch(IOException e) {
                log.e("IOException");
            }
        }

        return gasStationInfo;

    }

}
