import React, { useState } from 'react';
import { config } from 'config/config';

const FetchDataComponent = () => {
    const [data, setData] = useState<any[]>([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [inputValue, setInputValue] = useState<string>('');



    const fetchData = async (releaseId: string) => {
        setLoading(true);
        setError(null);
        try {
            const response = await fetch(`${config.apiUrl}/fetch-data?releaseId=${releaseId}`);
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            const result = await response.json();
            setData(Array.isArray(result) ? result : [result]);
        } catch (error: any) {
            setError(error.message);
        } finally {
            setLoading(false);
        }
    };

    const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        setInputValue(e.target.value);
    };

    const handleButtonClick = () => {
        fetchData(inputValue);
    };

    return (
        <div>
            <input
                type="text"
                value={inputValue}
                onChange={handleInputChange}
                placeholder="Enter releaseId"
            />
            <button onClick={handleButtonClick} disabled={loading}>
                Fetch Data
            </button>
            {loading && <p>Loading...</p>}
            {error && <p>Error: {error}</p>}
            <ul>
                {data.map((item, index) => (
                    <li key={index}>{JSON.stringify(item)}</li>
                ))}
            </ul>
        </div>
    );
};

export default FetchDataComponent;
