package com.silverback.carman.rest;

import com.tickaroo.tikxml.annotation.PropertyElement;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


// Reserved class that should be replaced w/ Retrofit2 and TikXml if the primitive type of
// XmlHandler outperform the third party api in handling the xml type of respnse from the server.
public class EvXmlPullParser {
    public EvXmlPullParser() {
        //default constructor left empty
    }

    public List<EvStationInfo> parseEvStationInfo(InputStream is) {
        List<EvStationInfo> evList = new ArrayList<>();
        try (InputStream inputStream = is) {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(inputStream, "utf-8");
            int eventType = parser.getEventType();

            String tagName = "";
            EvStationInfo evInfo = null;

            while (eventType != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        tagName = parser.getName();
                        if (tagName.equalsIgnoreCase("item"))
                            evInfo = new EvStationInfo();
                        break;

                    case XmlPullParser.END_TAG:
                        if (parser.getName().equalsIgnoreCase("item")) evList.add(evInfo);
                        break;

                    case XmlPullParser.TEXT:
                        if (evInfo == null) break;
                        switch (tagName) {
                            case "statNm":
                                evInfo.setEvName(parser.getText());
                                break;
                            case "lat":
                                evInfo.setLat(Double.parseDouble(parser.getText()));
                                break;
                            case "lng":
                                evInfo.setLng(Double.parseDouble(parser.getText()));
                                break;
                            case "chgerId":
                                evInfo.setChargerId(parser.getText());
                                break;
                            case "chgerType":
                                evInfo.setChargerType(parser.getText());
                                break;
                            case "stat":
                                evInfo.setChargerStatus(parser.getText());
                                break;
                            case "limitYn":
                                evInfo.setIsPublic(parser.getText());
                                break;
                            case "limitDetail":
                                evInfo.setLimitDetail(parser.getText());
                                break;
                        }
                        break;
                }

                eventType = parser.next();
            }
        } catch (XmlPullParserException | IOException e) {
            e.printStackTrace();
        }

        return evList;
    }

    public static class EvStationInfo {
        private String evName;
        private String chargerId;
        private double lat;
        private double lng;
        private int distance;
        private String chargerType;
        private String chargerStatus;
        private String isPublic;
        private String limitDetail;

        public EvStationInfo() {}

        public double getLat() {
            return lat;
        }
        public void setLat(double lat) {
            this.lat = lat;
        }

        public double getLng() {
            return lng;
        }
        public void setLng(double lng) {
            this.lng = lng;
        }

        public String getEvName() {
            return evName;
        }
        public void setEvName(String evName) {
            this.evName = evName;
        }

        public int getDistance() {return distance;}
        public void setDistance(int distance) {this.distance = distance;}

        public String getChargerId() {
            return chargerId;
        }
        public void setChargerId(String chargerId) {
            this.chargerId = chargerId;
        }

        public String getChargerType() {
            return chargerType;
        }
        public void setChargerType(String chgrType) {
            this.chargerType = chgrType;
        }

        public String getChargerStatus() {
            return chargerStatus;
        }
        public void setChargerStatus(String code) {
            this.chargerStatus = code;
        }

        public String getIsPublic() {
            return isPublic;
        }
        public void setIsPublic(String isPublic) {
            this.isPublic = isPublic;
        }

        public String getLimitDetail() {
            return limitDetail;
        }

        public void setLimitDetail(String limitDetail) {
            this.limitDetail = limitDetail;
        }
    }
}
