package com.db.teamdb;

import org.springframework.stereotype.Component;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
@Component
public class Operating {
    private static final Pattern PATTERN_INSERT = Pattern.compile("insert\\s+into\\s+(\\w+)(\\(((\\w+,?)+)\\))?\\s+\\w+\\((([^\\)]+,?)+)\\);?");
    private static final Pattern PATTERN_CREATE_TABLE = Pattern.compile("create\\stable\\s(\\w+)\\s?\\(((?:\\s?\\w+\\s\\w+(\\s\\*)?,?)+)\\)\\s?;");
    private static final Pattern PATTERN_ALTER_TABLE_ADD = Pattern.compile("alter\\stable\\s(\\w+)\\sadd\\s(\\w+\\s\\w+)\\s?;");
    private static final Pattern PATTERN_DELETE = Pattern.compile("delete\\sfrom\\s(\\w+)(?:\\swhere\\s(\\w+\\s?[<=>]\\s?[^\\s\\;]+(?:\\sand\\s(?:\\w+)\\s?(?:[<=>])\\s?(?:[^\\s\\;]+))*))?\\s?;");
    private static final Pattern PATTERN_UPDATE = Pattern.compile("update\\s(\\w+)\\sset\\s(\\w+\\s?=\\s?[^,\\s]+(?:\\s?,\\s?\\w+\\s?=\\s?[^,\\s]+)*)(?:\\swhere\\s(\\w+\\s?[<=>]\\s?[^\\s\\;]+(?:\\sand\\s(?:\\w+)\\s?(?:[<=>])\\s?(?:[^\\s\\;]+))*))?\\s?;");
    private static final Pattern PATTERN_DROP_TABLE = Pattern.compile("drop\\stable\\s(\\w+);");
    private static final Pattern PATTERN_SELECT = Pattern.compile("select\\s(\\*|(?:(?:\\w+(?:\\.\\w+)?)+(?:\\s?,\\s?\\w+(?:\\.\\w+)?)*))\\sfrom\\s(\\w+(?:\\s?,\\s?\\w+)*)(?:\\swhere\\s([^\\;]+\\s?;))?");
    private static final Pattern PATTERN_DELETE_INDEX = Pattern.compile("delete\\sindex\\s(\\w+)\\s?;");


    public static DataMessage handleCmd(String cmd){
        DataMessage dataMessage = new DataMessage(3);

        Matcher matcherInsert = PATTERN_INSERT.matcher(cmd);
        Matcher matcherCreateTable = PATTERN_CREATE_TABLE.matcher(cmd);
        Matcher matcherAlterTable_add = PATTERN_ALTER_TABLE_ADD.matcher(cmd);
        Matcher matcherDelete = PATTERN_DELETE.matcher(cmd);
        Matcher matcherUpdate = PATTERN_UPDATE.matcher(cmd);
        Matcher matcherDropTable = PATTERN_DROP_TABLE.matcher(cmd);
        Matcher matcherSelect = PATTERN_SELECT.matcher(cmd);
        Matcher matcherDeleteIndex = PATTERN_DELETE_INDEX.matcher(cmd);

        if (matcherAlterTable_add.find()) {
            return alterTableAdd(matcherAlterTable_add);
        }

        if (matcherDropTable.find()) {
            return dropTable(matcherDropTable);
        }

        if (matcherCreateTable.find()) {
            return createTable(matcherCreateTable);
        }

        if (matcherDelete.find()) {
            return delete(matcherDelete);
        }

        if (matcherUpdate.find()) {
            return update(matcherUpdate);
        }

        if (matcherInsert.find()) {
            return insert(matcherInsert);
        }

        if (matcherSelect.find()) {
            return select(matcherSelect);
        }

        if (matcherDeleteIndex.find()) {
            return deleteIndex(matcherDeleteIndex);
        }

        return dataMessage;
    }

    private static DataMessage deleteIndex(Matcher matcherDeleteIndex) {
        String tableName = matcherDeleteIndex.group(1);
        Table table = Table.getTable(tableName);
        System.out.println(table.deleteIndex());
        return new DataMessage(1);
    }

