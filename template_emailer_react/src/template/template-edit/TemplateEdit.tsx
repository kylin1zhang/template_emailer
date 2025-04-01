import React, { useState, useEffect } from 'react';
import EmailEditor, { EditorRef, EmailEditorProps } from 'react-email-editor';
import { config } from 'config/config';
import { useParams, useNavigate, useLocation } from 'react-router-dom';
import './TemplateEdit.css';

const TemplateEdit: React.FC = () => {
    const emailEditorRef = React.useRef<EditorRef | null>(null);
    const [preview, setPreview] = useState(false);
    const { id } = useParams<{ id: string }>();
    const [fileName, setFileName] = useState('');
    const location = useLocation();
    const navigate = useNavigate();

    useEffect(() => {
        if (location.state && location.state.fileName) {
            setFileName(location.state.fileName);
        }
    }, [location.state]);

    const saveDesign = () => {
        if (!fileName) {
            alert('Please enter a file name.');
            return;
        }

        const unlayer = emailEditorRef.current?.editor;

        unlayer?.saveDesign((design) => {
            const formData = new FormData();
            const blob = new Blob([JSON.stringify(design)], { type: 'application/json' });
            formData.append('file', blob, `${fileName}.json`);
            if (id) {
                formData.append('objectId', id);
            }

            fetch(`${config.apiUrl}/template/upload`, {
                method: 'POST',
                body: formData,
            })
                .then((response) => response.text())
                .then((data) => {
                    console.log(data);
                    alert(data);
                })
                .catch((error) => console.error('Error:', error));
        });
    };

    const exportHtml = () => {
        const unlayer = emailEditorRef.current?.editor;

        unlayer?.exportHtml((data) => {
            const html = data.html;
            console.log('exportHtml', html);
            alert('Output HTML has been logged in your developer console.');
        });
    };

    const togglePreview = () => {
        const unlayer = emailEditorRef.current?.editor;

        if (preview) {
            unlayer?.hidePreview();
            setPreview(false);
        } else {
            unlayer?.showPreview({ device: 'desktop' });
            setPreview(!preview);
        }
    };

    const onDesignLoad = async () => {
        const unlayer = emailEditorRef.current?.editor;

        if (id) {
            console.log('id', id);
            try {
                const response = await fetch(`${config.apiUrl}/template/load/${id}`);
                const text = await response.text();
                const jsonData = JSON.parse(text);

                unlayer?.loadDesign(jsonData);
            } catch (error) {
                console.error('Error:', error);
            }
        }
    };

    const onLoad: EmailEditorProps['onLoad'] = (unlayer) => {
        console.log('onload', unlayer);
        onDesignLoad();
    };

    const onReady: EmailEditorProps['onReady'] = (unlayer) => {
        console.log('onReady', unlayer);
    };

    const handleBack = () => {
        navigate('/template');
    };

    return (
        <div className="full-height-wrapper">
            <div className="container">
                <div className="bar">
                    <div className="left-section">
                        <label htmlFor="filename-input" className="filename-label">Filename</label>
                        <input
                            type="text"
                            id="filename-input"
                            className="filename-input"
                            placeholder="Enter filename"
                            value={fileName}
                            onChange={(e) => setFileName(e.target.value)}
                        />
                    </div>
                    <div className="right-section">
                        <button className="action-button" onClick={handleBack}>Back</button>
                        <button className="action-button" onClick={togglePreview}>
                            {preview ? 'Hide' : 'Show'} Preview
                        </button>
                        <button className="action-button" onClick={saveDesign}>Save Design</button>
                        <button className="action-button" onClick={exportHtml}>Export HTML</button>
                    </div>
                </div>
                <React.StrictMode>
                    <EmailEditor
                        ref={emailEditorRef}
                        onLoad={onLoad}
                        onReady={onReady}
                        options={{
                            version: "latest",
                            appearance: {
                                theme: "modern_light"
                            },
                            features: {
                                columnEditor: {
                                    enabled: true,
                                    layouts: [
                                        { name: "1:1", layout: [1, 1] },
                                        { name: "1:1:1", layout: [1, 1, 1] },
                                        { name: "1:1:1:1", layout: [1, 1, 1, 1] },
                                        { name: "1:2", layout: [1, 2] },
                                        { name: "2:1", layout: [2, 1] },
                                        { name: "1:2:1", layout: [1, 2, 1] },
                                        { name: "2:1:2", layout: [2, 1, 2] }
                                    ]
                                }
                            } as any
                        }}
                    />
                </React.StrictMode>
            </div>
        </div>
    );
};

export default TemplateEdit;
