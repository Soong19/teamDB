package com.db.teamdb;

import org.springframework.stereotype.Component;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class DataMessage implements Serializable {
    // 0表示初始，1表示成功，2表示无数据，3表示失败
    private int Msg=0;
    private List<String> fieldNames=null;

    private List<Map<String, String>> data=null;

    private int count=0;


    private int countFieldName=0;

    public int getMsg() {
        return Msg;
    }

    public void setMsg(int msg) {
        Msg = msg;
    }

    public List<String> getFieldNames() {
        return fieldNames;
    }

    public List<Map<String, String>> getData() {
        return data;
    }

    public int getCount() {
        return count;
    }

    public int getCountFieldName() {
        return countFieldName;
    }

    public DataMessage(){

    }

    public DataMessage(int Msg){
        this.Msg=Msg;
    }

    public DataMessage(List<String> dataNameList,List<Map<String, String>> resultDatas,int Msg){
        this.fieldNames =new ArrayList<>(dataNameList);
        this.countFieldName = fieldNames.size();
        this.data =new ArrayList<>(resultDatas);
        this.count = resultDatas.size();
        this.Msg=Msg;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public void setCountFieldName(int countFieldName) {
        this.countFieldName = countFieldName;
    }

    public void setFieldNames(List<String> dataNameList){
        this.fieldNames =new ArrayList<>(dataNameList);
        setCountFieldName(fieldNames.size());
    }

    public void setData(List<Map<String, String>> resultDatas) {
        this.data =new ArrayList<>(resultDatas);
        setCount(data.size());
    }
}
