import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import './UserPage.css'; // Ensure the correct path to the CSS file
import { config } from 'config/config';
import { UserInfo } from 'interface/UserInfo';
import { Page } from 'interface/Page';

const UserPage: React.FC = () => {
  const [users, setUsers] = useState<UserInfo[]>([]);
  const [currentPage, setCurrentPage] = useState(1);
  const [usersPerPage, setUsersPerPage] = useState(10);
  const [totalPages, setTotalPages] = useState(0);
  const [filter, setFilter] = useState('');
  const navigate = useNavigate();

  useEffect(() => {
    const params = {
      page: currentPage - 1,
      size: usersPerPage,
      soeid: filter
    };

    fetch(`${config.apiUrl}/user/usersList`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(params),
    })
      .then(response => response.json())
      .then((data: Page<UserInfo>) => {
        setUsers(data.content);
        setTotalPages(data.totalPages);
      })
      .catch(error => console.error('Error fetching users:', error));
  }, [currentPage, usersPerPage, filter]);

  const handleEdit = (id: string, name: string) => {
    navigate(`/user/${id}`, { state: { name } });
  };

  const handleDelete = (id: string) => {
    console.log('Delete user with id:', id);
  };

  const handleCreate = () => {
    navigate('/user/create');
  };

  const handleFilterChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFilter(e.target.value);
    setCurrentPage(1);
  };

  const paginate = (pageNumber: number) => setCurrentPage(pageNumber);

  return (
    <div className="user-page">
      <div className="header">
        <h1>Users</h1>
        <button className="create-button" onClick={handleCreate}>Create</button>
      </div>
      <div className="filter">
        <label>Filter by Name:</label>
        <input type="text" value={filter} onChange={handleFilterChange} />
      </div>
      <table className="user-table">
        <thead>
          <tr>
            <th>Name</th>
            <th>Soeid</th>
            <th>Email</th>
            <th>Modified Time</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {users?.map(user => (
            <tr key={user.soeId}>
              <td>{`${user.firstName} ${user.lastName}`}</td>
              <td>{user.soeId}</td>
              <td>{user.email}</td>
              <td>{new Date(user.modifiedTime).toLocaleString()}</td>
              <td>
                <button className="edit-button" onClick={() => handleEdit(user.soeId, `${user.firstName} ${user.lastName}`)}>Edit</button>
                <button className="delete-button" onClick={() => handleDelete(user.soeId)}>Delete</button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      <div className="pagination">
        <button onClick={() => paginate(currentPage - 1)} disabled={currentPage === 1}>Previous</button>
        {[...Array(totalPages).keys()].map(number => (
          <button key={number + 1} onClick={() => paginate(number + 1)}>{number + 1}</button>
        ))}
        <button onClick={() => paginate(currentPage + 1)} disabled={currentPage === totalPages}>Next</button>
      </div>
      <div className="users-per-page">
        <label>Users per page:</label>
        <select value={usersPerPage} onChange={(e) => setUsersPerPage(Number(e.target.value))}>
          <option value={5}>5</option>
          <option value={10}>10</option>
          <option value={20}>20</option>
        </select>
      </div>
    </div>
  );
};

export default UserPage;
