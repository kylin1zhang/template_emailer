import React, { useState, useEffect } from 'react';
import './Navbar.css';
import { Link, useLocation } from 'react-router-dom';

const Navbar: React.FC = () => {
  const location = useLocation();
  const [activeTab, setActiveTab] = useState(location.pathname);

  useEffect(() => {
    if (location.pathname === '/') {
      setActiveTab('/');
    } else {
      setActiveTab(location.pathname);
    }
  }, [location.pathname]);

  return (
    <nav className="navbar">
      <div className="navbar-header">
        Template-Emailer
      </div>
      <ul className="navbar-list">
        <li className="navbar-item">
          <Link 
            to="/user" 
            className={activeTab === '/user' ? 'active' : ''} 
            onClick={() => setActiveTab('/user')}
          >
            User
          </Link>
        </li>
        <li className="navbar-item">
          <Link 
            to="/template" 
            className={activeTab === '/template' ? 'active' : ''} 
            onClick={() => setActiveTab('/template')}
          >
            Template
          </Link>
        </li>
        <li className="navbar-item">
          <Link 
            to="/email" 
            className={activeTab === '/email' ? 'active' : ''} 
            onClick={() => setActiveTab('/email')}
          >
            Email
          </Link>
        </li>
        <li className="navbar-item">
          <Link 
            to="/rod" 
            className={activeTab === '/rod' ? 'active' : ''} 
            onClick={() => setActiveTab('/rod')}
          >
            ROD
          </Link>
        </li>
      </ul>
    </nav>
  );
};

export default Navbar;
