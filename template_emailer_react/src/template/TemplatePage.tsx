import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import './TemplatePage.css';
import { config } from 'config/config';
import { TemplateInfo } from 'interface/TemplateInfo';
import { Page } from 'interface/Page';

const TemplatePage: React.FC = () => {
    const [templates, setTemplates] = useState<TemplateInfo[]>([]);
    const [currentPage, setCurrentPage] = useState(1);
    const [templatesPerPage, setTemplatesPerPage] = useState(10);
    const [totalPages, setTotalPages] = useState(0);
    const [filter, setFilter] = useState('');

    const navigate = useNavigate();

    useEffect(() => {
        const params = {
            page: currentPage - 1,
            size: templatesPerPage,
        };

        // Fetch template data from API
        fetch(`${config.apiUrl}/template/templatesList`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(params),
        })
            .then((res) => res.json())
            .then((data: Page<TemplateInfo>) => {
                setTemplates(data.content);
                setTotalPages(data.totalPages);
            })
            .catch((error) => console.error('Error fetching templates:', error));
    }, [currentPage, templatesPerPage, filter]);

    const handleEdit = (id: string, fileName: string) => {
        // Handle edit action
        console.log('Edit template with id:', id);
        navigate(`/template/${id}`, { state: { fileName } });
    };

    const handleDelete = (id: string) => {
        // 确认删除操作
        if (window.confirm('Are you sure you want to delete this template?')) {
            // 调用删除API
            fetch(`${config.apiUrl}/template/${id}`, {
                method: 'DELETE',
            })
                .then(response => {
                    if (!response.ok) {
                        throw new Error('Failed to delete template');
                    }
                    return response.text();
                })
                .then(result => {
                    console.log('Delete successful:', result);
                    alert('Template deleted successfully');
                    
                    // 刷新模板列表
                    const params = {
                        page: currentPage - 1,
                        size: templatesPerPage,
                        name: filter,
                    };
                    return fetch(`${config.apiUrl}/template/templatesList`, {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json',
                        },
                        body: JSON.stringify(params),
                    });
                })
                .then(response => response.json())
                .then((data: Page<TemplateInfo>) => {
                    setTemplates(data.content);
                    setTotalPages(data.totalPages);
                })
                .catch(error => {
                    console.error('Error deleting template:', error);
                    alert(`Failed to delete template: ${error.message}`);
                });
        }
    };

    const handleCreate = () => {
        // Handle create action
        console.log('Create template');
        navigate('/template/create');
    };

    const handleFilterChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        setFilter(e.target.value);
        setCurrentPage(1);
    };

    const paginate = (pageNumber: number) => setCurrentPage(pageNumber);

    return (
        <div className="template-page">
            <div className="header">
                <h1>Template</h1>
                <button className="create-button" onClick={handleCreate}>Create</button>
            </div>
            <div className="filter">
                <label>Filter by Name:</label>
                <input type="text" value={filter} onChange={handleFilterChange} />
            </div>
            <table className="template-table">
                <thead>
                    <tr>
                        <th>Name</th>
                        <th>Updated By</th>
                        <th>Updated Time</th>
                        <th>Actions</th>
                    </tr>
                </thead>
                <tbody>
                    {templates.map((template) => (
                        <tr key={template.id}>
                            <td>{template.filename}</td>
                            <td>{template.updateBy}</td>
                            <td>{new Date(template.updateTime).toLocaleString()}</td>
                            <td>
                                <button className="edit-button" onClick={() => handleEdit(template.id, template.filename)}>Edit</button>
                                <button className="delete-button" onClick={() => handleDelete(template.id)}>Delete</button>
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>
            <div className="pagination">
                <button onClick={() => paginate(currentPage - 1)} disabled={currentPage === 1}>Previous</button>
                {[...Array(totalPages).keys()].map((number) => (
                    <button key={number} onClick={() => paginate(number + 1)}>
                        {number + 1}
                    </button>
                ))}
                <button onClick={() => paginate(currentPage + 1)} disabled={currentPage === totalPages}>Next</button>
            </div>
            <div className="templates-per-page">
                <label>Templates per page:</label>
                <select value={templatesPerPage} onChange={(e) => setTemplatesPerPage(Number(e.target.value))}>
                    <option value={5}>5</option>
                    <option value={10}>10</option>
                    <option value={20}>20</option>
                </select>
            </div>
        </div>
    );
};

export default TemplatePage;
