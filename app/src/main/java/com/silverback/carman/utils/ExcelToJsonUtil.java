package com.silverback.carman.utils;

public class ExcelToJsonUtil {
    /*
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


    public JsonObject convExcelToJson() throws IOException, InvalidFormatException {
        JsonObject sheetsJsonObject = new JsonObject();
        Workbook workbook = new XSSFWorkbook(excelFile);

        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            JsonArray sheetArray = new JsonArray();
            ArrayList<String> columnNames = new ArrayList<>();
            Sheet sheet = workbook.getSheetAt(i);

            for (Row currentRow : sheet) {
                JsonObject jsonObject = new JsonObject();
                if (currentRow.getRowNum() != 0) {
                    for(int j = 0; j < columnNames.size(); j++) {
                        if (currentRow.getCell(j) != null) {
                            if (currentRow.getCell(j).getCellType() == CellType.STRING) {
                                jsonObject.addProperty(columnNames.get(j), currentRow.getCell(j).getStringCellValue());
                            } else if (currentRow.getCell(j).getCellType() == CellType.NUMERIC) {
                                jsonObject.addProperty(columnNames.get(j), currentRow.getCell(j).getNumericCellValue());
                            } else if (currentRow.getCell(j).getCellType() == CellType.BOOLEAN) {
                                jsonObject.addProperty(columnNames.get(j), currentRow.getCell(j).getBooleanCellValue());
                            } else if (currentRow.getCell(j).getCellType() == CellType.BLANK) {
                                jsonObject.addProperty(columnNames.get(j), "");
                            }
                        } else {
                            jsonObject.addProperty(columnNames.get(j), "");
                        }

                    }
                    sheetArray.add(jsonObject);
                } else {
                    // store column names
                    for (int k = 0; k < currentRow.getPhysicalNumberOfCells(); k++) {
                        columnNames.add(currentRow.getCell(k).getStringCellValue());
                    }
                }

            }

            sheetsJsonObject.add(workbook.getSheetName(i), sheetArray);
        }

        return sheetsJsonObject;
    }

     */

}
