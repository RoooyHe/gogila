import axios from 'axios';

const http = axios.create({
    baseURL: '/api',
    timeout: 10000
});

http.interceptors.response.use(
    (resp) => resp,
    (err) => {
        console.error('HTTP Error', err);
        return Promise.reject(err);
    }
);

export default http;