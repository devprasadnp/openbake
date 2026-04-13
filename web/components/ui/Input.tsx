import { InputHTMLAttributes, forwardRef } from "react";
import { cn } from "@/lib/utils";

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
  hint?: string;
}

const Input = forwardRef<HTMLInputElement, InputProps>(
  ({ className, label, error, hint, id, ...props }, ref) => (
    <div className="w-full">
      {label && (
        <label htmlFor={id} className="mb-1.5 block text-sm font-semibold text-text-primary">
          {label}
        </label>
      )}
      <input
        ref={ref}
        id={id}
        aria-invalid={Boolean(error)}
        aria-describedby={id ? (error ? `${id}-error` : hint ? `${id}-hint` : undefined) : undefined}
        className={cn(
          "w-full rounded-xl border bg-white px-4 py-3 text-text-primary shadow-sm",
          "placeholder:text-text-secondary/60",
          "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/35 focus-visible:border-primary",
          "disabled:cursor-not-allowed disabled:bg-surface-muted disabled:text-text-secondary",
          error ? "border-error/80" : "border-border hover:border-border-strong",
          className
        )}
        {...props}
      />
      {hint && !error && (
        <p id={id ? `${id}-hint` : undefined} className="mt-1.5 text-xs text-text-secondary">
          {hint}
        </p>
      )}
      {error && (
        <p id={id ? `${id}-error` : undefined} className="mt-1.5 text-sm font-medium text-error">
          {error}
        </p>
      )}
    </div>
  )
);
Input.displayName = "Input";

export default Input;
