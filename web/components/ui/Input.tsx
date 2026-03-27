import { InputHTMLAttributes, forwardRef } from "react";
import { cn } from "@/lib/utils";

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
}

const Input = forwardRef<HTMLInputElement, InputProps>(
  ({ className, label, error, id, ...props }, ref) => (
    <div className="w-full">
      {label && (
        <label htmlFor={id} className="block text-sm font-medium text-text-primary mb-1">
          {label}
        </label>
      )}
      <input
        ref={ref}
        id={id}
        className={cn(
          "w-full px-4 py-3 rounded-xl border bg-white text-text-primary",
          "placeholder:text-text-secondary/50",
          "focus:outline-none focus:ring-2 focus:ring-primary/30 focus:border-primary",
          "transition-colors duration-200",
          error ? "border-error" : "border-border",
          className
        )}
        {...props}
      />
      {error && <p className="mt-1 text-sm text-error">{error}</p>}
    </div>
  )
);
Input.displayName = "Input";

export default Input;
