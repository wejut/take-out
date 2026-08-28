package com.sky.service.impl;

import com.sky.entity.Orders;
import com.sky.mapper.ReportMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private ReportMapper reportMapper;
    @Autowired
    private WorkspaceService workspaceService;

    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {

        List<LocalDate> dateList=new ArrayList<>();
        List<Double> turnoverList=new ArrayList<>();
        while (!begin.equals(end)){
            dateList.add(begin);
            begin = begin.plusDays(1);
        }
        dateList.add(end);
        for(LocalDate date: dateList){
            //遍历日历，每次进行数据库检索sun（amount），add到turnoverList里面
            LocalDateTime dayMin = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime dayMax = LocalDateTime.of(date, LocalTime.MAX);
            //select sum(amount) from orders where order_time<MAX and order_time>MIN and status=5
            Double turnover = reportMapper.sumAmountByDate(dayMin, dayMax, Orders.COMPLETED);
            turnover= turnover==null ? 0.0 : turnover;//如果为空赋值0.0
            turnoverList.add(turnover);
        }

        TurnoverReportVO turnoverReportVO = TurnoverReportVO
                .builder()
                .dateList(StringUtils.join(dateList, ","))
                .turnoverList(StringUtils.join(turnoverList, ","))
                .build();
        return turnoverReportVO;
    }

    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();
        List<Integer> countUserList = new ArrayList<>();
        List<Integer> countNewUserList = new ArrayList<>();
        while (!begin.equals(end)){
            dateList.add(begin);
            begin = begin.plusDays(1);
        }
        dateList.add(end);
        //select count(id) from user where create_time<dayMax作为用户总数
        //select count(id) from user where create_time<dayMax and create_time>dayMin作为新增用户数
        //用Mybatis动态查询count(id)如果参数只有end那么查询的就是用户总数，在放入begin那么查的就是区间内的新增用户数
        for(LocalDate date: dateList){
            LocalDateTime dayMin = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime dayMax = LocalDateTime.of(date, LocalTime.MAX);
            Map map = new HashMap<>();
            map.put("dayMax",dayMax);
            Integer AmountOfUsers = reportMapper.countUserIdByDate(map);
            map.put("dayMin",dayMin);
            Integer AmountOfNewUsers = reportMapper.countUserIdByDate(map);
            countUserList.add(AmountOfUsers);
            countNewUserList.add(AmountOfNewUsers);
        }
        UserReportVO userReportVO = UserReportVO.builder()
                .dateList(StringUtils.join(dateList))
                .newUserList(StringUtils.join(countNewUserList))
                .totalUserList(StringUtils.join(countUserList))
                .build();
        return userReportVO;
    }

    public OrderReportVO getOrderStatisticsByDate(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();
        List<Integer> countOrdersList = new ArrayList<>();
        List<Integer> countValidOrdersList = new ArrayList<>();
        while (!begin.equals(end)){
            dateList.add(begin);
            begin = begin.plusDays(1);
        }
        dateList.add(end);

        //select count(id) from orders where order_time < dayMax and order_time > dayMin订单数
        //select count(id) from orders where order_time < dayMax and order_time > dayMin and status = 5订单数
        for(LocalDate date: dateList){
            LocalDateTime dayMax = LocalDateTime.of(date, LocalTime.MAX);
            LocalDateTime dayMin = LocalDateTime.of(date, LocalTime.MIN);
            Map map = new HashMap<>();
            map.put("dayMax",dayMax);
            map.put("dayMin",dayMin);
            Integer AmountOfOrders = reportMapper.countOrdersByDateAndStatusDynamically(map);
            map.put("status",Orders.COMPLETED);
            Integer AmountOfValidOrders = reportMapper.countOrdersByDateAndStatusDynamically(map);
            countOrdersList.add(AmountOfOrders);
            countValidOrdersList.add(AmountOfValidOrders);
        }
        Integer totalAmountOfOrders = countOrdersList.stream().reduce(Integer::sum).get();
        Integer totalAmountOfValidOrders = countValidOrdersList.stream().reduce(Integer::sum).get();
        Double orderCompletionRate = 0.0;
        if (totalAmountOfOrders != 0){
            orderCompletionRate = Double.valueOf(totalAmountOfValidOrders) / totalAmountOfOrders;
        }
        OrderReportVO orderReportVO = OrderReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .orderCountList(StringUtils.join(countOrdersList, ","))
                .validOrderCountList(StringUtils.join(countValidOrdersList, ","))
                .orderCompletionRate(orderCompletionRate)
                .totalOrderCount(totalAmountOfOrders)
                .validOrderCount(totalAmountOfValidOrders)
                .build();
        return orderReportVO;
    }


    public SalesTop10ReportVO getTop10ByDate(LocalDate begin, LocalDate end) {
        LocalDateTime Max = LocalDateTime.of(end, LocalTime.MAX);
        LocalDateTime Min = LocalDateTime.of(begin, LocalTime.MIN);
        List<String> nameList = new ArrayList<>();
        List<Integer> numberList = new ArrayList<>();
        //select od.name, sum(od.number) from order_detail od left join orders o on od.order_id= o.id where o.status = 5 and o.order_time < Max and o.order_time > Min group by od.name order by sum(od.number) desc
        Map map = new HashMap();
        map.put("Max",Max);
        map.put("Min",Min);
        List<Map<String, Object>> top10ByDate = reportMapper.getTop10ByDate(map);
        for(Map<String,Object> NameNumberList : top10ByDate){
            String name = (String) NameNumberList.get("name");
            Integer number = (Integer) NameNumberList.get("number");
            nameList.add(name);
            numberList.add(number);
        }
        SalesTop10ReportVO top10 = SalesTop10ReportVO.builder()
                .nameList(StringUtils.join(nameList,","))
                .numberList(StringUtils.join(numberList,","))
                .build();
        return top10;
    }

    /**
     * 导出运营数据报表
     * @param response
     */
    public void exportBusinessData(HttpServletResponse response) {
        //1. 查询数据库，获取营业数据---查询最近30天的运营数据
        LocalDate dateBegin = LocalDate.now().minusDays(30);
        LocalDate dateEnd = LocalDate.now().minusDays(1);

        //查询概览数据
        BusinessDataVO businessDataVO = workspaceService.getBusinessData(LocalDateTime.of(dateBegin, LocalTime.MIN), LocalDateTime.of(dateEnd, LocalTime.MAX));

        //2. 通过POI将数据写入到Excel文件中
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");

        try {
            //基于模板文件创建一个新的Excel文件
            XSSFWorkbook excel = new XSSFWorkbook(in);

            //获取表格文件的Sheet页
            XSSFSheet sheet = excel.getSheet("Sheet1");

            //填充数据--时间
            sheet.getRow(1).getCell(1).setCellValue("时间：" + dateBegin + "至" + dateEnd);

            //获得第4行
            XSSFRow row = sheet.getRow(3);
            row.getCell(2).setCellValue(businessDataVO.getTurnover());
            row.getCell(4).setCellValue(businessDataVO.getOrderCompletionRate());
            row.getCell(6).setCellValue(businessDataVO.getNewUsers());

            //获得第5行
            row = sheet.getRow(4);
            row.getCell(2).setCellValue(businessDataVO.getValidOrderCount());
            row.getCell(4).setCellValue(businessDataVO.getUnitPrice());

            //填充明细数据
            for (int i = 0; i < 30; i++) {
                LocalDate date = dateBegin.plusDays(i);
                //查询某一天的营业数据
                BusinessDataVO businessData = workspaceService.getBusinessData(LocalDateTime.of(date, LocalTime.MIN), LocalDateTime.of(date, LocalTime.MAX));

                //获得某一行
                row = sheet.getRow(7 + i);
                row.getCell(1).setCellValue(date.toString());
                row.getCell(2).setCellValue(businessData.getTurnover());
                row.getCell(3).setCellValue(businessData.getValidOrderCount());
                row.getCell(4).setCellValue(businessData.getOrderCompletionRate());
                row.getCell(5).setCellValue(businessData.getUnitPrice());
                row.getCell(6).setCellValue(businessData.getNewUsers());
            }

            //3. 通过输出流将Excel文件下载到客户端浏览器
            ServletOutputStream out = response.getOutputStream();
            excel.write(out);

            //关闭资源
            out.close();
            excel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
