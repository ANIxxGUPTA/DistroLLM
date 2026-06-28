import React, { useEffect } from 'react';
import './Toast.css';

const Toast = ({ message, type, visible, onHide }) => {
  useEffect(() => {
    if (visible) {
      const timer = setTimeout(() => {
        onHide();
      }, 2000);
      return () => clearTimeout(timer);
    }
  }, [visible, onHide]);

  if (!visible && !message) return null;

  return (
    <div className={`toast-container ${visible ? 'toast-visible' : 'toast-hidden'} toast-${type}`}>
      {message}
    </div>
  );
};

export default Toast;
