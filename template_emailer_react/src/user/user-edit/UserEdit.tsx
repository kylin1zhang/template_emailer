// UserEdit.tsx
import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { config } from 'config/config';
import './UserEdit.css';
import { UserInfo } from '../../interface/UserInfo';

const UserEditor = () => {
    const { id } = useParams();
    const navigate = useNavigate();
    const [userInfo, setUserInfo] = useState<UserInfo | null>(null);

    useEffect(() => {
        if (id) {
            fetch(`${config.apiUrl}/user/${id}`)
                .then(response => {
                    if (!response.ok) {
                        throw new Error('Failed to fetch user');
                    }
                    return response.json();
                })
                .then(data => {
                    if (!data) {
                        throw new Error('No data received');
                    }

                    if (data.modifiedTime) {
                        data.modifiedTime = new Date(data.modifiedTime);
                    }

                    setUserInfo(data);
                })
                .catch(error => {
                    console.error('Error fetching user:', error);
                    setUserInfo({
                        soeId: '',
                        firstName: '',
                        lastName: '',
                        updatedBy: '',
                        email: '',
                        modifiedTime: new Date()
                    });
                });
        } else {
            setUserInfo({
                soeId: '',
                firstName: '',
                lastName: '',
                updatedBy: '',
                email: '',
                modifiedTime: new Date()
            });
        }
    }, [id]);

    const handleSave = () => {
        if (!userInfo) return;

        if (userInfo.soeId.trim() === '') {
            alert('SOE ID cannot be empty');
            return;
        }

        if (userInfo.firstName.trim() === '') {
            alert('First name cannot be empty');
            return;
        }

        if (userInfo.lastName.trim() === '') {
            alert('Last name cannot be empty');
            return;
        }

        if (userInfo.email.trim() === '') {
            alert('Email cannot be empty');
            return;
        }

        const url = `${config.apiUrl}/user/save`;
        const method = 'POST';

        const userToSave = JSON.parse(JSON.stringify(userInfo));

        if (!userToSave.modifiedTime) userToSave.modifiedTime = new Date();

        console.log('Saving user data:', JSON.stringify(userToSave, null, 2));

        fetch(url, {
            method,
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(userToSave),
        })
            .then(response => {
                console.log('Save user response status:', response.status);
                if (!response.ok) {
                    throw new Error(`Save failed: ${response.status} ${response.statusText}`);
                }
                return response.text();
            })
            .then(text => {
                console.log('Raw response content:', text);

                if (!text || text.trim() === '') {
                    console.error('Received empty response');
                    throw new Error('Server returned empty response');
                }

                try {
                    if (text.startsWith('{')) {
                        const jsonObj = JSON.parse(text);
                        if (jsonObj && (jsonObj.id || jsonObj._id)) {
                            return jsonObj.id || jsonObj._id;
                        }
                    }

                    if (text.trim().match(/^[a-f0-9]{24}$/i)) {
                        return text.trim();
                    }

                    if (text.trim().match(/^"[a-f0-9]{24}"$/i)) {
                        return text.trim().replace(/^"|"$/g, '');
                    }

                    if (text.length < 100 && !text.includes(' ')) {
                        return text.trim();
                    }

                    console.error('Could not extract valid ID from response:', text);
                    throw new Error('Server response format not recognized');
                } catch (e: any) {
                    console.error('Response parsing error:', e);
                    if (text && text.trim() && !text.includes(' ') && text.length < 100) {
                        return text.trim();
                    }
                    throw new Error(`Response parsing failed: ${e.message}`);
                }
            })
            .then(id => {
                if (id) {
                    console.log('Final ID:', id);

                    setUserInfo(prev => {
                        if (prev) {
                            return { ...prev, id: id.toString() };
                        }
                        return prev;
                    });

                    alert('User saved successfully');
                    navigate(-1); // Navigate back to the previous page
                    return;
                }
                throw new Error('Server did not return a valid ID');
            })
            .catch(error => {
                console.error('Error saving user:', error);
                alert(`Failed to save user. Error: ${error.message}`);
            });
    };

    return (
        <div className="user-editor">
            <h2>Edit User</h2>
            {userInfo && (
                <form onSubmit={e => { e.preventDefault(); handleSave(); }}>
                    <div className="editor-section">
                        <label>SOEID:</label>
                        <input
                            type="text"
                            value={userInfo.soeId}
                            onChange={e => setUserInfo({ ...userInfo, soeId: e.target.value })}
                            required
                        />
                    </div>
                    <div className="editor-section">
                        <label>First Name:</label>
                        <input
                            type="text"
                            value={userInfo.firstName}
                            onChange={e => setUserInfo({ ...userInfo, firstName: e.target.value })}
                            required
                        />
                    </div>
                    <div className="editor-section">
                        <label>Last Name:</label>
                        <input
                            type="text"
                            value={userInfo.lastName}
                            onChange={e => setUserInfo({ ...userInfo, lastName: e.target.value })}
                            required
                        />
                    </div>
                    <div className="editor-section">
                        <label>Email:</label>
                        <input
                            type="email"
                            value={userInfo.email}
                            onChange={e => setUserInfo({ ...userInfo, email: e.target.value })}
                            required
                        />
                    </div>
                    <div className="button-group">
                        <button type="button" onClick={() => navigate(-1)}>Back</button>
                        <div style={{flexGrow: 1}}></div>
                        <button type="submit" className="save-button">Save</button>
                    </div>
                </form>
            )}
        </div>
    );
};

export default UserEditor;
