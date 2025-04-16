import React, { useEffect, useRef, useState } from 'react';
import grapesjs from 'grapesjs';
import 'grapesjs/dist/css/grapes.min.css';
import './TemplateEdit.css';
import { config } from 'config/config';
import { useParams, useNavigate, useLocation } from 'react-router-dom';

const TemplateEdit: React.FC = () => {
    const editorRef = useRef<any>(null); // Store GrapesJS editor instance
    const editorInstance = useRef<any>(null);
    const { id } = useParams<{ id: string }>();
    const [fileName, setFileName] = useState('');
    const navigate = useNavigate();
    const location = useLocation();

    useEffect(() => {
        if (editorRef.current) {
            const editor = grapesjs.init({
                container: editorRef.current,
                fromElement: true,
                height: '100vh',
                width: 'auto',
                storageManager: false,
                plugins: [],
                pluginsOpts: {},
            });

            // Save editor instance
            editorRef.current = editor;

            addBlocks(editor);
        }
    }, []);

    // Load template data
    useEffect(() => {
        if (id && editorRef.current) {
            fetch(`${config.apiUrl}/template/load/${id}`)
                .then(response => response.json())
                .then(data => {
                    console.log('Data:', data);
                    const editor = editorRef.current;
                    editor.setComponents(data.html);
                    editor.setStyle(data.css);
                    if (location.state && location.state.fileName) {
                        setFileName(location.state.fileName);
                    }
                })
                .catch(error => console.error('Error:', error));
        }
    }, [id, location]);

    const handleBack = () => {
        const confirmDiscard = window.confirm('Are you sure you want to discard your changes?');
        if (confirmDiscard) {
            navigate(-1); // Go back to the previous page
        }
    };

    const handleSave = async () => {
        console.log('Filename:', fileName);
        if (!fileName) {
            alert('Please enter a file name.');
            return;
        }

        const editor = editorRef.current;
        const html = editor.getHtml() || '';
        const css = editor.getCss() || '';
        const jsonData = JSON.stringify({ html, css });
        const blob = new Blob([jsonData], { type: 'application/json' });
        const formData = new FormData();
        formData.append('file', blob, `${fileName}.json`);
        if (id) formData.append('objectId', id);

        try {
            const response = await fetch(`${config.apiUrl}/template/upload`, {
                method: 'POST',
                body: formData,
            });
            if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
            await response.text();
            alert('Save successful!');
            navigate(-1); // Go back to the previous page
        } catch (error) {
            alert('Save failed. Please try again.');
            console.error('Error:', error);
        }
    };

    // Add custom blocks to the editor
    const addBlocks = (editor: any) => {
        editor.BlockManager.add('text-block', {
            label: `
        <div style="text-align: center;">
          <i class="fa fa-font" style="font-size: 24px; display: block; margin-bottom: 5px;"></i>
          <span>Text Block</span>
        </div>
        `,
            content: { type: 'text', content: 'Text!' },
            category: 'Basic',
        });

        editor.BlockManager.add('image-block', {
            label: `
        <div style="text-align: center;">
          <i class="fa fa-image" style="font-size: 24px; display: block; margin-bottom: 5px;"></i>
          <span>Image Block</span>
        </div>
        `,
            content: { type: 'image', src: 'https://via.placeholder.com/150' },
            category: 'Basic',
        });

        editor.BlockManager.add('custom-block', {
            label: `
        <div style="text-align: center;">
          <i class="fa fa-cube" style="font-size: 24px; display: block; margin-bottom: 5px;"></i>
          <span>Custom Block</span>
        </div>
        `,
            content: `<div class="custom-block">Custom Block</div>`,
            category: 'Custom',
        });

        editor.BlockManager.add('one-column', {
            label: `
        <div style="text-align: center;">
          <i class="fa fa-square" style="font-size: 24px; display: block; margin-bottom: 5px;"></i>
          <span>One Column</span>
        </div>
        `,
            content: `
        <div class="one-column" data-gjs-droppable="true">
          <div class="column" style="min-height: 50px; border: 1px dashed #ccc; padding: 10px; position: relative;">
            <div class="placeholder" style="position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%); color: #aaa; pointer-events: none;">
          </div>
        </div>
        </div>
      `,
            category: 'Layout',
        });

        editor.BlockManager.add('two-column', {
            label: `
        <div style="text-align: center;">
          <i class="fa fa-columns" style="font-size: 24px; display: block; margin-bottom: 5px;"></i>
          <span>Two Columns</span>
        </div>`,
            content: `
        <div class="two-column" data-gjs-droppable="true">
          <div class="column" style="min-height: 50px; border: 1px dashed #ccc; padding: 10px;">
            <div class="placeholder" style="position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%); color: #aaa; pointer-events: none;"></div>
          </div>
          <div class="column" style="min-height: 50px; border: 1px dashed #ccc; padding: 10px;">
            <div class="placeholder" style="position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%); color: #aaa; pointer-events: none;"></div>
          </div>
        </div>`,
            category: 'Layout',
        });
    };

    // Add default styles
editor.CssComposer.addRules(`
    /* Custom Block */
    .custom-block {
      width: 50%;
      height: 200px;
      margin: 0 auto;
      text-align: center;
      background-color: #f0f0f0;
      line-height: 200px;
      border: 1px solid #ddd;
    }
    
    /* One Column */
    .one-column {
      display: flex;
      justify-content: center;
      align-items: center;
      width: 100%;
      background-color: #e0e0e0;
      border: 1px solid #ccc;
    }
    
    .one-column .column {
      width: 100%;
      text-align: center;
    }
    
    /* Two Columns */
    .two-column {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 10px;
      width: 100%;
      background-color: #e0e0e0;
      border: 1px solid #ccc;
    }
    
    .two-column .column {
      text-align: center;
      background-color: #f9f9f9;
      border: 1px dashed #ddd;
      padding: 10px;
    }
    
    .column:empty > .placeholder {
      display: block !important;
    }
    
    .column:not(:empty) > .placeholder {
      display: none !important;
    }
    
    .column:empty .placeholder::before {
      content: "Drag components here";
      position: absolute;
      top: 50%;
      left: 50%;
      transform: translate(-50%, -50%);
      color: #aaa;
      pointer-events: none;
    }
    `);
};  
    return (
        <div>
            <div className="toolbar">
                <div className="toolbar-left">
                    <label htmlFor="fileName">FileName:</label>
                    <input
                        id="fileName"
                        type="text"
                        value={fileName}
                        onChange={(e) => setFileName(e.target.value)}
                    />
                </div>
                <div className="toolbar-right">
                    <button onClick={handleBack}>Back</button>
                    <button onClick={handleSave}>Save</button>
                </div>
            </div>
            <div ref={editorRef} className="editor-container"></div>
        </div>
    );
};
};

export default TemplateEdit;
