import { config } from 'config/config';

export const registerReleaseTool = (unlayer) => {
  console.log('registerReleaseTool called with unlayer:', unlayer);
  unlayer.registerTool({
    name: 'release',
    label: 'Release Table',
    icon: 'fa-table',
    supportedDisplayModes: ['web', 'email'],
    options: {
      default: {
        title: 'Release Table'
      }
    },
    renderers: {
      web: (props) => {
        return <div id="release-table"></div>;
      },
      email: (props) => {
        return <div id="release-table"></div>;
      }
    },
    exporters: {
      web: (props) => {
        return <div id="release-table"></div>;
      },
      email: (props) => {
        return <div id="release-table"></div>;
      }
    }
  });
};

const fetchData = async () => {
  try {
    const response = await fetch(`${config.apiUrl}/release/list`);
    const data = await response.json();

    const table = document.createElement('table');
    const thead = document.createElement('thead');
    const tbody = document.createElement('tbody');

    const headerRow = document.createElement('tr');
    const releaseHeader = document.createElement('th');
    const versionHeader = document.createElement('th');

    releaseHeader.textContent = 'Project';
    versionHeader.textContent = 'Version';
    headerRow.appendChild(releaseHeader);
    headerRow.appendChild(versionHeader);
    thead.appendChild(headerRow);

    data.forEach((item) => {
      const row = document.createElement('tr');
      const releaseCell = document.createElement('td');
      const versionCell = document.createElement('td');

      releaseCell.textContent = item.project;
      versionCell.textContent = item.version;
      row.appendChild(releaseCell);
      row.appendChild(versionCell);
      tbody.appendChild(row);
    });

    table.appendChild(thead);
    table.appendChild(tbody);
    document.getElementById('release-table').appendChild(table);
  } catch (error) {
    console.error('Error fetching release data:', error);
  }
};

document.addEventListener('DOMContentLoaded', fetchData);
