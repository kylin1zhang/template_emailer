import React, { useState, useEffect } from 'react';
import { config } from 'config/config';
import './FetchData.css';
import moment from 'moment';

const CACHE_KEY = 'rodApiData';
const CACHE_EXPIRY_KEY = 'rodApiDataExpiry';
const CACHE_EXPIRY_TIME = 30 * 60 * 1000; // Cache expiry time: 30 minutes

const FetchDataComponent = () => {
    const [data, setData] = useState<any[]>([]);
    const [filteredData, setFilteredData] = useState<any[]>([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [releasedId, setReleaseId] = useState<string>('');
    const [expandedRow, setExpandedRow] = useState<string | null>(null); // Current open row ID
    const [summaryRow, setSummaryRow] = useState<string | null>(null); // Current visible summary ID

    // Check and retrieve cached data
    const fetchData = async () => {
        setLoading(true);
        setError(null);

        try {

            const cachedData = localStorage.getItem(CACHE_KEY);
            const cachedExpiry = localStorage.getItem(CACHE_EXPIRY_KEY);

            if (cachedData && cachedExpiry && Date.now() < parseInt(cachedExpiry)) {

                const parsedData = JSON.parse(cachedData);
                setData(parsedData);
                setFilteredData(parsedData);
                setLoading(false);
                return;
            }


            const response = await fetch(`${config.apiUrl}/fetch-data?releaseId=${releasedId}`);
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            const result = await response.json();
            const fetchedData = Array.isArray(result) ? result : [result];

            // Update data and cache
            setData(fetchedData);
            setFilteredData(fetchedData);
            localStorage.setItem(CACHE_KEY, JSON.stringify(fetchedData));
            localStorage.setItem(CACHE_EXPIRY_KEY, (Date.now() + CACHE_EXPIRY_TIME).toString());
        } catch (error: any) {
            setError(error.message);
        } finally {
            setLoading(false);
        }
    };

    // Triggered when page loads
    useEffect(() => {
        fetchData();
    }, [releasedId]); // Re-fetch data when releaseId changes

    // Handle filtering
    const handleFilterChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const value = e.target.value;
        setReleaseId(value);
        if (value) {
            setFilteredData(data.filter((item) => item.id.includes(value)));
        } else {
            setFilteredData(data);
        }
    };


    // Toggle details display
    const toggleDetails = (id: string) => {
        setExpandedRow(expandedRow === id ? null : id);
    };

    // Toggle summary display
    const toggleSummary = (id: string) => {
        setSummaryRow(summaryRow === id ? null : id);
    };

    // Copy summary to clipboard
    const copySummaryToClipboard = (item: any) => {
        const summaryHtml = `
      <table border="1">
        <thead>
          <tr>
            <th>Pipeline</th>
            <th>Release Candidate Version</th>
          </tr>
        </thead>
        <tbody>
          ${item.pipelines
                .map(
                    (pipeline: any) => `
            <tr>
              <td>${pipeline.name}</td>
              <td>${pipeline.version}</td>
            </tr>
          `
                )
                .join('')}
        </tbody>
      </table>
    `;
        navigator.clipboard.writeText(summaryHtml).then(
            () => alert('Summary copied to clipboard!'),
            (err) => alert('Failed to copy summary: ' + err)
        );
    };

    // Render collapsible table
    const renderTable = () => {
        return (
            <table className="user-table">
                <thead>
                    <tr>
                        <th>ID</th>
                        <th>Start Date</th>
                        <th>Actions</th>
                    </tr>
                </thead>
                <tbody>
                    {filteredData.map((item) => (
                        <React.Fragment key={item.id}>
                            <tr>
                                <td>{item.id}</td>
                                <td>{moment(item.startDateTimeIso).format('YYYY-MM-DD')}</td>
                                <td>
                                    <button
                                        className="toggle-button"
                                        onClick={() => toggleDetails(item.id)}
                                    >
                                        Toggle Details
                                    </button>
                                    <button 
                                    className="summary-button" 
                                    onClick={() => toggleSummary(item.id)}
                                    >
                                        Show Summary
                                    </button>
                                    <button 
                                    className="copy-button" 
                                    onClick={() => copySummaryToClipboard(item)}
                                    >
                                        Copy Summary
                                    </button>
                                </td>
                            </tr>
                            {expandedRow === item.id && (
                                <tr>
                                    <td colSpan={3}>
                                        <pre>{JSON.stringify(item, null, 2)}</pre>
                                    </td>
                                </tr>
                            )}
                            {summaryRow === item.id && (
                                <tr>
                                    <td colSpan={3}>
                                        <table className="summary-table">
                                            <thead>
                                                <tr>
                                                    <th>Pipelines</th>
                                                    <th>Release Candidate Version</th>
                                                </tr>
                                            </thead>
                                            <tbody>
                                                {item.pipelines.map((pipeline: any, index: number) => (
                                                    <tr key={index}>
                                                        <td>{pipeline.name}</td>
                                                        <td>{pipeline.version}</td>
                                                    </tr>
                                                ))}
                                            </tbody>
                                        </table>
                                    </td>
                                </tr>
                            )}
                        </React.Fragment>
                    ))}
                </tbody>
            </table>
        );
    };

    return (
        <div className="template-page">
            <div className="header">
                <h1>ROD Release Info</h1>
            </div>
            <div className="filter">
                <label htmlFor="releasedId">Filter by Release ID:</label>
                <input
                    type="text"
                    id="releasedId"
                    value={releasedId}
                    onChange={handleFilterChange}
                    placeholder="Enter Release ID"
                />
            </div>
            {loading && <p>Loading...</p>}
            {error && <p className="error">{error}</p>}
            {!loading && !error && renderTable()}
        </div>
    );
};

export default FetchDataComponent;
