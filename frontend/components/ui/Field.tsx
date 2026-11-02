"use client";
import { useId, useState, type InputHTMLAttributes, type ReactNode, type SelectHTMLAttributes, type TextareaHTMLAttributes } from "react";
import { Icon } from "./Icon";

interface FieldProps {
  label: string;
  hint?: ReactNode;
  error?: string;
  children: (ids: { id: string; describedBy?: string; invalid: boolean }) => ReactNode;
}

/** Campo com etiqueta, ajuda e erro ligados por aria-describedby. */
export function Field({ label, hint, error, children }: FieldProps) {
  const id = useId();
  const hintId = hint ? `${id}-hint` : undefined;
  const errId = error ? `${id}-err` : undefined;
  return (
    <div className="field">
      <label htmlFor={id}>{label}</label>
      {children({ id, describedBy: [hintId, errId].filter(Boolean).join(" ") || undefined, invalid: !!error })}
      {hint && !error && <span id={hintId} className="hint">{hint}</span>}
      {error && (
        <span id={errId} className="err" role="alert">
          <Icon name="warn" />
          {error}
        </span>
      )}
    </div>
  );
}

export function Input({ invalid, ok, className, ...props }: InputHTMLAttributes<HTMLInputElement> & { invalid?: boolean; ok?: boolean }) {
  return <input className={["input", invalid && "is-err", ok && "is-ok", className].filter(Boolean).join(" ")} aria-invalid={invalid || undefined} {...props} />;
}

export function Select({ invalid, className, children, ...props }: SelectHTMLAttributes<HTMLSelectElement> & { invalid?: boolean }) {
  return (
    <select className={["select", invalid && "is-err", className].filter(Boolean).join(" ")} aria-invalid={invalid || undefined} {...props}>
      {children}
    </select>
  );
}

export function TextArea({ invalid, className, ...props }: TextareaHTMLAttributes<HTMLTextAreaElement> & { invalid?: boolean }) {
  return <textarea className={["input", invalid && "is-err", className].filter(Boolean).join(" ")} aria-invalid={invalid || undefined} {...props} />;
}

/** Campo de palavra-passe com botão para mostrar/esconder (o texto não sai do campo; o leitor de ecrã anuncia o estado). */
export function PasswordInput({ invalid, className, ...props }: Omit<InputHTMLAttributes<HTMLInputElement>, "type"> & { invalid?: boolean }) {
  const [visible, setVisible] = useState(false);
  return (
    <div className="password-wrap">
      <Input {...props} invalid={invalid} className={["password-input", className].filter(Boolean).join(" ")}
        type={visible ? "text" : "password"} autoCapitalize="none" spellCheck={false} />
      <button type="button" className="password-toggle" aria-pressed={visible}
        aria-label={visible ? "Esconder a palavra-passe" : "Mostrar a palavra-passe"} onClick={() => setVisible((v) => !v)}>
        <Icon name={visible ? "eyeOff" : "eye"} />
      </button>
    </div>
  );
}
