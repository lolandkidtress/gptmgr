package com.openapi.Basic;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.type.TypeReference;


/**
 * Created by James on 16/5/23.
 */
public class Return extends HashMap<String, Object> {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LogManager.getLogger(Return.class.getName());



    //////////////////////////////// create//////////////////////////////////
    public static Return create() {
        return new Return();
    }

    public static Return create(String key, Object value) {
        return new Return().add(key, value);
    }

    public static Return create(String json) {
        Return jo = new Return();
        try {
            Map<String, Object> fromJson = JsonConvert.toObject(json, new TypeReference<HashMap<String, Object>>() {
            });
            for (Entry<String, Object> entry : fromJson.entrySet()) {
                jo.put(entry.getKey(), entry.getValue());
            }
        } catch (IOException e) {
            LOGGER.error("TReturn.create 解析 JSON 失败", e);
            return Return.FAIL(BasicCode.error);
        }
        return jo;
    }

    public static Return copy(Return ret){
        return Return.create(ret.toJson());
    }



    /////////////////////////////////////////// SUCCESS/////////////////////////

    public static Return SUCCESS(Integer code, String note) {
        Return jo = new Return();
        jo.put(Return_Fields.success.name(), true);
        jo.put(Return_Fields.code.name(), code);
        jo.put(Return_Fields.note.name(), note);

        return jo;
    }

    public static Return SUCCESS(String json) {
        Return jo = create(json);
        jo.put(Return_Fields.success.name(), true);
        return jo;
    }
    //public <T extends absNode> boolean register(T node){
    public static <T extends BasicCode> Return SUCCESS(T basicCode) {
        return SUCCESS(basicCode.code, basicCode.note);
    }

    ///////////////////////////////////////////////// FAIL////////////////////////////
    public static Return FAIL(Integer code, String note) {
        Return jo = new Return();
        jo.put(Return_Fields.success.name(), false);
        jo.put(Return_Fields.code.name(), code);
        jo.put(Return_Fields.note.name(), note);
        return jo;
    }

    public static <T extends BasicCode> Return FAIL(T basicCode) {
        return FAIL(basicCode.code, basicCode.note);
    }

    //////////////////////////////////// GETTER SETTER///////////////////////////
    public Boolean is_success() {
        return (Boolean) this.getOrDefault(Return_Fields.success.name(), false);
    }

    public Integer get_code() {
        return (Integer) this.getOrDefault(Return_Fields.code.name(), BasicCode.error.code);
    }

    public String get_note() {
        return (String) this.getOrDefault(Return_Fields.note.name(), "");
    }

    public Object get_data() {
        return this.getOrDefault(Return_Fields.data.name(), "");
    }

    public String get_format() {
        return (String) this.getOrDefault(Return_Fields.format.name(), "");
    }

    public Object get_extent() {
        return this.getOrDefault(Return_Fields.extent.name(), "");
    }

    public int get_from() {
        return (Integer) this.getOrDefault(Return_Fields.from.name(), 0);
    }

    public int get_size() {
        return (Integer) this.getOrDefault(Return_Fields.size.name(), 0);
    }

    public int get_total() {
        return (Integer) this.getOrDefault(Return_Fields.total.name(), 0);
    }

    //////////////////////// @Override/////////////////////////////////////
    @Override
    public Return put(String key, Object value) {
        super.put(key, value);
        return this;
    }

    public Return add(String key, Object value) {
        super.put(key, value);
        return this;
    }

    public String toJson() {
        try {
            return JsonConvert.toJson(this);
        } catch (Exception e) {
            LOGGER.error("json 解析失败:", e);
            return JsonConvert.toJson(Return.FAIL(BasicCode.error));
        }
    }

    public Return note(String note) {
        super.put(Return_Fields.note.name(), note);
        return this;
    }
    public Return data(Object data) {
        super.put(Return_Fields.data.name(), data);
        return this;
    }
    public Return from(int from) {
        super.put(Return_Fields.from.name(), from);
        return this;
    }
    public Return size(int size) {
        super.put(Return_Fields.size.name(), size);
        return this;
    }
    public Return total(int total) {
        super.put(Return_Fields.total.name(), total);
        return this;
    }
    public Return extent(String extent) {
        super.put(Return_Fields.extent.name(), extent);
        return this;
    }
    public Return format(String format) {
        super.put(Return_Fields.format.name(), format);
        return this;
    }
    public Return trackid(String trackid) {
        super.put(Return_Fields.trackid.name(), trackid);
        return this;
    }



}
