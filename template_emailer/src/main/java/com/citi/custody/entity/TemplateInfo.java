package com.citi.custody.entity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class TemplateInfo {
    private String id;
    private String filename;
    private String updateBy;
    private Date updateTime;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getFormattedUpdateTime(Date updateTime) {
        if (updateTime == null) {
            return "";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        return sdf.format(updateTime);
    }

    @Override
    public String toString() {
        return "TemplateInfo{" +
                "id='" + id + '\'' +
                ", filename='" + filename + '\'' +
                ", updateBy='" + updateBy + '\'' +
                ", updateTime=" + updateTime +
                '}';
    }
}
