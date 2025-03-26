import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import './EmailPage.css'; // Ensure the correct path to the CSS file
import { config } from '../config/config';
import { EmailInfo } from '../interface/EmailInfo';
import { Page } from '../interface/Page';

const EmailPage: React.FC = () => {
    const [emails, setEmails] = useState<EmailInfo[]>([]);
    const [currentPage, setCurrentPage] = useState(1);
    const [emailsPerPage, setEmailsPerPage] = useState(10);
    const [totalPages, setTotalPages] = useState(0);
    const [filter, setFilter] = useState('');
    const navigate = useNavigate();

    useEffect(() => {
        const params = {
            page: currentPage - 1,
            size: emailsPerPage,
            filter: filter
        };

        fetch(`${config.apiUrl}/email/emailsList`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(params),
        })
            .then(response => response.json())
            .then((data: Page<EmailInfo>) => {
                if (data && Array.isArray(data.content)) {
                    setEmails(data.content);
                    setTotalPages(data.totalPages || 0);
                } else {
                    console.error('Invalid data format received:', data);
                    setEmails([]);
                    setTotalPages(0);
                }
            })
            .catch(error => {
                console.error('Error fetching emails:', error);
                setEmails([]);
                setTotalPages(0);
            });
    }, [currentPage, emailsPerPage, filter]);

    const handleEdit = (id: string, email: string) => {
        navigate(`/email/${id}`, { state: { email } });
    };

    const handleDelete = (id: string) => {
        if (window.confirm('Are you sure you want to delete this email?')) {
            fetch(`${config.apiUrl}/email/${id}`, {
                method: 'DELETE',
            })
                .then(response => {
                    if (!response.ok) {
                        throw new Error('Failed to delete email');
                    }
                    return response.text();
                })
                .then(result => {
                    alert(result);
                    // 刷新邮件列表
                    const params = {
                        page: currentPage - 1,
                        size: emailsPerPage,
                        filter: filter
                    };
                    return fetch(`${config.apiUrl}/email/emailsList`, {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json',
                        },
                        body: JSON.stringify(params),
                    });
                })
                .then(response => response.json())
                .then((data: Page<EmailInfo>) => {
                    if (data && Array.isArray(data.content)) {
                        setEmails(data.content);
                        setTotalPages(data.totalPages || 0);
                    } else {
                        console.error('Invalid data format received:', data);
                        setEmails([]);
                        setTotalPages(0);
                    }
                })
                .catch(error => {
                    console.error('Error deleting email:', error);
                    alert(`Error deleting email: ${error.message}`);
                });
        }
    };

    const handleCreate = () => {
        navigate('/email/create');
    };

    const handleFilterChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        setFilter(e.target.value);
        setCurrentPage(1);
    };

    const paginate = (pageNumber: number) => setCurrentPage(pageNumber);

    const handleSend = (id: string) => {
        if (window.confirm('Are you sure you want to send this email now?')) {
            fetch(`${config.apiUrl}/email/send/${id}`, {
                method: 'POST',
            })
                .then(response => {
                    if (!response.ok) {
                        throw new Error('Failed to send email');
                    }
                    return response.text();
                })
                .then(result => {
                    alert(result);
                    // Refresh the email list
                    const params = {
                        page: currentPage - 1,
                        size: emailsPerPage,
                        filter: filter
                    };
                    return fetch(`${config.apiUrl}/email/emailsList`, {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json',
                        },
                        body: JSON.stringify(params),
                    });
                })
                .then(response => response.json())
                .then((data: Page<EmailInfo>) => {
                    if (data && Array.isArray(data.content)) {
                        setEmails(data.content);
                        setTotalPages(data.totalPages || 0);
                    } else {
                        console.error('Invalid data format received:', data);
                        setEmails([]);
                        setTotalPages(0);
                    }
                })
                .catch(error => {
                    console.error('Error sending email:', error);
                    alert(`Error sending email: ${error.message}`);
                });
        }
    };

    const handleRetry = (id: string) => {
        if (window.confirm('Do you want to retry sending this failed email?')) {
            fetch(`${config.apiUrl}/email/retry/${id}`, {
                method: 'POST',
            })
                .then(response => {
                    if (!response.ok) {
                        throw new Error('Failed to retry email');
                    }
                    return response.text();
                })
                .then(result => {
                    alert(result);
                    // Refresh the email list
                    const params = {
                        page: currentPage - 1,
                        size: emailsPerPage,
                        filter: filter
                    };
                    return fetch(`${config.apiUrl}/email/emailsList`, {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json',
                        },
                        body: JSON.stringify(params),
                    });
                })
                .then(response => response.json())
                .then((data: Page<EmailInfo>) => {
                    if (data && Array.isArray(data.content)) {
                        setEmails(data.content);
                        setTotalPages(data.totalPages || 0);
                    } else {
                        console.error('Invalid data format received:', data);
                        setEmails([]);
                        setTotalPages(0);
                    }
                })
                .catch(error => {
                    console.error('Error retrying email:', error);
                    alert(`Error retrying email: ${error.message}`);
                });
        }
    };

    // Function to get status badge CSS class
    const getStatusBadgeClass = (status: string | undefined) => {
        if (!status) return 'status-badge info';
        
        switch (status.toUpperCase()) {
            case 'SENT':
                return 'status-badge success';
            case 'SCHEDULED':
                return 'status-badge warning';
            case 'FAILED':
                return 'status-badge error';
            case 'DRAFT':
            default:
                return 'status-badge info';
        }
    };

    // Function to get readable status text
    const getStatusText = (status: string | undefined) => {
        if (!status) return 'Draft';
        
        switch (status.toUpperCase()) {
            case 'SENT':
                return 'Sent';
            case 'SCHEDULED':
                return 'Scheduled';
            case 'FAILED':
                return 'Failed';
            case 'DRAFT':
                return 'Draft';
            default:
                return status;
        }
    };

    return (
        <div className="email-page">
            <div className="header">
                <h1>Email</h1>
                <button className="create-button" onClick={handleCreate}>Create</button>
            </div>
            <div className="filter">
                <label>Filter by Email:</label>
                <input type="text" value={filter} onChange={handleFilterChange} />
            </div>
            <table className="email-table">
                <thead>
                    <tr>
                        <th>Email Name</th>
                        <th>Modified Time</th>
                        <th>Created By</th>
                        <th>Sent Time</th>
                        <th>Status</th>
                        <th>Actions</th>
                    </tr>
                </thead>
                <tbody>
                    {Array.isArray(emails) && emails.map(email => (
                        <tr key={email.id}>
                            <td>{email.emailName}</td>
                            <td>{email.modifiedTime ? new Date(email.modifiedTime).toLocaleString() : ''}</td>
                            <td>{email.createdBy}</td>
                            <td>{email.sentTime ? new Date(email.sentTime).toLocaleString() : 'Not Set'}</td>
                            <td>
                                <span className={getStatusBadgeClass(email.status)}>
                                    {getStatusText(email.status)}
                                </span>
                                {email.status === 'FAILED' && email.errorMessage && (
                                    <span className="error-tooltip" title={email.errorMessage}>
                                        <i className="error-icon">!</i>
                                    </span>
                                )}
                            </td>
                            <td>
                                <button className="edit-button" onClick={() => handleEdit(email.id, email.emailName)}>Edit</button>
                                <button className="delete-button" onClick={() => handleDelete(email.id)}>Delete</button>
                                
                                {(!email.status || email.status === 'DRAFT') && (
                                    <button className="send-button" onClick={() => handleSend(email.id)}>Send</button>
                                )}
                                {email.status === 'FAILED' && (
                                    <button className="retry-button" onClick={() => handleRetry(email.id)}>Retry</button>
                                )}
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>
            <div className="pagination">
                <button onClick={() => paginate(currentPage - 1)} disabled={currentPage === 1}>Previous</button>
                {Array.isArray([...Array(totalPages).keys()]) && [...Array(totalPages).keys()].map(number => (
                    <button key={number + 1} onClick={() => paginate(number + 1)}>
                        {number + 1}
                    </button>
                ))}
                <button onClick={() => paginate(currentPage + 1)} disabled={currentPage === totalPages}>Next</button>
            </div>
            <div className="emails-per-page">
                <label>Emails per page:</label>
                <select value={emailsPerPage} onChange={(e) => setEmailsPerPage(Number(e.target.value))}>
                    <option value={5}>5</option>
                    <option value={10}>10</option>
                    <option value={20}>20</option>
                </select>
            </div>
        </div>
    );
};

export default EmailPage;