    public static DataMessage select(Matcher matcherSelect) {
        DataMessage dataMessage=new DataMessage(1);
        try{

            //将读到的所有数据放到tableDatasMap中
            Map<String, List<Map<String, String>>> tableDatasMap = new LinkedHashMap<>();

            //将投影放在Map<String,List<String>> projectionMap中
            Map<String, List<String>> projectionMap = new LinkedHashMap<>();


            List<String> tableNames = StringUtil.parseFrom(matcherSelect.group(2));

            String whereStr = matcherSelect.group(3);

            //将tableName和table.fieldMap放入
            Map<String, Map<String, Field>> fieldMaps = new HashMap();

            for (String tableName : tableNames) {
                Table table = Table.getTable(tableName);
                if (null == table) {
                    System.out.println("未找到表：" + tableName);
                    dataMessage.setMsg(3);
                    return dataMessage;
                }
                Map<String, Field> fieldMap = table.getFieldMap();
                fieldMaps.put(tableName, fieldMap);

                //解析选择
                List<SingleFilter> singleFilters = new ArrayList<>();
                List<Map<String, String>> filtList = StringUtil.parseWhere(whereStr, tableName, fieldMap);
                for (Map<String, String> filtMap : filtList) {
                    SingleFilter singleFilter = new SingleFilter(fieldMap.get(filtMap.get("fieldName"))
                            , filtMap.get("relationshipName"), filtMap.get("condition"));

                    singleFilters.add(singleFilter);
                }

                //解析最终投影
                List<String> projections = StringUtil.parseProjection(matcherSelect.group(1), tableName, fieldMap);
                projectionMap.put(tableName, projections);


                //读取数据并进行选择操作
                List<Map<String, String>> srcDatas = table.read(singleFilters);
                List<Map<String, String>> datas = associatedTableName(tableName, srcDatas);

                tableDatasMap.put(tableName, datas);
            }


            //解析连接条件，并创建连接对象jion
            List<Map<String, String>> joinConditionMapList = StringUtil.parseWhere_join(whereStr, fieldMaps);
            List<JoinCondition> joinConditionList = new LinkedList<>();
            for (Map<String, String> joinMap : joinConditionMapList) {
                String tableName1 = joinMap.get("tableName1");
                String tableName2 = joinMap.get("tableName2");
                String fieldName1 = joinMap.get("field1");
                String fieldName2 = joinMap.get("field2");
                Field field1 = fieldMaps.get(tableName1).get(fieldName1);
                Field field2 = fieldMaps.get(tableName2).get(fieldName2);
                String relationshipName = joinMap.get("relationshipName");
                JoinCondition joinCondition = new JoinCondition(tableName1, tableName2, field1, field2, relationshipName);

                joinConditionList.add(joinCondition);

                //将连接条件的字段加入投影中
                projectionMap.get(tableName1).add(fieldName1);
                projectionMap.get(tableName2).add(fieldName2);
            }

            List<Map<String, String>> resultDatas = Join.joinData(tableDatasMap, joinConditionList, projectionMap);
            //System.out.println(resultDatas);

            //将需要显示的字段名按table.filed的型式存入dataNameList
            List<String> dataNameList = new LinkedList<>();
            for (Map.Entry<String, List<String>> projectionEntry : projectionMap.entrySet()) {
                String projectionKey = projectionEntry.getKey();
                List<String> projectionValues = projectionEntry.getValue();
                for (String projectionValue : projectionValues) {
                    dataNameList.add(projectionKey + "." + projectionValue);
                }
            }
            //设置返回数据
            dataMessage=new DataMessage(dataNameList,resultDatas,1);
        }catch (Exception e){
            dataMessage.setMsg(3);
        }
        //返回得到的信息
        return dataMessage;

    }

    private static DataMessage insert(Matcher matcherInsert) {
        DataMessage dataMessage=new DataMessage(1);
        String tableName = matcherInsert.group(1);
        Table table = Table.getTable(tableName);
        if (null == table) {
            System.out.println("未找到表：" + tableName);
            dataMessage.setMsg(3);
            return dataMessage;
        }
        Map dictMap = table.getFieldMap();
        Map<String, String> data = new HashMap<>();

        String[] fieldValues = matcherInsert.group(5).trim().split(",");
        //如果插入指定的字段
        if (null != matcherInsert.group(2)) {
            String[] fieldNames = matcherInsert.group(3).trim().split(",");
            //如果insert的名值数量不相等，错误
            if (fieldNames.length != fieldValues.length) {
                dataMessage.setMsg(3);
                return dataMessage;
            }
            for (int i = 0; i < fieldNames.length; i++) {
                String fieldName = fieldNames[i].trim();
                String fieldValue = fieldValues[i].trim();
                //如果在数据字典中未发现这个字段，返回错误
                if (!dictMap.containsKey(fieldName)) {
                    dataMessage.setMsg(3);
                    return dataMessage;
                }
                data.put(fieldName, fieldValue);
            }
        } else {//否则插入全部字段
            Set<String> fieldNames = dictMap.keySet();
            int i = 0;
            for (String fieldName : fieldNames) {
                String fieldValue = fieldValues[i].trim();

                data.put(fieldName, fieldValue);

                i++;
            }
        }
        table.insert(data);
        return dataMessage;
    }

