import React from 'react';
import './Button.css';

/**
 * Button Component
 * Supports 'primary', 'secondary', 'destructive', and 'inverse' variants.
 */
export const Button = ({
  children,
  variant = 'primary',
  disabled = false,
  onClick,
  type = 'button',
  className = '',
  style = {}
}) => {
  const normalizedVariant = ['primary', 'secondary', 'destructive', 'inverse'].includes(variant) ? variant : 'primary';

  return (
    <button
      type={type}
      className={`bank-btn bank-btn--${normalizedVariant} ${className}`}
      disabled={disabled}
      onClick={onClick}
      style={style}
    >
      {children}
    </button>
  );
};

export default Button;
