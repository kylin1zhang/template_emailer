import React, { useEffect, useRef, useState } from 'react';
import grapesjs from 'grapesjs';
import 'grapesjs/dist/css/grapes.min.css';
import './TemplateEdit.css';
import { config } from 'config/config';
import { fetchRodData } from './rodUtils';
import { useParams, useNavigate, useLocation } from 'react-router-dom';
import plugin from 'grapesjs-blocks-basic';

const TemplateEdit: React.FC = () => {
  const editorRef = useRef<HTMLDivElement>(null);
  const editorInstance = useRef<any>(null); // Store GrapesJS editor instance
  const { id } = useParams<{ id: string }>();
  const [fileName, setFileName] = useState('');
  const navigate = useNavigate();
  const location = useLocation();
  const [releaseId, setRodData] = useState<any[]>([]); // Store ROD data


  useEffect(() => {
    if (editorRef.current) {
      const editor = grapesjs.init({
        container: editorRef.current,
        fromElement: true,
        height: "100vh",
        width: 'auto',
        storageManager: false,
        plugins: [
          editor => plugin(editor, { blocks: ['column1', 'column2', 'column3', 'column3-7', 'text', 'link', 'image', 'video', 'map'] }),
        ],
        pluginsOpts: {

        },
      });


      editorInstance.current = editor;

      addBlocks(editor);
    }
  }, []);

  // Load template data
  useEffect(() => {
    if (id && editorInstance.current) {
      fetch(`${config.apiUrl}/template/load/${id}`)
        .then(response => response.json())
        .then(data => {
          console.log('Data:', data);
          const editor = editorInstance.current;
          editor.setComponents(data.html);
          editor.setStyle(data.css);
          if (location.state && location.state.fileName) {
            setFileName(location.state.fileName);
          }
        })
        .catch(error => console.error('Error:', error));
    }
  }, [id, location.state]);

  const handleBack = () => {
    const confirmDiscard = window.confirm('Are you sure you want to discard your changes?');
    if (confirmDiscard) {
      navigate(-1); // Go back to the previous page
    }
  };

  const handleSave = async () => {
    console.log('FileName:', fileName);
    if (!fileName) {
      alert('Please enter a file name.');
      return;
    }

    const editor = editorInstance.current;
    const html = editor.getHtml();
    const css = editor.getCss();
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



  // Fetch ROD data on page load
  useEffect(() => {
    const loadRodData = async () => {
      const data = await fetchRodData(releaseId);
      setRodData(data);
    };

    loadRodData();
  }, []);


  // Get ROD block content
  const getRodBlockContent = () => {
    const cachedData = localStorage.getItem('rodApiData');
    let parsedData = [];

    if (cachedData) {
      try {
        parsedData = JSON.parse(cachedData);
      } catch (error) {
        console.error('Error parsing cached data:', error);
      }
    }


    const dataHtml = `
    <table style="width: 100%; border-collapse: collapse; margin-top: 20px; border: 1px solid black;">
      <thead>
        <tr style="background-color: #007bff; font-family: Arial; text-align: left; border: 1px solid black;">
          <th style="padding: 8px; border: 1px solid black;">Start Date</th>
          <th style="padding: 8px; border: 1px solid black;">Pipelines</th>
        </tr>
      </thead>
      <tbody>
        ${parsedData.length > 0
        ? parsedData
          .map(
            (item: any) => `
                <tr style="border: 1px solid black;">
                  <td style="padding: 8px; font-family: Arial; border: 1px solid black;">${item.startDateTimeIso}</td>
                  <td style="padding: 8px; border: 1px solid black;">
                    <table style="width: 100%; border-collapse: collapse; border: 1px solid black;">
                      <thead>
                        <tr style="background-color: #007bff; color: white; font-family: Arial; border: 1px solid black;">
                          <th style="padding: 4px; border: 1px solid black;">Project</th>
                          <th style="padding: 4px; border: 1px solid black;">Branch</th>
                          <th style="padding: 4px; border: 1px solid black;">Version</th>
                        </tr>
                      </thead>
                      <tbody>
                        ${item.pipelines
                .map(
                  (pipeline: any) => `
                            <tr style="border: 1px solid black;">
                              <td style="padding: 4px; font-family: Arial; border: 1px solid black;">${pipeline.name}</td>
                              <td style="padding: 4px; font-family: Arial; border: 1px solid black;">${pipeline.branch}</td>
                              <td style="padding: 4px; font-family: Arial; border: 1px solid black;">${pipeline.version}</td>
                            </tr>
                          `
                )
                .join('')
              }
                      </tbody>
                    </table>
                  </td>
                </tr>
              `
          )
          .join('')
        : `<tr><td colspan="2" style="text-align: center; padding: 8px; border: 1px solid black;">No data available</td></tr>`
      }
      </tbody>
    </table>
    `;

    return `
      <div style="text-align: center;">
        <div style="margin-top: 20px; text-align: left;">
          ${dataHtml}
        </div>
      </div>
    `;
  };

// Add custom blocks to the editor
const addBlocks = (editor: any) => {
  // Add ROD block
  editor.BlockManager.add('ROD-block', {
      //label: 'ROD Block',
      label: `
          <div class="gjs-block__media">
              <svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg" style="width: 100%; height: 100%;">
                  <rect x="3" y="4" width="18" height="2" fill="currentColor"></rect>
                  <rect x="3" y="8" width="18" height="2" fill="currentColor"></rect>
                  <rect x="3" y="12" width="18" height="2" fill="currentColor"></rect>
                  <rect x="3" y="16" width="18" height="2" fill="currentColor"></rect>
              </svg>
          </div>
          <div class="gjs-block-label">ROD Table</div>
      `,
      content: getRodBlockContent(),
      category: 'Basic',
      //attributes: { class: 'fa fa-cube' }, // Icon for the block
  });

  // Add default styles
  editor.CssComposer.addRules(
      `.rod-block {
          width: auto; /* 表格宽度根据内容调整 */
          margin-left: 0; /* 表格站近画布左边 */
          text-align: center;
          background-color: #f0f0f0;
          border: 1px solid #ddd;
          table-layout: auto; /* 列宽根据内容调整 */
      }`
  );
};

  return (
      <div>
          <div className="toolbar">
              <div className="toolbar-left">
                  <label htmlFor="fileName">fileName:</label>
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

export default TemplateEdit;
