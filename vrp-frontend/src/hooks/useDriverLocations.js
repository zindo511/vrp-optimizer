import { useEffect, useState } from 'react';
import { Client } from '@stomp/stompjs';

const WS_URL = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080')
  .replace(/^http/, 'ws') + '/ws';

/**
 * Subscribe /topic/driver-location và trả về map:
 * { [driverId]: { driverId, driverName, lat, lng } }
 */
export const useDriverLocations = () => {
  const [locations, setLocations] = useState({});

  useEffect(() => {
    const client = new Client({
      brokerURL: WS_URL,
      reconnectDelay: 5000,
      onConnect: () => {
        client.subscribe('/topic/driver-location', (frame) => {
          const data = JSON.parse(frame.body);
          setLocations(prev => ({ ...prev, [data.driverId]: data }));
        });
      },
      onStompError: (frame) => {
        console.error('WebSocket STOMP error:', frame.headers['message']);
      },
    });

    client.activate();
    return () => { client.deactivate(); };
  }, []);

  return locations;
};
