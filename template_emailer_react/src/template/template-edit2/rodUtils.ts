
import { config } from 'config/config';


const CACHE_KEY = 'rodApiData';
const CACHE_EXPIRY_KEY = 'rodApiDataExpiry';
const CACHE_EXPIRY_TIME = 30 * 60 * 1000; // Cache expiry time: 30 minutes

/**
 * Fetch ROD data (with cache check)
 */
export const fetchRodData = async (releaseId): Promise<any[]> => {
  try {
    // Check cache
    const cachedData = localStorage.getItem(CACHE_KEY);
    const cachedExpiry = localStorage.getItem(CACHE_EXPIRY_KEY);

    if (cachedData && cachedExpiry && Date.now() < parseInt(cachedExpiry)) {
      // If cache is valid, return cached data
      return JSON.parse(cachedData);
    }

    // If cache is invalid, fetch from API
    const response = await fetch(`${config.apiUrl}/fetch-data?releaseId=${releaseId}`);
    if (!response.ok) {
      throw new Error('Failed to fetch ROD data');
    }
    const result = await response.json();
    const fetchedData = Array.isArray(result) ? result : [result];

    // Update cache
    localStorage.setItem(CACHE_KEY, JSON.stringify(fetchedData));
    localStorage.setItem(CACHE_EXPIRY_KEY, (Date.now() + CACHE_EXPIRY_TIME).toString());

    return fetchedData;
  } catch (error) {
    console.error('Error fetching ROD data:', error);
    return [];
  }
};
