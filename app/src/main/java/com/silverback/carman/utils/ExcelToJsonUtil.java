package com.silverback.carman.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class ExcelToJsonUtil {

    private static final LoggingHelper log = LoggingHelperFactory.create(ExcelToJsonUtil.class);

    private File excelFile;
    private ExcelToJsonUtil() {

    }
    private static class InnerClazz {
        private static final ExcelToJsonUtil sInstance = new ExcelToJsonUtil();
    }
    public static ExcelToJsonUtil getInstance() {
        return InnerClazz.sInstance;
    }

    public void setExcelFile(File file) {
        this.excelFile = file;
    }


    public JSONObject convExcelToJson() throws IOException, InvalidFormatException, JSONException {
        JSONObject sheetsJsonObject = new JSONObject();
        //Workbook workbook = new XSSFWorkbook(excelFile);
        Workbook workbook = WorkbookFactory.create(excelFile);

        for(int i = 0; i < workbook.getNumberOfSheets(); i++) {
            JSONArray sheetArray = new JSONArray();
            ArrayList<String> columnNames = new ArrayList<>();
            Sheet sheet = workbook.getSheetAt(i);
            for(Row currentRow : sheet) {
                JSONObject jsonObject = new JSONObject();
                if (currentRow.getRowNum() >= 3) {
                    for(int j = 0; j < columnNames.size(); j++) {
                        if (currentRow.getCell(j) != null) {
                            if (currentRow.getCell(j).getCellTypeEnum() == CellType.STRING) {
                                log.i("name: %s, %s", currentRow.getCell(0).getStringCellValue(), currentRow.getCell(2).getStringCellValue());
                                jsonObject.put(columnNames.get(j), currentRow.getCell(j).getStringCellValue());
                            } else if (currentRow.getCell(j).getCellTypeEnum() == CellType.NUMERIC) {
                                jsonObject.put(columnNames.get(j), currentRow.getCell(j).getNumericCellValue());
                            } else if (currentRow.getCell(j).getCellTypeEnum() == CellType.BOOLEAN) {
                                jsonObject.put(columnNames.get(j), currentRow.getCell(j).getBooleanCellValue());
                            } else if (currentRow.getCell(j).getCellTypeEnum() == CellType.BLANK) {
                                jsonObject.put(columnNames.get(j), "");
                            }
                        } else {
                            jsonObject.put(columnNames.get(j), "");
                        }

                    }

                    sheetArray.put(jsonObject);

                }

                else {
                    // store column names
                    for (int k = 0; k < currentRow.getPhysicalNumberOfCells(); k++) {
                        log.i("store column names: %s", currentRow.getCell(k).getStringCellValue());
                        columnNames.add(currentRow.getCell(k).getStringCellValue());
                    }
                }

            }

            sheetsJsonObject.put(workbook.getSheetName(i), sheetArray);
        }

        log.i("JSONObject: %s", sheetsJsonObject);
        return sheetsJsonObject;
    }



}
