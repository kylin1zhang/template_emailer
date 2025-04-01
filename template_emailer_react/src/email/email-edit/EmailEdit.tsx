// EmailEdit.tsx
import React, { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { config } from '../../config/config';
import './EmailEdit.css';
import { EmailInfo } from '../../interface/EmailInfo';
import { TemplateInfo } from '../../interface/TemplateInfo';

const EmailEditor = () => {
    const { id } = useParams();
    const navigate = useNavigate();
    const [emailInfo, setEmailInfo] = useState<EmailInfo | null>(null);
    const [templates, setTemplates] = useState<TemplateInfo[]>([]);
    const [isUploading, setIsUploading] = useState(false);
    const [uploadProgress, setUploadProgress] = useState(0);
    const [isTemplateLoading, setIsTemplateLoading] = useState(false);
    const fileInputRef = useRef<HTMLInputElement>(null);

    useEffect(() => {
        if (id) {
            fetch(`${config.apiUrl}/email/${id}`)
                .then(response => {
                    if (!response.ok) {
                        throw new Error('Failed to fetch email');
                    }
                    return response.json();
                })
                .then(data => {
                    if (!data) {
                        throw new Error('No data received');
                    }

                    if (data.sentTime) {
                        data.sentTime = new Date(data.sentTime);
                    }
                    if (data.createTime) {
                        data.createTime = new Date(data.createTime);
                    }
                    if (data.modifiedTime) {
                        data.modifiedTime = new Date(data.modifiedTime);
                    }

                    // Ensure attachments is an array
                    if (!data.attachments) {
                        data.attachments = [];
                    }

                    setEmailInfo(data);
                })
                .catch(error => {
                    console.error('Error fetching email:', error);
                    // Create a default empty email
                    setEmailInfo({
                        emailName: '',
                        createTime: null,
                        sentTime: null,
                        createdBy: '',
                        to: [],
                        cc: [],
                        contentTemplateId: '',
                        id: '',
                        modifiedTime: null,
                        status: 'DRAFT',
                        attachments: []
                    });
                });
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
                modifiedTime: null,
                status: 'DRAFT',
                attachments: []
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
            console.log('Fetching templates...');
            setIsTemplateLoading(true);
            // 使用模板列表API而不是按更新者筛选
            fetch(`${config.apiUrl}/template/templatesList`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    page: 0,
                    size: 100,
                    name: '',
                }),
            })
                .then(response => {
                    console.log('Template API response status:', response.status);
                    if (!response.ok) {
                        throw new Error(`Failed to fetch templates: ${response.status}`);
                    }
                    return response.json();
                })
                .then(data => {
                    console.log('Templates data received:', data);
                    // 根据API返回的格式提取模板列表
                    let templatesList: TemplateInfo[] = [];
                    if (data && data.content && Array.isArray(data.content)) {
                        templatesList = data.content;
                    } else if (Array.isArray(data)) {
                        templatesList = data;
                    }
                    setTemplates(templatesList);
                    console.log('Templates state set:', templatesList.length, 'templates');
                })
                .catch(error => {
                    console.error('Error fetching templates:', error);
                    setTemplates([]);
                    alert(`Error loading templates: ${error.message}`);
                })
                .finally(() => {
                    setIsTemplateLoading(false);
                });
        }
    };

    const handleSave = () => {
        if (!emailInfo) return;

        // Add frontend validation
        if (!emailInfo.emailName.trim()) {
            alert('Subject cannot be empty');
            return;
        }

        if (!emailInfo.to || emailInfo.to.length === 0 || !emailInfo.to[0]) {
            alert('Recipient(s) cannot be empty');
            return;
        }

        if (!emailInfo.contentTemplateId) {
            alert('Please select a template');
            return;
        }

        const url = `${config.apiUrl}/email/save`;
        const method = 'POST';

        // 让我们创建一个深拷贝而不仅仅是引用
        const emailToSave = JSON.parse(JSON.stringify(emailInfo));

        // 确保没有null值，替换为适当的默认值
        if (!emailToSave.id) emailToSave.id = '';
        if (!emailToSave.createTime) emailToSave.createTime = new Date();
        if (!emailToSave.modifiedTime) emailToSave.modifiedTime = new Date();
        if (!emailToSave.createdBy) emailToSave.createdBy = 'TEST';
        if (!emailToSave.status) emailToSave.status = 'DRAFT';
        if (emailToSave.sentTime && !(emailToSave.sentTime instanceof Date)) {
            emailToSave.sentTime = new Date(emailToSave.sentTime);
        }
        if (!Array.isArray(emailToSave.attachments)) emailToSave.attachments = [];

        console.log('Saving email data:', JSON.stringify(emailToSave, null, 2));

        // 定义一个递归函数用于重试
        const attemptSave = (retryCount = 0, maxRetries = 1) => {
            fetch(url, {
                method,
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(emailToSave),
            })
                .then(response => {
                    console.log('Save email response status:', response.status);
                    // Headers.entries 在某些 TypeScript 版本可能不支持
                    const headerObj: { [key: string]: string } = {};
                    response.headers.forEach((value, key) => {
                        headerObj[key] = value;
                    });
                    console.log('Response headers:', headerObj);

                    if (!response.ok) {
                        throw new Error(`Save failed: ${response.status} ${response.statusText}`);
                    }
                    return response.text();
                })
                .then(text => {
                    console.log('Raw response content:', text);

                    // Check if response is empty
                    if (!text || text.trim() === '') {
                        console.error('Received empty response');
                        throw new Error('Server returned empty response');
                    }

                    try {
                        // Try different approaches to extract the ID
                        // 1. Try parsing as JSON
                        if (text.startsWith('{')) {
                            const jsonObj = JSON.parse(text);
                            if (jsonObj && (jsonObj.id || jsonObj._id)) {
                                return jsonObj.id || jsonObj._id;
                            }
                        }

                        // 2. Check if it's a plain string ID (MongoDB ObjectId is 24 chars)
                        if (text.trim().match(/^[a-f0-9]{24}$/i)) {
                            return text.trim();
                        }

                        // 3. Check if it's a quoted string like "60c73c2f7071b02e28a57b5d"
                        if (text.trim().match(/^"[a-f0-9]{24}"$/i)) {
                            return text.trim().replace(/^"|"$/g, '');
                        }

                        // 4. If the text is short and doesn't contain spaces, try using it
                        if (text.length < 100 && !text.includes(' ')) {
                            return text.trim();
                        }

                        console.error('Could not extract valid ID from response:', text);
                        throw new Error('Server response format not recognized');
                    } catch (e: any) {
                        console.error('Response parsing error:', e);
                        // Last resort - try to use raw text directly if it's short
                        if (text && text.trim() && !text.includes(' ') && text.length < 100) {
                            return text.trim();
                        }
                        throw new Error(`Response parsing failed: ${e.message}`);
                    }
                })
                .then(id => {
                    if (id) {
                        console.log('Final ID:', id);

                        // Update the email ID in state
                        setEmailInfo(prev => {
                            if (prev) {
                                return { ...prev, id: id.toString() };
                            }
                            return prev;
                        });

                        // Navigate and show success message
                        navigate(`/email/${id}`);
                        alert('Email saved successfully');
                        return;
                    }
                    throw new Error('Server did not return a valid ID');
                })
                .catch(error => {
                    console.error('Error saving email:', error);

                    // 尝试重试
                    if (retryCount < maxRetries) {
                        console.log(`Retrying save attempt ${retryCount + 1} of ${maxRetries}...`);
                        setTimeout(() => attemptSave(retryCount + 1, maxRetries), 1000);
                    } else {
                        // 所有重试都失败了，显示错误消息
                        const errorDetails = `
Failed to save email: ${error.message}
Please check if:
1. The backend server is running
2. MongoDB is running and accessible
3. The template exists and is valid
4. Network connectivity is stable

Technical details:
${error.stack || '(No stack trace available)'}
                        `;
                        console.error(errorDetails);
                        alert(`Failed to save email after ${maxRetries + 1} attempts. Error: ${error.message}`);

                        // 为了调试，我们将错误信息写入页面的隐藏元素中
                        const debugElement = document.createElement('div');
                        debugElement.style.display = 'none';
                        debugElement.id = 'save-error-details';
                        debugElement.innerText = errorDetails;
                        document.body.appendChild(debugElement);
                    }
                });
        };

        // 开始第一次尝试
        attemptSave();
    };

    const handleSend = () => {
        if (!emailInfo) return;

        // Add frontend validation
        if (!emailInfo.emailName.trim()) {
            alert('Subject cannot be empty');
            return;
        }

        if (!emailInfo.to || emailInfo.to.length === 0 || !emailInfo.to[0]) {
            alert('Recipient(s) cannot be empty');
            return;
        }

        if (!emailInfo.contentTemplateId) {
            alert('Please select a template');
            return;
        }

        // First save the email
        const saveUrl = `${config.apiUrl}/email/save`;

        // 让我们创建一个深拷贝而不仅仅是引用
        const emailToSave = JSON.parse(JSON.stringify(emailInfo));

        // 确保没有null值，替换为适当的默认值
        if (!emailToSave.id) emailToSave.id = '';
        if (!emailToSave.createTime) emailToSave.createTime = new Date();
        if (!emailToSave.modifiedTime) emailToSave.modifiedTime = new Date();
        if (!emailToSave.createdBy) emailToSave.createdBy = 'TEST';
        if (!emailToSave.status) emailToSave.status = 'DRAFT';
        if (!Array.isArray(emailToSave.attachments)) emailToSave.attachments = [];

        console.log('Saving email data before sending:', JSON.stringify(emailToSave, null, 2));

        // 定义一个递归函数用于重试保存
        const attemptSaveAndSend = (retryCount = 0, maxRetries = 1) => {
            fetch(saveUrl, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(emailToSave),
            })
                .then(response => {
                    console.log('Save email response status:', response.status);
                    // Headers.entries 在某些 TypeScript 版本可能不支持
                    const headerObj: { [key: string]: string } = {};
                    response.headers.forEach((value, key) => {
                        headerObj[key] = value;
                    });
                    console.log('Response headers:', headerObj);

                    if (!response.ok) {
                        throw new Error(`Save failed: ${response.status} ${response.statusText}`);
                    }
                    return response.text();
                })
                .then(text => {
                    console.log('Raw response content:', text);

                    // Check if response is empty
                    if (!text || text.trim() === '') {
                        console.error('Received empty response');
                        throw new Error('Server returned empty response');
                    }

                    try {
                        // Try different approaches to extract the ID
                        // 1. Try parsing as JSON
                        if (text.startsWith('{')) {
                            const jsonObj = JSON.parse(text);
                            if (jsonObj && (jsonObj.id || jsonObj._id)) {
                                return jsonObj.id || jsonObj._id;
                            }
                        }

                        // 2. Check if it's a plain string ID (MongoDB ObjectId is 24 chars)
                        if (text.trim().match(/^[a-f0-9]{24}$/i)) {
                            return text.trim();
                        }

                        // 3. Check if it's a quoted string like "60c73c2f7071b02e28a57b5d"
                        if (text.trim().match(/^"[a-f0-9]{24}"$/i)) {
                            return text.trim().replace(/^"|"$/g, '');
                        }

                        // 4. If the text is short and doesn't contain spaces, try using it
                        if (text.length < 100 && !text.includes(' ')) {
                            return text.trim();
                        }

                        console.error('Could not extract valid ID from response:', text);
                        throw new Error('Server response format not recognized');
                    } catch (e: any) {
                        console.error('Response parsing error:', e);
                        // Last resort - try to use raw text directly if it's short
                        if (text && text.trim() && !text.includes(' ') && text.length < 100) {
                            return text.trim();
                        }
                        throw new Error(`Response parsing failed: ${e.message}`);
                    }
                })
                .then(id => {
                    if (id) {
                        // Update the email ID in state
                        setEmailInfo(prev => {
                            if (prev) {
                                return { ...prev, id: id.toString() };
                            }
                            return prev;
                        });

                        // 开始尝试发送邮件
                        console.log('Starting to send email, ID:', id);
                        const sendAttempt = (sendRetryCount = 0, sendMaxRetries = 1) => {
                            const sendUrl = `${config.apiUrl}/email/send/${id}`;
                            return fetch(sendUrl, {
                                method: 'POST',
                            })
                                .then(response => {
                                    console.log('Send email response status:', response.status);
                                    if (!response.ok) {
                                        throw new Error(`Send failed: ${response.status} ${response.statusText}`);
                                    }
                                    return response.text();
                                })
                                .then(result => {
                                    console.log('Send result:', result);
                                    alert(result || 'Email sent successfully');
                                    navigate('/email');
                                })
                                .catch(error => {
                                    console.error('Error sending email:', error);

                                    if (sendRetryCount < sendMaxRetries) {
                                        console.log(`Retrying send attempt ${sendRetryCount + 1} of ${sendMaxRetries}...`);
                                        setTimeout(() => sendAttempt(sendRetryCount + 1, sendMaxRetries), 1000);
                                    } else {
                                        alert(`Failed to send email after ${sendMaxRetries + 1} attempts: ${error.message}`);
                                    }
                                });
                        };

                        // 开始第一次发送尝试
                        return sendAttempt();
                    }
                    throw new Error('Server did not return a valid ID');
                })
                .catch(error => {
                    console.error('Error in save and send process:', error);

                    // 尝试重试
                    if (retryCount < maxRetries) {
                        console.log(`Retrying save attempt ${retryCount + 1} of ${maxRetries}...`);
                        setTimeout(() => attemptSaveAndSend(retryCount + 1, maxRetries), 1000);
                    } else {
                        // 所有重试都失败了，显示错误消息
                        const errorDetails = `
Failed to save email: ${error.message}
Please check if:
1. The backend server is running
2. MongoDB is running and accessible
3. The template exists and is valid
4. Network connectivity is stable

Technical details:
${error.stack || '(No stack trace available)'}
                        `;
                        console.error(errorDetails);
                        alert(`Failed to save or send email after ${maxRetries + 1} attempts. Error: ${error.message}`);

                        // 为了调试，我们将错误信息写入页面的隐藏元素中
                        const debugElement = document.createElement('div');
                        debugElement.style.display = 'none';
                        debugElement.id = 'send-error-details';
                        debugElement.innerText = errorDetails;
                        document.body.appendChild(debugElement);
                    }
                });
        };

        // 开始第一次尝试
        attemptSaveAndSend();
    };

    const handleTemplateSelect = (templateId: string) => {
        if (!emailInfo || !templateId) return;

        try {
            setEmailInfo({ ...emailInfo, contentTemplateId: templateId });
            console.log('Template selected:', templateId);
        } catch (error) {
            console.error('Error selecting template:', error);
            alert('Failed to select template. Please try again.');
        }
    };

    const handleTemplateRemove = () => {
        if (!emailInfo) return;

        try {
            setEmailInfo({ ...emailInfo, contentTemplateId: '' });
            console.log('Template removed');
        } catch (error) {
            console.error('Error removing template:', error);
            alert('Failed to remove template. Please try again.');
        }
    };

    const handleFileSelect = () => {
        if (fileInputRef.current) {
            fileInputRef.current.click();
        }
    };

    const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        if (!e.target.files || !emailInfo) return;

        const files = Array.from(e.target.files);
        if (files.length === 0) return;

        setIsUploading(true);
        setUploadProgress(0);

        const formData = new FormData();
        files.forEach(file => {
            formData.append('files', file);
        });

        // Upload files
        fetch(`${config.apiUrl}/email/upload-attachment`, {
            method: 'POST',
            body: formData,
        })
            .then(response => {
                if (!response.ok) {
                    throw new Error('File upload failed');
                }
                return response.json();
            })
            .then((filenames: any) => {
                // Update email with new attachments
                if (Array.isArray(filenames)) {
                    const updatedAttachments = [...(emailInfo.attachments || []), ...filenames];
                    setEmailInfo({ ...emailInfo, attachments: updatedAttachments });
                } else {
                    console.error('Invalid response format for uploaded files:', filenames);
                }
                setIsUploading(false);
                // Reset file input
                if (fileInputRef.current) {
                    fileInputRef.current.value = '';
                }
            })
            .catch(error => {
                console.error('Error uploading attachments:', error);
                alert(`Error uploading attachments: ${error.message}`);
                setIsUploading(false);
            });
    };

    const handleRemoveAttachment = (index: number) => {
        if (!emailInfo || !emailInfo.attachments) return;

        const updatedAttachments = [...emailInfo.attachments];
        updatedAttachments.splice(index, 1);
        setEmailInfo({ ...emailInfo, attachments: updatedAttachments });
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
                    value={
                        emailInfo.createTime
                            ? new Date(emailInfo.createTime.getTime() - new Date().getTimezoneOffset() * 60000)
                                .toISOString()
                                .slice(0, 16)
                            : ''
                    }
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
                    value={
                        emailInfo.sentTime
                            ? new Date(emailInfo.sentTime.getTime() - new Date().getTimezoneOffset() * 60000)
                                .toISOString()
                                .slice(0, 16)
                            : ''
                    }
                    onChange={(e) => {
                        const newSentTime = new Date(e.target.value);
                        console.log('New Sent Time:', newSentTime); // 调试日志
                        setEmailInfo({ ...emailInfo, sentTime: newSentTime });
                    }}
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
                    {isTemplateLoading ? (
                        <div>Loading templates...</div>
                    ) : emailInfo.contentTemplateId ? (
                        <div className="selected-template">
                            <span>
                                {Array.isArray(templates) && templates.length > 0
                                    ? templates.find((t) => t && t.id === emailInfo.contentTemplateId)?.filename || 'Template not found'
                                    : 'Template not found'}
                            </span>
                            <button onClick={handleTemplateRemove}>Remove</button>
                        </div>
                    ) : (
                        <div style={{ width: '100%' }}>
                            <select
                                onChange={(e) => e.target.value && handleTemplateSelect(e.target.value)}
                                value={emailInfo.contentTemplateId || ''}
                                style={{ width: '100%' }}
                            >
                                <option value="">Select a template</option>
                                {Array.isArray(templates) && templates.length > 0 ? (
                                    templates.map((t) => (
                                        t && t.id ? <option key={t.id} value={t.id}>{t.filename || 'Unnamed template'}</option> : null
                                    ))
                                ) : (
                                    <option value="" disabled>No templates available</option>
                                )}
                            </select>
                            {templates.length === 0 && !isTemplateLoading && (
                                <div className="template-warning">No templates found. Please create a template first.</div>
                            )}
                        </div>
                    )}
                </div>
            </div>
            <div className="editor-section">
                <label>Attachments</label>
                <div className="attachment-section">
                    <input
                        type="file"
                        ref={fileInputRef}
                        onChange={handleFileChange}
                        multiple
                        style={{ display: 'none' }}
                    />
                    <button onClick={handleFileSelect} disabled={isUploading}>
                        {isUploading ? 'Uploading...' : 'Add Attachments'}
                    </button>

                    {isUploading && (
                        <div className="upload-progress">
                            <div className="progress-bar" style={{ width: `${uploadProgress}%` }}></div>
                        </div>
                    )}

                    {emailInfo.attachments && emailInfo.attachments.length > 0 && (
                        <div className="attachments-list">
                            {emailInfo.attachments.map((attachment, index) => (
                                <div key={index} className="attachment-item">
                                    <span>{attachment}</span>
                                    <button onClick={() => handleRemoveAttachment(index)}>Remove</button>
                                </div>
                            ))}
                        </div>
                    )}
                </div>
            </div>
            <div className="button-group">
                <button onClick={() => navigate(-1)}>Back</button>
                <div style={{ flexGrow: 1 }}></div>
                <button onClick={handleSave}>Save</button>
                <button onClick={handleSend} className="send-button">Send</button>
            </div>
        </div>
    );
};

export default EmailEditor;