    private static DataMessage update(Matcher matcherUpdate) {
        DataMessage dataMessage = new DataMessage(1);
        String tableName = matcherUpdate.group(1);
        String setStr = matcherUpdate.group(2);
        String whereStr = matcherUpdate.group(3);

        Table table = Table.getTable(tableName);
        if (null == table) {
            System.out.println("未找到表：" + tableName);
            dataMessage.setMsg(3);
            return dataMessage;
        }
        Map<String, Field> fieldMap = table.getFieldMap();
        Map<String, String> data = StringUtil.parseUpdateSet(setStr);

        List<SingleFilter> singleFilters = new ArrayList<>();
        if (null == whereStr) {
            table.update(data, singleFilters);
        } else {
            List<Map<String, String>> filtList = StringUtil.parseWhere(whereStr);
            for (Map<String, String> filtMap : filtList) {
                SingleFilter singleFilter = new SingleFilter(fieldMap.get(filtMap.get("fieldName"))
                        , filtMap.get("relationshipName"), filtMap.get("condition"));

                singleFilters.add(singleFilter);
            }
            table.update(data, singleFilters);
        }
        return dataMessage;
    }

    private static DataMessage delete(Matcher matcherDelete) {
        DataMessage dataMessage = new DataMessage(1);
        String tableName = matcherDelete.group(1);
        String whereStr = matcherDelete.group(2);
        Table table = Table.getTable(tableName);
        if (null == table) {
            System.out.println("未找到表：" + tableName);
            dataMessage.setMsg(3);
            return dataMessage;
        }

        Map<String, Field> fieldMap = table.getFieldMap();

        List<SingleFilter> singleFilters = new ArrayList<>();
        if (null == whereStr) {
            table.delete(singleFilters);
        } else {
            List<Map<String, String>> filtList = StringUtil.parseWhere(whereStr);
            for (Map<String, String> filtMap : filtList) {
                SingleFilter singleFilter = new SingleFilter(fieldMap.get(filtMap.get("fieldName"))
                        , filtMap.get("relationshipName"), filtMap.get("condition"));

                singleFilters.add(singleFilter);
            }
            table.delete(singleFilters);
        }
        return dataMessage;
    }

    private static DataMessage createTable(Matcher matcherCreateTable) {
        String tableName = matcherCreateTable.group(1);
        String propertys = matcherCreateTable.group(2);
        Map<String, Field> fieldMap = StringUtil.parseCreateTable(propertys);

        System.out.println(Table.createTable(tableName, fieldMap));
        return new DataMessage(1);
    }

    private static DataMessage dropTable(Matcher matcherDropTable) {
        String tableName = matcherDropTable.group(1);
        System.out.println(Table.dropTable(tableName));
        return new DataMessage(1);
    }

    private static DataMessage alterTableAdd(Matcher matcherAlterTable_add) {
        DataMessage dataMessage = new DataMessage(1);
        String tableName = matcherAlterTable_add.group(1);
        String propertys = matcherAlterTable_add.group(2);
        Map<String, Field> fieldMap = StringUtil.parseCreateTable(propertys);
        Table table = Table.getTable(tableName);
        if (null == table) {
            System.out.println("未找到表：" + tableName);
            dataMessage.setMsg(3);
            return dataMessage;
        }
        System.out.println(table.addDict(fieldMap));
        return dataMessage;
    }

    /**
     * 将数据整理成tableName.fieldName dataValue的型式
     *
     * @param tableName 表名
     * @param srcDatas  原数据
     * @return 添加表名后的数据
     */
    private static List<Map<String, String>> associatedTableName(String tableName, List<Map<String, String>> srcDatas) {
        List<Map<String, String>> destDatas = new ArrayList<>();
        for (Map<String, String> srcData : srcDatas) {
            Map<String, String> destData = new LinkedHashMap<>();
            for (Map.Entry<String, String> data : srcData.entrySet()) {
                destData.put(tableName + "." + data.getKey(), data.getValue());
            }
            destDatas.add(destData);
        }
        return destDatas;
    }

}

