// EmailEdit.tsx
import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { config } from 'config/config';
import './EmailEdit.css';
import { EmailInfo } from 'interface/EmailInfo';
import { TemplateInfo } from 'interface/TemplateInfo';

const EmailEditor = () => {
    const { id } = useParams();
    const navigate = useNavigate();
    const [emailInfo, setEmailInfo] = useState<EmailInfo | null>(null);
    const [templates, setTemplates] = useState<TemplateInfo[]>([]);

    useEffect(() => {
        if (id) {
            fetch(`${config.apiUrl}/email/${id}`)
                .then(response => response.json())
                .then(data => {
                    if (data.sentTime) {
                        data.sentTime = new Date(data.sentTime);
                    }
                    if (data.createTime) {
                        data.createTime = new Date(data.createTime);
                    }
                    if (data.modifiedTime) {
                        data.modifiedTime = new Date(data.modifiedTime);
                    }

                    setEmailInfo(data);
                })
                .catch(error => console.error('Error fetching email:', error));
        } else {
            setEmailInfo({
                emailName: '',
                createTime: null,
                sentTime: null,
                createdBy: '',
                to: [],
                cc: [],
                contentTemplateId: '',
                id: '',
                modifiedTime: null
            });
        }
    }, [id]);

    useEffect(() => {
        if (emailInfo) {
            fetchTemplates();
        }
    }, [emailInfo]);

    const fetchTemplates = () => {
        if (emailInfo) {
            fetch(`${config.apiUrl}/template/updateBy/TEST`)
                .then(response => response.json())
                .then(data => {
                    setTemplates(data);
                })
                .catch(error => console.error('Error fetching templates:', error));
        }
    };

    const handleSave = () => {
        const url = `${config.apiUrl}/email/save`;
        const method = 'POST';

        fetch(url, {
            method,
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(emailInfo),
        })
            .then(response => response.json())
            .then(id => {
                if (id) {
                    navigate(`/email/${id}`);
                    alert('Email saved successfully.');
                }
            })
            .catch(error => console.error('Error saving email:', error));
    };

    const handleTemplateSelect = (templateId: string) => {
        if (emailInfo) {
            setEmailInfo({ ...emailInfo, contentTemplateId: templateId });
        }
    };

    const handleTemplateRemove = () => {
        if (emailInfo) {
            setEmailInfo({ ...emailInfo, contentTemplateId: '' });
        }
    };

    if (!emailInfo) {
        return <div>Loading...</div>;
    }

    return (
        <div className="email-editor">
            <h1>Email</h1>
            <div className="editor-section">
                <label>Subject</label>
                <input
                    type="text"
                    value={emailInfo.emailName}
                    onChange={(e) => setEmailInfo({ ...emailInfo, emailName: e.target.value })}
                />
            </div>
            <div className="editor-section">
                <label>Create Time</label>
                <input
                    type="datetime-local"
                    value={emailInfo.createTime ? emailInfo.createTime.toISOString().slice(0, 16) : ''}
                    onChange={(e) => setEmailInfo({ ...emailInfo, createTime: new Date(e.target.value) })}
                    disabled
                    className="disabled-input"
                />
            </div>
            <div className="editor-section">
                <label>Created By</label>
                <input
                    type="text"
                    value={emailInfo.createdBy}
                    onChange={(e) => setEmailInfo({ ...emailInfo, createdBy: e.target.value })}
                    disabled
                    className="disabled-input"
                />
            </div>
            <div className="editor-section">
                <label>Sent Time</label>
                <input
                    type="datetime-local"
                    value={emailInfo.sentTime ? emailInfo.sentTime.toISOString().slice(0, 16) : ''}
                    onChange={(e) => setEmailInfo({ ...emailInfo, sentTime: new Date(e.target.value) })}
                />
            </div>
            <div className="editor-section">
                <label>To List</label>
                <input
                    type="text"
                    value={emailInfo.to.length > 0 ? emailInfo.to.join(', ') : ''}
                    onChange={(e) => setEmailInfo({ ...emailInfo, to: e.target.value.split(', ') })}
                />
            </div>
            <div className="editor-section">
                <label>CC List</label>
                <input
                    type="text"
                    value={emailInfo.cc.length > 0 ? emailInfo.cc.join(', ') : ''}
                    onChange={(e) => setEmailInfo({ ...emailInfo, cc: e.target.value.split(', ') })}
                />
            </div>
            <div className="editor-section">
                <label>Template</label>
                <div className="template-preview">
                    {emailInfo.contentTemplateId ? (
                        <div className="selected-template">
                            <span>{templates.find((t) => t.id === emailInfo.contentTemplateId)?.filename}</span>
                            <button onClick={handleTemplateRemove}>Remove</button>
                        </div>
                    ) : (
                        <select onChange={(e) => handleTemplateSelect(e.target.value)}>
                            <option value="">Select a template</option>
                            {templates.map((t) => (
                                <option key={t.id} value={t.id}>{t.filename}</option>
                            ))}
                        </select>
                    )}
                </div>
            </div>
            <div className="button-group">
                <button onClick={() => navigate(-1)}>Back</button>
                <button onClick={handleSave}>Save</button>
            </div>
        </div>
    );
};

export default EmailEditor;
