import React from 'react';
import { BrowserRouter as Router, Route, Routes } from 'react-router-dom';
import './App.css';
import Navbar from 'navigator/Navbar';
import UserPage from 'user/UserPage';

import TemplatePage from 'template/TemplatePage';
//import TemplateEditor from 'template/template-edit/TemplateEdit';
import EmailPage from 'email/EmailPage';
import EmailEditor from 'email/email-edit/EmailEdit';
import FetchDataComponent from 'template/template-edit/FetchData';

import GrapesJSEditor from 'template/template-edit/TemplateEdit';

const App: React.FC = () => {
    const location = useLocation();

    const hideNavbar = !hideNavbarRoutes.some(route => location.pathname.startsWith(route))
    return (
        <Router>
            <div className="App">
                {showNavbar &&<Navbar />}
                <Routes>
                    <Route path="/" element={<TemplatePage />} />

                    <Route path="/user" element={<UserPage />} />
                    <Route path="/user/create" element={<div>Create User Page</div>} />
                    <Route path="/user/:id" element={<div>Edit User Page</div>} />

                    <Route path="/template" element={<TemplatePage />} />
                    <Route path="/template/create" element={<GrapesJSEditor />} />
                    <Route path="/template/:id" element={<GrapesJSEditor />} />

                    <Route path="/email" element={<EmailPage />} />
                    <Route path="/email/create" element={<EmailEditor />} />
                    <Route path="/email/:id" element={<EmailEditor />} />
                    <Route path="/rod" element={<FetchDataComponent />} />
                </Routes>
            </div>
        </Router>
    );
};

const Root: React.FC = () =>{
    return (
        <Router>
            <App></App>
        </Router>
    );
};

export default App;
