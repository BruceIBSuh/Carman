package com.silverback.carman.viewmodels;

import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;

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

    // Objects
    private Opinet.DistrictCode districtCode;
    private List<Opinet.DistrictCode> distCodeList;
    private Opinet.OilPrice oilPrice;
    private Opinet.SidoPrice sidoPrice;
    private Opinet.SigunPrice sigunPrice;
    private Opinet.StationPrice stnPrice;
    private Opinet.GasStnParcelable gasStnParcelable;
    private Opinet.GasStationInfo gasStationInfo;
    private String text;


    public XmlPullParserHandler(){
        // Default constructor left empty
    }

    // Constructor for downloading the sigun codes and names which is made with creating
    // the inputstream to the url on the sido basis and receive the sigun list of the sido.
    public XmlPullParserHandler(List<Opinet.DistrictCode> distCodeList) {
        this.distCodeList = distCodeList;
    }

    // Receive the sigun list on each sido basis and add it to the list, which MUST BE created in
    // the constructor; otherwise, it would be overwritten and only the last element would remain.
    public List<Opinet.DistrictCode> parseDistrictCode(InputStream is) {
        //List<Opinet.DistrictCode> distCodeList = new ArrayList<>();
        try(InputStream inputStream = is) {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(inputStream, "utf-8");
            int eventType = parser.getEventType();

            while(eventType != XmlPullParser.END_DOCUMENT) {
                String tagName = parser.getName();
                switch(eventType) {
                    case XmlPullParser.START_TAG:
                        if (tagName.equalsIgnoreCase("OIL"))
                            districtCode = new Opinet.DistrictCode();
                        break;
                    case XmlPullParser.TEXT:
                        text = parser.getText();
                        break;
                    case XmlPullParser.END_TAG:
                        if (tagName.equalsIgnoreCase("OIL")) distCodeList.add(districtCode);
                        else if (tagName.equalsIgnoreCase("AREA_CD")) districtCode.setDistrictCode(text);
                        else if (tagName.equalsIgnoreCase("AREA_NM")) districtCode.setDistrictName(text);
                        break;
                    default: break;
                }
                eventType = parser.next();
            }

        } catch (XmlPullParserException | IOException e) { e.printStackTrace();}

        return distCodeList;
    }

    // Get the nation-wide average price
    public List<Opinet.OilPrice> parseOilPrice(InputStream is) {
        List<Opinet.OilPrice> oilPriceList = new ArrayList<>();
        try(InputStream inputStream = is) {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(inputStream, "utf-8");
            int eventType = parser.getEventType();

            while(eventType != XmlPullParser.END_DOCUMENT) {
                String tagName = parser.getName();
                switch(eventType) {
                    case XmlPullParser.START_TAG:
                        if (tagName.equalsIgnoreCase("OIL")) oilPrice = new Opinet.OilPrice();
                        break;
                    case XmlPullParser.TEXT:
                        text = parser.getText();
                        break;
                    case XmlPullParser.END_TAG:
                        if (tagName.equalsIgnoreCase("OIL")) oilPriceList.add(oilPrice);
                        else if (tagName.equalsIgnoreCase("TRADE_DT")) oilPrice.setTradeDate(text);
                        else if (tagName.equalsIgnoreCase("PRODCD")) oilPrice.setProductCode(text);
                        else if (tagName.equalsIgnoreCase("PRODNM")) oilPrice.setProductName(text);
                        else if (tagName.equalsIgnoreCase("PRICE")) oilPrice.setPrice(Float.parseFloat(text));
                        else if (tagName.equalsIgnoreCase("DIFF")) oilPrice.setDiff(Float.parseFloat(text));
                        break;
                    default: break;
                }
                eventType = parser.next();
            }
        } catch (XmlPullParserException | IOException e) { e.printStackTrace();}

        return oilPriceList;
    }

    // Get the Sido price
    public List<Opinet.SidoPrice> parseSidoPrice(InputStream is) {
        List<Opinet.SidoPrice> sidoPriceList = new ArrayList<>();
        try(InputStream inputStream = is) {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(inputStream, "utf-8");
            int eventType = parser.getEventType();

            while(eventType != XmlPullParser.END_DOCUMENT) {
                String tagName = parser.getName();
                switch(eventType) {
                    case XmlPullParser.START_TAG:
                        if (tagName.equalsIgnoreCase("OIL")) sidoPrice = new Opinet.SidoPrice();
                        break;
                    case XmlPullParser.TEXT:
                        text = parser.getText();
                        break;
                    case XmlPullParser.END_TAG:
                        if (tagName.equalsIgnoreCase("OIL")) sidoPriceList.add(sidoPrice);
                        else if (tagName.equalsIgnoreCase("SIDOCD")) sidoPrice.setSidoCode(text);
                        else if (tagName.equalsIgnoreCase("SIDONM")) sidoPrice.setSidoName(text);
                        else if (tagName.equalsIgnoreCase("PRODCD")) sidoPrice.setProductCd(text);
                        else if (tagName.equalsIgnoreCase("PRICE")) sidoPrice.setPrice(Float.parseFloat(text));
                        else if (tagName.equalsIgnoreCase("DIFF")) sidoPrice.setDiff(Float.parseFloat(text));
                        break;
                    default: break;
                }
                eventType = parser.next();
            }
        } catch (XmlPullParserException | IOException e) { e.printStackTrace();}

        return sidoPriceList;
    }


    // Get the Sigun Price
    public List<Opinet.SigunPrice> parseSigunPrice(InputStream is) {
        List<Opinet.SigunPrice> sigunPriceList = new ArrayList<>();
        try(InputStream inputStream = is) {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(inputStream, "utf-8");
            int eventType = parser.getEventType();

            while(eventType != XmlPullParser.END_DOCUMENT) {
                String tagName = parser.getName();
                switch(eventType) {
                    case XmlPullParser.START_TAG:
                        if (tagName.equalsIgnoreCase("OIL")) sigunPrice = new Opinet.SigunPrice();
                        break;
                    case XmlPullParser.TEXT:
                        text = parser.getText();
                        break;
                    case XmlPullParser.END_TAG:
                        if (tagName.equalsIgnoreCase("OIL")) sigunPriceList.add(sigunPrice);
                        else if (tagName.equalsIgnoreCase("SIGUNCD")) sigunPrice.setSigunCode(text);
                        else if (tagName.equalsIgnoreCase("SIGUNNM")) sigunPrice.setSigunName(text);
                        else if (tagName.equalsIgnoreCase("PRODCD")) sigunPrice.setProductCd(text);
                        else if (tagName.equalsIgnoreCase("PRICE")) sigunPrice.setPrice(Float.parseFloat(text));
                        else if (tagName.equalsIgnoreCase("DIFF")) sigunPrice.setDiff(Float.parseFloat(text));
                        break;
                    default: break;
                }
                eventType = parser.next();
            }
        } catch (XmlPullParserException | IOException e) { e.printStackTrace();}
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

    // Get the station price
    public Opinet.StationPrice parseStationPrice(InputStream is) {
        List<Opinet.StationPrice> stnPriceList = new ArrayList<>();
        try(InputStream inputStream = is) {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(inputStream, "utf-8");
            int eventType = parser.getEventType();

            while(eventType != XmlPullParser.END_DOCUMENT) {
                String tagName = parser.getName();
                String prodCd = null;
                switch(eventType) {
                    case XmlPullParser.START_TAG:
                        if(tagName.equalsIgnoreCase("OIL")) stnPrice = new Opinet.StationPrice();
                        else if(tagName.equalsIgnoreCase("OIL_PRICE")) {
                            int nestedEventType = parser.next();
                            // Loop the nested tag.
                            while(nestedEventType != XmlPullParser.END_DOCUMENT) {
                                String nestedTag = parser.getName();
                                switch(nestedEventType) {
                                    case XmlPullParser.START_TAG:
                                        break;
                                    case XmlPullParser.TEXT:
                                        text = parser.getText();
                                        break;
                                    case XmlPullParser.END_TAG:
                                        if(nestedTag.equalsIgnoreCase("PRODCD")) {
                                            prodCd = text;
                                            stnPrice.setProductCd(text);
                                        }else if(nestedTag.equalsIgnoreCase("PRICE")) {
                                            stnPrice.setStnPrice(prodCd, Float.parseFloat(text));
                                        }
                                        /*
                                        else if(nestedTag.equalsIgnoreCase("TRADE_DT")) {
                                            log.i("Trade date: %s", text);
                                        }else if(nestedTag.equalsIgnoreCase("TRADE_TM")) {
                                            log.i("Trade Time: %s", text);
                                        }
                                        */
                                        break;
                                }
                                nestedEventType = parser.next();
                            }
                        }
                        break;
                    case XmlPullParser.TEXT:
                        text = parser.getText();
                        break;
                    case XmlPullParser.END_TAG:
                        if (tagName.equalsIgnoreCase("OIL")) stnPriceList.add(stnPrice);
                        else if(tagName.equalsIgnoreCase("UNI_ID")) stnPrice.setStnId(text);
                        else if (tagName.equalsIgnoreCase("POLL_DIV_CO")) stnPrice.setStnCompany(text);
                        else if (tagName.equalsIgnoreCase("OS_NM")) stnPrice.setStnName(text);
                        else if (tagName.equalsIgnoreCase("PRODCD")) stnPrice.setProductCd(text);
                        break;
                    default: break;
                }

                eventType = parser.next();
            }

        } catch (XmlPullParserException | IOException e) { e.printStackTrace();}

        // Rearrange the original order(regular-premium-kerotine-diesel) to a new order(preminum-
        // regular-diesel-kerotine)
        /*
        if(sigunPriceList.size() >= 4) {
            //Collections.swap(sigunPriceList, 0, 1);
            //Collections.swap(sigunPriceList, 2, 3);
        }
        */
        for(Opinet.StationPrice stnPrice : stnPriceList) log.i("station price: %s", stnPrice.getStnPrice());
        return stnPrice;
    }


    // GasStation made parcelable
    public List<Opinet.GasStnParcelable> parseStationListParcelable(InputStream is) {
        List<Opinet.GasStnParcelable> gasStnParcelableList = new ArrayList<>();
        try(InputStream inputStream = is) {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(inputStream, "utf-8");
            int eventType = parser.getEventType();

            while(eventType != XmlPullParser.END_DOCUMENT) {
                String tagName = parser.getName();
                switch(eventType) {
                    case XmlPullParser.START_TAG:
                        if (tagName.equalsIgnoreCase("OIL")) gasStnParcelable = new Opinet.GasStnParcelable();
                        break;
                    case XmlPullParser.TEXT:
                        text = parser.getText();
                        break;
                    case XmlPullParser.END_TAG:
                        if (tagName.equalsIgnoreCase("OIL")) gasStnParcelableList.add(gasStnParcelable);
                        else if (tagName.equalsIgnoreCase("UNI_ID")) gasStnParcelable.setStnId(text);
                        else if (tagName.equalsIgnoreCase("POLL_DIV_CO")) gasStnParcelable.setStnCode(text);
                        else if (tagName.equalsIgnoreCase("OS_NM")) gasStnParcelable.setStnName(text);
                        else if (tagName.equalsIgnoreCase("PRICE")) gasStnParcelable.setStnPrice(Integer.parseInt(text));
                        else if (tagName.equalsIgnoreCase("DISTANCE")) gasStnParcelable.setStnDistance(Float.parseFloat(text));
                        else if(tagName.equalsIgnoreCase("GIS_X_COOR")) gasStnParcelable.setLongitude(Float.parseFloat(text));
                        else if(tagName.equalsIgnoreCase("GIS_Y_COOR")) gasStnParcelable.setLatitude(Float.parseFloat(text));
                        break;
                    default: break;
                }

                eventType = parser.next();
            }
        } catch (XmlPullParserException | IOException e) { e.printStackTrace();}

        return gasStnParcelableList;
    }

    // Information on a gas station fetched by its Station ID
    public Opinet.GasStationInfo parseGasStationInfo(InputStream is) {
        //List<Opinet.GasStationInfo> gasStationInfoList = new ArrayList<>();
        try(InputStream inputStream = is) {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(inputStream, "utf-8");
            int eventType = parser.getEventType();

            while(eventType != XmlPullParser.END_DOCUMENT) {
                String tagName = parser.getName();
                switch(eventType){
                    case XmlPullParser.START_TAG:
                        if (tagName.equalsIgnoreCase("OIL")) gasStationInfo = new Opinet.GasStationInfo();
                        break;
                    case XmlPullParser.TEXT:
                        text = parser.getText();
                        break;
                    case XmlPullParser.END_TAG:
                        if(tagName.equalsIgnoreCase("POLL_DIV_CO")) gasStationInfo.setStationCode(text);
                        else if(tagName.equalsIgnoreCase("OS_NAME")) gasStationInfo.setStationName(text);
                        else if(tagName.equalsIgnoreCase("VAN_ADR")) gasStationInfo.setOldAddrs(text);
                        else if(tagName.equalsIgnoreCase("NEW_ADR")) gasStationInfo.setNewAddrs(text);
                        else if(tagName.equalsIgnoreCase("TEL")) gasStationInfo.setTelNo(text);
                        else if(tagName.equalsIgnoreCase("CAR_WASH_YN")) gasStationInfo.setIsCarWash(text);
                        else if(tagName.equalsIgnoreCase("MAINT_YN")) gasStationInfo.setIsService(text);
                        else if(tagName.equalsIgnoreCase("CVS_YN")) gasStationInfo.setIsCVS(text);
                        else if(tagName.equalsIgnoreCase("GIS_X_COOR")) gasStationInfo.setxCoord(text);
                        else if(tagName.equalsIgnoreCase("GIS_Y_COOR")) gasStationInfo.setyCoord(text);
                        else if(tagName.equalsIgnoreCase("OIL_PRICE")) {
                            // Read nested tags in <OIL_PRICE> Looks like that Informaion of all fuels is not provided.
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
                                        if(nestedTag.equalsIgnoreCase("PRICE")) log.i("PRICE: %s", text);
                                        else if (nestedTag.equalsIgnoreCase("PRODCD")) log.i("PRODCD: %s", text);
                                        break;
                                }
                                nestedEventType = parser.next();
                            }
                        }
                        break;

                    default: break;

                }

                eventType = parser.next();
            }


        } catch (XmlPullParserException | IOException e) { e.printStackTrace();}

        return gasStationInfo;

    }

}
