package com.novax.util;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ExcelUtil {
    public static HSSFCell getCell(String name, int sheetIndex, int rowIndex, int cellIndex) {
        InputStream xlsInputStream = null;
        HSSFCell cell = null;
        try {
            // Excel工作簿输入流
            xlsInputStream = ExcelUtil.class.getResourceAsStream(name);
            // 构造工作簿对象
            HSSFWorkbook hssfWorkbook = new HSSFWorkbook(Objects.requireNonNull(xlsInputStream));
            // 获取工作簿
            HSSFSheet sheetAt = hssfWorkbook.getSheetAt(sheetIndex);
            // 获取行
            HSSFRow hssfRow = sheetAt.getRow(rowIndex);
            // 获取单元格
            cell = hssfRow.getCell(cellIndex);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (xlsInputStream != null) {
                try {
                    xlsInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return cell;
    }


    public static Object[][] readObjDatas(String name, int sheetIndex) throws IOException {
        List<Object[]> records = new ArrayList<>();
        Object[][] results;

        // Excel工作簿输入流
        InputStream xlsInputStream = ExcelUtil.class.getResourceAsStream(name);
        // 构造工作簿对象
        HSSFWorkbook hssfWorkbook = new HSSFWorkbook(Objects.requireNonNull(xlsInputStream));
        // 获取工作簿
        HSSFSheet sheetAt = hssfWorkbook.getSheetAt(sheetIndex);

        int lastRowNum = sheetAt.getLastRowNum();
        // System.out.println("行数：" + lastRowNum);
        out:
        for (int i = 1; i <= lastRowNum; i++) {
            // 获取行
            HSSFRow hssfRow = sheetAt.getRow(i);
            // System.out.println("最大列号：" + hssfRow.getLastCellNum());
            String[] fields = new String[hssfRow.getLastCellNum()];
            for (int j = 0; j < hssfRow.getLastCellNum(); j++) {
                // 获取单元格
                HSSFCell cell = hssfRow.getCell(j);
                if (cell == null) {
                    break;
                } else {
                    fields[j] = cell.getStringCellValue();
                }
            }
            for (String field : fields) {
                if (field == null || field.equals("")) {
                    break out;
                }
            }
            records.add(fields);
        }
        // list转换为Object[][]
        // System.out.println("records.size():"+records.size());
        results = new Object[records.size()][];
        // 设置二维数组每行的值，每行是一个Object对象
        for (int i = 0; i < records.size(); i++) {
            results[i] = records.get(i);
        }
        return results;
    }


    public static void main(String[] args) throws IOException {
        String path = "/order/queryGridDetail.xls";
        Object[][] res = ExcelUtil.readObjDatas(path, 2);
        System.out.println("res.length:" + res.length);

        for (int i = 0; i < res.length; i++) {
            for (int j = 0; j < res[i].length; j++) {
                System.out.println(res[i][j]);
            }
        }
        HSSFCell token = ExcelUtil.getCell("/order/adjustMarketPrice.xls", 0, 1, 0);
        System.out.println(token.getStringCellValue());


    }
}
