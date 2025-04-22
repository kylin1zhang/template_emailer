import React from 'react';
import { BrowserRouter as Router, Route, Routes, useLocation } from 'react-router-dom';
import './App.css';
import Navbar from 'navigator/Navbar';
import UserPage from 'user/UserPage';

import TemplatePage from 'template/TemplatePage';
// import TemplateEditor from 'template/template-edit/TemplateEdit';
import EmailPage from 'email/EmailPage';
import EmailEditor from 'email/email-edit/EmailEdit';
import FetchDataComponent from 'template/template-edit/FetchData';
import UserEditor from 'user/user-edit/UserEdit';
import GrapesJSEditor from 'template/template-edit2/TemplateEdit';

const App: React.FC = () => {
  const location = useLocation();

  // Hide Navbar for specific routes
  const hideNavbarRoutes = ['/template/'];
  const showNavbar = !hideNavbarRoutes.some(route => location.pathname.startsWith(route));

  return (
    <div className="App">
      {showNavbar && <Navbar />}
      <Routes>
        <Route path="/" element={<TemplatePage />} />

        <Route path="/user" element={<UserPage />} />
        <Route path="/user/create" element={<UserEditor />} />
        <Route path="/user/:id" element={<UserEditor />} />
        
        <Route path="/template" element={<TemplatePage />} />
        <Route path="/template/create" element={<GrapesJSEditor />} />
        <Route path="/template/:id" element={<GrapesJSEditor />} />
        {/* <Route path="/template" element={<TemplateEditor />} /> */}

        <Route path="/email" element={<EmailPage />} />
        <Route path="/email/create" element={<EmailEditor />} />
        <Route path="/email/:id" element={<EmailEditor />} />

        <Route path="/rod" element={<FetchDataComponent />} />
      </Routes>
    </div>
  );
};

const Root: React.FC = () => {
  return (
    <Router>
      <App />
    </Router>
  );
};

export default Root;
