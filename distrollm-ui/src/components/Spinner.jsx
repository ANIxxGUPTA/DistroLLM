import React from 'react';
import './Spinner.css';

const Spinner = ({ size = 20 }) => {
  return (
    <div 
      className="spinner" 
      style={{ width: size, height: size }}
    ></div>
  );
};

export default Spinner;
