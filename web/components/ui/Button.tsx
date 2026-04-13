import { ButtonHTMLAttributes, forwardRef } from "react";
import { Loader2 } from "lucide-react";
import { cn } from "@/lib/utils";

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: "primary" | "secondary" | "outline" | "ghost" | "danger";
  size?: "sm" | "md" | "lg";
  isLoading?: boolean;
}

const variants = {
  primary: "bg-gradient-to-r from-primary to-primary/90 text-white shadow-sm hover:shadow-md hover:brightness-105",
  secondary: "bg-gradient-to-r from-secondary to-secondary/90 text-text-primary shadow-sm hover:shadow-md hover:brightness-105",
  outline: "border-2 border-primary/70 text-primary bg-transparent hover:bg-primary/10",
  ghost: "text-text-secondary hover:bg-surface-muted hover:text-primary",
  danger: "bg-gradient-to-r from-error to-error/90 text-white shadow-sm hover:shadow-md hover:brightness-105",
};

const sizes = {
  sm: "px-3.5 py-2 text-sm",
  md: "px-5 py-2.5 text-sm",
  lg: "px-6 py-3 text-base",
};

const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className, variant = "primary", size = "md", disabled, isLoading = false, children, ...props }, ref) => (
    <button
      ref={ref}
      disabled={disabled || isLoading}
      className={cn(
        "inline-flex items-center justify-center gap-2 font-semibold rounded-full transition-all",
        "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/35 focus-visible:ring-offset-2 focus-visible:ring-offset-cream",
        "disabled:opacity-60 disabled:cursor-not-allowed disabled:shadow-none",
        variants[variant],
        sizes[size],
        className
      )}
      {...props}
    >
      {isLoading && <Loader2 size={16} className="animate-spin" aria-hidden="true" />}
      {children}
    </button>
  )
);
Button.displayName = "Button";

export default Button;
