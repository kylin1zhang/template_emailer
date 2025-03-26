import React from 'react';
import { BrowserRouter as Router, Route, Routes } from 'react-router-dom';
import './App.css';
import Navbar from 'navigator/Navbar';
import UserPage from 'user/UserPage';

import TemplatePage from 'template/TemplatePage';
import TemplateEditor from 'template/template-edit/TemplateEdit';
import EmailPage from 'email/EmailPage';
import EmailEditor from 'email/email-edit/EmailEdit';
import FetchDataComponent from 'template/template-edit/FetchData';

const App: React.FC = () => {
    return (
        <Router>
            <div className="App">
                <Navbar />
                <Routes>
                    <Route path="/" element={<TemplatePage />} />
                    <Route path="/user" element={<UserPage />} />
                    <Route path="/user/create" element={<div>Create User Page</div>} />
                    <Route path="/user/:id" element={<div>Edit User Page</div>} />
                    <Route path="/template" element={<TemplatePage />} />
                    <Route path="/template/create" element={<TemplateEditor />} />
                    <Route path="/template/:id" element={<TemplateEditor />} />
                    <Route path="/email" element={<EmailPage />} />
                    <Route path="/email/create" element={<EmailEditor />} />
                    <Route path="/email/:id" element={<EmailEditor />} />
                    <Route path="/rod" element={<FetchDataComponent />} />
                </Routes>
            </div>
        </Router>
    );
}

export default App;
