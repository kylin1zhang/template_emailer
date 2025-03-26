import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import './EmailPage.css'; // Ensure the correct path to the CSS file
import { config } from 'config/config';
import { EmailInfo } from 'interface/EmailInfo';
import { Page } from 'interface/Page';

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
                setEmails(data.content);
                setTotalPages(data.totalPages);
            })
            .catch(error => console.error('Error fetching emails:', error));
    }, [currentPage, emailsPerPage, filter]);

    const handleEdit = (id: string, email: string) => {
        navigate(`/email/${id}`, { state: { email } });
    };

    const handleDelete = (id: string) => {
        console.log('Delete email with id:', id);
    };

    const handleCreate = () => {
        navigate('/email/create');
    };

    const handleFilterChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        setFilter(e.target.value);
        setCurrentPage(1);
    };

    const paginate = (pageNumber: number) => setCurrentPage(pageNumber);

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
                        <th>Actions</th>
                    </tr>
                </thead>
                <tbody>
                    {emails.map(email => (
                        <tr key={email.id}>
                            <td>{email.emailName}</td>
                            <td>{email.modifiedTime ? new Date(email.modifiedTime).toLocaleString() : ''}</td>
                            <td>{email.createdBy}</td>
                            <td>{email.sentTime ? new Date(email.sentTime).toLocaleString() : 'Not Ready to Sent'}</td>
                            <td>
                                <button className="edit-button" onClick={() => handleEdit(email.id, email.emailName)}>Edit</button>
                                <button className="delete-button" onClick={() => handleDelete(email.id)}>Delete</button>
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>
            <div className="pagination">
                <button onClick={() => paginate(currentPage - 1)} disabled={currentPage === 1}>Previous</button>
                {[...Array(totalPages).keys()].map(number => (
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
