package com.jianzhang.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Excel 导出 / 导入解析工具（Apache POI）
 * <p>
 * 同时支持 .xlsx 与 .xls（读取经由 WorkbookFactory 自动识别格式）
 */
@Slf4j
public class ExcelUtil {

    /** 单次导入允许的最大数据行数，避免超大文件拖垮服务 */
    public static final int MAX_IMPORT_ROWS = 1000;

    private ExcelUtil() {
    }

    /**
     * 导出 Excel 到浏览器
     *
     * @param response 响应对象
     * @param fileName 文件名（不含扩展名）
     * @param headers  表头
     * @param rows     数据行（与表头顺序一致）
     */
    public static void export(HttpServletResponse response, String fileName,
                              List<String> headers, List<List<Object>> rows) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("数据");
            CellStyle headerStyle = buildHeaderStyle(workbook);
            CellStyle bodyStyle = buildBodyStyle(workbook);
            fillSheet(workbook, sheet, headers, rows, headerStyle, bodyStyle);
            writeToResponse(workbook, response, fileName);
            log.info("Excel 导出完成：{}，共 {} 行", fileName, rows.size());
        }
    }

    /**
     * 导出导入模板：第 1 行为表头，紧随示例数据，另附「填写说明」工作表
     *
     * @param fileName    文件名（不含扩展名）
     * @param headers     表头（需与导入解析的表头一致）
     * @param sampleRows  示例数据（可为空）
     * @param tips        填写说明（逐条展示）
     */
    public static void exportTemplate(HttpServletResponse response, String fileName,
                                      List<String> headers, List<List<Object>> sampleRows,
                                      List<String> tips) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("数据");
            CellStyle headerStyle = buildHeaderStyle(workbook);
            CellStyle bodyStyle = buildBodyStyle(workbook);
            fillSheet(workbook, sheet, headers, sampleRows == null ? new ArrayList<>() : sampleRows,
                    headerStyle, bodyStyle);

            // 说明页
            Sheet tipSheet = workbook.createSheet("填写说明");
            int index = 0;
            Row titleRow = tipSheet.createRow(index++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("导入填写说明");
            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 13);
            titleStyle.setFont(titleFont);
            titleCell.setCellStyle(titleStyle);
            tipSheet.setColumnWidth(0, 100 * 256);
            index++;

            List<String> lines = new ArrayList<>();
            lines.add("表头：" + String.join(" | ", headers));
            if (tips != null) {
                lines.addAll(tips);
            }
            lines.add("提示：请勿修改「数据」工作表的表头名称与列顺序，示例行可直接删除或覆盖。");
            for (String line : lines) {
                Row row = tipSheet.createRow(index++);
                row.createCell(0).setCellValue(line);
            }
            writeToResponse(workbook, response, fileName);
            log.info("Excel 模板生成完成：{}", fileName);
        }
    }

    /**
     * 读取上传的 Excel（仅取第一个工作表），返回去除空行后的文本矩阵
     * <p>
     * 第一行为表头，调用方自行处理；所有单元格按显示值转字符串，日期/数字不会出现科学计数法。
     */
    public static List<List<String>> read(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IOException("请选择需要导入的 Excel 文件");
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (!name.endsWith(".xlsx") && !name.endsWith(".xls")) {
            throw new IOException("仅支持 .xlsx / .xls 格式的 Excel 文件");
        }
        List<List<String>> rows = new ArrayList<>();
        DataFormatter formatter = new DataFormatter();
        try (InputStream in = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(in)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                return rows;
            }
            for (int r = 0; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) {
                    continue;
                }
                int cellCount = row.getLastCellNum();
                if (cellCount <= 0) {
                    continue;
                }
                List<String> cells = new ArrayList<>(cellCount);
                boolean blank = true;
                for (int c = 0; c < cellCount; c++) {
                    Cell cell = row.getCell(c);
                    String text = cell == null ? "" : formatter.formatCellValue(cell).trim();
                    if (!text.isEmpty()) {
                        blank = false;
                    }
                    cells.add(text);
                }
                if (!blank) {
                    rows.add(cells);
                }
            }
        } catch (IllegalArgumentException e) {
            throw new IOException("Excel 文件格式无法识别，请使用模板重新填写");
        }
        if (rows.size() > MAX_IMPORT_ROWS + 1) {
            throw new IOException("单次最多导入 " + MAX_IMPORT_ROWS + " 行数据，请拆分文件后重试");
        }
        return rows;
    }

    // ------------------------------------------------------------
    // 内部实现
    // ------------------------------------------------------------

    private static void fillSheet(Workbook workbook, Sheet sheet, List<String> headers,
                                  List<List<Object>> rows, CellStyle headerStyle, CellStyle bodyStyle) {
        Row headerRow = sheet.createRow(0);
        headerRow.setHeightInPoints(24);
        for (int i = 0; i < headers.size(); i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers.get(i));
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(i, 20 * 256);
        }

        int rowIndex = 1;
        for (List<Object> data : rows) {
            Row row = sheet.createRow(rowIndex++);
            row.setHeightInPoints(20);
            for (int i = 0; i < data.size(); i++) {
                Cell cell = row.createCell(i);
                Object value = data.get(i);
                if (value instanceof BigDecimal) {
                    cell.setCellValue(((BigDecimal) value).doubleValue());
                } else if (value instanceof Number) {
                    cell.setCellValue(((Number) value).doubleValue());
                } else {
                    cell.setCellValue(value == null ? "" : String.valueOf(value));
                }
                cell.setCellStyle(bodyStyle);
            }
        }
    }

    private static CellStyle buildHeaderStyle(Workbook workbook) {
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 11);
        headerStyle.setFont(headerFont);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        return headerStyle;
    }

    private static CellStyle buildBodyStyle(Workbook workbook) {
        CellStyle bodyStyle = workbook.createCellStyle();
        bodyStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        bodyStyle.setBorderBottom(BorderStyle.HAIR);
        return bodyStyle;
    }

    private static void writeToResponse(Workbook workbook, HttpServletResponse response,
                                        String fileName) throws IOException {
        String encodedName = URLEncoder.encode(fileName, "UTF-8").replaceAll("\\+", "%20");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment;filename*=UTF-8''" + encodedName + ".xlsx");
        response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
        response.setHeader("Cache-Control", "no-store");
        OutputStream out = response.getOutputStream();
        workbook.write(out);
        out.flush();
    }

    /**
     * 便捷方法：把一组表头 + 示例行按导入模板输出
     */
    public static void exportTemplate(HttpServletResponse response, String fileName,
                                      List<String> headers, List<List<Object>> sampleRows,
                                      String... tips) throws IOException {
        exportTemplate(response, fileName, headers, sampleRows,
                tips == null ? new ArrayList<>() : Arrays.asList(tips));
    }
}
