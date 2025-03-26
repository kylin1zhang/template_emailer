package com.citi.custody.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

@Document(collection = "email")
public class Email {
    @Id
    private String id;
    private String emailName; // Subject Name
    private String contentTemplateId; // Reference to TemplateInfo's objectId
    private Date createTime;
    private Date modifiedTime;
    private Date sentTime;
    private String createdBy;
    private List<String> to;
    private List<String> cc;
    private String status; // DRAFT, SCHEDULED, SENT, FAILED
    private String errorMessage;
    private List<String> attachments;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        if (id != null && id.trim().isEmpty()) {
            this.id = null;
        } else {
            this.id = id;
        }
    }

    public String getEmailName() {
        return emailName;
    }

    public void setEmailName(String emailName) {
        this.emailName = emailName;
    }

    public String getContentTemplateId() {
        return contentTemplateId;
    }

    public void setContentTemplateId(String contentTemplateId) {
        this.contentTemplateId = contentTemplateId;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getModifiedTime() {
        return modifiedTime;
    }

    public void setModifiedTime(Date modifiedTime) {
        this.modifiedTime = modifiedTime;
    }

    public Date getSentTime() {
        return sentTime;
    }

    public void setSentTime(Date sentTime) {
        this.sentTime = sentTime;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public List<String> getTo() {
        return to;
    }

    public void setTo(List<String> to) {
        this.to = to;
    }

    public List<String> getCc() {
        return cc;
    }

    public void setCc(List<String> cc) {
        this.cc = cc;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public List<String> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<String> attachments) {
        this.attachments = attachments;
    }

    @Override
    public String toString() {
        return "Email{" +
                "id='" + id + '\'' +
                ", emailName='" + emailName + '\'' +
                ", contentTemplateId='" + contentTemplateId + '\'' +
                ", createTime=" + createTime +
                ", modifiedTime=" + modifiedTime +
                ", sentTime=" + sentTime +
                ", createdBy='" + createdBy + '\'' +
                ", to=" + to +
                ", cc=" + cc +
                ", status='" + status + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                ", attachments=" + attachments +
                '}';
    }
}
