package com.silverback.carman.utils;

import android.text.TextUtils;

import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ExcelToJsonUtil {

    private static final LoggingHelper log = LoggingHelperFactory.create(ExcelToJsonUtil.class);

    private final List<HydroStationObj> hydroList;
    private Workbook workbook;
    private int numOfSheets;

    private ExcelToJsonUtil() {
        hydroList = new ArrayList<>();
    }
    private static class InnerClazz {
        private static final ExcelToJsonUtil sInstance = new ExcelToJsonUtil();
    }
    public static ExcelToJsonUtil getInstance() {
        return InnerClazz.sInstance;
    }

    public void setExcelFile(File file) throws IOException, InvalidFormatException {
        workbook = WorkbookFactory.create(file);
        numOfSheets = workbook.getNumberOfSheets();
    }

    public void convExcelToList(int sheetNumber, int header, int start) {
        if(sheetNumber > numOfSheets) return;
        Sheet sheet  = workbook.getSheetAt(sheetNumber);
        ArrayList<String> columnNames = new ArrayList<>();

        // Iterate a row to get a column and its value, which is set to the pojo.
        for(Row currentRow : sheet) {
            HydroStationObj obj = new HydroStationObj();
            if(currentRow.getRowNum() >= start) {
                for(int i = 0; i < columnNames.size(); i++) { // exclude the last row due to no values in it

                    if(currentRow.getCell(i) != null) {
                        if (currentRow.getCell(i).getCellTypeEnum() == CellType.STRING) {
                            obj.setName(currentRow.getCell(0).getStringCellValue());
                            int count = Integer.parseInt(currentRow.getCell(1).getStringCellValue());
                            obj.setCharger(count);
                            obj.setAddrs(currentRow.getCell(2).getStringCellValue());
                            obj.setBizhour(currentRow.getCell(3).getStringCellValue());
                            obj.setPrice(currentRow.getCell(4).getStringCellValue());
                            obj.setPhone(currentRow.getCell(5).getStringCellValue());
                        }
                        /*
                        else if (row.getCell(i).getCellTypeEnum() == CellType.NUMERIC) {

                        } else if (row.getCell(i).getCellTypeEnum() == CellType.BOOLEAN) {

                        } else if (row.getCell(i).getCellTypeEnum() == CellType.BLANK) {

                        }
                        */
                    }
                }

                // HydroStation name should be required.
                if(!TextUtils.isEmpty(obj.getName())) hydroList.add(obj);

            } else if(currentRow.getRowNum() == header){
                // Add a column name to the column name list with the row number given.
                for (int k = 0; k < currentRow.getPhysicalNumberOfCells(); k++) {
                    columnNames.add(currentRow.getCell(k).getStringCellValue());
                }
            }
        }
    }

    /*
    public void convExcelToJson() throws IOException, InvalidFormatException, JSONException {
        JSONObject sheetsJsonObject = new JSONObject();
        //Workbook workbook = new XSSFWorkbook(excelFile);
        Workbook workbook = WorkbookFactory.create(excelFile);

        for(int i = 0; i < workbook.getNumberOfSheets(); i++) {
            //JSONArray sheetArray = new JSONArray();
            ArrayList<String> columnNames = new ArrayList<>();
            Sheet sheet = workbook.getSheetAt(i);
            for(Row currentRow : sheet) {
                HydroStationObj obj = new HydroStationObj();
                //JSONObject jsonObject = new JSONObject();
                if (currentRow.getRowNum() >= 3) {
                    for(int j = 0; j < columnNames.size(); j++) {
                        if (currentRow.getCell(j) != null) {
                            if (currentRow.getCell(j).getCellTypeEnum() == CellType.STRING) {
                                obj.setName(currentRow.getCell(0).getStringCellValue());
                                int count = Integer.parseInt(currentRow.getCell(1).getStringCellValue());
                                obj.setCount(count);
                                obj.setAddrs(currentRow.getCell(2).getStringCellValue());
                                obj.setBizhour(currentRow.getCell(3).getStringCellValue());
                                obj.setPrice(currentRow.getCell(4).getStringCellValue());
                                obj.setPhone(currentRow.getCell(5).getStringCellValue());

                                //jsonObject.put(columnNames.get(j), currentRow.getCell(j).getStringCellValue());
                            } else if (currentRow.getCell(j).getCellTypeEnum() == CellType.NUMERIC) {
                                //jsonObject.put(columnNames.get(j), currentRow.getCell(j).getNumericCellValue());
                            } else if (currentRow.getCell(j).getCellTypeEnum() == CellType.BOOLEAN) {
                                //jsonObject.put(columnNames.get(j), currentRow.getCell(j).getBooleanCellValue());
                            } else if (currentRow.getCell(j).getCellTypeEnum() == CellType.BLANK) {
                                //jsonObject.put(columnNames.get(j), "");
                            }
                        } else {
                            //jsonObject.put(columnNames.get(j), "");
                        }

                    }

                    //sheetArray.put(jsonObject);
                    hydroList.add(obj);

                } else {
                    // store column names
                    log.i("current column name row number: %s", currentRow.getRowNum());
                    for (int k = 0; k < currentRow.getPhysicalNumberOfCells(); k++) {
                        columnNames.add(currentRow.getCell(k).getStringCellValue());
                    }
                }

            }

            //sheetsJsonObject.put(workbook.getSheetName(i), sheetArray);
        }

        //return hydroList;
    }
     */

    public List<HydroStationObj> getHydroList() {
        return hydroList;
    }

    public static class HydroStationObj {
        String name;
        int charger;
        String addrs;
        double lat;
        double lng;
        String bizhour;
        String price;
        String phone;

        public HydroStationObj(){}
        public String getName() {
            return name;
        }

        public void setName(String stationName) {
            this.name = stationName;
        }

        public int getCharger() {
            return charger;
        }

        public void setCharger(int count) {
            this.charger = count;
        }

        public String getAddrs() {
            return addrs;
        }

        public void setAddrs(String addrs) {
            this.addrs = addrs;
        }

        public String getBizhour() {
            return bizhour;
        }

        public void setBizhour(String bizhour) {
            this.bizhour = bizhour;
        }

        public String getPrice() {
            return price;
        }

        public void setPrice(String price) {
            this.price = price;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

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


    }



}
