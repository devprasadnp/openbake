import { ButtonHTMLAttributes, forwardRef } from "react";
import { cn } from "@/lib/utils";

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: "primary" | "secondary" | "outline" | "ghost" | "danger";
  size?: "sm" | "md" | "lg";
}

const variants = {
  primary: "bg-primary text-white hover:opacity-90",
  secondary: "bg-secondary text-white hover:opacity-90",
  outline: "border-2 border-primary text-primary hover:bg-primary hover:text-white",
  ghost: "text-text-secondary hover:bg-cream hover:text-primary",
  danger: "bg-error text-white hover:opacity-90",
};

const sizes = {
  sm: "px-3 py-1.5 text-sm",
  md: "px-5 py-2.5 text-sm",
  lg: "px-6 py-3 text-base",
};

const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className, variant = "primary", size = "md", disabled, ...props }, ref) => (
    <button
      ref={ref}
      disabled={disabled}
      className={cn(
        "inline-flex items-center justify-center font-semibold rounded-full transition-all duration-200",
        "focus:outline-none focus:ring-2 focus:ring-primary/30",
        "disabled:opacity-50 disabled:cursor-not-allowed",
        variants[variant],
        sizes[size],
        className
      )}
      {...props}
    />
  )
);
Button.displayName = "Button";

export default Button;
